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
      case 0 -> "( Z, S)";
      case 1 -> "(-X, W)";
      case 2 -> "(-Z, N)";
      default -> "( X, E)";
    };
  }
  public static String coordinates(int x, int y, int z) { return x + ", " + y + ", " + z; }

  public static String location(String coordinates, String direction, boolean showCoordinates, boolean showDirection) {
    return showCoordinates ? coordinates + (showDirection ? " " + direction : "") : showDirection ? direction : "";
  }
}
