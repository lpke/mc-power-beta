package local.luke.power.config;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;

/** One defaults target and one acknowledged revision for every future-world entry point. */
public final class RuleDefaults {
  public static final int REVISION = 3;
  public static final String RELEASE = "4.0.0";
  private RuleDefaults() {}
  public static RuleSet latest(DefaultBases.Base base, boolean cheats) {
    RuleSet rules = cheats ? RuleSet.cheats() : new RuleSet();
    for (GameRule rule : GameRule.values()) rules.set(rule,
        DefaultBases.value(base, "rules." + (cheats ? "cheats." : "survival.") + rule.id(), rules.get(rule)));
    return rules;
  }
  public static JsonObject acknowledgement(DefaultBases.Base base) {
    JsonObject meta = new JsonObject();
    meta.addProperty("format", 1); meta.addProperty("base", base.id);
    meta.addProperty("defaultsRevision", REVISION); meta.addProperty("rulesVersion", RuleSet.VERSION);
    meta.addProperty("release", RELEASE);
    JsonObject signature = new JsonObject();
    for (String name : List.of("survival", "cheats"))
      signature.addProperty(name, signature(latest(base, name.equals("cheats")).values()));
    meta.add("defaultsSignature", signature); return meta;
  }
  private static String signature(JsonObject values) {
    JsonObject sorted = new JsonObject();
    values.keySet().stream().sorted().forEach(key -> sorted.add(key, values.get(key)));
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(sorted.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
  }
  /** Only proven, supported defaults follow a base change. Never infer this from custom values. */
  public static JsonObject followBase(JsonElement source,
      DefaultBases.Base previous, DefaultBases.Base next) {
    if (previous == next || source == null || !source.isJsonObject()) return null;
    JsonObject section = source.getAsJsonObject();
    try {
      JsonObject meta = section.has("review") ? section.getAsJsonObject("review") : null;
      DefaultBases.Base reference = meta == null ? previous : DefaultBases.Base.read(meta.get("base").getAsString());
      Review review = new Review(section, reference);
      if (!review.compatible()) return null;
      if (meta != null && meta.has("defaultsSignature")) {
        JsonObject signatures = meta.getAsJsonObject("defaultsSignature");
        for (String name : List.of("survival", "cheats")) {
          JsonElement saved = signatures.get(name);
          if (saved == null || !saved.isJsonPrimitive() || !saved.getAsJsonPrimitive().isString()
              || !saved.getAsString().matches("[0-9a-f]{64}")
              || !saved.getAsString().equals(signature(section.getAsJsonObject(name).getAsJsonObject("values")))) return null;
        }
      } else if (!review.matches) return null; // Older releases did not retain a defaults signature.
      JsonObject replacement = section.deepCopy();
      for (String name : List.of("survival", "cheats")) replacement.add(name, latest(next, name.equals("cheats")).document());
      replacement.add("review", acknowledgement(next)); return replacement;
    } catch (RuntimeException unsupported) { return null; }
  }
  public record Difference(String template, String id, String label, String current, String latest, boolean removable) {}
  public static final class Review {
    private final JsonElement expected;
    public final DefaultBases.Base base;
    private JsonObject draft;
    private String problem = "";
    private boolean compatible, matches, acknowledged;
    private List<Difference> differences = List.of();
    Review(JsonElement source, DefaultBases.Base base) {
      this.expected = source == null ? null : source.deepCopy(); this.base = base;
      draft = source != null && source.isJsonObject() ? source.getAsJsonObject().deepCopy() : new JsonObject();
      inspect();
    }
    private void inspect() {
      compatible = true; matches = true; problem = "";
      List<Difference> rows = new ArrayList<>();
      for (String name : List.of("survival", "cheats")) {
        boolean cheats = name.equals("cheats");
        RuleSet defaults = latest(base, cheats);
        JsonElement raw = draft.get(name);
        JsonObject values = raw != null && raw.isJsonObject() && raw.getAsJsonObject().has("values")
            && raw.getAsJsonObject().get("values").isJsonObject() ? raw.getAsJsonObject().getAsJsonObject("values") : new JsonObject();
        boolean supported = false;
        try { RuleSet.version(raw.getAsJsonObject()); supported = true; }
        catch (RuntimeException ignored) { }
        RuleSet converted = null;
        try { converted = TemplateDocuments.read(raw, cheats); }
        catch (RuntimeException failure) { compatible = false; problem = shortText(Objects.toString(failure.getMessage(), "Invalid template"), 220); }
        if (converted == null || !converted.equals(defaults) || !values.equals(defaults.values())) matches = false;
        for (GameRule rule : GameRule.values()) {
          JsonElement old = values.get(rule.id());
          if (old == null || !old.equals(defaults.get(rule)))
            rows.add(new Difference(name, rule.id(), rule.label(), old == null ? "Added" : display(old), display(defaults.get(rule)), false));
        }
        for (var field : values.entrySet()) {
          GameRule known = GameRule.find(field.getKey());
          if (known == null || !known.id().equals(field.getKey()))
            rows.add(new Difference(name, field.getKey(), shortText(field.getKey(), 96), display(field.getValue()),
                known == null ? "Removed / unknown" : "Now " + known.id(), supported));
        }
        if (!supported) rows.add(new Difference(name, "version", "Template format", raw != null && raw.isJsonObject()
            ? display(raw.getAsJsonObject().get("version")) : "Invalid", Integer.toString(RuleSet.VERSION), false));
      }
      JsonElement meta = draft.get("review");
      if (meta != null) {
        try {
          JsonObject data = meta.getAsJsonObject();
          if (data.get("format").getAsBigDecimal().intValueExact() != 1
              || data.get("defaultsRevision").getAsBigDecimal().intValueExact() > REVISION
              || data.get("rulesVersion").getAsBigDecimal().intValueExact() > RuleSet.VERSION)
            throw new IllegalArgumentException();
        } catch (RuntimeException future) { compatible = matches = false; problem = "Unsupported creation-default review format. Original templates are preserved."; }
      }
      acknowledged = false;
      if (compatible && meta != null && meta.isJsonObject()) try {
        JsonObject data = meta.getAsJsonObject();
        acknowledged = data.get("format").getAsBigDecimal().intValueExact() == 1
            && data.get("base").getAsString().equals(base.id)
            && data.get("defaultsRevision").getAsBigDecimal().intValueExact() == REVISION
            && data.get("rulesVersion").getAsBigDecimal().intValueExact() == RuleSet.VERSION;
      } catch (RuntimeException ignored) { }

      if (rows.size() > 512) problem = "Showing the first 512 differences. Reset replaces both templates after saving a verified backup.";
      differences = List.copyOf(rows.subList(0, Math.min(512, rows.size())));
    }
    public String releases() {
      try {
        if (expected != null && expected.isJsonObject()
            && expected.getAsJsonObject().getAsJsonObject("review").get("defaultsRevision").getAsInt() >= 1) return RELEASE;
      } catch (RuntimeException ignored) { }
      return "3.0.0 and " + RELEASE;
    }
    public boolean compatible() { return compatible; }
    public boolean required() { return !compatible || !matches && !acknowledged; }
    public boolean needsAcknowledgement() { return !acknowledged; }
    public String problem() { return problem; }
    public List<Difference> differences() { return differences; }
    public void remove(Difference field) {
      if (!field.removable() || !differences.contains(field)) throw new IllegalArgumentException("This field cannot be removed individually.");
      draft.getAsJsonObject(field.template()).getAsJsonObject("values").remove(field.id()); inspect();
    }
    public void commit(boolean reset) throws IOException {
      if (!reset && !compatible) throw new IOException("Incompatible creation templates require Reset or a compatible release.");
      JsonObject next = draft.deepCopy();
      for (String name : List.of("survival", "cheats")) {
        RuleSet rules = reset ? latest(base, name.equals("cheats")) : TemplateDocuments.read(next.get(name), name.equals("cheats"));
        next.add(name, rules.document());
      }
      next.add("review", acknowledgement(base));
      PowerConfig.replaceSection("gameRules", expected, next);
    }
    private static String shortText(String text, int limit) {
      String safe = text.replaceAll("[\\p{Cntrl}\u00a7]", " ");
      return safe.length() <= limit ? safe : safe.substring(0, limit - 3) + "...";
    }
    private static String display(JsonElement value) {
      if (value == null) return "Missing";
      if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) return value.getAsBoolean() ? "On" : "Off";
      String text = value.toString(); return text.length() <= 80 ? text : text.substring(0, 77) + "...";
    }
  }
  public static Review review() {
    JsonObject root = PowerConfig.document();
    return new Review(root.getAsJsonObject("settings").get("gameRules"), DefaultBases.current());
  }
  public static void resetLatest() throws IOException { review().commit(true); }
}
