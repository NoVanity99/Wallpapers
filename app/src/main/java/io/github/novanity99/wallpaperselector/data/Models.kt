package io.github.novanity99.wallpaperselector.data

import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

/** Highest wallpaper/catalog "format" this build understands. Newer entries are hidden. */
const val SUPPORTED_FORMAT = 1

val CATEGORIES = listOf("Abstract", "Space", "Nature", "Neon/Cyber", "Minimal", "Retro")

data class CatalogFile(val name: String, val size: Long)

/** One row of catalog.json. */
data class CatalogEntry(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val interactive: Boolean,
    val version: Int,
    val format: Int,
    val path: String,
    val thumbnail: String,
    val size: Long,
    val files: List<CatalogFile>,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name).put("category", category)
        .put("description", description).put("interactive", interactive)
        .put("version", version).put("format", format).put("path", path)
        .put("thumbnail", thumbnail).put("size", size)
        .put("files", JSONArray().also { arr ->
            files.forEach { arr.put(JSONObject().put("name", it.name).put("size", it.size)) }
        })

    companion object {
        private val SAFE_ID = Regex("[A-Za-z0-9_-]{1,64}")
        private val SAFE_FILE = Regex("[A-Za-z0-9_][A-Za-z0-9._-]{0,63}")

        /** Returns null for malformed entries or names that could escape the wallpaper folder. */
        fun fromJson(o: JSONObject): CatalogEntry? {
            val id = o.optString("id")
            if (!SAFE_ID.matches(id)) return null
            val filesJson = o.optJSONArray("files") ?: JSONArray()
            val files = (0 until filesJson.length()).map {
                val f = filesJson.getJSONObject(it)
                CatalogFile(f.optString("name"), f.optLong("size"))
            }
            val thumbnail = o.optString("thumbnail", "thumbnail.webp")
            if (files.any { !SAFE_FILE.matches(it.name) } || !SAFE_FILE.matches(thumbnail)) return null
            return CatalogEntry(
                id = id,
                name = o.optString("name", id),
                category = o.optString("category", "Abstract"),
                description = o.optString("description"),
                interactive = o.optBoolean("interactive"),
                version = o.optInt("version", 1),
                format = o.optInt("format", 1),
                path = o.optString("path", "wallpapers/$id").trim('/'),
                thumbnail = thumbnail,
                size = o.optLong("size", files.sumOf { it.size }),
                files = files,
            )
        }

        fun listFromJson(text: String): List<CatalogEntry> {
            val arr = JSONObject(text).optJSONArray("wallpapers") ?: return emptyList()
            return (0 until arr.length()).mapNotNull { fromJson(arr.getJSONObject(it)) }
        }
    }
}

/** A slider the user can move for one of a wallpaper's "parameters". */
data class Control(val key: String, val label: String, val min: Float, val max: Float, val default: Float)

/** What the user changed for one wallpaper. Defaults mean "as designed". */
data class Custom(
    val touch: Boolean = true,
    val speed: Float = 1f,
    val quality: Float = 1f,
    val brightness: Float = 1f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val vignette: Float = 0f,
    /** Replacement colours as ARGB ints, by slot; null = keep the wallpaper's own. */
    val colors: List<Int?> = listOf(null, null, null, null),
    val params: Map<String, Float> = emptyMap(),
) {
    fun toJson(): String = JSONObject()
        .put("touch", touch).put("speed", speed.toDouble()).put("quality", quality.toDouble())
        .put("brightness", brightness.toDouble()).put("contrast", contrast.toDouble())
        .put("saturation", saturation.toDouble()).put("vignette", vignette.toDouble())
        .put("colors", JSONArray().also { a -> colors.forEach { a.put(it ?: JSONObject.NULL) } })
        .put("params", JSONObject().also { o -> params.forEach { (k, v) -> o.put(k, v.toDouble()) } })
        .toString()

    companion object {
        fun fromJson(text: String): Custom = runCatching {
            val o = JSONObject(text)
            val c = o.optJSONArray("colors")
            val p = o.optJSONObject("params")
            Custom(
                touch = o.optBoolean("touch", true),
                speed = o.optDouble("speed", 1.0).toFloat(),
                quality = o.optDouble("quality", 1.0).toFloat(),
                brightness = o.optDouble("brightness", 1.0).toFloat(),
                contrast = o.optDouble("contrast", 1.0).toFloat(),
                saturation = o.optDouble("saturation", 1.0).toFloat(),
                vignette = o.optDouble("vignette", 0.0).toFloat(),
                colors = (0 until 4).map { i -> if (c == null || c.isNull(i)) null else c.optInt(i) },
                params = buildMap { p?.keys()?.forEach { k -> put(k, p.optDouble(k, 0.0).toFloat()) } },
            )
        }.getOrDefault(Custom())
    }
}

