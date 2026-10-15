package local.luke.power.rules;

import com.google.gson.*;
import java.io.IOException;
import local.luke.power.storage.PowerConfig;

/** Global creation templates. Readers always receive independent drafts. */
public final class RulePresets {
  private static RuleSet survival, cheats;
  private RulePresets() {}
  private static void load() {
    if (survival != null) return;
    JsonObject value = PowerConfig.section("gameRules");
    RuleSet s = read(value, "survival", new RuleSet());
    RuleSet c = read(value, "cheats", RuleSet.cheats());
    survival = s; cheats = c;
  }
  private static RuleSet read(JsonObject object, String id, RuleSet defaults) {
    return object.has(id) ? RuleSet.read(object.getAsJsonObject(id), defaults) : defaults;
  }
  public static synchronized RuleSet survival() { load(); return survival.copy(); }
  public static synchronized RuleSet cheats() { load(); return cheats.copy(); }
  public static synchronized void save(RuleSet s, RuleSet c) throws IOException {
    JsonObject object = new JsonObject();
    object.add("survival", s.document()); object.add("cheats", c.document());
    PowerConfig.put("gameRules", object);
    survival = s.copy(); cheats = c.copy();
  }
}
