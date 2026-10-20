package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.map.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

/** Native controls, persistence and render regressions for the 3.1.0 boundaries. Disposable clones only. */
final class Release310Checks {
  static final Path REPORT = Path.of("power-beta-data/reports/release-310");
  static Object invoke(Object object, String method, Class<?>[] types, Object... args) throws Exception {
    return Editor230Checks.invoke(object, method, types, args);
  }
  static Setting setting(PowerOptionsScreen panel, String id) {
    return panel.session().settings().stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow();
  }
  static void render(Minecraft mc, Screen screen, String name, int w, int h) throws Exception {
    screen.init(mc, w, h); SettingsSnapshot.render(mc, screen, w, h, REPORT.resolve(name + ".png"));
  }
  static void run(Minecraft mc, String mode) throws Exception {
    failures = 0; Files.createDirectories(REPORT);
    if (mode.startsWith("capture-")) { MapCapture310Checks.run(mc,mode); return; }
    if (mode.equals("chat")) { Chat310Checks.run(mc); return; }
    if (mode.equals("notes-write")) { local.luke.power.chat.ChatNotes.record("Power Beta 3.1 persistent note"); PowerConfig.flushPending(); log("310 notes written"); return; }
    if (mode.equals("notes-read")) { check(local.luke.power.chat.ChatNotes.history().contains("Power Beta 3.1 persistent note"), "note did not survive real client restart"); log("PASS 310 notes survived real client restart"); return; }
    check(mc.world != null && mc.player != null, "open disposable world first");
    var previous = mc.currentScreen; var originalMap = MapConfig.copy();
    PowerConfig.flushPending(); byte[] original = Files.readAllBytes(PowerConfig.path());
    boolean autoApply = MenuPreferences.current().autoApply;
    try {
      MenuPreferences.current().autoApply = false;
      var enabled = MapConfig.copy(); enabled.worldMap = enabled.minimap = true; MapConfig.preview(enabled); Maps.tick(mc);
      test("split keeps map ownership, scopes search and reuses appearance controls", () -> {
        var map = new WorldMapScreen(); mc.setScreen(map); map.init(mc, 960, 540);
        call(map, "toggleSettings"); var panel = (PowerOptionsScreen)field(map, "settingsPanel");
        check(mc.currentScreen == map, "sidecar replaced current map screen");
        check(panel.session() != null, "missing settings session");
        search(panel, "zoom");
        check(((List<?>)field(panel,"rows")).stream().anyMatch(row -> {
          try { return call(row,"setting") instanceof Setting s && s.id.equals("maps.zoom"); } catch(Exception failure) { throw new RuntimeException(failure); }
        }), "map search excluded HUD appearance");
        for (Object row : (List<?>)field(panel,"rows")) if (call(row,"setting") instanceof Setting s)
          check(s.id.startsWith("maps.") || s.group.equals("Maps"), "unrelated setting escaped scope: " + s.id);
        invoke(panel,"minimapAppearance",new Class<?>[]{String.class},"maps.zoom");
        check(field(panel,"mapView").equals("Minimap"), "appearance left split");
        Setting zoom = setting(panel,"maps.zoom"); zoom.value = new JsonPrimitive(1.75); panel.changed(zoom);
        check(MapConfig.current().zoom == 1.75, "map appearance did not preview");
        render(mc,map,"split-appearance",960,540);
        Maps.cache().visibleView(Maps.dimension(),Maps.layer(),new MapCoverage.Bounds(0,100,100,100,100));
        Object interest = field(Maps.cache(),"visibleView");
        MinimapBuffer.clear(); MapRenderer.beginFrame(1);
        Minimap.draw(mc,MapConfig.current(),960,540,false,true);
        check(field(Maps.cache(),"visibleView").equals(interest),"minimap replaced/cancelled the world-map read interest");
        Object miniSurface = MinimapHandoffChecks.staticValue(MinimapBuffer.class,"active");
        field(miniSurface,"failed",true); MapRenderer.beginFrame(1);
        Minimap.draw(mc,MapConfig.current(),960,540,false,true);
        check(field(Maps.cache(),"visibleView").equals(interest),"fallback minimap replaced the world-map read interest");
        field(miniSurface,"failed",false);
        invoke(panel,"linkedControls",new Class<?>[]{Setting.class},zoom);
        check(mc.currentScreen == map && field(panel,"mapView").equals("Controls"), "related key left split");
        check(!((String)field(panel,"highlightedId")).isEmpty(), "related key was not highlighted");
        call(panel,"back"); check(field(panel,"mapView").equals("Minimap"), "Back did not restore appearance");
        Setting color = setting(panel,"maps.destinationArrowColor");
        field(map,"panelPress",true);
        invoke(panel,"activate",new Class<?>[]{Setting.class,int.class},color,1);
        check(mc.currentScreen instanceof ColourScreen, "color picker did not open");
        ((local.luke.power.validation.mixin.ScreenInput)(Object)mc.currentScreen).power$key('\0',org.lwjgl.input.Keyboard.KEY_ESCAPE);
        check(mc.currentScreen == map, "color picker returned to detached Options");
        check(!(Boolean)field(map,"panelPress"),"nested editor return retained the old mouse press");
        map.init(mc,960,540); panel = (PowerOptionsScreen)field(map,"settingsPanel");
        check(setting(panel,"maps.zoom").value.getAsDouble() == 1.75, "nested return lost manual draft");
        render(mc,map,"split-return",960,540);
        render(mc,map,"split-narrow",320,240);
        render(mc,map,"split-wide-again",960,540);
        check(setting(panel,"maps.zoom").value.getAsDouble() == 1.75, "resize lost draft");
        panel.session().discard(); call(map,"closeSettings");
        check(mc.currentScreen == map && field(map,"settingsPanel")==null, "closing settings closed map");
        check(Arrays.equals(original,Files.readAllBytes(PowerConfig.path())),"preview wrote settings");
      });
      test("cave level prepares offscreen while keeping previous terrain", () -> MinimapHandoffChecks.run(mc));
      test("debug axes share native HUD and editor rendering", () -> {
        var options = new PowerOptionsScreen(null); mc.setScreen(options);
        Setting axes = setting(options,"interface.debugAxes"); axes.value = new JsonPrimitive(true);
        var hud = new HudEditorScreen(options,"interface.debugAxes"); mc.setScreen(hud);
        field(hud,"scenario",2); render(mc,hud,"debug-axes-preview",960,540);
        check(Arrays.equals(original,Files.readAllBytes(PowerConfig.path())),"axes preview wrote settings");
      });
      test("versioned creation defaults retain custom values and force review of unknown fields", () -> {
        JsonObject document = PowerConfig.document(); JsonObject templates = document.getAsJsonObject("settings").getAsJsonObject("gameRules");
        templates.remove("review"); templates.getAsJsonObject("survival").getAsJsonObject("values").addProperty("allow_sprinting",false);
        PowerConfig.write(document); check(RuleDefaults.review().required(),"old defaults not detected");
        var reviewParent = new PowerOptionsScreen(null); mc.setScreen(reviewParent);
        check(RuleDefaultsScreen.openIfNeeded(mc,reviewParent,()->{},()->mc.setScreen(reviewParent)),"review did not open");
        var screen = mc.currentScreen; field(screen,"expanded",true); render(mc,screen,"rule-review",640,360);
        invoke(screen,"commit",new Class<?>[]{boolean.class},false);
        check(!RuleDefaults.review().required(),"Keep did not acknowledge version");
        check(!local.luke.power.rules.RulePresets.survival().enabled(local.luke.power.rules.GameRule.ALLOW_SPRINTING),"Keep changed custom sprint rule");
        RuleDefaults.resetLatest(); check(local.luke.power.rules.RulePresets.survival().enabled(local.luke.power.rules.GameRule.ALLOW_SPRINTING),"Reset did not update defaults");
        check(local.luke.power.rules.RulePresets.cheats().enabled(local.luke.power.rules.GameRule.ALLOW_SPRINTING),"Cheats sprint missing");
      });
      test("retro entrance is limited to a fresh menu, including both cycle directions", () -> {
        Class<?> features = Class.forName("local.luke.power.title.TitleFeatures");
        Object config = features.getField("CONFIG").get(null);
        JsonObject old = (JsonObject)config.getClass().getMethod("snapshot").invoke(config);
        try {
          JsonObject title = old.deepCopy(); title.addProperty("style",1);
          title.getAsJsonObject("logo").getAsJsonObject("animation").addProperty("enabled",true);
          config.getClass().getMethod("replace",JsonObject.class).invoke(config,title);
          for (var f : features.getFields()) if (f.getType().getSimpleName().equals("ConfigKey")) f.getType().getMethod("load").invoke(f.get(null));
          var screen = new net.minecraft.client.gui.screen.TitleScreen(); mc.setScreen(screen);
          check(!(Boolean)call(screen,"power$animation"),"non-retro entry allowed animation");
          TitleSelection.cycle(-1);
          check(!(Boolean)call(screen,"power$animation"),"backward cycle animated retro");
          render(mc,screen,"cycled-retro",640,360);
          var fresh = new net.minecraft.client.gui.screen.TitleScreen(); mc.setScreen(fresh);
          check((Boolean)call(fresh,"power$animation"),"fresh retro entry did not animate");
          fresh.init(mc,640,360); check((Boolean)call(fresh,"power$animation"),"resize changed entry gate");
          Object style = features.getField("style").get(null);
          for (int i=0;i<24;i++) { TitleSelection.cycle(1); if (((Number)style.getClass().getMethod("get").invoke(style)).intValue()==0) break; }
          check(!(Boolean)call(fresh,"power$animation"),"forward cycle restarted entrance");
        } finally {
          PowerConfig.flushPending(); config.getClass().getMethod("replace",JsonObject.class).invoke(config,old);
          for (var f : features.getFields()) if (f.getType().getSimpleName().equals("ConfigKey")) f.getType().getMethod("load").invoke(f.get(null));
        }
      });
    } finally {
      MapConfig.preview(originalMap); MenuPreferences.current().autoApply=autoApply;
      PowerConfig.flushPending(); PowerConfig.write(JsonParser.parseString(new String(original,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
      mc.setScreen(previous);
    }
    log("RELEASE310 COMPLETE failures="+failures);
  }
}
