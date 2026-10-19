package local.luke.power.sprint;

/** Ten native-sized status icons, with half-icon steps and an armor collision boundary. */
public final class StaminaIcons {
  private static final String[] BOLT = {
    "000011100", "000111000", "001110000", "011111110", "000111100",
    "000111000", "001110000", "001100000", "011000000"};
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
  public static int y(int right, int baseline, boolean armor, int armorX, int armorY) {
    return armor && right - 81 < armorX + 81 && right > armorX && Math.abs(baseline - armorY) < 10
        ? armorY - 10 : baseline;
  }
  public static int y(int right, int baseline, boolean armor, int armorX, int armorY, int height) {
    if (baseline < 0) baseline += 32;
    int min = height >= 11 ? 1 : 0, max = Math.max(min, height - 10);
    int result = Math.max(min, Math.min(max, baseline));
    if (armor && right - 81 < armorX + 81 && right > armorX && Math.abs(result - armorY) < 10) {
      if (armorY >= 11) result = Math.min(max, armorY - 10);
      else result = Math.max(min, Math.min(max, armorY + 10));
    }
    return result;
  }
}
