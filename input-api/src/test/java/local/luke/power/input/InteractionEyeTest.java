package local.luke.power.input;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class InteractionEyeTest {
  @AfterEach void reset() { InteractionEye.register((actor, delta, fallback) -> fallback); }

  @Test void nativeRaysKeepTheirOwnFallbackOutsideThePoseOwner() {
    Object owner = new Object(), stranger = new Object();
    InteractionEye.register((actor, delta, fallback) -> actor == owner ? .35 : fallback);
    assertEquals(.35, InteractionEye.offset(owner, .5f, 0));
    assertEquals(0, InteractionEye.offset(stranger, .5f, 0));
    assertEquals(1.22, InteractionEye.offset(stranger, .5f, 1.22));
  }

  @Test void zeroOverridesTheNativeItemOffsetInsteadOfAddingItAgain() {
    InteractionEye.register((actor, delta, fallback) -> 0);
    assertEquals(0, InteractionEye.offset(new Object(), 1, 1.22));
  }

  @Test void frameInterpolationAndNativeFallbackReachTheProvider() {
    Object owner = new Object();
    InteractionEye.register((actor, delta, fallback) -> {
      assertSame(owner, actor); assertEquals(.35, fallback);
      return 1.62 + (.4 - 1.62) * delta - .4;
    });
    assertEquals(1.22, InteractionEye.offset(owner, 0, .35), 1e-12);
    assertEquals(.61, InteractionEye.offset(owner, .5f, .35), 1e-12);
    assertEquals(0, InteractionEye.offset(owner, 1, .35), 1e-12);
  }

  @Test void invalidQueriesAndProviderResultsRetainTheNativeOrigin() {
    InteractionEye.register((actor, delta, fallback) -> { fail("invalid query reached provider"); return 0; });
    assertEquals(.35, InteractionEye.offset(null, 1, .35));
    assertEquals(.35, InteractionEye.offset(new Object(), Float.NaN, .35));
    InteractionEye.register((actor, delta, fallback) -> Double.NaN);
    assertEquals(.35, InteractionEye.offset(new Object(), 1, .35));
    InteractionEye.register((actor, delta, fallback) -> Double.POSITIVE_INFINITY);
    assertEquals(.35, InteractionEye.offset(new Object(), 1, .35));
  }
}
