package local.luke.power.light;

/** Native 7.99-pixel digit quads and advances, prepared only when the font changes. */
public final class LightGlyphs {
  private LightGlyphs() {}
  public static double[][] create(int[] advances) {
    if (advances.length != 10) throw new IllegalArgumentException("Ten digit advances are required");
    double[][] numbers = new double[16][];
    for (int level = 0; level < numbers.length; level++) {
      String text = Integer.toString(level); int width = 0;
      for (int i = 0; i < text.length(); i++) width += advances[text.charAt(i) - '0'];
      double[] vertices = new double[text.length() * 16]; int at = 0, x = -width / 2;
      for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i); int u = c % 16 * 8, v = c / 16 * 8;
        for (int corner = 0; corner < 4; corner++) {
          double dx = corner == 1 || corner == 2 ? 7.99 : 0;
          double dy = corner == 0 || corner == 1 ? 7.99 : 0;
          vertices[at++] = x + dx; vertices[at++] = -4 + dy;
          vertices[at++] = (u + dx) / 128; vertices[at++] = (v + dy) / 128;
        }
        x += advances[c - '0'];
      }
      numbers[level] = vertices;
    }
    return numbers;
  }
}
