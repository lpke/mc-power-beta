package local.luke.power.status;

/** Nine anchors, with one shared edge for every line and bounded GUI-pixel offsets. */
public record StatusLayout(int x, int y, int alignment) {
  public static StatusLayout at(int width, int height, int contentWidth, int rows, StatusSettings s) {
    int column = s.position % 3, row = s.position / 3;
    int contentHeight = rows * 10;
    int x = column == 0 ? 8 : column == 1 ? (width - contentWidth) / 2 : width - contentWidth - 8;
    int y = row == 0 ? 8 : row == 1 ? (height - contentHeight) / 2 : height - contentHeight - 8;
    return new StatusLayout(Math.max(2, Math.min(width - contentWidth - 2, x + s.offsetX)),
        Math.max(2, Math.min(height - contentHeight - 2, y + s.offsetY)), column);
  }
  public int lineX(int contentWidth, int lineWidth) {
    return x + (alignment == 0 ? 0 : alignment == 1 ? (contentWidth - lineWidth) / 2 : contentWidth - lineWidth);
  }
}
