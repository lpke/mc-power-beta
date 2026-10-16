package local.luke.power.rules;

/** The flight command never grants a game mode or bypasses survival's world permission. */
public final class FlightAccess {
  private FlightAccess() {}
  public static boolean allowed(boolean spectator, boolean creative, boolean permission,
      boolean preference, int override) {
    if (spectator || override == 0) return false;
    return creative ? override == 1 || preference : permission && override == 1;
  }
}
