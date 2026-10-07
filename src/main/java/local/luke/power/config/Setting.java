package local.luke.power.config;

import com.google.gson.*;
import java.math.BigDecimal;
import java.util.*;

/** A typed draft value. ConfigSession explicitly previews eligible edits without saving them. */
public final class Setting {
  public enum Kind {
    BOOLEAN,
    INTEGER,
    DECIMAL,
    CHOICE,
    TEXT,
    LIST,
    KEY
  }

  public final String id, backend, page, group, label, description;
  public final Kind kind;
  public final List<String> choices;
  public final JsonElement defaultValue;
  public final double min, max, step;
  public final boolean restart;
  private final double displayDivisor;
  private final String unit, control;
  private transient java.util.function.Consumer<JsonElement> checkValue = value -> {};
  public JsonElement value;
  private JsonElement original;

  public Setting(
      String id,
      String backend,
      String page,
      String group,
      String label,
      String description,
      Kind kind,
      JsonElement value,
      JsonElement defaultValue,
      double min,
      double max,
      double step,
      List<String> choices,
      boolean restart) {
    this.id = id;
    this.backend = backend;
    JsonObject metadata = Catalog.metadata(id);
    this.displayDivisor = Catalog.number(metadata, "displayDivisor", 1);
    this.unit = Catalog.text(metadata, "unit", "");
    this.control = Catalog.text(metadata, "control", "slider");
    this.page = Catalog.text(metadata, "page", page);
    this.group = Catalog.text(metadata, "group", group);
    this.label = Catalog.text(metadata, "label", label);
    this.description = Tooltips.description(id, this.label, Catalog.text(metadata, "description", description));
    this.kind = kind;
    this.value = value.deepCopy();
    this.original = value.deepCopy();
    this.defaultValue = defaultValue.deepCopy();
    this.min = min;
    this.max = max;
    this.step = step;
    this.choices = List.copyOf(choices);
    this.restart = restart;
  }

  public boolean changed() {
    return !value.equals(original);
  }

  public JsonElement original() {
    return original.deepCopy();
  }

  public void accept() {
    original = value.deepCopy();
  }

  public void reset() {
    value = defaultValue.deepCopy();
  }

  public void cycle(int direction) {
    if (direction != 1 && direction != -1)
      throw new IllegalArgumentException("Direction must be -1 or 1");
    switch (kind) {
      case BOOLEAN -> value = new JsonPrimitive(!value.getAsBoolean());
      case CHOICE ->
          value = new JsonPrimitive(Math.floorMod(value.getAsInt() + direction, choices.size()));
      case INTEGER, DECIMAL -> {
        BigDecimal next = value.getAsBigDecimal().add(BigDecimal.valueOf(step * direction));
        if (next.doubleValue() > max + 1e-8) next = BigDecimal.valueOf(min);
        if (next.doubleValue() < min - 1e-8) next = BigDecimal.valueOf(max);
        value =
            kind == Kind.INTEGER
                ? new JsonPrimitive(next.intValueExact())
                : new JsonPrimitive(next.stripTrailingZeros());
      }
      default -> {}
    }
  }

  public void slide(double fraction) {
    if (kind != Kind.INTEGER && kind != Kind.DECIMAL) return;
    double raw = min + Math.max(0, Math.min(1, fraction)) * (max - min);
    double next = Math.max(min, Math.min(max, min + Math.round((raw - min) / step) * step));
    value = kind == Kind.INTEGER ? new JsonPrimitive((int) Math.round(next))
        : new JsonPrimitive(BigDecimal.valueOf(next).setScale(6, java.math.RoundingMode.HALF_UP).stripTrailingZeros());
  }

  public void parse(String text) {
    JsonElement next =
        switch (kind) {
          case TEXT -> new JsonPrimitive(text);
          case LIST -> JsonParser.parseString(text);
          case BOOLEAN -> {
            if (!text.equals("true") && !text.equals("false"))
              throw new IllegalArgumentException("Use true or false");
            yield new JsonPrimitive(Boolean.parseBoolean(text));
          }
          default -> new JsonPrimitive(new BigDecimal(text.trim()).multiply(BigDecimal.valueOf(displayDivisor)));
        };
    validate(next);
    value = next;
  }

