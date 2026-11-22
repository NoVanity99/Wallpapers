package io.github.novanity99.wallpaperselector.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import io.github.novanity99.wallpaperselector.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Knows where every wallpaper lives:
 *  - bundled ones in the APK assets (assets/wallpapers/<id>/)
 *  - downloaded ones in app storage (files/wallpapers/<id>/)
 *  - everything else in the online catalog (BuildConfig.CATALOG_BASE_URL)
 */
class WallpaperRepository(private val context: Context) {
    private val baseUrl = BuildConfig.CATALOG_BASE_URL.trimEnd('/') + "/"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val wallpapersDir = File(context.filesDir, "wallpapers")
    private val catalogCache = File(context.filesDir, "catalog.json")
    private val previewDir = File(context.cacheDir, "preview")
    private val thumbDir = File(context.cacheDir, "thumbs")

    private val _items = MutableStateFlow<List<WallpaperItem>>(emptyList())
    val items: StateFlow<List<WallpaperItem>> = _items
    private val _status = MutableStateFlow(CatalogStatus.Idle)
    val status: StateFlow<CatalogStatus> = _status
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors

    private val progress = ConcurrentHashMap<String, Float>()
    private val thumbs = LruCache<String, Bitmap>(64)

    val shaderHeader: String by lazy { readAsset("shader_header.glsl") }

    private val bundled: List<CatalogEntry> by lazy {
        runCatching { CatalogEntry.listFromJson(readAsset("wallpapers/bundled.json")) }.getOrDefault(emptyList())
    }

    @Volatile
    private var remote: List<CatalogEntry> =
        runCatching { CatalogEntry.listFromJson(catalogCache.readText()) }.getOrDefault(emptyList())

    init {
        rebuild()
    }

    /** Fetches catalog.json. On failure the last saved copy (or just the bundled wallpapers) stays in use. */
    fun refresh() {
        if (_status.value == CatalogStatus.Loading) return
        _status.value = CatalogStatus.Loading
        scope.launch {
            try {
                val text = String(httpGet(baseUrl + "catalog.json"))
                remote = CatalogEntry.listFromJson(text)
                catalogCache.writeText(text)
                _status.value = CatalogStatus.Online
            } catch (e: Exception) {
                Log.w(TAG, "Catalog refresh failed", e)
                _status.value = CatalogStatus.Offline
            }
            rebuild()
        }
    }

    @Synchronized
    private fun rebuild() {
        val local = HashMap<String, CatalogEntry>()
        wallpapersDir.listFiles()?.forEach { dir ->
            val entryFile = File(dir, ENTRY_FILE)
            if (dir.isDirectory && entryFile.exists()) {
                runCatching { CatalogEntry.fromJson(org.json.JSONObject(entryFile.readText())) }.getOrNull()
                    ?.let { local[it.id] = it }
            }
        }
        val bundledById = bundled.associateBy { it.id }
        val merged = LinkedHashMap<String, CatalogEntry>()
        remote.filter { it.format <= SUPPORTED_FORMAT }.forEach { merged[it.id] = it }
        bundled.forEach { merged.putIfAbsent(it.id, it) }
        local.values.forEach { merged.putIfAbsent(it.id, it) }
        _items.value = merged.values
            .sortedWith(compareBy({ CATEGORIES.indexOf(it.category).let { i -> if (i < 0) 99 else i } }, { it.name }))
            .map { e ->
                WallpaperItem(
                    entry = e,
                    bundled = e.id in bundledById,
                    downloaded = e.id in local,
                    localVersion = local[e.id]?.version ?: bundledById[e.id]?.version ?: 0,
                    progress = progress[e.id],
                )
            }
    }

