package local.luke.power.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PressModifiersTest {
  @Test void releasingModifierDoesNotFallThroughToPlainKey() {
    var presses = new PressModifiers();
    assertEquals(Chord.CTRL, presses.update(35, true, Chord.CTRL));
    assertEquals(Chord.CTRL, presses.update(35, true, 0));
    presses.update(35, false, 0);
    assertEquals(0, presses.update(35, true, 0));
  }
  @Test void pressingModifierDoesNotTurnHeldKeyIntoAnotherAction() {
    var presses = new PressModifiers();
    assertEquals(0, presses.update(35, true, 0));
    assertEquals(0, presses.update(35, true, Chord.SHIFT));
    presses.update(35, false, Chord.SHIFT);
    assertEquals(Chord.SHIFT, presses.update(35, true, Chord.SHIFT));
  }
  @Test void mouseButtonsKeepIndependentPresses() {
    var presses = new PressModifiers();
    assertEquals(Chord.ALT, presses.update(-98, true, Chord.ALT));
    assertEquals(Chord.CTRL, presses.update(-99, true, Chord.CTRL));
    assertEquals(Chord.ALT, presses.update(-98, true, 0));
    presses.update(-98, false, 0);
    assertEquals(0, presses.update(-98, true, 0));
  }
}
