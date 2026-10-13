package local.luke.power.hud;

/** Anchor the scaled block of text, then apply offsets in GUI pixels. */
public record InfoHudLayout(double x, double y, int alignment) {
  public static InfoHudLayout at(int width, int height, int textWidth, int rows, InfoHudSettings.Cluster c) {
    int column = c.position % 3, row = c.position / 3;
    double w = textWidth * c.textSize, h = rows * 10 * c.textSize;
    double x = column == 0 ? 4 : column == 1 ? (width - w) / 2 : width - w - 4;
    double y = row == 0 ? 4 : row == 1 ? (height - h) / 2 : height - h - 4;
    return new InfoHudLayout(Math.max(2, Math.min(width - w - 2, x + c.offsetX)),
        Math.max(2, Math.min(height - h - 2, y + c.offsetY)), column);
  }
  public int lineX(int width, int lineWidth) {
    return alignment == 0 ? 0 : alignment == 1 ? (width - lineWidth) / 2 : width - lineWidth;
  }
}
