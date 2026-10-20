package local.luke.power.rules;

import com.google.gson.*;

/** Strict supported conversion for creation templates, separated from global configuration recovery. */
public final class TemplateDocuments {
  private TemplateDocuments() {}
  public static RuleSet read(JsonElement document, boolean cheats, JsonObject evidence) {
    if (document == null || !document.isJsonObject()) throw new IllegalArgumentException("Missing or invalid creation template.");
    JsonObject raw = document.getAsJsonObject();
    int version = RuleSet.version(raw);
    RuleSet defaults = cheats ? RuleSet.cheats() : new RuleSet();
    if (version < 3) defaults = WorldRuleUpgrade210.defaults(evidence, defaults);
    RuleSet.read(raw, defaults);
    return RuleSet.readValues(WorldRuleUpgrade210.values(raw.getAsJsonObject("values"), version, evidence), defaults);
  }
}
