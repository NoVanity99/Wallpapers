package io.github.novanity99.wallpaperselector.gl

import android.opengl.GLES20
import android.util.Log
import io.github.novanity99.wallpaperselector.data.WallpaperDef
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max

/**
 * Draws one wallpaper: a full-screen quad running the wallpaper's fragment shader.
 * When the wallpaper asks for a renderScale below 1 it is drawn into a smaller
 * off-screen texture first and stretched to the screen, which saves a lot of GPU time.
 * All methods must be called on the GL thread with a current context.
 */
class ShaderRenderer(private val header: String, private val footer: String) {
    var scaleMultiplier = 1f

    private var def: WallpaperDef? = null
    private var dirty = true
    private var paramsDirty = false
    private var uAdjust = -1
    private val noTouch = FloatArray(3)
    private val noTaps = FloatArray(16) { if (it % 4 == 2) 99f else 0f }
    private var program = 0
    private var blitProgram = 0
    private var fbo = 0
    private var fboTex = 0
    private var fboW = 0
    private var fboH = 0

    private var uResolution = -1
    private var uTime = -1
    private var uTouch = -1
    private var uTaps = -1
    private var uOffset = -1
    private val uColors = IntArray(4)
    private var paramLocs = IntArray(0)
    private var paramValues = FloatArray(0)

    private val quad: FloatBuffer = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer()
        .apply { put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)); position(0) }

    fun setDefinition(d: WallpaperDef?) {
        // Same shader with different colours or parameters: no need to compile again.
        if (d?.shader != def?.shader || program == 0) dirty = true else paramsDirty = true
        def = d
    }


    fun draw(width: Int, height: Int, time: Float, touch: FloatArray, taps: FloatArray, offset: Float) {
        if (dirty) rebuild() else if (paramsDirty) readParams()
        val d = def
        val touchOn = d?.touch != false
        val scale = ((d?.renderScale ?: 1f) * scaleMultiplier).coerceIn(0.2f, 1f)
        val useFbo = scale < 0.999f
        val rw = if (useFbo) max(1, (width * scale).toInt()) else width
        val rh = if (useFbo) max(1, (height * scale).toInt()) else height

        if (useFbo) {
            ensureFbo(rw, rh)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo)
        } else {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        }
        GLES20.glViewport(0, 0, rw, rh)
        GLES20.glUseProgram(program)
        GLES20.glUniform2f(uResolution, rw.toFloat(), rh.toFloat())
        GLES20.glUniform1f(uTime, time)
        GLES20.glUniform3fv(uTouch, 1, if (touchOn) touch else noTouch, 0)
        GLES20.glUniform4fv(uTaps, 4, if (touchOn) taps else noTaps, 0)
        if (d != null) GLES20.glUniform4fv(uAdjust, 1, d.adjust, 0) else GLES20.glUniform4f(uAdjust, 1f, 1f, 1f, 0f)
        GLES20.glUniform1f(uOffset, offset)
        if (d != null) {
            for (i in 0 until 4) GLES20.glUniform3fv(uColors[i], 1, d.colors, i * 3)
            for (i in paramLocs.indices) GLES20.glUniform1f(paramLocs[i], paramValues[i])
        }
        drawQuad(program)

        if (useFbo) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            GLES20.glViewport(0, 0, width, height)
            GLES20.glUseProgram(blitProgram)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex)
            GLES20.glUniform1i(GLES20.glGetUniformLocation(blitProgram, "u_tex"), 0)
            GLES20.glUniform2f(GLES20.glGetUniformLocation(blitProgram, "u_size"), width.toFloat(), height.toFloat())
            drawQuad(blitProgram)
        }
    }

    private fun drawQuad(prog: Int) {
        val loc = GLES20.glGetAttribLocation(prog, "a_pos")
        GLES20.glEnableVertexAttribArray(loc)
        GLES20.glVertexAttribPointer(loc, 2, GLES20.GL_FLOAT, false, 0, quad)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun rebuild() {
        dirty = false
        if (program != 0) GLES20.glDeleteProgram(program)
        if (blitProgram == 0) blitProgram = link(BLIT)
        val d = def
        program = if (d != null) link(header + "\n" + d.shader + footer) else 0
        if (program == 0) {
            if (d != null) Log.e(TAG, "Shader for ${d.id} failed to compile, showing fallback")
            program = link(header + "\n" + FALLBACK + footer)
        }
        uResolution = GLES20.glGetUniformLocation(program, "u_resolution")
        uTime = GLES20.glGetUniformLocation(program, "u_time")
        uTouch = GLES20.glGetUniformLocation(program, "u_touch")
        uTaps = GLES20.glGetUniformLocation(program, "u_taps")
        uOffset = GLES20.glGetUniformLocation(program, "u_offset")
        uAdjust = GLES20.glGetUniformLocation(program, "u_adjust")
        for (i in 0 until 4) uColors[i] = GLES20.glGetUniformLocation(program, "u_color${i + 1}")
        readParams()
    }

    private fun readParams() {
        paramsDirty = false
        val params = def?.params?.entries?.toList().orEmpty()
        paramLocs = IntArray(params.size) { GLES20.glGetUniformLocation(program, "u_" + params[it].key) }
        paramValues = FloatArray(params.size) { params[it].value }
    }

    private fun ensureFbo(w: Int, h: Int) {
        if (fbo != 0 && fboW == w && fboH == h) return
        val ids = IntArray(1)
        if (fbo == 0) {
            GLES20.glGenFramebuffers(1, ids, 0); fbo = ids[0]
            GLES20.glGenTextures(1, ids, 0); fboTex = ids[0]
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo)
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTex, 0)
        fboW = w; fboH = h
    }

    private fun link(fragment: String): Int {
        val vs = compile(GLES20.GL_VERTEX_SHADER, VERTEX)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, fragment)
        if (vs == 0 || fs == 0) return 0
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) {
            Log.e(TAG, "Link failed: " + GLES20.glGetProgramInfoLog(prog))
            GLES20.glDeleteProgram(prog)
            return 0
        }
        return prog
    }

    private fun compile(type: Int, source: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, source)
        GLES20.glCompileShader(s)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) {
            Log.e(TAG, "Compile failed: " + GLES20.glGetShaderInfoLog(s))
            GLES20.glDeleteShader(s)
            return 0
        }
        return s
    }

    private companion object {
        const val TAG = "ShaderRenderer"
        const val VERTEX = "attribute vec2 a_pos;\nvoid main() { gl_Position = vec4(a_pos, 0., 1.); }\n"
        const val BLIT = "precision mediump float;\nuniform sampler2D u_tex;\nuniform vec2 u_size;\n" +
            "void main() { gl_FragColor = texture2D(u_tex, gl_FragCoord.xy / u_size); }\n"
        const val FALLBACK = "void main() { vec2 uv = gl_FragCoord.xy / u_resolution;\n" +
            "gl_FragColor = vec4(mix(vec3(.05, .06, .1), vec3(.14, .1, .22), uv.y + .05 * sin(u_time + uv.x * 3.)), 1.); }\n"
    }
}
