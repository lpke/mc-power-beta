package local.luke.power.fastplace;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SmartSlabTest {
  @Test void smartUsesDoubleSlabsVerticallyAndKeepsTheHorizontalAnchor() {
    for (boolean half : new boolean[]{false, true})
      for (int face = 0; face < 6; face++)
        assertEquals(half && face >= 2 ? SlabMode.CONTINUOUS : SlabMode.DOUBLE,
            SlabPolicy.effective(SlabMode.SMART, face, half));
  }

  @Test void directionChangesKeepTheStrokeAnchorAndNativeFaceRestrictions() {
    for (boolean half : new boolean[]{false, true}) {
      var stroke = new PlacementSession(new BlockPos(0, 70, 0), 5, 0, 44, 3, 0);
      stroke.halfSlabAnchor = half;
      for (int face : new int[]{5, 1, 0, 2, 4, 3, 5}) {
        var effective = SlabPolicy.effective(SlabMode.SMART, face, stroke.halfSlabAnchor);
        assertEquals(half && face >= 2 ? SlabMode.CONTINUOUS : SlabMode.DOUBLE, effective);
        assertEquals(face, SlabPolicy.restrictionFace(effective, 5, 70, 70, face));
      }
    }
  }

  @Test void existingSavedModesKeepTheirBehaviorAndMissingPreferencesUseSmart() {
    for (var mode : new SlabMode[]{SlabMode.CONTINUOUS, SlabMode.DOUBLE, SlabMode.MATCH_FIRST})
      for (boolean half : new boolean[]{false, true})
        for (int face = 0; face < 6; face++) assertEquals(mode, SlabPolicy.effective(mode, face, half));
    assertEquals(SlabMode.SMART, new local.luke.power.fastplace.config.Settings().slabMode);
    var gson = new com.google.gson.Gson();
    assertEquals(SlabMode.SMART, gson.fromJson("{}", local.luke.power.fastplace.config.Settings.class).slabMode);
    for (var mode : SlabMode.values())
      assertEquals(mode, gson.fromJson("{\"slabMode\":\"" + mode + "\"}", local.luke.power.fastplace.config.Settings.class).slabMode);
  }
}
