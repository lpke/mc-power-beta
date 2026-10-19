package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MapStorageTest {
  @TempDir Path root;

  private MapTile tile(int x, int z) {
    var tile = new MapTile(new MapMath.Key(0, -1, 0, x, z));
    for (int i = 0; i < 4096; i++)
      if (i % 3 == 0) tile.put(i, 0x558844 + i, 0x777777, 1 + i % 127, i % 16, 1 + i % 96, i % 14);
    return tile;
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
    assertTrue(encoded.length < 4096 * 15 / 2);
    assertFalse(restored.dirty());
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
    future[7] = 2;
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
  void surveyedVoidReplacesOldTerrainAcrossCoarserLevels() {
    var child = tile(0, 0);
    var parent = new MapTile(child.key.parent());
    parent.merge(child);
    for (int i = 0; i < 4096; i++) child.put(i, 0, 0, 0, 0, 0, 0);
    assertTrue(parent.merge(child));
    assertTrue(parent.known(0));
    assertEquals(0, parent.block(0));
    assertEquals(0xff000000, parent.colour(0, new MapSettings()));
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
