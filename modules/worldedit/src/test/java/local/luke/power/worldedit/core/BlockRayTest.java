package local.luke.power.worldedit.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class BlockRayTest {
  @Test
  void includesCornerCrossingsThatQuarterBlockSamplesMiss() {
    var cells = BlockRay.cells(.5, 1.0102, .5, 1, -.02, 0, 1);
    assertTrue(cells.contains(new Pos(1, 1, 0)));
    double length = Math.sqrt(1.0004);
    for (double d = .25; d <= 1; d += .25)
      assertNotEquals(new Pos(1, 1, 0), new Pos((int)Math.floor(.5 + d / length),
          (int)Math.floor(1.0102 - .02 * d / length), 0));
  }

  @Test
  void handlesNegativeCoordinatesBoundariesAndAxisAlignedRays() {
    assertEquals(List.of(new Pos(-1, 2, -1), new Pos(-2, 2, -1), new Pos(-3, 2, -1)),
        BlockRay.cells(-1, 2, -.5, -1, 0, 0, 1.5));
    assertEquals(List.of(new Pos(0, 0, 0), new Pos(0, 1, 0), new Pos(0, 2, 0)),
        BlockRay.cells(.5, .5, .5, 0, 3, 0, 2));
    assertEquals(List.of(new Pos(0, 0, 0), new Pos(1, 1, 1)),
        BlockRay.cells(.5, .5, .5, 1, 1, 1, 1));
  }

  @Test
  void boundsTraversalAndRejectsInvalidRays() {
    assertTrue(BlockRay.cells(.5, .5, .5, 1, 1, 1, 256).size() <= 445);
    for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, 1e300})
      assertThrows(IllegalArgumentException.class, () -> BlockRay.cells(invalid, 0, 0, 1, 0, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> BlockRay.cells(0, 0, 0, 0, 0, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> BlockRay.cells(0, 0, 0, 1, 0, 0, 257));
  }
}
