package local.luke.power.render;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class CullingRenderersTest {
  static class Base {}
  static class Child extends Base {}
  static class Missing {}

  @Test void workerReadsNeverResolveOrMutateTheNativeRegistry() throws Exception {
    var source = new HashMap<Class<?>, Object>();
    source.put(Base.class, new Object()); source.put(Missing.class, null);
    var snapshot = new CullingRenderers(); snapshot.publish(source);
    Thread worker = new Thread(() -> {
      for (int i = 0; i < 10000; i++) {
        assertTrue(snapshot.hasRenderer(Child.class));
        assertFalse(snapshot.hasRenderer(Missing.class));
      }
    });
    var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
    worker.setUncaughtExceptionHandler((thread, error) -> failure.set(error));
    worker.start();
    for (int i = 0; i < 10000; i++) {
      for (Object renderer : source.values()) assertTrue(renderer == null || renderer.getClass() == Object.class);
      snapshot.resolved(Child.class, true);
    }
    worker.join(); assertNull(failure.get());
    assertEquals(2, source.size()); assertFalse(source.containsKey(Child.class));
    source.clear(); assertTrue(snapshot.hasRenderer(Base.class));
    snapshot.publish(source); assertFalse(snapshot.hasRenderer(Base.class));
  }
}
