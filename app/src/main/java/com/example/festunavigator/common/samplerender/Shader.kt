package com.example.festunavigator.common.samplerender

import android.content.res.AssetManager
import android.opengl.GLES30
import android.opengl.GLException
import android.util.Log
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.regex.Matcher


/**
 * Represents a GPU shader, the state of its associated uniforms, and some additional draw state.
 */
class Shader(
    render: SampleRender?,
    vertexShaderCode: String,
    fragmentShaderCode: String,
    defines: Map<String, String>?
) : Closeable {
    /**
     * A factor to be used in a blend function.
     *
     * @see [glBlendFunc](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glBlendFunc.xhtml)
     */
    enum class BlendFactor(  /* package-private */
        val glesEnum: Int
    ) {
        ZERO(GLES30.GL_ZERO), ONE(GLES30.GL_ONE), SRC_COLOR(GLES30.GL_SRC_COLOR), ONE_MINUS_SRC_COLOR(
            GLES30.GL_ONE_MINUS_SRC_COLOR
        ),
        DST_COLOR(GLES30.GL_DST_COLOR), ONE_MINUS_DST_COLOR(GLES30.GL_ONE_MINUS_DST_COLOR), SRC_ALPHA(
            GLES30.GL_SRC_ALPHA
        ),
        ONE_MINUS_SRC_ALPHA(GLES30.GL_ONE_MINUS_SRC_ALPHA), DST_ALPHA(GLES30.GL_DST_ALPHA), ONE_MINUS_DST_ALPHA(
            GLES30.GL_ONE_MINUS_DST_ALPHA
        ),
        CONSTANT_COLOR(GLES30.GL_CONSTANT_COLOR), ONE_MINUS_CONSTANT_COLOR(GLES30.GL_ONE_MINUS_CONSTANT_COLOR), CONSTANT_ALPHA(
            GLES30.GL_CONSTANT_ALPHA
        ),
        ONE_MINUS_CONSTANT_ALPHA(GLES30.GL_ONE_MINUS_CONSTANT_ALPHA);

    }

    private var programId = 0
    private val uniforms: MutableMap<Int, Uniform> = HashMap()
    private var maxTextureUnit = 0
    private val uniformLocations: MutableMap<String, Int> = HashMap()
    private val uniformNames: MutableMap<Int, String> = HashMap()
    private var depthTest = true
    private var depthWrite = true
    private var sourceRgbBlend = BlendFactor.ONE
    private var destRgbBlend = BlendFactor.ZERO
    private var sourceAlphaBlend = BlendFactor.ONE
    private var destAlphaBlend = BlendFactor.ZERO
    override fun close() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId)
            programId = 0
        }
    }

    /**
     * Sets depth test state.
     *
     * @see [glEnable
    ](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glEnable.xhtml) */
    fun setDepthTest(depthTest: Boolean): Shader {
        this.depthTest = depthTest
        return this
    }

    /**
     * Sets depth write state.
     *
     * @see [glDepthMask](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glDepthMask.xhtml).
     */
    fun setDepthWrite(depthWrite: Boolean): Shader {
        this.depthWrite = depthWrite
        return this
    }

    /**
     * Sets blending function.
     *
     * @see [glBlendFunc](https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glBlendFunc.xhtml)
     */
    fun setBlend(sourceBlend: BlendFactor, destBlend: BlendFactor): Shader {
        sourceRgbBlend = sourceBlend
        destRgbBlend = destBlend
        sourceAlphaBlend = sourceBlend
        destAlphaBlend = destBlend
        return this
    }

    /**
     * Sets blending functions separately for RGB and alpha channels.
     *
     * @see [glBlendFunc](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glBlendFuncSeparate.xhtml)
     */
    fun setBlend(
        sourceRgbBlend: BlendFactor,
        destRgbBlend: BlendFactor,
        sourceAlphaBlend: BlendFactor,
        destAlphaBlend: BlendFactor
    ): Shader {
        this.sourceRgbBlend = sourceRgbBlend
        this.destRgbBlend = destRgbBlend
        this.sourceAlphaBlend = sourceAlphaBlend
        this.destAlphaBlend = destAlphaBlend
        return this
    }

    /** Sets a texture uniform.  */
    fun setTexture(name: String, texture: Texture): Shader {
        // Special handling for Textures. If replacing an existing texture uniform, reuse the texture
        // unit.
        val location = getUniformLocation(name)
        val uniform = uniforms[location]
        val textureUnit: Int
        textureUnit = if (uniform !is UniformTexture) {
            maxTextureUnit++
        } else {
            uniform.textureUnit
        }
        uniforms[location] = UniformTexture(textureUnit, texture)
        return this
    }

    /** Sets a `bool` uniform.  */
    fun setBool(name: String, v0: Boolean): Shader {
        val values = intArrayOf(if (v0) 1 else 0)
        uniforms[getUniformLocation(name)] = UniformInt(values)
        return this
    }

    /** Sets an `int` uniform.  */
    fun setInt(name: String, v0: Int): Shader {
        val values = intArrayOf(v0)
        uniforms[getUniformLocation(name)] = UniformInt(values)
        return this
    }

    /** Sets a `float` uniform.  */
    fun setFloat(name: String, v0: Float): Shader {
        val values = floatArrayOf(v0)
        uniforms[getUniformLocation(name)] = Uniform1f(values)
        return this
    }

    /** Sets a `vec2` uniform.  */
    fun setVec2(name: String, values: FloatArray): Shader {
        require(values.size == 2) { "Value array length must be 2" }
        uniforms[getUniformLocation(name)] = Uniform2f(values.clone())
        return this
    }

    /** Sets a `vec3` uniform.  */
    fun setVec3(name: String, values: FloatArray): Shader {
        require(values.size == 3) { "Value array length must be 3" }
        uniforms[getUniformLocation(name)] = Uniform3f(values.clone())
        return this
    }

    /** Sets a `vec4` uniform.  */
    fun setVec4(name: String, values: FloatArray): Shader {
        require(values.size == 4) { "Value array length must be 4" }
        uniforms[getUniformLocation(name)] = Uniform4f(values.clone())
        return this
    }

    /** Sets a `mat2` uniform.  */
    fun setMat2(name: String, values: FloatArray): Shader {
        require(values.size == 4) { "Value array length must be 4 (2x2)" }
        uniforms[getUniformLocation(name)] = UniformMatrix2f(values.clone())
        return this
    }

    /** Sets a `mat3` uniform.  */
    fun setMat3(name: String, values: FloatArray): Shader {
        require(values.size == 9) { "Value array length must be 9 (3x3)" }
        uniforms[getUniformLocation(name)] = UniformMatrix3f(values.clone())
        return this
    }

    /** Sets a `mat4` uniform.  */
    fun setMat4(name: String, values: FloatArray): Shader {
        require(values.size == 16) { "Value array length must be 16 (4x4)" }
        uniforms[getUniformLocation(name)] = UniformMatrix4f(values.clone())
        return this
    }

    /** Sets a `bool` array uniform.  */
    fun setBoolArray(name: String, values: BooleanArray): Shader {
        val intValues = IntArray(values.size)
        for (i in values.indices) {
            intValues[i] = if (values[i]) 1 else 0
        }
        uniforms[getUniformLocation(name)] = UniformInt(intValues)
        return this
    }

    /** Sets an `int` array uniform.  */
    fun setIntArray(name: String, values: IntArray): Shader {
        uniforms[getUniformLocation(name)] = UniformInt(values.clone())
        return this
    }

    /** Sets a `float` array uniform.  */
    fun setFloatArray(name: String, values: FloatArray): Shader {
        uniforms[getUniformLocation(name)] = Uniform1f(values.clone())
        return this
    }

    /** Sets a `vec2` array uniform.  */
    fun setVec2Array(name: String, values: FloatArray): Shader {
        require(values.size % 2 == 0) { "Value array length must be divisible by 2" }
        uniforms[getUniformLocation(name)] = Uniform2f(values.clone())
        return this
    }

    /** Sets a `vec3` array uniform.  */
    fun setVec3Array(name: String, values: FloatArray): Shader {
        require(values.size % 3 == 0) { "Value array length must be divisible by 3" }
        uniforms[getUniformLocation(name)] = Uniform3f(values.clone())
        return this
    }

    /** Sets a `vec4` array uniform.  */
    fun setVec4Array(name: String, values: FloatArray): Shader {
        require(values.size % 4 == 0) { "Value array length must be divisible by 4" }
        uniforms[getUniformLocation(name)] = Uniform4f(values.clone())
        return this
    }

    /** Sets a `mat2` array uniform.  */
    fun setMat2Array(name: String, values: FloatArray): Shader {
        require(values.size % 4 == 0) { "Value array length must be divisible by 4 (2x2)" }
        uniforms[getUniformLocation(name)] = UniformMatrix2f(values.clone())
        return this
    }

    /** Sets a `mat3` array uniform.  */
    fun setMat3Array(name: String, values: FloatArray): Shader {
        require(values.size % 9 == 0) { "Values array length must be divisible by 9 (3x3)" }
        uniforms[getUniformLocation(name)] = UniformMatrix3f(values.clone())
        return this
    }

    /** Sets a `mat4` uniform.  */
    fun setMat4Array(name: String, values: FloatArray): Shader {
        require(values.size % 16 == 0) { "Value array length must be divisible by 16 (4x4)" }
        uniforms[getUniformLocation(name)] = UniformMatrix4f(values.clone())
        return this
    }

    /**
     * Activates the shader. Don't call this directly unless you are doing low level OpenGL code;
     * instead, prefer [SampleRender.draw].
     */
    fun lowLevelUse() {
        // Make active shader/set uniforms
        check(programId != 0) { "Attempted to use freed shader" }
        GLES30.glUseProgram(programId)
        GLError.maybeThrowGLException("Failed to use shader program", "glUseProgram")
        GLES30.glBlendFuncSeparate(
            sourceRgbBlend.glesEnum,
            destRgbBlend.glesEnum,
            sourceAlphaBlend.glesEnum,
            destAlphaBlend.glesEnum
        )
        GLError.maybeThrowGLException("Failed to set blend mode", "glBlendFuncSeparate")
        GLES30.glDepthMask(depthWrite)
        GLError.maybeThrowGLException("Failed to set depth write mask", "glDepthMask")
        if (depthTest) {
            GLES30.glEnable(GLES30.GL_DEPTH_TEST)
            GLError.maybeThrowGLException("Failed to enable depth test", "glEnable")
        } else {
            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
            GLError.maybeThrowGLException("Failed to disable depth test", "glDisable")
        }
        try {
            // Remove all non-texture uniforms from the map after setting them, since they're stored as
            // part of the program.
            val obsoleteEntries = ArrayList<Int>(uniforms.size)
            for ((key, value) in uniforms) {
                try {
                    value.use(key)
                    if (value !is UniformTexture) {
                        obsoleteEntries.add(key)
                    }
                } catch (e: GLException) {
                    val name = uniformNames[key]
                    throw IllegalArgumentException("Error setting uniform `$name'", e)
                }
            }
            uniforms.keys.removeAll(obsoleteEntries)
        } finally {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLError.maybeLogGLError(
                Log.WARN,
                TAG,
                "Failed to set active texture",
                "glActiveTexture"
            )
        }
    }

    private interface Uniform {
        fun use(location: Int)
    }

    private class UniformTexture(val textureUnit: Int, texture: Texture) : Uniform {
        private val texture: Texture
        override fun use(location: Int) {
            check(!(texture.getTextureId() === 0)) { "Tried to draw with freed texture" }
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + textureUnit)
            GLError.maybeThrowGLException("Failed to set active texture", "glActiveTexture")
            GLES30.glBindTexture(texture.target.glesEnum, texture.getTextureId())
            GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture")
            GLES30.glUniform1i(location, textureUnit)
            GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniform1i")
        }

        init {
            this.texture = texture
        }
    }

    private class UniformInt(private val values: IntArray) : Uniform {
        override fun use(location: Int) {
            GLES30.glUniform1iv(location, values.size, values, 0)
            GLError.maybeThrowGLException("Failed to set shader uniform 1i", "glUniform1iv")
        }
    }

    private class Uniform1f(private val values: FloatArray) : Uniform {
        override fun use(location: Int) {
            GLES30.glUniform1fv(location, values.size, values, 0)
            GLError.maybeThrowGLException("Failed to set shader uniform 1f", "glUniform1fv")
        }
    }

    private class Uniform2f(private val values: FloatArray) : Uniform {
        override fun use(location: Int) {
            GLES30.glUniform2fv(location, values.size / 2, values, 0)
            GLError.maybeThrowGLException("Failed to set shader uniform 2f", "glUniform2fv")
        }
    }

    private class Uniform3f(private val values: FloatArray) : Uniform {
        override fun use(location: Int) {
            GLES30.glUniform3fv(location, values.size / 3, values, 0)
            GLError.maybeThrowGLException("Failed to set shader uniform 3f", "glUniform3fv")
        }
    }

    private class Uniform4f(private val values: FloatArray) : Uniform {
        override fun use(location: Int) {
            GLES30.glUniform4fv(location, values.size / 4, values, 0)
            GLError.maybeThrowGLException("Failed to set shader uniform 4f", "glUniform4fv")
        }
    }

    private class UniformMatrix2f(private val values: FloatArray) : Uniform {
        override fun use(location: Int) {
            GLES30.glUniformMatrix2fv(location, values.size / 4,  /*transpose=*/false, values, 0)
            GLError.maybeThrowGLException(
                "Failed to set shader uniform matrix 2f",
                "glUniformMatrix2fv"
            )
        }
    }

    private class UniformMatrix3f(private val values: FloatArray) : Uniform {
        override fun use(location: Int) {
            GLES30.glUniformMatrix3fv(location, values.size / 9,  /*transpose=*/false, values, 0)
            GLError.maybeThrowGLException(
                "Failed to set shader uniform matrix 3f",
                "glUniformMatrix3fv"
            )
        }
    }

    private class UniformMatrix4f(private val values: FloatArray) : Uniform {
        override fun use(location: Int) {
            GLES30.glUniformMatrix4fv(location, values.size / 16,  /*transpose=*/false, values, 0)
            GLError.maybeThrowGLException(
                "Failed to set shader uniform matrix 4f",
                "glUniformMatrix4fv"
            )
        }
    }

    private fun getUniformLocation(name: String): Int {
        val locationObject = uniformLocations[name]
        if (locationObject != null) {
            return locationObject
        }
        val location = GLES30.glGetUniformLocation(programId, name)
        GLError.maybeThrowGLException("Failed to find uniform", "glGetUniformLocation")
        require(location != -1) { "Shader uniform does not exist: $name" }
        uniformLocations[name] = Integer.valueOf(location)
        uniformNames[Integer.valueOf(location)] = name
        return location
    }

    companion object {
        private val TAG = Shader::class.java.simpleName

        /**
         * Creates a [Shader] from the given asset file names.
         *
         *
         * The file contents are interpreted as UTF-8 text.
         *
         * @param defines A map of shader precompiler symbols to be defined with the given names and
         * values
         */
        @Throws(IOException::class)
        fun createFromAssets(
            render: SampleRender,
            vertexShaderFileName: String?,
            fragmentShaderFileName: String?,
            defines: Map<String, String>?
        ): Shader {
            val assets: AssetManager = render.assets
            return Shader(
                render,
                inputStreamToString(assets.open(vertexShaderFileName!!)),
                inputStreamToString(assets.open(fragmentShaderFileName!!)),
                defines
            )
        }

        private fun createShader(type: Int, code: String): Int {
            val shaderId = GLES30.glCreateShader(type)
            GLError.maybeThrowGLException("Shader creation failed", "glCreateShader")
            GLES30.glShaderSource(shaderId, code)
            GLError.maybeThrowGLException("Shader source failed", "glShaderSource")
            GLES30.glCompileShader(shaderId)
            GLError.maybeThrowGLException("Shader compilation failed", "glCompileShader")
            val compileStatus = IntArray(1)
            GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == GLES30.GL_FALSE) {
                val infoLog = GLES30.glGetShaderInfoLog(shaderId)
                GLError.maybeLogGLError(
                    Log.WARN, TAG, "Failed to retrieve shader info log", "glGetShaderInfoLog"
                )
                GLES30.glDeleteShader(shaderId)
                GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free shader", "glDeleteShader")
                throw GLException(0, "Shader compilation failed: $infoLog")
            }
            return shaderId
        }

        private fun createShaderDefinesCode(defines: Map<String, String>?): String {
            if (defines == null) {
                return ""
            }
            val builder = StringBuilder()
            for ((key, value) in defines) {
                builder.append(
                    """#define $key $value
"""
                )
            }
            return builder.toString()
        }

        private fun insertShaderDefinesCode(sourceCode: String, definesCode: String): String {
            val result = sourceCode.replace(
                "(?m)^(\\s*#\\s*version\\s+.*)$".toRegex(), """
     $1
     ${Matcher.quoteReplacement(definesCode)}
     """.trimIndent()
            )
            return if (result == sourceCode) {
                // No #version specified, so just prepend source
                definesCode + sourceCode
            } else result
        }

        @Throws(IOException::class)
        private fun inputStreamToString(stream: InputStream): String {
            val reader = InputStreamReader(stream, StandardCharsets.UTF_8.name())
            val buffer = CharArray(1024 * 4)
            val builder = StringBuilder()
            var amount = 0
            while (reader.read(buffer).also { amount = it } != -1) {
                builder.append(buffer, 0, amount)
            }
            reader.close()
            return builder.toString()
        }
    }

    /**
     * Constructs a [Shader] given the shader code.
     *
     * @param defines A map of shader precompiler symbols to be defined with the given names and
     * values
     */
    init {
        var vertexShaderId = 0
        var fragmentShaderId = 0
        val definesCode = createShaderDefinesCode(defines)
        try {
            vertexShaderId = createShader(
                GLES30.GL_VERTEX_SHADER, insertShaderDefinesCode(vertexShaderCode, definesCode)
            )
            fragmentShaderId = createShader(
                GLES30.GL_FRAGMENT_SHADER, insertShaderDefinesCode(fragmentShaderCode, definesCode)
            )
            programId = GLES30.glCreateProgram()
            GLError.maybeThrowGLException("Shader program creation failed", "glCreateProgram")
            GLES30.glAttachShader(programId, vertexShaderId)
            GLError.maybeThrowGLException("Failed to attach vertex shader", "glAttachShader")
            GLES30.glAttachShader(programId, fragmentShaderId)
            GLError.maybeThrowGLException("Failed to attach fragment shader", "glAttachShader")
            GLES30.glLinkProgram(programId)
            GLError.maybeThrowGLException("Failed to link shader program", "glLinkProgram")
            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == GLES30.GL_FALSE) {
                val infoLog = GLES30.glGetProgramInfoLog(programId)
                GLError.maybeLogGLError(
                    Log.WARN,
                    TAG,
                    "Failed to retrieve shader program info log",
                    "glGetProgramInfoLog"
                )
                throw GLException(0, "Shader link failed: $infoLog")
            }
        } catch (t: Throwable) {
            close()
            throw t
        } finally {
            // Shader objects can be flagged for deletion immediately after program creation.
            if (vertexShaderId != 0) {
                GLES30.glDeleteShader(vertexShaderId)
                GLError.maybeLogGLError(
                    Log.WARN,
                    TAG,
                    "Failed to free vertex shader",
                    "glDeleteShader"
                )
            }
            if (fragmentShaderId != 0) {
                GLES30.glDeleteShader(fragmentShaderId)
                GLError.maybeLogGLError(
                    Log.WARN,
                    TAG,
                    "Failed to free fragment shader",
                    "glDeleteShader"
                )
            }
        }
    }
}