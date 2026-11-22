package io.github.novanity99.wallpaperselector.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

const val THEME_SYSTEM = 0
const val THEME_LIGHT = 1
const val THEME_DARK = 2
const val DEFAULT_WALLPAPER = "aurora"
const val CUSTOM_PREFIX = "custom_"

data class SettingsState(
    val speed: Float,
    val fps: Int,
    val batterySaver: Boolean,
    val theme: Int,
    val livePreviews: Boolean,
    val homeId: String,
    val lockId: String,
    val previewId: String,
    val autoUpdateCheck: Boolean,
)

/** App settings. Shared with the wallpaper service, which lives in the same process. */
class Settings(context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Reads the saved values. Always current, also right after a write. */
    fun read() = SettingsState(
        speed = prefs.getFloat("speed", 1f),
        fps = prefs.getInt("fps", 60),
        batterySaver = prefs.getBoolean("batterySaver", true),
        theme = prefs.getInt("theme", THEME_SYSTEM),
        livePreviews = prefs.getBoolean("livePreviews", true),
        homeId = prefs.getString("homeId", DEFAULT_WALLPAPER)!!,
        lockId = prefs.getString("lockId", DEFAULT_WALLPAPER)!!,
        previewId = prefs.getString("previewId", DEFAULT_WALLPAPER)!!,
        autoUpdateCheck = prefs.getBoolean("autoUpdateCheck", true),
    )

    private fun readCustoms(): Map<String, Custom> = prefs.all
        .filter { it.key.startsWith(CUSTOM_PREFIX) && it.value is String }
        .map { it.key.removePrefix(CUSTOM_PREFIX) to Custom.fromJson(it.value as String) }.toMap()

    /** Per-wallpaper customisations, keyed by wallpaper id. */
    private val _customs = MutableStateFlow(readCustoms())
    val customs: StateFlow<Map<String, Custom>> = _customs

    fun custom(id: String): Custom? = prefs.getString(CUSTOM_PREFIX + id, null)?.let(Custom::fromJson)

    /** Saves the customisation for one wallpaper; null (or all defaults) puts it back as designed. */
    fun setCustom(id: String, custom: Custom?) {
        val c = custom?.takeIf { it != Custom() }
        prefs.edit().apply { if (c == null) remove(CUSTOM_PREFIX + id) else putString(CUSTOM_PREFIX + id, c.toJson()) }.apply()
        _customs.value = if (c == null) _customs.value - id else _customs.value + (id to c)
    }

    fun setAutoUpdateCheck(v: Boolean) = edit { putBoolean("autoUpdateCheck", v) }

    private val _state = MutableStateFlow(read())
    val state: StateFlow<SettingsState> = _state

    // Every write goes through here, so the UI state can simply be refreshed afterwards
    // (apply() updates the in-memory values immediately and saves to disk in the background).
    private inline fun edit(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        _state.value = read()
    }

    fun setSpeed(v: Float) = edit { putFloat("speed", v) }
    fun setFps(v: Int) = edit { putInt("fps", v) }
    fun setBatterySaver(v: Boolean) = edit { putBoolean("batterySaver", v) }
    fun setTheme(v: Int) = edit { putInt("theme", v) }
    fun setLivePreviews(v: Boolean) = edit { putBoolean("livePreviews", v) }

    /** Remembers which wallpaper the service should play on the home and/or lock screen. */
    fun selectWallpaper(id: String, home: Boolean, lock: Boolean) = edit {
        putString("previewId", id)
        if (home) putString("homeId", id)
        if (lock) putString("lockId", id)
    }
}
