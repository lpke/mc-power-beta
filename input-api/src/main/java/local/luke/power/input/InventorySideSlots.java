package local.luke.power.input;

/** Shared native-pixel layout for the offhand and Creative destroy extension. */
public final class InventorySideSlots {
  public static final int WIDTH = 22, OFFHAND_X = -17, OFFHAND_Y = 142;
  public static final int DESTROY_Y = 120;
  private InventorySideSlots() {}
  public static int top(boolean destroyAbove) { return top(OFFHAND_Y, destroyAbove); }
  public static int top(int hotbarY, boolean destroyAbove) { return hotbarY - (destroyAbove ? 27 : 5); }
  public static boolean contains(int px, int py, int x, int y, int height, boolean destroyAbove) {
    return contains(px, py, x, y, height, OFFHAND_Y, destroyAbove);
  }
  public static boolean contains(int px, int py, int x, int y, int height, int hotbarY, boolean destroyAbove) {
    return px >= x - WIDTH && px < x + 4 && py >= y + top(hotbarY, destroyAbove) && py < y + height;
  }
  @FunctionalInterface public interface Fill { void draw(int left, int top, int right, int bottom, int color); }
  public static void frame(Fill fill, int x, int y, int height, boolean destroyAbove) {
    frame(fill, x, y, height, OFFHAND_Y, destroyAbove);
  }
  public static void frame(Fill fill, int x, int y, int height, int hotbarY, boolean destroyAbove) {
    int left = x - WIDTH, top = y + top(hotbarY, destroyAbove), bottom = y + height;
    fill.draw(left + 1, top, x + 4, bottom, 0xffc6c6c6);
    // The upper edge joins the native panel; only the lower edge continues into it.
    fill.draw(left + 1, top, x, top + 1, 0xff000000);
    fill.draw(left, top + 1, x + 4, bottom - 1, 0xff000000);
    fill.draw(left + 1, top + 1, x + 4, bottom - 1, 0xffc6c6c6);
    // Continue the native panel's white left bevel into the horizontal extension.
    // Clearing its black edge alone leaves a gray break above the white ledge.
    fill.draw(x, top, x + 3, top + 1, 0xffffffff);
    fill.draw(left + 2, top + 1, x + 3, top + 3, 0xffffffff);
    fill.draw(left + 1, top + 2, left + 3, bottom - 3, 0xffffffff);
    fill.draw(left + 2, bottom - 3, x + 4, bottom - 1, 0xff555555);
    fill.draw(left + 1, bottom - 1, x + 4, bottom, 0xff000000);
  }
}
