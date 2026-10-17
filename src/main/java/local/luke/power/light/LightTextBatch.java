package local.luke.power.light;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Tessellator;
import org.lwjgl.opengl.GL11;

/** One native tessellation batch replaces hundreds of font display-list submissions. */
final class LightTextBatch {
  private TextRenderer font;
  private int texture;
  private double[][] glyphs;
  void prepare(TextRenderer current) {
    if (font == current && texture == current.field_2461) return;
    font = current; texture = current.field_2461;
    int[] advances = new int[10];
    for (int digit = 0; digit < 10; digit++) advances[digit] = font.getWidth(Integer.toString(digit));
    glyphs = LightGlyphs.create(advances);
  }
  void begin() {
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture); GL11.glColor4f(1, 1, 1, 1);
    Tessellator.INSTANCE.startQuads();
  }
  void number(int level, double x, double y, double z, double scale, double cosine, double sine, int color) {
    Tessellator t = Tessellator.INSTANCE; t.color(color);
    double[] vertices = glyphs[level];
    for (int i = 0; i < vertices.length; i += 4) {
      double u = -vertices[i] * scale, v = -vertices[i + 1] * scale;
      t.vertex(x + cosine * u - sine * v, y, z + sine * u + cosine * v, vertices[i + 2], vertices[i + 3]);
    }
  }
  void end() { Tessellator.INSTANCE.draw(); }
}
