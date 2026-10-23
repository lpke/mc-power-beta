package local.luke.power.creative;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CreativeTabsDataTest {
  private CreativeTabsData layout() {
    CreativeTabsData data = CreativeTabsData.read(CreativeTabsData.defaults()); data.customized(true);
    data.append(CreativeTabsData.tab("blocks", "Blocks", "1", List.of("1", "", "35:4", "mod:item")));
    return data;
  }
  @Test void gson289RoundTripPreservesHolesOrderAndUnavailableItems() {
    var original = layout();
    JsonObject serialized = new Gson().fromJson(new Gson().toJson(original.json()), JsonObject.class);
    var read = CreativeTabsData.read(serialized);
    assertEquals(original.json(), read.json());
    assertEquals(List.of("1", "", "35:4", "mod:item"), CreativeTabsData.slots(read.tab(0)));
    read.tab(0).addProperty("title", "Changed");
    assertEquals("Blocks", original.tab(0).get("title").getAsString());
  }
  @Test void unsupportedAndMalformedLayoutsStayUntouched() {
    JsonObject future = layout().json(); future.addProperty("format", 2); JsonObject before = future.deepCopy();
    assertThrows(IllegalArgumentException.class, () -> CreativeTabsData.read(future)); assertEquals(before, future);
    JsonObject partial = layout().json(); partial.getAsJsonArray("tabs").get(0).getAsJsonObject().remove("slots");
    assertThrows(IllegalArgumentException.class, () -> CreativeTabsData.read(partial));
    JsonObject fractional = layout().json(); fractional.addProperty("format", 1.1);
    assertThrows(IllegalArgumentException.class, () -> CreativeTabsData.read(fractional));
  }
  @Test void editingAndReorderingCopiesCannotOverwriteTheSource() {
    var source = layout(); var draft = CreativeTabsData.read(source.json());
    draft.append(CreativeTabsData.tab("tools", "Tools", "257", List.of("257", "258")));
    draft.move(1, 0);
    assertEquals("tools", draft.tab(0).get("id").getAsString());
    assertEquals("blocks", draft.tab(1).get("id").getAsString());
    assertEquals(1, source.size());
    draft.remove(0); assertEquals(source.json(), draft.json());
  }
  @Test void dragPreservesEveryCellAndSelectedSpacingAcrossOverlaps() {
    List<String> source = List.of("1", "2", "", "3", "4", "5", "6", "7");
    List<String> result = CreativeTabsData.moveCells(source, List.of(0, 2, 3), 2);
    assertEquals("1", result.get(2)); assertEquals("", result.get(4)); assertEquals("3", result.get(5));
    assertEquals(sorted(source), sorted(result));
    assertEquals(source, CreativeTabsData.moveCells(result, List.of(2, 4, 5), -2));
    assertEquals(source, CreativeTabsData.moveCells(source, List.of(0, 2, 3), 0));
  }
  @Test void dragRejectsOutOfBoundsWithoutChangingAnyCell() {
    List<String> source = List.of("1", "2", "", "3");
    assertThrows(IllegalArgumentException.class, () -> CreativeTabsData.moveCells(source, List.of(0, 3), 1));
    assertThrows(IllegalArgumentException.class, () -> CreativeTabsData.moveCells(source, List.of(0), -1));
    assertEquals(List.of("1", "2", "", "3"), source);
  }
  @Test void duplicateStableIdsAndOversizedLayoutsAreRejected() {
    var data = layout(); data.append(data.tab(0));
    assertThrows(IllegalArgumentException.class, () -> CreativeTabsData.read(data.json()));
    JsonObject large = CreativeTabsData.defaults(); large.addProperty("unknown", "x".repeat(1048576));
    assertThrows(IllegalArgumentException.class, () -> CreativeTabsData.read(large));
  }
  private static List<String> sorted(List<String> cells) { return cells.stream().sorted().toList(); }
}
