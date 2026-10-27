package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.map.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Real map sidebar presses and title controls in a muted disposable client. */
final class Release464MapLogoChecks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-464/map-logo");
  private static final int[][] SIZES = {{854, 480}, {320, 240}};
  private Release464MapLogoChecks() {}

  static void run(Minecraft mc, String action) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote, "open a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    check(action.equals("check") || action.equals("capture"), "use release-464-map-logo check|capture");
    failures = 0;
    Screen previous = mc.currentScreen;
    var runtime = MapConfig.copy();
    PowerConfig.flushPending();
    var stored = PowerConfig.section("maps").deepCopy();
    boolean automatic = MenuPreferences.current().autoApply, grabbed = Mouse.isGrabbed();
    boolean heads = MapKeys.minimapHeadsToggled();
    int guiScale = mc.options.guiScale;
    var customScale = Class.forName("local.luke.power.controls.util.ModOptions").getField("guiScale");
    var realScale = Class.forName("local.luke.power.controls.util.ModOptions").getField("realGuiScale");
    float originalScale = customScale.getFloat(null), originalRealScale = realScale.getFloat(null);
    int matrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      Files.createDirectories(REPORT); Mouse.setGrabbed(false);
      mc.options.guiScale = 2; customScale.setFloat(null, .25f); realScale.setFloat(null, .25f);
      var style = MapConfig.copy(); style.worldMap = style.minimap = true; MapConfig.preview(style);
      if (action.equals("check")) {
        map(mc, previous);
        for (int[] size : SIZES) logo(mc, previous, size, false);
      } else {
        MenuPreferences.current().autoApply = false;
        for (int[] size : SIZES) {
          logo(mc, previous, size, true);
          var map = new WorldMapScreen(previous); open(mc, map, SIZES[0]);
          call(map, "toggleSettings"); map.init(mc, size[0], size[1]);
          call(get(map, "settingsPanel"), "minimapAppearance", new Class<?>[]{String.class}, "");
          shot(mc, map, "map-settings", size);
          ((PowerOptionsScreen)get(map, "settingsPanel")).session().discard();
        }
      }
    } finally {
      MenuPreferences.current().autoApply = automatic;
      mc.options.guiScale = guiScale; customScale.setFloat(null, originalScale); realScale.setFloat(null, originalRealScale);
      if (MapKeys.minimapHeadsToggled() != heads) MapToggle.HEADS.toggleMinimap();
      PowerConfig.saveDeferred("maps", stored); PowerConfig.flushPending(); MapConfig.preview(runtime);
      mc.setScreen(previous); Mouse.setGrabbed(grabbed);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(matrix);
    }
    check(PowerConfig.section("maps").equals(stored), "fixture did not restore map preferences");
    log("RELEASE464_MAP_LOGO failures=" + failures + " captures=" + REPORT.toAbsolutePath());
    check(failures == 0, "release-464-map-logo failures=" + failures);
  }

  private static void map(Minecraft mc, Screen previous) throws Exception {
    for (boolean automatic : new boolean[]{false, true}) for (boolean borrowed : new boolean[]{false, true}) {
      MenuPreferences.current().autoApply = automatic;
      var owner = new PowerOptionsScreen(previous); open(mc, owner, SIZES[0]);
      WorldMapScreen map;
      if (borrowed) {
        check(WorldMapScreen.openMinimapSettings(mc, owner, true), "map refused borrowed session");
        map = (WorldMapScreen)mc.currentScreen; map.init(mc, 854, 480);
      } else {
        map = new WorldMapScreen(previous); open(mc, map, SIZES[0]); call(map, "toggleSettings");
      }
      var panel = (PowerOptionsScreen)get(map, "settingsPanel");
      check(!borrowed || panel.session() == owner.session(), "borrowed sidebar lost its owner");
      map.render(-1, -1, 0);
      var hit = (MinimapGeometry.Box)get(map, "minimapHit");
      check(hit != null, "sidebar minimap did not render");
      int x = (int)(hit.x() + hit.width() / 2), y = (int)(hit.y() + hit.height() / 2);
      String view = (String)get(panel, "mapView");
      var beforeLeft = MapConfig.copy();
      press(map, x, y, 0);
      check(mc.currentScreen == map && get(map, "settingsPanel") == panel && get(panel, "mapView").equals(view),
          "left minimap press reopened or navigated the map sidebar");
      check(Catalog.JSON.toJsonTree(beforeLeft).equals(Catalog.JSON.toJsonTree(MapConfig.current())), "left press changed settings");
      for (MapToggle action : MapToggle.ALL.subList(0, setting(panel, "maps.middleClickAction").choices.size())) {
        Setting middle = setting(panel, "maps.middleClickAction");
        middle.value = new JsonPrimitive(action.ordinal()); middle.accept();
        var style = MapConfig.copy(); style.middleClickAction = action.ordinal(); MapConfig.preview(style);
        var before = MapConfig.copy();
        boolean heads = MapKeys.minimapHeadsToggled();
        var durable = PowerConfig.section("maps").deepCopy();
        var expected = MapConfig.copy();
        if (action != MapToggle.HEADS) MapToggle.apply(expected, action.changes(expected));
        press(map, x, y, 2);
        check(mc.currentScreen == map && get(map, "settingsPanel") == panel, "middle action replaced the map");
        check(Catalog.JSON.toJsonTree(expected).equals(Catalog.JSON.toJsonTree(MapConfig.current())), "wrong action: " + action);
        if (action == MapToggle.HEADS) {
          check(MapKeys.minimapHeadsToggled() != heads, "HEADS did not use the minimap latch");
          check(MapConfig.current().worldMapRadarIcons == before.worldMapRadarIcons, "HEADS changed world-map preference");
          check(PowerConfig.section("maps").equals(durable), "HEADS wrote settings");
        } else {
          for (String field : action.fields) {
            Setting changed = setting(panel, "maps." + field);
            check(changed.changed() != automatic, "action escaped session ownership: " + action);
          }
          if (automatic) {
            PowerConfig.flushPending();
            for (String field : action.fields)
              check(PowerConfig.section("maps").get(field).equals(setting(panel, "maps." + field).value), "automatic action did not persist " + field);
          } else {
            check(PowerConfig.section("maps").equals(durable), "manual action wrote before Apply");
            panel.session().discard();
            for (String field : action.fields)
              check(Catalog.JSON.toJsonTree(MapConfig.current()).getAsJsonObject().get(field)
                  .equals(Catalog.JSON.toJsonTree(before).getAsJsonObject().get(field)), "Cancel lost " + field);
          }
        }
      }
      // An open context menu owns a click over the sidebar, which only dismisses it.
      set(map, "target", MapKeys.newWaypoint()); set(map, "context", true);
      set(map, "contextX", 600); set(map, "contextY", 80);
      var snapshot = new SettingsDraft(panel.session().settings(), value -> true).snapshot();
      press(map, 20, 60, 0);
      check(!(boolean)get(map, "context"), "sidebar click did not dismiss the context owner");
      check(snapshot.equals(new SettingsDraft(panel.session().settings(), value -> true).snapshot()), "context click leaked into sidebar");
      if (!automatic) {
        Setting middle = setting(panel, "maps.middleClickAction");
        middle.value = new JsonPrimitive(MapToggle.ITEMS.ordinal()); middle.accept();
        var style = MapConfig.copy(); style.middleClickAction = MapToggle.ITEMS.ordinal(); MapConfig.preview(style);
        press(map, x, y, 2);
        boolean accepted = setting(panel, "maps.items").value.getAsBoolean();
        int applyX = (int)call(panel, "actionX", new Class<?>[]{int.class}, 1);
        panel.layerPress(applyX + 8, panel.height - 20, 0);
        check(!setting(panel, "maps.items").changed() && PowerConfig.section("maps").get("items").getAsBoolean() == accepted,
            "manual Apply did not accept the minimap action");
      }
      panel.session().discard(); owner.session().discard(); mc.setScreen(previous);
      log("RELEASE464_MAP_LOGO map automatic=" + automatic + " borrowed=" + borrowed + " all actions passed");
    }
  }

  private static void logo(Minecraft mc, Screen previous, int[] size, boolean capture) throws Exception {
    MenuPreferences.current().autoApply = false;
    var owner = new PowerOptionsScreen(previous); open(mc, owner, size);
    var parent = new SettingsDraft(owner.session().settings(), value -> value.id.startsWith("logo.")).snapshot();
    var screen = new TitleLetteringScreen(owner); open(mc, screen, size);
    var draft = (SettingsDraft)get(screen, "draft");
    JsonArray lines = new JsonArray();
    for (int row = 0; row < 12; row++) lines.add(row == 0 ? " ".repeat(79) + "*" : row == 11 ? "*" + " ".repeat(79) : " ".repeat(80));
    draft.get("logo.logo.lines").value = lines;
    JsonObject palette = new JsonObject(); palette.addProperty("*", 1); draft.get("logo.blocks.map").value = palette;
    call(screen, "afterEdit");
    var original = draft.snapshot();
    Object dimensions = get(screen, "dimensions");
    for (String axis : List.of("columns", "rows")) {
      var control = (Setting)get(dimensions, axis);
      call(screen, "revealControl", new Class<?>[]{Setting.class}, control);
      int y = (int)call(screen, "controlY", new Class<?>[]{Setting.class}, control);
      int x = (int)call(screen, "controlX", new Class<?>[]{Setting.class}, control);
      check(y >= n(screen, "controlsTop") && y + 18 <= n(screen, "controlsBottom"),
          "fixture control is clipped: " + axis + " y=" + y + " at " + size[0]);
      press(screen, x + 4, y + 8, 0);
      check(draft.snapshot().equals(original), "slider changed painted draft: " + axis);
      check(!(String.valueOf(get(screen, "error"))).isEmpty(), "slider press missed its validation: " + axis);
      check(control.value.getAsInt() == (axis.equals("columns") ? 80 : 12), "slider control did not recover");
      press(screen, x + 6, y + 8, 1);
      Object inline = get(screen, "inline");
      check((boolean)call(inline, "active"), "right click did not open exact entry");
      for (String value : List.of("1", "0", "2.5", "81")) {
        ((TextInput)get(inline, "input")).setText(value);
        ((ScreenInput)(Object)screen).power$key('\0', Keyboard.KEY_RETURN);
        check(draft.snapshot().equals(original) && (boolean)call(inline, "active"), "exact entry clipped " + axis + ": " + value);
      }
      ((ScreenInput)(Object)screen).power$key('\0', Keyboard.KEY_ESCAPE);
      int resetX = n(screen, "controlX") + n(screen, "controlWidth") - 20;
      press(screen, resetX + 6, y + 8, 0);
      check(draft.snapshot().equals(original), "individual reset clipped " + axis);
    }
    check(parent.equals(new SettingsDraft(owner.session().settings(), value -> value.id.startsWith("logo.")).snapshot()), "logo draft leaked into Options");
    if (capture) shot(mc, screen, "title-resize-protected", size);
    else {
      JsonArray small = new JsonArray(); small.add(" *  "); small.add("    ");
      draft.get("logo.logo.lines").value = small; call(screen, "afterEdit");
      var columns = (Setting)get(dimensions, "columns"); draft.checkpoint();
      columns.parse("6"); call(screen, "valueChanged", new Class<?>[]{Setting.class}, columns);
      check(TitleTiles.width(draft.get("logo.logo.lines").value.getAsJsonArray()) == 6, "valid expansion failed");
      call(screen, "undo");
      check(draft.get("logo.logo.lines").value.equals(small) && columns.value.getAsInt() == 4, "Undo lost painted dimensions");
    }
    owner.session().discard(); mc.setScreen(previous);
    log("RELEASE464_MAP_LOGO logo " + size[0] + " controls passed");
  }

  private static void open(Minecraft mc, Screen screen, int[] size) { mc.setScreen(screen); screen.init(mc, size[0], size[1]); }
  private static void press(Screen screen, int x, int y, int button) {
    ((ScreenInput)(Object)screen).power$click(x, y, button);
    ((ScreenInput)(Object)screen).power$release(x, y, button);
  }
  private static void shot(Minecraft mc, Screen screen, String label, int[] size) throws Exception {
    SettingsSnapshot.render(mc, screen, size[0], size[1], REPORT.resolve(label + "-" + size[0] + ".png"), -1, -1);
  }
  private static Setting setting(PowerOptionsScreen screen, String id) { return screen.session().settings().stream().filter(value -> value.id.equals(id)).findFirst().orElseThrow(); }
  private static int n(Object owner, String name) throws Exception { return ((Number)call(owner, name)).intValue(); }
  private static Field field(Class<?> type, String name) throws Exception {
    for (; type != null; type = type.getSuperclass()) try { var f = type.getDeclaredField(name); f.setAccessible(true); return f; }
    catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  private static Object get(Object owner, String name) throws Exception { return field(owner.getClass(), name).get(owner); }
  private static void set(Object owner, String name, Object value) throws Exception { field(owner.getClass(), name).set(owner, value); }
  private static Object call(Object owner, String name) throws Exception { return call(owner, name, new Class<?>[0]); }
  private static Object call(Object owner, String name, Class<?>[] types, Object... args) throws Exception {
    for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) try {
      var method = type.getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(owner, args);
    } catch (NoSuchMethodException ignored) {}
    throw new NoSuchMethodException(name);
  }
}
