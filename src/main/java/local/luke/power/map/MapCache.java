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
  private static final int VISIBLE = 1, SAMPLE = 2, PARENT = 4, GENERAL = 8, SECONDARY = 16;
  private final Map<Key, Integer> loadKinds = new HashMap<>();
  private ViewRequest visibleView;
  private record ViewRequest(int dimension, int layer, String context, MapCoverage.Bounds bounds) {
    boolean contains(Key key) {
      return key.dimension() == dimension && key.layer() == layer && key.context().equals(context)
          && key.level() == bounds.level() && key.x() >= bounds.minX() && key.x() <= bounds.maxX()
          && key.z() >= bounds.minZ() && key.z() <= bounds.maxZ();
    }
  }
  private final Map<Key, Save> saving = new LinkedHashMap<>();
  private final LinkedHashMap<Key, MapTile> parents = new LinkedHashMap<>();
  private int limit;
  private int retryTicks;
  private boolean writeFailed;
  private boolean closed, retiring;
  private long generation;
  private String viewContext = "";
  private final Set<Consumer<Key>> listeners = new LinkedHashSet<>();

  public void listen(Consumer<Key> listener) { listeners.add(listener); }
  public void unlisten(Consumer<Key> listener) { listeners.remove(listener); }

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

  public long generation() {
    return generation;
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

  /** Stop new view requests while already queued durable work drains in bounded ticks. */
  public void retire() {
    if (retiring || closed) return;
    retiring = true;
    writeFailed = false;
    retryTicks = 0;
  }

  public void resume() { if (!closed) retiring = false; }
  public boolean retiring() { return retiring; }

  public boolean idle() {
    return loading.isEmpty() && saving.isEmpty() && parents.isEmpty()
        && tiles.values().stream().noneMatch(MapTile::dirty);
  }

  public boolean releaseIfIdle() {
    if (!retiring || !idle()) return false;
    closed = true;
    tiles.clear();
    listeners.clear();
    return true;
  }

  public String viewContext() { return viewContext; }

  /** Switching cave windows selects another namespace without draining the shared cache. */
  public void viewContext(String context) {
    new Key(0, 0, 0, 0, 0, context);
    if (!viewContext.equals(context)) { viewContext = context; generation++; }
  }

  public Key viewKey(Key key) {
    return key.layer() < 0 || !key.context().isEmpty() ? key : key.withContext(viewContext);
  }

  public MapTile peek(Key key) {
    return closed || retiring ? null : tiles.get(viewKey(key));
  }

  public MapTile get(Key key) { return get(viewKey(key), GENERAL); }
  public MapTile visible(Key key) { return get(viewKey(key), VISIBLE); }
  /** A second visible map gets two reads without retiring the main view or taking every maintenance slot. */
  public MapTile secondary(Key key) { return get(viewKey(key), SECONDARY); }
  public MapTile sample(Key key) { return get(viewKey(key), SAMPLE); }

  /** Retire only queued reads that belonged solely to the previous view. Never discard writes. */
  public void visibleView(int dimension, int layer, MapCoverage.Bounds bounds) {
    var request = new ViewRequest(dimension, layer, layer < 0 ? "" : viewContext, bounds);
    if (request.equals(visibleView)) return;
    visibleView = request;
    for (var it = loading.entrySet().iterator(); it.hasNext();) {
      var entry = it.next();
      if (loadKinds.getOrDefault(entry.getKey(), GENERAL) != VISIBLE || request.contains(entry.getKey())) continue;
      if (MapIO.cancelQueuedRead(entry.getValue())) { loadKinds.remove(entry.getKey()); it.remove(); }
    }
  }

  private int loads(int kind) {
    int count = 0;
    for (int flags : loadKinds.values()) if ((flags & kind) != 0) count++;
    return count;
  }

  private MapTile get(Key key, boolean requiredParent) { return get(key, requiredParent ? PARENT : GENERAL); }
  private MapTile get(Key key, int kind) {
    boolean requiredParent = kind == PARENT;
    if (closed || retiring && !requiredParent) return null;
    MapTile tile = tiles.get(key);
    if (tile != null) return tile;
    // Panning cannot build an unbounded backlog of migrated/seam-repaired leaves.
    // Parent reads still proceed so existing durable work can drain.
    if (!requiredParent && parents.size() >= limit) return null;
    // Reserve cache slots for in-flight loads, even if every resident tile is dirty.
    if (loading.containsKey(key)) {
      loadKinds.merge(key, kind, (a, b) -> a | b);
      return null;
    }
    if (loading.size() >= 16 || kind == VISIBLE && loads(VISIBLE) >= 8
        || kind == SAMPLE && loads(SAMPLE) >= 4 || kind == SECONDARY && loads(SECONDARY) >= 2 || kind == PARENT && loads(PARENT) >= 4) return null;
    // Leave room for parent maintenance while visible and sampling consumers fill their shares.
    if ((kind == VISIBLE || kind == SAMPLE) && loading.size() - loads(PARENT) >= 12) return null;
    if (kind == SECONDARY && loading.size() - loads(PARENT) >= 14) return null;
    trim();
    if (tiles.size() + loading.size() >= limit && !evictOne()) return null;
    var future = MapIO.submit(() -> MapCodec.read(root.resolve(key.file()), key));
    if (future != null) { loading.put(key, future); loadKinds.put(key, kind); }
    return null;
  }

  public boolean canSample() {
    return !closed && !retiring && parents.size() < limit;
  }

  public void changed(MapTile tile) {
    if (tile.key.level() == 0) {
      refresh(tile);
      refreshEdges(tile.key);
    }
    queueParent(tile);
  }

  private void queueParent(MapTile tile) {
    generation++;
    for (var listener : listeners) listener.accept(tile.key);
    if (tile.key.level() < MAX_LEVEL) parents.put(tile.key, tile.copy());
  }

  private boolean refresh(MapTile tile) {
    return tile.refreshRelief(
        (x, z) -> {
          MapTile neighbor = tiles.get(key(tile.key.dimension(), tile.key.layer(), 0, x, z, tile.key.context()));
          int i = index(x, z);
          return neighbor != null && neighbor.known(i) ? neighbor.height(i) : -1;
        });
  }

  private void refreshEdges(Key key) {
    for (int dx = 0; dx <= 1; dx++)
      for (int dz = 0; dz <= 1; dz++) {
        if (dx == 0 && dz == 0) continue;
        if (Math.abs((long) key.x() + dx) > BORDER / SIZE + 1
            || Math.abs((long) key.z() + dz) > BORDER / SIZE + 1) continue;
        MapTile neighbor =
            tiles.get(new Key(key.dimension(), key.layer(), 0, key.x() + dx, key.z() + dz, key.context()));
        if (neighbor == null || neighbor.readOnly) continue;
        if (dx == 0) for (int x = 0; x < SIZE; x++) neighbor.invalidateReliefPixel(x);
        else if (dz == 0) for (int z = 1; z < SIZE; z++) neighbor.invalidateReliefPixel(z * SIZE);
        else neighbor.invalidateReliefPixel(0);
        if (refresh(neighbor)) queueParent(neighbor);
      }
  }

  public void tick() {
    if (closed) return;
    drain();
    // Preserve failed dirty snapshots without retrying disabled functionality forever.
    if (retiring && writeFailed) return;
    int work = 0;
    for (Key key : new ArrayList<>(parents.keySet())) {
      if (work++ >= 4) break;
      MapTile child = parents.get(key);
      if (child == null) {
        parents.remove(key);
        continue;
      }
      MapTile parent = get(key.parent(), true);
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
    drainReads(16, Long.MAX_VALUE);
    drainSaves();
  }

  /** Publish completed immutable reads on the owning game/render thread, without advancing a tick. */
  public int publish(int maximum, long deadline) {
    if (closed || retiring || maximum <= 0) return 0;
    return drainReads(Math.min(4, maximum), deadline);
  }

  private int drainReads(int maximum, long deadline) {
    int published = 0;
    for (var it = loading.entrySet().iterator(); it.hasNext(); ) {
      if (published >= maximum || System.nanoTime() >= deadline) break;
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
      generation++;
      if (tile.key.level() == 0 && !tile.readOnly) {
        // Reconcile resident seams as either side arrives, without requesting new terrain.
        if (tile.key.z() > -BORDER / SIZE - 1
            && tiles.containsKey(
                new Key(tile.key.dimension(), tile.key.layer(), 0, tile.key.x(), tile.key.z() - 1, tile.key.context())))
          for (int x = 0; x < SIZE; x++) tile.invalidateReliefPixel(x);
        if (tile.key.x() > -BORDER / SIZE - 1
            && tiles.containsKey(
                new Key(tile.key.dimension(), tile.key.layer(), 0, tile.key.x() - 1, tile.key.z(), tile.key.context())))
          for (int z = 0; z < SIZE; z++) tile.invalidateReliefPixel(z * SIZE);
        if (tile.key.x() > -BORDER / SIZE - 1
            && tile.key.z() > -BORDER / SIZE - 1
            && tiles.containsKey(
                new Key(
                    tile.key.dimension(), tile.key.layer(), 0, tile.key.x() - 1, tile.key.z() - 1, tile.key.context())))
          tile.invalidateReliefPixel(0);
        if (refresh(tile) || tile.propagateRelief) queueParent(tile);
        tile.propagateRelief = false;
        refreshEdges(tile.key);
      }
      loadKinds.remove(entry.getKey());
      it.remove();
      published++;
    }
    return published;
  }

  private void drainSaves() {
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
    retiring = false;
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
