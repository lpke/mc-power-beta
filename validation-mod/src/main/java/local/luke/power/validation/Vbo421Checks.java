package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.field;
import java.nio.IntBuffer;
import net.mine_diver.smoothbeta.client.render.VboPool;
import net.mine_diver.smoothbeta.client.render.VertexFormat;
import net.mine_diver.smoothbeta.client.render.VertexFormats;
import net.mine_diver.smoothbeta.client.render.gl.VertexBuffer;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

public final class Vbo421Checks {
  private Vbo421Checks() {}
  public static void run(Minecraft mc) throws Exception {
    int oldArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
    int oldBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
    var pool = new VboPool(VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
    try {
      var mesh = new VertexBuffer(pool);
      mesh.upload(BufferUtils.createByteBuffer(4 * 28));
      var pos = (VboPool.Pos)field(mesh, "poolPos");
      for (int i = 0; i < 40000; i++) pool.upload(VertexFormat.DrawMode.QUADS, pos);
      var before = (IntBuffer)field(pool, "bufferIndirect");
      check(before.position() == 40000 * 5, "draw commands were dropped");
      mesh.upload(BufferUtils.createByteBuffer(131072 * 28));
      var commands = (IntBuffer)field(pool, "bufferIndirect");
      check(commands == before && commands.capacity() * 4 < 2 * 1024 * 1024,
          "vertex growth replaced or inflated command storage");
      var read = commands.duplicate(); read.flip();
      for (int i = 0; i < 40000; i++) {
        check(read.get() == 6 && read.get() == 1 && read.get() == 0
            && read.get() == 0 && read.get() == i, "native command words changed");
      }
      check(GL11.glGetError() == 0, "native VBO growth raised a GL error");
      log("VBO 421 PASS: 40000 native commands retained through vertex growth; commandBytes="
          + commands.capacity() * 4 + " vertexCapacity=" + field(pool, "capacity"));
    } finally {
      pool.close(); GL30.glDeleteVertexArrays((int)field(pool, "vertexArrayId"));
      GL30.glBindVertexArray(oldArray); GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldBuffer);
    }
  }
}
