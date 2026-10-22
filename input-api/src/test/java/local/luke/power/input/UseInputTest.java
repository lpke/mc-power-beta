package local.luke.power.input;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class UseInputTest {
  @AfterEach void clear(){UseInput.cancelPress();UseInput.register(()->false);}
  @Test void activationRunsOneInitialUseAndRetainsHeldRepeats(){
    UseInput.register(()->true);UseInput.press();assertTrue(UseInput.consumePress());
    assertFalse(UseInput.consumePress());assertTrue(UseInput.held());
  }
  @Test void cancellationBeforeNativeDispatchDropsThePendingUse(){
    UseInput.register(()->true);UseInput.press();UseInput.cancelPress();assertFalse(UseInput.consumePress());
    UseInput.press();UseInput.register(()->false);assertFalse(UseInput.consumePress());
    UseInput.register(()->true);assertFalse(UseInput.consumePress());
  }
}
