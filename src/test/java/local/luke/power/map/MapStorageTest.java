package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MapStorageTest {
  @Test void framePublicationIsBoundedAndDoesNotAdvanceDurableMaintenance() throws Exception {
    var cache = new MapCache(root, 64, message -> fail(message));
    for (int x = 0; x < 16; x++) cache.get(new MapMath.Key(0, -1, 0, x, 0));
    MapIO.awaitQueued();
    assertEquals(0, cache.publish(4, System.nanoTime() - 1));
    assertEquals(0, cache.size());
    assertEquals(4, cache.publish(100, Long.MAX_VALUE));
    assertEquals(4, cache.size());
    assertEquals(12, cache.pending());
    cache.retire();
    assertEquals(0, cache.publish(4, Long.MAX_VALUE));
    cache.finish();
  }
  @TempDir Path root;

  private MapTile tile(int x, int z) {
    var tile = new MapTile(new MapMath.Key(0, -1, 0, x, z));
    for (int i = 0; i < 4096; i++)
      if (i % 3 == 0) tile.put(i, 0x558844 + i, 0x777777, 1 + i % 127, i % 16, 1 + i % 96, i % 14);
    return tile;
  }

  @Test
  void caveContextSurvivesCodecParentWorkAndCacheViewChanges() throws Exception {
    String first = CaveWindow.at(64, 2, 8).context(), second = CaveWindow.at(65, 2, 8).context();
    var key = new MapMath.Key(0, 8, 0, 0, 0, first);
    var old = new MapTile(key);
    old.put(4095, 0x808080, 0x808080, 63, 15, 1, 0);
    var otherHeight = new MapTile(key.withContext(second));
    otherHeight.put(4095, 0x334455, 0x334455, 120, 15, 1, 0);
    var southeast = new MapTile(new MapMath.Key(0, 8, 0, 1, 1, first));
    southeast.put(0, 0x808080, 0x808080, 64, 15, 1, 0);
    byte[] bytes = MapCodec.encode(old);
    assertEquals(2, java.nio.ByteBuffer.wrap(bytes).getInt(4));
    assertEquals(old.key, MapCodec.decode(bytes, old.key).key);
    for (MapTile tile : List.of(old, otherHeight, southeast)) MapCodec.write(root.resolve(tile.key.file()), tile);
    var errors = new ArrayList<String>();
    var cache = new MapCache(root, 8, errors::add);
    cache.viewContext(first);
    assertEquals(old.key, cache.viewKey(new MapMath.Key(0, 8, 0, 0, 0)));
    cache.get(old.key); MapIO.awaitQueued(); cache.tick();
    cache.viewContext(second);
    assertEquals(otherHeight.key, cache.viewKey(new MapMath.Key(0, 8, 0, 0, 0)));
    assertEquals(old.key, cache.viewKey(old.key));
    assertEquals(new MapMath.Key(0, -1, 0, 0, 0), cache.viewKey(new MapMath.Key(0, -1, 0, 0, 0)));
    cache.get(otherHeight.key); MapIO.awaitQueued(); cache.tick();
    cache.get(southeast.key); MapIO.awaitQueued(); cache.tick();
    int expected = MapRelief.color(0x808080, 64, 64, 63, 0);
    assertEquals(expected, cache.peek(southeast.key).reliefAccurate[0]);
    cache.finish();
    assertEquals(List.of(), errors);
    assertEquals(first, MapCodec.read(root.resolve(southeast.key.file()), southeast.key).key.context());
    assertEquals(expected, MapCodec.read(root.resolve(southeast.key.file()), southeast.key).reliefAccurate[0]);
    assertEquals(0x808080, MapCodec.read(root.resolve(old.key.file()), old.key).accurate[4095]);
    assertEquals(0x334455, MapCodec.read(root.resolve(otherHeight.key.file()), otherHeight.key).accurate[4095]);
    assertTrue(Files.exists(root.resolve(southeast.key.parent().file())));
    assertTrue(cache.size() <= 8);
  }

  @Test
  void disablingTerrainDrainsExistingDurableWorkWithoutAcceptingNewRequests() throws Exception {
    var errors = new ArrayList<String>();
    var cache = new MapCache(root, 8, errors::add);
    var key = new MapMath.Key(0, -1, 0, 0, 0);
    cache.get(key); MapIO.awaitQueued(); cache.tick();
    MapTile tile = cache.get(key);
    assertNotNull(tile);
    tile.put(0, 0x123456, 0x654321, 64, 15, 1, 0);
    cache.changed(tile);
    cache.retire();
    assertTrue(cache.retiring()); assertFalse(cache.canSample());
    long requests = MapIO.submitted();
    assertNull(cache.get(new MapMath.Key(0, -1, 0, 40, 40)));
    assertEquals(requests, MapIO.submitted());
    assertFalse(cache.releaseIfIdle(), "Dirty exploration must remain owned until durable");
    for (int tick = 0; tick < 512 && !cache.idle(); tick++) {
      cache.tick(); MapIO.awaitQueued();
    }
    assertTrue(cache.idle());
    assertTrue(cache.releaseIfIdle());
    assertEquals(0, cache.size());
    assertEquals(List.of(), errors);
    assertEquals(0x123456, MapCodec.read(root.resolve(key.file()), key).accurate[0]);
    assertTrue(Files.exists(root.resolve(key.parent().file())));
    assertNull(cache.get(key));
  }

  @Test
  void disabledWaypointMaintenanceDoesNotStartFreshReadsAndStillSavesExistingEdits() {
    var errors = new ArrayList<String>();
    var store = new WaypointStore(root, errors::add);
    long requests = MapIO.submitted();
    store.tick(false);
    assertEquals(requests, MapIO.submitted());
    assertFalse(store.needsMaintenance());
    assertFalse(store.ready());
    store.tick(); MapIO.awaitQueued(); store.tick(false);
    assertTrue(store.ready());
    assertFalse(store.needsMaintenance());
    Waypoint point = new Waypoint(); point.name = "Retained while hidden";
    store.put(point);
    assertTrue(store.needsMaintenance());
    store.tick(false); MapIO.awaitQueued(); store.tick(false);
    assertFalse(store.needsMaintenance());
    assertFalse(store.dirty());
    assertTrue(store.contains(point.id));
    assertEquals(List.of(), errors);
  }

  @Test
  void roundtripAllDataAndNegativeCoordinates() throws Exception {
    var tile = tile(-3, -1);
    byte[] encoded = MapCodec.encode(tile);
    var restored = MapCodec.decode(encoded, tile.key);
    assertArrayEquals(tile.accurate, restored.accurate);
    assertArrayEquals(tile.vanilla, restored.vanilla);
    assertArrayEquals(tile.heights, restored.heights);
    assertArrayEquals(tile.blocks, restored.blocks);
    assertArrayEquals(tile.lights, restored.lights);
    assertArrayEquals(tile.biomes, restored.biomes);
    assertArrayEquals(tile.reliefAccurate, restored.reliefAccurate);
    assertArrayEquals(tile.reliefVanilla, restored.reliefVanilla);
    assertTrue(encoded.length < 4096 * 15 / 2);
    assertFalse(restored.dirty());
  }

  private byte[] legacy(MapTile tile) throws Exception {
    var raw = new java.io.ByteArrayOutputStream();
    try (var out = new java.io.DataOutputStream(raw)) {
      for (int i = 0; i < MapMath.PIXELS; i++) {
        out.writeInt(tile.accurate[i]);
        out.writeInt(tile.vanilla[i]);
        out.writeInt(tile.blocks[i]);
        out.writeByte(tile.heights[i]);
        out.writeByte(tile.lights[i]);
        out.writeByte(tile.biomes[i]);
      }
    }
    var crc = new java.util.zip.CRC32();
    crc.update(raw.toByteArray());
    var result = new java.io.ByteArrayOutputStream();
    var out = new java.io.DataOutputStream(result);
    out.writeInt(0x50424d50);
    out.writeInt(1);
    for (int value :
        new int[] {
          tile.key.dimension(),
          tile.key.layer(),
          tile.key.level(),
          tile.key.x(),
          tile.key.z(),
          (int) crc.getValue()
        }) out.writeInt(value);
    try (var compressed = new java.util.zip.DeflaterOutputStream(result)) {
      compressed.write(raw.toByteArray());
    }
    return result.toByteArray();
  }

  @Test
  void legacyUpgradeRetainsVerifiedOriginalAndRestartsWithShadedLevels() throws Exception {
    var tile = tile(-5, 3);
    byte[] original = legacy(tile);
    Path file = root.resolve(tile.key.file());
    Files.createDirectories(file.getParent());
    Files.write(file, original);
    var loaded = MapCodec.read(file, tile.key);
    assertArrayEquals(tile.accurate, loaded.accurate);
    assertFalse(loaded.dirty());
    assertNotEquals(loaded.accurate[0], loaded.reliefAccurate[0]);
    loaded.put(1, 0x112233, 0x445566, 65, 15, 2, 3);
    MapCodec.write(file, loaded);
    assertArrayEquals(
        original, Files.readAllBytes(file.resolveSibling(file.getFileName() + ".v1.bak")));
    var restarted = MapCodec.read(file, tile.key);
    assertArrayEquals(loaded.reliefAccurate, restarted.reliefAccurate);
    assertEquals(0x112233, restarted.accurate[1]);
    MapCodec.write(file, restarted);
    assertArrayEquals(
        original, Files.readAllBytes(file.resolveSibling(file.getFileName() + ".v1.bak")));
  }

  @Test
  void failedUpgradeAndFutureReplacementKeepOriginalBytes() throws Exception {
    var tile = tile(4, 7);
    Path file = root.resolve(tile.key.file());
    Files.createDirectories(file.getParent());
    byte[] original = legacy(tile);
    Files.write(file, original);
    Files.write(file.resolveSibling(file.getFileName() + ".v1.bak"), new byte[] {1, 2, 3});
    assertThrows(java.io.IOException.class, () -> MapCodec.write(file, tile));
    assertArrayEquals(original, Files.readAllBytes(file));
    byte[] future = MapCodec.encode(tile);
    future[7] = 50;
    Files.write(file, future);
    assertThrows(java.io.IOException.class, () -> MapCodec.write(file, tile));
    assertArrayEquals(future, Files.readAllBytes(file));
    Files.write(file, new byte[] {1, 2, 3});
    assertThrows(java.io.IOException.class, () -> MapCodec.write(file, tile));
    assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(file));
  }

  @Test
  void unchangedLegacyLeafRebuildsExistingDistantRelief() throws Exception {
    var leaf = tile(0, 0);
    var parent = new MapTile(leaf.key.parent());
    parent.merge(leaf);
    int[] expected = parent.reliefAccurate.clone();
    byte[] oldParent = legacy(parent);
    for (var value : List.of(leaf, parent)) {
      Path path = root.resolve(value.key.file());
      Files.createDirectories(path.getParent());
      Files.write(path, value == parent ? oldParent : legacy(value));
    }
    var errors = new ArrayList<String>();
    var cache = new MapCache(root, 16, errors::add);
    assertNull(cache.get(leaf.key));
    MapIO.awaitQueued();
    cache.tick();
    cache.finish();
    assertEquals(List.of(), errors);
    assertArrayEquals(
        expected, MapCodec.read(root.resolve(parent.key.file()), parent.key).reliefAccurate);
    Path path = root.resolve(parent.key.file());
    assertArrayEquals(
        oldParent, Files.readAllBytes(path.resolveSibling(path.getFileName() + ".v1.bak")));
  }

  @Test
  void diagonalReliefCornerIsIndependentOfTileLoadOrder() throws Exception {
    var northwest = new MapTile(new MapMath.Key(0, -1, 0, 0, 0));
    northwest.put(4095, 0x808080, 0x808080, 63, 15, 1, 0);
    var southeast = new MapTile(new MapMath.Key(0, -1, 0, 1, 1));
    southeast.put(0, 0x808080, 0x808080, 64, 15, 1, 0);
    int expected = MapRelief.color(0x808080, 64, 64, 63, 0);
    for (boolean reverse : new boolean[] {false, true}) {
      Path folder = root.resolve(reverse ? "reverse" : "forward");
      for (var tile : List.of(northwest, southeast))
        MapCodec.write(folder.resolve(tile.key.file()), tile);
      var errors = new ArrayList<String>();
      var cache = new MapCache(folder, 16, errors::add);
      for (var tile : reverse ? List.of(southeast, northwest) : List.of(northwest, southeast)) {
        cache.get(tile.key);
        MapIO.awaitQueued();
        cache.tick();
      }
      assertEquals(expected, cache.peek(southeast.key).reliefAccurate[0]);
      cache.finish();
      assertEquals(List.of(), errors);
      assertEquals(
          expected,
          MapCodec.read(folder.resolve(southeast.key.file()), southeast.key).reliefAccurate[0]);
    }
  }

  @Test
  void rapidLegacyMapPanningKeepsMigrationBacklogBoundedAndDurable() throws Exception {
    for (int x = 0; x < 96; x++) {
      var tile = tile(x, 0);
      Path path = root.resolve(tile.key.file());
      Files.createDirectories(path.getParent());
      Files.write(path, legacy(tile));
    }
    var errors = new ArrayList<String>();
    var cache = new MapCache(root, 16, errors::add);
    var field = MapCache.class.getDeclaredField("parents");
    field.setAccessible(true);
    for (int tick = 0; tick < 500; tick++) {
      for (int x = 0; x < 96; x++) cache.get(new MapMath.Key(0, -1, 0, x, 0));
      cache.tick();
      assertTrue(((Map<?, ?>) field.get(cache)).size() <= 16 + 64);
      assertTrue(cache.size() <= 16);
      if (tick % 10 == 0) Thread.sleep(1);
    }
    cache.finish();
    assertEquals(List.of(), errors);
    assertTrue(((Map<?, ?>) field.get(cache)).isEmpty());
  }

  @Test
  void rejectsTruncationCorruptionWrongLocationsAndFutureData() throws Exception {
    var tile = tile(0, 0);
    byte[] encoded = MapCodec.encode(tile);
    for (int length : new int[] {0, 1, 8, 35, encoded.length - 1})
      assertThrows(
          java.io.IOException.class,
          () -> MapCodec.decode(Arrays.copyOf(encoded, length), tile.key));
    byte[] corrupt = encoded.clone();
    corrupt[corrupt.length - 5] ^= 0x20;
    assertThrows(java.io.IOException.class, () -> MapCodec.decode(corrupt, tile.key));
    assertThrows(
        java.io.IOException.class, () -> MapCodec.decode(encoded, new MapMath.Key(0, -1, 0, 1, 0)));
    byte[] future = encoded.clone();
    future[7] = 50;
    assertThrows(java.io.IOException.class, () -> MapCodec.decode(future, tile.key));
  }

  @Test
  void writesVerifiedAtomicFilesAndDoesNotShareSnapshotArrays() throws Exception {
    var tile = tile(1, -2);
    var snapshot = tile.copy();
    tile.put(0, 0xff00ff, 1, 127, 15, 99, 4);
    assertNotEquals(tile.accurate[0], snapshot.accurate[0]);
    Path file = root.resolve(tile.key.file());
    MapCodec.write(file, snapshot);
    assertArrayEquals(snapshot.accurate, MapCodec.read(file, snapshot.key).accurate);
    assertEquals(1, Files.list(file.getParent()).count());
  }

  @Test
  void coarserTilesPreserveUnrelatedQuadrantsAndUnknownPixels() {
    var child = tile(-1, -1);
    var parent = new MapTile(child.key.parent());
    parent.put(0, 0x123456, 0x654321, 32, 4, 5, 2);
    assertTrue(parent.merge(child));
    assertEquals(0x123456, parent.accurate[0]);
    assertTrue(parent.known(32 * 64 + 32));
    assertFalse(parent.known(0 * 64 + 32));
    assertFalse(parent.merge(child));
    assertThrows(
        IllegalArgumentException.class,
        () -> new MapTile(new MapMath.Key(0, -1, 1, 4, 0)).merge(child));
  }

  @Test
  void readOnlyParentRejectsMergeWithoutMutation() {
    var child = tile(0, 0);
    var parent = new MapTile(child.key.parent());
    parent.put(0, 0x123456, 0x654321, 32, 4, 5, 2);
    parent.savedRevision = parent.revision;
    parent.readOnly = true;
    var before = parent.copy();
    long childRevision = child.revision;

    assertFalse(parent.merge(child));
    assertArrayEquals(before.accurate, parent.accurate);
    assertArrayEquals(before.vanilla, parent.vanilla);
    assertArrayEquals(before.reliefAccurate, parent.reliefAccurate);
    assertArrayEquals(before.reliefVanilla, parent.reliefVanilla);
    assertArrayEquals(before.blocks, parent.blocks);
    assertArrayEquals(before.heights, parent.heights);
    assertArrayEquals(before.lights, parent.lights);
    assertArrayEquals(before.biomes, parent.biomes);
    assertEquals(before.revision, parent.revision);
    assertEquals(before.savedRevision, parent.savedRevision);
    assertEquals(childRevision, child.revision);
    assertFalse(parent.dirty());
    assertThrows(
        IllegalArgumentException.class,
        () -> parent.merge(new MapTile(new MapMath.Key(0, -1, 0, 8, 0))));
  }

  @Test
  void surveyedVoidReplacesOldTerrainAcrossCoarserLevels() {
    var child = tile(0, 0);
    var parent = new MapTile(child.key.parent());
    parent.merge(child);
    for (int i = 0; i < 4096; i++) child.put(i, 0, 0, 0, 0, 0, 0);
    assertTrue(parent.merge(child));
    assertTrue(parent.known(0));
    assertEquals(0, parent.block(0));
    assertEquals(0xff000000, parent.color(0, new MapSettings()));
  }

  @Test
  void dirtyCacheEvictionPersistsEveryExploredTileAndProtectsFutureFiles() throws Exception {
    var errors = new ArrayList<String>();
    var cache = new MapCache(root, 8, errors::add);
    for (int n = 0; n < 18; n++) {
      var key = new MapMath.Key(0, -1, 0, n, 0);
      MapTile current = null;
      for (int attempt = 0; attempt < 4000 && current == null; attempt++) {
        cache.tick();
        current = cache.get(key);
        if (current == null) Thread.sleep(1);
      }
      assertNotNull(current);
      current.put(0, 0x123456 + n, 0x654321, 64, 2, 1, 4);
      cache.changed(current);
      assertTrue(cache.size() + cache.pending() <= 24);
    }
    cache.finish();
    assertEquals(List.of(), errors);
    for (int n = 0; n < 18; n++) {
      var key = new MapMath.Key(0, -1, 0, n, 0);
      assertEquals(0x123456 + n, MapCodec.read(root.resolve(key.file()), key).accurate[0]);
    }
    var futureTile = tile(40, 40);
    byte[] future = MapCodec.encode(futureTile);
    future[7] = 50;
    Path file = root.resolve(futureTile.key.file());
    Files.createDirectories(file.getParent());
    Files.write(file, future);
    var another = new MapCache(root, 8, errors::add);
    MapTile loaded = null;
    for (int i = 0; i < 1000 && loaded == null; i++) {
      another.tick();
      loaded = another.get(futureTile.key);
      Thread.sleep(1);
    }
    assertNotNull(loaded);
    assertTrue(loaded.readOnly);
    assertFalse(loaded.put(0, 1, 2, 3, 4, 5, 6));
    another.finish();
    assertArrayEquals(future, Files.readAllBytes(file));
  }
}
