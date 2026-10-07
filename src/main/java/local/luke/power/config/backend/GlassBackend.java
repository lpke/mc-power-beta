package local.luke.power.config.backend;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import net.glasslauncher.mods.gcapi3.api.*;
import net.glasslauncher.mods.gcapi3.impl.*;
import net.glasslauncher.mods.gcapi3.impl.object.*;
import net.minecraft.client.resource.language.TranslationStorage;

public final class GlassBackend implements Backend {
  private final String id;
  private final ConfigRootEntry root;
  private final Map<String, ConfigEntryHandler<?>> handlers = new LinkedHashMap<>();
  private final List<Setting> entries = new ArrayList<>();

  public GlassBackend(String id, ConfigRootEntry root) {
    this.id = id;
    this.root = root;
    visit("", root.configCategoryHandler());
  }

  public static void register(ConfigSession session) {
    GCCore.MOD_CONFIGS.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            e -> {
              GlassBackend b = new GlassBackend(e.getKey(), e.getValue());
              session.add(b, b.entries);
            });
  }

  private static boolean inverted(String key) { return key.equals("power_hud:config.disableVignette") || key.equals("entityculling:config.disableEntityCulling") || key.equals("entityculling:config.disableBlockEntityCulling"); }
  private static JsonElement encode(String key, Object value) { return inverted(key) ? new JsonPrimitive(!(Boolean)value) : Catalog.JSON.toJsonTree(value); }

  private void visit(String prefix, ConfigCategoryHandler category) {
    for (ConfigHandlerBase child : category.values.values()) {
      if (child instanceof ConfigCategoryHandler nested) {
        visit(prefix + child.id + ".", nested);
        continue;
      }
      ConfigEntryHandler<?> h = (ConfigEntryHandler<?>) child;
      ConfigEntry m = h.parentField.getAnnotation(ConfigEntry.class);
      String key = id + "." + prefix + h.id;
      if (Catalog.text(Catalog.metadata(key), "group", "").equals("Available controls")) continue;
      JsonObject metadata = Catalog.metadata(key);
      if (metadata.has("hidden") && metadata.get("hidden").getAsBoolean()) continue;
      String supersedingMod = Catalog.text(metadata, "hiddenWithMod", "");
      if (!supersedingMod.isEmpty() && net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(supersedingMod)) continue;
      handlers.put(key, h);
      Class<?> type = h.parentField.getType();
      List<String> choices =
          type.isEnum()
              ? Arrays.stream(type.getEnumConstants())
                  .map(v -> TranslationStorage.getInstance().get(v.toString()))
                  .map(Catalog::words)
                  .toList()
              : List.of();
      Setting.Kind kind =
          type.isEnum()
              ? Setting.Kind.CHOICE
              : type == Boolean.class
                  ? Setting.Kind.BOOLEAN
                  : type == Integer.class
                      ? Setting.Kind.INTEGER
                      : type == Float.class || type == Double.class
                          ? Setting.Kind.DECIMAL
                          : type.isArray() ? Setting.Kind.LIST : Setting.Kind.TEXT;
      entries.add(
          Catalog.setting(
              key,
              id,
              "Advanced",
              "Other settings",
              h.name,
              h.description,
              kind,
              encode(key, h.value),
              encode(key, h.defaultValue),
              m.minValue() == 0 ? m.minLength() : m.minValue(),
              m.maxValue() == 32 ? m.maxLength() : m.maxValue(),
              kind == Setting.Kind.DECIMAL ? .05 : 1,
              choices,
              m.requiresRestart()));
    }
  }

  public String id() {
    return id;
  }

  public List<Path> files() {
    return List.of(local.luke.power.storage.PowerConfig.path());
  }

  public void validate(Map<String, JsonElement> values) throws Exception {
    for (var e : values.entrySet()) {
      ConfigEntryHandler<?> h = handlers.get(e.getKey());
      if (h == null) throw new IllegalArgumentException("Unknown setting");
      if (h.multiplayerLoaded)
        throw new IllegalArgumentException("This value is controlled by the server");
      Class<?> type = h.parentField.getType();
      JsonElement v = e.getValue();
      if (type.isArray()) {
        ConfigEntry m = h.parentField.getAnnotation(ConfigEntry.class);
        if (!v.isJsonArray()
            || v.getAsJsonArray().size() > m.maxArrayLength()
            || v.getAsJsonArray().size() < m.minArrayLength())
          throw new IllegalArgumentException("Invalid list length");
        for (JsonElement element : v.getAsJsonArray()) {
          if (!element.isJsonPrimitive())
            throw new IllegalArgumentException("List entries must be simple values");
          if (type.getComponentType() == Integer.class) element.getAsBigDecimal().intValueExact();
          else if (!element.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Use quoted text in this list");
        }
      }
      if (e.getKey().endsWith("panoramaFolder") && !v.getAsString().matches("[A-Za-z0-9_-]{1,64}"))
        throw new IllegalArgumentException("Use a folder name with letters, numbers, - or _");
    }
    Map<String, JsonElement> all = new HashMap<>();
    handlers.forEach((k, v) -> all.put(k, Catalog.JSON.toJsonTree(v.value)));
    all.putAll(values);
    String min = id + ".fastLeafDecay.minimumDecayTime",
        max = id + ".fastLeafDecay.maximumDecayTime";
    if (all.containsKey(min) && all.get(min).getAsInt() >= all.get(max).getAsInt())
      throw new IllegalArgumentException("Minimum leaf decay time must be below maximum");
  }

  public boolean previewsAutomatically(Setting s) { return !s.restart; }

  public boolean previews(Setting s) {
    return !s.restart && (s.page.equals("Audio") || s.page.equals("Video")
        || s.id.equals("entityculling:config.showF3Info") || s.id.equals("entityculling:config.f3InfoYOffset")
        || s.id.equals("power_controls:userinterface.frontViewThirdPerson"));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public void preview(Map<String, JsonElement> values) throws Exception {
    for (var e : values.entrySet()) {
      ConfigEntryHandler h = handlers.get(e.getKey());
      Class<?> type = h.parentField.getType();
      h.value = inverted(e.getKey()) ? !e.getValue().getAsBoolean() : Catalog.JSON.fromJson(e.getValue(), type.isEnum() ? Integer.class : type);
      h.saveToField();
    }
    local.luke.power.audio.AudioController.rulesChanged();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public void apply(Map<String, JsonElement> values) {
    for (var e : values.entrySet()) {
      ConfigEntryHandler h = handlers.get(e.getKey());
      Class<?> type = h.parentField.getType();
      h.value = inverted(e.getKey()) ? !e.getValue().getAsBoolean() : Catalog.JSON.fromJson(e.getValue(), type.isEnum() ? Integer.class : type);
    }
    GCCore.saveConfig(
        root.modContainer(), root.configCategoryHandler(), EventStorage.EventSource.USER_SAVE);
    local.luke.power.audio.AudioController.rulesChanged();
  }
}
