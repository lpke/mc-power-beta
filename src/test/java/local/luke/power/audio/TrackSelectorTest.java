package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class TrackSelectorTest {
  private final TrackSelector selector = new TrackSelector();
  private final Random random = new Random(1);

  private String reserve(String context, String... ids) {
    return selector.reserve(context, List.of(ids), false, false, random, id -> id);
  }

  @Test void contextsStartAtTheirFirstTrackAndResumeTheirOwnSuccessor() {
    assertEquals("a", reserve("world", "a", "b", "c"));
    selector.played("world", "a");
    assertEquals("x", reserve("menu", "x", "y"));
    selector.played("menu", "x");
    assertEquals("b", reserve("world", "a", "b", "c"));
    assertEquals("y", reserve("menu", "x", "y"));
  }

  @Test void reservationsDoNotAdvancePlayback() {
    for (int i = 0; i < 10; i++) assertEquals("a", reserve("world", "a", "b", "c"));
    selector.played("world", "a");
    for (int i = 0; i < 10; i++) assertEquals("b", reserve("world", "a", "b", "c"));
    selector.played("world", "b");
    assertEquals("c", reserve("world", "a", "b", "c"));
  }

  @Test void reorderingUsesThePlayedTracksNewSuccessor() {
    reserve("world", "a", "b", "c");
    selector.played("world", "a");
    assertEquals("c", reserve("world", "b", "a", "c"));
    selector.played("world", "c");
    assertEquals("b", reserve("world", "b", "a", "c"));
  }

  @Test void missingPlayedTrackUsesItsFirstSurvivingSuccessor() {
    reserve("world", "a", "b", "c", "d");
    selector.played("world", "b");
    assertEquals("c", reserve("world", "d", "a", "c"));
    assertEquals("c", reserve("world", "d", "a", "c"));
    assertEquals("d", reserve("world", "d", "a"));
    assertNull(reserve("world"));
    assertEquals("d", reserve("world", "d", "a"));
  }

  @Test void shuffledReservationDoesNotCountAsThePreviousPlayedTrack() {
    Random fixed = new Random() { @Override public int nextInt(int bound) { return 0; } };
    assertEquals("a", selector.reserve("world", List.of("a", "b"), true, true, fixed, id -> id));
    assertEquals("a", selector.reserve("world", List.of("a", "b"), true, true, fixed, id -> id));
    selector.played("world", "a");
    assertEquals("b", selector.reserve("world", List.of("a", "b"), true, true, fixed, id -> id));
  }

  @Test void explicitMiddleTrackReplacesAnyOldReservationAndWrapsInSavedOrder() {
    assertEquals("a", reserve("world", "a", "b", "c", "d"));
    assertTrue(selector.align("world", List.of("a", "b", "c", "d"), "c"));
    assertEquals("d", reserve("world", "a", "b", "c", "d"));
    selector.played("world", "d");
    assertEquals("a", reserve("world", "a", "b", "c", "d"));
  }

  @Test void explicitOutsideSongLeavesPositionReservationAndRepeatStateAlone() {
    assertTrue(selector.align("world", List.of("a", "b", "c"), "a"));
    assertEquals("b", reserve("world", "a", "b", "c"));
    assertFalse(selector.align("world", List.of("a", "b", "c"), "outside"));
    assertEquals("b", reserve("world", "a", "b", "c"));
    Random fixed = new Random() { @Override public int nextInt(int bound) { return 0; } };
    assertEquals("b", selector.reserve("world", List.of("a", "b"), true, true, fixed, id -> id));
  }

  @Test void explicitChoiceUsesFullSavedOrderForMissingOrFilteredSuccessors() {
    assertTrue(selector.align("world", List.of("a", "b", "c", "d"), "b"));
    assertEquals("c", reserve("world", "d", "a", "c"));
    assertEquals("d", reserve("world", "d", "a"));
    assertEquals("x", reserve("menu", "x", "y"));
    assertEquals("c", reserve("world", "a", "c", "d"));
  }
}
