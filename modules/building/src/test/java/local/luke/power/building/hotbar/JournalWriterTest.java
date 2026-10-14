package local.luke.power.building.hotbar;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JournalWriterTest {
  @TempDir Path directory;

  @Test void waitingForDiskDoesNotBlockTheCallerAndCompletionMeansDurableRecord() throws Exception {
    Path path = directory.resolve("inventory.journal");
    RecoveryJournal journal = new RecoveryJournal(path);
    Future<?> write;
    synchronized (journal) {
      write = JournalWriter.append(journal, new byte[]{1, 2, 3});
      assertFalse(write.isDone());
      assertFalse(Files.exists(path));
    }
    write.get(5, TimeUnit.SECONDS);
    assertEquals(RecoveryJournal.HEADER + 3, Files.size(path));
    // Reopen and validate the persisted checksum before appending a second record.
    new RecoveryJournal(path).append(new byte[]{4});
    assertEquals(2 * RecoveryJournal.HEADER + 4, Files.size(path));
  }

  @Test void diskFailureReachesTheGameThreadWithoutChangingExistingData() throws Exception {
    Path path = directory.resolve("full.journal");
    RecoveryJournal journal = new RecoveryJournal(path, RecoveryJournal.HEADER + 1);
    journal.append(new byte[]{9});
    byte[] original = Files.readAllBytes(path);
    Future<?> write = JournalWriter.append(journal, new byte[]{8});
    assertThrows(ExecutionException.class, () -> write.get(5, TimeUnit.SECONDS));
    assertArrayEquals(original, Files.readAllBytes(path));
  }
}
