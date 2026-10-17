package local.luke.power.light;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class LightGlyphsTest {
  @Test void everyLightValueKeepsNativeCenteringGlyphOrderAndTextureCoordinates() {
    int[] advances = new int[10]; Arrays.fill(advances, 6); advances[1] = 4;
    double[][] glyphs = LightGlyphs.create(advances);
    for (int level = 0; level < 16; level++) {
      String text = Integer.toString(level); int width = 0;
      for (char c : text.toCharArray()) width += advances[c - '0'];
      assertEquals(text.length() * 16, glyphs[level].length);
      assertEquals(-width / 2, glyphs[level][0]);
      for (int digit = 0; digit < text.length(); digit++) {
        char c = text.charAt(digit); int offset = digit * 16;
        assertEquals(c % 16 * 8 / 128d, glyphs[level][offset + 2]);
        assertEquals(c / 16 * 8 / 128d, glyphs[level][offset + 15]);
        for (int corner = 0; corner < 4; corner++) {
          assertTrue(glyphs[level][offset + corner * 4 + 2] >= 0 && glyphs[level][offset + corner * 4 + 2] <= 1);
          assertTrue(glyphs[level][offset + corner * 4 + 3] >= 0 && glyphs[level][offset + corner * 4 + 3] <= 1);
        }
      }
    }
  }
}
