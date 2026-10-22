package local.luke.power.input;

/** Shared native-pixel layout for the offhand and Creative destroy extension. */
public final class InventorySideSlots {
  public static final int WIDTH = 20, OFFHAND_X = -14, OFFHAND_Y = 142;
  public static final int SLOT_X = OFFHAND_X - 1, SLOT_SIZE = 18;
  public static final int DESTROY_Y = 120;
  public static final int FRAME_TEXTURE_WIDTH = 24, FRAME_TEXTURE_HEIGHT = 8;
  private InventorySideSlots() {}
  public static int top(boolean destroyAbove) { return top(OFFHAND_Y, destroyAbove); }
  public static int top(int hotbarY, boolean destroyAbove) { return hotbarY - (destroyAbove ? 28 : 6); }
  public static boolean containsSlot(int px, int py, int x, int y, int slotY) {
    return px >= x + SLOT_X && px < x + SLOT_X + SLOT_SIZE
        && py >= y + slotY - 1 && py < y + slotY - 1 + SLOT_SIZE;
  }
  public static boolean contains(int px, int py, int x, int y, int height, boolean destroyAbove) {
    return contains(px, py, x, y, height, OFFHAND_Y, destroyAbove);
  }
  public static boolean contains(int px, int py, int x, int y, int height, int hotbarY, boolean destroyAbove) {
    return px >= x - WIDTH && px < x - WIDTH + FRAME_TEXTURE_WIDTH
        && py >= y + top(hotbarY, destroyAbove) && py < y + height;
  }
  // Namespaced requests let StationAPI prioritize packs over bundled legacy paths.
  public static final String FRAME_TEXTURE = "powerbeta:textures/gui/offhand_frame.png";
  public static final String SLOT_TEXTURE = "powerbeta:textures/gui/offhand_slot.png";
  @FunctionalInterface public interface Texture {
    void draw(int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight);
  }
  public static void frame(Texture texture, int x, int y, int height, boolean destroyAbove) {
    frame(texture, x, y, height, OFFHAND_Y, destroyAbove);
  }
  public static void frame(Texture texture, int x, int y, int height, int hotbarY, boolean destroyAbove) {
    int left = x - WIDTH, top = y + top(hotbarY, destroyAbove), bottom = y + height;
    texture.draw(left, top, FRAME_TEXTURE_WIDTH, 3, 0, 0, FRAME_TEXTURE_WIDTH, 3);
    // Keep all four bottom rows fixed; only the middle row stretches.
    texture.draw(left, top + 3, FRAME_TEXTURE_WIDTH, bottom - top - 7, 0, 3, FRAME_TEXTURE_WIDTH, 1);
    texture.draw(left, bottom - 4, FRAME_TEXTURE_WIDTH, 4, 0, 4, FRAME_TEXTURE_WIDTH, 4);
  }
}
