package local.luke.power.fastplace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SlabRestrictionTest {
  @Test
  void doubleSlabsUseExactlyTheSameFaceRulesAsFullBlocks() {
    for (int first = 0; first < 6; first++)
      for (int face = 0; face < 6; face++)
        for (int dy = -2; dy <= 2; dy++) {
          int effective = SlabPolicy.restrictionFace(SlabMode.DOUBLE, first, 70, 70 + dy, face);
          assertEquals(face, effective);
          for (var rule : RestrictionMode.values())
            assertEquals(
                rule.allows(new BlockPos(0, 70, 0), first, new BlockPos(1, 70 + dy, 0), face),
                rule.allows(new BlockPos(0, 70, 0), first, new BlockPos(1, 70 + dy, 0), effective));
        }
  }

  @Test
  void halfSlabSideContinuationNeverEscapesItsStartingLayerOrHorizontalFace() {
    for (var mode : new SlabMode[] {SlabMode.CONTINUOUS, SlabMode.MATCH_FIRST}) {
      for (int first = 0; first < 6; first++)
        for (int face = 0; face < 6; face++)
          for (int dy = -1; dy <= 1; dy++) {
            int effective = SlabPolicy.restrictionFace(mode, first, 70, 70 + dy, face);
            assertEquals(first < 2 && face >= 2 && dy == 0 ? first : face, effective);
          }
    }
  }

  @Test
  void roofDoubleSlabsCannotTurnUpwardDuringOneHold() {
    int face = SlabPolicy.restrictionFace(SlabMode.DOUBLE, 3, 70, 71, 1);
    assertFalse(
        RestrictionMode.FACE.allows(new BlockPos(0, 70, 0), 3, new BlockPos(0, 71, 1), face));
  }
}
