package local.luke.fastplace;

/** MATCH_FIRST keeps one horizontal operation while other modes may climb continuously. */
public final class SlabPolicy {
  private SlabPolicy() {}

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
