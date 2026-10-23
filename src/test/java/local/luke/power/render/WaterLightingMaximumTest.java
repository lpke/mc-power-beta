package local.luke.power.render;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WaterLightingMaximumTest {
  @Test void maximumShortcutHonorsEveryEntryIncludingCustomAndChangedTables() {
    float[] table = new float[16];
    for (int i = 0; i < 16; i++) table[i] = i / 15F;
    assertTrue(WaterLighting.maximum(table, 1));
    assertFalse(WaterLighting.maximum(table, .9F));
    table[2] = 1.25F;
    assertFalse(WaterLighting.maximum(table, 1));
    assertTrue(WaterLighting.maximum(table, 1.25F));
    table[2] = Float.NaN;
    assertFalse(WaterLighting.maximum(table, 1.25F));
    assertFalse(WaterLighting.maximum(table, Float.NaN));
  }
}
