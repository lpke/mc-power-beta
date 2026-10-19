package local.luke.power.map;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import local.luke.power.config.Catalog;

/** Exports the visible area, bounded to 4096 square pixels and 1024 tiles. */
public final class MapExport {
  private static CompletableFuture<Path> writing;
  private static String status = "";

  private MapExport() {}

  public static String status() {
    if (writing != null && writing.isDone()) {
      try {
        status = "Exported " + writing.join().getFileName();
      } catch (RuntimeException e) {
        status = "Map export failed: " + MapCache.reason(e);
      }
      writing = null;
    }
    return status;
  }

  public static void start(
      MapCache cache,
      MapProjection view,
      int dimension,
      int layer,
      MapSettings settings,
      long seed,
      boolean local) {
    if (writing != null) {
      status = "A map export is already running";
      return;
    }
    double reduction = Math.min(1, 4096 / Math.max(view.width, view.height));
    int width = Math.max(1, (int) Math.round(view.width * reduction)),
        height = Math.max(1, (int) Math.round(view.height * reduction));
    double outputScale = view.scale * reduction;
    var coverage = MapCoverage.select(view, false, 1024);
    int level = coverage.level(), span = MapMath.SIZE << level;
    double minX = view.x - view.width / 2 / view.scale,
        minZ = view.z - view.height / 2 / view.scale;
    int lowX = coverage.minX(),
        highX = coverage.maxX(),
        lowZ = coverage.minZ(),
        highZ = coverage.maxZ();
    MapSettings style =
        Catalog.JSON.fromJson(
            Catalog.JSON.toJson(settings), MapSettings.class);
    String context = layer < 0 ? "" : cache.viewContext();
    Map<MapMath.Key, MapTile> snapshot = new HashMap<>();
    for (int z = lowZ; z <= highZ; z++)
      for (int x = lowX; x <= highX; x++) {
        var key = new MapMath.Key(dimension, layer, level, x, z, context);
        MapTile tile = cache.peek(key);
        if (tile != null) snapshot.put(key, tile.copy());
      }
    Path root = cache.root();
    Path output =
        root.resolve(
            "exports/map-" + System.currentTimeMillis() + "-" + UUID.randomUUID() + ".png");
    writing =
        MapIO.submit(
            () -> {
              BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
              for (int ty = lowZ; ty <= highZ; ty++)
                for (int tx = lowX; tx <= highX; tx++) {
                  var key = new MapMath.Key(dimension, layer, level, tx, ty, context);
                  MapTile tile = snapshot.get(key);
                  if (tile == null) tile = MapCodec.read(root.resolve(key.file()), key);
                  int x0 = Math.max(0, (int) Math.floor((key.minX() - minX) * outputScale)),
                      x1 =
                          Math.min(
                              width, (int) Math.ceil((key.minX() + span - minX) * outputScale));
                  int y0 = Math.max(0, (int) Math.floor((key.minZ() - minZ) * outputScale)),
                      y1 =
                          Math.min(
                              height, (int) Math.ceil((key.minZ() + span - minZ) * outputScale));
                  for (int py = y0; py < y1; py++)
                    for (int px = x0; px < x1; px++) {
                      double wx = minX + (px + .5) / outputScale,
                          wz = minZ + (py + .5) / outputScale;
                      if (Math.abs(wx) > MapMath.BORDER || Math.abs(wz) > MapMath.BORDER) continue;
                      int ix = (int) Math.floor((wx - key.minX()) / (1 << level)),
                          iz = (int) Math.floor((wz - key.minZ()) / (1 << level));
                      if (ix >= 0 && ix < 64 && iz >= 0 && iz < 64)
                        image.setRGB(px, py, tile.color(iz * 64 + ix, style));
                    }
                }
              ByteArrayOutputStream bytes = new ByteArrayOutputStream();
              if (!ImageIO.write(image, "PNG", bytes))
                throw new java.io.IOException("PNG encoder is unavailable");
              MapFiles.replace(output, bytes.toByteArray());
              return output;
            });
    status =
        writing == null ? "Map storage is busy; try exporting again" : "Exporting visible map…";
  }
}
