package local.luke.power.config.backend;

import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;

/** Writes the title configuration atomically, then asks the existing renderer to reload it. */
public final class LogoBackend implements Backend {
  private final Map<String, Object> keys = new LinkedHashMap<>();
  private final List<Setting> entries = new ArrayList<>();
  private final Object config;

  public LogoBackend() throws Exception {
    Class<?> logo = Class.forName("local.luke.power.title.TitleFeatures");
    config = logo.getField("CONFIG").get(null);
    for (Field f : logo.getFields()) {
      if (!f.getType().getSimpleName().equals("ConfigKey")) continue;
      Object key = f.get(null);
      String name = (String) key.getClass().getField("key").get(key);
      String id = "logo." + name;
      Object value = key.getClass().getMethod("get").invoke(key),
          defaults = key.getClass().getField("defaultValue").get(key);
      keys.put(id, key);
      Setting.Kind kind =
          value instanceof Boolean
              ? Setting.Kind.BOOLEAN
              : value instanceof Number ? Setting.Kind.DECIMAL : Setting.Kind.LIST;
      String label =
          switch (f.getName()) {
            case "logo" -> "Title lettering";
            case "logoRotation" -> "Title rotation";
            case "blockMap" -> "Letter block palette";
            case "invertedBlocks" -> "Invert title blocks";
            case "scrollingBackground" -> "Scrolling menu background";
            case "scrollingBackgroundSpeed" -> "Background scroll speed";
            case "scrollingBackgroundRestricted" -> "Scroll only on the title screen";
            case "shadow" -> "Title shadow";
            case "animation" -> "Title animation";
            case "animationSpeed" -> "Title animation speed";
            default -> Catalog.words(f.getName());
          };
      String description =
          name.equals("logo.rotation")
              ? "Three rotation angles in degrees: [15, 0, 0]."
              : name.equals("blocks.map")
                  ? "Map single letters to block IDs, for example {\"*\":1}."
                  : name.equals("logo.lines")
                      ? "Text rows use the letter block palette. Spaces leave gaps."
                      : "";
      entries.add(
          Catalog.setting(
              id,
              id(),
              "Interface",
              "Title screen",
              label,
              description,
              kind,
              Catalog.JSON.toJsonTree(value),
              Catalog.JSON.toJsonTree(defaults),
              0,
              10,
              .1,
              List.of(),
              false));
    }
  }

  public static void register(ConfigSession s) throws Exception {
    LogoBackend b = new LogoBackend();
    s.add(b, b.entries);
  }

  public String id() {
    return "logo";
  }

  public List<Path> files() {
    return List.of(local.luke.power.storage.PowerConfig.path());
  }

  public void validate(Map<String, JsonElement> values) {
    for (var e : values.entrySet()) {
      String id = e.getKey();
      JsonElement v = e.getValue();
      if (id.equals("logo.logo.rotation")) {
        if (!v.isJsonArray() || v.getAsJsonArray().size() != 3)
          throw new IllegalArgumentException("Use three rotation angles");
        for (var a : v.getAsJsonArray())
          if (!a.isJsonPrimitive()
              || !a.getAsJsonPrimitive().isNumber()
              || !Double.isFinite(a.getAsDouble())
              || Math.abs(a.getAsDouble()) > 360)
            throw new IllegalArgumentException("Angles must be -360 to 360");
      }
      if (id.equals("logo.logo.lines")) {
        if (!v.isJsonArray() || v.getAsJsonArray().size() < 1 || v.getAsJsonArray().size() > 12)
          throw new IllegalArgumentException("Use 1 to 12 title rows");
        for (var line : v.getAsJsonArray())
          if (!line.isJsonPrimitive()
              || !line.getAsJsonPrimitive().isString()
              || line.getAsString().length() > 80)
            throw new IllegalArgumentException("Title rows must be text up to 80 characters");
      }
      if (id.equals("logo.blocks.map")) {
        if (!v.isJsonObject() || v.getAsJsonObject().size() > 64)
          throw new IllegalArgumentException("Use a letter-to-block object");
        for (var block : v.getAsJsonObject().entrySet()) {
          int n = block.getValue().getAsBigDecimal().intValueExact();
          if (block.getKey().length() != 1
              || n <= 0
              || n >= Block.BLOCKS.length
              || Block.BLOCKS[n] == null)
            throw new IllegalArgumentException("Use single letters and existing block IDs");
        }
      }
    }
  }

  public void apply(Map<String, JsonElement> values) throws Exception {
    Path file = files().get(0);
    JsonObject data = local.luke.power.storage.PowerConfig.section("title");
    for (var e : values.entrySet()) {
      String[] path = e.getKey().substring(5).split("\\.");
      JsonObject at = data;
      for (int i = 0; i < path.length - 1; i++) {
        if (!at.has(path[i])) at.add(path[i], new JsonObject());
        at = at.getAsJsonObject(path[i]);
      }
      at.add(path[path.length - 1], e.getValue().deepCopy());
    }
    local.luke.power.storage.PowerConfig.put("title", data);
    config.getClass().getMethod("load").invoke(config);
    for (Object key : keys.values()) key.getClass().getMethod("load").invoke(key);
  }
}