  public void validate(JsonElement next) {
    if (next == null || next.isJsonNull())
      throw new IllegalArgumentException(label + ": a value is required");
    checkValue.accept(next);
    if (kind == Kind.KEY) {
      local.luke.power.input.Chord.decode(next.getAsBigDecimal().intValueExact());
      return;
    }
    if (kind == Kind.LIST) {
      if (!next.isJsonArray() && !next.isJsonObject())
        throw new IllegalArgumentException("Enter a list or object");
      int limit = switch (id) {
        case "audio.presets", "audio.favourites", "audio.exclusions", "audio.groupVolumes", "audio.presetTrackPool" -> 1048576;
        default -> 16384;
      };
      if (next.toString().length() > limit)
        throw new IllegalArgumentException("Value exceeds " + limit + " characters");
      return;
    }
    if (!next.isJsonPrimitive()) throw new IllegalArgumentException(label + ": invalid value type");
    JsonPrimitive p = next.getAsJsonPrimitive();
    if (kind == Kind.BOOLEAN) {
      if (!p.isBoolean()) throw new IllegalArgumentException("Choose On or Off");
      return;
    }
    if (kind == Kind.TEXT) {
      if (!p.isString() || next.getAsString().length() > 4096)
        throw new IllegalArgumentException("Text exceeds 4096 characters");
      return;
    }
    if (!p.isNumber()) throw new IllegalArgumentException("Enter a number");
    double n = next.getAsDouble();
    if (!Double.isFinite(n)) throw new IllegalArgumentException("Enter a finite number");
    if (kind == Kind.CHOICE) {
      if (n != Math.rint(n) || n < 0 || n >= choices.size())
        throw new IllegalArgumentException("Choose a listed option");
      return;
    }
    if (kind == Kind.INTEGER) {
      try { next.getAsBigDecimal().intValueExact(); }
      catch (ArithmeticException e) { throw new IllegalArgumentException(displayDivisor == 1 ? "Enter a whole number" : "Use increments of " + number(1 / displayDivisor) + " seconds"); }
    }
    if (n < min || n > max)
      throw new IllegalArgumentException("Use " + rangeText());
  }

  public String display() {
    return display(value);
  }

  public String display(JsonElement v) {
    if (id.equals("tweaks.obsidianBreakingSpeed"))
      return v.getAsInt() == 0 ? "Off (vanilla)" : v.getAsInt() + "%";
    if (id.equals("native.fpsLimit") && v.getAsInt() > 1000) return "Unlimited";
    if (id.equals("native.guiScale")) return v.getAsInt() == 0 ? "Auto" : v.getAsInt() + "x";
    return switch (kind) {
      case BOOLEAN ->
          choices.size() == 2
              ? choices.get(v.getAsBoolean() ? 1 : 0)
              : (v.getAsBoolean() ? "On" : "Off");
      case CHOICE -> {
        int n = v.getAsInt();
        yield n >= 0 && n < choices.size() ? choices.get(n) : "Invalid selection";
      }
      case LIST ->
          v.isJsonArray()
              ? v.getAsJsonArray().size() + " entries"
              : v.getAsJsonObject().size() + " entries";
      case INTEGER, DECIMAL -> numericText(v.getAsBigDecimal());
      default -> v.getAsString();
    };
  }

  public String editText() {
    return numeric() ? numericText(value.getAsBigDecimal()) : kind == Kind.LIST ? value.toString() : value.getAsString();
  }

  public boolean numeric() { return kind == Kind.INTEGER || kind == Kind.DECIMAL; }
  public boolean slider() { return numeric() && !control.equals("number"); }
  public void validator(java.util.function.Consumer<JsonElement> validator) { checkValue = Objects.requireNonNull(validator); }
  private String numericText(BigDecimal value) {
    return value.divide(BigDecimal.valueOf(displayDivisor)).stripTrailingZeros().toPlainString();
  }
  public String rangeText() {
    String limits = numericText(BigDecimal.valueOf(min)) + " to " + numericText(BigDecimal.valueOf(max));
    return limits + (unit.isEmpty() ? "" : " " + unit);
  }

  public static String number(double v) {
    return BigDecimal.valueOf(v).stripTrailingZeros().toPlainString();
  }
}
