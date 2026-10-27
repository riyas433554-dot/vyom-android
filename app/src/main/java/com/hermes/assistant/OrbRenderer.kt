package com.hermes.assistant

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OrbRenderer : GLSurfaceView.Renderer {
    @Volatile var mode: Int = 0
    @Volatile var reduceMotion: Boolean = false

    private val particleCount = 3000
    private val positions = FloatArray(particleCount * 3)
    private val colors = FloatArray(particleCount * 4)
    private val random = Random(73)

    private var program = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var matrixHandle = 0
    private var pointSizeHandle = 0
    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val vp = FloatArray(16)
    private var angle = 0f
    private lateinit var positionBuffer: FloatBuffer
    private lateinit var colorBuffer: FloatBuffer

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)

        val vertexShader = """
            attribute vec3 aPosition;
            attribute vec4 aColor;
            uniform mat4 uMatrix;
            uniform float uPointSize;
            varying vec4 vColor;
            void main() {
                gl_Position = uMatrix * vec4(aPosition, 1.0);
                gl_PointSize = uPointSize;
                vColor = aColor;
            }
        """.trimIndent()

        val fragmentShader = """
            precision mediump float;
            varying vec4 vColor;
            void main() {
                vec2 p = gl_PointCoord - vec2(0.5);
                float d = length(p) * 2.0;
                float glow = exp(-d * 5.0) * (1.0 - smoothstep(0.1, 1.0, d));
                gl_FragColor = vec4(vColor.rgb, vColor.a * glow);
            }
        """.trimIndent()

        program = createProgram(vertexShader, fragmentShader)
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        colorHandle = GLES20.glGetAttribLocation(program, "aColor")
        matrixHandle = GLES20.glGetUniformLocation(program, "uMatrix")
        pointSizeHandle = GLES20.glGetUniformLocation(program, "uPointSize")

        positionBuffer = ByteBuffer.allocateDirect(positions.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        colorBuffer = ByteBuffer.allocateDirect(colors.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()

        for (i in 0 until particleCount) {
            val index = i * 3
            val z = random.nextFloat() * 2f - 1f
            val theta = random.nextFloat() * (Math.PI.toFloat() * 2f)
            val r = (1f - z * z).coerceAtLeast(0f).let { sqrt(it) }
            positions[index] = cos(theta) * r
            positions[index + 1] = sin(theta) * r
            positions[index + 2] = z

            val ci = i * 4
            colors[ci] = 0.25f
            colors[ci + 1] = 0.72f
            colors[ci + 2] = 1f
            colors[ci + 3] = 0.85f
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.coerceAtLeast(1)
        Matrix.frustumM(projection, 0, -aspect, aspect, -1f, 1f, 2f, 10f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        angle += if (reduceMotion) 0.0015f else if (mode == 1) 0.035f else 0.008f

        Matrix.setLookAtM(view, 0, 0f, 0f, 4f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.rotateM(view, 0, angle * 57.2958f, 0f, 1f, 0f)
        Matrix.multiplyMM(vp, 0, projection, 0, view, 0)

        val rgb = when (mode) {
            1 -> floatArrayOf(1f, 0.58f, 0.16f)
            2 -> floatArrayOf(0.55f, 0.35f, 1f)
            3 -> floatArrayOf(0.28f, 1f, 0.58f)
            else -> floatArrayOf(0.25f, 0.72f, 1f)
        }
        for (i in 0 until particleCount) {
            val ci = i * 4
            colors[ci] = rgb[0]
            colors[ci + 1] = rgb[1]
            colors[ci + 2] = rgb[2]
        }

        positionBuffer.clear()
        positionBuffer.put(positions).position(0)
        colorBuffer.clear()
        colorBuffer.put(colors).position(0)

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, vp, 0)
        GLES20.glUniform1f(pointSizeHandle, 5.5f)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, positionBuffer)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, particleCount)
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }

    private fun createProgram(vertex: String, fragment: String): Int {
        fun shader(type: Int, source: String): Int {
            val id = GLES20.glCreateShader(type)
            GLES20.glShaderSource(id, source)
            GLES20.glCompileShader(id)
            val status = IntArray(1)
            GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0) {
                val error = GLES20.glGetShaderInfoLog(id)
                android.util.Log.e("VyomOrb", "Shader compile failed: $error")
                GLES20.glDeleteShader(id)
                throw IllegalStateException("OpenGL shader compilation failed: $error")
            }
            return id
        }
        val vs = shader(GLES20.GL_VERTEX_SHADER, vertex)
        val fs = shader(GLES20.GL_FRAGMENT_SHADER, fragment)
        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glLinkProgram(it)
            val status = IntArray(1)
            GLES20.glGetProgramiv(it, GLES20.GL_LINK_STATUS, status, 0)
            if (status[0] == 0) {
                val error = GLES20.glGetProgramInfoLog(it)
                android.util.Log.e("VyomOrb", "Program link failed: $error")
                GLES20.glDeleteProgram(it)
                throw IllegalStateException("OpenGL program linking failed: $error")
            }
            GLES20.glDeleteShader(vs)
            GLES20.glDeleteShader(fs)
        }
    }

    private fun sqrt(value: Float): Float = kotlin.math.sqrt(value)
}
