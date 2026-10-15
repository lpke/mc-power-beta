package local.luke.power.storage;

import com.google.gson.*;
import java.util.List;

/** Schema 3 switches replaced by rules, direct bindings or one shared behavior. */
final class ConfigCleanup {
  private ConfigCleanup() {}
  private static final List<String> RETIRED = List.of(
      "creative.modePicker",
      "gameplay.explosionProofItems",
      "gameplay.fireSpread",
      "power_camera:config.enabled",
      "power_client_fixes:config.enableDebugGraphChanges",
      "power_client_fixes:config.enableDebugGraphModernToggle",
      "power_compat:config.slabPlacementFixesEnabled",
      "power_controls:features.fallingPipe",
      "power_controls:recipes.enableRecipes",
      "power_controls:tweaks.disableSleepSpawning",
      "power_environment:config.SLEEP_CONFIG.asleepPlayerPercentageForSkippingNight",
      "power_inventory:config.CRAFTING_RESULT_CONFIG.StopShiftClickCraftingWhenItemChanges",
      "power_inventory:config.MODERN_MINECRAFT_CONFIG.EnableDragGraphics",
      "power_mechanics:config.EXPLOSION_AND_FIRE_CONFIG.disableAllExplosionsBreakingBlocks",
      "power_mechanics:config.EXPLOSION_AND_FIRE_CONFIG.disableCreeperExplosionBreakingBlocks",
      "power_mechanics:config.EXPLOSION_AND_FIRE_CONFIG.disableGhastExplosionBreakingBlocks",
      "power_mechanics:config.EXPLOSION_AND_FIRE_CONFIG.disableTntExplosionBreakingBlocks",
      "power_mechanics:config.MOB_CONFIG.disableTramplingFarmland",
      "building.autoMine",
      "building.autoWalk",
      "building.freeLook",
      "building.hotbar.scroll",
      "building.hotbar.swap",
      "visual.containerPreview",
      "visual.slashChat",
      "power_environment:config.ENTITY_SPAWN_MECHANICS_CONFIG.SPAWN_LIST_CONFIG");

