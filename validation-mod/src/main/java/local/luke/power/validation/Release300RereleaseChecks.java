package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.map.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Native rerelease regressions. Settings, waypoint snapshots and camera state stay visit-local. */
final class Release300RereleaseChecks {
  private static final int[][] SIZES = {{320, 180}, {640, 360}};
  private static final List<String> MINIMAP = List.of("directionTextSize", "belowTextSize", "minimapArrowSize",
      "minimapWaypointArrows", "minimapDestinationArrow", "minimapDeathpointArrows",
      "destinationArrowColor", "destinationArrowOpacity");
  private Release300RereleaseChecks() {}

  static void run(Minecraft mc, String action) throws Exception {
    action = action.isBlank() ? "all" : action.trim();
    check(Set.of("all", "ui", "hud", "colors", "camera", "defaults").contains(action),
        "Use release-300-rerelease [all|ui|hud|colors|camera|defaults]");
    check(mc.world != null && !mc.world.isRemote && mc.player != null, "open a disposable singleplayer world first");
    check(Display.isActive(), "focus the disposable client before rerelease input checks");
    failures = 0;
    Screen previous = mc.currentScreen;
    MapSettings original = MapConfig.copy();
    boolean auto = MenuPreferences.current().autoApply;
    byte[] preferences = Files.readAllBytes(PowerConfig.path());
    try {
      MenuPreferences.current().autoApply = false;
      if (Set.of("all", "ui").contains(action)) tabs(mc);
      if (Set.of("all", "ui", "hud").contains(action)) { hud(mc); minimap(mc); }
      if (Set.of("all", "ui", "colors").contains(action)) colors(mc);
      if (Set.of("all", "defaults").contains(action)) defaults(mc);
      if (Set.of("all", "camera").contains(action)) camera(mc);
    } finally {
      MenuPreferences.current().autoApply = auto;
      MapConfig.preview(original);
      mc.setScreen(previous);
    }
    test("rerelease fixture leaves durable preferences unchanged", () ->
        check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "rerelease fixture wrote global preferences"));
    log("RELEASE300 RERELEASE " + action.toUpperCase(Locale.ROOT) + " failures=" + failures);
  }

  private static Object get(Object target, String name) throws Exception { return Editor230Checks.get(target, name); }
  private static Object call(Object target, String name, Class<?>[] types, Object... values) throws Exception {
    return Editor230Checks.invoke(target, name, types, values);
  }
  private static Object call(Object target, String name) throws Exception { return call(target, name, new Class<?>[0]); }
  private static double value(Object target, String name) throws Exception { return ((Number)call(target, name)).doubleValue(); }
  private static int number(Object target, String name) throws Exception { return (int)value(target, name); }
  private static Field field(Class<?> type, String name) throws Exception {
    for (; type != null; type = type.getSuperclass()) try {
      Field f = type.getDeclaredField(name); f.setAccessible(true); return f;
    } catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  private static void set(Object target, String name, Object value) throws Exception { field(target.getClass(), name).set(target, value); }
  private static Object stat(Class<?> type, String name) throws Exception { return field(type, name).get(null); }
  private static void click(Screen screen, int x, int y, int button) { ((ScreenInput)(Object)screen).power$click(x, y, button); }
  private static void release(Screen screen, int x, int y) { ((ScreenInput)(Object)screen).power$release(x, y, 0); }
  private static void key(Screen screen, int key) { ((ScreenInput)(Object)screen).power$key((char)0, key); }
  private static void open(Minecraft mc, Screen screen, int[] size) { mc.setScreen(screen); screen.init(mc, size[0], size[1]); }
  private static Setting setting(PowerOptionsScreen options, String id) { return find(options.session(), id); }
  private static SettingsDraft draft(HudEditorScreen hud) throws Exception { return (SettingsDraft)get(hud, "draft"); }
  private static void refresh(HudEditorScreen hud) throws Exception { call(hud, "afterEdit"); }
  private static void draw(Minecraft mc, Screen screen) {
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight); GL11.glDisable(GL11.GL_SCISSOR_TEST);
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity();
      GL11.glOrtho(0, screen.width, screen.height, 0, 1000, 3000);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity(); GL11.glTranslatef(0, 0, -2000);
      screen.render(-1, -1, 0);
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(mode);
    }
  }

  private static void tabs(Minecraft mc) throws Exception {
    for (int[] size : SIZES) test("active tab clears Related/search focus and Back while retaining scroll at " + size[0], () -> {
      var options = new PowerOptionsScreen(null); open(mc, options, size);
      try {
        for (boolean search : new boolean[]{false, true}) {
          if (search) ((TextInput)get(options, "search")).setText("zoom");
          Setting target = setting(options, "keys.powerbeta.mapZoomIn");
          call(options, "openSetting", new Class<?>[]{Setting.class}, target);
          check(get(options, "highlightedId").equals(target.id) && get(options, "focusAnchorId").equals(target.id), "Related destination did not own actual focus");
          check(!((Collection<?>)get(options, "history")).isEmpty(), "Related destination lacks Back history");
          double scroll = ((Number)get(options, "scroll")).doubleValue();
          check(scroll > 0, "fixture destination did not require scrolling");
          List<?> pages = (List<?>)get(options, "pages");
          int y = 24 + pages.indexOf(get(options, "page")) * 22 - ((Number)get(options, "sideScroll")).intValue() + 10;
          click(options, number(options, "origin") + number(options, "sidebar") / 2, y, 0);
          check(get(options, "highlightedId").equals("") && get(options, "focusAnchorId").equals(""), "active tab retained highlight/focus anchor");
          check(((Collection<?>)get(options, "history")).isEmpty(), "active tab retained Back");
          check(((Number)get(options, "scroll")).doubleValue() == scroll, "accepting Related destination changed scroll");
          call(options, "layout");
          check(get(options, "highlightedId").equals("") && get(options, "focusAnchorId").equals("")
              && ((Number)get(options, "scroll")).doubleValue() == scroll, "layout revived accepted destination focus");
          draw(mc, options);
        }
      } finally { options.session().discard(); }
    });
  }

  private static Object element(HudEditorScreen hud) throws Exception {
    for (Object e : (List<?>)call(get(hud, "preview"), "elements")) if (call(e, "id").equals("minimap")) return e;
    throw new AssertionError("minimap preview hitbox missing");
  }
  private static int[] pointer(HudEditorScreen hud, Object element) throws Exception {
    double scale = value(hud, "scale");
    return new int[]{(int)Math.round(value(hud, "canvasX") + (value(element, "x") + value(element, "width") / 2) * scale),
        (int)Math.round(value(hud, "canvasY") + (value(element, "y") + value(element, "height") / 2) * scale)};
  }
  private static void cancel(HudEditorScreen hud) throws Exception {
    for (Object button : (List<?>)call(hud, "footer")) if (call(button, "label").equals("Cancel")) {
      click(hud, number(button, "x") + number(button, "width") / 2, hud.height - 19, 0);
      check((boolean)call(get(hud, "exit"), "active"), "dirty HUD Cancel missed confirmation");
      click(hud, hud.width / 2, hud.height / 2 + 18, 0); return;
    }
    throw new AssertionError("HUD Cancel button missing");
  }
  private static void hud(Minecraft mc) throws Exception {
    for (int[] size : SIZES) test("minimap native drag rebuilds actual preview and hitbox without publishing at " + size[0], () -> {
      var before = Catalog.JSON.toJsonTree(MapConfig.current());
      var options = new PowerOptionsScreen(null); open(mc, options, size);
      try {
        setting(options, "maps.minimap").value = new JsonPrimitive(true);
        var parent = setting(options, "maps.offsetX").value.deepCopy();
        var hud = new HudEditorScreen(options, "maps.offsetX"); open(mc, hud, size);
        var local = draft(hud);
        local.get("maps.corner").value = new JsonPrimitive(0);
        local.get("maps.offsetX").value = new JsonPrimitive(28);
        local.get("maps.offsetY").value = new JsonPrimitive(18);
        local.get("maps.size").value = new JsonPrimitive(64);
        for (String id : List.of("maps.coordinates", "maps.biome", "maps.clock")) local.get(id).value = new JsonPrimitive(false);
        set(hud, "snapping", false); set(hud, "background", false);
        call(hud, "select", new Class<?>[]{String.class}, "layout"); refresh(hud); draw(mc, hud);
        Object first = element(hud); int[] p = pointer(hud, first);
        var unselected = local.snapshot();
        click(hud, p[0], p[1], 0); release(hud, p[0] + 8, p[1] + 5);
        check(get(hud, "selected").equals("minimap") && local.snapshot().equals(unselected), "selection press moved minimap");
        draw(mc, hud); first = element(hud); p = pointer(hud, first);
        click(hud, p[0], p[1], 0); check(get(hud, "dragging") != null, "selected minimap did not own native press");
        int dx = Math.max(5, (int)Math.round(24 * value(hud, "scale")));
        int dy = Math.max(3, (int)Math.round(12 * value(hud, "scale")));
        call(hud, "move", new Class<?>[]{int.class, int.class}, p[0] + dx, p[1] + dy);
        var preview = (MapSettings)get(get(hud, "preview"), "map");
        check(preview.offsetX == local.get("maps.offsetX").value.getAsInt() && preview.offsetY == local.get("maps.offsetY").value.getAsInt(), "drag left renderer's MapSettings stale");
        check(preview.offsetX > 28 && preview.offsetY > 18, "drag did not update minimap position");
        release(hud, p[0] + dx, p[1] + dy); draw(mc, hud);
        Object moved = element(hud);
        var bounds = Minimap.frame(mc, preview, hud.width, hud.height, false, false).bounds();
        check(value(moved, "x") == bounds.x() && value(moved, "y") == bounds.y()
            && value(moved, "width") == bounds.width() && value(moved, "height") == bounds.height(), "preview hitbox differs from actual minimap frame");
        check(value(moved, "x") > value(first, "x") && value(moved, "y") > value(first, "y"), "actual rendered preview stayed at old location");
        int[] next = pointer(hud, moved); click(hud, next[0], next[1], 0);
        check(get(hud, "dragging") != null, "new preview location is not draggable"); release(hud, next[0], next[1]);
        check(before.equals(Catalog.JSON.toJsonTree(MapConfig.current())) && setting(options, "maps.offsetX").value.equals(parent), "minimap drag escaped isolated HUD draft");
        cancel(hud); check(mc.currentScreen == options && setting(options, "maps.offsetX").value.equals(parent), "HUD Cancel accepted drag");
      } finally { options.session().discard(); }
      check(before.equals(Catalog.JSON.toJsonTree(MapConfig.current())), "HUD Cancel changed live minimap");
    });
  }

  private static void minimap(Minecraft mc) throws Exception {
    test("new minimap controls belong to one HUD draft and arrow kinds remain independent", () -> {
      var options = new PowerOptionsScreen(null); mc.setScreen(options);
      var before = Catalog.JSON.toJsonTree(MapConfig.current());
      try {
        var hud = new HudEditorScreen(options, "maps.directionTextSize"); open(mc, hud, SIZES[1]);
        var local = draft(hud);
        for (String suffix : MINIMAP) {
          Setting s = local.get("maps." + suffix);
          check(HudEditorSettings.contains(s) && HudEditorSettings.tab(s, 0).equals("minimap"), "new minimap setting is outside HUD draft: " + s.id);
          if (s.kind == Setting.Kind.BOOLEAN) s.value = new JsonPrimitive(!s.value.getAsBoolean());
          else if (suffix.endsWith("Size")) s.value = new JsonPrimitive(125);
          else if (suffix.endsWith("Color")) s.value = new JsonPrimitive(0x2468ac);
          else if (suffix.endsWith("Opacity")) s.value = new JsonPrimitive(.35);
        }
        refresh(hud); var map = (MapSettings)get(get(hud, "preview"), "map");
        check(map.directionTextSize == 125 && map.belowTextSize == 125 && map.minimapArrowSize == 125
            && map.destinationArrowColor == 0x2468ac && map.destinationArrowOpacity == .35, "new minimap renderer fields did not follow draft");
        check(ColorSettings.opacity(local.get("maps.destinationArrowColor"), local.settings()) == local.get("maps.destinationArrowOpacity"), "destination arrow color lost paired alpha");
        map.waypoints = true; map.minimapArrowSize = 150; map.destinationArrowColor = 0x13579b; map.destinationArrowOpacity = .42;
        var saved = new Waypoint(); var death = new Waypoint(); death.death = true;
        var destination = new Waypoint(); destination.id = "temporary-destination";
        Waypoint[] kinds = {saved, destination, death};
        for (int enabled = 0; enabled < 3; enabled++) {
          map.minimapWaypointArrows = enabled == 0; map.minimapDestinationArrow = enabled == 1; map.minimapDeathpointArrows = enabled == 2;
          for (int i = 0; i < kinds.length; i++) {
            var style = MinimapArrows.style(kinds[i], map, true);
            check(style.visible() == (i == enabled) && style.scale() == 1.5, "minimap arrow preferences share ownership");
            check(!MinimapArrows.style(kinds[i], map, false).visible(), "minimap arrow preference affected World Map");
          }
        }
        var style = MinimapArrows.style(destination, map, true);
        check(style.color() == 0x13579b && style.opacity() == .42, "destination arrow color/alpha policy ignored draft");
        map.waypoints = false; check(!MinimapArrows.style(death, map, true).visible(), "disabled map waypoints retained edge arrows");
        draw(mc, hud); cancel(hud);
        check(mc.currentScreen == options && before.equals(Catalog.JSON.toJsonTree(MapConfig.current())), "minimap controls Cancel leaked settings");
      } finally { options.session().discard(); }
    });
  }

  private static void tab(ColourScreen picker, int tab) throws Exception {
    click(picker, number(picker, "left") + tab * (number(picker, "tabWidth") + 4) + 8, number(picker, "tabsY") + 8, 0);
    check(((Number)get(picker, "tab")).intValue() == tab, "color tab native hit target missed");
  }
  private static void slider(ColourScreen picker, int channel, double fraction) throws Exception {
    int y = ((Number)call(picker, "sliderY", new Class<?>[]{int.class}, channel)).intValue();
    if (y < number(picker, "bodyTop") || y + 18 > number(picker, "bodyBottom")) {
      set(picker, "scroll", Math.max(0d, ((Number)get(picker, "scroll")).doubleValue() + y + 18 - number(picker, "bodyBottom")));
      y = ((Number)call(picker, "sliderY", new Class<?>[]{int.class}, channel)).intValue();
    }
    int x = (int)Math.round(number(picker, "controlX") + 4 + fraction * (number(picker, "controlWidth") - 8));
    click(picker, number(picker, "controlX") + 4, y + 8, 0);
    check(((Number)get(picker, "dragging")).intValue() == channel, "color slider did not own native press");
    release(picker, x, y + 8);
    check(((Number)get(picker, "dragging")).intValue() == -1, "color slider retained released button");
  }
  private static void colors(Minecraft mc) throws Exception {
    for (int[] size : SIZES) test("RGBA and wheel share RGB/hex/alpha, keep 36px preview, and Cancel at " + size[0], () -> {
      var options = new PowerOptionsScreen(null); open(mc, options, size);
      try {
        Setting color = setting(options, "maps.pathColor"), alpha = setting(options, "maps.pathOpacity"), swatches = setting(options, "interface.colours");
        JsonElement before = color.value.deepCopy(), oldAlpha = alpha.value.deepCopy(), oldSwatches = swatches.value.deepCopy();
        int[] accepted = {0};
        var picker = new ColourScreen(options, color, alpha, () -> accepted[0]++, swatches); open(mc, picker, size);
        check(((Number)get(picker, "tab")).intValue() == 0, "Color wheel is not the default tab");
        ((TextInput)get(picker, "hex")).setText("#2468AC"); call(picker, "parse");
        tab(picker, 0); check(((Number)get(picker, "color")).intValue() == 0x2468ac, "wheel discarded RGBA hex");
        slider(picker, 3, .35);
        check(Math.abs(ColorSettings.opacityPercent((Setting)get(picker, "localOpacity")) - 35) < 1, "wheel opacity did not edit local alpha");
        Object bounds = call(picker, "wheelBounds");
        int x = (int)Math.round(value(bounds, "x") + value(bounds, "radius") * .6), y = (int)Math.round(value(bounds, "y"));
        click(picker, x, y, 0); release(picker, x, y);
        int selected = ((Number)get(picker, "color")).intValue();
        check(selected != 0x2468ac && ((TextInput)get(picker, "hex")).text().equalsIgnoreCase(ColorSettings.hex(selected)), "wheel selection failed to update shared hex");
        tab(picker, 1); check(((Number)get(picker, "color")).intValue() == selected, "RGBA discarded wheel selection");
        slider(picker, 0, .8);
        selected = ((Number)get(picker, "color")).intValue();
        tab(picker, 0);
        check(((Number)get(picker, "color")).intValue() == selected
            && ((TextInput)get(picker, "hex")).text().equalsIgnoreCase(ColorSettings.hex(selected)), "RGBA slider and wheel disagree");
        check(Math.abs(ColorSettings.opacityPercent((Setting)get(picker, "localOpacity")) - 35) < 1, "tab switch discarded local alpha");
        check(field(ColourScreen.class, "PREVIEW").getInt(null) == 36
            && number(picker, "tabsY") - number(picker, "previewY") == 42
            && number(picker, "bodyTop") < number(picker, "bodyBottom"), "small color layout lost 36px preview/body");
        draw(mc, picker); tab(picker, 1); draw(mc, picker);
        click(picker, picker.width / 2 + 52, picker.height - 18, 0);
        check(mc.currentScreen == picker && (boolean)call(get(picker, "exit"), "active"), "dirty color Cancel missed confirmation");
        click(picker, picker.width / 2, picker.height / 2 + 18, 0);
        check(mc.currentScreen == options && accepted[0] == 0 && color.value.equals(before)
            && alpha.value.equals(oldAlpha) && swatches.value.equals(oldSwatches), "color Cancel published RGB, alpha or swatches");
        picker = new ColourScreen(options, color, alpha, () -> accepted[0]++, swatches); open(mc, picker, size);
        tab(picker, 0); ((TextInput)get(picker, "hex")).setText("#13579B"); call(picker, "parse");
        slider(picker, 3, .42); tab(picker, 1); key(picker, Keyboard.KEY_RETURN);
        check(mc.currentScreen == options && accepted[0] == 1 && ColorSettings.rgb(color) == 0x13579b
            && Math.abs(ColorSettings.opacityPercent(alpha) - 42) < 1, "Done did not accept shared hex and alpha together");
      } finally { options.session().discard(); }
    });
  }

  private static void defaults(Minecraft mc) throws Exception {
    test("recent path off, zoom one and symmetric ten-block caves apply to every defaults base", () -> {
      var session = SettingsRegistry.open(mc);
      try {
        for (var base : DefaultBases.Base.values()) for (var expected : Map.of("maps.footsteps", false,
            "maps.zoom", 1, "maps.caveAbove", 10, "maps.caveBelow", 10).entrySet()) {
          Setting s = find(session, expected.getKey());
          JsonElement target = DefaultBases.value(base, s.id, s.powerBetaDefault);
          if (expected.getValue() instanceof Boolean b) check(target.getAsBoolean() == b, "wrong " + base.id + " default: " + s.id);
          else check(target.getAsDouble() == ((Number)expected.getValue()).doubleValue(), "wrong " + base.id + " default: " + s.id);
        }
        var fresh = new MapSettings(); check(!fresh.footsteps && fresh.zoom == 1 && fresh.caveAbove == 10 && fresh.caveBelow == 10, "backend fallback defaults disagree");
      } finally { session.discard(); }
    });
  }

  /** Native camera invocations can touch interpolation/FOV state; restore all primitive fields. */
  private static final class Primitives implements AutoCloseable {
    final Object target; final Map<Field, Object> values = new LinkedHashMap<>();
    Primitives(Object target) throws Exception {
      this.target = target;
      for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) for (Field f : type.getDeclaredFields())
        if (f.getType().isPrimitive() && !Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())) {
          f.setAccessible(true); values.put(f, f.get(target));
        }
    }
    public void close() throws Exception { for (var e : values.entrySet()) e.getKey().set(target, e.getValue()); }
  }
  private static float[] matrix() {
    var buffer = BufferUtils.createFloatBuffer(16); GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
    float[] result = new float[16]; for (int i = 0; i < 16; i++) result[i] = buffer.get(i); return result;
  }
  private static Map<String, double[]> labels() throws Exception {
    Map<String, double[]> result = new LinkedHashMap<>();
    for (Object label : (List<?>)stat(WaypointMarkers.class, "LABELS"))
      result.put((String)call(label, "text"), new double[]{value(label, "x"), value(label, "y")});
    return result;
  }
  private static Map<String, double[]> frame(Minecraft mc, Method camera, boolean bob, float phase) throws Exception {
    mc.options.bobView = bob;
    mc.player.field_1634 = phase - .4f; mc.player.field_1635 = phase;
    mc.player.field_524 = mc.player.field_525 = .6f;
    mc.player.field_1043 = mc.player.field_1044 = 2;
    camera.invoke(mc.field_2818, .5f, 0);
    WaypointMarkers.project(mc, .5f);
    return labels();
  }
  private static void camera(Minecraft mc) throws Exception {
    test("native camera bob hook stabilizes nearby and 800m waypoint labels across walking phases", () -> {
      check(Maps.waypoints() != null && Maps.waypoints().ready(), "wait for waypoint store before native camera checks");
      check(!local.luke.power.input.DetachedCamera.isActive(), "leave freecam before native camera check");
      Method camera = class_555.class.getDeclaredMethod("method_1840", float.class, int.class); camera.setAccessible(true);
      Screen previous = mc.currentScreen; var settings = MapConfig.copy(); Object view = mc.field_2807;
      Object oldPoints = stat(Maps.class, "points"), oldWorld = stat(WaypointMarkers.class, "world");
      var liveLabels = (List<Object>)stat(WaypointMarkers.class, "LABELS"); var oldLabels = List.copyOf(liveLabels);
      Object correction = stat(WaypointMarkers.class, "CAMERA");
      Map<Field, double[]> arrays = new LinkedHashMap<>();
      for (String name : List.of("before", "inverse", "correction")) {
        Field f = field(correction.getClass(), name); arrays.put(f, ((double[])f.get(correction)).clone());
      }
      boolean valid = field(correction.getClass(), "valid").getBoolean(correction);
      boolean captureBob = field(WaypointMarkers.class, "captureBob").getBoolean(null);
      Map<FloatBuffer, float[]> buffers = new IdentityHashMap<>();
      for (String name : List.of("MODEL", "PROJECTION", "UNBOBBED")) {
        var buffer = (FloatBuffer)stat(WaypointMarkers.class, name); float[] values = new float[buffer.capacity()];
        for (int i = 0; i < values.length; i++) values[i] = buffer.get(i); buffers.put(buffer, values);
      }
      var viewport = (IntBuffer)stat(WaypointMarkers.class, "VIEWPORT"); int[] oldViewport = new int[viewport.capacity()];
      for (int i = 0; i < oldViewport.length; i++) oldViewport[i] = viewport.get(i);
      int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
      try (var player = new Primitives(mc.player); var options = new Primitives(mc.options); var renderer = new Primitives(mc.field_2818)) {
        mc.setScreen(null); mc.field_2807 = mc.player;
        mc.options.hideHud = false; mc.options.thirdPerson = false; mc.options.anaglyph3d = false;
        mc.player.x = mc.player.field_1637 = Math.floor(mc.player.x) + .5;
        mc.player.z = mc.player.field_1639 = Math.floor(mc.player.z) + .5;
        mc.player.y = mc.player.field_1638 = Math.max(4, Math.min(122, Math.floor(mc.player.y)));
        mc.player.yaw = mc.player.prevYaw = mc.player.pitch = mc.player.prevPitch = 0;
        var config = MapConfig.copy(); config.worldWaypoints = true; config.waypointNames = true; config.waypointDistances = false; config.waypointDistance = 0;
        MapConfig.preview(config);
        var points = new ArrayList<Waypoint>();
        for (int distance : new int[]{48, 800}) {
          var point = new Waypoint(); point.name = "Rerelease " + distance;
          point.x = (int)Math.floor(mc.player.x); point.y = (int)Math.floor(mc.player.y) - 1;
          point.z = (int)Math.floor(mc.player.z) + distance; point.dimension = Maps.dimension(); point.global = true;
          point.set = Maps.waypoints().selectedSet(); points.add(point);
        }
        field(Maps.class, "points").set(null, List.copyOf(points));
        GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
        var baseline = frame(mc, camera, false, .2f); float[] unbobbed = matrix();
        check(baseline.size() == 2, "native camera clipped near/far labels: " + baseline.keySet());
        double[] far = baseline.get("Rerelease 800");
        check(Math.abs(far[0] - .5) < .01 && Math.abs(far[1] - .5) < .02, "800m point looking directly ahead missed screen center");
        boolean nativeBobMoved = false;
        for (float phase : new float[]{.2f, .65f, 1.1f, 1.6f}) {
          var labels = frame(mc, camera, true, phase); float[] bobbed = matrix();
          for (int i = 0; i < 16; i++) nativeBobMoved |= Math.abs(bobbed[i] - unbobbed[i]) > .001;
          check(labels.keySet().equals(baseline.keySet()), "walking bob hid near/far waypoint");
          for (String name : baseline.keySet()) for (int axis = 0; axis < 2; axis++)
            check(Math.abs(labels.get(name)[axis] - baseline.get(name)[axis]) < .00002,
                "native bob moved label " + name + " axis=" + axis + " phase=" + phase);
          check(field(correction.getClass(), "valid").getBoolean(correction), "native method_1840 never captured its bob correction");
        }
        check(nativeBobMoved, "fixture did not exercise nonzero native walking bob");
      } finally {
        field(Maps.class, "points").set(null, oldPoints); field(WaypointMarkers.class, "world").set(null, oldWorld);
        liveLabels.clear(); liveLabels.addAll(oldLabels);
        for (var entry : arrays.entrySet()) System.arraycopy(entry.getValue(), 0, entry.getKey().get(correction), 0, 16);
        field(correction.getClass(), "valid").setBoolean(correction, valid);
        field(WaypointMarkers.class, "captureBob").setBoolean(null, captureBob);
        for (var entry : buffers.entrySet()) for (int i = 0; i < entry.getValue().length; i++) entry.getKey().put(i, entry.getValue()[i]);
        for (int i = 0; i < oldViewport.length; i++) viewport.put(i, oldViewport[i]);
        mc.field_2807 = (net.minecraft.entity.LivingEntity)view;
        MapConfig.preview(settings); mc.setScreen(previous);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
        GL11.glPopAttrib(); GL11.glMatrixMode(matrixMode);
      }
      check(GL11.glGetError() == GL11.GL_NO_ERROR, "native waypoint camera left OpenGL error");
    });
  }
}
