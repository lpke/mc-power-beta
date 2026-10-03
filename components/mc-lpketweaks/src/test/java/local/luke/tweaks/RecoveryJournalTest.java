package local.luke.tweaks;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import local.luke.tweaks.hotbar.RecoveryJournal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecoveryJournalTest {
  @TempDir Path folder;

  @Test
  void appendPreservesAllPreviousSnapshotsAcrossReopen() throws Exception {
    Path file = folder.resolve("backup.journal");
    RecoveryJournal journal = new RecoveryJournal(file);
    journal.append(new byte[] {1, 2, 3});
    byte[] before = Files.readAllBytes(file);
    new RecoveryJournal(file).append(new byte[] {4, 5});
    byte[] after = Files.readAllBytes(file);
    assertArrayEquals(before, Arrays.copyOf(after, before.length));
    assertEquals(before.length + RecoveryJournal.HEADER + 2, after.length);
  }

  @Test
  void truncatedRecordRefusesFurtherWritesAndPreservesEvidence() throws Exception {
    Path file = folder.resolve("backup.journal");
    new RecoveryJournal(file).append(new byte[] {1, 2, 3});
    byte[] data = Files.readAllBytes(file);
    data = Arrays.copyOf(data, data.length - 1);
    Files.write(file, data);
    assertThrows(IOException.class, () -> new RecoveryJournal(file).append(new byte[] {4}));
    assertArrayEquals(data, Files.readAllBytes(file));
  }

  @Test
  void checksumFailureCannotBeSilentlyAppendedOrRepaired() throws Exception {
    Path file = folder.resolve("backup.journal");
    new RecoveryJournal(file).append(new byte[] {1, 2, 3});
    byte[] data = Files.readAllBytes(file);
    data[data.length - 1] ^= 1;
    Files.write(file, data);
    assertThrows(IOException.class, () -> new RecoveryJournal(file).append(new byte[] {4}));
    assertArrayEquals(data, Files.readAllBytes(file));
  }

  @Test
  void quotaKeepsAllExistingRecordsAndRefusesNewOne() throws Exception {
    Path file = folder.resolve("backup.journal");
    RecoveryJournal journal = new RecoveryJournal(file, 25);
    journal.append(new byte[] {1});
    byte[] before = Files.readAllBytes(file);
    assertThrows(IOException.class, () -> journal.append(new byte[] {2}));
    assertArrayEquals(before, Files.readAllBytes(file));
  }

  @Test
  void missingPermissionsOrInvalidDirectoryCannotProduceASnapshot() throws Exception {
    Path file = folder.resolve("not-a-directory");
    Files.writeString(file, "preserve");
    assertThrows(
        IOException.class,
        () -> new RecoveryJournal(file.resolve("backup")).append(new byte[] {1}));
    assertEquals("preserve", Files.readString(file));
  }

  @Test
  void oversizeDataIsRejectedBeforeCreatingAFile() {
    Path file = folder.resolve("backup.journal");
    assertThrows(
        IOException.class,
        () -> new RecoveryJournal(file).append(new byte[RecoveryJournal.MAX_RECORD + 1]));
    assertFalse(Files.exists(file));
  }
}
