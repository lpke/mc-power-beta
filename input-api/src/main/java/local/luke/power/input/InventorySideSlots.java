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
  public static final String FRAME_TEXTURE = "/assets/powerbeta/textures/gui/offhand_frame.png";
  public static final String SLOT_TEXTURE = "/assets/powerbeta/textures/gui/offhand_slot.png";
  @FunctionalInterface public interface Texture {
    void draw(int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight);
  }
  public static void frame(Texture texture, int x, int y, int height, boolean destroyAbove) {
    frame(texture, x, y, height, OFFHAND_Y, destroyAbove);
  }
  public static void frame(Texture texture, int x, int y, int height, int hotbarY, boolean destroyAbove) {
    int left = x - WIDTH, top = y + top(hotbarY, destroyAbove), bottom = y + height;
    texture.draw(left, top, 26, 3, 0, 0, 26, 3);
    texture.draw(left, top + 3, 26, bottom - top - 6, 0, 3, 26, 1);
    texture.draw(left, bottom - 3, 26, 3, 0, 4, 26, 3);
  }
}
