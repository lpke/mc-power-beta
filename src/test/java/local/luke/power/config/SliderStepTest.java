package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SliderStepTest {
  private static Setting setting(String id, Setting.Kind kind, double min, double max, double step, double value) {
    return new Setting(id, "test", "", "", "Numeric setting", "", kind,
        new JsonPrimitive(value), new JsonPrimitive(value), min, max, step, List.of(), false);
  }
  @Test void songFadeAndBatchCountDragUseTenButExactValuesAndIntegerRulesStayIndependent() {
    for (String id : List.of("audio.musicFadeMillis", "audio.mp3ConversionLimit")) {
      double min = id.equals("audio.musicFadeMillis") ? 0 : 1, max = min == 0 ? 10000 : 2048;
      Setting setting = setting(id, Setting.Kind.INTEGER, min, max, 1, 137);
      assertEquals(10, setting.sliderStep, id); assertEquals(1, setting.step, id);
      assertEquals(137, setting.value.getAsInt(), "construction changed a saved precise value");
      for (double target : new double[]{23, 136, 499, 1001}) {
        setting.slide((target - min) / (max - min));
        assertEquals(Math.round(target / 10) * 10, setting.value.getAsInt(), id);
      }
      setting.slide(0); assertEquals(min, setting.value.getAsDouble());
      setting.slide(1); assertEquals(max, setting.value.getAsDouble());
      setting.parse("137"); assertEquals(137, setting.value.getAsInt());
      setting.cycle(1); assertEquals(138, setting.value.getAsInt(), "keyboard cycle inherited coarse drag step");
      assertThrows(IllegalArgumentException.class, () -> setting.parse("137.5"));
      setting.reset(); assertEquals(setting.defaultValue, setting.value);
    }
  }
  @Test void wideIntegerControlsRetainPreciseEntryAndNaturalZeroAlignedDragMultiples() {
    for (String id : List.of("activeTweaks.offsetX", "infoHud.clusters.2.offsetY", "subtitles.offsetX", "audio.menuControlsOffsetX",
        "veinMining.maxBlocks", "veinMining.maxChecks", "veinMining.checksPerTick", "gameplay.mergeThreshold", "maps.waypointDistance")) {
      double min = id.contains("offset") || id.contains("Offset") ? -4096 : 1;
      double max = id.equals("maps.waypointDistance") ? 1000000 : 16384;
      Setting setting = setting(id, Setting.Kind.INTEGER, min, max, 1, 137);
      assertTrue(setting.sliderStep > setting.step, id);
      setting.slide(.373);
      double quotient = setting.value.getAsDouble() / setting.sliderStep;
      assertEquals(Math.rint(quotient), quotient, 1e-9, id);
      setting.parse("137"); assertEquals(137, setting.value.getAsInt(), id);
      setting.slide(0); assertEquals(min, setting.value.getAsDouble());
      setting.slide(1); assertEquals(max, setting.value.getAsDouble());
    }
  }
  @Test void secondsDisplayedOverNativeTicksDragByOneSecondButExactSubsecondsSurvive() {
    for (String id : List.of("power_hud:config.chatFadeTime", "power_hud:config.hotbarItemSelectionFadeTime",
        "power_controls:features.fastLeafDecay.minimumDecayTime", "power_controls:features.fastLeafDecay.maximumDecayTime")) {
      double divisor = Catalog.number(Catalog.metadata(id), "displayDivisor", 1);
      Setting setting = setting(id, Setting.Kind.INTEGER, 1, 12000, 1, 3);
      assertEquals(divisor, setting.sliderStep, id);
      setting.slide(.421); assertEquals(Math.rint(setting.value.getAsDouble() / divisor), setting.value.getAsDouble() / divisor, id);
      String precise = java.math.BigDecimal.valueOf(3).divide(java.math.BigDecimal.valueOf(divisor)).toPlainString();
      setting.parse(precise); assertEquals(3, setting.value.getAsInt(), id);
      assertThrows(IllegalArgumentException.class, () -> setting.parse("0.001"));
    }
  }
  @Test void percentagesSensitivityFovAndExistingMapBudgetsKeepTheirOriginalDragResolution() {
    for (String id : List.of("native.sensitivity", "native.fov", "audio.master", "audio.category.blocks", "audio.sound.example")) {
      Setting setting = setting(id, Setting.Kind.INTEGER, 0, 200, 1, 100);
      assertEquals(setting.step, setting.sliderStep, id); setting.slide(.355); assertEquals(71, setting.value.getAsInt(), id);
    }
    Setting budget = Catalog.setting("maps.columnsPerTick", "test", "", "", "", "", Setting.Kind.INTEGER,
        new JsonPrimitive(96), new JsonPrimitive(96), 0, 1, 1, List.of(), false);
    assertEquals(32, budget.sliderStep); budget.parse("97"); assertEquals(97, budget.value.getAsInt());
    Setting decimal = setting("slider-test", Setting.Kind.DECIMAL, 0, 5, .1, 1.375);
    decimal.parse("2.375"); assertEquals(2.375, decimal.value.getAsDouble());
  }
}
