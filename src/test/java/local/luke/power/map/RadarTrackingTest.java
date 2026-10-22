package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import local.luke.power.visual.VisualSettings;
import org.junit.jupiter.api.Test;

class RadarTrackingTest {
  @Test void fullyHiddenMinimapCategoriesNeedNoDiscoveryButUnknownLivingEntitiesRemainEligible() {
    var settings = new MapSettings();
    var visibility = new VisualSettings();
    visibility.hideEntities = visibility.hideItems = visibility.hideMobs = visibility.hidePlayers = true;
    visibility.hideVehicles = visibility.hideProjectiles = visibility.hideTnt = visibility.hideOther = true;
    assertFalse(RadarTracking.minimapTypesVisible(settings, visibility));
    visibility.hideMobs = false;
    assertTrue(RadarTracking.minimapTypesVisible(settings, visibility));
    settings.animals = settings.hostile = settings.players = settings.items = false;
    assertTrue(RadarTracking.minimapTypesVisible(settings, visibility), "Unknown living entities can belong to Other");
    settings.otherEntities = false;
    assertFalse(RadarTracking.minimapTypesVisible(settings, visibility));
  }

  @Test void growingEntityListsDoNotRestartDiscoveryOrBlockOtherChunks() {
    var scanner = new RadarChunkScan<Chunk, Integer>();
    var storm = new Chunk(0, new ArrayList<>());
    for (int i = 0; i < 1000; i++) storm.sections[0].add(i);
    var neighbor = new Chunk(0, new ArrayList<>(List.of(10000)));
    var access = access(Map.of("0,0", storm, "1,0", neighbor));
    Set<Integer> found = new HashSet<>();
    for (int tick = 0; tick < 20; tick++) {
      storm.sections[0].add(20000 + tick);
      int visited = scanner.scan(access, 0, 0, 1, 0, 15, 9, 16, found::add);
      assertTrue(visited <= 16);
      assertTrue(found.contains(10000), "Dense center must not block the next chunk");
    }
    assertTrue(found.contains(150), "Count changes must retain progress through the dense section");
  }

  @Test void denseSectionsDoNotStarveAnotherSectionAndAllWorkIsBounded() {
    var scanner = new RadarChunkScan<Chunk, Integer>();
    var chunk = new Chunk(-32, new ArrayList<>(), new ArrayList<>(List.of(5000)), new ArrayList<>(List.of(6000)));
    for (int i = 0; i < 10000; i++) chunk.sections[0].add(i);
    var access = access(Map.of("0,0", chunk));
    var found = new HashSet<Integer>();
    for (int i = 0; i < 3; i++)
      assertTrue(scanner.scan(access, 0, 0, 0, -32, 15, 1, 8, found::add) <= 8);
    assertTrue(found.contains(5000)); assertTrue(found.contains(6000));
    found.clear();
    scanner.scan(access, 0, 0, 0, -16, -1, 1, 8, found::add);
    assertEquals(Set.of(5000), found, "Flattened section origins must apply to the height window");
  }

  @Test void emptyAndShrinkingSectionsResumeWithoutStaleIndices() {
    var scanner = new RadarChunkScan<Chunk, Integer>();
    var chunk = new Chunk(0, new ArrayList<>());
    for (int i = 0; i < 100; i++) chunk.sections[0].add(i);
    var access = access(Map.of("0,0", chunk));
    scanner.scan(access, 0, 0, 0, 0, 15, 1, 8, ignored -> {});
    chunk.sections[0].clear(); chunk.sections[0].add(1234);
    var found = new ArrayList<Integer>();
    scanner.scan(access, 0, 0, 0, 0, 15, 1, 8, found::add);
    assertEquals(List.of(1234), found);
    chunk.sections[0].clear();
    assertEquals(0, scanner.scan(access, 0, 0, 0, 0, 15, 1, 8, found::add));
  }

  @Test void selectedEntitiesRefreshWithoutWaitingForAnotherDiscoverySweep() {
    var selection = new RadarSelection<double[]>();
    var near = new double[] {1}; var far = new double[] {9}; var newcomer = new double[] {4};
    selection.refresh(2, value -> value[0]);
    selection.offer(near, near[0]); selection.offer(far, far[0]);
    near[0] = 25;
    selection.refresh(2, value -> value[0]);
    selection.offer(newcomer, newcomer[0]);
    assertEquals(Set.of(far, newcomer), new HashSet<>(selection.values()));
    far[0] = Double.NaN;
    selection.refresh(2, value -> value[0]);
    assertEquals(List.of(newcomer), new ArrayList<>(selection.values()));
    selection.refresh(0, value -> fail("Disabled view must not read retained entities"));
    assertTrue(selection.values().isEmpty());
  }

