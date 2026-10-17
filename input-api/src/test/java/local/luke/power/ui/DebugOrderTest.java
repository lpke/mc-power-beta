package local.luke.power.ui;

import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DebugOrderTest {
  @Test void identitiesCoverNativeAndBundledLinesWithoutCapturingRightSideText() {
    for (var line : DebugLines.values()) assertEquals(line.id, DebugLines.identify(line.sample, 2, 2), line.id);
    assertEquals("", DebugLines.identify("Used memory: 10%", 2, 2));
    assertEquals("", DebugLines.identify("Allocated memory: 50%", 140, 12));
    assertEquals("version", DebugLines.identify("Power Beta 2.4.0 (120 fps)", 2, 2));
    assertEquals("world", DebugLines.identify("MultiplayerChunkCache: 1024", 2, 42));
  }
  @Test void normalisationRetainsFutureIdsAndRejectsMalformedOrDuplicateRows() {
    var rows = new ArrayList<>(List.of("future.provider", "", "version"));
    var complete = DebugOrder.complete(rows);
    assertEquals(rows, complete.subList(0, rows.size()));
    assertEquals(DebugLines.values().length + 2, complete.size());
    assertEquals(complete, DebugOrder.complete(complete));
    assertThrows(IllegalArgumentException.class, () -> DebugOrder.read(JsonParser.parseString("[1]")));
    assertThrows(IllegalArgumentException.class, () -> DebugOrder.read(JsonParser.parseString("[\"x\",\"x\"]")));
    assertThrows(IllegalArgumentException.class, () -> DebugOrder.validate(Collections.nCopies(257, "")));
  }
  @Test void edgeDropsInsertAndCentreDropsUseGapsWithoutMovingOtherRows() {
    var source = List.of("version", "terrain", "entities");
    assertEquals(List.of("terrain", "entities", "version"), DebugOrder.move(source, "version", 3, true, id -> true));
    assertEquals(List.of("entities", "version", "terrain"), DebugOrder.move(source, "entities", 0, true, id -> true));
    assertEquals(List.of("", "terrain", "version", "entities"), DebugOrder.move(List.of("version", "terrain", "", "entities"), "version", 2, false, id -> true));
    assertEquals(List.of("", "terrain", "entities", "", "", "version"), DebugOrder.move(source, "version", 5, false, id -> true));
    assertEquals(List.of("version", "terrain", "entities"), source);
  }
  @Test void movesPreserveDisabledFutureLinesAndBoundDistantDrops() {
    var moved = DebugOrder.move(List.of("version", "future.provider", "", "terrain"), "terrain", 0, true, id -> !id.startsWith("future"));
    assertEquals(List.of("terrain", "version", "future.provider", ""), moved);
    var far = DebugOrder.move(List.of("version", "terrain"), "version", 10000, false, id -> true);
    assertEquals(256, far.size()); assertEquals("version", far.get(255)); assertEquals("terrain", far.get(1));
  }
  @Test void savedOrderAndGapsDriveTheSameLayoutWithoutMovingTheRightColumn() {
    var source = List.of(new DebugOverlay.Line("x: 128", 2, 64, 0), new DebugOverlay.Line("Biome: Forest", 2, 120, 0),
        new DebugOverlay.Line("Used memory: 10%", 180, 2, 100));
    var placed = DebugOverlay.arrangeOrdered(source, 320, String::length, List.of("biome", "", "x"), id -> true, 2);
    assertEquals(source.get(2), placed.get(0));
    assertEquals(2, placed.get(1).y()); assertEquals("biome", placed.get(1).id());
    assertEquals(42, placed.get(2).y()); assertEquals("x", placed.get(2).id());
    assertEquals(2, DebugOverlay.arrangeOrdered(source, 320, String::length, List.of("biome", "x"), id -> !id.equals("x"), 1).size());
  }
}
