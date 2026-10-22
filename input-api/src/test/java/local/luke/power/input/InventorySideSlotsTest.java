package local.luke.power.input;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InventorySideSlotsTest {
  private static final java.awt.image.BufferedImage FRAME = frameImage();
  private static java.awt.image.BufferedImage frameImage() {
    try (var input = InventorySideSlotsTest.class.getResourceAsStream("/assets/" + InventorySideSlots.FRAME_TEXTURE.replace(':', '/'))) {
      return javax.imageio.ImageIO.read(input);
    } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
  }
  private static void raster(int[][] pixels, int left, int top, int width, int height, int u, int v, int sw, int sh) {
    for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
      int color = FRAME.getRGB(u + x * sw / width, v + y * sh / height);
      if ((color >>> 24) != 0) pixels[top + y][left + x] = color;
    }
  }
  @Test void containerFramesAndHitsFollowTheirOwnHotbarAndBottom() {
    for (int[] layout : new int[][]{{142, 166}, {143, 168}, {197, 222}}) {
      int hotbarY = layout[0], height = layout[1], x = 30, top = hotbarY - 5;
      int[][] pixels = new int[height + 1][40];
      InventorySideSlots.frame((px, py, w, h, u, v, sw, sh) -> raster(pixels, px, py, w, h, u, v, sw, sh), x, 0, height, hotbarY, false);
      assertEquals(0, pixels[top - 1][9]);
      assertEquals(0xff000000, pixels[top][9]);
      assertEquals(0xffffffff, pixels[top][x]);
      assertEquals(0xff000000, pixels[height - 1][9]);
      assertEquals(0, pixels[height][9]);
      assertTrue(InventorySideSlots.contains(8, top, x, 0, height, hotbarY, false));
      assertTrue(InventorySideSlots.contains(33, height - 1, x, 0, height, hotbarY, false));
      assertFalse(InventorySideSlots.contains(7, hotbarY, x, 0, height, hotbarY, false));
      assertFalse(InventorySideSlots.contains(34, hotbarY, x, 0, height, hotbarY, false));
      assertFalse(InventorySideSlots.contains(8, top - 1, x, 0, height, hotbarY, false));
      assertFalse(InventorySideSlots.contains(8, height, x, 0, height, hotbarY, false));
    }
  }

  @Test void upperJoinHasNoBlackOverhangAndLowerBorderRemainsContinuous() {
    for (boolean destroy : new boolean[]{false, true}) {
      int[][] pixels = new int[166][40];
      InventorySideSlots.frame((px, py, w, h, u, v, sw, sh) -> raster(pixels, px, py, w, h, u, v, sw, sh), 30, 0, 166, destroy);
      int top = InventorySideSlots.top(destroy);
      for (int x = 30; x < 33; x++) assertEquals(0xffffffff, pixels[top][x]);
      assertEquals(0xffc6c6c6, pixels[top][33]);
      for (int x = 9; x < 34; x++) assertEquals(0xff000000, pixels[165][x]);
      assertEquals(0xff000000, pixels[top][10]);
      assertEquals(0xff000000, pixels[top + 1][8]);
      assertEquals(0xffffffff, pixels[top + 1][10]);
    }
  }

  @Test void nativeLeftBevelJoinsTheExtensionWithoutAGrayGap() {
    for (boolean destroy : new boolean[]{false, true}) {
      int[][] pixels = new int[166][40];
      for (int y = 0; y < pixels.length; y++) {
        pixels[y][30] = 0xff000000;
        pixels[y][31] = pixels[y][32] = 0xffffffff;
        for (int x = 33; x < 40; x++) pixels[y][x] = 0xffc6c6c6;
      }
      InventorySideSlots.frame((px, py, w, h, u, v, sw, sh) -> raster(pixels, px, py, w, h, u, v, sw, sh), 30, 0, 166, destroy);
      int top = InventorySideSlots.top(destroy);
      for (int y = top - 1; y <= top + 2; y++)
        assertEquals(0xffffffff, pixels[y][31], "The native white bevel must reach the extension ledge");
      assertEquals(0xff000000, pixels[top - 1][30]);
      for (int y = top; y <= top + 2; y++) assertNotEquals(0xff000000, pixels[y][30]);
      for (int x = 9; x < 34; x++) assertEquals(0xff000000, pixels[165][x]);
    }
  }
}
