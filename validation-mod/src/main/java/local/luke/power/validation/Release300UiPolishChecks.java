package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.*;
import local.luke.power.audio.AudioConfig;
import local.luke.power.audio.AudioSettings;
import local.luke.power.config.*;
import local.luke.power.map.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import local.luke.power.validation.mixin.ButtonBounds;
import net.minecraft.class_525;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

/** Disposable-world checks for picker modal ownership and the actual relocated pause cluster. */
public final class Release300UiPolishChecks {
  private Release300UiPolishChecks() {}
  public static void check(Minecraft mc) throws Exception {
    Validation.check(mc.world != null && mc.player != null && !mc.world.isRemote, "open a disposable singleplayer world first");
    Screen previous = mc.currentScreen;
    var originalMap = MapConfig.copy();
    var originalAudio = AudioConfig.copy();
    PowerConfig.flushPending(); byte[] preferences = Files.readAllBytes(PowerConfig.path());
    var previews = new LinkedHashMap<String, Object>();
    for (String name : List.of("pauseToolsPreview", "pauseMapPreview", "pauseWaypointsPreview")) previews.put(name, field(MenuPreferences.class, name).get(null));
    Object photoConfig = photoConfig(); Field photoField = photoConfig.getClass().getField("enablePhotoModeButton");
    Object originalPhoto = photoField.get(photoConfig);
    try {
      test("color wheel is the default left tab and dirty Escape/Cancel preserve RGB, opacity and swatches", () -> colors(mc));
      test("pause tool buttons track Back to game after music relocation and respect map availability", () -> pause(mc, photoConfig, photoField));
      test("retired map shading and per-mode flight FOV settings cannot become visible rows", () -> {
        var options = new PowerOptionsScreen(null); mc.setScreen(options);
        try {
          for (String id : List.of("maps.heightShading", "maps.slopeShading", "maps.caveLighting", "sprint.creativeFov", "sprint.spectatorFov")) {
            var setting = options.session().settings().stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow();
            Validation.check(HudEditorSettings.internal(setting) && !HudEditorSettings.contains(setting), "retired row remains editable: " + id);
            for (Object row : (List<?>)get(options, "rows"))
              Validation.check(call(row, "setting") != setting, "retired Options row remains visible: " + id);
          }
        } finally { options.session().discard(); }
      });
    } finally {
      for (var value : previews.entrySet()) field(MenuPreferences.class, value.getKey()).set(null, value.getValue());
      photoField.set(photoConfig, originalPhoto); MapConfig.preview(originalMap); AudioConfig.preview(originalAudio); mc.setScreen(previous);
    }
    test("UI polish fixture preserves durable preference bytes", () -> Validation.check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "UI polish fixture wrote preferences"));
    log("300 UI POLISH complete");
  }
  private static void colors(Minecraft mc) throws Exception {
    var parent = new PowerOptionsScreen(mc.currentScreen); mc.setScreen(parent);
    int[] accepted = {0};
    Setting rgb = value("maps.destinationArrowColor", Setting.Kind.INTEGER, new JsonPrimitive(0x224466), 0, 0xffffff);
    Setting alpha = value("maps.destinationArrowOpacity", Setting.Kind.DECIMAL, new JsonPrimitive(.8), 0, 1);
    Setting swatches = value("interface.colours", Setting.Kind.LIST, new JsonArray(), 0, 16);
    try {
      var picker = new ColourScreen(parent, rgb, alpha, () -> accepted[0]++, swatches); mc.setScreen(picker);
      Validation.check(number(picker, "tab") == 0 && number(picker, "previewY") >= 34, "picker default tab/title gap regressed");
      Object wheel = call(picker, "wheelBounds");
      click(picker, ((Number)call(wheel, "x")).intValue(), ((Number)call(wheel, "y")).intValue());
      Validation.check(number(picker, "color") != rgb.value.getAsInt(), "native wheel click did not edit the private color");
      key(picker, Keyboard.KEY_ESCAPE); Object prompt = get(picker, "exit");
      Validation.check((boolean)call(prompt, "active") && mc.currentScreen == picker, "dirty Escape bypassed the prompt");
      click(picker, number(picker, "left") + number(picker, "tabWidth") + 8, number(picker, "tabsY") + 8);
      Validation.check(number(picker, "tab") == 0, "modal click reached a tab behind the prompt");
      prompt(picker, 2); Validation.check(mc.currentScreen == picker && accepted[0] == 0, "Keep editing accepted or closed the picker");
      click(picker, picker.width / 2 + 52, picker.height - 18);
      Validation.check((boolean)call(prompt, "active"), "dirty Cancel bypassed the prompt");
      prompt(picker, 1);
      Validation.check(mc.currentScreen == parent && rgb.value.getAsInt() == 0x224466 && alpha.value.getAsDouble() == .8 && swatches.value.getAsJsonArray().size() == 0 && accepted[0] == 0, "Discard leaked the color draft");

      picker = new ColourScreen(parent, rgb, alpha, () -> accepted[0]++, swatches); mc.setScreen(picker);
      wheel = call(picker, "wheelBounds"); click(picker, ((Number)call(wheel, "x")).intValue(), ((Number)call(wheel, "y")).intValue());
      click(picker, number(picker, "left") + number(picker, "tabWidth") + 8, number(picker, "tabsY") + 8);
      Validation.check(number(picker, "tab") == 1, "right tab is not RGBA");
      int alphaY = (int)call(picker, "sliderY", new Class<?>[]{int.class}, 3);
      set(picker, "scroll", Math.max(0, alphaY - number(picker, "bodyTop") - 2d));
      alphaY = (int)call(picker, "sliderY", new Class<?>[]{int.class}, 3);
      click(picker, number(picker, "controlX") + number(picker, "controlWidth") / 4, alphaY + 8);
      Validation.check(((Setting)get(picker, "localOpacity")).value.getAsDouble() != .8, "RGBA opacity slider did not edit its local draft");
      int swatchY = number(picker, "saveY");
      set(picker, "scroll", ((Number)get(picker, "scroll")).doubleValue() + swatchY - number(picker, "bodyTop") - 2);
      click(picker, number(picker, "left") + 30, number(picker, "saveY") + 8);
      Validation.check(((List<?>)get(picker, "colors")).size() == 1, "Save color did not stage a swatch");
      key(picker, Keyboard.KEY_ESCAPE); prompt(picker, 0);
      Validation.check(mc.currentScreen == parent && accepted[0] == 1 && rgb.value.getAsInt() != 0x224466 && alpha.value.getAsDouble() != .8 && swatches.value.getAsJsonArray().size() == 1, "Save did not accept RGB, opacity and swatches together");
    } finally { parent.session().discard(); }
  }
  private static void pause(Minecraft mc, Object photo, Field photoField) throws Exception {
    photoField.set(photo, true); MenuPreferences.previewPauseTools(0);
    MenuPreferences.previewPauseMap(true); MenuPreferences.previewPauseWaypoints(true);
    var settings = MapConfig.copy(); settings.worldMap = true; MapConfig.preview(settings);
    var audio = AudioConfig.copy(); audio.menuControls = false; AudioConfig.preview(audio);
    var pause = new class_525(); mc.setScreen(pause); pause.render(-1, -1, 1);
    Object tools = get(pause, "power$tools"); var bounds = (PauseToolsLayout.Box)get(tools, "bounds");
    Validation.check(bounds != null && bounds.width() == 68, "pause menu did not include all three item buttons");
    if (pause.height >= 220) Validation.check(bounds.y() >= 52, "default pause tools moved above the title despite available space");
    var buttons = ((ScreenInput)(Object)pause).power$buttons();
    var back = buttons.stream().filter(button -> button.id == 4).findFirst().orElseThrow();
    Validation.check(bounds.x() + bounds.width() / 2 == back.x + ((ButtonBounds)(Object)back).power$width() / 2 && bounds.y() + bounds.height() <= back.y, "default cluster is not centered above Back to game");
    for (var button : buttons) if (button.visible)
      Validation.check(!bounds.overlaps(new PauseToolsLayout.Box(button.x, button.y, ((ButtonBounds)(Object)button).power$width(), ((ButtonBounds)(Object)button).power$height())), "pause cluster overlaps a native button");
    Validation.check(buttons.stream().noneMatch(button -> button.id == 20), "legacy photo button remained beside Options");
    audio.menuControls = true; audio.menuControlsScrub = false;
    for (var position : List.of(AudioSettings.MenuControlsPosition.MENU_TOP, AudioSettings.MenuControlsPosition.MENU_BOTTOM, AudioSettings.MenuControlsPosition.TOP_CENTER)) {
      audio.menuControlsPosition = position; AudioConfig.preview(audio); pause.render(-1, -1, 1);
      bounds = (PauseToolsLayout.Box)get(get(pause, "power$tools"), "bounds");
      var music = ((PauseMenuMusic)get(pause, "power$music")).bounds();
      Validation.check(music != null && !bounds.overlaps(new PauseToolsLayout.Box(music.x(), music.y(), music.width(), music.height())), "pause tools overlap music at " + position);
      for (var button : buttons) if (button.visible)
        Validation.check(!bounds.overlaps(new PauseToolsLayout.Box(button.x, button.y, ((ButtonBounds)(Object)button).power$width(), ((ButtonBounds)(Object)button).power$height())), "music relocation moved a native button beneath the tools");
    }
    click(pause, bounds.x() + 24 + 10, bounds.y() + 10);
    Validation.check(mc.currentScreen instanceof WorldMapScreen, "map item button missed its native screen");
    mc.setScreen(pause); pause.render(-1, -1, 1);
    tools = get(pause, "power$tools"); bounds = (PauseToolsLayout.Box)get(tools, "bounds");
    click(pause, bounds.x() + 48 + 10, bounds.y() + 10);
    Validation.check(mc.currentScreen instanceof WaypointsScreen, "diamond button missed Waypoints");
    key(mc.currentScreen, Keyboard.KEY_ESCAPE); Validation.check(mc.currentScreen == pause, "Waypoints did not return to the same pause menu");
    settings.worldMap = false; MapConfig.preview(settings); pause.render(-1, -1, 1);
    Validation.check(((List<?>)get(get(pause, "power$tools"), "included")).size() == 2, "disabled World Map retained its pause button");
    photoField.set(photo, false); MenuPreferences.previewPauseMap(false); MenuPreferences.previewPauseWaypoints(false); pause.render(-1, -1, 1);
    Validation.check(get(get(pause, "power$tools"), "bounds") == null, "all-hidden pause tools retained a cluster");
  }
  private static Setting value(String id, Setting.Kind kind, JsonElement current, double min, double max) {
    return new Setting(id, "test", "Interface", "", id, "", kind, current, current, min, max, kind == Setting.Kind.DECIMAL ? .01 : 1, List.of(), false);
  }
  private static Object photoConfig() throws Exception {
    Object ui = Class.forName("local.luke.power.controls.ControlFeatures").getField("USER_INTERFACE_CONFIG").get(null);
    return ui.getClass().getField("photoModeConfig").get(ui);
  }
  private static Field field(Class<?> owner, String name) throws Exception {
    for (Class<?> type = owner; type != null; type = type.getSuperclass()) try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; } catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  private static Object get(Object object, String name) throws Exception { return field(object.getClass(), name).get(object); }
  private static void set(Object object, String name, Object value) throws Exception { field(object.getClass(), name).set(object, value); }
  private static int number(Object object, String name) throws Exception {
    try { return ((Number)get(object, name)).intValue(); } catch (NoSuchFieldException e) { return ((Number)call(object, name)).intValue(); }
  }
  private static Object call(Object object, String name) throws Exception { return call(object, name, new Class<?>[0]); }
  private static Object call(Object object, String name, Class<?>[] types, Object... args) throws Exception {
    for (Class<?> owner = object.getClass(); owner != null; owner = owner.getSuperclass()) try {
      Method method = owner.getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(object, args);
    } catch (NoSuchMethodException ignored) {}
    throw new NoSuchMethodException(name);
  }
  private static void click(Screen screen, int x, int y) { ((ScreenInput)(Object)screen).power$click(x, y, 0); ((ScreenInput)(Object)screen).power$release(x, y, 0); }
  private static void key(Screen screen, int key) { ((ScreenInput)(Object)screen).power$key((char)0, key); }
  private static void prompt(ColourScreen screen, int index) throws Exception {
    Object prompt = get(screen, "exit");
    int x = (int)call(prompt, "buttonX", new Class<?>[]{UiScreen.class, int.class}, screen, index);
    int span = (int)call(prompt, "buttonWidth", new Class<?>[]{UiScreen.class}, screen);
    click(screen, x + span / 2, screen.height / 2 + 18);
  }
}
