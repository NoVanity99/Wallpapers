package io.github.novanity99.wallpaperselector.data

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import io.github.novanity99.wallpaperselector.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val version: String, val url: String) : UpdateState
    data class Downloading(val version: String, val progress: Float) : UpdateState
    data class Ready(val version: String, val file: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/**
 * In-app updates from this project's GitHub Releases. The release tag is "v1.0.<build number>"
 * and the build number is also the app's version code, so a higher number means a newer build.
 * Android itself verifies that the downloaded APK is signed with the same key before installing.
 */
class Updater(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    fun check() {
        val s = _state.value
        if (s is UpdateState.Checking || s is UpdateState.Downloading) return
        _state.value = UpdateState.Checking
        scope.launch {
            _state.value = try {
                val conn = open(BuildConfig.UPDATE_API_URL)
                val json = try {
                    JSONObject(conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) })
                } finally {
                    conn.disconnect()
                }
                val tag = json.getString("tag_name")
                val build = tag.substringAfterLast('.').toIntOrNull() ?: 0
                val assets = json.optJSONArray("assets")
                val apk = (0 until (assets?.length() ?: 0)).map { assets!!.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk") }
                if (build > BuildConfig.VERSION_CODE && apk != null) {
                    UpdateState.Available(tag.removePrefix("v"), apk.getString("browser_download_url"))
                } else {
                    UpdateState.UpToDate
                }
            } catch (e: Exception) {
                Log.w(TAG, "Update check failed", e)
                UpdateState.Failed(
                    if (e.message?.startsWith("HTTP 404") == true) {
                        "No public release found. If the GitHub repository is private, the app can't see its releases."
                    } else {
                        "Couldn't check for updates. Check your connection."
                    },
                )
            }
        }
    }

    fun download(update: UpdateState.Available) {
        _state.value = UpdateState.Downloading(update.version, 0f)
        scope.launch {
            val dir = File(context.cacheDir, "updates")
            val file = File(dir, "WallpaperSelector.apk")
            _state.value = try {
                dir.deleteRecursively()
                dir.mkdirs()
                val conn = open(update.url)
                try {
                    val total = conn.contentLengthLong.coerceAtLeast(1)
                    conn.inputStream.use { input ->
                        file.outputStream().use { out ->
                            val buf = ByteArray(16384)
                            var done = 0L
                            var shown = 0f
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                done += n
                                val p = (done.toFloat() / total).coerceAtMost(1f)
                                if (p - shown >= 0.02f) {
                                    shown = p
                                    _state.value = UpdateState.Downloading(update.version, p)
                                }
                            }
                        }
                    }
                } finally {
                    conn.disconnect()
                }
                UpdateState.Ready(update.version, file)
            } catch (e: Exception) {
                Log.w(TAG, "Update download failed", e)
                UpdateState.Failed("The update download failed. Try again.")
            }
        }
    }

    /** Hands the downloaded APK to Android's installer, which asks the user to confirm. */
    fun install(activityContext: Context, ready: UpdateState.Ready) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", ready.file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            activityContext.startActivity(intent)
        } catch (e: Exception) {
            _state.value = UpdateState.Failed("Couldn't open the installer on this device.")
        }
    }

    private fun open(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 20_000
        conn.setRequestProperty("Accept", if (url == BuildConfig.UPDATE_API_URL) "application/vnd.github+json" else "application/octet-stream")
        val code = conn.responseCode
        if (code != 200) {
            conn.disconnect()
            throw IOException("HTTP $code for $url")
        }
        return conn
    }

    private companion object {
        const val TAG = "Updater"
    }
}
