package local.luke.power.subtitles;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SubtitleForwardingTest {
  private boolean consume() { return SubtitleForwarding.consume("step.stone", 1, 2, 3, 1, 1); }
  @Test void claimsOnlyMatchingCallOnceAndPreservesIdenticalReentrantEvents() {
    try (var ignored = SubtitleForwarding.push("step.stone", 1, 2, 3, 1, 1)) {
      assertFalse(SubtitleForwarding.consume("step.grass", 1, 2, 3, 1, 1)); assertFalse(SubtitleForwarding.consume("step.stone", 9, 2, 3, 1, 1));
      assertTrue(consume()); assertFalse(consume());
      try (var nested = SubtitleForwarding.push("step.stone", 1, 2, 3, 1, 1)) { assertTrue(consume()); assertFalse(consume()); }
      assertFalse(consume());
    }
    assertFalse(consume());
    try (var ignored = SubtitleForwarding.push("step.stone", 1, 2, 3, 1, 1)) { assertTrue(consume()); }
  }
  @Test void nativeFailureCannotLeaveAStaleSuppressionToken() {
    assertThrows(IllegalStateException.class, () -> { try (var ignored = SubtitleForwarding.push("step.stone", 1, 2, 3, 1, 1)) { throw new IllegalStateException(); } });
    assertFalse(consume());
  }
}
