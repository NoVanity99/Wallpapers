package io.github.novanity99.wallpaperselector.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

const val THEME_SYSTEM = 0
const val THEME_LIGHT = 1
const val THEME_DARK = 2
const val DEFAULT_WALLPAPER = "aurora"

data class SettingsState(
    val speed: Float,
    val fps: Int,
    val batterySaver: Boolean,
    val theme: Int,
    val livePreviews: Boolean,
    val homeId: String,
    val lockId: String,
    val previewId: String,
)

/** App settings. Shared with the wallpaper service, which lives in the same process. */
class Settings(context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun read() = SettingsState(
        speed = prefs.getFloat("speed", 1f),
        fps = prefs.getInt("fps", 60),
        batterySaver = prefs.getBoolean("batterySaver", true),
        theme = prefs.getInt("theme", THEME_SYSTEM),
        livePreviews = prefs.getBoolean("livePreviews", true),
        homeId = prefs.getString("homeId", DEFAULT_WALLPAPER)!!,
        lockId = prefs.getString("lockId", DEFAULT_WALLPAPER)!!,
        previewId = prefs.getString("previewId", DEFAULT_WALLPAPER)!!,
    )

    private val _state = MutableStateFlow(read())
    val state: StateFlow<SettingsState> = _state

    // Held in a field: SharedPreferences only keeps weak references to listeners.
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> _state.value = read() }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setSpeed(v: Float) = prefs.edit().putFloat("speed", v).apply()
    fun setFps(v: Int) = prefs.edit().putInt("fps", v).apply()
    fun setBatterySaver(v: Boolean) = prefs.edit().putBoolean("batterySaver", v).apply()
    fun setTheme(v: Int) = prefs.edit().putInt("theme", v).apply()
    fun setLivePreviews(v: Boolean) = prefs.edit().putBoolean("livePreviews", v).apply()

    /** Remembers which wallpaper the service should play on the home and/or lock screen. */
    fun selectWallpaper(id: String, home: Boolean, lock: Boolean) {
        prefs.edit().apply {
            putString("previewId", id)
            if (home) putString("homeId", id)
            if (lock) putString("lockId", id)
        }.apply()
    }
}
