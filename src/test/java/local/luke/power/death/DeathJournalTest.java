package local.luke.power.death;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.List;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeathJournalTest {
  @TempDir Path temp;
  private DeathRecord death(String id) {
    DeathRecord death = new DeathRecord(); death.id = id; death.world = "world"; death.player = "player";
    death.x = -12; death.y = 64; death.z = 9; death.dimension = -1; death.cause = "Fall";
    death.items = List.of("native-item-nbt"); death.slots = List.of(0); return death;
  }
  @Test void gson289KeepsPlainFieldsAndDefaults() {
    Gson gson = new Gson(); DeathSettings defaults = gson.fromJson("{}", DeathSettings.class);
    assertFalse(defaults.enabled); assertTrue(defaults.announceLocation); assertFalse(defaults.announceItems);
    DeathRecord saved = gson.fromJson(gson.toJson(death("1")), DeathRecord.class);
    assertEquals(-12, saved.x); assertEquals(-1, saved.dimension); assertEquals(List.of("native-item-nbt"), saved.items);
  }
  @Test void restartKeepsSequentialDecimalIdsAndRecoveryClaims() throws Exception {
    Path path = temp.resolve("deaths.journal"); DeathJournal journal = new DeathJournal(path);
    for (int i = 1; i <= 37; i++) journal.death(death(journal.nextId()));
    assertEquals("38", journal.nextId());
    journal.recovery("1", "complete-before", "complete-after"); journal.finish("1", true);
    DeathJournal reloaded = new DeathJournal(path);
    assertEquals(37, reloaded.records().size()); assertEquals("38", reloaded.nextId());
    assertEquals("recovered", reloaded.state("1"));
    assertThrows(IOException.class, () -> reloaded.recovery("1", "before", "after"));
  }
  @Test void rollbackCanRetryButInterruptedRecoveryCannotDuplicate() throws Exception {
    Path path = temp.resolve("deaths.journal"); DeathJournal journal = new DeathJournal(path);
    journal.death(death("1")); journal.recovery("1", "before", "after"); journal.finish("1", false);
    journal = new DeathJournal(path); assertEquals("available", journal.state("1"));
    journal.recovery("1", "before", "after");
    DeathJournal restarted = new DeathJournal(path); assertEquals("pending", restarted.state("1"));
    assertThrows(IOException.class, () -> restarted.recovery("1", "before", "after"));
    String saved = new String(Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(saved.contains("before")); assertTrue(saved.contains("after"));
  }
  @Test void partialTailPreservesCompletedRecordsAndOriginalBytes() throws Exception {
    Path path = temp.resolve("deaths.journal"); DeathJournal journal = new DeathJournal(path); journal.death(death("1"));
    Files.write(path, new byte[]{1, 2, 3}, StandardOpenOption.APPEND); byte[] original = Files.readAllBytes(path);
    DeathJournal restarted = new DeathJournal(path); assertTrue(restarted.damaged()); assertEquals(1, restarted.records().size());
    assertThrows(IOException.class, () -> restarted.death(death("2"))); assertArrayEquals(original, Files.readAllBytes(path));
  }
  @Test void checksumFailureKeepsValidPrefixAndDoesNotRewrite() throws Exception {
    Path path = temp.resolve("deaths.journal"); DeathJournal journal = new DeathJournal(path); journal.death(death("1"));
    int firstLength = (int)Files.size(path); journal.death(death("2"));
    byte[] bytes = Files.readAllBytes(path); bytes[firstLength + 20] ^= 1; Files.write(path, bytes);
    DeathJournal restarted = new DeathJournal(path); assertTrue(restarted.damaged()); assertEquals(1, restarted.records().size());
    assertThrows(IOException.class, () -> restarted.death(death("2"))); assertArrayEquals(bytes, Files.readAllBytes(path));
  }
  @Test void changedJournalAndSymlinksStopWrites() throws Exception {
    Path path = temp.resolve("deaths.journal"); DeathJournal first = new DeathJournal(path), other = new DeathJournal(path);
    first.death(death("1")); assertThrows(IOException.class, () -> other.death(death("1")));
    Path link = temp.resolve("linked.journal"); Files.createSymbolicLink(link, path);
    assertThrows(IOException.class, () -> new DeathJournal(link));
    assertEquals(1, new DeathJournal(path).records().size());
  }
  @Test void positionsAndIndividualClaimsSurviveRestartAndBulkRecovery() throws Exception {
    Path path = temp.resolve("slots.journal"); DeathJournal journal = new DeathJournal(path);
    DeathRecord record = death("1"); record.items = List.of("a", "b", "c"); record.slots = List.of(8, 19, 39);
    journal.death(record); journal.recovery("1", List.of(1), "before", "after"); journal.finish("1", true);
    journal = new DeathJournal(path);
    assertEquals(List.of(8, 19, 39), journal.records().get(0).slots);
    assertEquals("partial", journal.state("1")); assertEquals(List.of(0, 2), journal.remaining("1"));
    DeathJournal restored = journal;
    assertThrows(IOException.class, () -> restored.recovery("1", List.of(1), "before", "after"));
    assertThrows(IOException.class, () -> restored.recovery("1", List.of(0, 0), "before", "after"));
    assertThrows(IOException.class, () -> restored.recovery("1", List.of(3), "before", "after"));
    assertThrows(IOException.class, () -> restored.recovery("1", List.of(), "before", "after"));
    assertFalse(restored.damaged());
    restored.recovery("1", "before-all", "after-all"); restored.finish("1", true);
    DeathJournal completed = new DeathJournal(path);
    assertEquals("recovered", completed.state("1")); assertTrue(completed.remaining("1").isEmpty());
  }
  @Test void partialRollbackKeepsPriorClaimsAndInterruptedSubsetBlocksWholeDeath() throws Exception {
    Path path = temp.resolve("partial.journal"); DeathJournal journal = new DeathJournal(path);
    DeathRecord record = death("1"); record.items = List.of("a", "b"); record.slots = List.of(0, 1); journal.death(record);
    journal.recovery("1", List.of(0), "before", "after"); journal.finish("1", true);
    journal.recovery("1", List.of(1), "before", "after"); journal.finish("1", false);
    DeathJournal restarted = new DeathJournal(path);
    assertEquals("partial", restarted.state("1")); assertEquals(List.of(1), restarted.remaining("1"));
    restarted.recovery("1", List.of(1), "before", "after");
    DeathJournal interrupted = new DeathJournal(path);
    assertEquals("pending", interrupted.state("1"));
    assertThrows(IOException.class, () -> interrupted.recovery("1", "before", "after"));
  }
  @Test void previousFormatIsWipedWithoutImportingRecords() throws Exception {
    Path path = temp.resolve("legacy.journal");
    // Exact version-1 framing and fields produced before slot-aware recovery.
    String death = "{\"kind\":\"death\",\"death\":{\"id\":\"1\",\"world\":\"world\",\"player\":\"player\",\"cause\":\"Fall\",\"items\":[\"native-nbt\"]}}";
    for (String json : List.of(death, "{\"kind\":\"pending\",\"id\":\"1\",\"before\":\"b\",\"after\":\"a\"}", "{\"kind\":\"recovered\",\"id\":\"1\"}")) {
      byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      var crc = new java.util.zip.CRC32(); crc.update(bytes);
      var frame = java.nio.ByteBuffer.allocate(20 + bytes.length).putInt(0x50424448).putInt(1).putInt(bytes.length).putLong(crc.getValue()).put(bytes);
      Files.write(path, frame.array(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    DeathJournal journal = new DeathJournal(path);
    assertFalse(journal.damaged()); assertTrue(journal.records().isEmpty()); assertEquals("1", journal.nextId());
    assertFalse(Files.exists(path)); journal.death(death("1"));
    assertEquals(1, new DeathJournal(path).records().size());
  }
  @Test void invalidSlotPositionsCannotBeSaved() throws Exception {
    DeathJournal journal = new DeathJournal(temp.resolve("invalid.journal"));
    for (List<Integer> slots : List.of(List.of(-1), List.of(41), List.of(0, 1))) {
      DeathRecord record = death("1"); record.slots = slots;
      assertThrows(IllegalArgumentException.class, () -> journal.death(record));
    }
    DeathRecord repeated = death("1"); repeated.items = List.of("a", "b"); repeated.slots = List.of(0, 0);
    assertThrows(IllegalArgumentException.class, () -> journal.death(repeated));
    assertTrue(journal.records().isEmpty()); assertFalse(journal.damaged());
  }
  @Test void keptDeathsClaimEveryStackInTheSameDurableDeathRecord() throws Exception {
    Path path = temp.resolve("kept.journal"); DeathJournal journal = new DeathJournal(path);
    DeathRecord record = death("1"); record.kept = true; journal.death(record);
    DeathJournal restarted = new DeathJournal(path);
    assertTrue(restarted.records().get(0).kept); assertEquals("kept", restarted.state("1"));
    assertTrue(restarted.remaining("1").isEmpty());
    assertThrows(IOException.class, () -> restarted.recovery("1", "before", "after"));
  }
}
