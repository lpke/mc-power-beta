package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WaypointsTest {
  @TempDir Path root;

  private WaypointStore store() throws Exception {
    var result = new WaypointStore(root, m -> fail(m));
    for (int n = 0; n < 2000 && !result.ready(); n++) {
      result.tick();
      Thread.sleep(1);
    }
    assertTrue(result.ready());
    return result;
  }

  @Test
  void isolatedDraftsPersistWithGson289AndRestart() throws Exception {
    var store = store();
    var point = new Waypoint();
    point.name = "Home";
    point.x = -113;
    point.set = "Builds";
    store.put(point);
    point.name = "Changed draft";
    store.select("Builds");
    store.finish();
    assertEquals("Home", store.points().get(0).name);
    var reloaded = store();
    assertEquals("Builds", reloaded.selectedSet());
    assertEquals(-113, reloaded.points().get(0).x);
    reloaded.put(reloaded.points().get(0));
    reloaded.finish();
    assertTrue(Files.exists(root.resolve("waypoints.previous.json")));
  }

  @Test
  void unknownFormatAndDuplicateIdsRemainProtected() throws Exception {
    var data = new WaypointStore.Data();
    data.waypoints.add(new Waypoint());
    byte[] bytes = WaypointStore.encode(data);
    String json =
        new String(bytes, StandardCharsets.UTF_8).replace("\"format\": 1", "\"format\": 99");
    assertThrows(
        java.io.IOException.class,
        () -> WaypointStore.decode(json.getBytes(StandardCharsets.UTF_8)));
    data.waypoints.add(data.waypoints.get(0).copy());
    assertThrows(IllegalArgumentException.class, () -> WaypointStore.encode(data));
    Files.writeString(root.resolve("waypoints.json"), json);
    var errors = new ArrayList<String>();
    var store = new WaypointStore(root, errors::add);
    for (int n = 0; n < 100; n++) {
      store.tick();
      Thread.sleep(1);
    }
    assertFalse(store.ready());
    assertFalse(errors.isEmpty());
    assertThrows(IllegalStateException.class, () -> store.put(new Waypoint()));
    assertEquals(json, Files.readString(root.resolve("waypoints.json")));
  }

  @Test
  void deathsAndVisibilityRetainWorldDimensionSetsAndDistances() throws Exception {
    var store = store();
    store.death(10, 64, 20, 0, true);
    store.death(15, 64, 25, -1, true);
    assertEquals(2, store.points().size());
    store.death(30, 70, 40, 0, false);
    assertEquals(1, store.points().size());
    Waypoint point = store.points().get(0);
    var s = new MapSettings();
    s.waypointDistance = 10;
    assertTrue(point.visible(0, "Default", false, 10000, 10000, s));
    point.global = false;
    assertFalse(point.visible(0, "Default", false, 10000, 10000, s));
    assertFalse(point.visible(-1, "Default", true, 30, 40, s));
    assertFalse(point.visible(0, "Other", false, 30, 40, s));
    assertTrue(point.visible(0, "Other", true, 30, 40, s));
  }

  @Test
  void newerDraftsSurviveAnOlderSaveAndExternalEditsRemainUntouched() throws Exception {
    var errors = new ArrayList<String>();
    var store = new WaypointStore(root, errors::add);
    for (int i = 0; i < 1000 && !store.ready(); i++) {
      store.tick();
      Thread.sleep(1);
    }
    var first = new Waypoint();
    first.name = "First";
    store.put(first);
    store.tick();
    var second = new Waypoint();
    second.name = "Second";
    store.put(second);
    store.finish();
    assertEquals(2, store().points().size());
    assertFalse(store.dirty());
    Path file = root.resolve("waypoints.json");
    byte[] external = Files.readAllBytes(file);
    String changed =
        new String(external, StandardCharsets.UTF_8).replace("First", "Externally changed");
    Files.writeString(file, changed);
    store.remove(first.id);
    store.finish();
    assertFalse(errors.isEmpty());
    assertEquals(changed, Files.readString(file));
    assertTrue(store.dirty());
  }

  @Test
  void deathTransitionsWaitForTheStoreWithoutLosingTheirOriginalSnapshot() throws Exception {
    var store = new WaypointStore(root, m -> fail(m));
    var queue = new DeathpointQueue();
    long before = System.currentTimeMillis();
    queue.capture(-100, 64, 321, -1, true);
    assertFalse(queue.flush(store));
    assertEquals(1, queue.size());
    for (int i = 0; i < 1000 && !store.ready(); i++) {
      store.tick();
      Thread.sleep(1);
    }
    assertTrue(queue.flush(store));
    assertEquals(0, queue.size());
    store.finish();
    var point = store().points().get(0);
    assertTrue(point.death);
    assertEquals(-100, point.x);
    assertEquals(321, point.z);
    assertEquals(-1, point.dimension);
    assertTrue(point.created >= before);
  }

  @Test
  void rejectsInvalidCoordinatesAndFormatting() {
    var point = new Waypoint();
    point.name = "Bad§name";
    assertThrows(IllegalArgumentException.class, point::validate);
    point.name = "Good";
    point.x = Integer.MIN_VALUE;
    assertThrows(IllegalArgumentException.class, point::validate);
  }
  @Test void emptyNamedSetsSurviveSwitchingWaypointDeletionAndRestart() throws Exception {
    var store = store();
    store.createSet("Builds");
    store.createSet("Exploration");
    store.select("Default");
    assertEquals(List.of("Builds", "Default", "Exploration"), store.sets());
    var point = new Waypoint();
    point.set = "Builds";
    store.put(point);
    store.remove(point.id);
    long revision = store.revision();
    assertThrows(IllegalArgumentException.class, () -> store.createSet("builds"));
    assertThrows(IllegalArgumentException.class, () -> store.createSet(" "));
    assertEquals(revision, store.revision());
    store.finish();
    var restarted = store();
    assertEquals(List.of("Builds", "Default", "Exploration"), restarted.sets());
    assertEquals("Default", restarted.selectedSet());
    assertTrue(restarted.points().isEmpty());
  }
  @Test void formatOneWithoutExplicitSetsKeepsExistingSelectionAndPointSets() throws Exception {
    String legacy = "{\"format\":1,\"selectedSet\":\"Existing empty\",\"waypoints\":[]}";
    var decoded = WaypointStore.decode(legacy.getBytes(StandardCharsets.UTF_8));
    assertTrue(decoded.sets.isEmpty());
    assertEquals("Existing empty", decoded.selectedSet);
    decoded.sets.add("New empty");
    var restored = WaypointStore.decode(WaypointStore.encode(decoded));
    assertEquals(List.of("New empty"), restored.sets);
    assertEquals("Existing empty", restored.selectedSet);
    assertTrue(restored.waypoints.isEmpty());
    decoded.sets.add("new empty");
    assertThrows(IllegalArgumentException.class, () -> WaypointStore.encode(decoded));
  }
  @Test void optionalAbbreviationsRoundTripAndLegacyNamesRemainAutomatic() throws Exception {
    String legacy = "{\"format\":1,\"waypoints\":[{\"id\":\"legacy\",\"name\":\"Home base\",\"set\":\"Default\"}]}";
    var old = WaypointStore.decode(legacy.getBytes(StandardCharsets.UTF_8));
    Waypoint point = old.waypoints.get(0);
    assertEquals("", point.abbreviation);
    assertEquals("H", point.abbreviation());
    point.name = "Village"; assertEquals("V", point.abbreviation());
    point.abbreviation = "V2"; point.death = true; point.created = 1234;
    var restored = WaypointStore.decode(WaypointStore.encode(old)).waypoints.get(0);
    assertEquals("V2", restored.abbreviation()); assertTrue(restored.death); assertEquals(1234, restored.created);
    assertEquals("legacy", restored.id); assertEquals("Default", restored.set);
    restored.name = "Changed"; assertEquals("V2", restored.copy().abbreviation());
    restored.abbreviation = ""; restored.name = "🌲 Trees"; assertEquals("🌲", restored.abbreviation());
    assertEquals("legacy", old.waypoints.get(0).id);
    String numeric = legacy.replace("\"name\":\"Home base\"", "\"name\":\"Home base\",\"abbreviation\":12");
    assertThrows(java.io.IOException.class, () -> WaypointStore.decode(numeric.getBytes(StandardCharsets.UTF_8)));
  }
  @Test void invalidAbbreviationsProtectTheOriginalStoreData() throws Exception {
    var point = new Waypoint();
    for (String invalid : List.of("ABCDE", "A B", " ", "§c", "\n", "\u0000", "\u200B", "\uD800")) {
      point.abbreviation = invalid; assertThrows(IllegalArgumentException.class, point::validate, invalid);
    }
    point.abbreviation = "🌲🌲🌲🌲"; assertDoesNotThrow(point::validate);
    var data = new WaypointStore.Data(); data.waypoints.add(point);
    String invalid = new String(WaypointStore.encode(data), StandardCharsets.UTF_8).replace("🌲🌲🌲🌲", "ABCDE");
    assertThrows(java.io.IOException.class, () -> WaypointStore.decode(invalid.getBytes(StandardCharsets.UTF_8)));
  }
  @Test void movingOneWaypointPreservesItsIdentityAndOtherSetsAcrossRestart() throws Exception {
    var store = store(); store.createSet("Builds"); store.select("Default");
    Waypoint first = new Waypoint(); first.name = "Home"; first.abbreviation = "H1"; first.death = true; first.enabled = false; first.created = 42;
    Waypoint other = new Waypoint(); other.name = "Village"; other.set = "Builds";
    store.put(first); store.put(other); long before = store.revision();
    store.move(first.id, "Builds");
    assertEquals(before + 1, store.revision());
    Waypoint moved = store.points().stream().filter(p -> p.id.equals(first.id)).findFirst().orElseThrow();
    assertEquals("Builds", moved.set); assertEquals("H1", moved.abbreviation); assertTrue(moved.death); assertFalse(moved.enabled); assertEquals(42, moved.created);
    assertEquals("Default", store.selectedSet()); assertEquals(2, store.points().size());
    assertEquals("Village", store.points().stream().filter(p -> p.id.equals(other.id)).findFirst().orElseThrow().name);
    store.select("Builds"); store.remove(other.id); store.remove(first.id);
    assertTrue(store.sets().contains("Builds")); store.put(moved);
    long revision = store.revision(); store.move(first.id, "Builds"); assertEquals(revision, store.revision());
    assertThrows(IllegalArgumentException.class, () -> store.move(first.id, "Missing"));
    assertThrows(IllegalArgumentException.class, () -> store.move("missing", "Builds"));
    store.finish(); assertEquals("H1", store().points().stream().filter(p -> p.id.equals(first.id)).findFirst().orElseThrow().abbreviation);
  }
  @Test void commandNamesResolveInTheSelectedViewAndIdsDisambiguateWithoutChangingData() throws Exception {
    var store = store(); Waypoint first = new Waypoint(); first.name = "Home";
    Waypoint other = new Waypoint(); other.name = "Home"; other.set = "Builds";
    store.put(first); store.put(other);
    assertEquals(first.id, WaypointCommands.find(store, "HOME").id);
    assertEquals(other.id, WaypointCommands.find(store, other.id).id);
    store.allSets(true); assertThrows(IllegalArgumentException.class, () -> WaypointCommands.find(store, "Home"));
    assertThrows(IllegalArgumentException.class, () -> WaypointCommands.find(store, "Missing"));
    assertEquals(2, store.points().size());
  }

  @Test void changingALegacyPointSetRetainsItsNowEmptyOriginalSet() throws Exception {
    String legacy = "{\"format\":1,\"waypoints\":[{\"id\":\"legacy\",\"name\":\"Home\",\"set\":\"Original set\"}]}";
    Files.writeString(root.resolve("waypoints.json"), legacy);
    var store = store(); Waypoint point = store.points().get(0); point.set = "New set"; store.put(point);
    store.remove(point.id); store.select("Default");
    assertTrue(store.sets().containsAll(List.of("Original set", "New set")));
    store.finish(); assertTrue(store().sets().containsAll(List.of("Original set", "New set")));
  }

}
