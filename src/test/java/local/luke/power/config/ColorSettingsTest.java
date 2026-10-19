package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.ui.ColorValue;
import local.luke.power.ui.SettingsDraft;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ColorSettingsTest {
  private Setting value(String id, JsonElement current, double min, double max, JsonElement fallback) {
    Setting.Kind kind = current.getAsJsonPrimitive().isString() ? Setting.Kind.TEXT
        : max <= 1 ? Setting.Kind.DECIMAL : Setting.Kind.INTEGER;
    return new Setting(id, "test", "Interface", "", id, "", kind, current, fallback,
        min, max, max <= 1 ? .01 : 1, List.of(), false);
  }
  private Setting color(String id) {
    return value(id, new JsonPrimitive("#FFFFFF"), 0, 0, new JsonPrimitive("#FFFFFF"));
  }
  private Setting number(String id, int current, int min, int max, int fallback) {
    return value(id, new JsonPrimitive(current), min, max, new JsonPrimitive(fallback));
  }

  @Test void everyDirectPairHidesOnlyItsOwnOpacityAndLabelsSharePercentages() {
    Map<String, String> pairs = new LinkedHashMap<>(Map.of(
        "worldedit.color", "worldedit.opacity", "tweaks.flexible.overlayColor", "tweaks.flexible.overlayOpacity",
        "activeTweaks.textColor", "activeTweaks.opacity", "maps.pathColor", "maps.pathOpacity", "maps.playerColor", "maps.playerOpacity"));
    for (int i = 0; i < 4; i++) pairs.put("infoHud.clusters." + i + ".textColor", "infoHud.clusters." + i + ".opacity");
    for (var pair : pairs.entrySet()) {
      boolean integer = pair.getKey().startsWith("maps.") || pair.getKey().equals("worldedit.color") || pair.getKey().equals("tweaks.flexible.overlayColor");
      Setting color = integer ? number(pair.getKey(), 0, 0, 0x1ffffff, 0) : color(pair.getKey());
      Setting opacity = pair.getKey().startsWith("maps.")
          ? value(pair.getValue(), new JsonPrimitive(.8), 0, 1, new JsonPrimitive(1))
          : number(pair.getValue(), 80, 0, 100, 100);
      var settings = List.of(color, opacity);
      assertTrue(ColorSettings.accepts(color), color.id);
      assertSame(opacity, ColorSettings.opacity(color, settings));
      assertTrue(ColorSettings.hiddenOpacity(opacity, settings));
      assertFalse(ColorSettings.hiddenOpacity(color, settings));
      assertEquals(ColorSettings.hex(color) + " 80%", ColorSettings.label(color, settings));
      assertFalse(ColorSettings.hiddenOpacity(opacity, List.of(opacity)));
    }
    assertEquals("#FFFFFF", ColorSettings.label("#FFFFFF", 100));
    assertEquals("#FFFFFF 0%", ColorSettings.label("#FFFFFF", 0));
    assertEquals("#FFFFFF 12.5%", ColorSettings.label("#FFFFFF", 12.5));
    Setting minimap = number("maps.opacity", 1, 0, 1, 1);
    assertFalse(ColorSettings.hiddenOpacity(minimap, List.of(minimap, number("maps.pathColor", 0, 0, 0xffffff, 0))));
    assertNull(ColorSettings.opacity(color("interface.debugTextColor"), List.of(minimap)));
    assertNull(ColorSettings.opacity(color("lightOverlay.lowColor"), List.of(minimap)));
  }

  @Test void invalidOpacityNeverPublishesTheColorHalfOfAnEdit() {
    Setting color = color("activeTweaks.textColor"), opacity = number("activeTweaks.opacity", 80, 0, 100, 100);
    assertThrows(IllegalArgumentException.class, () -> ColorSettings.apply(color, opacity, 0x123456, 101));
    assertEquals("#FFFFFF", color.value.getAsString()); assertEquals(80, opacity.value.getAsInt());
    assertThrows(IllegalArgumentException.class, () -> ColorSettings.apply(color, opacity, 0x123456, Double.NaN));
    assertEquals("#FFFFFF", color.value.getAsString());
  }

  @Test void cancelUndoAndResetKeepBothValuesAndParentDraftIsolated() {
    Setting color = color("infoHud.clusters.2.textColor"), opacity = number("infoHud.clusters.2.opacity", 80, 0, 100, 100);
    var parent = List.of(color, opacity);
    var cancelled = new SettingsDraft(parent, s -> true);
    ColorSettings.apply(cancelled.get(color.id), cancelled.get(opacity.id), 0x123456, 25);
    assertEquals("#FFFFFF", color.value.getAsString()); assertEquals(80, opacity.value.getAsInt());
    var draft = new SettingsDraft(parent, s -> true);
    var before = draft.snapshot(); draft.checkpoint();
    ColorSettings.apply(draft.get(color.id), draft.get(opacity.id), 0, 0);
    assertTrue(draft.canUndo()); draft.undo(); assertEquals(before, draft.snapshot());
    assertFalse(draft.canUndo());
    assertFalse(ColorSettings.atDefault(opacity, draft.settings()));
    draft.checkpoint(); ColorSettings.reset(draft.get(color.id), draft.settings());
    assertTrue(ColorSettings.atDefault(draft.get(color.id), draft.settings()));
    draft.undo(); assertEquals(before, draft.snapshot());
  }

  @Test void legacyPaletteDefaultResetAndCustomBlackKeepIntegerStorage() {
    for (String id : List.of("worldedit.color", "tweaks.flexible.overlayColor")) {
      Setting color = number(id, 3, 0, 0x1ffffff, 0);
      String opacityId = id.equals("worldedit.color") ? "worldedit.opacity" : "tweaks.flexible.overlayOpacity";
      Setting opacity = number(opacityId, 80, 10, id.equals("worldedit.color") ? 100 : 90, 60);
      ColorSettings.apply(color, opacity, 0, 30);
      assertEquals(ColorValue.CUSTOM, color.value.getAsInt()); assertEquals(0, ColorSettings.rgb(color));
      ColorSettings.reset(color, List.of(color, opacity));
      assertEquals(color.defaultValue, color.value); assertEquals(opacity.defaultValue, opacity.value);
      assertEquals(0, color.value.getAsInt());
    }
  }

  @Test void pathOpacityRemainsFractionalAndRgbRetainsItsNativeInteger() {
    Setting color = number("maps.pathColor", 0, 0, 0xffffff, 0);
    Setting opacity = value("maps.pathOpacity", new JsonPrimitive(.7), 0, 1, new JsonPrimitive(.7));
    ColorSettings.apply(color, opacity, 3, 12.5);
    assertEquals(3, color.value.getAsInt()); assertEquals(.125, opacity.value.getAsDouble());
    assertEquals("#000003 12.5%", ColorSettings.label(color, List.of(color, opacity)));
  }

  @Test void saveRollbackAndRestartKeepColorAndOpacityTogether(@TempDir Path game) throws Exception {
    Gson gson = new Gson(); Path file = game.resolve("colors.json");
    JsonObject initial = new JsonObject(); initial.addProperty("color", "#FFFFFF"); initial.addProperty("opacity", 80);
    Files.writeString(file, gson.toJson(initial));
    Setting color = color("activeTweaks.textColor"), opacity = number("activeTweaks.opacity", 80, 0, 100, 100);
    boolean[] fail = {true}; List<Set<String>> batches = new ArrayList<>();
    ConfigSession session = new ConfigSession();
    session.add(new Backend() {
      public String id() { return "test"; }
      public List<Path> files() { return List.of(file); }
      public void validate(Map<String, JsonElement> values) {}
      public void apply(Map<String, JsonElement> values) throws Exception {
        batches.add(Set.copyOf(values.keySet()));
        JsonObject saved = gson.fromJson(Files.readString(file), JsonObject.class);
        if (values.containsKey(color.id)) saved.add("color", values.get(color.id));
        if (values.containsKey(opacity.id)) saved.add("opacity", values.get(opacity.id));
        Files.writeString(file, gson.toJson(saved));
        if (fail[0] && saved.get("opacity").getAsInt() == 25) throw new java.io.IOException("save interrupted");
      }
    }, List.of(color, opacity));
    ColorSettings.apply(color, opacity, 1, 25);
    assertThrows(java.io.IOException.class, () -> session.save(game));
    assertEquals(initial, gson.fromJson(Files.readString(file), JsonObject.class));
    fail[0] = false; session.save(game);
    JsonObject restarted = gson.fromJson(Files.readString(file), JsonObject.class);
    assertEquals("#000001", restarted.get("color").getAsString());
    assertEquals(25, restarted.get("opacity").getAsInt());
    assertTrue(batches.stream().allMatch(batch -> batch.equals(Set.of(color.id, opacity.id))));
    assertEquals(0, session.changes());
  }
}
