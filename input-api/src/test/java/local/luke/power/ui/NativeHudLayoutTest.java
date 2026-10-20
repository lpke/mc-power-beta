package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class NativeHudLayoutTest {
  @Test void nativeDefaultsAndDisabledPositioningRemainExact() {
    for (int width : new int[]{320, 427, 640}) for (int height : new int[]{240, 301, 420})
      for (int element = 0; element <= NativeHudLayout.STAMINA; element++) {
        assertEquals(0, NativeHudLayout.xOffset(width, element, 0, 0, 103));
        assertEquals(0, NativeHudLayout.yOffset(height, element, 0, 2, true, true));
        assertEquals(0, NativeHudLayout.xOffset(width, element, 1, 1, 103));
        assertEquals(0, NativeHudLayout.yOffset(height, element, 1, 0, false, false));
      }
  }
  @Test void separateAnchorsReachTheNativeScreenEdgesIncludingOddDimensions() {
    for (int width : new int[]{320, 427, 640}) for (int element = 0; element <= NativeHudLayout.STAMINA; element++) {
      int textWidth = 103, size = element == 0 ? 182 : element == 4 ? textWidth : 81;
      int left = NativeHudLayout.baseX(width, element, textWidth) + NativeHudLayout.xOffset(width, element, 2, 0, textWidth);
      int right = NativeHudLayout.baseX(width, element, textWidth) + NativeHudLayout.xOffset(width, element, 2, 2, textWidth);
      assertEquals(0, left);
      assertEquals(width, right + size);
    }
  }
  @Test void combinedHudKeepsSpacingAndBelowControlsStayIndependent() {
    for (int element = 0; element <= NativeHudLayout.STAMINA; element++) {
      int normal = NativeHudLayout.yOffset(301, element, 1, 2, false, false);
      assertEquals(52 - 301, normal);
      assertEquals(element == 0 || element == 4 ? 0 : element == 3 ? 51 : 33,
          NativeHudLayout.yOffset(301, element, 1, 2, true, false) - normal);
      assertEquals(element == 4 ? 74 : 0, NativeHudLayout.yOffset(301, element, 1, 2, false, true) - normal);
    }
  }

  @Test void staminaSharesArmorAnchorsAndKeepsItsOwnOffsetAndNinePixelCollisionBounds() {
    for (int width : new int[]{320, 427, 640}) for (int height : new int[]{180, 301, 420})
      for (int mode = 0; mode <= 2; mode++) for (int anchor = 0; anchor <= 2; anchor++) {
        assertEquals(NativeHudLayout.baseX(width, NativeHudLayout.ARMOUR, 0), NativeHudLayout.baseX(width, NativeHudLayout.STAMINA, 0));
        assertEquals(NativeHudLayout.xOffset(width, NativeHudLayout.ARMOUR, mode, anchor, 0), NativeHudLayout.xOffset(width, NativeHudLayout.STAMINA, mode, anchor, 0));
        assertEquals(NativeHudLayout.yOffset(height, NativeHudLayout.ARMOUR, mode, anchor, true, false), NativeHudLayout.yOffset(height, NativeHudLayout.STAMINA, mode, anchor, true, false));
      }
    assertEquals(0, NativeHudLayout.staminaY(10, -100, false, 10, 0, 180));
    assertEquals(171, NativeHudLayout.staminaY(10, 200, false, 10, 0, 180));
    assertEquals(90, NativeHudLayout.staminaY(10, 100, true, 10, 100, 180));
    assertEquals(10, NativeHudLayout.staminaY(10, 0, true, 10, 0, 180));
    assertEquals(100, NativeHudLayout.staminaY(91, 100, true, 10, 100, 180), "adjacent rows do not collide");
  }

  @Test void separateStaminaOffsetsMoveImmediatelyFromTheArmorAwareAnchor() {
    int x = 170, y = 148;
    assertEquals(138, NativeHudLayout.staminaY(x, y, true, x, y, 180, 0, 0));
    assertEquals(137, NativeHudLayout.staminaY(x, y - 1, true, x, y, 180, 0, -1));
    assertEquals(128, NativeHudLayout.staminaY(x, y - 10, true, x, y, 180, 0, -10));
    assertEquals(138, NativeHudLayout.staminaY(x + 100, y, true, x, y, 180, 100, 0),
        "a horizontal drag cannot change the original armor-aware Y anchor");
    assertEquals(0, NativeHudLayout.staminaY(x, -100, true, x, y, 180, 0, -248));
    assertEquals(171, NativeHudLayout.staminaY(x, 500, true, x, y, 180, 0, 352));
  }
}
