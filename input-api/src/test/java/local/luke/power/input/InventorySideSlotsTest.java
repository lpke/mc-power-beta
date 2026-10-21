package local.luke.power.input;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InventorySideSlotsTest {
  @Test void upperJoinHasNoBlackOverhangAndLowerBorderRemainsContinuous() {
    for (boolean destroy : new boolean[]{false, true}) {
      int[][] pixels = new int[166][40];
      InventorySideSlots.frame((x0, y0, x1, y1, color) -> {
        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) pixels[y][x] = color;
      }, 30, 0, 166, destroy);
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
      InventorySideSlots.frame((x0, y0, x1, y1, color) -> {
        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) pixels[y][x] = color;
      }, 30, 0, 166, destroy);
      int top = InventorySideSlots.top(destroy);
      for (int y = top - 1; y <= top + 2; y++)
        assertEquals(0xffffffff, pixels[y][31], "The native white bevel must reach the extension ledge");
      assertEquals(0xff000000, pixels[top - 1][30]);
      for (int y = top; y <= top + 2; y++) assertNotEquals(0xff000000, pixels[y][30]);
      for (int x = 9; x < 34; x++) assertEquals(0xff000000, pixels[165][x]);
    }
  }
}