  @Test void hiddenWorldMapEntriesCannotConsumeTheMinimapLimit() {
    var mini = new RadarSelection<String>(); var full = new RadarSelection<String>();
    mini.refresh(1, value -> 0); full.refresh(1, value -> 0);
    full.offer("hidden item", 1);
    mini.offer("visible mob", 9); full.offer("visible mob", 9);
    assertEquals(List.of("visible mob"), new ArrayList<>(mini.values()));
    assertEquals(List.of("hidden item"), new ArrayList<>(full.values()));
  }

  @Test void rangeDefaultsToEightChunksAndNeverExceedsNativeTerrainRange() {
    var settings = new MapSettings();
    assertEquals(8, settings.radarRange);
    assertEquals(16, settings.radarHeight);
    assertEquals(8, RadarTracking.range(8, 12)); assertEquals(4, RadarTracking.range(8, 4));
    settings.radarRange = 0; assertThrows(IllegalArgumentException.class, settings::validate);
    settings.radarRange = 33; assertThrows(IllegalArgumentException.class, settings::validate);
  }

  @Test void pausedLocalSnapshotsHoldStillWithoutDisablingMultiplayerInterpolation() {
    double held = RadarTracking.previous(4, 6, true, false);
    for (float delta : new float[] {0, .2f, .9f, 1, 0})
      assertEquals(6, MapMotion.position(held, 6, delta));
    assertEquals(4, RadarTracking.previous(4, 6, false, false));
    assertEquals(4, RadarTracking.previous(4, 6, true, true));
  }

  @Test void existingCompactIndexIsUsedOnlyWhenBothOmittedNativeTypesAreUnwanted() {
    var settings = new MapSettings(); var visibility = new VisualSettings();
    settings.items = settings.otherEntities = true;
    visibility.hideEntities = visibility.hideItems = visibility.hideTnt = true;
    assertTrue(RadarTracking.compactIndex(settings, false, visibility));
    assertFalse(RadarTracking.compactIndex(settings, true, visibility), "Only the viewed WorldMap ignores hiding");
    settings.worldMap = true;
    assertTrue(RadarTracking.compactIndex(settings, false, visibility), "An enabled but closed WorldMap adds no work");
    visibility.hideTnt = false;
    assertFalse(RadarTracking.compactIndex(settings, false, visibility));
    settings.otherEntities = false;
    assertTrue(RadarTracking.compactIndex(settings, false, visibility));
    visibility.hideEntities = false;
    assertFalse(RadarTracking.compactIndex(settings, false, visibility));
    settings.items = false;
    assertTrue(RadarTracking.compactIndex(settings, true, visibility));
  }

  @Test void aColdMinimapPreviewKeepsItsCompactDiscoveryAlongsideAnUnrestrictedWorldMap() {
    var settings = new MapSettings(); var visibility = new VisualSettings();
    settings.items = settings.otherEntities = true;
    visibility.hideEntities = visibility.hideItems = visibility.hideTnt = true;
    assertTrue(RadarTracking.filteredLane(settings, true, true, visibility));
    assertFalse(RadarTracking.filteredLane(settings, false, true, visibility));
    assertFalse(RadarTracking.filteredLane(settings, true, false, visibility));
    visibility.hideItems = false;
    assertFalse(RadarTracking.filteredLane(settings, true, true, visibility));
    settings.items = settings.otherEntities = false;
    assertFalse(RadarTracking.filteredLane(settings, true, true, visibility), "Both views can share the compact scan");
  }

  private static final class Chunk {
    final int bottom;
    final List<Integer>[] sections;
    @SafeVarargs Chunk(int bottom, List<Integer>... sections) { this.bottom = bottom; this.sections = sections; }
  }
  private static RadarChunkScan.Access<Chunk, Integer> access(Map<String, Chunk> chunks) {
    return new RadarChunkScan.Access<>() {
      public Chunk loaded(int x, int z) { return chunks.get(x + "," + z); }
      public List<Integer>[] sections(Chunk chunk) { return chunk.sections; }
      public int bottom(Chunk chunk) { return chunk.bottom; }
    };
  }
}
