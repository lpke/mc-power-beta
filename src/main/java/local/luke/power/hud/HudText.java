package local.luke.power.hud;

/** Formatting stays independent of the simulation clock and the system locale. */
public final class HudText {
  private HudText() {}
  public static String time(long ticks) {
    int minutes = (int)(Math.floorMod(ticks, 24000L) * 60 / 1000 + 360) % 1440;
    int hour = minutes / 60;
    return (hour % 12 == 0 ? 12 : hour % 12) + ":" + (minutes % 60 < 10 ? "0" : "")
        + minutes % 60 + (hour < 12 ? " AM" : " PM");
  }
  public static String direction(float yaw) {
    return switch (Math.floorMod((int)Math.floor(yaw / 90.0 + .5), 4)) {
      case 0 -> "South (+Z)";
      case 1 -> "West (-X)";
      case 2 -> "North (-Z)";
      default -> "East (+X)";
    };
  }
}
