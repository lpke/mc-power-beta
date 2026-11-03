package local.luke.power.config.backend;

import com.google.gson.*;
import java.util.Set;
import local.luke.power.config.Catalog;

/** Translate presentation values without changing the persisted configuration fields. */
final class GlassValues {
  private GlassValues() {}

  private static boolean inverted(String key) {
    return key.startsWith("power_compat:config.PARTICLES_CONFIG.disable")
        || Set.of("power_hud:config.disableVignette", "entityculling:config.disableEntityCulling",
            "entityculling:config.disableBlockEntityCulling", "power_hud:config.disableCrosshair",
            "power_controls:userinterface.hideAchievementToast", "power_controls:userinterface.disableDebugEntityIdTags").contains(key);
  }

  static JsonElement encode(String key, Object value) {
    return value instanceof Boolean && inverted(key) ? new JsonPrimitive(!(Boolean)value) : Catalog.JSON.toJsonTree(value);
  }

  static Object decode(String key, JsonElement value, Class<?> type) {
    return (type == Boolean.class || type == boolean.class) && inverted(key) ? !value.getAsBoolean()
        : Catalog.JSON.fromJson(value, type.isEnum() ? Integer.class : type);
  }
}
