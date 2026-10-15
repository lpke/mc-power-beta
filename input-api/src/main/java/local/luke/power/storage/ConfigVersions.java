package local.luke.power.storage;

import com.google.gson.*;
import local.luke.power.rules.*;

/** Explicit supported conversions, independent of the release version. */
public final class ConfigVersions {
  public static final int CURRENT = 5;
  private ConfigVersions() {}

  public static int version(JsonObject root) {
    try {
      JsonPrimitive version = root.getAsJsonPrimitive("schemaVersion");
      if (!version.isNumber()) throw new IllegalArgumentException();
      return version.getAsBigDecimal().intValueExact();
    } catch (RuntimeException e) { throw new IllegalArgumentException("Missing or invalid schemaVersion. Supported configuration versions: 3, 4, 5.", e); }
  }
  public static JsonObject convert(JsonObject source) {
    int version = version(source);
    if (version != 3 && version != 4 && version != CURRENT)
      throw new IllegalArgumentException("Configuration version " + version + " is unsupported. Supported versions: 3, 4, 5. Use a compatible release or restore a supported backup; original file is preserved.");
    if (!source.has("settings") || !source.get("settings").isJsonObject())
      throw new IllegalArgumentException("Configuration settings must be an object.");
    JsonObject root = source.deepCopy();
    JsonObject settings = root.getAsJsonObject("settings");
    if (version < CURRENT) {
      JsonObject frozen = new JsonObject();
      for (String id : new String[]{"gameplay", "power_mechanics:config"})
        if (settings.has(id)) frozen.add(id, settings.get(id).deepCopy());
      root.add("worldRuleUpgrade", frozen);
    }
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
      JsonObject presets = new JsonObject();
      presets.add("survival", new RuleSet().document());
      presets.add("cheats", RuleSet.cheats().document());
      if (!settings.has("gameRules")) settings.add("gameRules", presets);
      root.addProperty("schemaVersion", CURRENT);
    }
    if (version < CURRENT) {
      ConfigCleanup.upgradeFour(settings);
      root.addProperty("schemaVersion", CURRENT);
    }
    if (settings.has("gameRules")) {
      JsonObject presets = settings.getAsJsonObject("gameRules");
      if (presets.has("survival")) presets.add("survival", RuleSet.read(presets.getAsJsonObject("survival"), new RuleSet()).document());
      if (presets.has("cheats")) presets.add("cheats", RuleSet.read(presets.getAsJsonObject("cheats"), RuleSet.cheats()).document());
    }
    return root;
  }
  private static void remove(JsonObject settings, String section, String... keys) {
    if (!settings.has(section)) return;
    JsonObject object = settings.getAsJsonObject(section);
    for (String key : keys) object.remove(key);
  }
}
