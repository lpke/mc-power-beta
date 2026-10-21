package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import local.luke.power.heldlight.HeldLightSettings;
import local.luke.power.inventory.tools.InventoryToolsSettings;
import local.luke.power.offhand.OffhandSettings;
import local.luke.power.recipes.RecipeBookSettings;
import local.luke.power.storage.ConfigVersions;
import local.luke.power.subtitles.SubtitleSettings;
import local.luke.power.world.WorldToolsSettings;
import org.junit.jupiter.api.Test;

class ModernFeatureIntegrationTest {
  static final List<String> FLAGS = List.of("offhand.enabled", "recipeBook.enabled", "heldLight.enabled", "subtitles.enabled",
      "inventoryTools.sorting", "inventoryTools.depositMatching", "inventoryTools.creativeHotbars", "inventoryTools.restock",
      "inventoryTools.toolSwitch", "worldTools.backup", "worldTools.duplicate", "worldTools.recreate");
  private static JsonObject resource(String path) throws IOException {
    try (var stream = ModernFeatureIntegrationTest.class.getResourceAsStream(path)) {
      assertNotNull(stream, path); return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }
  }
  @Test void everyFreshFeatureFieldMatchesItsPowerBetaResetValueAndStableFallback() throws Exception {
    JsonObject fresh = resource("/assets/powerbeta/defaults/power-beta.json").getAsJsonObject("settings");
    Map<String, Object> groups = Map.of("offhand", new OffhandSettings(), "recipeBook", new RecipeBookSettings(),
        "heldLight", new HeldLightSettings(), "inventoryTools", new InventoryToolsSettings(),
        "worldTools", new WorldToolsSettings(), "subtitles", new SubtitleSettings());
    for (var entry : groups.entrySet()) {
      Set<String> fields = new HashSet<>();
      for (var field : entry.getValue().getClass().getFields()) {
        fields.add(field.getName()); String id = entry.getKey() + "." + field.getName();
        JsonElement fallback = Catalog.JSON.toJsonTree(field.get(entry.getValue()));
        assertEquals(DefaultBases.value(DefaultBases.Base.POWER_BETA, id, fallback), fresh.getAsJsonObject(entry.getKey()).get(field.getName()), id);
      }
      assertEquals(fields, fresh.getAsJsonObject(entry.getKey()).keySet(), entry.getKey());
    }
  }
  @Test void allFeatureSwitchesFollowExplicitBasePolicyAndExceptionsStayOff() throws Exception {
    JsonObject bases = resource("/assets/powerbeta/default-bases.json");
    for (var base : DefaultBases.Base.values()) for (String id : FLAGS) {
      assertTrue(bases.getAsJsonObject(base.id).has(id), base + " must freeze " + id);
      boolean expected = base == DefaultBases.Base.POWER_BETA && !Set.of("subtitles.enabled", "inventoryTools.restock", "inventoryTools.toolSwitch").contains(id);
      assertEquals(expected, DefaultBases.value(base, id, new JsonPrimitive(!expected)).getAsBoolean(), base + " " + id);
    }
  }
  @Test void inventoryControlsHaveUnboundCatalogDefaultsAndExactRelatedSettings() {
    Map<String, String> pairs = Map.of("swapHands", "offhand.enabled", "restock", "inventoryTools.restock",
        "recipeBook", "recipeBook.enabled", "sortInventory", "inventoryTools.sortingButton", "depositMatching", "inventoryTools.depositMatchingButton",
        "sortingLayout", "inventoryTools.preferredItems",
        "saveCreativeHotbar", "inventoryTools.creativeHotbars", "loadCreativeHotbar", "inventoryTools.creativeHotbars");
    var session = new ConfigSession(); var entries = new ArrayList<Setting>();
    entries.add(row("rules.world.allow_creative_mode", false));
    for (String id : new LinkedHashSet<>(pairs.values())) entries.add(row(id, false));
    for (String key : pairs.keySet()) entries.add(row("keys.powerbeta." + key, true));
    session.add(new Backend() {
      public String id() { return "fixture"; }
      public List<java.nio.file.Path> files() { return List.of(); }
      public void validate(Map<String, JsonElement> values) {}
      public void apply(Map<String, JsonElement> values) {}
    }, entries);
    pairs.forEach((key, target) -> {
      String id = "keys.powerbeta." + key; var metadata = Catalog.metadata(id);
      assertEquals(0, metadata.get("default").getAsInt(), id);
      Setting control = entries.stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow();
      assertEquals("Controls", control.page); assertEquals("Inventory", control.group);
      assertEquals(target, ControlLinks.related(session, control).id, id);
      assertFalse(ControlLinks.description(id).equals("Activate this action using the assigned key or mouse button."), id);
    });
  }
  @Test void compatibleSchemaConversionPreservesOpaquePresetsAndNativeDisplayPreferences() throws Exception {
    JsonObject document = resource("/assets/powerbeta/defaults/power-beta.json");
    JsonObject settings = document.getAsJsonObject("settings"), presets = new JsonObject();
    presets.addProperty("format", 999); presets.addProperty("opaque", "future item and preset data"); settings.add("savedCreativeHotbars", presets);
    settings.getAsJsonObject("native").addProperty("fov", "0.431");
    settings.getAsJsonObject("native").addProperty("guiScale", "3");
    settings.getAsJsonObject("native").addProperty("fpsLimit", "2");
    settings.getAsJsonObject("native").addProperty("gui_scale", "0.375");
    settings.getAsJsonObject("native").addProperty("framerate_limit", 145);
    JsonObject nativeSettings = settings.getAsJsonObject("native").deepCopy();
    for (int version : List.of(3, 4, 5, 6, 7)) {
      JsonObject old = document.deepCopy(); old.addProperty("schemaVersion", version);
      JsonObject converted = ConfigVersions.convert(old).getAsJsonObject("settings");
      assertEquals(presets, converted.get("savedCreativeHotbars"), "preset data at schema " + version);
      for (String id : List.of("fov", "guiScale", "gui_scale", "fpsLimit", "framerate_limit")) assertEquals(nativeSettings.get(id), converted.getAsJsonObject("native").get(id), id);
    }
    assertFalse(resource("/assets/powerbeta/defaults/power-beta.json").getAsJsonObject("settings").has("savedCreativeHotbars"));
  }
  private static Setting row(String id, boolean key) {
    return new Setting(id, "fixture", "Inventory", "Hands", id, "", key ? Setting.Kind.KEY : Setting.Kind.BOOLEAN,
        key ? new JsonPrimitive(0) : new JsonPrimitive(true), key ? new JsonPrimitive(0) : new JsonPrimitive(false), 0, 1, 1, List.of(), false);
  }
}
