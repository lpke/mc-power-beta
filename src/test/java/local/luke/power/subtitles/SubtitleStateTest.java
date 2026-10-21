package local.luke.power.subtitles;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SubtitleStateTest {
  private final SubtitleState state = new SubtitleState();
  private final SubtitleState.Listener listener = new SubtitleState.Listener(0, 0, 0, 0, 1);
  private SubtitleState.Cue cue(String key, long time, double x, double z, double range, boolean looping) {
    return new SubtitleState.Cue(new SubtitleCatalog.Caption(key, key), time, x, 0, z, range, looping, false);
  }
  private void add(SubtitleState.Cue cue) { state.accept(cue, cue.time(), 1, listener); }
  @Test void exactExpiryIsStrictAndBrightnessUsesOpaqueGray() {
    add(cue("steps", 0, 1, 0, 16, false));
    assertEquals(255, state.visible(0, 1, listener).get(0).brightness());
    assertEquals(165, state.visible(1500, 1, listener).get(0).brightness());
    assertEquals(75, state.visible(3000, 1, listener).get(0).brightness()); assertTrue(state.visible(3001, 1, listener).isEmpty());
  }
  @Test void repeatsKeepInitialRowOrderAndReplaceOnlyIdenticalPositions() {
    add(cue("first", 0, 1, 0, 16, false)); add(cue("second", 1, 2, 0, 16, false));
    add(cue("first", 2, 1, 0, 16, false)); assertEquals(2, state.positionCount());
    add(cue("first", 3, 3, 0, 16, false)); assertEquals(3, state.positionCount());
    assertEquals(List.of("first", "second"), state.visible(3, 1, listener).stream().map(SubtitleState.Visible::key).toList());
  }
  @Test void newestInRangeWinsEvenWhenNewestOverallIsOutsideRange() {
    add(cue("steps", 0, 1, 0, 16, false)); add(cue("steps", 10, -3, 0, 16, false)); add(cue("steps", 20, 30, 0, 16, false));
    assertEquals('>', state.visible(20, 1, listener).get(0).arrow());
  }
  @Test void equalTimestampTieSelectsTheFartherInRangeLocation() {
    add(cue("steps", 10, 1, 0, 16, false)); add(cue("steps", 10, -3, 0, 16, false));
    assertEquals('>', state.visible(10, 1, listener).get(0).arrow());
  }
  @Test void rangeIsStrictAndFirstAdmissionRangeSurvivesRefreshAndEmptyIntervals() {
    add(cue("steps", 0, 4, 0, 4, false)); assertTrue(state.visible(0, 1, listener).isEmpty());
    add(cue("steps", 1, 3.99, 0, 16, false)); assertEquals(1, state.visible(1, 1, listener).size());
    state.visible(4000, 1, listener); add(cue("steps", 4001, 5, 0, 64, false)); assertTrue(state.visible(4001, 1, listener).isEmpty());
  }
  @Test void initialLoopFlagAndUnscaledHoldMatchModernBrightness() {
    add(cue("loop", 0, 1, 0, 16, true)); add(cue("loop", 1, 1, 0, 16, false));
    assertEquals(255, state.visible(501, 1, listener).get(0).brightness());
    assertEquals(165, SubtitleState.brightness(1750, 1, true));
    assertEquals(255, SubtitleState.brightness(500, 2, true)); assertEquals(165, SubtitleState.brightness(3000, 2, true));
  }
  @Test void arrowsUseActualListenerBasisAndThreeDimensionalDirection() {
    assertEquals('<', SubtitleState.arrow(listener, 5, 0, 0)); assertEquals('>', SubtitleState.arrow(listener, -5, 0, 0));
    assertEquals('\0', SubtitleState.arrow(listener, 0, 0, 5)); assertEquals('\0', SubtitleState.arrow(listener, 0, 5, 0));
    assertEquals('\0', SubtitleState.arrow(listener, 0, 0, 0));
    var turned = new SubtitleState.Listener(0, 0, 0, 1, 0);
    assertEquals('>', SubtitleState.arrow(turned, 0, 0, 5)); assertEquals('<', SubtitleState.arrow(turned, 0, 0, -5));
  }
  @Test void relativeCuesHaveNoArrowAndInfiniteRange() {
    var cue = new SubtitleState.Cue(new SubtitleCatalog.Caption("portal", "Portal noise fades"), 0, 0, 0, 0, Double.POSITIVE_INFINITY, false, true);
    add(cue); var far = new SubtitleState.Listener(1e6, 64, -1e6, 0, 1);
    assertEquals('\0', state.visible(0, 1, far).get(0).arrow());
  }
  @Test void boundedPositionBurstKeepsRecentNearbySoundAndReportsSaturation() {
    for (int i = 0; i < 2000; i++) add(cue("burst", i, i % 2 == 0 ? i + 32 : -(i % 15 + 1), 0, 16, false));
    assertEquals(SubtitleState.MAX_POSITIONS, state.positionCount()); assertTrue(state.saturated() > 0);
    assertEquals('>', state.visible(2000, 1, listener).get(0).arrow());
  }
  @Test void rowCountCannotGrowWithUnknownDynamicNamesAndClearRemovesHistory() {
    for (int i = 0; i < 200; i++) add(cue("row" + i, 0, 1, 0, 16, false));
    assertEquals(SubtitleState.MAX_CAPTIONS, state.positionCount()); state.clear(); assertEquals(0, state.positionCount()); assertEquals(0, state.saturated());
  }
  @Test void invalidLocationsRangesAndListenersNeverEnterVisibleState() {
    for (var cue : List.of(cue("nan", 0, Double.NaN, 0, 16, false), cue("inf", 0, Double.POSITIVE_INFINITY, 0, 16, false), cue("range", 0, 0, 0, Double.NaN, false), cue("zero", 0, 0, 0, 0, false))) add(cue);
    assertEquals(0, state.positionCount()); assertTrue(state.visible(0, 1, null).isEmpty());
    assertTrue(state.visible(0, 1, new SubtitleState.Listener(Double.NaN, 0, 0, 0, 1)).isEmpty());
  }
}
