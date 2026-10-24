package local.luke.power.map;

import local.luke.power.hud.HudCoordinates;
import local.luke.power.input.ReportedPosition;
import net.minecraft.client.Minecraft;

/** Shared command/editor parsing; relative values use the unbounded reported pose. */
public final class WaypointCoordinates {
  private static final String BOUNDS = "X/Z from -32,000,000 to 32,000,000; Y from 0 to 127.";

  private WaypointCoordinates() {}

  public static ReportedPosition.Pose current(Minecraft mc) {
    if (mc.player == null) throw new IllegalArgumentException("Open a world first.");
    var player = mc.player;
    return ReportedPosition.current(player, player.x, HudCoordinates.y(player), player.z, player.yaw, player.pitch);
  }

  public static Waypoint resolve(Waypoint initial, String x, String y, String z, ReportedPosition.Pose origin) {
    int px = coordinate(x, origin.x(), -MapMath.BORDER, MapMath.BORDER);
    int py = coordinate(y, origin.y(), 0, 127);
    int pz = coordinate(z, origin.z(), -MapMath.BORDER, MapMath.BORDER);
    Waypoint point = initial.copy();
    point.x = px; point.y = py; point.z = pz;
    return point;
  }

  private static int coordinate(String text, double origin, int min, int max) {
    double value;
    try {
      if (text.startsWith("~")) {
        double offset = text.length() == 1 ? 0 : Double.parseDouble(text.substring(1));
        value = Math.floor(origin + offset);
        if (!Double.isFinite(offset) || !Double.isFinite(value)) throw new NumberFormatException();
      } else value = Integer.parseInt(text);
    } catch (NumberFormatException failure) {
      throw new IllegalArgumentException("Use whole coordinates, ~ or ~offset. " + BOUNDS);
    }
    if (value < min || value > max)
      throw new IllegalArgumentException("Position is outside waypoint bounds: " + BOUNDS);
    return (int)value;
  }

  /** Includes malformed numeric/relative tokens so invalid suffixes cannot become saved names. */
  static boolean candidate(String text) {
    if (text.startsWith("~")) return true;
    try { Double.parseDouble(text); return true; }
    catch (NumberFormatException failure) { return false; }
  }
}
