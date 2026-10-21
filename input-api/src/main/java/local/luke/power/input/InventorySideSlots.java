package local.luke.power.input;

/** Shared native-pixel layout for the offhand and Creative destroy extension. */
public final class InventorySideSlots {
  public static final int WIDTH = 22, OFFHAND_X = -17, OFFHAND_Y = 142;
  public static final int DESTROY_Y = 120;
  private InventorySideSlots() {}
  public static int top(boolean destroyAbove) { return destroyAbove ? 115 : 137; }
  public static boolean contains(int px, int py, int x, int y, int height, boolean destroyAbove) {
    return px >= x - WIDTH && px < x + 4 && py >= y + top(destroyAbove) && py < y + height;
  }
  @FunctionalInterface public interface Fill { void draw(int left, int top, int right, int bottom, int color); }
  public static void frame(Fill fill, int x, int y, int height, boolean destroyAbove) {
    int left = x - WIDTH, top = y + top(destroyAbove), bottom = y + height;
    fill.draw(left + 1, top, x + 4, bottom, 0xff000000);
    fill.draw(left, top + 1, x + 4, bottom - 1, 0xff000000);
    fill.draw(left + 1, top + 1, x + 4, bottom - 1, 0xffc6c6c6);
    fill.draw(left + 2, top + 1, x + 3, top + 3, 0xffffffff);
    fill.draw(left + 1, top + 2, left + 3, bottom - 3, 0xffffffff);
    fill.draw(left + 2, bottom - 3, x + 4, bottom - 1, 0xff555555);
    fill.draw(left + 1, bottom - 1, x + 4, bottom, 0xff000000);
  }
}
