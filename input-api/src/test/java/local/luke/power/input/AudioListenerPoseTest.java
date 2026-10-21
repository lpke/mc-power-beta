package local.luke.power.input;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;

class AudioListenerPoseTest {
  @AfterEach void reset() { AudioListenerPose.interested(false); AudioListenerPose.register((world, x, y, z, fx, fz) -> {}); }
  @Test void disabledCaptionConsumerReceivesNoListenerWork() {
    AtomicInteger count = new AtomicInteger(); AudioListenerPose.register((world, x, y, z, fx, fz) -> count.incrementAndGet());
    AudioListenerPose.interested(false); AudioListenerPose.publish(new Object(), 1, 2, 3, 0, 1); assertEquals(0, count.get());
  }
  @Test void listenerBridgeRetainsOwnerDoublePositionsAndExactAudioOrientation() {
    Object owner = new Object(); AtomicInteger count = new AtomicInteger();
    AudioListenerPose.register((world, x, y, z, fx, fz) -> {
      assertSame(owner, world); assertEquals(30_000_000.125, x); assertEquals(65.375, y); assertEquals(-30_000_000.25, z);
      assertEquals(-.5F, fx); assertEquals(.866F, fz); count.incrementAndGet();
    });
    AudioListenerPose.interested(true); AudioListenerPose.publish(owner, 30_000_000.125, 65.375, -30_000_000.25, -.5F, .866F);
    assertEquals(1, count.get());
  }
}
