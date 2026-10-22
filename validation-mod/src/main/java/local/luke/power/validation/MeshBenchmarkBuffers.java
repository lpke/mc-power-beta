package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.field;
import net.mine_diver.smoothbeta.client.render.VboPool;
import net.mine_diver.smoothbeta.client.render.VertexFormats;
import net.mine_diver.smoothbeta.client.render.gl.VertexBuffer;
import net.minecraft.class_66;
import org.lwjgl.opengl.*;

/** Byte readback and allocator stress must never touch the live world's shared VBO. */
final class MeshBenchmarkBuffers implements AutoCloseable {
  private final int previousArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
  private final int previousBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
  private final VboPool pool = new VboPool(VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);

  void attach(class_66 mesh) throws Exception {
    // Newly constructed mesh buffers have no allocations in the live pool yet.
    var buffers = (VertexBuffer[])field(mesh, "smoothbeta_buffers");
    for (int i = 0; i < buffers.length; i++) buffers[i] = new VertexBuffer(pool);
  }

  @Override public void close() throws Exception {
    pool.close();
    GL30.glDeleteVertexArrays((int)field(pool, "vertexArrayId"));
    GL30.glBindVertexArray(previousArray);
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
  }
}
