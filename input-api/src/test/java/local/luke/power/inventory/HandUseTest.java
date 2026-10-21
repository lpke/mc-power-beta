package local.luke.power.inventory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HandUseTest {
  @Test void aManualSwapInvalidatesHeldPlacementEvenWithEqualStackData() {
    long before=HandUse.manualRevision();HandUse.swapping();assertNotEquals(before,HandUse.manualRevision());
  }
  @Test void nestedCallbacksRestoreTheirOwnerAndSetterPermissionAfterFailure() {
    Object inventory=new Object(), other=new Object();
    assertEquals(HandUse.Hand.MAIN,HandUse.hand(inventory));
    try(HandUse ignored=HandUse.begin(inventory,HandUse.Hand.OFF,true)) {
      assertEquals(HandUse.Hand.OFF,HandUse.hand(inventory));
      assertTrue(HandUse.routesSelectedSetter(inventory));
      assertEquals(HandUse.Hand.MAIN,HandUse.hand(other));
      assertThrows(IllegalArgumentException.class,()-> {
        try(HandUse nested=HandUse.begin(inventory,HandUse.Hand.MAIN,false)) {
          assertFalse(HandUse.routesSelectedSetter(inventory));throw new IllegalArgumentException();
        }
      });
      assertEquals(HandUse.Hand.OFF,HandUse.hand(inventory));
    }
    assertEquals(HandUse.Hand.MAIN,HandUse.hand(inventory));
    assertFalse(HandUse.routesSelectedSetter(inventory));
  }
  @Test void scopesCannotLeakToAWorkerOrCloseOutOfOrder() throws Exception {
    Object inventory=new Object();HandUse outer=HandUse.begin(inventory,HandUse.Hand.OFF,false);
    HandUse inner=HandUse.begin(inventory,HandUse.Hand.OFF,true);
    try {
      assertThrows(IllegalStateException.class,outer::close);
      var seen=new java.util.concurrent.atomic.AtomicReference<HandUse.Hand>();
      Thread worker=new Thread(()->seen.set(HandUse.hand(inventory)));worker.start();worker.join();
      assertEquals(HandUse.Hand.MAIN,seen.get());
    } finally { inner.close();outer.close(); }
  }
}
