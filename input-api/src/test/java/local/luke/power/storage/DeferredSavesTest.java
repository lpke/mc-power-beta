package local.luke.power.storage;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class DeferredSavesTest {
  private static JsonObject value(int number) {
    JsonObject result = new JsonObject(); result.addProperty("value", number); return result;
  }
  private static void await(CountDownLatch latch) throws IOException {
    try { if (!latch.await(5, TimeUnit.SECONDS)) throw new IOException("Timed out waiting for test writer"); }
    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
  }

  @Test void inputNeverWaitsForABlockedWriterAndSnapshotsAreIndependent() throws Exception {
    var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
    var writes = new ArrayList<Map<String, JsonObject>>();
    var queue = new DeferredSaves(batch -> {
      if (writes.isEmpty()) { entered.countDown(); await(release); }
      writes.add(batch);
    });
    var caller = Executors.newSingleThreadExecutor();
    try {
      queue.submit("first", value(1)); await(entered);
      JsonObject next = value(2);
      caller.submit(() -> queue.submit("second", next)).get(1, TimeUnit.SECONDS);
      next.addProperty("value", 99);
      queue.submit("second", value(3));
      JsonObject independent = value(4); queue.submit("third", independent); independent.addProperty("value", 99);
      release.countDown(); queue.flush();
      assertEquals(2, writes.size());
      assertEquals(3, writes.get(1).get("second").get("value").getAsInt());
      assertEquals(4, writes.get(1).get("third").get("value").getAsInt());
      assertEquals(1, writes.get(0).get("first").get("value").getAsInt());
    } finally { release.countDown(); caller.shutdownNow(); queue.flush(); }
  }

  @Test void flushWaitsForTheDiskWrite() throws Exception {
    var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
    var queue = new DeferredSaves(batch -> { entered.countDown(); await(release); });
    var caller = Executors.newSingleThreadExecutor();
    try {
      queue.submit("test", value(1)); await(entered);
      Future<?> flushed = caller.submit(() -> { queue.flush(); return null; });
      assertThrows(TimeoutException.class, () -> flushed.get(50, TimeUnit.MILLISECONDS));
      release.countDown(); flushed.get(5, TimeUnit.SECONDS);
    } finally { release.countDown(); caller.shutdownNow(); queue.flush(); }
  }

  @Test void failuresRetainTheLatestValuesForRetryAndAreReportedOnce() throws Exception {
    var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
    var broken = new AtomicBoolean(true); var restored = new HashMap<String, JsonObject>();
    var queue = new DeferredSaves(batch -> {
      if (broken.get()) { entered.countDown(); await(release); throw new IOException("injected failure"); }
      restored.putAll(batch);
    });
    queue.submit("a", value(1)); await(entered);
    queue.submit("a", value(2)); queue.submit("b", value(3)); release.countDown();
    assertThrows(IOException.class, queue::flush);
    assertNotNull(queue.takeFailure()); assertNull(queue.takeFailure());
    broken.set(false); queue.flush();
    assertEquals(2, restored.get("a").get("value").getAsInt());
    assertEquals(3, restored.get("b").get("value").getAsInt());
    queue.flush();
  }
}
