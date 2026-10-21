package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.HudSettings400Ui.*;
import com.google.gson.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.death.DeathItems;
import local.luke.power.inventory.tools.*;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;

/** Restores all fixture preferences and user-data bytes through PowerConfig. Never writes world rules. */
final class HudSettings400State implements AutoCloseable {
  final Minecraft mc;
  final Path report;
  final List<PowerOptionsScreen> opened = new ArrayList<>();
  private final JsonObject document;
  private final Map<String, JsonElement> original;
  private final String inventory;
  private final RuleSet rules;
  private final Screen screen;
  private final boolean focus, paused, grabbed;
  private final Field baseField, menuField, dataField;
  private final Object base, menu, bars;
  private SavedCreativeHotbarsData sentinel;

  HudSettings400State(Minecraft mc) throws Exception {
    this.mc = mc; PowerConfig.flushPending(); document = PowerConfig.document(); original = values(SettingsRegistry.open(mc));
    inventory = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
    rules = WorldRules.of(mc.world.method_262()).copy(); screen = mc.currentScreen;
    focus = mc.field_2778; paused = mc.paused; grabbed = Mouse.isGrabbed();
    baseField = field(DefaultBases.class, "current"); base = baseField.get(null);
    menuField = field(MenuPreferences.class, "current"); menu = menuField.get(null);
    dataField = field(SavedCreativeHotbars.class, "data"); bars = dataField.get(null);
    report = FabricLoader.getInstance().getGameDir().resolve("power-beta-data/reports/settings-400").resolve(UUID.randomUUID().toString());
    Files.createDirectories(report);
  }
  void seedBars() throws Exception {
    sentinel = SavedCreativeHotbarsData.empty();
    for (int row = 0; row < 9; row++) {
      String[] items = new String[9];
      for (int slot = 0; slot < 9; slot++) items[slot] = DeathItems.encode(DeathChecks.tagged(35, row + slot + 1, slot,
          "settings-400:" + row + ":" + slot));
      sentinel = sentinel.with(row, items);
    }
    JsonObject saved = sentinel.json(); saved.addProperty("fixture-note", "preserve unknown saved-bar metadata");
    sentinel = SavedCreativeHotbarsData.read(saved);
    PowerConfig.save("savedCreativeHotbars", sentinel.json()); dataField.set(null, sentinel); checkBars();
  }
  void context(DefaultBases.Base value, boolean automatic) throws Exception {
    baseField.set(null, value);
    MenuPreferences.Values copy = Catalog.JSON.fromJson(Catalog.JSON.toJson(menu), MenuPreferences.Values.class);
    copy.autoApply = automatic; menuField.set(null, copy);
  }
  PowerOptionsScreen options(DefaultBases.Base value, boolean automatic) throws Exception {
    context(value, automatic); var options = new PowerOptionsScreen(screen); opened.add(options); open(mc, options, 640, 360); return options;
  }
  void discard(PowerOptionsScreen options) throws Exception { options.session().discard(); }
  void checkBars() throws Exception {
    check(PowerConfig.section("savedCreativeHotbars").equals(sentinel.json()), "native settings action changed saved hotbar user data");
    for (int row = 0; row < 9; row++) check(Arrays.equals(sentinel.bar(row), SavedCreativeHotbars.snapshot(row)), "native settings action changed cached saved hotbar " + row);
  }
  void unchangedItemsAndWorld() throws Exception {
    check(inventory.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer))), "settings touched a native inventory slot");
    check(rules.equals(WorldRules.of(mc.world.method_262())), "global settings edited loaded-world rules");
    JsonObject nativeBefore = document.getAsJsonObject("settings").getAsJsonObject("native");
    JsonObject nativeAfter = PowerConfig.document().getAsJsonObject("settings").getAsJsonObject("native");
    for (String key : List.of("fov", "guiScale", "gui_scale", "fpsLimit", "framerate_limit"))
      check(Objects.equals(nativeBefore == null ? null : nativeBefore.get(key), nativeAfter == null ? null : nativeAfter.get(key)), "settings reset changed saved display preference " + key);
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "settings fixture changed muted audio volumes");
    checkBars();
  }
  @Override public void close() throws Exception {
    try {
      for (int i = opened.size() - 1; i >= 0; i--) opened.get(i).session().discard();
      baseField.set(null, base); context((DefaultBases.Base)base, false);
      ConfigSession restore = SettingsRegistry.open(mc);
      for (Setting setting : restore.settings()) if (original.containsKey(setting.id) && scope(setting)) setting.value = original.get(setting.id).deepCopy();
      restore.save(FabricLoader.getInstance().getGameDir());
    } finally {
      try { PowerConfig.write(document); }
      finally {
        baseField.set(null, base); menuField.set(null, menu); dataField.set(null, bars);
        mc.setScreen(screen); mc.field_2778 = focus; mc.paused = paused; Mouse.setGrabbed(grabbed);
      }
    }
    check(PowerConfig.document().equals(document), "fixture did not restore the complete settings document");
    check(inventory.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)))
        && rules.equals(WorldRules.of(mc.world.method_262())), "fixture teardown changed native items or world rules");
  }
  private static boolean scope(Setting setting) {
    return HudEditorSettings.contains(setting) || setting.id.startsWith("offhand.") || setting.id.startsWith("recipeBook.")
        || setting.id.startsWith("heldLight.") || setting.id.startsWith("inventoryTools.") || setting.id.startsWith("worldTools.")
        || setting.id.startsWith("keys.powerbeta.");
  }
}
