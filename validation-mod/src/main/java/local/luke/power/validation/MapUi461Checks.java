package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import local.luke.power.config.*;
import local.luke.power.input.PhotoModeHooks;
import local.luke.power.map.*;
import local.luke.power.recipes.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Native sidebar wheel ownership, row bounds and every changed content-tab view. */
final class MapUi461Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-461/map-ui");
  private static final int[][] SIZES = {{854, 480}, {320, 240}};

  private MapUi461Checks() {}

  static void run(Minecraft mc, String action) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote, "open a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    check(action.equals("check") || action.equals("capture"), "use map-ui-461 check|capture");
    failures = 0;
    if (action.equals("check")) zoom(mc);
    else capture(mc);
    log("MAP_UI461 failures=" + failures);
    check(failures == 0, "map-ui-461 failures=" + failures);
  }

  private static void zoom(Minecraft mc) throws Exception {
    Screen previous = mc.currentScreen;
    MapSettings runtime = MapConfig.copy();
    PowerConfig.flushPending();
    var stored = PowerConfig.section("maps").deepCopy();
    boolean automatic = MenuPreferences.current().autoApply;
    try {
      for (boolean borrowed : new boolean[] {false, true}) {
        test("MAP_UI461 automatic " + (borrowed ? "borrowed" : "owned") + " minimap wheel never waits for the settings writer", () -> {
          MenuPreferences.current().autoApply = true;
          var panel = panel(mc, borrowed);
          var zoom = setting(panel, "maps.zoom");
          call(panel, "zoomMinimap", new Class<?>[] {int.class}, 0);
          try (var blocked = new StalledWriter()) {
            long longest = 0;
            double expected = zoom.value.getAsDouble();
            for (int index = 0; index < 12; index++) {
              int wheel = index % 3 == 0 ? -120 : 120;
              expected = MinimapZoom.scroll(expected, wheel);
              long started = System.nanoTime();
              call(panel, "zoomMinimap", new Class<?>[] {int.class}, wheel);
              longest = Math.max(longest, System.nanoTime() - started);
              check(blocked.held(), "wheel waited until the stalled writer timed out");
              check(Math.abs(MapConfig.current().zoom - expected) < 1e-9, "automatic wheel lost a target");
              check(!zoom.changed(), "automatic wheel left a manual draft");
            }
            check(longest < 150_000_000L, "minimap wheel blocked for " + longest / 1e6 + "ms");
            log("MAP_UI461 " + (borrowed ? "borrowed" : "owned") + " wheel max=" + longest / 1e6 + "ms while settings writer held");
          }
          PowerConfig.flushPending();
          check(Math.abs(PowerConfig.section("maps").get("zoom").getAsDouble() - zoom.value.getAsDouble()) < 1e-9,
              "automatic wheel did not persist its final immutable target");
          mc.setScreen(previous);
        });
        test("MAP_UI461 manual " + (borrowed ? "borrowed" : "owned") + " minimap wheel keeps Cancel and Apply", () -> {
          MenuPreferences.current().autoApply = false;
          var panel = panel(mc, borrowed);
          var zoom = setting(panel, "maps.zoom");
          double before = zoom.value.getAsDouble();
          var durable = PowerConfig.section("maps").deepCopy();
          int wheel = before > 1 ? -120 : 120;
          call(panel, "zoomMinimap", new Class<?>[] {int.class}, wheel);
          check(zoom.changed() && MapConfig.current().zoom != before, "manual wheel did not preview its draft");
          check(PowerConfig.section("maps").equals(durable), "manual wheel wrote before Apply");
          int cancelX = ((Number)call(panel, "actionX", new Class<?>[] {int.class}, 0)).intValue();
          panel.layerPress(cancelX + 8, panel.height - 20, 0);
          if (!borrowed) {
            check((boolean)get(panel, "confirmClose"), "owned Cancel did not offer the draft exit");
            panel.layerPress(panel.width / 2 + 12, panel.height / 2 + 20, 0);
          }
          check(MapConfig.current().zoom == before && !zoom.changed(), "manual Cancel did not restore zoom");
          var applyPanel = panel(mc, borrowed);
          var applyZoom = setting(applyPanel, "maps.zoom");
          call(applyPanel, "zoomMinimap", new Class<?>[] {int.class}, wheel);
          double accepted = applyZoom.value.getAsDouble();
          int applyX = ((Number)call(applyPanel, "actionX", new Class<?>[] {int.class}, 1)).intValue();
          applyPanel.layerPress(applyX + 8, applyPanel.height - 20, 0);
          check(!applyZoom.changed() && PowerConfig.section("maps").get("zoom").getAsDouble() == accepted,
              "manual Apply lost the accepted zoom");
          mc.setScreen(previous);
        });
      }
    } finally {
      MenuPreferences.current().autoApply = automatic;
      PowerConfig.saveDeferred("maps", stored);
      PowerConfig.flushPending();
      MapConfig.preview(runtime);
      mc.setScreen(previous);
    }
    check(PowerConfig.section("maps").equals(stored), "wheel fixture did not restore stored map preferences");
  }

  private static PowerOptionsScreen panel(Minecraft mc, boolean borrowed) throws Exception {
    var owner = new PowerOptionsScreen(mc.currentScreen);
    mc.setScreen(owner); owner.init(mc, 854, 480);
    WorldMapScreen map;
    if (borrowed) {
      check(WorldMapScreen.openMinimapSettings(mc, owner, true), "could not borrow the Options map session");
      map = (WorldMapScreen)mc.currentScreen;
    } else {
      map = new WorldMapScreen(owner); mc.setScreen(map); map.init(mc, 854, 480);
      call(map, "toggleSettings", new Class<?>[0]);
    }
    map.init(mc, 854, 480);
    var panel = (PowerOptionsScreen)get(map, "settingsPanel");
    check(panel != null, "map settings sidebar did not open");
    check(!borrowed || panel.session() == owner.session(), "borrowed map replaced the Options session");
    return panel;
  }

  private static void capture(Minecraft mc) throws Exception {
    Files.createDirectories(REPORT);
    Screen previous = mc.currentScreen;
    MapSettings runtime = MapConfig.copy();
    var recipeRuntime = RecipeBookConfig.copy();
    boolean automatic = MenuPreferences.current().autoApply, grabbed = Mouse.isGrabbed();
    int guiScale = mc.options.guiScale;
    var customScale = Class.forName("local.luke.power.controls.util.ModOptions").getField("guiScale");
    var realScale = Class.forName("local.luke.power.controls.util.ModOptions").getField("realGuiScale");
    float originalScale = customScale.getFloat(null);
    float originalRealScale = realScale.getFloat(null);
    int matrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try (var points = new PreviewPoints()) {
      MenuPreferences.current().autoApply = false; Mouse.setGrabbed(false);
      mc.options.guiScale = 2;
      customScale.setFloat(null, .25f);
      realScale.setFloat(null, .25f);
      var style = MapConfig.copy(); style.worldMap = true; MapConfig.preview(style);
      var recipes = RecipeBookConfig.copy(); recipes.enabled = recipes.pauseMenu = true; RecipeBookConfig.preview(recipes);
      for (int[] size : SIZES) {
        var owner = new PowerOptionsScreen(previous); open(mc, owner, size);
        var color = setting(owner, "maps.destinationArrowColor");
        var opacity = setting(owner, "maps.destinationArrowOpacity");
        var swatches = setting(owner, "interface.colours");
        var picker = new ColourScreen(owner, color, opacity, () -> {}, swatches); open(mc, picker, size);
        for (int tab = 0; tab < 2; tab++) {
          ((ScreenInput)(Object)picker).power$click(number(picker, "left") + tab * (number(picker, "tabWidth") + 4) + 10,
              number(picker, "tabsY") + 8, 0);
          check((int)get(picker, "tab") == tab, "color-picker tab click did not switch views");
          shot(mc, picker, "color-" + (tab == 0 ? "wheel" : "rgba"), size, -1, -1);
          openTab("color-" + (tab == 0 ? "wheel" : "rgba"), size,
              number(picker, "left") + tab * (number(picker, "tabWidth") + 4) + 12,
              number(picker, "left") + (1 - tab) * (number(picker, "tabWidth") + 4) + 12,
              number(picker, "tabsY") + 17);
        }
        var lettering = new TitleLetteringScreen(owner); open(mc, lettering, size);
        for (int tab = 0; tab < 3; tab++) {
          ((ScreenInput)(Object)lettering).power$click(number(lettering, "left") + tab * (number(lettering, "tabWidth") + 4) + 10, 38, 0);
          check((int)get(lettering, "tab") == tab, "title-lettering tab click did not switch views");
          String label = "title-" + new String[] {"paint", "appearance", "presets"}[tab];
          shot(mc, lettering, label, size, -1, -1);
          openTab(label, size, number(lettering, "left") + tab * (number(lettering, "tabWidth") + 4) + 12,
              number(lettering, "left") + (tab + 1) % 3 * (number(lettering, "tabWidth") + 4) + 12, 47);
        }
        waypointShots(mc, points, size);
        var map = new WorldMapScreen(previous); open(mc, map, size);
        set(map, "target", points.first); set(map, "context", true); set(map, "contextX", 20); set(map, "contextY", 48);
        shot(mc, map, "world-map-context", size, 21, 49);
        flushHover("world-map-context", size, 20, 48, 142);
        Screen photo = (Screen)Class.forName("local.luke.power.controls.tweaks.photomode.PhotoModeScreen")
            .getConstructor(Screen.class).newInstance(previous);
        open(mc, photo, size);
        Object tools = PhotoModeHooks.tools();
        set(tools, "target", points.first); set(tools, "context", true); set(tools, "contextSafe", false);
        set(tools, "contextActions", getStatic(PhotoModeTools.class, "WITH_TELEPORT"));
        set(tools, "contextX", 20); set(tools, "contextY", 48);
        shot(mc, photo, "photo-context", size, 21, 67);
        flushHover("photo-context", size, 20, 66, 142);
        var viewer = new RecipeBookViewerScreen(previous); open(mc, viewer, size);
        for (int kind = 0; kind < 2; kind++) {
          var bounds = viewer.power$recipeBook().layout().kindTab(kind);
          ((ScreenInput)(Object)viewer).power$click(bounds.x() + 10, bounds.y() + 10, 0);
          check(get(viewer, "kind").toString().equals(kind == 0 ? "CRAFTING" : "FURNACE"), "recipe content tab click did not switch views");
          String label = "recipe-" + (kind == 0 ? "crafting" : "smelting");
          shot(mc, viewer, label, size, -1, -1);
          var inactive = viewer.power$recipeBook().layout().kindTab(1 - kind);
          openTab(label, size, bounds.x() + 12, inactive.x() + 12, bounds.y() + bounds.height() - 1);
        }
        owner.session().discard();
      }
    } finally {
      mc.options.guiScale = guiScale;
      customScale.setFloat(null, originalScale);
      realScale.setFloat(null, originalRealScale);
      MenuPreferences.current().autoApply = automatic; MapConfig.preview(runtime); RecipeBookConfig.preview(recipeRuntime);
      mc.setScreen(previous); Mouse.setGrabbed(grabbed);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(matrix);
    }
    log("MAP_UI461 captures " + REPORT.toAbsolutePath());
  }

  private static void waypointShots(Minecraft mc, PreviewPoints points, int[] size) throws Exception {
    var screen = new WaypointsScreen(mc.currentScreen); open(mc, screen, size);
    set(screen, "selected", points.first.id);
    Object geometry = call(screen, "layout", new Class<?>[0]);
    int left = number(geometry, "left"), right = number(geometry, "right");
    check(call(screen, "rowAt", new Class<?>[] {int.class, int.class}, left, 84) != null, "left control edge is outside the waypoint row");
    check(call(screen, "rowAt", new Class<?>[] {int.class, int.class}, right - 1, 84) != null, "right control edge is outside the waypoint row");
    check(call(screen, "rowAt", new Class<?>[] {int.class, int.class}, right, 84) == null, "waypoint row overlaps scrollbar input");
    shot(mc, screen, "waypoints-rows", size, right - 1, 112);
    ((ScreenInput)(Object)screen).power$click(left + 40, 84, 1);
    check((boolean)get(screen, "context"), "waypoint row context did not open");
    shot(mc, screen, "waypoints-context", size, (int)get(screen, "contextX"), (int)get(screen, "contextY") + 1);
    flushHover("waypoints-context", size, (int)get(screen, "contextX"), (int)get(screen, "contextY"), number(screen, "menuWidth"));
  }

  private static void open(Minecraft mc, Screen screen, int[] size) { mc.setScreen(screen); screen.init(mc, size[0], size[1]); }
  private static void shot(Minecraft mc, Screen screen, String label, int[] size, int x, int y) throws Exception {
    SettingsSnapshot.render(mc, screen, size[0], size[1], REPORT.resolve(label + "-" + size[0] + ".png"), x, y);
  }
  private static void flushHover(String label, int[] size, int x, int y, int width) throws Exception {
    var image = ImageIO.read(REPORT.resolve(label + "-" + size[0] + ".png").toFile());
    for (int px : new int[] {x, x + width - 1}) for (int py : new int[] {y, y + 17})
      check((image.getRGB(px * 2, py * 2) & 0xffffff) == 0x444444, label + " hover does not reach its row edge");
  }
  private static void openTab(String label, int[] size, int selectedX, int inactiveX, int bottom) throws Exception {
    var image = ImageIO.read(REPORT.resolve(label + "-" + size[0] + ".png").toFile());
    check((image.getRGB(selectedX * 2, bottom * 2) & 0xffffff) != 0x777777, label + " selected tab has a closed bottom");
    check((image.getRGB(inactiveX * 2, bottom * 2) & 0xffffff) == 0x777777, label + " inactive tab lost the shared baseline");
  }
  private static Setting setting(PowerOptionsScreen screen, String id) {
    return screen.session().settings().stream().filter(setting -> setting.id.equals(id)).findFirst().orElseThrow();
  }
  private static Field declared(Class<?> type, String name) throws Exception {
    for (; type != null; type = type.getSuperclass()) try {
      var field = type.getDeclaredField(name); field.setAccessible(true); return field;
    } catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  private static Object get(Object owner, String name) throws Exception { return declared(owner.getClass(), name).get(owner); }
  private static Object getStatic(Class<?> type, String name) throws Exception { return declared(type, name).get(null); }
  private static void set(Object owner, String name, Object value) throws Exception { declared(owner.getClass(), name).set(owner, value); }
  private static Object call(Object owner, String name, Class<?>[] types, Object... args) throws Exception {
    for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) try {
      var method = type.getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(owner, args);
    } catch (NoSuchMethodException ignored) {}
    throw new NoSuchMethodException(name);
  }
  private static int number(Object owner, String name) throws Exception { return ((Number)call(owner, name, new Class<?>[0])).intValue(); }

  private static final class StalledWriter implements AutoCloseable {
    private final CountDownLatch release = new CountDownLatch(1), exited = new CountDownLatch(1);
    private final Thread worker;
    StalledWriter() throws Exception {
      Object disk = getStatic(PowerConfig.class, "DISK");
      var entered = new CountDownLatch(1);
      worker = new Thread(() -> {
        try { synchronized (disk) { entered.countDown(); release.await(5, TimeUnit.SECONDS); } }
        catch (InterruptedException failure) { Thread.currentThread().interrupt(); }
        finally { exited.countDown(); }
      }, "Map UI 461 stalled settings writer");
      worker.start();
      check(entered.await(2, TimeUnit.SECONDS), "settings writer fixture did not acquire its lock");
    }
    boolean held() { return exited.getCount() != 0; }
    public void close() throws Exception { release.countDown(); worker.join(2000); check(!worker.isAlive(), "settings writer fixture did not release"); }
  }

  private static final class PreviewPoints implements AutoCloseable {
    private final WaypointStore store = Maps.waypoints();
    private final Object data, points, explicitWaypoints;
    private final long revision, saved;
    final Waypoint first;
    PreviewPoints() throws Exception {
      check(store != null && store.ready() && !store.needsMaintenance(), "wait for loaded, idle waypoints before captures");
      data = get(store, "data"); revision = ((Number)get(store, "revision")).longValue(); saved = ((Number)get(store, "saved")).longValue();
      points = getStatic(Maps.class, "points");
      explicitWaypoints = getStatic(Maps.class, "explicitWaypoints");
      var preview = new WaypointStore.Data(); preview.sets.addAll(List.of("Exploration", "Builds"));
      for (int index = 0; index < 24; index++) {
        Waypoint point = MapKeys.newWaypoint(); point.name = index == 0 ? "Base camp" : "Exploration " + index;
        point.set = "Default";
        point.x += index * 16; point.z += index * 8; point.colour = index % 2 == 0 ? 0x55cc88 : 0xffcc55;
        preview.waypoints.add(point);
      }
      first = preview.waypoints.get(0); preview.validate();
      set(store, "data", preview); set(store, "revision", revision + 1); set(store, "saved", revision + 1);
      declared(Maps.class, "points").set(null, List.copyOf(preview.waypoints));
    }
    public void close() throws Exception {
      set(store, "data", data); set(store, "revision", revision); set(store, "saved", saved);
      declared(Maps.class, "points").set(null, points); declared(Maps.class, "explicitWaypoints").set(null, explicitWaypoints);
    }
  }
}
