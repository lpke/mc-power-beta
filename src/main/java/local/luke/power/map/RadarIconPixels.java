package local.luke.power.map;

import java.nio.ByteBuffer;

/** Bounded one-pixel black silhouette dilation in the eight neighboring directions. */
final class RadarIconPixels {
  private RadarIconPixels() {}
  static void outline(ByteBuffer source, ByteBuffer target, int side) {
    for (int y = 0; y < side; y++) for (int x = 0; x < side; x++) {
      int index = (y * side + x) * 4, alpha = source.get(index + 3) & 255;
      if (alpha != 0) {
        for (int channel = 0; channel < 3; channel++) {
          int component = source.get(index + channel) & 255;
          target.put(index + channel, (byte)Math.min(255, (component * 255 + alpha / 2) / alpha));
        }
        target.put(index + 3, (byte)alpha);
      } else {
        int outlineAlpha = 0;
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
          int nx = x + dx, ny = y + dy;
          if (nx >= 0 && ny >= 0 && nx < side && ny < side)
            outlineAlpha = Math.max(outlineAlpha, source.get((ny * side + nx) * 4 + 3) & 255);
        }
        target.put(index, (byte)0); target.put(index + 1, (byte)0); target.put(index + 2, (byte)0);
        target.put(index + 3, (byte)outlineAlpha);
      }
    }
    target.position(0); target.limit(side * side * 4);
  }
}
