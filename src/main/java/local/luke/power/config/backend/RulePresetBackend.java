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
    List<Setting> entries = new ArrayList<>(RuleSettings.entries(backend.id(), "rules.survival.", RulePresets.survival(), new RuleSet()));
    entries.addAll(RuleSettings.entries(backend.id(), "rules.cheats.", RulePresets.cheats(), RuleSet.cheats()));
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
