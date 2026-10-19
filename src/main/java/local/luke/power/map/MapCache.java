package local.luke.power.map;

import static local.luke.power.map.MapMath.*;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Game-thread cache. Missing tiles are requested asynchronously; dirty tiles cannot be evicted. */
public final class MapCache {
  private final Path root;
  private final Consumer<String> errors;
  private final LinkedHashMap<Key, MapTile> tiles = new LinkedHashMap<>(64, .75f, true);
  private final Map<Key, CompletableFuture<MapTile>> loading = new LinkedHashMap<>();
  private final Map<Key, Save> saving = new LinkedHashMap<>();
  private final LinkedHashMap<Key, MapTile> parents = new LinkedHashMap<>();
  private int limit;
  private int retryTicks;
  private boolean writeFailed;
  private boolean closed;

  private record Save(long revision, CompletableFuture<Void> future) {}

  public MapCache(Path root, int limit, Consumer<String> errors) {
    this.root = root;
    this.limit = limit;
    this.errors = errors;
  }

  public Path root() {
    return root;
  }

  public int size() {
    return tiles.size();
  }

  public int pending() {
    return loading.size() + saving.size();
  }

  public void limit(int limit) {
    this.limit = limit;
    trim();
  }

  public Collection<MapTile> resident() {
    return List.copyOf(tiles.values());
  }

  public MapTile peek(Key key) {
    return tiles.get(key);
  }

  public MapTile get(Key key) {
    if (closed) return null;
    MapTile tile = tiles.get(key);
    if (tile != null) return tile;
    // Reserve cache slots for in-flight loads, even if every resident tile is dirty.
    if (loading.containsKey(key) || loading.size() >= 16) return null;
    trim();
    if (tiles.size() + loading.size() >= limit && !evictOne()) return null;
    var future = MapIO.submit(() -> MapCodec.read(root.resolve(key.file()), key));
    if (future != null) loading.put(key, future);
    return null;
  }

  public boolean canSample() {
    return parents.size() < limit;
  }

  public void changed(MapTile tile) {
    if (tile.key.level() < MAX_LEVEL) parents.put(tile.key, tile.copy());
  }

  public void tick() {
    drain();
    int work = 0;
    for (Key key : new ArrayList<>(parents.keySet())) {
      if (work++ >= 4) break;
      MapTile child = parents.get(key);
      if (child == null) {
        parents.remove(key);
        continue;
      }
      MapTile parent = get(key.parent());
      if (parent == null) continue;
      parents.remove(key);
      if (parent.merge(child)) changed(parent);
    }
    int writes = 0;
    if (retryTicks > 0) {
      retryTicks--;
      trim();
      return;
    }
    // Reverse access order protects the oldest dirty tiles first, freeing cache capacity.
    for (MapTile tile : new ArrayList<>(tiles.values()))
      if (tile.dirty() && !saving.containsKey(tile.key)) {
        if (writes++ >= 2) break;
        save(tile);
      }
    trim();
  }

  private boolean save(MapTile tile) {
    MapTile snapshot = tile.copy();
    var future =
        MapIO.<Void>submit(
            () -> {
              MapCodec.write(root.resolve(snapshot.key.file()), snapshot);
              return null;
            });
    if (future == null) return false;
    saving.put(tile.key, new Save(tile.revision, future));
    return true;
  }

  private void drain() {
    for (var it = loading.entrySet().iterator(); it.hasNext(); ) {
      var entry = it.next();
      if (!entry.getValue().isDone()) continue;
      MapTile tile;
      try {
        tile = entry.getValue().join();
      } catch (RuntimeException e) {
        tile = new MapTile(entry.getKey());
        tile.readOnly = true;
        errors.accept(
            "Map tile retained without changes: " + entry.getKey().file() + ": " + reason(e));
      }
      tiles.put(entry.getKey(), tile);
      it.remove();
    }
    for (var it = saving.entrySet().iterator(); it.hasNext(); ) {
      var entry = it.next();
      if (!entry.getValue().future.isDone()) continue;
      try {
        entry.getValue().future.join();
        MapTile tile = tiles.get(entry.getKey());
        if (tile != null) tile.savedRevision = entry.getValue().revision;
      } catch (RuntimeException e) {
        errors.accept("Map could not be saved: " + reason(e));
        retryTicks = 200;
        writeFailed = true;
      }
      it.remove();
    }
  }

  private boolean evictOne() {
    for (var it = tiles.entrySet().iterator(); it.hasNext(); ) {
      var entry = it.next();
      if (!entry.getValue().dirty() && !saving.containsKey(entry.getKey())) {
        it.remove();
        return true;
      }
    }
    return false;
  }

  private void trim() {
    while (tiles.size() + loading.size() > limit && evictOne()) {}
  }

  /**
   * Only called at world exit or shutdown. No disk waits occur during normal sampling/rendering.
   */
  public void finish() {
    if (closed) return;
    int attempts = 0;
    retryTicks = 0;
    writeFailed = false;
    while (attempts++ < Math.max(1024, limit * (MAX_LEVEL + 2))) {
      tick();
      if (writeFailed) {
        errors.accept("Some explored terrain could not be saved; existing map files were retained");
        return;
      }
      if (loading.isEmpty()
          && saving.isEmpty()
          && parents.isEmpty()
          && tiles.values().stream().noneMatch(MapTile::dirty)) break;
      var waits = new ArrayList<CompletableFuture<?>>();
      waits.addAll(loading.values());
      saving.values().forEach(s -> waits.add(s.future));
      if (waits.isEmpty()) {
        MapIO.awaitQueued();
        continue;
      }
      for (var future : waits)
        try {
          future.join();
        } catch (RuntimeException ignored) {
        }
    }
    if (!parents.isEmpty() || tiles.values().stream().anyMatch(MapTile::dirty))
      errors.accept("Map shutdown could not finish all explored terrain writes");
    closed = true;
  }

  public static String reason(Throwable failure) {
    while (failure.getCause() != null) failure = failure.getCause();
    return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
  }
}
