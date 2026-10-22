package local.luke.power.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/** Five integers per submitted mesh, independent of the number of terrain vertices. */
public final class TerrainDrawCommands {
  public static final int WORDS = 5;
  private static final int INITIAL_WORDS = 1024 * WORDS;
  private TerrainDrawCommands() {}

  public static IntBuffer initial() { return allocate(INITIAL_WORDS); }

  public static IntBuffer room(IntBuffer commands) {
    if (commands.remaining() >= WORDS) return commands;
    int capacity = Math.max(Math.addExact(commands.position(), WORDS),
        Math.multiplyExact(commands.capacity(), 2));
    var next = allocate(capacity);
    next.put(commands.duplicate().flip());
    return next;
  }

  private static IntBuffer allocate(int words) {
    return ByteBuffer.allocateDirect(Math.multiplyExact(words, Integer.BYTES))
        .order(ByteOrder.nativeOrder()).asIntBuffer();
  }
}
