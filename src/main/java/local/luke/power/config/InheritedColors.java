package local.luke.power.config;

import com.google.gson.JsonPrimitive;
import java.util.Collection;

/** Optional RGB overrides use -1 for inheritance and stay inside their owning settings draft. */
public final class InheritedColors {
  private InheritedColors() {}

  public static boolean accepts(Setting setting) {
    return setting.kind == Setting.Kind.INTEGER && Catalog.metadata(setting.id).has("inheritsColor");
  }

  public static boolean custom(Setting setting) {
    return accepts(setting) && setting.value.getAsInt() >= 0;
  }

  public static String label(Setting setting) {
    return custom(setting) ? "Custom" : "Inherit";
  }

  public static int inheritedRgb(Setting setting, Collection<Setting> settings) {
    if (!accepts(setting)) throw new IllegalArgumentException("This color cannot inherit");
    String parent = Catalog.metadata(setting.id).get("inheritsColor").getAsString();
    return settings.stream().filter(value -> value.id.equals(parent)).findFirst()
        .map(ColorSettings::rgb)
        .orElseThrow(() -> new IllegalArgumentException("Missing inherited color: " + parent));
  }

  public static void toggle(Setting setting, Collection<Setting> settings) {
    JsonPrimitive next = new JsonPrimitive(custom(setting) ? -1 : inheritedRgb(setting, settings));
    setting.validate(next);
    setting.value = next;
  }
}
