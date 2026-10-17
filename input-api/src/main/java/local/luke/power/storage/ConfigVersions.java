package local.luke.power.storage;

import com.google.gson.*;
import local.luke.power.rules.*;

/** Explicit supported conversions, independent of the release version. */
public final class ConfigVersions {
  public static final int CURRENT = 7;
  private ConfigVersions() {}

  public static int version(JsonObject root) {
    try {
      JsonPrimitive version = root.getAsJsonPrimitive("schemaVersion");
      if (!version.isNumber()) throw new IllegalArgumentException();
      return version.getAsBigDecimal().intValueExact();
    } catch (RuntimeException e) { throw new IllegalArgumentException("Missing or invalid schemaVersion. Supported configuration versions: 3, 4, 5, 6, 7.", e); }
  }
  public static JsonObject convert(JsonObject source) {
    int version = version(source);
    if (version < 3 || version > CURRENT)
      throw new IllegalArgumentException("Configuration version " + version + " is unsupported. Supported versions: 3, 4, 5, 6, 7. Use a compatible release or restore a supported backup; original file is preserved.");
    if (!source.has("settings") || !source.get("settings").isJsonObject())
      throw new IllegalArgumentException("Configuration settings must be an object.");
    JsonObject root = source.deepCopy();
    JsonObject settings = root.getAsJsonObject("settings");
    if (version < 5) {
      JsonObject frozen = new JsonObject();
      for (String id : new String[]{"gameplay", "power_mechanics:config"})
        if (settings.has(id)) frozen.add(id, settings.get(id).deepCopy());
      root.add("worldRuleUpgrade", frozen);
    }
    boolean createPresets = version == 3 && !settings.has("gameRules");
    if (version == 3) {
      // Frozen conversion evidence, never a live setting or an inheritance layer.
      // Unconverted worlds can be loaded much later, after global preferences change.
      JsonObject legacy = new JsonObject();
      for (String id : new String[]{"commandAccess", "editor", "gameplay", "power_controls:tweaks", "power_environment:config", "power_mechanics:config"})
        if (settings.has(id)) legacy.add(id, settings.get(id).deepCopy());
      root.add("legacyWorldRules", legacy);
      ConfigCleanup.upgradeThree(settings);
      settings.remove("commandAccess");
      remove(settings, "editor", "enabled");
      remove(settings, "gameplay", "fireSpread", "explosionProofItems");
      root.addProperty("schemaVersion", CURRENT);
    }
    if (version < 5) {
      ConfigCleanup.upgradeFour(settings);
      root.addProperty("schemaVersion", CURRENT);
    }
    if (version < 6) {
      JsonObject frozen = new JsonObject();
      for (String id : new String[]{"power_controls:tweaks", "power_mechanics:config", "power_environment:config"})
        if (settings.has(id)) frozen.add(id, settings.get(id).deepCopy());
      WorldRuleUpgrade210.defaults(frozen, new RuleSet()); // Validate before retiring preferences.
      root.add("worldRuleUpgrade210", frozen);
      ConfigCleanup.upgradeFive(settings);
      root.addProperty("schemaVersion", CURRENT);
    }
    if (version < 7) {
      ConfigCleanup.upgradeSix(settings);
      root.addProperty("schemaVersion", CURRENT);
    }
    JsonObject evidence210 = root.has("worldRuleUpgrade210") ? root.getAsJsonObject("worldRuleUpgrade210") : new JsonObject();
    if (createPresets) {
      JsonObject presets = new JsonObject();
      presets.add("survival", WorldRuleUpgrade210.defaults(evidence210, new RuleSet()).document());
      presets.add("cheats", WorldRuleUpgrade210.defaults(evidence210, RuleSet.cheats()).document());
      settings.add("gameRules", presets);
    } else if (settings.has("gameRules")) {
      JsonObject presets = settings.getAsJsonObject("gameRules");
      for (String name : new String[]{"survival", "cheats"}) if (presets.has(name)) {
        JsonObject document = presets.getAsJsonObject(name);
        int ruleVersion = RuleSet.version(document);
        RuleSet defaults = name.equals("cheats") ? RuleSet.cheats() : new RuleSet();
        if (ruleVersion < 3) defaults = WorldRuleUpgrade210.defaults(evidence210, defaults);
        RuleSet.read(document, defaults); // Validate structure and unknown fields before conversion.
        JsonObject values = WorldRuleUpgrade210.values(document.getAsJsonObject("values"), ruleVersion, evidence210);
        presets.add(name, RuleSet.readValues(values, defaults).document());
      }
    }
    validateDebugLayout(settings);
    return root;
  }
  private static void validateDebugLayout(JsonObject settings) {
    JsonObject ui = settings.has("interface") ? settings.getAsJsonObject("interface") : new JsonObject();
    if (ui.has("debugOrder")) local.luke.power.ui.DebugOrder.read(ui.get("debugOrder"));
    else {
      var old = local.luke.power.ui.DebugOrder.legacy(settings);
      if (!old.equals(local.luke.power.ui.DebugOrder.defaults())) {
        JsonArray rows = new JsonArray(); old.forEach(rows::add); ui.add("debugOrder", rows); settings.add("interface", ui);
      }
    }
    var left = ui.has("debugOrder") ? local.luke.power.ui.DebugOrder.read(ui.get("debugOrder")) : local.luke.power.ui.DebugOrder.defaults();
    var right = ui.has("debugRightOrder") ? local.luke.power.ui.DebugOrder.read(ui.get("debugRightOrder")) : local.luke.power.ui.DebugColumns.defaultRight();
    new local.luke.power.ui.DebugColumns(left, right);
    if (ui.has("debugTextSize")) {
      var n = ui.get("debugTextSize");
      if (!n.isJsonPrimitive() || !n.getAsJsonPrimitive().isNumber() || !Double.isFinite(n.getAsDouble()) || n.getAsDouble() < .5 || n.getAsDouble() > 3)
        throw new IllegalArgumentException("F3 text size must be between 0.5 and 3. Original settings preserved.");
    }
    if (ui.has("debugLines")) {
      if (!ui.get("debugLines").isJsonObject()) throw new IllegalArgumentException("F3 visibility must be an object.");
      for (var e : ui.getAsJsonObject("debugLines").entrySet())
        if (!e.getKey().matches("[A-Za-z0-9_.-]{1,96}") || !e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isBoolean())
          throw new IllegalArgumentException("Invalid F3 visibility setting. Original settings preserved.");
    }
  }
  private static void remove(JsonObject settings, String section, String... keys) {
    if (!settings.has(section)) return;
    JsonObject object = settings.getAsJsonObject(section);
    for (String key : keys) object.remove(key);
  }
}
