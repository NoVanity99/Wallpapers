package io.github.novanity99.wallpaperselector.gl

import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.exp

/** Turns touch events into the u_touch / u_taps shader uniforms. Written from the UI thread, read from the GL thread. */
class TouchState {
    private var x = 0.5f
    private var y = 0.5f
    private var down = false
    private var presence = 0f
    private val tapX = FloatArray(TAPS)
    private val tapY = FloatArray(TAPS)
    private val tapTime = LongArray(TAPS)
    private var next = 0
    private var lastX = -1f
    private var lastY = -1f

    @Synchronized
    fun onEvent(e: MotionEvent, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val nx = (e.x / width).coerceIn(0f, 1f)
        val ny = (1f - e.y / height).coerceIn(0f, 1f)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                down = true; x = nx; y = ny
                addTap(nx, ny)
            }
            MotionEvent.ACTION_MOVE -> {
                down = true; x = nx; y = ny
                // drop a new ripple every so often while dragging
                if (abs(nx - lastX) + abs(ny - lastY) > 0.12f) addTap(nx, ny)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> down = false
        }
    }

    private fun addTap(nx: Float, ny: Float) {
        tapX[next] = nx; tapY[next] = ny; tapTime[next] = System.nanoTime()
        next = (next + 1) % TAPS
        lastX = nx; lastY = ny
    }

    /** Advances the fade by [dt] seconds and fills [touch] (3 floats) and [taps] (16 floats). */
    @Synchronized
    fun fill(dt: Float, touch: FloatArray, taps: FloatArray) {
        presence = if (down) presence + (1f - presence) * (1f - exp(-dt * 10f)) else presence * exp(-dt * 1.6f)
        if (presence < 0.001f) presence = 0f
        touch[0] = x; touch[1] = y; touch[2] = presence
        val now = System.nanoTime()
        for (i in 0 until TAPS) {
            val age = if (tapTime[i] == 0L) 99f else (now - tapTime[i]) / 1e9f
            val valid = age < 5f
            taps[i * 4] = tapX[i]
            taps[i * 4 + 1] = tapY[i]
            taps[i * 4 + 2] = if (valid) age else 99f
            taps[i * 4 + 3] = if (valid) 1f else 0f
        }
    }

    private companion object {
        const val TAPS = 4
    }
}