  static void upgradeThree(JsonObject settings) {
    unbindDisabled(settings, "building.autoWalk", true, "Auto-walk (toggle)");
    unbindDisabled(settings, "building.autoMine", true, "Auto-mine (toggle)");
    unbindDisabled(settings, "building.freeLook", true, "key.powerbeta.free_look");
    unbindDisabled(settings, "building.hotbar.swap", true, "power_building.hotbar.base",
        "power_building.hotbar.row1", "power_building.hotbar.row2", "power_building.hotbar.row3");
    unbindDisabled(settings, "building.hotbar.scroll", true, "power_building.hotbar.scroll");
    unbindDisabled(settings, "visual.containerPreview", false, "powerbeta.containerPreview");
    unbindDisabled(settings, "creative.modePicker", true, "power_creative.picker", "power_creative.modifier");
    unbindDisabled(settings, "power_camera:config.enabled", false, "Toggle Freecam", "Toggle Player Movement",
        "Change Freecam Speed", "Save Or Load Camera Position", "Open Camera Position Gui");

    JsonObject nativeKeys = section(settings, "native"), modifiers = section(settings, "bindings");
    nativeKeys.addProperty("key_powerbeta.openCommands", enabled(settings, "visual.slashChat", false) ? "53" : "0");
    nativeKeys.remove("key_Command");
    modifiers.remove("Command");
    boolean graph = enabled(settings, "power_client_fixes:config.enableDebugGraphChanges", true);
    boolean modern = enabled(settings, "power_client_fixes:config.enableDebugGraphModernToggle", true);
    JsonElement oldGraph = nativeKeys.get("key_Debug Graph");
    nativeKeys.add("key_powerbeta.debugGraph", graph
        ? modern ? new JsonPrimitive("61") : oldGraph == null ? new JsonPrimitive("29") : oldGraph.deepCopy()
        : new JsonPrimitive("0"));
    if (graph && modern) modifiers.addProperty("powerbeta.debugGraph", 1);
    else if (graph && modifiers.has("Debug Graph")) modifiers.add("powerbeta.debugGraph", modifiers.get("Debug Graph"));
    nativeKeys.remove("key_Debug Graph");
    modifiers.remove("Debug Graph");

    // These startup recipes were dormant behind the old master. Preserve their effective values.
    if (!enabled(settings, "power_controls:recipes.enableRecipes", true)) {
      JsonObject recipes = section(settings, "power_controls:recipes");
      for (String group : List.of("modern", "obtainable")) {
        JsonObject fields = section(recipes, group);
        for (var field : fields.entrySet()) {
          if (!field.getValue().isJsonPrimitive() || !field.getValue().getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException("Invalid recipe setting: " + field.getKey());
          field.setValue(new JsonPrimitive(false));
        }
      }
      for (String key : List.of("shapelessFlintAndSteel", "shapelessMushroomStew", "shapelessChestMinecart",
          "shapelessFurnaceMinecart", "shapelessStickyPistons", "booksRequireLeather", "woolRedyeing",
          "sixSlabsPerCraft", "oneStonePerButton", "modernFenceRecipe", "snowLayerRecipe", "threeLaddersPerCraft"))
        section(recipes, "modern").addProperty(key, false);
      for (String key : List.of("craftableGrassBlocks", "craftableCobwebs", "craftableFire", "craftableCoalOre",
          "craftableIronOre", "craftableGoldOre", "craftableLapisOre", "craftableDiamondOre"))
        section(recipes, "obtainable").addProperty(key, false);
      recipes.addProperty("furnaceFuels", false);
      JsonObject tweaked = section(recipes, "tweaked");
      tweaked.addProperty("shapelessJackOLantern", false);
      tweaked.addProperty("stairsPerCraft", 4);
      tweaked.addProperty("trapdoorsPerCraft", 2);
    }
    for (String id : RETIRED) remove(settings, id);
  }

  static void upgradeFour(JsonObject settings) {
    JsonObject features = section(settings, "power_controls:features");
    if (!enabled(settings, "power_controls:features.chestSounds", false))
      features.addProperty("moreSoundsChests", 0);
    else if (!features.has("moreSoundsChests")) features.addProperty("moreSoundsChests", 2);
    // An inactive placement master must not unexpectedly activate old hold bindings.
    unbindDisabled(settings, "building.flexible.enabled", false, "Placement offset (hold)",
        "Placement adjacent (hold)", "Placement rotation (hold)", "Placement reverse (hold)", "Placement into face (hold)");
    if (!enabled(settings, "building.flexible.enabled", false))
      section(section(settings, "building"), "flexible").addProperty("placeAgainstContainers", false);
    section(settings, "native").remove("key_Flexible placement (toggle)");
    section(settings, "bindings").remove("Flexible placement (toggle)");
    for (String id : List.of("power_controls:features.betterBurning", "power_controls:features.burpingSounds",
        "power_controls:features.chestSounds", "power_controls:oldfeatures.punchTntToIgnite",
        "power_mechanics:config.MOB_CONFIG.disableTrampleFarmlandWhenBarefoot",
        "power_mechanics:config.MOB_CONFIG.disableTrampleFarmlandWithLeatherBoots",
        "gameplay.lavaProofItems", "gameplay.cactusProofItems", "visual.spectatorBlockOutlines", "building.flexible.enabled"))
      remove(settings, id);
  }

  private static void unbindDisabled(JsonObject settings, String id, boolean fallback, String... names) {
    if (enabled(settings, id, fallback)) return;
    JsonObject keys = section(settings, "native"), modifiers = section(settings, "bindings");
    for (String name : names) { keys.addProperty("key_" + name, "0"); modifiers.remove(name); }
  }
  private static boolean enabled(JsonObject settings, String path, boolean fallback) {
    JsonElement value = settings;
    for (String key : path.split("\\.")) {
      if (!value.isJsonObject()) throw new IllegalArgumentException("Invalid configuration section: " + path);
      value = value.getAsJsonObject().get(key);
      if (value == null) return fallback;
    }
    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
      throw new IllegalArgumentException("Invalid configuration boolean: " + path);
    return value.getAsBoolean();
  }
  private static void remove(JsonObject settings, String path) {
    String[] keys = path.split("\\.");
    JsonObject parent = settings;
    for (int i = 0; i < keys.length - 1; i++) {
      if (!parent.has(keys[i])) return;
      parent = parent.getAsJsonObject(keys[i]);
    }
    parent.remove(keys[keys.length - 1]);
  }
  private static JsonObject section(JsonObject parent, String key) {
    if (!parent.has(key)) parent.add(key, new JsonObject());
    return parent.getAsJsonObject(key);
  }
}
