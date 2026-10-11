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
    death.items = List.of("native-item-nbt"); return death;
  }
  @Test void gson289KeepsPlainFieldsAndDefaults() {
    Gson gson = new Gson(); DeathSettings defaults = gson.fromJson("{}", DeathSettings.class);
    assertFalse(defaults.enabled); assertTrue(defaults.announceLocation); assertTrue(defaults.announceItems);
    DeathRecord saved = gson.fromJson(gson.toJson(death("1")), DeathRecord.class);
    assertEquals(-12, saved.x); assertEquals(-1, saved.dimension); assertEquals(List.of("native-item-nbt"), saved.items);
  }
  @Test void restartKeepsDeathsShortIdsAndRecoveryClaims() throws Exception {
    Path path = temp.resolve("deaths.journal"); DeathJournal journal = new DeathJournal(path);
    for (int i = 1; i <= 37; i++) journal.death(death(journal.nextId()));
    assertEquals("12", journal.nextId());
    journal.recovery("1", "complete-before", "complete-after"); journal.finish("1", true);
    DeathJournal reloaded = new DeathJournal(path);
    assertEquals(37, reloaded.records().size()); assertEquals("12", reloaded.nextId());
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
}