/** Everything the renderer needs to draw one wallpaper (wallpaper.json + its shader source). */
class WallpaperDef(
    val id: String,
    val version: Int,
    /** Four RGB colours, 12 floats. */
    val colors: FloatArray,
    val speed: Float,
    val renderScale: Float,
    val params: Map<String, Float>,
    val shader: String,
    val controls: List<Control> = emptyList(),
    val touch: Boolean = true,
    /** brightness, contrast, saturation, vignette */
    val adjust: FloatArray = floatArrayOf(1f, 1f, 1f, 0f),
) {
    /** The same wallpaper with the user's changes applied. */
    fun customized(c: Custom?): WallpaperDef {
        if (c == null) return this
        val newColors = colors.copyOf()
        c.colors.forEachIndexed { i, argb ->
            if (argb != null && i < 4) {
                newColors[i * 3] = Color.red(argb) / 255f
                newColors[i * 3 + 1] = Color.green(argb) / 255f
                newColors[i * 3 + 2] = Color.blue(argb) / 255f
            }
        }
        return WallpaperDef(
            id, version, newColors, speed * c.speed, (renderScale * c.quality).coerceIn(0.2f, 1f),
            params + c.params.filterKeys { it in params }, shader, controls, c.touch,
            floatArrayOf(c.brightness, c.contrast, c.saturation, c.vignette),
        )
    }

    /** Colour slot [i] (0..3) as an ARGB int. */
    fun colorInt(i: Int): Int = Color.rgb(
        (colors[i * 3] * 255).toInt(), (colors[i * 3 + 1] * 255).toInt(), (colors[i * 3 + 2] * 255).toInt(),
    )

    companion object {
        fun parse(json: String, readFile: (String) -> String): WallpaperDef {
            val o = JSONObject(json)
            val colorsJson = o.optJSONArray("colors")
            val parsed = (0 until (colorsJson?.length() ?: 0)).mapNotNull {
                runCatching { Color.parseColor(colorsJson!!.getString(it)) }.getOrNull()
            }.ifEmpty { listOf(Color.BLACK, Color.WHITE) }
            val colors = FloatArray(12)
            for (i in 0 until 4) {
                val c = parsed[i % parsed.size]
                colors[i * 3] = Color.red(c) / 255f
                colors[i * 3 + 1] = Color.green(c) / 255f
                colors[i * 3 + 2] = Color.blue(c) / 255f
            }
            val params = HashMap<String, Float>()
            o.optJSONObject("parameters")?.let { p ->
                p.keys().forEach { k -> params[k] = p.optDouble(k, 0.0).toFloat() }
            }
            return WallpaperDef(
                id = o.getString("id"),
                version = o.optInt("version", 1),
                colors = colors,
                speed = o.optDouble("speed", 1.0).toFloat(),
                renderScale = o.optDouble("renderScale", 1.0).toFloat().coerceIn(0.25f, 1f),
                params = params,
                shader = readFile(o.optString("shader", "shader.frag")),
                // Sliders: taken from "controls" when given, otherwise a sensible range around the value.
                controls = params.entries.sortedBy { it.key }.map { (key, value) ->
                    val c = o.optJSONObject("controls")?.optJSONObject(key)
                    Control(
                        key = key,
                        label = c?.optString("label")?.takeIf { it.isNotBlank() } ?: key.replaceFirstChar { it.uppercase() },
                        min = c?.optDouble("min", Double.NaN)?.takeIf { !it.isNaN() }?.toFloat() ?: (value * 0.25f),
                        max = c?.optDouble("max", Double.NaN)?.takeIf { !it.isNaN() }?.toFloat() ?: (value * 2.5f).coerceAtLeast(1f),
                        default = value,
                    )
                },
            )
        }
    }
}

/** A catalog entry plus what we have of it on this device. */
data class WallpaperItem(
    val entry: CatalogEntry,
    val bundled: Boolean,
    val downloaded: Boolean,
    val localVersion: Int,
    /** 0..1 while a download is running, otherwise null. */
    val progress: Float?,
) {
    val available get() = bundled || downloaded
    val updateAvailable get() = available && entry.version > localVersion
}

enum class CatalogStatus { Idle, Loading, Online, Offline }
