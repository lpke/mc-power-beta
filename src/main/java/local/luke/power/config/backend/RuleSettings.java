package local.luke.power.config.backend;

import com.google.gson.*;
import java.util.*;
import local.luke.power.config.Setting;
import local.luke.power.rules.*;

public final class RuleSettings {
  private RuleSettings() {}
  public static List<Setting> entries(String backend, String prefix, RuleSet current, RuleSet defaults) {
    List<Setting> entries = new ArrayList<>();
    for (GameRule rule : GameRule.values()) entries.add(new Setting(prefix + rule.id(), backend, "Game rules", rule.group(), rule.label(), rule.help(),
        rule.integer() ? Setting.Kind.INTEGER : Setting.Kind.BOOLEAN, current.get(rule), defaults.get(rule),
        0, rule.maximum(), 1, List.of(), false));
    return entries;
  }
  public static RuleSet edited(RuleSet source, String prefix, Map<String, JsonElement> edits) {
    RuleSet next = source.copy();
    for (var entry : edits.entrySet()) {
      if (!entry.getKey().startsWith(prefix)) continue;
      GameRule rule = GameRule.find(entry.getKey().substring(prefix.length()));
      if (rule == null) throw new IllegalArgumentException("Unknown game rule: " + entry.getKey());
      next.set(rule, entry.getValue());
    }
    return next;
  }
}
