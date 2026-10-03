package local.luke.power.fastplace;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class RestrictionTest {
  @Test
  void matchesTweakerooRestrictions() {
    Random random = new Random(173);
    for (int i = 0; i < 100000; i++) {
      BlockPos first =
          new BlockPos(random.nextInt(200) - 100, random.nextInt(128), random.nextInt(200) - 100);
      BlockPos next =
          new BlockPos(
              first.x() + random.nextInt(9) - 4,
              first.y() + random.nextInt(9) - 4,
              first.z() + random.nextInt(9) - 4);
      int side = random.nextInt(6), firstSide = random.nextInt(6);
      for (RestrictionMode mode : RestrictionMode.values()) {
        assertEquals(
            reference(mode, first, firstSide, next, side),
            mode.allows(first, firstSide, next, side));
      }
    }
  }

  // Direct coordinate predicates from Tweakeroo PlacementTweaks, LGPL-3.0-only.
  private boolean reference(RestrictionMode mode, BlockPos a, int firstSide, BlockPos b, int side) {
    int axis = firstSide / 2; // Y, Z, X
    boolean x = a.x() == b.x(), y = a.y() == b.y(), z = a.z() == b.z();
    int dx = Math.abs(a.x() - b.x()), dy = Math.abs(a.y() - b.y()), dz = Math.abs(a.z() - b.z());
    return switch (mode) {
      case FACE -> side == firstSide;
      case LAYER -> y;
      case PLANE -> axis == 0 ? y : axis == 1 ? z : x;
      case COLUMN -> axis == 0 ? x && z : axis == 1 ? x && y : y && z;
      case LINE -> axis == 0 ? y && (x || z) : axis == 1 ? z && (x || y) : x && (y || z);
      case DIAGONAL -> axis == 0 ? y && dx == dz : axis == 1 ? z && dx == dy : x && dy == dz;
    };
  }

  @Test
  void faceDoesNotMeanSamePlane() {
    var first = new BlockPos(0, 64, 0);
    assertTrue(RestrictionMode.FACE.allows(first, 1, new BlockPos(10, 70, 20), 1));
    assertFalse(RestrictionMode.PLANE.allows(first, 1, new BlockPos(10, 70, 20), 1));
    assertFalse(RestrictionMode.FACE.allows(first, 1, first, 0));
  }

  @Test
  void invalidFacesAndExtremeCoordinatesAreSafe() {
    for (var m : RestrictionMode.values())
      assertFalse(m.allows(new BlockPos(0, 0, 0), 6, new BlockPos(0, 0, 0), 0));
    assertFalse(
        RestrictionMode.DIAGONAL.allows(
            new BlockPos(Integer.MIN_VALUE, 0, 0), 1, new BlockPos(Integer.MAX_VALUE, 0, 1), 1));
  }

  @Test
  void neverRepeatLastPosition() {
    var pos = new BlockPos(0, 65, 0);
    var s = new PlacementSession(pos, 1, 0, 1, 0, 0);
    assertFalse(s.allows(pos, 1, false, RestrictionMode.FACE));
    var next = new BlockPos(1, 65, 0);
    assertTrue(s.allows(next, 1, true, RestrictionMode.FACE));
    s.placed(next);
    assertFalse(s.allows(next, 1, false, RestrictionMode.FACE));
  }
}
