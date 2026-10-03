package local.luke.flexible;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GeometryTest {
  @Test
  void fiveRegionBoundaries() {
    assertEquals(FaceGrid.Part.CENTER, FaceGrid.part(.25, .75));
    assertEquals(FaceGrid.Part.TOP, FaceGrid.part(.1, .9));
    assertEquals(FaceGrid.Part.LEFT, FaceGrid.part(.01, .5));
    assertEquals(FaceGrid.Part.RIGHT, FaceGrid.part(.99, .5));
    assertEquals(FaceGrid.Part.BOTTOM, FaceGrid.part(.5, .01));
  }

  @Test
  void renderedRegionsMatchSelectionOnEveryFace() {
    var random = new java.util.Random(173);
    for (Direction face : Direction.values())
      for (Direction forward : Direction.values())
        if (forward.y == 0)
          for (int i = 0; i < 1000; i++) {
            double u = random.nextDouble(), v = random.nextDouble();
            double[] p = FaceGrid.point(face, forward, u, v, .5),
                uv = FaceGrid.uv(face, forward, p[0], p[1], p[2]);
            assertEquals(u, uv[0], 1e-14);
            assertEquals(v, uv[1], 1e-14);
            assertEquals(FaceGrid.part(u, v), FaceGrid.part(uv[0], uv[1]));
          }
  }

  @Test
  void outerDirectionsAreInTheFacePlane() {
    for (Direction face : Direction.values())
      for (Direction forward : Direction.values())
        if (forward.y == 0)
          for (FaceGrid.Part part : FaceGrid.Part.values()) {
            Direction d = FaceGrid.selected(face, forward, part);
            assertEquals(
                part == FaceGrid.Part.CENTER ? 1 : 0, d.x * face.x + d.y * face.y + d.z * face.z);
          }
    assertEquals(
        Direction.NORTH, FaceGrid.selected(Direction.UP, Direction.NORTH, FaceGrid.Part.TOP));
    assertEquals(
        Direction.UP, FaceGrid.selected(Direction.NORTH, Direction.NORTH, FaceGrid.Part.TOP));
  }

  @Test
  void metadataReversesEveryPistonDirectionAndRestoresScope() {
    BlockPos p = new BlockPos(0, 100, 0);
    for (int block : new int[] {29, 33})
      for (Direction d : Direction.values()) {
        var previous = Orientation.begin(new Orientation.Request(block, p, null, true));
        try {
          assertEquals(d.opposite().ordinal(), Orientation.metadata(block, 0, 100, 0, d.ordinal()));
          assertEquals(d.ordinal(), Orientation.metadata(block, 1, 100, 0, d.ordinal()));
        } finally {
          Orientation.restore(previous);
        }
        assertEquals(d.ordinal(), Orientation.metadata(block, 0, 100, 0, d.ordinal()));
      }
  }

  @Test
  void horizontalBlocksRejectVerticalOrientation() {
    for (int block : new int[] {23, 53, 61, 67, 86, 91, 93}) {
      assertFalse(Orientation.valid(block, Direction.UP));
      assertFalse(Orientation.valid(block, Direction.DOWN));
      assertTrue(Orientation.valid(block, Direction.EAST));
    }
  }

  @Test
  void reversalPreservesRepeaterDelayAndDoubleReversalIsIdentity() {
    BlockPos p = new BlockPos(0, 100, 0);
    for (int block : new int[] {23, 53, 61, 67, 86, 91, 93})
      for (int meta = 0; meta < 16; meta++) {
        if ((block == 23 || block == 61) && (meta < 2 || meta > 5)) continue;
        var previous = Orientation.begin(new Orientation.Request(block, p, null, true));
        try {
          int first = Orientation.metadata(block, 0, 100, 0, meta),
              second = Orientation.metadata(block, 0, 100, 0, first);
          assertEquals(block == 53 || block == 67 ? meta & 3 : meta, second);
          if (block == 93) assertEquals(meta & 12, first & 12);
        } finally {
          Orientation.restore(previous);
        }
      }
  }
}
