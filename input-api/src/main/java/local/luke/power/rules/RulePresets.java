package local.luke.power.rules;

import com.google.gson.*;
import java.io.IOException;
import local.luke.power.storage.PowerConfig;

/** Global creation templates. Always read independent drafts, including after rollback or maintenance. */
public final class RulePresets {
  private RulePresets() {}
  private static RuleSet read(String id, RuleSet defaults) {
    JsonObject root = PowerConfig.document();
    JsonElement section = root.getAsJsonObject("settings").get("gameRules");
    if (section == null) return defaults;
    if (!section.isJsonObject()) throw new IllegalArgumentException("Review creation defaults before creating a world.");
    JsonObject templates = section.getAsJsonObject();
    if (!templates.has(id)) return defaults;
    JsonObject evidence = root.has("worldRuleUpgrade210") ? root.getAsJsonObject("worldRuleUpgrade210") : new JsonObject();
    return TemplateDocuments.read(templates.get(id), id.equals("cheats"), evidence);
  }
  public static RuleSet survival() { return read("survival", new RuleSet()); }
  public static RuleSet cheats() { return read("cheats", RuleSet.cheats()); }
  public static synchronized void save(RuleSet s, RuleSet c) throws IOException {
    JsonObject object = PowerConfig.section("gameRules");
    object.add("survival", s.document()); object.add("cheats", c.document());
    PowerConfig.put("gameRules", object);
  }
}
