package com.example.festunavigator.common.samplerender

import android.content.res.AssetManager
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


/** A SampleRender context.  */
class SampleRender(
    glSurfaceView: GLSurfaceView, renderer: Renderer, /* package-private */
    val assets: AssetManager
) {
    private var viewportWidth = 1
    private var viewportHeight = 1

    /** Draw a [Mesh] with the specified [Shader].  */
    fun draw(mesh: Mesh, shader: Shader) {
        draw(mesh, shader,  /*framebuffer=*/null)
    }

    /**
     * Draw a [Mesh] with the specified [Shader] to the given [Framebuffer].
     *
     *
     * The `framebuffer` argument may be null, in which case the default framebuffer is used.
     */
    fun draw(mesh: Mesh, shader: Shader, framebuffer: Framebuffer?) {
        useFramebuffer(framebuffer)
        shader.lowLevelUse()
        mesh.lowLevelDraw()
    }

    /**
     * Clear the given framebuffer.
     *
     *
     * The `framebuffer` argument may be null, in which case the default framebuffer is
     * cleared.
     */
    fun clear(framebuffer: Framebuffer?, r: Float, g: Float, b: Float, a: Float) {
        useFramebuffer(framebuffer)
        GLES30.glClearColor(r, g, b, a)
        GLError.maybeThrowGLException("Failed to set clear color", "glClearColor")
        GLES30.glDepthMask(true)
        GLError.maybeThrowGLException("Failed to set depth write mask", "glDepthMask")
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        GLError.maybeThrowGLException("Failed to clear framebuffer", "glClear")
    }

    /** Interface to be implemented for rendering callbacks.  */
    interface Renderer {
        /**
         * Called by [SampleRender] when the GL render surface is created.
         *
         *
         * See [GLSurfaceView.Renderer.onSurfaceCreated].
         */
        fun onSurfaceCreated(render: SampleRender?)

        /**
         * Called by [SampleRender] when the GL render surface dimensions are changed.
         *
         *
         * See [GLSurfaceView.Renderer.onSurfaceChanged].
         */
        fun onSurfaceChanged(render: SampleRender?, width: Int, height: Int)

        /**
         * Called by [SampleRender] when a GL frame is to be rendered.
         *
         *
         * See [GLSurfaceView.Renderer.onDrawFrame].
         */
        fun onDrawFrame(render: SampleRender?)
    }

    private fun useFramebuffer(framebuffer: Framebuffer?) {
        val framebufferId: Int
        val viewportWidth: Int
        val viewportHeight: Int
        if (framebuffer == null) {
            framebufferId = 0
            viewportWidth = this.viewportWidth
            viewportHeight = this.viewportHeight
        } else {
            framebufferId = framebuffer.getFramebufferId()
            viewportWidth = framebuffer.width
            viewportHeight = framebuffer.height
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId)
        GLError.maybeThrowGLException("Failed to bind framebuffer", "glBindFramebuffer")
        GLES30.glViewport(0, 0, viewportWidth, viewportHeight)
        GLError.maybeThrowGLException("Failed to set viewport dimensions", "glViewport")
    }

    companion object {
        private val TAG = SampleRender::class.java.simpleName
    }

    /**
     * Constructs a SampleRender object and instantiates GLSurfaceView parameters.
     *
     * @param glSurfaceView Android GLSurfaceView
     * @param renderer Renderer implementation to receive callbacks
     * @param assetManager AssetManager for loading Android resources
     */
    init {
        glSurfaceView.preserveEGLContextOnPause = true
        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        glSurfaceView.setRenderer(
            object : GLSurfaceView.Renderer {
                override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
                    GLES30.glEnable(GLES30.GL_BLEND)
                    GLError.maybeThrowGLException("Failed to enable blending", "glEnable")
                    renderer.onSurfaceCreated(this@SampleRender)
                }

                override fun onSurfaceChanged(gl: GL10, w: Int, h: Int) {
                    viewportWidth = w
                    viewportHeight = h
                    renderer.onSurfaceChanged(this@SampleRender, w, h)
                }

                override fun onDrawFrame(gl: GL10) {
                    clear( /*framebuffer=*/null, 0f, 0f, 0f, 1f)
                    renderer.onDrawFrame(this@SampleRender)
                }
            })
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        glSurfaceView.setWillNotDraw(false)
    }
}