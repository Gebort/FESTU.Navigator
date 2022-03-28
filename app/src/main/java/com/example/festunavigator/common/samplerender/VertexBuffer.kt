package com.example.festunavigator.common.samplerender

import android.opengl.GLES30
import java.io.Closeable
import java.nio.FloatBuffer


/**
 * A list of vertex attribute data stored GPU-side.
 *
 *
 * One or more [VertexBuffer]s are used when constructing a [Mesh] to describe vertex
 * attribute data; for example, local coordinates, texture coordinates, vertex normals, etc.
 *
 * @see [glVertexAttribPointer](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glVertexAttribPointer.xhtml)
 */
class VertexBuffer(render: SampleRender?, numberOfEntriesPerVertex: Int, entries: FloatBuffer?) :
    Closeable {
    private val buffer: GpuBuffer

    /* package-private */
    val numberOfEntriesPerVertex: Int

    /**
     * Populate with new data.
     *
     *
     * The entire buffer is replaced by the contents of the *direct* buffer `entries`
     * starting from the beginning of the buffer, not the current cursor position. The cursor will be
     * left in an undefined position after this function returns.
     *
     *
     * The GPU buffer is reallocated automatically if necessary.
     *
     *
     * The `entries` buffer may be null, in which case the buffer will become empty.
     * Otherwise, the size of `entries` must be divisible by the number of entries per vertex
     * specified during construction.
     */
    fun set(entries: FloatBuffer?) {
        if (entries != null && entries.limit() % numberOfEntriesPerVertex != 0) {
            throw IllegalArgumentException(
                "If non-null, vertex buffer data must be divisible by the number of data points per"
                        + " vertex"
            )
        }
        buffer.set(entries)
    }

    override fun close() {
        buffer.free()
    }

    /* package-private */
    val bufferId: Int
        get() = buffer.getBufferId()

    /* package-private */
    val numberOfVertices: Int
        get() = buffer.size / numberOfEntriesPerVertex

    /**
     * Construct a [VertexBuffer] populated with initial data.
     *
     *
     * The GPU buffer will be filled with the data in the *direct* buffer `entries`,
     * starting from the beginning of the buffer (not the current cursor position). The cursor will be
     * left in an undefined position after this function returns.
     *
     *
     * The number of vertices in the buffer can be expressed as `entries.limit() /
     * numberOfEntriesPerVertex`. Thus, The size of the buffer must be divisible by `numberOfEntriesPerVertex`.
     *
     *
     * The `entries` buffer may be null, in which case an empty buffer is constructed
     * instead.
     */
    init {
        if (entries != null && entries.limit() % numberOfEntriesPerVertex != 0) {
            throw IllegalArgumentException(
                (
                        "If non-null, vertex buffer data must be divisible by the number of data points per"
                                + " vertex")
            )
        }
        this.numberOfEntriesPerVertex = numberOfEntriesPerVertex
        buffer = GpuBuffer(GLES30.GL_ARRAY_BUFFER, GpuBuffer.FLOAT_SIZE, entries)
    }
}
