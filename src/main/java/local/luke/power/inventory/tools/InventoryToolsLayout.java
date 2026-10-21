package local.luke.power.inventory.tools;

/** GUI pixels shared by native header widgets, modal rendering and pointer ownership. */
public final class InventoryToolsLayout {
  public static final int BUTTON_SIZE = 10, BUTTON_PITCH = 12;
  public static final int MODAL_ROW_TOP = 8, MODAL_BACK_TOP = MODAL_ROW_TOP + 9 * 18 + 8;
  public record Rect(int x, int y, int width, int height) {
    public boolean contains(int px, int py) { return px >= x && px < x + width && py >= y && py < y + height; }
  }
  public record Header(Rect sort, Rect second, Rect recipe) {}
  private InventoryToolsLayout() {}
  public static Header header(int width, int height, int panelWidth, int panelHeight, int offset,
                              boolean sort, boolean second, boolean recipe) {
    int right = (width + panelWidth) / 2 + offset - 7;
    int top = (height - panelHeight) / 2 + 5;
    return new Header(sort ? button(right, top, second ? 1 : 0) : null,
        second ? button(right, top, 0) : null,
        recipe ? button(right, top, (sort ? 1 : 0) + (second ? 1 : 0)) : null);
  }
  private static Rect button(int right, int top, int index) {
    return new Rect(right - BUTTON_SIZE - index * BUTTON_PITCH, top, BUTTON_SIZE, BUTTON_SIZE);
  }
  public static Rect sort(int width, int height, int panelWidth, int panelHeight, int offset, boolean second) {
    return header(width, height, panelWidth, panelHeight, offset, true, second, false).sort();
  }
  public static Rect second(int width, int height, int panelWidth, int panelHeight, int offset) {
    return header(width, height, panelWidth, panelHeight, offset, false, true, false).second();
  }
  public static Rect recipe(int width, int height, int panelWidth, int panelHeight, int offset, boolean second) {
    return header(width, height, panelWidth, panelHeight, offset, true, second, true).recipe();
  }
  public static Rect modal(int width, int height, int offset) {
    int span = Math.min(258, width - 16), tall = Math.min(MODAL_BACK_TOP + 22, height - 8);
    return new Rect(Math.max(8, Math.min(width - span - 8, width / 2 + offset - span / 2)), Math.max(4, (height - tall) / 2), span, tall);
  }
}
