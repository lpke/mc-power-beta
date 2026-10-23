package local.luke.power.creative.inventory;

import local.luke.power.input.InventorySideSlots;

/** The extra catalog row grows upward without moving the player's hotbar. */
public final class CreativeGrid {
  public static final int ROWS = 8, CAPACITY = ROWS * 8, EXTRA_HEIGHT = 22, ITEM_TOP = 16, TITLE_TOP = 5, SCROLL_TRAVEL = 127;
  public static final int DESTROY_WIDTH = InventorySideSlots.WIDTH, DESTROY_TOP = InventorySideSlots.top(false);
  private CreativeGrid() {}
  public static int tabTop(int baseY) { return Math.max(0, baseY - EXTRA_HEIGHT - 21); }
  public static int tabHeight(int baseY) { return Math.max(20, Math.min(24, baseY - EXTRA_HEIGHT + 3 - tabTop(baseY))); }
  public static int tabIconTop(int baseY) {
    return tabTop(baseY) + Math.max(0, Math.min(4, baseY - EXTRA_HEIGHT + 4 - tabTop(baseY) - 16));
  }
  public static int maxScroll(int items) { return Math.max(0, (items - CAPACITY + 7) / 8) * 8; }

  /** Category and view tabs handle their own clicks before checking the body. */
  public static boolean outsideBody(int mouseX, int mouseY, int x, int y,
                                    int width, int height, boolean destroySlot) {
    return outsideBody(mouseX, mouseY, x, y, width, height, destroySlot, false);
  }

  public static boolean outsideBody(int mouseX, int mouseY, int x, int y,
                                    int width, int height, boolean destroySlot, boolean offhand) {
    if (mouseX >= x && mouseX < x + width
        && mouseY >= y - EXTRA_HEIGHT && mouseY < y + height) return false;
    if ((destroySlot || offhand) && InventorySideSlots.contains(mouseX, mouseY, x, y, height,
        destroySlot && offhand)) return false;
    return true;
  }
}
