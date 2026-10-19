package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MapRefreshQueueTest {
  private static MapMath.Key key(int x, int z) { return new MapMath.Key(0, -1, 0, x, z); }
  private static MapProjection wideView() { return new MapProjection(32, 32, 1, 200, 50, 1024, 128, 0); }

  @Test void aReadyVisibleUpdateWinsMissingCenterAndCloserOffscreenTilesInAnUltrawideView() {
    var queue = new MapRefreshQueue();
    var missingCenter = key(0, 0); var offscreen = key(0, 2); var visible = key(7, 0);
    queue.add(missingCenter); queue.add(offscreen); queue.add(visible);
    var ready = Set.of(offscreen, visible);
    assertEquals(visible, queue.take(ready::contains, wideView()));
    assertEquals(offscreen, queue.take(ready::contains, wideView()));
    assertNull(queue.take(ready::contains, wideView()));
    assertEquals(1, queue.size(), "a missing update must survive until its tile becomes ready");
    assertEquals(missingCenter, queue.take(k -> true, wideView()));
    assertTrue(queue.isEmpty());
  }

  @Test void partialTileIntersectionCountsAsVisibleAndReadyVisibleTilesRemainNearestFirst() {
    var queue = new MapRefreshQueue();
    var offscreen = key(0, 2); var partial = key(8, 0); var near = key(1, 0);
    // The right edge is world X=532; the partial tile starts at512 but its center is544.
    var view = new MapProjection(32, 32, 1, 20, 10, 1000, 128, 0);
    queue.add(offscreen); queue.add(partial); queue.add(near);
    assertEquals(near, queue.take(k -> true, view));
    assertEquals(partial, queue.take(k -> true, view));
    assertEquals(offscreen, queue.take(k -> true, view));
  }

  @Test void oneUploadSlotAlternatesLiveUpdatesWithInitialFillWithoutStarvingEither() {
    var updates = new MapRefreshQueue(); var update = key(0, 0);
    var fill = new ArrayDeque<>(List.of(key(1, 0), key(2, 0), key(3, 0)));
    var accepted = new ArrayList<MapMath.Key>();
    for (int frame = 0; frame < 6; frame++) {
      updates.add(update);
      accepted.add(updates.updatesFirst(!fill.isEmpty()) ? updates.take(k -> true, wideView()) : fill.removeFirst());
    }
    assertEquals(List.of(update, key(1, 0), update, key(2, 0), update, key(3, 0)), accepted);
    assertTrue(fill.isEmpty());
    for (int frame = 0; frame < 4; frame++) assertTrue(updates.updatesFirst(false));
    updates.clear();
    assertTrue(updates.updatesFirst(true), "a new surface must start with its ready updates");
  }

  @Test void missingReadAdmissionIsBoundedRotatesAndNeverDropsOrDuplicatesQueuedUpdates() {
    var queue = new MapRefreshQueue(); var ready = key(100, 100);
    queue.add(ready);
    var missing = new LinkedHashSet<MapMath.Key>();
    for (int x = 0; x < 7; x++) { missing.add(key(x, 0)); assertTrue(queue.add(key(x, 0))); }
    assertFalse(queue.add(key(0, 0)));
    var requested = new LinkedHashSet<MapMath.Key>();
    for (int pass = 0; pass < 4; pass++) {
      var batch = new ArrayList<MapMath.Key>(); var scans = new AtomicInteger();
      queue.requestMissing(k -> { scans.incrementAndGet(); return k.equals(ready); }, batch::add);
      assertEquals(2, batch.size()); assertEquals(2, new HashSet<>(batch).size());
      assertTrue(scans.get() <= 64); assertFalse(batch.contains(ready));
      requested.addAll(batch); assertEquals(8, queue.size());
    }
    assertEquals(missing, requested);
    assertEquals(ready, queue.take(k -> k.equals(ready), wideView()));
    assertEquals(7, queue.size());
  }

  @Test void eachReadinessScanIsBoundedAndLongMissingPrefixesRotateOutOfTheWay() {
    var queue = new MapRefreshQueue();
    for (int x = 0; x < 65; x++) queue.add(key(x, 0));
    var ready = key(66, 0); queue.add(ready);
    var scans = new AtomicInteger();
    assertNull(queue.take(k -> { scans.incrementAndGet(); return k.equals(ready); }, wideView()));
    assertEquals(64, scans.get()); scans.set(0);
    assertEquals(ready, queue.take(k -> { scans.incrementAndGet(); return k.equals(ready); }, wideView()));
    assertTrue(scans.get() <= 64); assertEquals(65, queue.size());
    scans.set(0); var requests = new ArrayList<MapMath.Key>();
    queue.requestMissing(k -> { scans.incrementAndGet(); return true; }, requests::add);
    assertEquals(64, scans.get()); assertTrue(requests.isEmpty());
  }
}
