package local.luke.power.rules;

import com.google.gson.*;
import java.util.*;

/** Owned mutable values. Copies never inherit later changes from their source. */
public final class RuleSet {
  public static final int VERSION = 4;
  private final EnumMap<GameRule, JsonPrimitive> values = new EnumMap<>(GameRule.class);

  public RuleSet() {
    for (GameRule rule : GameRule.values()) values.put(rule, rule.defaultValue());
  }
  public static RuleSet cheats() {
    RuleSet rules = new RuleSet();
    for (GameRule rule : GameRule.values()) if (rule.access() || rule == GameRule.KEEP_INVENTORY) rules.set(rule, true);
    return rules;
  }
  public RuleSet copy() { RuleSet copy = new RuleSet(); copy.values.putAll(values); return copy; }
  public JsonPrimitive get(GameRule rule) { return values.get(Objects.requireNonNull(rule)); }
  public boolean enabled(GameRule rule) {
    if (rule.integer()) throw new IllegalArgumentException("Not a boolean rule: " + rule.id());
    return get(rule).getAsBoolean();
  }
  public int number(GameRule rule) {
    if (!rule.integer()) throw new IllegalArgumentException("Not an integer rule: " + rule.id());
    return get(rule).getAsInt();
  }
  public void set(GameRule rule, JsonElement value) { values.put(rule, rule.checked(value)); }
  public void set(GameRule rule, boolean value) { set(rule, new JsonPrimitive(value)); }
  public void set(GameRule rule, int value) { set(rule, new JsonPrimitive(value)); }
  public JsonObject values() {
    JsonObject object = new JsonObject();
    for (GameRule rule : GameRule.values()) object.add(rule.id(), get(rule));
    return object;
  }
  public JsonObject document() {
    JsonObject object = new JsonObject();
    object.addProperty("version", VERSION);
    object.add("values", values());
    return object;
  }
  public static RuleSet read(JsonObject document, RuleSet defaults) {
    int version = version(document);
    if (!document.has("values") || !document.get("values").isJsonObject())
      throw new IllegalArgumentException("Game rules values are missing or invalid.");
    return readValues(upgradeValues(document.getAsJsonObject("values"), version), defaults);
  }
  public static int version(JsonObject document) {
    try {
      JsonPrimitive value = document.getAsJsonPrimitive("version");
      if (!value.isNumber()) throw new IllegalArgumentException();
      int version = value.getAsBigDecimal().intValueExact();
      if (version < 1 || version > VERSION) throw new IllegalArgumentException();
      return version;
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Unsupported game rules version. Supported versions: 1 through " + VERSION + ". Original data is preserved.", e);
    }
  }
  public static JsonObject upgradeValues(JsonObject source, int version) {
    JsonObject values = source.deepCopy();
    if (version < 3) {
      JsonElement old = values.remove("sleep_ambushes");
      if (old != null) {
        if (values.has("sleep_nightmares") && !old.equals(values.get("sleep_nightmares")))
          throw new IllegalArgumentException("Conflicting sleep nightmare rules. Original data is preserved.");
        values.add("sleep_nightmares", old);
      }
      JsonElement percentage = values.get("players_sleeping_percentage");
      if (percentage != null && percentage.isJsonPrimitive() && percentage.getAsJsonPrimitive().isNumber()
          && percentage.getAsBigDecimal().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
        // Old values above 100 disabled skipping. Keep that restriction explicitly.
        try { percentage.getAsBigDecimal().intValueExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Invalid old sleeping percentage", e); }
        values.addProperty("allow_sleep", false);
        values.addProperty("players_sleeping_percentage", 100);
      }
    }
    return values;
  }
  public static RuleSet readValues(JsonObject object, RuleSet defaults) {
    RuleSet rules = defaults.copy();
    for (var entry : object.entrySet()) {
      GameRule rule = GameRule.find(entry.getKey());
      if (rule == null || !rule.id().equals(entry.getKey()))
        throw new IllegalArgumentException("Unknown saved game rule: " + entry.getKey() + ". Use a compatible release; original data is preserved.");
      rules.set(rule, entry.getValue());
    }
    return rules;
  }
  @Override public boolean equals(Object other) { return other instanceof RuleSet rules && values.equals(rules.values); }
  @Override public int hashCode() { return values.hashCode(); }
}
