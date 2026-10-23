package local.luke.power.creativeui;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.creative.CreativeTabsData;
import local.luke.power.storage.PowerConfig;

public final class CreativeTabsBackend implements Backend {
  public static final String SETTING = "creativeTabs.tabs";
  public static void register(ConfigSession session) {
    JsonObject current = PowerConfig.section("creativeTabs");
    if (current.size() == 0) current = CreativeTabsData.defaults();
    Setting setting = new Setting(SETTING, "creativeTabs", "Creative", "Inventory", "Creative tabs",
        "Edit Creative tab names, icons, order and item layouts. Search always stays available.",
        Setting.Kind.LIST, current, CreativeTabsData.defaults(), 0, 0, 1, List.of(), false);
    setting.validator(value -> CreativeTabsData.read(value.getAsJsonObject()));
    session.add(new CreativeTabsBackend(), List.of(setting));
  }
  public String id() { return "creativeTabs"; }
  public List<Path> files() { return List.of(PowerConfig.path()); }
  public void validate(Map<String, JsonElement> values) {
    // A future layout must remain recoverable even when unrelated preferences are saved.
    CreativeTabsData.read(PowerConfig.section("creativeTabs"));
    for (var entry : values.entrySet()) {
      if (!entry.getKey().equals(SETTING)) throw new IllegalArgumentException("Unknown Creative tabs setting");
      CreativeTabsData.read(entry.getValue().getAsJsonObject());
    }
  }
  public boolean previews(Setting setting) { return true; }
  public void preview(Map<String, JsonElement> values) throws Exception {
    validate(values);
    if (values.containsKey(SETTING)) registry("reload", new Class<?>[]{JsonObject.class}, values.get(SETTING).getAsJsonObject());
  }
  public void apply(Map<String, JsonElement> values) throws Exception {
    validate(values);
    if (values.containsKey(SETTING)) {
      JsonObject value = values.get(SETTING).getAsJsonObject();
      PowerConfig.put("creativeTabs", value); registry("reload", new Class<?>[]{JsonObject.class}, value);
    }
  }
  public static JsonObject defaultLayout() { return snapshot("defaultLayout"); }
  public static JsonObject nativeLayout() { return snapshot("nativeLayout"); }
  private static JsonObject snapshot(String method) {
    try { return (JsonObject) registry(method, new Class<?>[0]); }
    catch (Exception e) { throw new IllegalStateException("Creative item groups are unavailable", e); }
  }
  private static Object registry(String method, Class<?>[] types, Object... arguments) throws Exception {
    try { return Class.forName("local.luke.power.creative.inventory.registry.TabRegistry").getMethod(method, types).invoke(null, arguments); }
    catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof Exception cause) throw cause;
      throw e;
    }
  }
}
