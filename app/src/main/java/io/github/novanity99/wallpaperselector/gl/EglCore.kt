package io.github.novanity99.wallpaperselector.gl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.util.Log

/** Minimal EGL wrapper: one ES 2.0 context and at most one window surface. Single-threaded. */
class EglCore {
    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var context: EGLContext = EGL14.EGL_NO_CONTEXT
    private var surface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var config: EGLConfig? = null

    val hasSurface get() = surface != EGL14.EGL_NO_SURFACE

    private fun init(): Boolean {
        if (context != EGL14.EGL_NO_CONTEXT) return true
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) return false
        val attrs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        if (!EGL14.eglChooseConfig(display, attrs, 0, configs, 0, 1, count, 0) || count[0] == 0) return false
        config = configs[0]
        context = EGL14.eglCreateContext(
            display, config, EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0,
        )
        return context != EGL14.EGL_NO_CONTEXT
    }

    /** [window] is a Surface, SurfaceHolder or SurfaceTexture. */
    fun createSurface(window: Any): Boolean {
        if (!init()) {
            Log.e(TAG, "EGL init failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
            return false
        }
        destroySurface()
        surface = try {
            EGL14.eglCreateWindowSurface(display, config, window, intArrayOf(EGL14.EGL_NONE), 0)
        } catch (e: RuntimeException) {
            EGL14.EGL_NO_SURFACE
        }
        if (surface == EGL14.EGL_NO_SURFACE) {
            Log.e(TAG, "eglCreateWindowSurface failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
            return false
        }
        if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
            destroySurface()
            return false
        }
        return true
    }

    fun swap(): Boolean = EGL14.eglSwapBuffers(display, surface)

    fun destroySurface() {
        if (surface == EGL14.EGL_NO_SURFACE) return
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(display, surface)
        surface = EGL14.EGL_NO_SURFACE
    }

    /** Destroys the context too; every GL object created in it goes with it. */
    fun release() {
        destroySurface()
        if (context != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(display, context)
            context = EGL14.EGL_NO_CONTEXT
        }
    }

    private companion object {
        const val TAG = "EglCore"
    }
}
