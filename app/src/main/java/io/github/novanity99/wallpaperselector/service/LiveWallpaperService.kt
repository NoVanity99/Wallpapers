package io.github.novanity99.wallpaperselector.service

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import io.github.novanity99.wallpaperselector.App
import io.github.novanity99.wallpaperselector.data.DEFAULT_WALLPAPER
import io.github.novanity99.wallpaperselector.gl.RenderThread

/** The live wallpaper. Plays whichever wallpaper was picked in the app. */
class LiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = ShaderEngine()

    private inner class ShaderEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val app get() = application as App
        private lateinit var renderer: RenderThread
        private var currentId: String? = null
        private var lowBattery = false
        private var colors: WallpaperColors? = null
        private var width = 0
        private var height = 0

        private val powerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) readBattery(intent)
                apply()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            renderer = RenderThread("wallpaper-gl", app.repository.shaderHeader)
            app.settings.prefs.registerOnSharedPreferenceChangeListener(this)
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply {
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            }
            // ACTION_BATTERY_CHANGED is sticky, so this also gives us the current level.
            val sticky = if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(powerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(powerReceiver, filter)
            }
            sticky?.let(::readBattery)
            apply()
        }

        private fun readBattery(intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            lowBattery = level >= 0 && scale > 0 && level * 100 / scale <= LOW_BATTERY_PERCENT && !plugged
        }

        /** Pushes the current settings and wallpaper choice to the render thread. */
        private fun apply() {
            val s = app.settings.read()
            renderer.fps = s.fps
            renderer.speed = s.speed
            val powerSave = (getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode
            renderer.setAnimate(!(s.batterySaver && (lowBattery || powerSave)))

            val lockOnly = Build.VERSION.SDK_INT >= 34 &&
                wallpaperFlags and WallpaperManager.FLAG_LOCK != 0 &&
                wallpaperFlags and WallpaperManager.FLAG_SYSTEM == 0
            val id = when {
                isPreview -> s.previewId
                lockOnly -> s.lockId
                else -> s.homeId
            }
            if (id != currentId) {
                currentId = id
                val def = app.repository.loadLocal(id) ?: app.repository.loadLocal(DEFAULT_WALLPAPER)
                renderer.setWallpaper(def)
                if (Build.VERSION.SDK_INT >= 27) {
                    colors = def?.colors?.let { c ->
                        WallpaperColors(Color.valueOf(c[3], c[4], c[5]), Color.valueOf(c[6], c[7], c[8]), Color.valueOf(c[0], c[1], c[2]))
                    }
                    notifyColorsChanged()
                }
            }
        }

        // Lets Android match its theme colours (Material You) to the wallpaper's palette.
        override fun onComputeColors(): WallpaperColors? = colors

        override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) = apply()

        override fun onWallpaperFlagsChanged(which: Int) {
            super.onWallpaperFlagsChanged(which)
            apply()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            width = w; height = h
            renderer.setSurface(holder.surface, w, h)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            renderer.releaseSurface()
            super.onSurfaceDestroyed(holder)
        }

        // The system calls this with false when the screen turns off or the wallpaper is covered.
        override fun onVisibilityChanged(visible: Boolean) = renderer.setVisible(visible)

        override fun onTouchEvent(event: MotionEvent) = renderer.touch.onEvent(event, width, height)

        override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xStep: Float, yStep: Float, xPixels: Int, yPixels: Int) {
            renderer.offset = xOffset
        }

        override fun onDestroy() {
            app.settings.prefs.unregisterOnSharedPreferenceChangeListener(this)
            unregisterReceiver(powerReceiver)
            renderer.quit()
            super.onDestroy()
        }
    }

    private companion object {
        const val LOW_BATTERY_PERCENT = 15
    }
}
