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
    version(document);
    if (!document.has("values") || !document.get("values").isJsonObject())
      throw new IllegalArgumentException("Game rules values are missing or invalid.");
    return readValues(document.getAsJsonObject("values"), defaults);
  }
  public static int version(JsonObject document) {
    try {
      JsonPrimitive value = document.getAsJsonPrimitive("version");
      if (!value.isNumber()) throw new IllegalArgumentException();
      int version = value.getAsBigDecimal().intValueExact();
      if (version != VERSION) throw new IllegalArgumentException();
      return version;
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Unsupported game rules version. Supported version: " + VERSION + ". Upgrade older worlds and templates with Power Beta 4.4.1 first, or use the release that saved this data. Original data is preserved.", e);
    }
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
