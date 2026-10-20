package local.luke.power.config.backend;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;

/** Only registered outside a world. Template edits never touch existing worlds. */
public final class RulePresetBackend implements Backend {
  public static void register(ConfigSession session) {
    RulePresetBackend backend = new RulePresetBackend();
    var review = RuleDefaults.review();
    if (review.required()) {
      session.add(backend, List.of(new Setting("rules.review", backend.id(), "Game rules", "Creation defaults", "Review creation defaults",
          "Review updated Survival and Cheats defaults before editing future-world rules.", Setting.Kind.TEXT,
          new JsonPrimitive(""), new JsonPrimitive(""), 0, 0, 1, List.of(), false)));
      return;
    }
    List<Setting> entries = new ArrayList<>(RuleSettings.entries(backend.id(), "rules.survival.", RulePresets.survival(), RuleDefaults.latest(DefaultBases.Base.POWER_BETA, false)));
    entries.addAll(RuleSettings.entries(backend.id(), "rules.cheats.", RulePresets.cheats(), RuleDefaults.latest(DefaultBases.Base.POWER_BETA, true)));
    session.add(backend, entries);
  }
  public String id() { return "rulePresets"; }
  public List<Path> files() { return List.of(PowerConfig.path()); }
  public void validate(Map<String, JsonElement> values) {
    RuleSettings.edited(RulePresets.survival(), "rules.survival.", values);
    RuleSettings.edited(RulePresets.cheats(), "rules.cheats.", values);
  }
  public void apply(Map<String, JsonElement> values) throws Exception {
    RulePresets.save(RuleSettings.edited(RulePresets.survival(), "rules.survival.", values),
        RuleSettings.edited(RulePresets.cheats(), "rules.cheats.", values));
  }
}
