package local.luke.power.creative.inventory;

/** The extra catalogue row grows upward without moving the player's hotbar. */
public final class CreativeGrid {
  public static final int ROWS = 8, CAPACITY = ROWS * 8, EXTRA_HEIGHT = 20, SCROLL_TRAVEL = 127;
  public static final int DESTROY_WIDTH = 22, DESTROY_TOP = 137;
  private CreativeGrid() {}
  public static int tabTop(int baseY) { return Math.max(0, baseY - EXTRA_HEIGHT - 21); }
  public static int tabHeight(int baseY) { return Math.min(24, baseY - EXTRA_HEIGHT + 3 - tabTop(baseY)); }
  public static int maxScroll(int items) { return Math.max(0, (items - CAPACITY + 7) / 8) * 8; }

  /** Category and view tabs handle their own clicks before checking the body. */
  public static boolean outsideBody(int mouseX, int mouseY, int x, int y,
                                    int width, int height, boolean destroySlot) {
    if (mouseX >= x && mouseX < x + width
        && mouseY >= y - EXTRA_HEIGHT && mouseY < y + height) return false;
    if (destroySlot && mouseX >= x - DESTROY_WIDTH && mouseX < x
        && mouseY >= y + DESTROY_TOP && mouseY < y + height) return false;
    return true;
  }
}
