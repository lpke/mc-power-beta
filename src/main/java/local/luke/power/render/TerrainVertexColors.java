package local.luke.power.render;

import java.nio.ByteBuffer;

/** Adds one color to SmoothBeta's 28-byte vertex; position, UV, alpha and normals stay native. */
public final class TerrainVertexColors {
  public static final int NATIVE_STRIDE = 28, STRIDE = 32, COLOR = 20, WATER_COLOR = 28;
  private TerrainVertexColors() {}

  public static int outputSize(int bytes) {
    if (bytes < 0 || bytes % NATIVE_STRIDE != 0) throw new IllegalArgumentException("Invalid terrain vertex data");
    return Math.multiplyExact(bytes / NATIVE_STRIDE, STRIDE);
  }

  public static boolean compatible(ByteBuffer nativeData, ByteBuffer clearData) {
    if (nativeData.limit() != clearData.limit()) return false;
    boolean reverse = nativeData.order() != clearData.order();
    for (int p = 0; p < nativeData.limit(); p += NATIVE_STRIDE) {
      // Models may vary between calls. Never attach colors to unrelated geometry.
      for (int j = 0; j < COLOR; j += 4)
        if (nativeData.getInt(p + j) != value(clearData, p + j, reverse)) return false;
      if (nativeData.get(p + 23) != clearData.get(p + 23)
          || nativeData.getInt(p + 24) != value(clearData, p + 24, reverse)) return false;
    }
    return true;
  }

  private static int value(ByteBuffer data, int offset, boolean reverse) {
    int value = data.getInt(offset);
    return reverse ? Integer.reverseBytes(value) : value;
  }

  public static boolean pack(ByteBuffer nativeData, ByteBuffer clearData, ByteBuffer output) {
    int size = outputSize(nativeData.limit());
    boolean matches = clearData == null || compatible(nativeData, clearData);
    if (!matches) clearData = null;
    output.clear();
    output.limit(size);
    for (int p = 0, q = 0; p < nativeData.limit(); p += NATIVE_STRIDE, q += STRIDE) {
      output.put(q, nativeData, p, NATIVE_STRIDE);
      output.put(q + WATER_COLOR, clearData == null ? nativeData : clearData, p + COLOR, 4);
    }
    return matches;
  }
}
