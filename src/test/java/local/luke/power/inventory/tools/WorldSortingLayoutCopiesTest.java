package local.luke.power.inventory.tools;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldSortingLayoutCopiesTest {
  @TempDir Path directory;
  private static final String FIRST = "00000000-0000-0000-0000-000000000001", SECOND = "00000000-0000-0000-0000-000000000002";
  @Test void duplicateImportsItsSourceLayoutWithOpaquePresetsAndRejectsWrongIdentity() throws Exception {
    Path config = directory.resolve("config/power-beta.json"); Files.createDirectories(config.getParent());
    Files.writeString(config, "{\"settings\":{\"inventoryTools\":{\"protectedSlots\":[9],\"preferredItems\":{\"11\":\"1\"}},\"sortingLayouts\":{\"format\":999,\"future\":true}}}");
    var snapshot = WorldSortingLayoutCopies.snapshot(directory, List.of(FIRST));
    JsonObject manifest = new JsonObject(); manifest.addProperty("duplicateWorldId", SECOND); manifest.add("sortingLayouts", snapshot);
    Files.writeString(directory.resolve("power-beta-copy.json"), new Gson().toJson(manifest));
    assertEquals(snapshot.getAsJsonObject("worlds").get(FIRST), WorldSortingLayoutCopies.imported(directory, SECOND));
    assertNull(WorldSortingLayoutCopies.imported(directory, FIRST));
    assertEquals(999, WorldSortingLayoutCopies.imported(directory, SECOND).getAsJsonObject("presets").get("format").getAsInt());
  }
  @Test void missingSettingsStillArchivesAnEmptyLayoutAndUnopenedCopiesRetainTheirScopedLayout() throws Exception {
    Path config = directory.resolve("config/power-beta.json"); Files.createDirectories(config.getParent()); Files.writeString(config, "{}");
    assertEquals(0, WorldSortingLayoutCopies.snapshot(directory, List.of(FIRST)).getAsJsonObject("worlds").getAsJsonObject(FIRST).getAsJsonArray("protectedSlots").size());
    JsonObject settings = JsonParser.parseString("{\"inventoryTools\":{\"protectedSlots\":[31],\"preferredItems\":{\"33\":\"35:4\"}}}").getAsJsonObject();
    var data = WorldSortingLayoutsData.migrate(settings).seed(List.of(FIRST));
    JsonObject root = new JsonObject(); root.add("settings", new JsonObject()); root.getAsJsonObject("settings").add(WorldSortingLayoutsData.SECTION, data.json()); Files.writeString(config, root.toString());
    JsonObject manifest = new JsonObject(); manifest.addProperty("duplicateWorldId", SECOND);
    manifest.add("sortingLayouts", WorldSortingLayoutCopies.snapshot(directory, List.of(FIRST)));
    Files.writeString(directory.resolve("power-beta-copy.json"), manifest.toString());
    var inherited = WorldSortingLayoutCopies.snapshot(directory, List.of(SECOND), directory);
    assertEquals(manifest.get("sortingLayouts"), inherited);
    assertEquals(31, inherited.getAsJsonObject("worlds").getAsJsonObject(FIRST).getAsJsonArray("protectedSlots").get(0).getAsInt());
  }
  @Test void unsupportedWorldLayoutStoreIsArchivedWithoutInterpretingIt() throws Exception {
    Path config = directory.resolve("config/power-beta.json"); Files.createDirectories(config.getParent());
    Files.writeString(config, "{\"settings\":{\"worldSortingLayouts\":{\"format\":999,\"opaque\":[1,2]}}}");
    var archived = WorldSortingLayoutCopies.snapshot(directory, List.of(FIRST));
    assertEquals(999, archived.getAsJsonObject("retainedStore").get("format").getAsInt());
    JsonObject manifest = new JsonObject(); manifest.addProperty("duplicateWorldId", SECOND); manifest.add("sortingLayouts", archived);
    Files.writeString(directory.resolve("power-beta-copy.json"), manifest.toString());
    assertThrows(java.io.IOException.class, () -> WorldSortingLayoutCopies.imported(directory, SECOND));
  }
}
