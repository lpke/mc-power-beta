package local.luke.power.ui;

/** Allocation-free Beta HUD geometry, shared by gameplay and the interactive preview. */
public final class NativeHudLayout {
  public static final int HOTBAR = 0, HEARTS = 1, ARMOUR = 2, OXYGEN = 3, MESSAGE = 4, STAMINA = 5;
  private NativeHudLayout() {}
  public static int xOffset(int width, int element, int mode, int anchor, int textWidth) {
    if (mode == 0 || anchor == 1) return 0;
    if (mode == 1 || element == HOTBAR) return anchor == 0 ? 91 - width / 2 : -91 + width / 2 + width % 2;
    if (element == MESSAGE) return anchor == 0 ? textWidth / 2 - width / 2 : width - (textWidth + 1) / 2 - width / 2;
    if (element == ARMOUR || element == STAMINA) return anchor == 0 ? -width / 2 - 10 : -91 + width / 2 + width % 2;
    return anchor == 0 ? 91 - width / 2 : 10 + width / 2 + width % 2;
  }
  public static int yOffset(int height, int element, int mode, int anchor, boolean iconsBelow, boolean messagesBelow) {
    if (mode == 0) return 0;
    int y = 0;
    if (mode == 1) y = anchor == 2 ? 52 - height : anchor == 1 ? (52 - height) / 2 : 0;
    else if (anchor == 2) y = (element == HOTBAR ? 22 : element == OXYGEN ? 41 : element == MESSAGE ? 52 : 32) - height;
    else if (anchor == 1) y = 11 - height / 2 - height % 2;
    if (iconsBelow && element != HOTBAR && element != MESSAGE) y += element == OXYGEN ? 51 : 33;
    if (messagesBelow && element == MESSAGE) y += 74;
    return y;
  }
  public static int baseX(int width, int element, int textWidth) {
    return element == ARMOUR || element == STAMINA ? width / 2 + 10 : element == MESSAGE ? width / 2 - textWidth / 2 : width / 2 - 91;
  }
  public static int baseY(int height, int element) {
    return height - (element == HOTBAR ? 22 : element == OXYGEN ? 41 : element == MESSAGE ? 48 : 32);
  }
  /** Keep the status row on screen and leave a one-pixel gap from overlapping armor. */
  public static int staminaY(int x, int baseline, boolean armor, int armorX, int armorY, int height) {
    int max = Math.max(0, height - 9);
    int y = Math.max(0, Math.min(max, baseline));
    if (armor && x < armorX + 81 && x + 81 > armorX && y < armorY + 9 && y + 9 > armorY) {
      y = armorY >= 10 ? Math.min(max, armorY - 10) : Math.min(max, armorY + 10);
    }
    return y;
  }
  /** Separate offsets move freely from the armor-aware anchor, without a collision dead zone. */
  public static int staminaY(int x, int baseline, boolean armor, int armorX, int armorY, int height,
      int offsetX, int offsetY) {
    int anchor = staminaY(x - offsetX, baseline - offsetY, armor, armorX, armorY, height);
    return Math.max(0, Math.min(Math.max(0, height - 9), anchor + offsetY));
  }

}
