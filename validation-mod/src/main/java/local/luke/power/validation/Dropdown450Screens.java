package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.MusicPreset;
import local.luke.power.config.*;
import local.luke.power.inventory.tools.SortingLayoutsData;
import local.luke.power.map.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Real screens with local example presets. Capture never accepts an editor or writes its drafts. */
final class Dropdown450Screens {
  private static final Path REPORT = Path.of("power-beta-data/reports/dropdowns-450/screens");
  private static final List<String> captures = new ArrayList<>();
  private Dropdown450Screens() {}

  static void capture(Minecraft mc) throws Exception {
    check(mc.world != null && Maps.waypoints() != null && Maps.waypoints().ready(), "open a disposable mapped world first");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    Files.createDirectories(REPORT); captures.clear();
    var previous = mc.currentScreen; boolean repeat = Keyboard.areRepeatEventsEnabled();
    var document = PowerConfig.document(); byte[] saved = Files.readAllBytes(PowerConfig.path());
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      for (int[] size : new int[][]{{640, 420}, {320, 240}}) {
        var options = new PowerOptionsScreen(previous); options.init(mc, size[0], size[1]);
        try {
          hud(mc, options, size); music(mc, options, size); waypoints(mc, previous, size);
          sorting(mc, options, size); creation(mc, size);
        } finally { options.removed(); }
      }
      check(document.equals(PowerConfig.document()) && Arrays.equals(saved, Files.readAllBytes(PowerConfig.path())), "screen captures changed saved preferences");
      Files.writeString(REPORT.resolve("captures.json"), Catalog.JSON.toJson(captures));
    } finally {
      mc.currentScreen = previous; Keyboard.enableRepeatEvents(repeat);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(mode);
    }
    log("DROPDOWN450 screens=" + captures.size() + " directory=" + REPORT);
  }

  private static void hud(Minecraft mc, PowerOptionsScreen options, int[] size) throws Exception {
    var screen = new HudEditorScreen(options, "power_hud:config.coordinateDisplay"); screen.init(mc, size[0], size[1]);
    try {
      set(screen, "settingsHidden", false); set(screen, "previewHidden", false);
      invoke(screen, "select", new Class<?>[]{String.class}, "debug");
      var draft = (SettingsDraft)get(screen, "draft"); var coordinates = draft.get("power_hud:config.coordinateDisplay");
      invoke(screen, "revealControl", new Class<?>[]{Setting.class}, coordinates);
      int y = (int)invoke(screen, "controlY", new Class<?>[]{Setting.class}, coordinates);
      invoke(screen, "edit", new Class<?>[]{Setting.class, int.class, int.class}, coordinates, 1, y);
      capture(mc, screen, size, "hud-choice"); close(get(screen, "choices"));
      screen.beginReorder(); Object reorder = get(screen, "reorder");
      seedDebug(reorder);
      capture(mc, screen, size, "f3-editor");
      input(screen).power$click((int)get(reorder, "left") + 8, (int)get(reorder, "toolbarTop") + 8, 0);
      check((boolean)invoke(reorder, "picker", new Class<?>[]{}), "F3 preset button did not open the shared picker");
      capture(mc, screen, size, "f3-presets");
    } finally { screen.removed(); }
  }

  private static void seedDebug(Object reorder) throws Exception {
    SettingsDraft draft = (SettingsDraft)invoke(reorder, "draft", new Class<?>[]{});
    var presets = new JsonArray();
    for (int i = 0; i < 12; i++) {
      var preset = new JsonObject(); preset.addProperty("id", "review-" + i); preset.addProperty("name", "F3 layout " + (i + 1));
      preset.add("left", draft.get("interface.debugOrder").value.deepCopy());
      preset.add("right", draft.get("interface.debugRightOrder").value.deepCopy());
      var values = new JsonObject(); values.addProperty("interface.debugTextSize", 1 + i / 20d);
      preset.add("values", values); presets.add(preset);
    }
    draft.get("interface.debugPresets").value = DebugPresets.checked(presets);
  }

  private static void music(Minecraft mc, PowerOptionsScreen options, int[] size) throws Exception {
    resetOptions(options, "Audio"); set(options, "libraryOpen", true);
    var presets = new ArrayList<MusicPreset>();
    for (int i = 0; i < 12; i++) presets.add(new MusicPreset("review-" + i, "Soundtrack " + (i + 1), Set.of(), Map.of(), Set.of()));
    options.session().settings().stream().filter(s -> s.id.equals("audio.presets")).findFirst().orElseThrow().value = Catalog.JSON.toJsonTree(presets);
    var library = (MusicLibraryScreen)get(options, "library");
    library.restore(new MusicLibraryScreen.State(false, "active", "", 0, 0));
    library.showTracks(); invoke(options, "layout", new Class<?>[]{});
    check((boolean)invoke(options, "libraryVisible", new Class<?>[]{}), "Audio staging did not show the music library");
    MusicFilters.Layout layout = (MusicFilters.Layout)invoke(library, "filters", new Class<?>[]{});
    if (layout.tabs().get(0).id().equals("cycle")) {
      var first = layout.tabs().get(0); input(library).power$click(first.x() + 8, first.y() + 8, 0);
    }
    capture(mc, options, size, "music-filters"); close(get(options, "choices"));
    invoke(library, "selectFilter", new Class<?>[]{String.class}, "presets");
    layout = (MusicFilters.Layout)invoke(library, "filters", new Class<?>[]{});
    input(library).power$click(layout.folderX() + 8, layout.folderY() + 8, 0);
    check((boolean)invoke(get(options, "choices"), "active", new Class<?>[]{}), "music preset selector did not open");
    capture(mc, options, size, "music-presets"); close(get(options, "choices"));
    set(library, "folders", List.of("music/Building", "music/Travel", "music/Quiet evenings"));
    invoke(library, "selectFilter", new Class<?>[]{String.class}, "folder");
    layout = (MusicFilters.Layout)invoke(library, "filters", new Class<?>[]{});
    input(library).power$click(layout.folderX() + 8, layout.folderY() + 8, 0);
    check((boolean)invoke(get(options, "choices"), "active", new Class<?>[]{}), "music folder selector did not open");
    capture(mc, options, size, "music-folders"); close(get(options, "choices"));
  }

  private static void waypoints(Minecraft mc, Screen previous, int[] size) throws Exception {
    var list = new WaypointsScreen(previous); list.init(mc, size[0], size[1]);
    try {
      Object layout = invoke(list, "layout", new Class<?>[]{});
      input(list).power$click((int)invoke(layout, "sortX", new Class<?>[]{}) + 8, 35, 0);
      capture(mc, list, size, "waypoint-sort"); close(get(list, "choices"));
      input(list).power$click((int)invoke(list, "left", new Class<?>[]{}) + 8, 57, 0);
      capture(mc, list, size, "waypoint-sets");
      var point = MapKeys.newWaypoint(); point.name = "Home";
      var editor = new WaypointEditorScreen(list, point); editor.init(mc, size[0], size[1]);
      try {
        invoke(editor, "beginSet", new Class<?>[]{}); capture(mc, editor, size, "waypoint-editor-set"); close(get(editor, "choices"));
        set(editor, "scroll", 25);
        int left = (int)invoke(editor, "left", new Class<?>[]{}), y = (int)invoke(editor, "row", new Class<?>[]{int.class}, 5);
        input(editor).power$click(left + 80, y + 8, 0); capture(mc, editor, size, "waypoint-editor-dimension");
      } finally { editor.removed(); }
    } finally { list.removed(); }
  }

  private static void sorting(Minecraft mc, PowerOptionsScreen options, int[] size) throws Exception {
    var editor = new SortingPreferencesScreen(options, options.session(), () -> {}); editor.init(mc, size[0], size[1]);
    try {
      var presets = SortingLayoutsData.read(new JsonObject());
      for (int i = 0; i < 12; i++) presets = presets.save("Inventory layout " + (i + 1), new JsonArray(), new JsonObject());
      set(editor, "presets", presets);
      input(editor).power$click((int)invoke(editor, "left", new Class<?>[]{}) + 8, (int)invoke(editor, "presetsTop", new Class<?>[]{}) + 8, 0);
      capture(mc, editor, size, "sorting-presets");
    } finally { editor.removed(); }
  }

  private static void creation(Minecraft mc, int[] size) throws Exception {
    var previous = mc.currentScreen; var create = new net.minecraft.class_180(null); create.init(mc, size[0], size[1]);
    try {
      for (int id : new int[]{2, 3}) {
        invoke(create, "power$select", new Class<?>[]{int.class}, id);
        check(mc.currentScreen instanceof ChoiceScreen, "native world creation did not open its choice popup");
        Screen popup = mc.currentScreen; popup.init(mc, size[0], size[1]);
        capture(mc, popup, size, id == 2 ? "creation-preset" : "creation-difficulty"); popup.removed(); mc.currentScreen = previous;
      }
    } finally { create.removed(); mc.currentScreen = previous; }
  }

  static void checkF3(Minecraft mc) throws Exception {
    var options = new PowerOptionsScreen(mc.currentScreen); options.init(mc, 640, 420);
    var editor = new HudEditorScreen(options, "interface.debugOrder"); editor.init(mc, 640, 420); editor.beginReorder();
    try {
      Object reorder = get(editor, "reorder"); seedDebug(reorder);
      invoke(reorder, "draw", new Class<?>[]{int.class, int.class, int.class, int.class, int.class, int.class}, 100, 60, 500, 290, -1000, -1000);
      var before = ((SettingsDraft)get(editor, "draft")).snapshot();
      input(editor).power$click((int)get(reorder, "left") + 8, (int)get(reorder, "toolbarTop") + 8, 0);
      input(editor).power$key('\0', Keyboard.KEY_END); input(editor).power$key('\0', Keyboard.KEY_RETURN);
      check((int)get(reorder, "presetIndex") == 11 && !(boolean)invoke(reorder, "picker", new Class<?>[]{}), "F3 keyboard selection failed");
      SettingsDraft selected = (SettingsDraft)invoke(reorder, "draft", new Class<?>[]{});
      check(Math.abs(selected.get("interface.debugTextSize").value.getAsDouble() - 1.55) < .00001, "F3 preset selection did not load its values");
      check(before.equals(((SettingsDraft)get(editor, "draft")).snapshot()), "F3 preset selection escaped its isolated draft");
    } finally { editor.removed(); options.removed(); }
  }

  static void checkSidebar(Minecraft mc) throws Exception {
    check(mc.world != null && Maps.cache() != null, "open a disposable mapped world for sidebar input checks");
    var map = new WorldMapScreen(); map.init(mc, 1000, 600);
    try {
      invoke(map, "toggleSettings", new Class<?>[]{});
      Object panel = get(map, "settingsPanel"); check(panel instanceof PowerOptionsScreen, "world-map settings sidebar did not open");
      Object popup = get(panel, "choices");
      var labels = java.util.stream.IntStream.range(0, 80).mapToObj(i -> "Sidebar choice " + i).toList();
      invoke(popup, "open", new Class<?>[]{List.class, List.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, java.util.function.IntConsumer.class},
          labels, List.of(), 0, -1, 10, 55, 180, 1000, 600, (java.util.function.IntConsumer)i -> {});
      int x = (int)get(popup, "x") + (int)get(popup, "width") - 5, y = (int)get(popup, "y");
      map.layerPress(x, y + 3, 0);
      Object bar = get(popup, "bar");
      check((boolean)get(map, "panelPress") && (boolean)get(bar, "dragging"), "sidebar failed to retain the dropdown scrollbar press");
      map.layerRelease(x + 200, y + 30, -1); map.layerRelease(x + 200, y + 30, 1);
      check((boolean)get(map, "panelPress") && (boolean)get(bar, "dragging"), "motion or another release ended the sidebar scrollbar drag");
      map.layerRelease(990, 599, 0);
      check(!(boolean)get(map, "panelPress") && !(boolean)get(bar, "dragging"), "originating release did not clear sidebar ownership");
      check((double)get(popup, "scroll") > 1000, "sidebar release outside the panel ignored its final coordinates");
    } finally { map.removed(); }
  }

  static void resetOptions(PowerOptionsScreen options, String page) throws Exception {
    close(get(options, "choices"));
    set(options, "page", page); set(options, "scroll", 0d); set(options, "sideScroll", 0d);
    set(options, "changedOnly", false); set(options, "conflictIds", List.of());
    set(options, "highlightedId", ""); set(options, "focusAnchorId", "");
    set(options, "selected", -1); set(options, "libraryOpen", false); set(options, "hidden", false); set(options, "error", "");
    set(options, "confirmClose", false); set(options, "confirmReset", false); set(options, "editorReturn", null);
    ((Deque<?>)get(options, "history")).clear(); ((Set<?>)get(options, "collapsed")).clear();
    TextInput search = (TextInput)get(options, "search"); search.setText(""); search.focused = false;
    invoke(options, "layout", new Class<?>[]{});
  }

  static void openSetting(PowerOptionsScreen options, Setting setting) throws Exception {
    resetOptions(options, setting.page);
    ((TextInput)get(options, "search")).setText(setting.label);
    invoke(options, "layout", new Class<?>[]{});
    Object target = null;
    for (Object row : (List<?>)get(options, "rows")) {
      Setting candidate = (Setting)invoke(row, "setting", new Class<?>[]{});
      if (candidate != null && candidate.id.equals(setting.id)) { target = row; break; }
    }
    check(target != null, "staged setting has no visible row: " + setting.id + " page=" + get(options, "page")
        + " query=" + ((TextInput)get(options, "search")).text());
    int rowY = (int)invoke(target, "y", new Class<?>[]{});
    Field headingHeight = Class.forName("local.luke.power.ui.SettingsGroup").getDeclaredField("HEIGHT"); headingHeight.setAccessible(true);
    set(options, "scroll", (double)Math.max(0, rowY - headingHeight.getInt(null))); invoke(options, "layout", new Class<?>[]{});
    int x = (int)invoke(options, "mainControlLeft", new Class<?>[]{target.getClass()}, target) + 8;
    int span = (int)invoke(options, "span", new Class<?>[]{});
    Method buttonOffset = Class.forName("local.luke.power.ui.SettingRows").getDeclaredMethod("buttonOffset", int.class); buttonOffset.setAccessible(true);
    int y = (int)invoke(options, "top", new Class<?>[]{}) + rowY - (int)(double)get(options, "scroll") + (int)buttonOffset.invoke(null, span) + 8;
    input(options).power$click(x, y, 0);
    check((boolean)invoke(get(options, "choices"), "active", new Class<?>[]{}), "staged row did not open its dropdown: " + setting.id);
  }

  private static void capture(Minecraft mc, Screen screen, int[] size, String name) throws Exception {
    String file = name + "-" + size[0] + ".png";
    Screen previous = mc.currentScreen; mc.currentScreen = screen;
    try { SettingsSnapshot.render(mc, screen, size[0], size[1], REPORT.resolve(file)); }
    finally { mc.currentScreen = previous; }
    captures.add(file);
    log("DROPDOWN450 screen=" + file);
  }
  private static ScreenInput input(Screen screen) { return (ScreenInput)(Object)screen; }
  private static void close(Object popup) throws Exception { invoke(popup, "close", new Class<?>[]{}); }
  static Object get(Object target, String name) throws Exception {
    for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) try {
      Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(target);
    } catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  static void set(Object target, String name, Object value) throws Exception {
    for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) try {
      Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(target, value); return;
    } catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
    for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) try {
      Method method = type.getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(target, args);
    } catch (NoSuchMethodException ignored) {}
    throw new NoSuchMethodException(name);
  }
}
