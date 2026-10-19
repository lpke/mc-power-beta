package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MapExportTest {
  @TempDir Path root;

  @Test
  void exportKeepsTheCompleteViewAndAspectRatioWhenDownscaled() throws Exception {
    var key = new MapMath.Key(0, -1, 0, 0, 0);
    var tile = new MapTile(key);
    for (int i = 0; i < 4096; i++) tile.put(i, i % 64 < 32 ? 0xff0000 : 0x0000ff, 0, 64, 15, 1, 1);
    MapCodec.write(root.resolve(key.file()), tile);
    var errors = new ArrayList<String>();
    var cache = new MapCache(root, 64, errors::add);
    var settings = new MapSettings();
    settings.heightShading = false;
    settings.slopeShading = false;
    MapExport.start(
        cache, new MapProjection(32, 32, 128, 0, 0, 8192, 4096, 0), 0, -1, settings, 0, true);
    String status = "";
    for (int i = 0; i < 10000; i++) {
      status = MapExport.status();
      if (status.startsWith("Exported") || status.contains("failed")) break;
      Thread.sleep(1);
    }
    assertTrue(status.startsWith("Exported"), status);
    Path file;
    try (var files = Files.list(root.resolve("exports"))) {
      file = files.findFirst().orElseThrow();
    }
    var image = ImageIO.read(file.toFile());
    assertEquals(4096, image.getWidth());
    assertEquals(2048, image.getHeight());
    assertEquals(0xffff0000, image.getRGB(100, 100));
    assertEquals(0xff0000ff, image.getRGB(4000, 100));
    cache.finish();
    assertTrue(errors.isEmpty());
  }

  @Test
  void exportsAtBothWorldBordersLeaveOutsidePixelsTransparent() throws Exception {
    var cache = new MapCache(root, 64, m -> fail(m));
    var settings = new MapSettings();
    settings.heightShading = false;
    settings.slopeShading = false;
    for (int border : new int[] {-MapMath.BORDER, MapMath.BORDER}) {
      var key = MapMath.key(0, -1, 0, border < 0 ? border : border - 1, 0);
      var tile = new MapTile(key);
      for (int i = 0; i < 4096; i++) tile.put(i, 0x22cc44, 0, 64, 15, 1, 1);
      MapCodec.write(root.resolve(key.file()), tile);
      MapExport.start(
          cache, new MapProjection(border, 32, 1, 0, 0, 200, 100, 0), 0, -1, settings, 0, true);
      String status = "";
      for (int i = 0; i < 5000; i++) {
        status = MapExport.status();
        if (status.startsWith("Exported") || status.contains("failed")) break;
        Thread.sleep(1);
      }
      assertTrue(status.startsWith("Exported"), status);
      var image =
          ImageIO.read(
              root.resolve("exports").resolve(status.substring("Exported ".length())).toFile());
      assertEquals(0xff22cc44, image.getRGB(border < 0 ? 104 : 96, 50));
      assertEquals(0, image.getRGB(border < 0 ? 96 : 104, 50));
    }
    cache.finish();
  }

  @Test
  void workerQueueRejectsExcessWorkWithoutBlockingTheGameThread() throws Exception {
    var started = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    var first =
        MapIO.submit(
            () -> {
              started.countDown();
              release.await();
              return 1;
            });
    assertNotNull(first);
    assertTrue(started.await(5, java.util.concurrent.TimeUnit.SECONDS));
    var accepted = new ArrayList<java.util.concurrent.CompletableFuture<Integer>>();
    try {
      for (int i = 0; i < 64; i++) {
        var request = MapIO.submit(() -> 2);
        assertNotNull(request);
        accepted.add(request);
      }
      assertEquals(64, MapIO.queued());
      assertNull(MapIO.submit(() -> 3));
    } finally {
      release.countDown();
      first.join();
      MapIO.awaitQueued();
      assertTrue(accepted.stream().allMatch(java.util.concurrent.CompletableFuture::isDone));
      accepted.forEach(java.util.concurrent.CompletableFuture::join);
    }
    assertEquals(0, MapIO.queued());
  }
}
