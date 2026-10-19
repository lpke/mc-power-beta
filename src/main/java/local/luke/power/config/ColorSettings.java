package local.luke.power.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import local.luke.power.light.LightSettings;
import local.luke.power.ui.ColorValue;

/** Color controls share an edit boundary without changing their saved field IDs or types. */
public final class ColorSettings {
  private ColorSettings() {}

  private static ColorValue.Palette palette(Setting color) {
    return switch (color.id) {
      case "tweaks.flexible.overlayColor" -> ColorValue.Palette.FLEXIBLE;
      case "worldedit.color" -> ColorValue.Palette.WORLD_EDIT;
      default -> null;
    };
  }

  public static boolean accepts(Setting setting) {
    if (palette(setting) != null || rawRgb(setting)) return true;
    String id = setting.id.toLowerCase(Locale.ROOT);
    return setting.kind == Setting.Kind.TEXT && (id.contains("color") || id.contains("colour"))
        && setting.value.isJsonPrimitive() && setting.value.getAsString().matches("#?[0-9a-fA-F]{6}");
  }

  public static Setting opacity(Setting color, Collection<Setting> settings) {
    if (!accepts(color)) return null;
    String id = color.id;
    String paired = id.equals("worldedit.color") ? "worldedit.opacity"
        : id.endsWith("textColor") ? id.substring(0, id.length() - "textColor".length()) + "opacity"
        : id.replaceFirst("(?:Color|Colour)$", "Opacity");
    if (paired.equals(id)) return null;
    return settings.stream().filter(s -> s.id.equals(paired) && s.numeric()).findFirst().orElse(null);
  }

  public static boolean hiddenOpacity(Setting setting, Collection<Setting> settings) {
    if (!setting.numeric() || !setting.id.toLowerCase(Locale.ROOT).endsWith("opacity")) return false;
    return settings.stream().anyMatch(color -> opacity(color, settings) == setting);
  }

  public static List<Setting> linked(Setting color, Collection<Setting> settings) {
    Setting opacity = opacity(color, settings);
    return opacity == null ? List.of(color) : List.of(color, opacity);
  }

  public static void reset(Setting color, Collection<Setting> settings) {
    linked(color, settings).forEach(Setting::reset);
  }

  public static boolean atDefault(Setting color, Collection<Setting> settings) {
    return linked(color, settings).stream().allMatch(s -> s.value.equals(s.defaultValue));
  }

  public static int rgb(Setting color) {
    ColorValue.Palette palette = palette(color);
    if (palette != null) return palette.rgb(color.value.getAsBigDecimal().intValueExact());
    if (rawRgb(color)) {
      int rgb = color.value.getAsBigDecimal().intValueExact();
      if (rgb < 0 || rgb > 0xffffff) throw new IllegalArgumentException("Invalid RGB color");
      return rgb;
    }
    return LightSettings.rgb(color.value.getAsString());
  }

  public static String hex(Setting color) { return hex(rgb(color)); }
  private static boolean rawRgb(Setting color) { return color.id.equals("maps.pathColor") || color.id.equals("maps.playerColor"); }
  public static String hex(int rgb) { return String.format(Locale.ROOT, "#%06X", rgb & 0xffffff); }

  public static double opacityPercent(Setting color, Collection<Setting> settings) {
    return opacityPercent(opacity(color, settings));
  }

  public static double opacityPercent(Setting opacity) {
    return opacity == null ? 100 : opacity.value.getAsBigDecimal().multiply(BigDecimal.valueOf(opacityScale(opacity))).doubleValue();
  }

  public static double opacityScale(Setting opacity) { return opacity.max <= 1 ? 100 : 1; }

  public static String label(Setting color, Collection<Setting> settings) {
    return label(hex(color), opacityPercent(color, settings));
  }

  public static String label(String hex, double opacity) {
    return hex + (Math.abs(opacity - 100) < .000001 ? "" : " " + Setting.number(Math.round(opacity * 100) / 100d) + "%");
  }

  /** Validate both fields before publishing either one. No previews or durable writes happen here. */
  public static void apply(Setting color, Setting opacity, int rgb, double percent) {
    if (rgb < 0 || rgb > 0xffffff) throw new IllegalArgumentException("Invalid RGB color");
    ColorValue.Palette palette = palette(color);
    JsonElement nextColor = palette != null ? new JsonPrimitive(palette.encode(rgb))
        : rawRgb(color) ? new JsonPrimitive(rgb) : new JsonPrimitive(hex(rgb));
    JsonElement nextOpacity = null;
    color.validate(nextColor);
    if (opacity != null) {
      if (!Double.isFinite(percent)) throw new IllegalArgumentException("Enter a finite opacity");
      BigDecimal raw = BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(opacityScale(opacity)));
      nextOpacity = opacity.kind == Setting.Kind.INTEGER ? new JsonPrimitive(raw.intValueExact()) : new JsonPrimitive(raw);
      opacity.validate(nextOpacity);
    }
    color.value = nextColor;
    if (opacity != null) opacity.value = nextOpacity;
  }
}
