package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class NativeHudLayoutTest {
  @Test void nativeDefaultsAndDisabledPositioningRemainExact() {
    for (int width : new int[]{320, 427, 640}) for (int height : new int[]{240, 301, 420})
      for (int element = 0; element < 5; element++) {
        assertEquals(0, NativeHudLayout.xOffset(width, element, 0, 0, 103));
        assertEquals(0, NativeHudLayout.yOffset(height, element, 0, 2, true, true));
        assertEquals(0, NativeHudLayout.xOffset(width, element, 1, 1, 103));
        assertEquals(0, NativeHudLayout.yOffset(height, element, 1, 0, false, false));
      }
  }
  @Test void separateAnchorsReachTheNativeScreenEdgesIncludingOddDimensions() {
    for (int width : new int[]{320, 427, 640}) for (int element = 0; element < 5; element++) {
      int textWidth = 103, size = element == 0 ? 182 : element == 4 ? textWidth : 81;
      int left = NativeHudLayout.baseX(width, element, textWidth) + NativeHudLayout.xOffset(width, element, 2, 0, textWidth);
      int right = NativeHudLayout.baseX(width, element, textWidth) + NativeHudLayout.xOffset(width, element, 2, 2, textWidth);
      assertEquals(0, left);
      assertEquals(width, right + size);
    }
  }
  @Test void combinedHudKeepsSpacingAndBelowControlsStayIndependent() {
    for (int element = 0; element < 5; element++) {
      int normal = NativeHudLayout.yOffset(301, element, 1, 2, false, false);
      assertEquals(52 - 301, normal);
      assertEquals(element == 0 || element == 4 ? 0 : element == 3 ? 51 : 33,
          NativeHudLayout.yOffset(301, element, 1, 2, true, false) - normal);
      assertEquals(element == 4 ? 74 : 0, NativeHudLayout.yOffset(301, element, 1, 2, false, true) - normal);
    }
  }
}
