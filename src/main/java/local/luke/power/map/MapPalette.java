package local.luke.power.map;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

/** Texture averaging is worker-owned. Native color hooks are sampled only by the game thread. */
public final class MapPalette {
  private static final String[] BIOMES = {
    "Unknown",
    "Rainforest",
    "Swampland",
    "Seasonal Forest",
    "Forest",
    "Savanna",
    "Shrubland",
    "Taiga",
    "Desert",
    "Plains",
    "Ice Desert",
    "Tundra",
    "Hell",
    "Sky"
  };
  private String pack;
  private int[] colors = new int[256];
  private CompletableFuture<int[]> loading;
  private int revision;

  public int revision() {
    return revision;
  }

  public static int biome(String name) {
    for (int i = 1; i < BIOMES.length; i++) if (BIOMES[i].equals(name)) return i;
    return 0;
  }

  public static String biome(int id) {
    return id >= 0 && id < BIOMES.length ? BIOMES[id] : "Unknown";
  }

  public void tick(Minecraft mc) {
    String next = mc.options.skin == null ? "Default" : mc.options.skin;
    if (loading != null && loading.isDone()) {
      try {
        colors = loading.join();
        revision++;
      } catch (RuntimeException e) {
        local.luke.power.PowerBeta.LOG.warn("Map texture colors unavailable", e);
      }
      loading = null;
    }
    if (loading != null || next.equals(pack)) return;
    Path folder =
        FabricLoader.getInstance()
            .getGameDir()
            .resolve("texturepacks")
            .toAbsolutePath()
            .normalize();
    loading = MapIO.submit(() -> read(folder, next));
    if (loading != null) pack = next;
  }

  static int[] read(Path folder, String name) throws IOException {
    Path path = folder.resolve(name).normalize();
    if (path.startsWith(folder) && !path.equals(folder) && Files.isRegularFile(path)) {
      try (ZipFile zip = new ZipFile(path.toFile())) {
        var entry = zip.getEntry("terrain.png");
        if (entry != null && entry.getSize() <= 64 * 1024 * 1024)
          try (var input = zip.getInputStream(entry)) {
            return average(input);
          }
      }
    }
    try (var input = MapPalette.class.getResourceAsStream("/terrain.png")) {
      return average(input);
    }
  }

  static int[] average(InputStream input) throws IOException {
    if (input == null) throw new IOException("Missing terrain texture");
    try (var stream = ImageIO.createImageInputStream(input)) {
      var readers = ImageIO.getImageReaders(stream);
      if (!readers.hasNext()) throw new IOException("Unknown terrain image");
      var reader = readers.next();
      try {
        reader.setInput(stream);
        int w = reader.getWidth(0), h = reader.getHeight(0);
        if (w < 16 || h < 16 || w > 8192 || h > 8192 || w % 16 != 0 || h % 16 != 0)
          throw new IOException("Unsupported terrain image dimensions");
        BufferedImage image = reader.read(0);
        int[] result = new int[256];
        int tw = w / 16, th = h / 16;
        for (int tile = 0; tile < 256; tile++) {
          long r = 0, g = 0, b = 0, a = 0, count = 0;
          int stepX = Math.max(1, Math.min(4, tw / 8));
          int stepY = Math.max(1, Math.min(4, th / 8));
          for (int y = 0; y < th; y += stepY)
            for (int x = 0; x < tw; x += stepX) {
              int pixel = image.getRGB((tile % 16) * tw + x, (tile / 16) * th + y),
                  alpha = pixel >>> 24;
              // Cutout holes are not black pigment. Keep translucent pigment and its
              // opacity separate so a surface can be composed over the block below.
              if (alpha <= 10) continue;
              r += pixel >> 16 & 255;
              g += pixel >> 8 & 255;
              b += pixel & 255;
              a += alpha;
              count++;
            }
          if (count > 0)
            result[tile] =
                (int) (a / count) << 24
                    | (int) (r / count) << 16
                    | (int) (g / count) << 8
                    | (int) (b / count);
        }
        return result;
      } finally {
        reader.dispose();
      }
    }
  }

  /** ARGB before terrain lighting. Beta's animated fluids have no static atlas pigment. */
  int surfaceColor(Block block, World world, int x, int y, int z) {
    if (block.field_1900 == Material.WATER) return 0xbf386bc7;
    if (block.field_1900 == Material.LAVA) return 0xfff36b12;
    int texture = block.method_1626(world, x, y, z, 1);
    int base = texture >= 0 && texture < colors.length ? colors[texture] : 0;
    int color;
    if (base == 0) {
      color = nativeColor(block, world, x, y, z);
      base = 0xff000000;
    } else {
      int tint = block.method_1600(world, x, y, z);
      color =
          ((base >> 16 & 255) * (tint >> 16 & 255) / 255) << 16
              | ((base >> 8 & 255) * (tint >> 8 & 255) / 255) << 8
              | (base & 255) * (tint & 255) / 255;
    }
    int alpha = base >>> 24;
    // Glass has opaque borders around cutout holes; averaging those borders alone
    // would otherwise turn a glass roof into a solid white patch. Ice is translucent.
    if (block.id == Block.GLASS.id) alpha = Math.min(alpha, 96);
    else if (block.id == 79) alpha = Math.min(alpha, 160);
    return alpha << 24 | color;
  }

  public static int nativeColor(Block block, World world, int x, int y, int z) {
    int color = block.field_1900.field_973.field_2752;
    if (block.id == Block.GRASS.id || block.field_1900 == Material.LEAVES)
      color = block.method_1600(world, x, y, z);
    if (block.id == 35)
      color =
          new int[] {
                0xeeeeee, 0xe08222, 0xb24bc4, 0x6699d8, 0xd7c23b, 0x68ac32, 0xd28099, 0x444444,
                0x999999, 0x337788, 0x793eb4, 0x334c99, 0x65482c, 0x456020, 0xa2322a, 0x202020
              }
              [world.method_1778(x, y, z) & 15];
    return color == 0 ? 0x777777 : color;
  }
}
