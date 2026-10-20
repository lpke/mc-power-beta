package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.map.*;
import local.luke.power.rules.*;
import local.luke.power.sprint.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.*;

/** Native 3.0.0 UI, draft, cave-column and sprint-transition checks in a disposable clone. */
public final class Release300Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-300");
  private Release300Checks() {}
  private static Background background;
  private static Float originalFramecap;
  private static final class Background {
    final MapSettings original, disabled;
    final Screen previous;
    final Waypoint destination;
    final byte[] preferences;
    final boolean paused;
    int ticks, stable;
    long submitted;
    String stage = "drain";
    Background(Minecraft mc) throws Exception {
      original = MapConfig.copy(); disabled = copy(original);
      disabled.minimap = disabled.worldMap = disabled.waypoints = disabled.worldWaypoints = disabled.deathpoints = false;
      previous = mc.currentScreen; destination = (Waypoint)getStatic(Maps.class, "destination");
      preferences = Files.readAllBytes(PowerConfig.path()); paused = mc.paused;
    }
  }
  private static MapSettings copy(MapSettings settings) {
    return Catalog.JSON.fromJson(Catalog.JSON.toJson(settings), MapSettings.class);
  }

  public static void run(Minecraft mc, String command) throws Exception {
    String[] args = command.trim().split("\\s+");
    String phase = args[0];
    failures = 0;
    if (phase.equals("background-stop")) { finishBackground(mc, new IllegalStateException("stopped by fixture command")); return; }
    check(background == null || phase.equals("state"), "wait for BACKGROUND COMPLETE or run release-300 background-stop");
    check(!Release300MapInteractions.pending() || phase.equals("state") || phase.equals("map-interactions"),
        "finish the armed map interaction check or run release-300 map-interactions stop");
    if (phase.equals("map-interactions")) { Release300MapInteractions.run(mc, args.length > 1 ? args[1] : "checks"); return; }
    if (phase.equals("framecap")) { framecap(args.length > 1 ? args[1] : ""); return; }
    if (phase.equals("pan-start")) {
      requireWorld(mc); check(mc.currentScreen instanceof WorldMapScreen, "open the World Map before pan-start");
      MapPanFrames.start(mc.currentScreen);
      log("300 PAN ARMED ring=1024; drag the map continuously, then run release-300 pan-check"); return;
    }
    if (phase.equals("pan-check")) { panCheck(mc); return; }
    if (phase.equals("background")) { beginBackground(mc); return; }
    if (phase.equals("state")) { state(mc); return; }
    if (phase.equals("open")) {
      requireWorld(mc);
      String target = args.length > 1 ? args[1] : "map";
      if (target.equals("map")) mc.setScreen(new WorldMapScreen());
      else if (target.equals("waypoints")) mc.setScreen(new WaypointsScreen(mc.currentScreen));
      else if (target.equals("hud")) {
        var options = new PowerOptionsScreen(mc.currentScreen);
        mc.setScreen(options);
        mc.setScreen(new HudEditorScreen(options, "maps.zoom"));
      } else throw new IllegalArgumentException("Use open map|hud|waypoints");
      state(mc);
      return;
    }
    if (phase.equals("capture")) {
      check(mc.currentScreen != null, "open a screen before capture");
      String name = args.length > 1 ? args[1] : "current";
      check(name.matches("[a-z0-9-]{1,64}"), "invalid capture name");
      capture(mc, mc.currentScreen, name, mc.currentScreen.width, mc.currentScreen.height);
      return;
    }
    if (!Set.of("ui", "navigation", "hud", "colors", "map", "refinement", "caves", "sprint").contains(phase))
      throw new IllegalArgumentException("Use release-300 ui|navigation|hud|colors|map|refinement|map-interactions [focus-arm|focus-check|paused|stop]|caves|sprint|background|background-stop|pan-start|pan-check|framecap 60|125|restore|open [map|hud|waypoints]|state|capture [name]");
    requireWorld(mc);
    Files.createDirectories(REPORT);
    var previous = mc.currentScreen;
    Waypoint previousDestination = (Waypoint)getStatic(Maps.class, "destination");
    var originalMap = MapConfig.copy();
    boolean auto = MenuPreferences.current().autoApply;
    PowerConfig.flushPending();
    byte[] preferences = Files.readAllBytes(PowerConfig.path());
    try {
      MenuPreferences.current().autoApply = false;
      switch (phase) {
        case "ui" -> { navigation(mc); keyDestinations(mc); hud(mc); colors(mc); map(mc); }
        case "navigation" -> { navigation(mc); keyDestinations(mc); }
        case "hud" -> hud(mc);
        case "colors" -> colors(mc);
        case "map" -> map(mc);
        case "refinement" -> Release300MapRefinement.check(mc);
        case "caves" -> caves(mc);
        case "sprint" -> sprint(mc);
      }
    } finally {
      MenuPreferences.current().autoApply = auto;
      MapConfig.preview(originalMap);
      Maps.destination(previousDestination);
      mc.setScreen(previous);
    }
    test("3.0.0 fixture leaves durable global preferences unchanged", () ->
        check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "fixture wrote global settings"));
    log("RELEASE300 " + phase.toUpperCase(Locale.ROOT) + " failures=" + failures);
  }

  private static void requireWorld(Minecraft mc) {
    check(mc.world != null && !mc.world.isRemote && mc.player != null, "open a disposable singleplayer world first");
  }
  private static Object get(Object target, String name) throws Exception { return Editor230Checks.get(target, name); }
  private static Object call(Object target, String name, Class<?>[] types, Object... args) throws Exception {
    return Editor230Checks.invoke(target, name, types, args);
  }
  private static Object call(Object target, String name) throws Exception { return call(target, name, new Class<?>[0]); }
  private static int number(Object target, String name) throws Exception { return ((Number)call(target, name)).intValue(); }
  private static void set(Object target, String name, Object value) throws Exception {
    for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) try {
      Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(target, value); return;
    } catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  private static void click(Screen screen, int x, int y, int button) { ((ScreenInput)(Object)screen).power$click(x, y, button); }
  private static void key(Screen screen, int key) { ((ScreenInput)(Object)screen).power$key((char)0, key); }
  private static void open(Minecraft mc, Screen screen, int width, int height) { mc.setScreen(screen); screen.init(mc, width, height); }
  private static SettingsDraft draft(HudEditorScreen screen) throws Exception { return (SettingsDraft)get(screen, "draft"); }
  private static void refresh(HudEditorScreen screen) throws Exception { call(screen, "afterEdit"); }
  private static void select(HudEditorScreen screen, String id) throws Exception { call(screen, "select", new Class<?>[]{String.class}, id); }
  private static Setting setting(PowerOptionsScreen screen, String id) { return find(screen.session(), id); }
  private static void footer(HudEditorScreen screen, String label) throws Exception {
    for (Object button : (List<?>)call(screen, "footer")) if (call(button, "label").equals(label)) {
      click(screen, number(button, "x") + number(button, "width") / 2, screen.height - 19, 0); return;
    }
    throw new AssertionError("Missing HUD footer " + label);
  }
  private static int expose(HudEditorScreen hud, Setting setting) throws Exception {
    call(hud, "revealControl", new Class<?>[]{Setting.class}, setting);
    int y = (int)call(hud, "controlY", new Class<?>[]{Setting.class}, setting);
    check(y >= number(hud, "controlsTop") && y + 18 <= number(hud, "controlsBottom"), "highlighted HUD row is outside viewport: " + setting.id);
    return y;
  }
  private static void capture(Minecraft mc, Screen screen, String name, int width, int height) throws Exception {
    Files.createDirectories(REPORT);
    int nativeScale = mc.options.guiScale;
    Class<?> modOptions = Class.forName("local.luke.power.controls.util.ModOptions");
    Field guiScale = modOptions.getField("guiScale"), realGuiScale = modOptions.getField("realGuiScale");
    float enhancedScale = guiScale.getFloat(null), realScale = realGuiScale.getFloat(null);
    mc.options.guiScale = 2;
    guiScale.setFloat(null, 2 / 8f); realGuiScale.setFloat(null, 2 / 8f);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      screen.init(mc, width, height);
      SettingsSnapshot.render(mc, screen, width, height, REPORT.resolve(name + ".png"));
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib();
      mc.options.guiScale = nativeScale;
      guiScale.setFloat(null, enhancedScale); realGuiScale.setFloat(null, realScale);
    }
    log("300 CAPTURE " + REPORT.resolve(name + ".png") + " physical=" + width * 2 + "x" + height * 2);
  }

  private static void navigation(Minecraft mc) throws Exception {
    test("HUD related controls visit Controls, reveal the exact key and return to the same unsaved draft", () -> {
      var options = new PowerOptionsScreen(null); mc.setScreen(options);
      call(options, "openSetting", new Class<?>[]{Setting.class}, setting(options, "native.guiScale"));
      String page = (String)get(options, "page");
      int history = ((Collection<?>)get(options, "history")).size();
      var hud = new HudEditorScreen(options, "maps.zoom"); mc.setScreen(hud);
      check(get(hud, "selected").equals("minimap") && get(hud, "highlightedSetting").equals("maps.zoom"), "HUD did not initially focus the requested setting");
      var zoom = draft(hud).get("maps.zoom");
      zoom.value = new JsonPrimitive(zoom.value.getAsDouble() == 1.75 ? 2 : 1.75); refresh(hud);
      var pending = draft(hud).snapshot();
      int y = expose(hud, zoom);
      click(hud, number(hud, "controlX") + 8, y + 8, 0);
      check(mc.currentScreen == options, "related control did not open the parent Options object");
      check(get(options, "page").equals("Controls"), "related control stayed outside Controls");
      check(get(options, "highlightedId").equals("keys.powerbeta.mapZoomIn"), "wrong key highlighted");
      Object row = ((List<?>)get(options, "rows")).stream().filter(value -> {
        try { return call(value, "setting") == setting(options, "keys.powerbeta.mapZoomIn"); }
        catch (Exception e) { throw new RuntimeException(e); }
      }).findFirst().orElseThrow();
      int rowY = number(options, "top") + number(row, "y") - ((Number)get(options, "scroll")).intValue();
      check(rowY >= number(options, "top") && rowY + number(row, "height") <= number(options, "bottom"), "related key was not revealed");
      capture(mc, options, "related-key", 640, 360);
      key(options, Keyboard.KEY_ESCAPE);
      check(mc.currentScreen == hud && draft(hud).snapshot().equals(pending), "Back replaced or accepted the HUD draft");
      check(get(options, "page").equals(page) && ((Collection<?>)get(options, "history")).size() == history, "Back lost the parent view/history");
      y = expose(hud, zoom); click(hud, number(hud, "controlX") + 8, y + 8, 0);
      Object linked = ((List<?>)get(options, "rows")).stream().filter(value -> {
        try { return call(value, "setting") == setting(options, "keys.powerbeta.mapZoomIn"); }
        catch (Exception e) { throw new RuntimeException(e); }
      }).findFirst().orElseThrow();
      int x = (int)call(options, "controlLeft", new Class<?>[]{linked.getClass()}, linked);
      int span = number(options, "span");
      y = number(options, "top") + number(linked, "y") - ((Number)get(options, "scroll")).intValue() + (span < 340 ? 16 : 3);
      click(options, x + 8, y + 8, 0);
      check(mc.currentScreen == hud && draft(hud).snapshot().equals(pending), "reverse Related setting created a second HUD draft");
      check(get(hud, "highlightedSetting").equals("maps.zoom"), "reverse Related setting missed exact focus");
      options.session().discard();
    });
  }

  private static void keyDestinations(Minecraft mc) throws Exception {
    test("binding Related arrows focus ground tuning, global info HUD and exact map appearance controls", () -> {
      for (var pair : Map.of("keys.powerbeta.sprint", "sprint.activation", "keys.powerbeta.infoHud", "infoHud.enabled",
          "keys.powerbeta.toggleMapRadar", "maps.radar", "keys.powerbeta.toggleMapGrid", "maps.chunkGrid").entrySet()) {
        var options = new PowerOptionsScreen(null); mc.setScreen(options);
        try {
          // Permission visibility uses the Options session's rule draft. No rule preview
          // or durable metadata edit is needed to test navigation to global tuning.
          setting(options, "rules.world.allow_sprinting").value = new JsonPrimitive(true);
          call(options, "layout");
          var binding = setting(options, pair.getKey());
          call(options, "openSetting", new Class<?>[]{Setting.class}, binding);
          Object row = null;
          for (Object candidate : (List<?>)get(options, "rows")) if (call(candidate, "setting") == binding) { row = candidate; break; }
          check(row != null, "binding row missing: " + pair.getKey());
          int x = (int)call(options, "controlLeft", new Class<?>[]{row.getClass()}, row);
          int y = number(options, "top") + number(row, "y") - ((Number)get(options, "scroll")).intValue()
              + (number(options, "span") < 340 ? 16 : 3);
          click(options, x + 8, y + 8, 0);
          if (HudEditorSettings.contains(setting(options, pair.getValue()))) {
            check(mc.currentScreen instanceof HudEditorScreen, "Related arrow missed HUD editor: " + pair.getKey());
            var editor = (HudEditorScreen)mc.currentScreen;
            check(get(editor, "highlightedSetting").equals(pair.getValue()), "Related arrow focused wrong HUD row: " + pair.getKey());
            expose(editor, draft(editor).get(pair.getValue()));
          } else {
            check(mc.currentScreen == options && get(options, "highlightedId").equals(pair.getValue()), "Related arrow focused wrong settings row: " + pair.getKey());
            check(get(options, "page").equals("Gameplay"), "ground sprint arrow left global tuning");
          }
        } finally { options.session().discard(); }
      }
    });
  }

  private static void hud(Minecraft mc) throws Exception {
    test("minimap live preview uses its isolated appearance draft, and Cancel/Done preserve the parent boundary", () -> {
      var before = Catalog.JSON.toJsonTree(MapConfig.current());
      var options = new PowerOptionsScreen(null); open(mc, options, 640, 360);
      try {
        var hud = new HudEditorScreen(options, "maps.zoom"); open(mc, hud, 640, 360);
        var values = draft(hud); var initial = values.snapshot(); values.checkpoint();
        values.get("maps.zoom").value = new JsonPrimitive(1.75);
        values.get("maps.circular").value = new JsonPrimitive(true);
        values.get("maps.opacity").value = new JsonPrimitive(.4);
        values.get("maps.playerIcon").value = new JsonPrimitive(2);
        values.get("maps.playerSize").value = new JsonPrimitive(150);
        values.get("maps.pathColor").value = new JsonPrimitive(0x224466);
        values.get("maps.pathOpacity").value = new JsonPrimitive(.35);
        refresh(hud);
        capture(mc, hud, "hud-minimap-draft", 640, 360);
        check(before.equals(Catalog.JSON.toJsonTree(MapConfig.current())), "HUD preview mutated live MapConfig");
        Object preview = get(hud, "preview"); var map = (MapSettings)get(preview, "map");
        check(map != MapConfig.current() && map.zoom == 1.75 && map.playerIcon == 2 && map.playerSize == 150 && map.pathColor == 0x224466, "live minimap renderer did not receive draft appearance");
        check(Maps.cache() != null && !((List<?>)call(preview, "elements")).isEmpty(), "loaded-world preview is missing its cache/elements");
        check((boolean)get(preview, "suppressToast") && (boolean)get(preview, "showToast"), "minimap failed to temporarily suppress toast");
        select(hud, "cluster.1"); check((boolean)get(preview, "suppressToast"), "Info cluster 2 did not suppress toast");
        select(hud, "cluster.2"); check(!(boolean)get(preview, "suppressToast") && (boolean)get(preview, "showToast"), "leaving special selections lost previous toast state");
        set(preview, "showToast", false); select(hud, "minimap"); select(hud, "layout");
        check(!(boolean)get(preview, "showToast"), "leaving minimap restored an unwanted toast");
        footer(hud, "Undo"); check(values.snapshot().equals(initial), "Undo did not restore the complete minimap appearance edit");
        select(hud, "minimap"); values.get("maps.zoom").value = new JsonPrimitive(2); refresh(hud);
        footer(hud, "Cancel"); check((boolean)call(get(hud, "exit"), "active"), "dirty Cancel missed confirmation");
        click(hud, hud.width / 2, hud.height / 2 + 18, 0);
        check(mc.currentScreen == options && before.equals(Catalog.JSON.toJsonTree(MapConfig.current())), "Discard changed live minimap preferences");
        check(setting(options, "maps.zoom").value.equals(initial.get("maps.zoom")), "Discard changed the Options draft");
        var accepted = new HudEditorScreen(options, "maps.zoom"); open(mc, accepted, 640, 360);
        draft(accepted).get("maps.zoom").value = new JsonPrimitive(1.5); refresh(accepted); footer(accepted, "Done");
        check(mc.currentScreen == options && setting(options, "maps.zoom").value.getAsDouble() == 1.5, "Done failed to accept minimap into Options");
      } finally { options.session().discard(); }
      check(before.equals(Catalog.JSON.toJsonTree(MapConfig.current())), "Options Discard failed to restore the live map");
    });
  }

  private static void colors(Minecraft mc) throws Exception {
    test("color picker Cancel isolates RGB/opacity and Done validates every supported stored color type", () -> {
      var options = new PowerOptionsScreen(null); open(mc, options, 640, 360);
      try {
        for (String id : List.of("maps.pathColor", "maps.playerColor", "activeTweaks.textColor", "infoHud.clusters.0.textColor", "worldedit.color", "tweaks.flexible.overlayColor", "lightOverlay.lowColor")) {
          Setting color = setting(options, id), opacity = ColorSettings.opacity(color, options.session().settings());
          JsonElement old = color.value.deepCopy(), alpha = opacity == null ? null : opacity.value.deepCopy();
          var picker = new ColourScreen(options, color, opacity, () -> {}); open(mc, picker, 640, 360);
          check(((Number)get(picker, "tab")).intValue() == 0, "Color wheel is not the default tab");
          ((TextInput)get(picker, "hex")).setText(ColorSettings.rgb(color) == 0x224466 ? "#335577" : "#224466");
          call(picker, "parse");
          if (opacity != null) ((Setting)get(picker, "localOpacity")).parse(opacity.max <= 1 ? ".35" : "35");
          key(picker, Keyboard.KEY_ESCAPE);
          check(mc.currentScreen == picker && (boolean)call(get(picker, "exit"), "active"), "dirty color Escape missed confirmation");
          click(picker, picker.width / 2, picker.height / 2 + 18, 0);
          check(mc.currentScreen == options && color.value.equals(old) && (opacity == null || opacity.value.equals(alpha)), "nested Discard changed " + id);
          picker = new ColourScreen(options, color, opacity, () -> {}); open(mc, picker, 640, 360);
          ((TextInput)get(picker, "hex")).setText("#224466");
          if (opacity != null) {
            int x = number(picker, "controlX"), y = (int)call(picker, "sliderY", new Class<?>[]{int.class}, 3);
            click(picker, x + 10, y + 8, 1);
            Object inline = get(picker, "inline");
            ((TextInput)get(inline, "input")).setText("35"); key(picker, Keyboard.KEY_RETURN);
            check(opacity.value.equals(alpha), "exact opacity published before Done for " + id);
          }
          key(picker, Keyboard.KEY_RETURN);
          check(mc.currentScreen == options && ColorSettings.rgb(color) == 0x224466, "Done did not convert RGB for " + id);
          if (opacity != null) check(Math.abs(ColorSettings.opacityPercent(opacity) - 35) < 1e-6, "Done opacity units wrong for " + id);
          if (id.equals("maps.pathColor")) capture(mc, new ColourScreen(options, color, opacity, () -> {}), "color-with-opacity", 640, 360);
          color.value = old; if (opacity != null) opacity.value = alpha;
        }
      } finally { options.session().discard(); }
    });
  }

  private static void map(Minecraft mc) throws Exception {
    test("world map and Waypoints retain usable controls and bounded 16:9 content at native and ultrawide resolutions", () -> {
      check(Maps.cache() != null && Maps.waypoints() != null && Maps.waypoints().ready(), "wait for maps and waypoints to finish loading");
      for (int[] pixels : new int[][]{{1280,720}, {5120,2160}}) {
        if (Display.getWidth() < pixels[0] || Display.getHeight() < pixels[1]) {
          log("SKIP 300 layout " + pixels[0] + "x" + pixels[1] + ": resize the disposable display and repeat map"); continue;
        }
        int w = pixels[0] / 2, h = pixels[1] / 2;
        var world = new WorldMapScreen(); open(mc, world, w, h);
        capture(mc, world, "world-map-" + pixels[0], w, h);
        var view = (MapMath.View)get(world, "view"); double px = mc.player.x, pz = mc.player.z;
        click(world, w / 2, h / 2, 1);
        check((boolean)get(world, "context"), "right click did not open map context");
        var actions = (List<?>)call(world, "contextActions");
        check(actions.containsAll(List.of("Copy coordinates", "Set destination")) && !actions.contains("Center here"), "world map context actions disagree");
        check(actions.contains("Teleport") == Maps.canTeleport(mc, (Waypoint)get(world, "target")), "world map context ignores teleport permission");
        key(world, Keyboard.KEY_ESCAPE); check(!(boolean)get(world, "context") && mc.currentScreen == world, "Escape closed map instead of context");
        Waypoint destination = MapKeys.newWaypoint(); destination.x += 3;
        set(world, "target", destination); call(world, "contextAction", new Class<?>[]{String.class}, "Set destination");
        check(getStatic(Maps.class, "destination") instanceof Waypoint point && point.id.equals("temporary-destination"), "destination action did not publish a marker");
        Maps.destination(null); open(mc, world, w, h);
        check(mc.player.x == px && mc.player.z == pz, "map context moved the player");
        view.pan(17, -9); key(world, Keyboard.KEY_HOME);
        check(Math.abs(view.x - Maps.x()) < 1e-6 && Math.abs(view.z - Maps.z()) < 1e-6, "Home did not center map");
        var list = new WaypointsScreen(world); open(mc, list, w, h);
        capture(mc, list, "waypoints-" + pixels[0], w, h);
        int left = number(list, "left"), span = number(list, "span");
        check(left >= 8 && left + span <= w - 8 && span <= h * 16 / 9 - 16, "Waypoints escaped centered 16:9 content");
        Object layout = call(list, "layout");
        click(list, left + 8, 55, 0); check((boolean)get(list, "selectingSet"), "set dropdown did not open");
        key(list, Keyboard.KEY_ESCAPE); check(!(boolean)get(list, "selectingSet"), "set dropdown did not own Escape");
        click(list, number(layout, "createX") + 8, 55, 0); check((boolean)get(list, "creatingSet"), "New set button did not open its dialog");
        int sort = (int)get(list, "sort"); click(list, number(layout, "sortX") + 8, 35, 0);
        check((int)get(list, "sort") == sort, "modal click reached the list behind New set");
        key(list, Keyboard.KEY_ESCAPE); check(!(boolean)get(list, "creatingSet"), "New set did not own Escape");
        click(list, number(layout, "sortX") + 8, 35, 1); check((int)get(list, "sort") == Math.floorMod(sort - 1, 3), "right click did not reverse sort");
      }
    });
    test("World Map screenshot action writes a readable native-size PNG inside the clone screenshots directory", () -> {
      var world = new WorldMapScreen(); open(mc, world, 640, 360);
      set(world, "screenshot", true); capture(mc, world, "map-screenshot-action", 640, 360);
      Field field = MapScreenshot.class.getDeclaredField("writing"); field.setAccessible(true);
      var writing = (CompletableFuture<?>)field.get(null); check(writing != null, "Screenshot action did not queue a PNG");
      Path file = (Path)writing.get(20, TimeUnit.SECONDS);
      Path screenshots = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("screenshots").toAbsolutePath().normalize();
      check(file.toAbsolutePath().normalize().startsWith(screenshots), "map screenshot escaped the instance");
      var image = ImageIO.read(file.toFile()); check(image != null && image.getWidth() == 1280 && image.getHeight() == 720, "map screenshot dimensions/encoding wrong");
      check(MapScreenshot.status().startsWith("Screenshot saved:"), "screenshot status did not confirm durable output");
      log("300 MAP SCREENSHOT " + file);
      mc.setScreen(null); open(mc, new WorldMapScreen(), 640, 360);
      check(MapScreenshot.status().isEmpty(), "reopening the map inside the four-second notice window retained stale screenshot status");
    });
  }

  private static void caves(Minecraft mc) throws Exception {
    MapTreeCoverChecks.run(mc);
    MapFullBrightChecks.run(mc);
    test("native cave samples honor exact height windows, skip thick ceilings and replace missing floors with surveyed void", () -> {
      int x = ((int)Math.floor(mc.player.x) >> 4) * 16 + 4, z = ((int)Math.floor(mc.player.z) >> 4) * 16 + 4;
      var chunk = MapSampler.loaded(mc.world, x >> 4, z >> 4); check(chunk != null, "fixture column is not loaded");
      int[] ids = new int[36], metadata = new int[36];
      for (int y = 80; y <= 115; y++) {
        check(mc.world.method_1777(x, y, z) == null, "choose another loaded fixture column; this one contains saved block-entity data");
        ids[y - 80] = mc.world.getBlockId(x, y, z); metadata[y - 80] = mc.world.method_1778(x, y, z);
      }
      try {
        for (int y = 80; y <= 115; y++) mc.world.method_154(x, y, z, 0, 0);
        mc.world.method_154(x, 90, z, 1, 0);
        for (int y = 100; y <= 105; y++) mc.world.method_154(x, y, z, 1, 0);
        var settings = MapConfig.copy(); settings.caveMode = 1; settings.caveAbove = 2; settings.caveBelow = 8;
        var low = MapSampler.window(settings, 96.9); var high = MapSampler.window(settings, 99.1);
        check(low.reference() == 96 && high.reference() == 99 && low.layer() == high.layer(), "exact camera height was rounded to an eight-block layer");
        check(!low.context().equals(high.context()), "same-layer cave heights reused one namespace");
        var sampler = new MapSampler(new MapPalette());
        Method sample = MapSampler.class.getDeclaredMethod("sample", net.minecraft.world.World.class, net.minecraft.class_43.class, MapTile.class, int.class, int.class, CaveWindow.class);
        sample.setAccessible(true);
        var first = new MapTile(MapMath.key(Maps.dimension(), low.layer(), 0, x, z, low.context()));
        var second = new MapTile(MapMath.key(Maps.dimension(), high.layer(), 0, x, z, high.context()));
        sample.invoke(sampler, mc.world, chunk, first, x, z, low);
        sample.invoke(sampler, mc.world, chunk, second, x, z, high);
        int index = MapMath.index(x, z);
        check(first.known(index) && first.height(index) == 90, "native floor inside lower search window was missed");
        check(second.known(index) && second.block(index) == 0, "higher window searched below its lower bound or left stale data");
        mc.world.method_154(x, 94, z, 1, 0); sample.invoke(sampler, mc.world, chunk, second, x, z, high);
        check(second.height(index) == 94 && first.height(index) == 90, "thick ceiling or adjacent height namespace overwrote the wrong floor");
        settings.caveMode = 2; settings.caveTop = 87;
        check(MapSampler.window(settings, 113).reference() == 87, "manual cave reference followed the camera");
        log("300 CAVES low=" + low.context() + " high=" + high.context() + " floors=" + first.height(index) + "/" + second.height(index));
      } finally { for (int y = 80; y <= 115; y++) mc.world.method_154(x, y, z, ids[y - 80], metadata[y - 80]); }
    });
  }

  private static Object staticCall(Class<?> type, String method, Class<?>[] signature, Object... args) throws Exception { return type.getMethod(method, signature).invoke(null, args); }
  private static void sprint(Minecraft mc) throws Exception {
    var saved = new NbtCompound(); mc.player.write(saved);
    var original = SprintConfig.copy(); var rules = AccessRules.copy(mc);
    var config = Class.forName("local.luke.power.creative.config.Config");
    var creative = staticCall(config, "current", new Class<?>[0]); creative = call(creative, "copy");
    var runtime = Class.forName("local.luke.power.creative.ClientRuntime");
    var flight = Class.forName("local.luke.power.creative.FlightController");
    var flying = mc.player.getClass().getMethod("creative_setFlying", boolean.class);
    var input = mc.player.field_161; var fixture = new net.minecraft.class_41(); mc.player.field_161 = fixture;
    Field field = Keyboard.class.getDeclaredField("keyDownBuffer"); field.setAccessible(true); var keys = (ByteBuffer)field.get(null);
    byte[] oldKeys = new byte[keys.capacity()]; for (int i = 0; i < oldKeys.length; i++) oldKeys[i] = keys.get(i);
    var bindingModifiers = Bindings.modifiers();
    var bindings = (Object[])getStatic(Bindings.class, "registered");
    Map<KeyBinding, Integer> keyCodes = new LinkedHashMap<>();
    boolean paused = mc.paused, focused = mc.field_2778;
    var previous = mc.currentScreen;
    try {
      mc.setScreen(null); FreecamPreviewChecks.active(false); mc.field_2807 = mc.player; mc.paused = false; mc.field_2778 = true;
      check(Display.isActive(), "focus the disposable client before sprint checks");
      for (Object object : bindings) if (object instanceof Binding binding) {
        if (binding.power$id().equals("powerbeta.sprint") || binding.power$id().equals("power_creative.sprint") || binding.power$id().equals("key.forward")) {
          KeyBinding nativeBinding = (KeyBinding)object;
          keyCodes.put(nativeBinding, nativeBinding.code);
          nativeBinding.code = binding.power$id().equals("key.forward") ? 17 : 29;
        }
      }
      Bindings.configure(Map.of()); for (int i = 0; i < oldKeys.length; i++) keys.put(i, (byte)0);
      var allowed = rules.copy(); allowed.set(GameRule.ALLOW_SPRINTING, true); allowed.set(GameRule.ALLOW_FLIGHT, true);
      allowed.set(GameRule.ALLOW_CREATIVE_MODE, true); AccessRules.restore(mc, allowed);
      var ground = SprintConfig.copy(); ground.activation = 1; ground.speed = 100; ground.stamina = false; ground.doubleTap = false; ground.noSprintInCombat = false; SprintConfig.preview(ground);
      var settings = call(creative, "copy"); set(settings, "flight", true); set(settings, "sprintToggle", true); set(settings, "sprintMultiplier", 200); set(settings, "landStopsFlight", false);
      staticCall(config, "preview", new Class<?>[]{settings.getClass()}, settings);
      CreativeVehicleChecks.mode(mc, "CREATIVE"); staticCall(runtime, "tick", new Class<?>[]{Minecraft.class}, mc);
      test("real Creative takeoff inherits held ground sprint at 100% speed before ground cancellation", () -> {
        flying.invoke(mc.player, false); staticCall(flight, "reset", new Class<?>[0]); SprintController.reset();
        fixture.field_2533 = 1; keys.put(17, (byte)1); keys.put(29, (byte)0); SprintController.movement(mc.player);
        keys.put(29, (byte)1); SprintController.movement(mc.player);
        check(SprintView.active(SprintView.Kind.GROUND) && SprintView.ratio(SprintView.Kind.GROUND) == 1, "100% ground sprint was not active");
        flying.invoke(mc.player, true); SprintController.tick(mc); staticCall(flight, "tick", new Class<?>[]{Minecraft.class}, mc);
        check(!SprintView.active(SprintView.Kind.GROUND) && SprintView.active(SprintView.Kind.CREATIVE), "takeoff lost active sprint or retained ground sprint");
        check(SprintView.ratio(SprintView.Kind.CREATIVE) == 2, "flight multiplier was not independent");
        double x = mc.player.x, z = mc.player.z; mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
        check((Boolean)staticCall(flight, "travel", new Class<?>[]{net.minecraft.entity.player.PlayerEntity.class}, mc.player), "native flight did not own travel");
        check(Math.hypot(mc.player.x - x, mc.player.z - z) > 0, "inherited sprint flight did not displace the player");
        keys.put(29, (byte)0); staticCall(flight, "tick", new Class<?>[]{Minecraft.class}, mc); check(SprintView.active(SprintView.Kind.CREATIVE), "release canceled inherited Toggle");
        keys.put(17, (byte)0); staticCall(flight, "tick", new Class<?>[]{Minecraft.class}, mc); check(!SprintView.active(SprintView.Kind.CREATIVE), "movement stop retained Toggle");
      });
      test("Survival /fly captures released ground Toggle and denied flight cannot retain pending inheritance", () -> {
        CreativeVehicleChecks.mode(mc, "SURVIVAL"); mc.player.getClass().getMethod("power_flightOverride", int.class).invoke(mc.player, 0);
        ground.activation = 2; SprintConfig.preview(ground); SprintController.reset(); keys.put(17, (byte)1); keys.put(29, (byte)0); fixture.field_2533 = 1; SprintController.movement(mc.player);
        keys.put(29, (byte)1); SprintController.movement(mc.player); keys.put(29, (byte)0); SprintController.movement(mc.player);
        check(SprintView.active(SprintView.Kind.GROUND), "released ground Toggle did not remain active");
        var commands = Class.forName("local.luke.power.creative.command.CreativeCommands"); staticCall(commands, "execute", new Class<?>[]{String.class}, "/fly");
        SprintController.tick(mc); staticCall(flight, "tick", new Class<?>[]{Minecraft.class}, mc);
        check(((PlayerMode)mc.player).power$flying() && SprintView.active(SprintView.Kind.CREATIVE), "/fly reset erased ground handoff");
        mc.setScreen(new net.minecraft.client.gui.screen.ChatScreen()); staticCall(flight, "tick", new Class<?>[]{Minecraft.class}, mc);
        check(!SprintView.active(SprintView.Kind.CREATIVE), "menu retained flight sprint"); mc.setScreen(null);
        flying.invoke(mc.player, false); AccessRules.set(mc, GameRule.ALLOW_FLIGHT, false); SprintView.publish(SprintView.Kind.GROUND, true, 1);
        flying.invoke(mc.player, true); staticCall(flight, "tick", new Class<?>[]{Minecraft.class}, mc);
        check(!((PlayerMode)mc.player).power$flying() && !SprintView.active(SprintView.Kind.CREATIVE), "denied takeoff created flight sprint");
      });
    } finally {
      for (int i = 0; i < oldKeys.length; i++) keys.put(i, oldKeys[i]);
      for (var entry : keyCodes.entrySet()) entry.getKey().code = entry.getValue();
      Bindings.configure(bindingModifiers); SprintConfig.preview(original); AccessRules.restore(mc, rules);
      staticCall(config, "preview", new Class<?>[]{creative.getClass()}, creative);
      mc.player.read(saved); mc.player.field_161 = input; mc.paused = paused; mc.field_2778 = focused;
      staticCall(flight, "reset", new Class<?>[0]); SprintController.reset(); mc.setScreen(previous);
    }
  }
  private static void framecap(String cap) throws Exception {
    var options = Class.forName("local.luke.power.controls.util.ModOptions");
    Field field = options.getField("fpsLimit");
    byte[] preferences = Files.readAllBytes(PowerConfig.path());
    if (cap.equals("restore")) {
      check(originalFramecap != null, "no fixture frame cap is saved");
      field.setFloat(null, originalFramecap); originalFramecap = null;
    } else {
      check(cap.equals("60") || cap.equals("125"), "Use release-300 framecap 60|125|restore");
      if (originalFramecap == null) originalFramecap = field.getFloat(null);
      field.setFloat(null, (Integer.parseInt(cap) - 5) / 1000f);
    }
    check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "frame-cap fixture wrote preferences");
    log("300 FRAMECAP " + options.getMethod("getFpsLimitValue").invoke(null) + " memory-only vendor="
        + GL11.glGetString(GL11.GL_VENDOR) + " renderer=" + GL11.glGetString(GL11.GL_RENDERER));
  }

  private static void panCheck(Minecraft mc) throws Exception {
    check(MapPanFrames.recording(), "run pan-start before pan-check");
    var result = MapPanFrames.finish();
    var report = new LinkedHashMap<String, Object>();
    report.put("physicalWidth", mc.displayWidth); report.put("physicalHeight", mc.displayHeight);
    report.put("guiScale", Class.forName("local.luke.power.controls.util.ModOptions").getMethod("getGuiScale").invoke(null));
    report.put("frameCap", Class.forName("local.luke.power.controls.util.ModOptions").getMethod("getFpsLimitValue").invoke(null));
    report.put("vendor", GL11.glGetString(GL11.GL_VENDOR)); report.put("renderer", GL11.glGetString(GL11.GL_RENDERER));
    report.put("elapsedFrames", result.elapsedFrames()); report.put("storedFrames", result.storedFrames());
    report.put("elapsedSeconds", result.elapsedSeconds()); report.put("sampledSeconds", result.sampledSeconds());
    report.put("renderFps", result.renderFps()); report.put("nativeTickHz", result.nativeTicksPerSecond());
    report.put("dragFrames", result.dragFrames()); report.put("dragSeconds", result.dragSeconds());
    report.put("dragRenderFps", result.dragFps()); report.put("distinctViewChanges", result.distinctViewChanges());
    report.put("viewChangeHz", result.viewChangesPerSecond());
    report.put("frameMedianMs", result.frameMedianMs()); report.put("frameP95Ms", result.frameP95Ms());
    report.put("viewChangeMedianMs", result.changeMedianMs()); report.put("viewChangeP95Ms", result.changeP95Ms());
    report.put("cadence", result.dragFps() <= 22 ? "full map rendering at or below 22 FPS"
        : result.viewChangesPerSecond() <= 22 ? "view updates at tick cadence despite faster rendering" : "view updates above tick cadence");
    Files.createDirectories(REPORT);
    String name = "pan-" + report.get("frameCap") + "-" + System.currentTimeMillis();
    Files.writeString(REPORT.resolve(name + ".csv"), MapPanFrames.csv());
    Files.writeString(REPORT.resolve(name + ".json"), Catalog.JSON.toJson(report));
    log("300 PAN " + Catalog.JSON.toJson(report));
    test("physical map drag updates the view faster than the 20 Hz game tick", () -> {
      check(result.dragSeconds() >= .5 && result.distinctViewChanges() >= 12, "hold and continuously drag the map for at least half a second before pan-check");
      check(result.viewChangesPerSecond() > 20, "pan rate=" + result.viewChangesPerSecond() + "Hz render=" + result.dragFps()
          + "FPS nativeTicks=" + result.nativeTicksPerSecond() + "Hz; " + report.get("cadence"));
    });
    log("RELEASE300 PAN failures=" + failures + " samples=" + REPORT.resolve(name + ".csv"));
  }

  private static void beginBackground(Minecraft mc) throws Exception {
    requireWorld(mc); PowerConfig.flushPending(); background = new Background(mc);
    try {
      mc.setScreen(null); mc.paused = false;
      var enabled = copy(background.original);
      enabled.minimap = enabled.worldMap = true; enabled.radar = enabled.footsteps = true;
      MapConfig.preview(enabled); Maps.tick(mc);
      var options = new PowerOptionsScreen(null); open(mc, options, 640, 360);
      var hud = new HudEditorScreen(options, "maps.zoom"); open(mc, hud, 640, 360);
      capture(mc, hud, "background-minimap-enabled", 640, 360);
      check(getStatic(MinimapBuffer.class, "active") != null, "enabled minimap did not allocate its native buffer");
      mc.setScreen(null); enabled.minimap = false; MapConfig.preview(enabled); Maps.tick(mc);
      check(getStatic(MinimapBuffer.class, "active") == null, "turning minimap off retained its native buffer");
      check(Maps.sampler() != null && Maps.cache() != null, "turning minimap off stopped the independently enabled World Map");
      log("PASS 300 minimap buffer clears independently while World Map remains active");
      var world = new WorldMapScreen(); open(mc, world, 640, 360);
      capture(mc, world, "background-world-map-enabled", 640, 360);
      Object buffer = get(world, "buffer");
      check(get(buffer, "active") != null || get(buffer, "pending") != null, "enabled World Map did not allocate its native surface");
      enabled.minimap = true; enabled.worldMap = false; MapConfig.preview(enabled); Maps.tick(mc);
      world.render(-1, -1, 0);
      check(mc.currentScreen == null && get(buffer, "active") == null && get(buffer, "pending") == null && get(buffer, "spare") == null,
          "turning World Map off retained its screen/native surfaces");
      check(Maps.sampler() != null && Maps.cache() != null, "turning World Map off stopped the independently enabled minimap");
      log("PASS 300 World Map surfaces clear independently while minimap remains active");
      MapConfig.preview(background.disabled); Maps.tick(mc); noTerrain();
      log("300 BACKGROUND ARMED; wait for RELEASE300 BACKGROUND COMPLETE, COMMAND DONE only confirms dispatch");
    } catch (Throwable failure) { finishBackground(mc, failure); }
  }

  private static void noTerrain() throws Exception {
    check(Maps.sampler() == null, "both maps off retained a terrain sampler");
    check(getStatic(Maps.class, "palette") == null, "both maps off retained the texture palette");
    check(Maps.radar().isEmpty() && Maps.path().isEmpty(), "both maps off retained radar/path work");
    check(Maps.cache() == null || Maps.cache().retiring(), "disabled terrain cache still accepts new view work");
  }
  private static boolean workerIdle() throws Exception {
    var worker = (java.util.concurrent.ThreadPoolExecutor)getStatic(MapIO.class, "WORKER");
    return MapIO.queued() == 0 && worker.getActiveCount() == 0;
  }
  private static boolean storeIdle() { return Maps.waypoints() == null || !Maps.waypoints().needsMaintenance(); }

  /** One bounded poll per native tick. Cache retirement and PNG writes never block this observer. */
  static void observe(Minecraft mc) {
    MapPanFrames.tick();
    Release300MapInteractions.observe(mc);
    Background task = background;
    if (task == null) return;
    try {
      check(++task.ticks <= 1800, "background fixture timed out after 1800 native ticks at stage " + task.stage);
      requireWorld(mc); Maps.tick(mc); noTerrain();
      switch (task.stage) {
        case "drain" -> {
          if (Maps.cache() != null || !storeIdle() || !workerIdle()) return;
          // The disabled cache and store are already durable and idle. Reset their world lease
          // before proving that a fresh all-off context never starts a read/decode/write.
          Maps.leave(); MapConfig.preview(task.disabled); Maps.tick(mc); noTerrain();
          check(Maps.cache() == null && Maps.waypoints() == null, "fresh disabled context opened map storage");
          task.submitted = MapIO.submitted(); task.stable = 0; task.stage = "fresh";
        }
        case "fresh" -> {
          check(Maps.cache() == null && Maps.waypoints() == null, "all-off fresh ticks created terrain or waypoint storage");
          check(MapIO.submitted() == task.submitted, "all-off fresh ticks submitted hidden map work");
          if (++task.stable < 40) return;
          log("PASS 300 fresh all-off context submitted zero tasks across 40 native ticks; accepted=" + task.submitted);
          var settings = copy(task.disabled); settings.worldWaypoints = true;
          MapConfig.preview(settings); Maps.tick(mc);
          task.stage = "world-waypoints"; task.stable = 0;
        }
        case "world-waypoints", "deathpoints" -> {
          check(Maps.cache() == null && Maps.waypoints() != null, "independent waypoint feature started terrain or failed to open its store");
          if (!Maps.waypoints().ready() || !storeIdle() || !workerIdle()) return;
          if (task.stable == 0) task.submitted = MapIO.submitted();
          check(MapIO.submitted() == task.submitted, "idle waypoint-only feature submitted terrain work");
          if (++task.stable < 20) return;
          log("PASS 300 " + task.stage + " retain only waypoint storage across 20 idle ticks");
          if (task.stage.equals("world-waypoints")) {
            var settings = copy(task.disabled); settings.deathpoints = true;
            MapConfig.preview(settings); Maps.tick(mc); task.stage = "deathpoints"; task.stable = 0;
          } else {
            MapConfig.preview(task.disabled); Maps.tick(mc); Maps.leave(); Maps.tick(mc);
            check(Maps.waypoints() == null, "fresh all-off context opened waypoints before an explicit action");
            mc.setScreen(new WaypointsScreen(null)); Maps.tick(mc);
            check(Maps.waypoints() != null, "explicit Waypoints screen failed to open its independent store");
            task.stage = "explicit-waypoints"; task.stable = 0;
          }
        }
        case "explicit-waypoints" -> {
          check(Maps.cache() == null && Maps.waypoints() != null, "explicit Waypoints screen started terrain");
          if (!Maps.waypoints().ready() || !storeIdle() || !workerIdle()) return;
          mc.setScreen(null); Maps.tick(mc); task.submitted = MapIO.submitted(); task.stable = 0; task.stage = "closed";
        }
        case "closed" -> {
          check(Maps.cache() == null && MapIO.submitted() == task.submitted, "closing explicit Waypoints left background work running");
          if (++task.stable < 40) return;
          log("PASS 300 explicit Waypoints loads without terrain and closing it submits zero tasks across 40 native ticks");
          finishBackground(mc, null);
        }
        default -> throw new AssertionError("unknown background stage " + task.stage);
      }
    } catch (Throwable failure) {
      try { finishBackground(mc, failure); }
      catch (Exception reporting) { reporting.printStackTrace(); }
    }
  }

  private static void finishBackground(Minecraft mc, Throwable failure) throws Exception {
    Background task = background;
    if (task == null) { log("300 BACKGROUND is not running"); return; }
    background = null;
    try {
      check(Arrays.equals(task.preferences, Files.readAllBytes(PowerConfig.path())), "background fixture wrote durable global preferences");
    } catch (Throwable changed) { if (failure == null) failure = changed; else failure.addSuppressed(changed); }
    try { MapConfig.preview(task.original); Maps.destination(task.destination); mc.paused = task.paused; mc.setScreen(task.previous); }
    catch (Throwable restore) { if (failure == null) failure = restore; else failure.addSuppressed(restore); }
    if (failure != null) { log("FAIL 300 background " + task.stage + " " + failure); failure.printStackTrace(); }
    log("RELEASE300 BACKGROUND COMPLETE failures=" + (failure == null ? 0 : 1) + " nativeTicks=" + task.ticks);
  }

  private static Object getStatic(Class<?> type, String field) throws Exception { Field value = type.getDeclaredField(field); value.setAccessible(true); return value.get(null); }

  private static void state(Minecraft mc) throws Exception {
    var state = new LinkedHashMap<String, Object>();
    state.put("screen", mc.currentScreen == null ? "HUD" : mc.currentScreen.getClass().getSimpleName());
    state.put("panRecording", MapPanFrames.recording());
    state.put("mapInteractions", Release300MapInteractions.state());
    state.put("glVendor", GL11.glGetString(GL11.GL_VENDOR)); state.put("glRenderer", GL11.glGetString(GL11.GL_RENDERER));
    state.put("display", Display.getWidth() + "x" + Display.getHeight());
    state.put("world", mc.world == null ? null : mc.world.dimension.id);
    state.put("backgroundPhase", background == null ? "idle" : background.stage);
    state.put("backgroundTicks", background == null ? 0 : background.ticks);
    state.put("workerSubmitted", MapIO.submitted());
    state.put("paletteAllocated", getStatic(Maps.class, "palette") != null);
    state.put("cacheTiles", Maps.cache() == null ? 0 : Maps.cache().size());
    state.put("cachePending", Maps.cache() == null ? 0 : Maps.cache().pending());
    state.put("cacheGeneration", Maps.cache() == null ? 0 : Maps.cache().generation());
    state.put("caveContext", Maps.cache() == null ? "" : Maps.cache().viewContext());
    state.put("columnsLastTick", Maps.sampler() == null ? 0 : Maps.sampler().columnsLastTick);
    state.put("sampleNanosLastTick", Maps.sampler() == null ? 0 : Maps.sampler().nanosLastTick);
    state.put("workerQueued", MapIO.queued()); state.put("radar", Maps.radar().size()); state.put("path", Maps.path().size());
    state.put("layer", Maps.layer()); state.put("error", Maps.error());
    for (var kind : SprintView.Kind.values()) state.put("sprint" + kind, Map.of("active", SprintView.active(kind), "ratio", SprintView.ratio(kind)));
    if (mc.currentScreen instanceof WorldMapScreen world) {
      var view = (MapMath.View)get(world, "view"); state.put("view", Map.of("x", view.x, "z", view.z, "scale", view.scale));
      state.put("contextOpen", get(world, "context")); state.put("scaleDragging", get(world, "scaleDragging"));
    }
    log("300 STATE " + Catalog.JSON.toJson(state));
  }
}
