package local.luke.power.worldedit.carry;

import java.nio.file.*;
import local.luke.power.worldedit.core.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CarryJournalTest {
  @TempDir Path dir;
  CarryJournal sample() { return new CarryJournal(new Pos(10,64,5),0,new BlockValue(54,2,new byte[]{10,0,1,5})); }
  @Test void everyStageRetainsExactInventorySnapshot() throws Exception {
    var record=sample();Path path=dir.resolve("carry.json");
    for(String phase:new String[]{"removing","held","placing","complete"}) {
      record.phase=phase;record.target=new Pos(20,64,5);record.write(path);
      var loaded=CarryJournal.read(path);assertEquals(phase,loaded.phase);assertTrue(record.value().same(loaded.value()));
    }
  }
  @Test void corruptionInDataOrPhaseIsRejectedAndPreserved() throws Exception {
    Path path=dir.resolve("carry.json");sample().write(path);String original=Files.readString(path);
    for(String changed:new String[]{original.replace("removing","complete"),original.replace("\"block\":54","\"block\":61"),original.replace("\"metadata\":2","\"metadata\":1")}) {
      assertNotEquals(original,changed);Files.writeString(path,changed);
      assertThrows(java.io.IOException.class,()->CarryJournal.read(path));assertEquals(changed,Files.readString(path));
    }
  }
  @Test void unsafeTargetDoesNotReplaceExistingJournal() throws Exception {
    Path path=dir.resolve("carry.json");var record=sample();record.write(path);String original=Files.readString(path);
    record.phase="placing";assertThrows(IllegalArgumentException.class,()->record.write(path));
    assertEquals(original,Files.readString(path));
  }
  @Test void rejectsSymlinkBeforeWriting() throws Exception {
    Path target=dir.resolve("keep");Files.writeString(target,"safe");Path link=dir.resolve("link");Files.createSymbolicLink(link,target);
    assertThrows(java.io.IOException.class,()->sample().write(link));assertEquals("safe",Files.readString(target));
  }
}
