package local.luke.power.sprint;

/** Ten native-sized status icons, with half-icon steps and an armor collision boundary. */
public final class StaminaIcons {
  private StaminaIcons() {}
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
    int max = Math.max(0, height - 9);
    int result = Math.max(0, Math.min(max, baseline));
    if (armor && right - 81 < armorX + 81 && right > armorX && Math.abs(result - armorY) < 10) {
      if (armorY >= 10) result = Math.min(max, armorY - 10);
      else result = Math.max(0, Math.min(max, armorY + 10));
    }
    return result;
  }
}
