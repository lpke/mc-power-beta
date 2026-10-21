package local.luke.power.inventory.tools;

/** GUI pixels shared by native header widgets, modal rendering and pointer ownership. */
public final class InventoryToolsLayout {
  public record Rect(int x, int y, int width, int height) {
    public boolean contains(int px, int py) { return px >= x && px < x + width && py >= y && py < y + height; }
  }
  private InventoryToolsLayout() {}
  public static Rect sort(int width, int height, int panelWidth, int panelHeight, int offset, boolean second) {
    int right = (width + panelWidth) / 2 + offset;
    return new Rect(right - (second ? 86 : 34), Math.max(2, (height - panelHeight) / 2 - 20), 34, 18);
  }
  public static Rect second(int width, int height, int panelWidth, int panelHeight, int offset) {
    int right = (width + panelWidth) / 2 + offset;
    return new Rect(right - 50, Math.max(2, (height - panelHeight) / 2 - 20), 50, 18);
  }
  public static Rect modal(int width, int height, int offset) {
    int span = Math.min(258, width - 16), tall = Math.min(224, height - 8);
    return new Rect(Math.max(8, Math.min(width - span - 8, width / 2 + offset - span / 2)), Math.max(4, (height - tall) / 2), span, tall);
  }
}
