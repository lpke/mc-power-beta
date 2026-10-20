package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MapReadPriorityTest {
  @TempDir Path root;
  private static MapMath.Key key(int x) { return new MapMath.Key(0,-1,0,x,0); }
  @Test void newViewCancelsOnlyObsoleteQueuedReadsAndKeepsSamplingAndDurableWork() throws Exception {
    var cache=new MapCache(root,64,message->fail(message));
    var started=new CountDownLatch(1);var release=new CountDownLatch(1);
    MapIO.submit(()->{started.countDown();assertTrue(release.await(10,TimeUnit.SECONDS));return null;});
    assertTrue(started.await(10,TimeUnit.SECONDS));
    try {
      cache.visibleView(0,-1,new MapCoverage.Bounds(0,0,7,0,0));
      for(int x=0;x<8;x++)cache.visible(key(x));
      assertEquals(8,cache.pending());
      cache.sample(key(0)); cache.secondary(key(1));
      Path journal=root.resolve("durable-proof");
      MapIO.submit(()->{MapFiles.replace(journal,new byte[]{4,5,6});return null;});
      cache.visibleView(0,-1,new MapCoverage.Bounds(0,100,100,0,0));
      assertEquals(2,cache.pending(),"sampling and secondary-view reads must survive cancellation");
      cache.visible(key(100));
      assertEquals(3,cache.pending());
      release.countDown();MapIO.awaitQueued();cache.publish(4,Long.MAX_VALUE);
      assertNotNull(cache.peek(key(0)));assertNotNull(cache.peek(key(1)));assertNotNull(cache.peek(key(100)));
      for(int x=2;x<8;x++)assertNull(cache.peek(key(x)));
      assertArrayEquals(new byte[]{4,5,6},Files.readAllBytes(journal));
    } finally {release.countDown();MapIO.awaitQueued();cache.finish();}
  }
  @Test void viewReadsCannotUseTheSamplingAndParentMaintenanceSlots() throws Exception {
    var cache=new MapCache(root,64,message->fail(message));
    cache.get(key(-1));MapIO.awaitQueued();cache.tick();
    var tile=cache.get(key(-1));tile.put(0,0x123456,0x654321,64,15,1,0);cache.changed(tile);
    var started=new CountDownLatch(1);var release=new CountDownLatch(1);
    MapIO.submit(()->{started.countDown();assertTrue(release.await(10,TimeUnit.SECONDS));return null;});
    assertTrue(started.await(10,TimeUnit.SECONDS));
    try {
      for(int x=0;x<8;x++)cache.visible(key(x));
      cache.visible(key(80));assertEquals(8,cache.pending());
      for(int x=10;x<14;x++)cache.sample(key(x));
      cache.sample(key(90));assertEquals(12,cache.pending());
      cache.secondary(key(20));cache.secondary(key(21));cache.secondary(key(22));
      assertEquals(14,cache.pending(),"secondary preview must admit only two reads");
      cache.tick();assertTrue(cache.pending()>14,"parent read and dirty-save maintenance need reserved admission");
    } finally {release.countDown();MapIO.awaitQueued();cache.finish();}
    assertEquals(0x123456,MapCodec.read(root.resolve(tile.key.file()),tile.key).accurate[0]);
    assertTrue(Files.exists(root.resolve(tile.key.parent().file())));
  }
  @Test void thinAndNegativeViewsEnumerateExactlyTheirBoundedCellsNearestFirst() {
    for(var bounds:new MapCoverage.Bounds[]{new MapCoverage.Bounds(0,-512,511,-9,-9),new MapCoverage.Bounds(0,-8,7,-8,7)}) {
      var cells=MapTileSweep.nearest(bounds,0,-9);
      assertEquals(bounds.count(),cells.size());assertEquals(cells.size(),new java.util.HashSet<>(cells).size());
      long previous=-1;
      for(var cell:cells){long distance=(long)cell.x()*cell.x()+(long)(cell.z()+9)*(cell.z()+9);assertTrue(distance>=previous);previous=distance;}
    }
  }
}
