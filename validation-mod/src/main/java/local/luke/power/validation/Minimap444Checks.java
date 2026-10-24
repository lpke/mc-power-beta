package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.util.*;
import local.luke.power.config.Catalog;
import local.luke.power.input.*;
import local.luke.power.map.*;
import local.luke.power.rules.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** New map controls, retained native owners and real expanded mouse input. Disposable clients only. */
public final class Minimap444Checks {
  private static Physical physical;
  private Minimap444Checks() {}
  public static void run(Minecraft mc, String action) throws Exception {
    action = action.trim();
    if (action.equals("aim")) { requireClient(mc); mc.setScreen(null); mc.player.pitch = mc.player.prevPitch = 75; return; }
    if (action.equals("stop")) { stop(mc); return; }
    if (action.startsWith("prepare ")) { prepare(mc, action.substring(8).trim()); return; }
    if (action.equals("check")) {
      check(physical != null, "prepare an expanded mouse case first");
      try { physical.checkResult(mc); } finally { stop(mc); }
      return;
    }
    check(action.isEmpty() || action.equals("run"), "use minimap-444 run, prepare CASE, check, or stop");
    check(physical == null, "stop the physical case before automated checks"); requireClient(mc);
    failures = 0;
    var map = MapConfig.copy(); boolean hidden = mc.options.hideHud, debug = mc.options.debugHud;
    try (var state = new Recipe402Checks.State(mc)) {
      mc.options.hideHud = mc.options.debugHud = false;
      for (boolean pause : new boolean[]{false, true}) {
        final boolean pauseHost = pause;
        test("MINIMAP444 " + (pause ? "pause" : "inventory") + " middle actions match all quick-menu rows", () -> {
          Screen host = host(mc, state, pauseHost); int[] point = clearPoint(mc);
          String items = state.snapshot();
          for (int row = 0; row < 5; row++) {
            var baseline = baseline(); baseline.middleClickAction = row; MapConfig.preview(baseline);
            check(Minimap.inventoryMiddle(mc, point[0], point[1]), "middle action was not claimed");
            String middle = Catalog.JSON.toJson(MapConfig.current());
            MapConfig.preview(baseline); MinimapQuickMenu.beginFrame(); var layout = quickLayout(mc);
            check(MinimapQuickMenu.mouse(mc, layout.gear().x() + 8, layout.gear().y() + 8, 0, true), "quick-menu button did not open");
            MinimapQuickMenu.mouse(mc, layout.gear().x() + 8, layout.gear().y() + 8, 0, false);
            var target = layout.row(row);
            MinimapQuickMenu.mouse(mc, target.x() + 20, target.y() + 10, 0, true);
            MinimapQuickMenu.mouse(mc, target.x() + 20, target.y() + 10, 0, false);
            check(middle.equals(Catalog.JSON.toJson(MapConfig.current())), "middle action differs from quick-menu row " + row);
            check(MinimapQuickMenu.key(mc, Keyboard.KEY_ESCAPE, true), "quick menu did not close");
          }
          check(mc.currentScreen == host && items.equals(state.snapshot()), "middle controls changed host or complete native inventory");
        });
        for (boolean pauses : new boolean[]{false, true}) {
          final boolean mapPauses = pauses;
          test("MINIMAP444 " + (pause ? "pause" : "inventory") + " map pause preference=" + pauses + " retains native owner", () -> {
            Screen host = host(mc, state, pauseHost); var style = MapConfig.copy(); style.pauseMap = mapPauses; MapConfig.preview(style);
            mc.player.inventory.setCursorStack(DeathChecks.tagged(35, 17, 5, "minimap-map-cursor"));
            state.context().inputs.get(0).setStack(DeathChecks.tagged(1, 3, 0, "minimap-map-grid"));
            Object owner = mc.player.container; String items = state.snapshot(); int[] point = clearPoint(mc);
            check(MinimapMapVisit.open(mc, point[0], point[1]), "left map action rejected clear terrain");
            check(mc.currentScreen instanceof WorldMapScreen, "world map did not open");
            check(mc.currentScreen.shouldPause() == (pauseHost || mapPauses), "map lost its pause origin or preference");
            check(mc.player.container == owner && items.equals(state.snapshot()), "opening map closed native container or altered cursor/grid");
            escape(mc.currentScreen);
            check(mc.currentScreen == host && mc.player.container == owner && items.equals(state.snapshot()), "map return lost host, container, cursor or crafting inputs");
          });
        }
      }
      test("MINIMAP444 overlapping panels and modals retain their pointer ownership", () -> {
        Screen host = host(mc, state, false); int[] point = clearPoint(mc); String items = state.snapshot();
        Object blocker = new Object();
        try {
          InventoryPanels.extra(host, blocker, List.of(new InventoryPanels.Rect(point[0] - 2, point[1] - 2, 5, 5)));
          check(((InventoryGui)host).power$containsGuiSpace(point[0], point[1]), "test overlap was not a native GUI panel");
          rejected(mc, point, "overlapping native panel"); InventoryPanels.removeExtra(host, blocker);
          InventoryPanels.modal(host, blocker, true); rejected(mc, point, "another modal");
        } finally { InventoryPanels.removeExtra(host, blocker); InventoryPanels.modal(host, blocker, false); }
        var style = MapConfig.copy(); style.worldMap = false; MapConfig.preview(style);
        check(!MinimapMapVisit.open(mc, point[0], point[1]), "disabled world map claimed left input");
        style = MapConfig.copy(); style.showInInventory = false; MapConfig.preview(style); rejected(mc, point, "hidden inventory map");
        check(mc.currentScreen == host && items.equals(state.snapshot()), "rejected minimap controls changed native items");
      });
      test("MINIMAP444 paused map children and centered maps retain pause and return ownership", () -> pauseChildren(mc, state));
      test("MINIMAP444 invalid middle actions are rejected and missing fields retain the default", () -> {
        check(Catalog.JSON.fromJson("{}", MapSettings.class).middleClickAction == 2, "missing middle action lost Show waypoints default");
        for (int value : new int[]{-1, 5}) {
          var invalid = baseline(); invalid.middleClickAction = value; boolean rejected = false;
          try { invalid.validate(); } catch (IllegalArgumentException expected) { rejected = true; }
          check(rejected, "invalid middle action accepted: " + value);
        }
      });
    } finally {
      MinimapQuickMenu.active(mc); MapConfig.queued(map); mc.options.hideHud = hidden; mc.options.debugHud = debug;
    }
    log("MINIMAP444 failures=" + failures); check(failures == 0, "minimap-444 failures=" + failures);
  }
  private static void pauseChildren(Minecraft mc, Recipe402Checks.State state) throws Exception {
    Screen host = host(mc, state, true); var style = MapConfig.copy(); style.pauseMap = false; MapConfig.preview(style);
    int[] point = clearPoint(mc); check(MinimapMapVisit.open(mc, point[0], point[1]), "pause map did not open");
    Screen map = mc.currentScreen; var waypoints = new WaypointsScreen(map); mc.setScreen(waypoints);
    check(waypoints.shouldPause(), "waypoint list unpaused pause-menu map");
    var waypoint = new Waypoint(); waypoint.name = "Disposable pause draft"; waypoint.dimension = Maps.dimension();
    var editor = new WaypointEditorScreen(waypoints, waypoint); mc.setScreen(editor);
    check(editor.shouldPause(), "waypoint editor unpaused pause-menu map"); escape(editor);
    check(mc.currentScreen == waypoints, "unchanged editor draft did not return to waypoint list");
    Constructor<WorldMapScreen> centeredConstructor = WorldMapScreen.class.getConstructor(Screen.class, Waypoint.class);
    Screen centered = centeredConstructor.newInstance(waypoints, waypoint); mc.setScreen(centered);
    check(centered.shouldPause(), "centered map unpaused its waypoint-list parent");
    Screen coordinates = new MapCoordinatesScreen(centered, waypoint.x, waypoint.z,
        (x, z) -> { throw new AssertionError("Escape accepted coordinate draft"); }); mc.setScreen(coordinates);
    check(coordinates.shouldPause(), "coordinate editor unpaused the centered map"); escape(coordinates);
    check(mc.currentScreen == centered, "coordinate editor lost centered-map return"); escape(centered);
    check(mc.currentScreen == waypoints, "centered map lost its waypoint-list return"); escape(waypoints);
    check(mc.currentScreen == map && map.shouldPause(), "waypoint list lost its paused map return"); escape(map);
    check(mc.currentScreen == host, "nested map visit lost native pause parent");
  }
  private static Screen host(Minecraft mc, Recipe402Checks.State state, boolean pause) throws Exception {
    state.reset(640); mc.options.hideHud = mc.options.debugHud = false; MapConfig.preview(baseline()); Maps.tick(mc);
    check(Maps.cache() != null, "wait for the native map cache");
    if (pause) { mc.setScreen(new net.minecraft.class_525()); mc.currentScreen.init(mc, 640, 360); }
    Mouse.setGrabbed(false); MinimapQuickMenu.beginFrame(); mc.currentScreen.render(0, 0, 1);
    return mc.currentScreen;
  }
  private static MapSettings baseline() {
    var style = MapConfig.copy(); style.minimap = style.worldMap = style.showInInventory = true;
    style.hideWithDebug = false; style.size = 64; style.corner = 1; style.offsetX = style.offsetY = 6;
    style.items = true; style.hostile = false; style.animals = true; style.waypoints = true;
    style.worldWaypoints = false; style.caveMode = 2; style.middleClickAction = 2; return style;
  }
  private static int[] clearPoint(Minecraft mc) {
    var layout = Minimap.layout(MapConfig.current(), mc.currentScreen.width, mc.currentScreen.height, false);
    for (int y = layout.y() + 4; y < layout.y() + layout.size() - 4; y++)
      for (int x = layout.x() + 4; x < layout.x() + layout.size() - 4; x++)
        if (Minimap.inventoryContains(mc, x, y) && !((InventoryGui)mc.currentScreen).power$containsGuiSpace(x, y)) return new int[]{x, y};
    throw new AssertionError("no minimap terrain outside native GUI controls");
  }
  private static void rejected(Minecraft mc, int[] point, String reason) {
    check(!MinimapMapVisit.open(mc, point[0], point[1]), reason + " lost left input");
    check(!Minimap.inventoryMiddle(mc, point[0], point[1]), reason + " lost middle input");
    check(!Minimap.inventoryReset(mc, point[0], point[1]), reason + " lost right input");
  }
  private static MinimapQuickMenuLayout.Layout quickLayout(Minecraft mc) throws Exception {
    Method method = MinimapQuickMenu.class.getDeclaredMethod("layout", Minecraft.class); method.setAccessible(true);
    return (MinimapQuickMenuLayout.Layout)method.invoke(null, mc);
  }
  private static void escape(Screen screen) { ((ScreenInput)screen).power$key('\0', Keyboard.KEY_ESCAPE); }
  private static void requireClient(Minecraft mc) {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable singleplayer world");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    check(!Mouse.isButtonDown(0) && !Mouse.isButtonDown(1) && !Mouse.isButtonDown(2), "release all mouse buttons");
  }
  private static void prepare(Minecraft mc, String name) throws Exception {
    check(physical == null, "check or stop the previous physical case"); requireClient(mc);
    check(Set.of("left", "right", "middle0", "middle1", "middle2", "middle3", "middle4", "disabled-middle").contains(name), "unknown expanded mouse case " + name);
    check(!Bindings.physical(Keyboard.KEY_Z), "release z before prepare");
    physical = new Physical(mc, name);
    log("MINIMAP444 READY " + name + ": hold z, click " + (name.equals("left") ? "LEFT once" : name.equals("right") ? "RIGHT once" : "MIDDLE once")
        + ", release the mouse button, then run check. " + (name.equals("left") ? "Keep the aimed block visible before clicking; native Creative attack must not break it." : ""));
  }
  private static void stop(Minecraft mc) throws Exception {
    if (physical != null) { Physical previous = physical; physical = null; previous.close(mc); }
  }
  private static Field field(Object owner, String name) throws Exception {
    for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
      try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field; } catch (NoSuchFieldException ignored) {}
    }
    throw new NoSuchFieldException(name);
  }
  private static final class Physical {
    final String name, mode;
    final Recipe402Checks.State state;
    final MapSettings map = MapConfig.copy();
    final RuleSet rules;
    final Map<KeyBinding, Integer> codes = new IdentityHashMap<>();
    final Map<String, Integer> modifiers = Bindings.modifiers();
    final int x, y, z, id, metadata;
    final String items;
    final boolean hidden, debug;
    final MapSettings before;
    Physical(Minecraft mc, String name) throws Exception {
      this.name = name; mode = mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
      rules = AccessRules.copy(mc); hidden = mc.options.hideHud; debug = mc.options.debugHud;
      check(mc.field_2823 != null, "aim at an ordinary loaded block before prepare");
      x = mc.field_2823.field_1984; y = mc.field_2823.field_1985; z = mc.field_2823.field_1986;
      id = mc.world.getBlockId(x, y, z); metadata = mc.world.method_1778(x, y, z);
      check(id > 0 && id < 256 && mc.world.method_1777(x, y, z) == null, "aim at a native block without a block entity");
      for (Object object : mc.options.allKeys) if (object instanceof KeyBinding binding) codes.put(binding, binding.code);
      state = new Recipe402Checks.State(mc); boolean prepared = false;
      try {
        state.reset(640); AccessRules.set(mc, GameRule.ALLOW_CREATIVE_MODE, true); CreativeVehicleChecks.mode(mc, "CREATIVE");
        mc.setScreen(null); mc.field_2778 = false; mc.method_2133();
        mc.paused = false; mc.options.hideHud = mc.options.debugHud = false;
        for (var binding : codes.keySet()) if (binding.code == Keyboard.KEY_Z || binding instanceof Binding key && key.power$id().equals("power_building.hotbar.scroll")) binding.code = 0;
        MapKeys.ENLARGE.code = Keyboard.KEY_Z; var chords = new HashMap<>(modifiers); chords.remove(MapKeys.ENLARGE.translationKey);
        Bindings.configure(chords); Bindings.register(mc.options.allKeys);
        var style = baseline(); style.zoomWhileExpanded = !name.equals("disabled-middle"); style.pauseMap = false;
        style.zoom = 2.5; style.zoomDefault = 1.25; style.middleClickAction = name.startsWith("middle") ? Integer.parseInt(name.substring(6)) : 2;
        MapConfig.preview(style); Maps.tick(mc); check(Maps.cache() != null, "wait for native map cache");
        before = MapConfig.current(); items = state.snapshot(); prepared = true;
      } finally { if (!prepared) close(mc); }
    }
    void checkResult(Minecraft mc) throws Exception {
      check(Bindings.physical(Keyboard.KEY_Z), "keep z held until check");
      check(!Mouse.isButtonDown(0) && !Mouse.isButtonDown(1) && !Mouse.isButtonDown(2), "release mouse button before check");
      check(mc.world.getBlockId(x, y, z) == id && mc.world.method_1778(x, y, z) == metadata, "expanded mouse input leaked a native attack/use");
      if (name.equals("left")) check(mc.currentScreen instanceof WorldMapScreen, "expanded left input did not open world map");
      else check(mc.currentScreen == null, "expanded mouse unexpectedly opened a GUI");
      if (name.equals("right")) check(Math.abs(MapConfig.current().zoom - 1.25) < 1e-9, "expanded right input did not reset zoom");
      else if (name.startsWith("middle")) {
        var expected = Catalog.JSON.fromJson(Catalog.JSON.toJson(before), MapSettings.class);
        switch (expected.middleClickAction) {
          case 0 -> expected.items = !expected.items;
          case 1 -> { boolean enabled = !(expected.hostile && expected.animals); expected.hostile = expected.animals = enabled; }
          case 2 -> expected.waypoints = !expected.waypoints;
          case 3 -> expected.worldWaypoints = !expected.worldWaypoints;
          case 4 -> expected.caveMode = 0;
          default -> throw new AssertionError("unsupported test action");
        }
        check(Catalog.JSON.toJson(expected).equals(Catalog.JSON.toJson(MapConfig.current())), "expanded middle input toggled the wrong preference or repeated");
      } else if (name.equals("disabled-middle")) {
        check(MapConfig.current() == before, "disabled utilities changed map settings");
        ItemStack picked = mc.player.inventory.getSelectedItem();
        check(picked != null && picked.count > 0, "disabled utilities swallowed native Creative block picking");
        check(field(mc, "power$mapMousePresses").getInt(mc) == 0, "disabled utilities retained a map-owned mouse press");
      }
      if (!name.equals("disabled-middle")) check(items.equals(state.snapshot()), "expanded mouse changed complete inventory/cursor data");
      log("PASS MINIMAP444 native expanded " + name + " target block unchanged and expected input owner retained");
    }
    void close(Minecraft mc) throws Exception {
      try {
        if (mc.currentScreen instanceof WorldMapScreen) escape(mc.currentScreen);
        state.close();
      } finally {
        codes.forEach((binding, code) -> binding.code = code); Bindings.configure(modifiers); Bindings.register(mc.options.allKeys);
        CreativeVehicleChecks.mode(mc, mode); AccessRules.restore(mc, rules.copy()); MapConfig.queued(map);
        mc.options.hideHud = hidden; mc.options.debugHud = debug;
      }
      log("MINIMAP444 restored; release z before the next case");
    }
  }
}
