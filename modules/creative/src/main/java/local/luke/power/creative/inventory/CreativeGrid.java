package local.luke.power.creative.inventory;

/** The extra catalogue row grows upward without moving the player's hotbar. */
public final class CreativeGrid {
  public static final int ROWS = 8, CAPACITY = ROWS * 8, EXTRA_HEIGHT = 18, SCROLL_TRAVEL = 127;
  private CreativeGrid() {}
  public static int tabTop(int baseY) { return Math.max(0, baseY - EXTRA_HEIGHT - 21); }
  public static int tabHeight(int baseY) { return Math.min(24, baseY - EXTRA_HEIGHT + 3 - tabTop(baseY)); }
  public static int maxScroll(int items) { return Math.max(0, (items - CAPACITY + 7) / 8) * 8; }
}
