package local.luke.power.map;

import java.util.Arrays;

/** Pixel-aligned atlas pieces and the circular frame's seam-aware texture coordinates. */
final class MinimapBorderGeometry {
  private float[] vertices;
  private int count;
  private MinimapBorderGeometry(int quads) { vertices = new float[quads * 16]; }

  static float[] create(int diameter, boolean circle) {
    if (diameter < 24 || diameter > 512) throw new IllegalArgumentException("Invalid map diameter");
    int sides = Math.max(32, (int)Math.ceil(Math.PI * (diameter + 8) / 32) * 4);
    var mesh = new MinimapBorderGeometry(circle ? sides : 24);
    if (circle) {
      double start = -Math.PI / 4 - (double)(16f / (diameter / 2 + 4));
      int shade = (int)((Math.PI / 4 - start) / (2 * Math.PI) * sides);
      mesh.ring(diameter, sides, start, 0, shade, false, false, 0, 73, 32);
      mesh.ring(diameter, sides, start, shade, shade + sides / 4, true, false, 138, 68, 20);
      mesh.ring(diameter, sides, start, shade + sides / 4, shade + sides / 2, true, true, 138, 68, 20);
      mesh.ring(diameter, sides, start, shade + sides / 2, sides, false, false, 0, 73, 32);
    } else {
      mesh.rect(-4, -4, 192, 97, 16, 16);
      mesh.rect(diameter - 12, -4, 192, 113, 16, 16);
      mesh.rect(-4, diameter - 12, 192, 129, 16, 16);
      mesh.rect(diameter - 12, diameter - 12, 192, 145, 16, 16);
      for (int x = 12; x < diameter - 12; x += 226) {
        int width = Math.min(226, diameter - 12 - x);
        mesh.rect(x, -4, 0, 0, width, 16);
        mesh.rect(x, diameter - 12, 0, 16, width, 16);
      }
      for (int y = 12, piece = 0; y < diameter - 12; y += 113, piece++) {
        int height = Math.min(113, diameter - 12 - y), u = 32 * (piece & 1);
        mesh.rect(-4, y, u, 97, 16, height);
        mesh.rect(diameter - 12, y, u + 16, 97, 16, height);
      }
    }
    return Arrays.copyOf(mesh.vertices, mesh.count);
  }

  private void rect(float x, float y, int u, int v, int width, int height) {
    vertex(x, y + height, u, v + height);
    vertex(x + width, y + height, u + width, v + height);
    vertex(x + width, y, u + width, v);
    vertex(x, y, u, v);
  }

  private void ring(int diameter, int sides, double start, int from, int to,
      boolean reset, boolean reverse, int textureX, int repeat, int seam) {
    float half = diameter / 2f, outer = half + 4;
    float step = (float)(Math.PI * 2 / sides * outer);
    int textureStart = reset ? reverse ? to : from : 0;
    float threshold = reverse ? seam + step : seam;
    float px = 0, py = 0, ox = 0, oy = 0, pu = 0;
    for (int i = from; i <= to; i++) {
      double angle = start + (double)i / sides * Math.PI * 2;
      double sin = Math.sin(angle), cos = Math.cos(angle);
      float x = half + (float)(half * sin), y = (float)(half * (1 - cos));
      float outerX = half + (float)(outer * sin), outerY = (float)(half - outer * cos);
      float offset = Math.abs(step * (i - textureStart)), u = textureX;
      if (offset >= threshold) { u += seam; offset -= threshold; if (offset >= repeat) offset %= repeat; }
      u += offset;
      if (i > from) {
        vertex(px, py, pu, 214); vertex(x, y, u, 214);
        vertex(outerX, outerY, u, 210); vertex(ox, oy, pu, 210);
      }
      px = x; py = y; ox = outerX; oy = outerY; pu = u;
    }
  }

  private void vertex(float x, float y, float u, float v) {
    vertices[count++] = x; vertices[count++] = y;
    vertices[count++] = u / 256; vertices[count++] = v / 256;
  }
}
