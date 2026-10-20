package local.luke.power.config.backend;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;

/** Internal metadata participates in the same durable transaction as a preset load. */
public final class DefaultsBackend implements Backend {
  public static void register(ConfigSession session) {
    DefaultBases.State state = DefaultBases.state();
    session.add(new DefaultsBackend(), List.of(
        setting("base", new JsonPrimitive(state.base), new JsonPrimitive("power_beta"), Setting.Kind.TEXT),
        setting("setupComplete", new JsonPrimitive(state.setupComplete), new JsonPrimitive(true), Setting.Kind.BOOLEAN)));
  }
  private static Setting setting(String id, JsonElement value, JsonElement defaults, Setting.Kind kind) {
    return new Setting("defaults." + id, "defaults", "Defaults", "", id, "", kind, value, defaults, 0, 1, 1, List.of(), false);
  }
  public String id() { return "defaults"; }
  public List<Path> files() { return List.of(PowerConfig.path()); }
  public void validate(Map<String, JsonElement> values) {
    if (values.containsKey("defaults.base")) DefaultBases.Base.read(values.get("defaults.base").getAsString());
    for (var e : values.entrySet()) if (!e.getKey().equals("defaults.base") && !e.getKey().equals("defaults.setupComplete")) throw new IllegalArgumentException("Unknown defaults preference");
  }
  public void apply(Map<String, JsonElement> values) throws Exception {
    validate(values);
    DefaultBases.State state = DefaultBases.state();
    DefaultBases.Base previous = DefaultBases.Base.read(state.base);
    JsonObject root = PowerConfig.document();
    JsonElement expected = root.getAsJsonObject("settings").get("gameRules");
    if (values.containsKey("defaults.base")) state.base = values.get("defaults.base").getAsString();
    if (values.containsKey("defaults.setupComplete")) state.setupComplete = values.get("defaults.setupComplete").getAsBoolean();
    state.validate();
    JsonObject replacement = RuleDefaults.followBase(expected,
        root.has("worldRuleUpgrade210") ? root.getAsJsonObject("worldRuleUpgrade210") : new JsonObject(),
        previous, DefaultBases.Base.read(state.base));
    PowerConfig.save("defaults", state);
    if (replacement != null) PowerConfig.replaceSection("gameRules", expected, replacement);
  }
}
