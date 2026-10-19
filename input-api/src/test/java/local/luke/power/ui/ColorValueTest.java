package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class ColorValueTest {
  static final class Saved { int color; int opacity = 80; }

  @Test void legacyPaletteAndEveryLowRgbValueRoundTripWithGson289() {
    Gson gson = new Gson();
    for (ColorValue.Palette palette : ColorValue.Palette.values()) {
      for (int legacy = 0; legacy < 4; legacy++) {
        Saved saved = gson.fromJson("{\"color\":" + legacy + ",\"opacity\":60}", Saved.class);
        assertEquals(legacy, palette.encode(palette.rgb(saved.color)));
        assertEquals(60, saved.opacity);
        assertEquals(legacy, gson.fromJson(gson.toJson(saved), Saved.class).color);
      }
      for (int rgb : new int[] {0, 1, 2, 3, 4, 0x10203, 0xffffff}) {
        Saved saved = new Saved(); saved.color = palette.encode(rgb);
        assertEquals(ColorValue.CUSTOM | rgb, saved.color);
        Saved loaded = gson.fromJson(gson.toJson(saved), Saved.class);
        assertEquals(rgb, palette.rgb(loaded.color));
        assertEquals(80, loaded.opacity);
      }
    }
  }

  @Test void invalidLegacyAndCustomValuesStayRejected() {
    for (ColorValue.Palette palette : ColorValue.Palette.values()) {
      for (int invalid : new int[] {-1, 4, 255, 0xffffff, 0x2000000, Integer.MAX_VALUE})
        assertThrows(IllegalArgumentException.class, () -> palette.rgb(invalid));
      assertThrows(IllegalArgumentException.class, () -> palette.encode(-1));
      assertThrows(IllegalArgumentException.class, () -> palette.encode(0x1000000));
    }
  }
}
