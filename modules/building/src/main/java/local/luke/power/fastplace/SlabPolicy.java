package local.luke.power.fastplace;

/** MATCH_FIRST keeps one horizontal operation while other modes may climb continuously. */
public final class SlabPolicy {
  private SlabPolicy() {}

  /** Vertical clicks build full blocks; horizontal strokes keep their original slab anchor. */
  public static SlabMode effective(SlabMode mode, int face, boolean halfSlabAnchor) {
    return mode == SlabMode.SMART
        ? face >= 2 && face <= 5 && halfSlabAnchor ? SlabMode.CONTINUOUS : SlabMode.DOUBLE
        : mode;
  }

  public static boolean allows(
      SlabMode mode, boolean firstMerge, boolean nextMerge, int firstY, int nextY) {
    return mode != SlabMode.MATCH_FIRST || (firstMerge == nextMerge && firstY == nextY);
  }

  /** Half slabs expose side faces as a row grows. Only that same-layer case is equivalent. */
  public static int restrictionFace(
      SlabMode mode, int firstFace, int firstY, int nextY, int hitFace) {
    return mode != SlabMode.DOUBLE
            && firstFace >= 0
            && firstFace < 2
            && firstY == nextY
            && hitFace >= 2
            && hitFace <= 5
        ? firstFace
        : hitFace;
  }
}
