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
    POWER_BETA("power_beta", "Power Beta", "Larger quality-of-life upgrades, including ore vein mining, faster boats and minecarts, easier building, clearer water and extra recipes and tool abilities. Includes inventory shortcuts, bug fixes and performance improvements."),
    REFINED("refined_beta", "Refined Beta 1.7.3", "Original Beta gameplay with bug fixes, smoother performance and easier inventory management. Keeps native recipes, tools and travel speeds; vein mining and other added gameplay mechanics are off."),
    BETA("beta", "Beta 1.7.3", "The original Beta experience, including its quirks. Optional bug fixes, inventory shortcuts and extra performance tweaks are off, so busy worlds may run slower. The bundled graphics renderer, faster startup, online login and compatibility support remain installed.");
    public final String id, label, description;
    Base(String id, String label, String description) { this.id = id; this.label = label; this.description = description; }
    public static Base read(String id) {
      if ("power_beta_full".equals(id)) return POWER_BETA; // Retain the retired Full selection without changing values.
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
    JsonObject values = OVERRIDES.getAsJsonObject(base.id);
    String lookup = id.startsWith("rules.") ? "rule." + id.substring(id.lastIndexOf('.') + 1) : id;
    return values != null && values.has(id) ? values.get(id).deepCopy()
        : values != null && values.has(lookup) ? values.get(lookup).deepCopy() : powerBeta.deepCopy();
  }
  public static void use(Base base, Path game) throws Exception {
    ConfigSession session = new ConfigSession(); DefaultsBackend.register(session);
    session.settings().stream().filter(s -> s.id.equals("defaults.base")).forEach(s -> s.value = new JsonPrimitive(base.id));
    session.save(game); reload();
  }
  public static boolean load(Base base, Minecraft mc, Path game) throws Exception {
    if (RuleDefaults.review().required()) RuleDefaults.resetLatest();
    ConfigSession session = SettingsRegistry.openDefaults(mc);
    Set<String> libraries = Set.of("audio.presets", "audio.favourites", "logo.presets", "interface.debugPresets", "interface.colours");
    for (Setting setting : session.settings()) {
      if (libraries.contains(setting.id) || Set.of("inventoryTools.protectedSlots", "inventoryTools.preferredItems").contains(setting.id)) continue;
      setting.value = value(base, setting.id, setting.powerBetaDefault);
    }
    session.settings().stream().filter(s -> s.id.equals("defaults.base")).forEach(s -> s.value = new JsonPrimitive(base.id));
    session.settings().stream().filter(s -> s.id.equals("defaults.setupComplete")).forEach(s -> s.value = new JsonPrimitive(true));
    boolean restart = session.restartRequired();
    session.save(game); reload();
    RuleDefaults.review().commit(false);
    return restart;
  }
}
