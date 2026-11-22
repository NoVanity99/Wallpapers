package io.github.novanity99.wallpaperselector.gl

import android.os.Handler
import android.os.HandlerThread
import android.view.Choreographer
import io.github.novanity99.wallpaperselector.data.WallpaperDef
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * A thread that owns an EGL context and draws a wallpaper into whatever surface it is given.
 * Used by the live wallpaper service and by the in-app previews.
 *
 * Frames are driven by vsync (Choreographer) and only while the surface is visible, so a
 * hidden wallpaper costs nothing. The public methods can be called from any thread.
 */
class RenderThread(name: String, header: String) {
    private val thread = HandlerThread(name).also { it.start() }
    private val handler = Handler(thread.looper)
    private val egl = EglCore()
    private val renderer = ShaderRenderer(header)

    val touch = TouchState()
    @Volatile var fps = 60
    @Volatile var speed = 1f
    @Volatile var offset = 0.5f

    // Everything below is only touched on the render thread.
    private var choreographer: Choreographer? = null
    private var visible = false
    private var animate = true
    private var width = 0
    private var height = 0
    private var scheduled = false
    private var stopped = false
    private var lastDraw = 0L
    private var time = 0.0
    private var defSpeed = 1f
    private val touchBuf = FloatArray(3)
    private val tapBuf = FloatArray(16)
    private val callback = Choreographer.FrameCallback { now ->
        scheduled = false
        frame(now, false)
    }

    /** [window] is a Surface, SurfaceHolder or SurfaceTexture. Call again when its size changes. */
    fun setSurface(window: Any, w: Int, h: Int) = post {
        if (!egl.hasSurface && !egl.createSurface(window)) return@post
        width = w; height = h
        frame(System.nanoTime(), true)
    }

    /** Blocks until the surface is no longer in use, so the caller can safely destroy it. */
    fun releaseSurface() {
        val latch = CountDownLatch(1)
        val posted = handler.post {
            egl.destroySurface()
            latch.countDown()
        }
        if (posted) latch.await(2, TimeUnit.SECONDS)
    }

    fun setWallpaper(def: WallpaperDef?) = post {
        renderer.setDefinition(def)
        defSpeed = def?.speed ?: 1f
        frame(System.nanoTime(), true)
    }

    /** Extra resolution scale on top of the wallpaper's own renderScale (used for small previews). */
    fun setRenderScale(scale: Float) = post { renderer.scaleMultiplier = scale }

    fun setVisible(v: Boolean) = post {
        if (visible != v) {
            visible = v
            if (v) frame(System.nanoTime(), true)
        }
    }

    /** false = keep showing a still frame (battery saver). */
    fun setAnimate(a: Boolean) = post {
        if (animate != a) {
            animate = a
            if (a) schedule()
        }
    }

    fun quit() {
        handler.post {
            stopped = true
            choreographer?.removeFrameCallback(callback)
            egl.release()
        }
        thread.quitSafely()
    }

    private fun post(block: () -> Unit) {
        handler.post(block)
    }

    private fun frame(now: Long, force: Boolean) {
        if (stopped || !visible || !egl.hasSurface || width <= 0 || height <= 0) return
        val interval = 1_000_000_000L / fps.coerceIn(1, 240)
        // FPS cap: skip this vsync if the previous frame is too recent.
        if (!force && now - lastDraw < interval - 3_000_000L) {
            schedule()
            return
        }
        val dt = ((now - lastDraw) / 1e9).coerceIn(0.0, 0.1)
        lastDraw = now
        if (animate) {
            time += dt * speed * defSpeed
            if (time > TIME_WRAP) time -= TIME_WRAP // keeps float precision in the shaders
        }
        touch.fill(dt.toFloat(), touchBuf, tapBuf)
        renderer.draw(width, height, time.toFloat(), touchBuf, tapBuf, offset)
        if (!egl.swap()) {
            egl.destroySurface()
            return
        }
        if (animate) schedule()
    }

    private fun schedule() {
        if (scheduled || stopped) return
        scheduled = true
        val c = choreographer ?: Choreographer.getInstance().also { choreographer = it }
        c.postFrameCallback(callback)
    }

    private companion object {
        const val TIME_WRAP = 14400.0
    }
}
