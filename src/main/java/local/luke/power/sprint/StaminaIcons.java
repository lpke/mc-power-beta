package local.luke.power.sprint;

import local.luke.power.ui.NativeHudLayout;

/** Native nine-pixel bolts, with armor-style half steps filled from the right. */
public final class StaminaIcons {
  private static final String[] BOLT = {
    "000000000", "000011100", "000111000", "001110000", "011111110",
    "000111000", "001110000", "001100000", "000000000"};
  private StaminaIcons() {}
  public static boolean bolt(int x, int y) {
    return x >= 0 && x < 9 && y >= 0 && y < 9 && BOLT[y].charAt(x) == '1';
  }
  public static boolean border(int x, int y) {
    return !bolt(x, y) && (bolt(x - 1, y) || bolt(x + 1, y) || bolt(x, y - 1) || bolt(x, y + 1));
  }
  public static int restartIcon(double percent) {
    return !Double.isFinite(percent) || percent <= 0 ? -1 : Math.min(9, (int)Math.ceil(percent / 10) - 1);
  }
  public static int units(double stamina, double capacity) {
    if (!Double.isFinite(stamina) || !Double.isFinite(capacity) || capacity <= 0) return 0;
    return Math.max(0, Math.min(20, (int)Math.ceil(stamina / capacity * 20)));
  }
  public static int x(int left, int icon) { return left + (9 - icon) * 8; }
  /** Negative means transparent. Restart reserves darken only depleted pixels. */
  public static int color(int icon, int units, double restartPercent, int x, int y) {
    return color(icon, units, restartPercent, false, x, y);
  }
  public static int color(int icon, int units, double restartPercent, boolean exhausted, int x, int y) {
    if (border(x, y)) return 0x000000;
    if (!bolt(x, y)) return -1;
    boolean filled = units >= (icon + 1) * 2 || units == icon * 2 + 1 && x >= 4;
    if (!filled) return exhausted && icon <= restartIcon(restartPercent) ? 0x3a3222 : 0x51462e;
    if (x == 4 && y == 1 || x == 3 && y == 2) return 0xffeaa0;
    return x == 7 && y == 4 || x == 3 && y == 7 ? 0xbf8c22 : 0xe2b32f;
  }
  public static int y(int right, int baseline, boolean armor, int armorX, int armorY) {
    return armor && right - 81 < armorX + 81 && right > armorX && Math.abs(baseline - armorY) < 9
        ? armorY - 10 : baseline;
  }
  public static int y(int right, int baseline, boolean armor, int armorX, int armorY, int height) {
    return NativeHudLayout.staminaY(right - 81, baseline, armor, armorX, armorY, height);
  }
}