    /** Downloads every file of a wallpaper into app storage so it works offline. */
    fun download(entry: CatalogEntry) {
        if (progress.putIfAbsent(entry.id, 0f) != null) return
        rebuild()
        scope.launch {
            val tmp = File(wallpapersDir, entry.id + ".tmp")
            try {
                tmp.deleteRecursively()
                if (!tmp.mkdirs()) throw IOException("Can't create ${tmp.path}")
                val total = entry.files.sumOf { it.size }.coerceAtLeast(1)
                var done = 0L
                var lastShown = 0f
                for (f in entry.files) {
                    val conn = open("$baseUrl${entry.path}/${f.name}")
                    try {
                        conn.inputStream.use { input ->
                            File(tmp, f.name).outputStream().use { out ->
                                val buf = ByteArray(8192)
                                while (true) {
                                    val n = input.read(buf)
                                    if (n < 0) break
                                    out.write(buf, 0, n)
                                    done += n
                                    val p = (done.toFloat() / total).coerceAtMost(1f)
                                    if (p - lastShown >= 0.02f) {
                                        lastShown = p
                                        progress[entry.id] = p
                                        rebuild()
                                    }
                                }
                            }
                        }
                    } finally {
                        conn.disconnect()
                    }
                }
                // Make sure what we got is usable before replacing anything.
                WallpaperDef.parse(File(tmp, DEF_FILE).readText()) { File(tmp, it).readText() }
                File(tmp, ENTRY_FILE).writeText(entry.toJson().toString())
                val target = File(wallpapersDir, entry.id)
                target.deleteRecursively()
                if (!tmp.renameTo(target)) throw IOException("Can't move download into place")
                thumbs.remove(thumbKey(entry))
            } catch (e: Exception) {
                Log.w(TAG, "Download failed: ${entry.id}", e)
                tmp.deleteRecursively()
                _errors.tryEmit("Couldn't download ${entry.name}. Check your connection and try again.")
            } finally {
                progress.remove(entry.id)
                rebuild()
            }
        }
    }

    fun delete(id: String) {
        scope.launch {
            File(wallpapersDir, id).deleteRecursively()
            rebuild()
        }
    }

    /** Loads a wallpaper that is on the device (downloaded copy first, then the bundled one). */
    fun loadLocal(id: String): WallpaperDef? {
        val dir = File(wallpapersDir, id)
        if (File(dir, DEF_FILE).exists()) {
            runCatching { WallpaperDef.parse(File(dir, DEF_FILE).readText()) { File(dir, it).readText() } }
                .onSuccess { return it }
                .onFailure { Log.w(TAG, "Bad downloaded wallpaper $id", it) }
        }
        return runCatching {
            WallpaperDef.parse(readAsset("wallpapers/$id/$DEF_FILE")) { readAsset("wallpapers/$id/$it") }
        }.getOrNull()
    }

    /**
     * Definition for a live preview. For wallpapers that aren't downloaded yet this streams the
     * (tiny) definition and shader into the cache; it is not kept for offline use.
     */
    suspend fun loadForPreview(entry: CatalogEntry): WallpaperDef? = withContext(Dispatchers.IO) {
        loadLocal(entry.id)?.let { return@withContext it }
        val dir = File(previewDir, "${entry.id}-v${entry.version}")
        try {
            if (!File(dir, DEF_FILE).exists()) {
                dir.mkdirs()
                val json = httpGet("$baseUrl${entry.path}/$DEF_FILE")
                val shaderName = org.json.JSONObject(String(json)).optString("shader", "shader.frag")
                if (entry.files.none { it.name == shaderName }) throw IOException("Unknown shader file")
                File(dir, shaderName).writeBytes(httpGet("$baseUrl${entry.path}/$shaderName"))
                File(dir, DEF_FILE).writeBytes(json)
            }
            WallpaperDef.parse(File(dir, DEF_FILE).readText()) { File(dir, it).readText() }
        } catch (e: Exception) {
            dir.deleteRecursively()
            null
        }
    }

    suspend fun loadThumbnail(entry: CatalogEntry): Bitmap? = withContext(Dispatchers.IO) {
        val key = thumbKey(entry)
        thumbs.get(key)?.let { return@withContext it }
        val bytes = runCatching { File(File(wallpapersDir, entry.id), entry.thumbnail).readBytes() }.getOrNull()
            ?: runCatching { context.assets.open("wallpapers/${entry.id}/${entry.thumbnail}").use { it.readBytes() } }.getOrNull()
            ?: runCatching {
                val cached = File(thumbDir, "$key.img")
                if (!cached.exists()) {
                    thumbDir.mkdirs()
                    cached.writeBytes(httpGet("$baseUrl${entry.path}/${entry.thumbnail}"))
                }
                cached.readBytes()
            }.getOrNull()
        bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }?.also { thumbs.put(key, it) }
    }

    private fun thumbKey(entry: CatalogEntry) = "${entry.id}-v${entry.version}"

    private fun readAsset(path: String) = context.assets.open(path).use { it.readBytes().toString(Charsets.UTF_8) }

    private fun open(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.useCaches = false
        val code = conn.responseCode
        if (code != 200) {
            conn.disconnect()
            throw IOException("HTTP $code for $url")
        }
        return conn
    }

    private fun httpGet(url: String): ByteArray {
        val conn = open(url)
        try {
            return conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val TAG = "WallpaperRepository"
        const val DEF_FILE = "wallpaper.json"
        const val ENTRY_FILE = "entry.json"
    }
}
