package local.luke.power.input;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

class SoundCaptionContextTest {
  @AfterEach void reset() { SoundCaptionContext.enabled(false); }
  @Test void disabledScopesPublishNothingAndDoNotBecomeActiveWhenEnabledLater() {
    SoundCaptionContext.enabled(false);
    try (var ignored = SoundCaptionContext.push("disabled")) {
      SoundCaptionContext.enabled(true); assertNull(SoundCaptionContext.claim());
    }
  }
  @Test void semanticTagIsClaimedOnceAndReentrantUnrelatedSoundsGetNoTag() {
    SoundCaptionContext.enabled(true);
    try (var ignored = SoundCaptionContext.push("placement")) {
      assertEquals("placement", SoundCaptionContext.claim()); assertNull(SoundCaptionContext.claim());
      try (var nested = SoundCaptionContext.push("chest")) { assertEquals("chest", SoundCaptionContext.claim()); assertNull(SoundCaptionContext.claim()); }
      assertNull(SoundCaptionContext.claim());
    }
    assertNull(SoundCaptionContext.claim());
  }
  @Test void throwingNativeSoundUnwindsTheTagWithoutLeakingIntoTheNextEvent() {
    SoundCaptionContext.enabled(true);
    assertThrows(IllegalStateException.class, () -> {
      try (var ignored = SoundCaptionContext.push("death")) { assertEquals("death", SoundCaptionContext.claim()); throw new IllegalStateException("native failure"); }
    });
    assertNull(SoundCaptionContext.claim());
    try (var ignored = SoundCaptionContext.push("footstep")) { assertEquals("footstep", SoundCaptionContext.claim()); }
  }
  @Test void threadsCannotClaimAnotherThreadsSemanticTag() throws Exception {
    SoundCaptionContext.enabled(true);
    try (var ignored = SoundCaptionContext.push("game thread")) {
      var result = new java.util.concurrent.atomic.AtomicReference<String>();
      Thread worker = new Thread(() -> result.set(SoundCaptionContext.claim())); worker.start(); worker.join();
      assertNull(result.get()); assertEquals("game thread", SoundCaptionContext.claim());
    }
  }
}
