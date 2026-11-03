package local.luke.power.gameplay;

/** Downward reach from the selected eye into the modern crouch-eye reach sphere. */
final class CrouchReachGeometry {
  static final double BLOCK_REACH = 4.5;
  private static final double CROUCH_EYE = 1.27, MAX_EYE_DIFFERENCE = .35;

  private CrouchReachGeometry() {}

  static double range(double eyeAboveFeet, float pitch) {
    if (!Double.isFinite(eyeAboveFeet) || !Float.isFinite(pitch) || pitch <= 0 || pitch > 90)
      return BLOCK_REACH;
    double height = Math.max(0, Math.min(MAX_EYE_DIFFERENCE, eyeAboveFeet - CROUCH_EYE));
    if (height == 0) return BLOCK_REACH;
    double down = Math.sin(Math.toRadians(pitch));
    // Solve |ray * reach + (0, height, 0)| = BLOCK_REACH without moving
    // the ray's origin or changing its direction. Retain the ordinary sphere
    // wherever the translated sphere would shorten the existing block range.
    double reach = height * down
        + Math.sqrt(BLOCK_REACH * BLOCK_REACH - height * height * (1 - down * down));
    return Math.max(BLOCK_REACH, reach);
  }
}
