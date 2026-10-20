package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class MinimapZoomTest {
  @Test void customDecimalStepsWrapFromExactAndUnlistedTargets() {
    double[] steps = {.25, .5, 1.25, 2.5, 4};
    assertEquals(.25, MinimapZoomSteps.next(steps, 4, 1));
    assertEquals(4, MinimapZoomSteps.next(steps, .25, -1));
    assertEquals(1.25, MinimapZoomSteps.next(steps, .5, 1));
    assertEquals(1.25, MinimapZoomSteps.next(steps, 2.5, -1));
    assertEquals(1.25, MinimapZoomSteps.next(steps, .9, 1));
    assertEquals(.5, MinimapZoomSteps.next(steps, .9, -1));
    assertEquals(1.25, MinimapZoomSteps.next(new double[] {1.25}, 1.25, 1));
  }
  @Test void rejectsInvalidOrUnboundedPersistedLists() {
    for (double[] values : new double[][] {null, {}, {.5, .5}, {1.25, .5}, {.249}, {4.1}, {Double.NaN}, {Double.POSITIVE_INFINITY}, new double[33]})
      assertThrows(IllegalArgumentException.class, () -> MinimapZoomSteps.validate(values));
    var gson = new Gson();
    var saved = new MapSettings(); saved.zoomSteps = new double[] {.26, 1.25, 2.27};
    var restored = gson.fromJson(gson.toJson(saved), MapSettings.class);
    assertArrayEquals(saved.zoomSteps, restored.zoomSteps); assertDoesNotThrow(restored::validate);
    assertArrayEquals(new MapSettings().zoomSteps, gson.fromJson("{}", MapSettings.class).zoomSteps);
    assertThrows(IllegalArgumentException.class, () -> gson.fromJson("{\"zoomSteps\":[2,1]}", MapSettings.class).validate());
  }
  @Test void simultaneousCaveAndZoomChangesDoNotCompareDifferentWorldExtents() {
    assertTrue(MinimapBuffer.replacementKnown(true, false, 81, 0));
    assertFalse(MinimapBuffer.replacementKnown(false, false, 81, 0));
    assertTrue(MinimapBuffer.replacementKnown(false, false, 81, 1));
    assertFalse(MinimapBuffer.replacementKnown(false, true, 81, 72));
    assertTrue(MinimapBuffer.replacementKnown(false, true, 81, 73));
    assertFalse(MinimapBuffer.replacementKnown(false, true, 0, 0));
  }
  @Test void animationUsesAbsoluteTimeAndReachesAnExactDecimalEndpoint() {
    var zoom = new MinimapZoom(); zoom.set(.26, 0); zoom.set(2.27, 1_000);
    assertEquals(.26, zoom.value(1_000));
    double halfway = zoom.value(90_001_000);
    assertTrue(halfway > .26 && halfway < 2.27);
    assertEquals(halfway, zoom.value(90_001_000));
    assertEquals(2.27, zoom.value(180_001_000));
    assertEquals(2.27, zoom.value(Long.MAX_VALUE));
    zoom.set(.5, 180_001_000); double moving = zoom.value(200_001_000);
    zoom.set(1.25, 200_001_000); assertEquals(moving, zoom.value(200_001_000));
    zoom.reset(); zoom.set(4, 0); assertEquals(4, zoom.value(0));
  }
}
