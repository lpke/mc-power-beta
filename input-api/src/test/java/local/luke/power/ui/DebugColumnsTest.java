package local.luke.power.ui;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DebugColumnsTest {
  @Test void movingBetweenColumnsPreservesFutureIdsAndGaps() {
    var initial = new DebugColumns(List.of("version", "future", "", "x"), List.of("usedMemory"));
    var moved = initial.move("x",0,1,0,true,id->!id.equals("future"));
    assertEquals(List.of("version","future",""),moved.left());
    assertEquals(List.of("x","usedMemory"),moved.right());
    var gap = moved.move("usedMemory",1,0,2,false,id->!id.equals("future"));
    assertEquals(List.of("version","future","","usedMemory"),gap.left());
    assertEquals(List.of("x"),gap.right());
    assertEquals(initial.complete(),initial.complete().complete());
  }
  @Test void fillGapInFullColumnWithoutExceedingBounds() {
    var target = new ArrayList<>(Collections.nCopies(256,""));
    var moved = new DebugColumns(List.of("version"),target).move("version",0,1,200,false,id->true);
    assertEquals(256,moved.right().size()); assertEquals("version",moved.right().get(200));
    assertThrows(IllegalArgumentException.class,()->new DebugColumns(List.of("version"),target).move("version",0,1,0,true,id->true));
    assertThrows(IllegalArgumentException.class,()->new DebugColumns(List.of("version"),List.of("version")));
  }
  @Test void rendererUsesSavedColumnAndRowAfterEveryMove() {
    var columns = new DebugColumns(List.of("version","x"),List.of("usedMemory"));
    columns=columns.move("version",0,1,0,true,id->true).move("usedMemory",1,0,3,false,id->true);
    var source=List.of(new DebugOverlay.Line("Minecraft Beta",2,2,0),new DebugOverlay.Line("x: 1",2,64,0),new DebugOverlay.Line("Used memory: 10%",200,2,0));
    var placed=DebugOverlay.arrangeOrdered(source,640,String::length,columns.left(),columns.right(),id->true,1);
    var version=placed.stream().filter(line->line.id().equals("version")).findFirst().orElseThrow();
    var memory=placed.stream().filter(line->line.id().equals("usedMemory")).findFirst().orElseThrow();
    assertTrue(version.x()>320); assertEquals(2,version.y());
    assertEquals(2,memory.x()); assertEquals(32,memory.y());
  }
  @org.junit.jupiter.api.Test void rangeMovesPreserveGapsUnknownIdsAndBothColumns() {
    var original = new DebugColumns(java.util.List.of("version", "", "future.provider", "terrain"), java.util.List.of("", "", "", "usedMemory"));
    var moved = original.moveRange(0, 0, 2, 1, 0, false);
    assertEquals(java.util.List.of("", "", "", "terrain"), moved.left());
    assertEquals(java.util.List.of("version", "", "future.provider", "usedMemory"), moved.right());
    assertEquals(original, original.moveRange(0, 0, 2, 0, 1, true));
    assertEquals(java.util.List.of("terrain", "version", "", "future.provider"), original.moveRange(0, 0, 2, 0, 4, true).left());
  }
  @org.junit.jupiter.api.Test void blankBoundariesOnlyInsertBesidePopulatedRows() {
    var rows = java.util.List.of("version", "", "", "terrain");
    assertFalse(DebugColumns.insertion(rows, 1, 11, 12));
    assertFalse(DebugColumns.insertion(rows, 2, 0, 12));
    assertTrue(DebugColumns.insertion(rows, 1, 0, 12));
    assertTrue(DebugColumns.insertion(rows, 2, 11, 12));
    assertFalse(DebugColumns.insertion(rows, 2, 6, 12));
  }
}
