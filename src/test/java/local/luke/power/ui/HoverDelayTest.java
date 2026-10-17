package local.luke.power.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HoverDelayTest {
  @Test void renderingCannotAdvanceDelayAndChangingTargetOrFocusRestartsIt() {
    HoverDelay delay = new HoverDelay();assertFalse(delay.ready("setting"));
    for (int i=0;i<1000;i++) assertFalse(delay.ready("setting"));
    for (int i=0;i<12;i++) {delay.tick();assertFalse(delay.ready("setting"));}
    delay.tick();assertTrue(delay.ready("setting"));assertFalse(delay.ready("setting.reset"));
    for (int i=0;i<13;i++) delay.tick();assertTrue(delay.ready("setting.reset"));
    delay.clear();assertFalse(delay.ready("setting.reset"));assertFalse(delay.ready(""));
  }
}
