package local.luke.power.render;

import java.nio.ByteBuffer;

/** Native geometry plus water and neutral-light endpoints, still 32 bytes per vertex. */
public final class TerrainVertexColors {
  public static final int NATIVE_STRIDE = 28, STRIDE = 32, COLOR = 20, WATER_COLOR = 28, LIGHT_META = 27;
  private TerrainVertexColors() {}

  public static int outputSize(int bytes) {
    if (bytes < 0 || bytes % NATIVE_STRIDE != 0) throw new IllegalArgumentException("Invalid terrain vertex data");
    return Math.multiplyExact(bytes / NATIVE_STRIDE, STRIDE);
  }

  public static boolean compatible(ByteBuffer first, ByteBuffer second) {
    return compatible(first, NATIVE_STRIDE, second);
  }

  private static boolean compatible(ByteBuffer first, int stride, ByteBuffer second) {
    if (first.limit() % stride != 0 || second.limit() % NATIVE_STRIDE != 0
        || first.limit() / stride != second.limit() / NATIVE_STRIDE) return false;
    boolean reverse = first.order() != second.order();
    for (int p = 0, q = 0; p < first.limit(); p += stride, q += NATIVE_STRIDE) {
      // Byte27 is native normal padding, repurposed as metadata after the first pass.
      for (int j = 0; j < COLOR; j += 4) {
        int value = second.getInt(q + j);
        if (first.getInt(p + j) != (reverse ? Integer.reverseBytes(value) : value)) return false;
      }
      for (int j = 23; j < 27; j++) if (first.get(p + j) != second.get(q + j)) return false;
    }
    return true;
  }

  /** Start with exact native color and no held-light endpoint. Inputs retain their cursors. */
  public static boolean pack(ByteBuffer nativeData, ByteBuffer clearData, ByteBuffer output) {
    int size = outputSize(nativeData.limit());
    output.clear(); output.limit(size);
    for (int p = 0, q = 0; p < nativeData.limit(); p += NATIVE_STRIDE, q += STRIDE) {
      output.put(q, nativeData, p, NATIVE_STRIDE);
      output.put(q + LIGHT_META, (byte)0);
      output.putInt(q + WATER_COLOR, 0);
    }
    return clearData == null || water(output, clearData);
  }

  /** A failed secondary pass cannot attach colors to a different mesh. */
  public static boolean water(ByteBuffer packed, ByteBuffer clearData) {
    if (!compatible(packed, STRIDE, clearData)) return false;
    for (int p = 0, q = 0; p < packed.limit(); p += STRIDE, q += NATIVE_STRIDE) {
      boolean different = false;
      for (int c = 0; c < 3; c++) different |= packed.get(p + COLOR + c) != clearData.get(q + COLOR + c);
      if (!different) continue;
      // Water keeps RGB888; padding and the duplicate alpha store RGB555 neutral light.
      packed.put(p + LIGHT_META, (byte)0x80); packed.put(p + 31, (byte)0);
      for (int c = 0; c < 3; c++) packed.put(p + WATER_COLOR + c, clearData.get(q + COLOR + c));
    }
    return true;
  }

  public static boolean neutral(ByteBuffer packed, ByteBuffer neutralData) {
    if (!compatible(packed, STRIDE, neutralData)) return false;
    for (int p = 0, q = 0; p < packed.limit(); p += STRIDE, q += NATIVE_STRIDE)
      neutral(packed, p, neutralData.get(q + 20) & 255, neutralData.get(q + 21) & 255, neutralData.get(q + 22) & 255);
    return true;
  }

  public static void reuseNative(ByteBuffer packed) {
    for (int p = 0; p < packed.limit(); p += STRIDE)
      neutral(packed, p, packed.get(p + 20) & 255, packed.get(p + 21) & 255, packed.get(p + 22) & 255);
  }

  public static void reuseWater(ByteBuffer packed) {
    for (int p = 0; p < packed.limit(); p += STRIDE) {
      int color = separateWater(packed, p) ? WATER_COLOR : COLOR;
      neutral(packed, p, packed.get(p + color) & 255, packed.get(p + color + 1) & 255, packed.get(p + color + 2) & 255);
    }
  }

  public static void clearNeutral(ByteBuffer packed) {
    for (int p = 0; p < packed.limit(); p += STRIDE) neutral(packed, p, 0, 0, 0);
  }

  private static void neutral(ByteBuffer packed, int p, int r, int g, int b) {
    if (separateWater(packed, p)) {
      int bits = 0x8000 | quantize(r) << 10 | quantize(g) << 5 | quantize(b);
      packed.put(p + LIGHT_META, (byte)(bits >>> 8)); packed.put(p + 31, (byte)bits);
    } else {
      packed.put(p + WATER_COLOR, (byte)r); packed.put(p + WATER_COLOR + 1, (byte)g);
      packed.put(p + WATER_COLOR + 2, (byte)b);
    }
  }

  private static boolean separateWater(ByteBuffer packed, int p) { return (packed.get(p + LIGHT_META) & 128) != 0; }
  static int quantize(int channel) { return (channel * 31 + 127) / 255; }
}
