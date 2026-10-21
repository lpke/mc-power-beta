package local.luke.power.storage;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.*;
import java.nio.file.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class JournalQueueTest {
  @TempDir Path directory;
  ThreadPoolExecutor executor() throws Exception {
    var field = JournalQueue.class.getDeclaredField("IO"); field.setAccessible(true);
    return (ThreadPoolExecutor)field.get(null);
  }
  @Test void submissionsCopyBytesAndCancellationCannotInterruptAnAdmittedRecord() throws Exception {
    var started = new CountDownLatch(1); var release = new CountDownLatch(1);
    executor().execute(() -> { started.countDown(); try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
    assertTrue(started.await(5, TimeUnit.SECONDS));
    Path file = directory.resolve("snapshots.journal"); byte[] bytes = {1, 2, 3};
    try {
      var durable = JournalQueue.append(new AppendJournal(file), bytes); bytes[0] = 99; durable.cancel(true);
    } finally { release.countDown(); }
    executor().submit(() -> {}).get(5, TimeUnit.SECONDS);
    ByteBuffer record = ByteBuffer.wrap(Files.readAllBytes(file)); record.position(AppendJournal.HEADER);
    assertEquals(1, record.get()); assertEquals(2, record.get()); assertEquals(3, record.get());
    new AppendJournal(file).append(new byte[]{4}); // The canceled request still left a complete valid envelope.
  }
  @Test void queueAndPayloadAreBoundedRatherThanGrowingWithRepeatedRequests() throws Exception {
    var started = new CountDownLatch(1); var release = new CountDownLatch(1);
    executor().execute(() -> { started.countDown(); try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
    assertTrue(started.await(5, TimeUnit.SECONDS));
    var journal = new AppendJournal(directory.resolve("bounded.journal"));
    try {
      for (int i = 0; i < 4; i++) JournalQueue.append(journal, new byte[]{(byte)i});
      assertThrows(RejectedExecutionException.class, () -> JournalQueue.append(journal, new byte[]{9}));
      assertThrows(IllegalArgumentException.class, () -> JournalQueue.append(journal, new byte[AppendJournal.MAX_RECORD + 1]));
    } finally { release.countDown(); }
    // Wait for the admitted records before the next test borrows this process-wide writer.
    JournalQueue.awaitQueued();
  }
}
