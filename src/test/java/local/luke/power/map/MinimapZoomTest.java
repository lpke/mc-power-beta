package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class MinimapZoomTest {
  @Test void expandedWheelDefaultsOnWithoutReplacingSavedOptOuts() {
    var gson = new Gson();
    assertTrue(gson.fromJson("{}", MapSettings.class).zoomWhileExpanded);
    var optedOut = gson.fromJson("{\"zoomWhileExpanded\":false}", MapSettings.class);
    assertFalse(gson.fromJson(gson.toJson(optedOut), MapSettings.class).zoomWhileExpanded);
    for (var base : local.luke.power.config.DefaultBases.Base.values())
      assertTrue(local.luke.power.config.DefaultBases.value(base, "maps.zoomWhileExpanded",
          new com.google.gson.JsonPrimitive(true)).getAsBoolean());
  }
  @Test void resetUsesTheChosenStepAndOldPreferencesStartAtOneAndAQuarter() {
    double[] steps = {.25, .5, 1.25, 2.5, 4};
    assertEquals(1.25, MinimapZoomSteps.reset(steps, 1.25));
    assertEquals(2.5, MinimapZoomSteps.reset(steps, 2.5));
    assertEquals(.5, MinimapZoomSteps.reset(new double[]{.5, 2.5}, 1.25));
    assertEquals(.5, MinimapZoomSteps.reset(new double[]{.5, 2}, 1.25));
    assertEquals(1.25, new Gson().fromJson("{}", MapSettings.class).zoomDefault);
    var saved = new MapSettings(); saved.zoomDefault = 2.5;
    assertEquals(2.5, new Gson().fromJson(new Gson().toJson(saved), MapSettings.class).zoomDefault);
  }
  @Test void fineWheelTargetsClampAndKeysChooseTheAdjacentConfiguredStep() {
    double target = MinimapZoom.scroll(1.25, 120);
    assertEquals(1.375, target, 1e-12);
    assertEquals(1.25, MinimapZoom.scroll(target, -120), 1e-12);
    assertEquals(.25, MinimapZoom.scroll(.25, -120));
    assertEquals(4, MinimapZoom.scroll(4, 120));
    assertEquals(1.25, MinimapZoom.scroll(1.25, 0));
    assertTrue(MinimapZoom.scroll(1.25, 60) > 1.25 && MinimapZoom.scroll(1.25, 60) < target);
    double[] steps = {.25, .5, 1.25, 2.5, 4};
    assertEquals(2.5, MinimapZoomSteps.next(steps, target, 1));
    assertEquals(1.25, MinimapZoomSteps.next(steps, target, -1));
  }
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
  @Test void replacementCanPublishWithMissingNewEdgesButKeepsAnyUnreplacedKnownCenter() {
    assertTrue(MinimapBuffer.replacementReady(true, false, 81, 9, 0));
    assertFalse(MinimapBuffer.replacementReady(true, false, 81, 80, 1));
    assertFalse(MinimapBuffer.replacementReady(true, false, 81, 0, 0));
    assertFalse(MinimapBuffer.replacementReady(false, true, 81, 72, 0));
    assertTrue(MinimapBuffer.replacementReady(false, true, 81, 73, 0));
    assertFalse(MinimapBuffer.replacementReady(false, true, 0, 0, 0));
    assertTrue(MinimapBuffer.replacementReady(false, false, 81, 1, 0));
  }
  @Test void zoomingOutStartsImmediatelyAndCanReverseBeforeTheFirstTransitionEnds() {
    var zoom = new MinimapZoom(); zoom.set(4, 0); zoom.set(.25, 1_000);
    double wider = zoom.value(30_001_000);
    assertTrue(wider < 4 && wider > .25);
    zoom.set(2.5, 30_001_000);
    assertEquals(wider, zoom.value(30_001_000));
    assertTrue(zoom.value(60_001_000) > wider);
    assertEquals(2.5, zoom.value(210_001_000));
  }
}
