package com.example.festunavigator.common.samplerender

import android.opengl.GLES30
import android.util.Log
import java.io.Closeable


/** A framebuffer associated with a texture.  */
class Framebuffer(render: SampleRender?, width: Int, height: Int) : Closeable {
    private val framebufferId = intArrayOf(0)
    private var colorTexture: Texture? = null
    private var depthTexture: Texture? = null

    /** Returns the width of the framebuffer.  */
    var width = -1
        private set

    /** Returns the height of the framebuffer.  */
    var height = -1
        private set

    override fun close() {
        if (framebufferId[0] != 0) {
            GLES30.glDeleteFramebuffers(1, framebufferId, 0)
            GLError.maybeLogGLError(
                Log.WARN,
                TAG,
                "Failed to free framebuffer",
                "glDeleteFramebuffers"
            )
            framebufferId[0] = 0
        }
        colorTexture!!.close()
        depthTexture!!.close()
    }

    /** Resizes the framebuffer to the given dimensions.  */
    fun resize(width: Int, height: Int) {
        if (this.width == width && this.height == height) {
            return
        }
        this.width = width
        this.height = height

        // Color texture
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, colorTexture!!.getTextureId())
        GLError.maybeThrowGLException("Failed to bind color texture", "glBindTexture")
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,  /*level=*/
            0,
            GLES30.GL_RGBA,
            width,
            height,  /*border=*/
            0,
            GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE,  /*pixels=*/
            null
        )
        GLError.maybeThrowGLException("Failed to specify color texture format", "glTexImage2D")

        // Depth texture
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTexture!!.getTextureId())
        GLError.maybeThrowGLException("Failed to bind depth texture", "glBindTexture")
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,  /*level=*/
            0,
            GLES30.GL_DEPTH_COMPONENT32F,
            width,
            height,  /*border=*/
            0,
            GLES30.GL_DEPTH_COMPONENT,
            GLES30.GL_FLOAT,  /*pixels=*/
            null
        )
        GLError.maybeThrowGLException("Failed to specify depth texture format", "glTexImage2D")
    }

    /** Returns the color texture associated with this framebuffer.  */
    fun getColorTexture(): Texture? {
        return colorTexture
    }

    /** Returns the depth texture associated with this framebuffer.  */
    fun getDepthTexture(): Texture? {
        return depthTexture
    }

    /* package-private */
    fun getFramebufferId(): Int {
        return framebufferId[0]
    }

    companion object {
        private val TAG = Framebuffer::class.java.simpleName
    }

    /**
     * Constructs a [Framebuffer] which renders internally to a texture.
     *
     *
     * In order to render to the [Framebuffer], use [SampleRender.draw].
     */
    init {
        try {
            colorTexture = Texture(
                render,
                Texture.Target.TEXTURE_2D,
                Texture.WrapMode.CLAMP_TO_EDGE,  /*useMipmaps=*/
                false
            )
            depthTexture = Texture(
                render,
                Texture.Target.TEXTURE_2D,
                Texture.WrapMode.CLAMP_TO_EDGE,  /*useMipmaps=*/
                false
            )

            // Set parameters of the depth texture so that it's readable by shaders.
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTexture!!.getTextureId())
            GLError.maybeThrowGLException("Failed to bind depth texture", "glBindTexture")
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_COMPARE_MODE,
                GLES30.GL_NONE
            )
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_NEAREST
            )
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_NEAREST
            )
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")

            // Set initial dimensions.
            resize(width, height)

            // Create framebuffer object and bind to the color and depth textures.
            GLES30.glGenFramebuffers(1, framebufferId, 0)
            GLError.maybeThrowGLException("Framebuffer creation failed", "glGenFramebuffers")
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId[0])
            GLError.maybeThrowGLException("Failed to bind framebuffer", "glBindFramebuffer")
            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER,
                GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D,
                colorTexture!!.getTextureId(),  /*level=*/
                0
            )
            GLError.maybeThrowGLException(
                "Failed to bind color texture to framebuffer", "glFramebufferTexture2D"
            )
            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER,
                GLES30.GL_DEPTH_ATTACHMENT,
                GLES30.GL_TEXTURE_2D,
                depthTexture!!.getTextureId(),  /*level=*/
                0
            )
            GLError.maybeThrowGLException(
                "Failed to bind depth texture to framebuffer", "glFramebufferTexture2D"
            )
            val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
            check(status == GLES30.GL_FRAMEBUFFER_COMPLETE) { "Framebuffer construction not complete: code $status" }
        } catch (t: Throwable) {
            close()
            throw t
        }
    }
}