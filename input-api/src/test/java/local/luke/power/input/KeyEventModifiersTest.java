package local.luke.power.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KeyEventModifiersTest {
  @Test void queuedCopyKeepsModifierUntilItsReleaseInEitherOrder() {
    for (boolean modifierFirst : new boolean[]{false, true}) {
      KeyEventModifiers.reconcile(0);
      KeyEventModifiers.event(29, true); KeyEventModifiers.event(46, true);
      KeyEventModifiers.begin();
      try { assertEquals(Chord.CTRL, KeyEventModifiers.mask(0)); }
      finally { KeyEventModifiers.end(); }
      KeyEventModifiers.event(modifierFirst ? 29 : 46, false);
      KeyEventModifiers.event(modifierFirst ? 46 : 29, false);
      KeyEventModifiers.begin();
      try { assertEquals(0, KeyEventModifiers.mask(Chord.CTRL)); }
      finally { KeyEventModifiers.end(); }
    }
  }
  @Test void overlappingSidesAndNestedScopesKeepMousePollingIndependent() {
    KeyEventModifiers.reconcile(0);
    KeyEventModifiers.event(29, true); KeyEventModifiers.event(157, true);
    KeyEventModifiers.event(29, false); KeyEventModifiers.event(42, true);
    assertEquals(Chord.ALT, KeyEventModifiers.mask(Chord.ALT));
    KeyEventModifiers.begin(); KeyEventModifiers.begin();
    try {
      assertEquals(Chord.CTRL | Chord.SHIFT, KeyEventModifiers.mask(0));
      KeyEventModifiers.event(157, false);
      assertEquals(Chord.SHIFT, KeyEventModifiers.mask(0));
      KeyEventModifiers.reconcile(32);
      assertEquals(Chord.ALT, KeyEventModifiers.mask(0));
    } finally { KeyEventModifiers.end(); KeyEventModifiers.end(); KeyEventModifiers.reconcile(0); }
    assertEquals(0, KeyEventModifiers.mask(0));
  }
}
