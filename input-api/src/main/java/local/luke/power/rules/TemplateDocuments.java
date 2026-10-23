package local.luke.power.rules;

import com.google.gson.*;

/** Strict current-format creation templates, separate from global configuration recovery. */
public final class TemplateDocuments {
  private TemplateDocuments() {}
  public static RuleSet read(JsonElement document, boolean cheats) {
    if (document == null || !document.isJsonObject()) throw new IllegalArgumentException("Missing or invalid creation template.");
    return RuleSet.read(document.getAsJsonObject(), cheats ? RuleSet.cheats() : new RuleSet());
  }
}
