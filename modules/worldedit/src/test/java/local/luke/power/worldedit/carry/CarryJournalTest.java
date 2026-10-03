package local.luke.power.worldedit.carry;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import local.luke.power.worldedit.core.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CarryJournalTest {
  @TempDir Path dir;

  CarryJournal sample() {
    return new CarryJournal(new Pos(10, 64, 5), 0, new BlockValue(54, 2, new byte[] {10, 0, 1, 5}));
  }

  @Test
  void everyStageRetainsExactInventorySnapshot() throws Exception {
    var record = sample();
    Path path = dir.resolve("carry.json");
    for (String phase : new String[] {"removing", "held", "placing", "complete"}) {
      record.phase = phase;
      record.target = new Pos(20, 64, 5);
      record.write(path);
      var loaded = CarryJournal.read(path);
      assertEquals(phase, loaded.phase);
      assertTrue(record.value().same(loaded.value()));
    }
  }

  @Test
  void corruptionInDataOrPhaseIsRejectedAndPreserved() throws Exception {
    Path path = dir.resolve("carry.json");
    sample().write(path);
    String original = Files.readString(path);
    for (String changed :
        new String[] {
          original.replace("removing", "complete"),
          original.replace("\"block\":54", "\"block\":61"),
          original.replace("\"metadata\":2", "\"metadata\":1")
        }) {
      assertNotEquals(original, changed);
      Files.writeString(path, changed);
      assertThrows(java.io.IOException.class, () -> CarryJournal.read(path));
      assertEquals(changed, Files.readString(path));
    }
  }

  @Test
  void unsafeTargetDoesNotReplaceExistingJournal() throws Exception {
    Path path = dir.resolve("carry.json");
    var record = sample();
    record.write(path);
    String original = Files.readString(path);
    record.phase = "placing";
    assertThrows(IllegalArgumentException.class, () -> record.write(path));
    assertEquals(original, Files.readString(path));
  }

  @Test
  void rejectsSymlinkBeforeWriting() throws Exception {
    Path target = dir.resolve("keep");
    Files.writeString(target, "safe");
    Path link = dir.resolve("link");
    Files.createSymbolicLink(link, target);
    assertThrows(java.io.IOException.class, () -> sample().write(link));
    assertEquals("safe", Files.readString(target));
  }

  @Test
  void doubleChestRoundtripRetainsBothInventoriesAndTargets() throws Exception {
    var record =
        new CarryJournal(
            new Pos(15, 64, 5),
            0,
            new BlockValue(54, 2, new byte[] {1, 2}),
            new Pos(16, 64, 5),
            new BlockValue(54, 3, new byte[] {3, 4}));
    Path path = dir.resolve("double.json");
    for (String phase : new String[] {"removing", "held", "placing", "complete"}) {
      record.targets(new Pos(5, 64, 15), new Pos(5, 64, 16), 0);
      record.phase = phase;
      record.write(path);
      var read = CarryJournal.read(path);
      assertEquals(2, read.size());
      for (int i = 0; i < 2; i++) {
        assertTrue(record.value(i).same(read.value(i)));
        assertEquals(record.source(i), read.source(i));
        assertEquals(record.target(i), read.target(i));
      }
    }
    var changed = com.google.gson.JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    changed.getAsJsonObject("second").addProperty("payload", "AQI=");
    Files.writeString(path, changed.toString());
    assertThrows(java.io.IOException.class, () -> CarryJournal.read(path));
  }

  @Test
  void refusesMissingOrNonAdjacentDoubleChestTargetsBeforeReplacingJournal() throws Exception {
    var record =
        new CarryJournal(
            new Pos(1, 64, 0),
            0,
            new BlockValue(54, 0, new byte[] {1}),
            new Pos(2, 64, 0),
            new BlockValue(54, 0, new byte[] {2}));
    Path path = dir.resolve("double.json");
    record.write(path);
    String saved = Files.readString(path);
    record.phase = "placing";
    for (Pos target : new Pos[] {null, new Pos(5, 64, 0), new Pos(1, 65, 0)}) {
      record.targets(new Pos(1, 64, 0), target, 0);
      assertThrows(IllegalArgumentException.class, () -> record.write(path));
      assertEquals(saved, Files.readString(path));
    }
  }
}
