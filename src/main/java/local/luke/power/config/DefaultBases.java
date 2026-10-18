package local.luke.power.config;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.backend.DefaultsBackend;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;

/** Reset targets are independent of the currently applied settings. */
public final class DefaultBases {
  public enum Base {
    FULL("power_beta_full", "Power Beta Full", "The full Power Beta setup, with vein mining, torch shovel, faster vehicles, clearer water and extra visual controls enabled. Temporary tools still need their controls, and world permissions still apply."),
    POWER_BETA("power_beta", "Power Beta Lite", "The original Power Beta defaults, with gameplay additions, fixes and inventory controls. Vein mining, faster vehicles and extra visual overrides start off."),
    REFINED("refined_beta", "Refined Beta 1.7.3", "Beta gameplay with performance improvements, fixes and inventory quality of life. Extra gameplay mechanics and recipes are off."),
    BETA("beta", "Beta 1.7.3", "Closest to vanilla Beta. Optional fixes, culling and performance controls are off. SmoothBeta rendering, StationAPI, shared input/configuration, GlassNetworking, RetroAuth and faster startup remain installed. Pack menus, safe data recovery and platform compatibility remain available.");
    public final String id, label, description;
    Base(String id, String label, String description) { this.id = id; this.label = label; this.description = description; }
    public static Base read(String id) {
      for (Base base : values()) if (base.id.equals(id)) return base;
      throw new IllegalArgumentException("Unknown defaults base: " + id + ". Use a compatible Power Beta version.");
    }
  }
  public static final class State {
    public String base = Base.POWER_BETA.id;
    public boolean setupComplete;
    public void validate() { Base.read(base); }
  }
  private static volatile Base current = Base.POWER_BETA;
  private static volatile boolean setupComplete = true;
  private static final JsonObject OVERRIDES = overrides();
  private DefaultBases() {}
  private static JsonObject overrides() {
    try (var input = DefaultBases.class.getResourceAsStream("/assets/powerbeta/default-bases.json")) {
      if (input == null) throw new IOException("Missing defaults bases");
      return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
    } catch (IOException e) { throw new ExceptionInInitializerError(e); }
  }
  public static State state() {
    JsonObject values = PowerConfig.section("defaults");
    if (!values.has("base") || !values.get("base").isJsonPrimitive() || !values.getAsJsonPrimitive("base").isString()
        || !values.has("setupComplete") || !values.get("setupComplete").isJsonPrimitive() || !values.getAsJsonPrimitive("setupComplete").isBoolean())
      throw new IllegalArgumentException("Invalid defaults selection. Restore a settings backup or use a compatible Power Beta version.");
    State state = Catalog.JSON.fromJson(values, State.class); state.validate(); return state;
  }
  public static void reload() {
    State state = state();
    current = Base.read(state.base); setupComplete = state.setupComplete;
  }
  public static Base current() { return current; }
  public static boolean needsSetup() { return !setupComplete; }
  public static JsonElement value(String id, JsonElement powerBeta) { return value(current, id, powerBeta); }
  public static JsonElement value(Base base, String id, JsonElement powerBeta) {
    JsonObject values = base == Base.POWER_BETA ? null : OVERRIDES.getAsJsonObject(base.id);
    String lookup = id.startsWith("rules.") ? "rule." + id.substring(id.lastIndexOf('.') + 1) : id;
    return values != null && values.has(lookup) ? values.get(lookup).deepCopy() : powerBeta.deepCopy();
  }
  public static void use(Base base, Path game) throws Exception {
    ConfigSession session = new ConfigSession(); DefaultsBackend.register(session);
    session.settings().stream().filter(s -> s.id.equals("defaults.base")).forEach(s -> s.value = new JsonPrimitive(base.id));
    session.save(game); reload();
  }
  public static boolean load(Base base, Minecraft mc, Path game) throws Exception {
    ConfigSession session = SettingsRegistry.openDefaults(mc);
    Set<String> libraries = Set.of("audio.presets", "audio.favourites", "logo.presets", "interface.debugPresets", "interface.colours");
    for (Setting setting : session.settings()) {
      if (libraries.contains(setting.id)) continue;
      setting.value = value(base, setting.id, setting.powerBetaDefault);
    }
    session.settings().stream().filter(s -> s.id.equals("defaults.base")).forEach(s -> s.value = new JsonPrimitive(base.id));
    session.settings().stream().filter(s -> s.id.equals("defaults.setupComplete")).forEach(s -> s.value = new JsonPrimitive(true));
    boolean restart = session.restartRequired();
    session.save(game); reload(); return restart;
  }
}
