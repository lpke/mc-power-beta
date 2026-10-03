package local.luke.power.fastplace;

/** Adapted from Tweakeroo PlacementTweaks, LGPL-3.0-only. */
public enum RestrictionMode {
  FACE,
  PLANE,
  LAYER,
  COLUMN,
  LINE,
  DIAGONAL;

  public boolean allows(BlockPos first, int firstFace, BlockPos next, int face) {
    if (firstFace < 0 || firstFace > 5 || face < 0 || face > 5) return false;
    long dx = (long) next.x() - first.x();
    long dy = (long) next.y() - first.y();
    long dz = (long) next.z() - first.z();
    long normal = firstFace < 2 ? dy : firstFace < 4 ? dz : dx;
    long u = firstFace < 2 ? dx : firstFace < 4 ? dx : dy;
    long v = firstFace < 2 ? dz : firstFace < 4 ? dy : dz;
    return switch (this) {
      case FACE -> face == firstFace;
      case PLANE -> normal == 0;
      case LAYER -> dy == 0;
      case COLUMN -> u == 0 && v == 0;
      case LINE -> normal == 0 && (u == 0 || v == 0);
      case DIAGONAL -> normal == 0 && Math.abs(u) == Math.abs(v);
    };
  }
}
