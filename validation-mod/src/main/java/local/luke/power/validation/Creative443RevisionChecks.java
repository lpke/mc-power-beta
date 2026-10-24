package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import com.google.gson.*;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.creative.CreativeTabsData;
import local.luke.power.creativeui.CreativeTabsBackend;
import local.luke.power.death.DeathItems;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Draft-only Ctrl selection/removal and one-step clear undo under both native release orders. */
final class Creative443RevisionChecks {
  private static Pointer pointer;
  private static final Set<Integer> SELECTED = Set.of(0, 1, 2, 3, 4, 5, 6, 7, 15);
  private static CreativeTabsData data(CreativeTabsScreen screen) throws Exception { return (CreativeTabsData)field(screen, "data"); }
  @SuppressWarnings("unchecked") private static Set<Integer> cells(CreativeTabsScreen screen) throws Exception { return (Set<Integer>)field(screen, "cells"); }
  private static Object invoke(CreativeTabsScreen screen, String name, Class<?>[] types, Object... values) throws Exception {
    Method method = screen.getClass().getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(screen, values);
  }
  private static int x(CreativeTabsScreen screen, int cell) throws Exception { return (int)call(screen, "gridLeft") + cell % 8 * 18 + 9; }
  private static int y(CreativeTabsScreen screen, int cell) throws Exception { return (int)call(screen, "gridTop") + cell / 8 * 18 + 9; }
  private static void motion(CreativeTabsScreen screen, int x, int y) throws Exception {
    screen.layerRelease(x, y, -1); invoke(screen, "updateDrag", new Class<?>[]{int.class, int.class}, x, y);
  }
  private static void undo(CreativeTabsScreen screen) throws Exception { invoke(screen, "undo", new Class<?>[0]); }
  private static String inventory(Minecraft mc) throws Exception { return DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)); }
  private static ByteBuffer keys() throws Exception {
    var field = Keyboard.class.getDeclaredField("keyDownBuffer"); field.setAccessible(true); return (ByteBuffer)field.get(null);
  }
  private static Setting setting() {
    var session = new ConfigSession(); CreativeTabsBackend.register(session); Setting setting = session.settings().get(0);
    CreativeTabsData draft = CreativeTabsData.read(CreativeTabsData.defaults()); draft.customized(true);
    List<String> slots = new ArrayList<>(Collections.nCopies(128, ""));
    for (int cell = 0; cell < 16; cell++) slots.set(cell, cell == 3 ? "missing:preserved" : (cell + 1) + ":0");
    draft.append(CreativeTabsData.tab("revision", "Revision draft", "1", slots)); setting.value = draft.json(); return setting;
  }
  private static JsonObject cleared(JsonObject original, int last) {
    CreativeTabsData draft = CreativeTabsData.read(original); List<String> slots = CreativeTabsData.slots(draft.tab(0));
    for (int cell = 0; cell <= last; cell++) slots.set(cell, ""); CreativeTabsData.slots(draft.tab(0), slots); return draft.json();
  }
  static void run(Minecraft mc, String action) throws Exception {
    if (!action.isEmpty() && !action.equals("run")) { nativePointer(mc, action); return; }
    check(pointer == null, "Stop the revision pointer editor first"); require(mc);
    check(!Mouse.isButtonDown(0) && !Mouse.isButtonDown(1), "Release pointer buttons before direct checks");
    Screen parent = mc.currentScreen; boolean repeat = Keyboard.areRepeatEventsEnabled(); ByteBuffer keys = keys();
    byte ctrl = keys.get(Keyboard.KEY_LCONTROL), rightCtrl = keys.get(Keyboard.KEY_RCONTROL);
    PowerConfig.flushPending(); byte[] disk = Files.readAllBytes(PowerConfig.path()); String items = inventory(mc); failures = 0;
    try {
      for (boolean ctrlFirst : List.of(true, false)) {
        test("CREATIVE443REV Ctrl-left deselects from selected origin, release Ctrl " + (ctrlFirst ? "first" : "last"), () -> {
          var setting = setting(); var editor = new CreativeTabsScreen(parent, setting, () -> { throw new AssertionError("Selection published"); });
          mc.setScreen(editor); editor.init(mc, 640, 420); JsonObject original = data(editor).json(); cells(editor).addAll(SELECTED);
          keys.put(Keyboard.KEY_LCONTROL, (byte)1); keys.put(Keyboard.KEY_RCONTROL, (byte)0);
          editor.layerPress(x(editor, 0), y(editor, 0), 0); editor.layerKey('\0', Keyboard.KEY_LCONTROL);
          motion(editor, x(editor, 3), y(editor, 3)); motion(editor, (int)call(editor, "gridLeft") - 5, y(editor, 3));
          editor.layerRelease(x(editor, 3), y(editor, 3), 1);
          if (ctrlFirst) keys.put(Keyboard.KEY_LCONTROL, (byte)0);
          editor.layerRelease(x(editor, 3), y(editor, 3), 0); keys.put(Keyboard.KEY_LCONTROL, (byte)0);
          check(cells(editor).equals(Set.of(4, 5, 6, 7, 15)), "Selected-origin Ctrl drag added cells, lost unrelated selection or canceled on another button");
          check(data(editor).json().equals(original) && setting.value.equals(original), "Deselect gesture edited item data or setting");
        });
        test("CREATIVE443REV Ctrl-right clears visited range with one Undo, release Ctrl " + (ctrlFirst ? "first" : "last"), () -> {
          var setting = setting(); var editor = new CreativeTabsScreen(parent, setting, () -> { throw new AssertionError("Clear published"); });
          mc.setScreen(editor); editor.init(mc, 640, 420); JsonObject original = data(editor).json();
          keys.put(Keyboard.KEY_LCONTROL, (byte)1); editor.layerPress(x(editor, 0), y(editor, 0), 1); editor.layerKey('\0', Keyboard.KEY_LCONTROL);
          motion(editor, x(editor, 3), y(editor, 3)); motion(editor, x(editor, 2), y(editor, 2)); motion(editor, x(editor, 5), y(editor, 5));
          editor.layerRelease(x(editor, 5), y(editor, 5), 0);
          // A wrong-button release must leave ownership until the originating right release.
          motion(editor, x(editor, 6), y(editor, 6));
          if (ctrlFirst) keys.put(Keyboard.KEY_LCONTROL, (byte)0);
          editor.layerRelease(x(editor, 6), y(editor, 6), 1); keys.put(Keyboard.KEY_LCONTROL, (byte)0);
          check(data(editor).json().equals(cleared(original, 6)), "Ctrl-right paint lost ownership, cleared extra cells or failed to clear its range");
          check(setting.value.equals(original), "Clear gesture escaped isolated draft");
          undo(editor); check(data(editor).json().equals(original), "One Undo failed to restore all cleared cells and unknown IDs");
          undo(editor); check(data(editor).json().equals(original), "Clear gesture created more than one content Undo step");
          keys.put(Keyboard.KEY_LCONTROL, (byte)1); editor.layerPress((int)call(editor, "gridLeft") - 5, y(editor, 0), 1);
          motion(editor, x(editor, 6), y(editor, 6)); editor.layerRelease(x(editor, 6), y(editor, 6), 1); keys.put(Keyboard.KEY_LCONTROL, (byte)0);
          check(data(editor).json().equals(original), "A press outside the grid cleared items after entering it");
        });
      }
      test("CREATIVE443REV Ctrl-left from an unselected origin still adds its range", () -> {
        var setting = setting(); var editor = new CreativeTabsScreen(parent, setting, () -> {}); mc.setScreen(editor); editor.init(mc, 320, 240);
        JsonObject original = data(editor).json(); cells(editor).addAll(Set.of(6, 15));
        keys.put(Keyboard.KEY_LCONTROL, (byte)1); editor.layerPress(x(editor, 0), y(editor, 0), 0);
        keys.put(Keyboard.KEY_LCONTROL, (byte)0); editor.layerRelease(x(editor, 3), y(editor, 3), 0);
        check(cells(editor).equals(Set.of(0, 1, 2, 3, 6, 15)) && data(editor).json().equals(original), "Unselected-origin Ctrl paint failed or replaced unrelated selection");
      });
      check(items.equals(inventory(mc)) && Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "Revision editor changed user inventory or persisted settings");
    } finally { keys.put(Keyboard.KEY_LCONTROL, ctrl); keys.put(Keyboard.KEY_RCONTROL, rightCtrl); mc.setScreen(parent); Keyboard.enableRepeatEvents(repeat); }
    log("CREATIVE443REV failures=" + failures); check(failures == 0, "Creative443 revision failures=" + failures);
  }
  private static void require(Minecraft mc) {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "Focus a disposable singleplayer world");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "Mute the disposable client");
  }
  private static void nativePointer(Minecraft mc, String action) throws Exception {
    if (action.startsWith("prepare ")) {
      require(mc); check(pointer == null, "Run creative-443-revision stop first");
      String name = action.substring(8).trim();
      check(Set.of("deselect-ctrl-first", "deselect-button-first", "clear-ctrl-first", "clear-button-first").contains(name), "Use prepare deselect|clear-ctrl-first|button-first");
      pointer = new Pointer(mc, name); pointer.logReady(mc); return;
    }
    check(pointer != null, "Prepare a revision pointer case first");
    if (action.equals("stop")) {
      Pointer old = pointer;
      try { old.isolated(mc); } finally { pointer = null; mc.setScreen(old.parent); Keyboard.enableRepeatEvents(old.repeat); }
      log("CREATIVE443REV POINTER stopped with native inventory/configuration unchanged"); return;
    }
    check(action.equals("check") || action.equals("hold-check") || action.equals("undo"), "Use check, hold-check, undo or stop");
    check(mc.currentScreen == pointer.screen, "Keep the revision editor open"); pointer.isolated(mc);
    if (action.equals("undo")) {
      check(pointer.clear, "Undo is for the clear case"); undo(pointer.screen);
      check(data(pointer.screen).json().equals(pointer.original), "Native clear was not one Undo step");
      undo(pointer.screen); check(data(pointer.screen).json().equals(pointer.original), "Native clear added multiple Undo steps");
    } else if (action.equals("hold-check")) {
      check(Mouse.isButtonDown(pointer.clear ? 1 : 0), "Hold the originating pointer button");
      if (!pointer.clear) check(data(pointer.screen).json().equals(pointer.original), "Native deselect changed item data");
    } else {
      check(!Mouse.isButtonDown(pointer.clear ? 1 : 0), "Release the originating pointer button");
      if (pointer.clear) check(data(pointer.screen).json().equals(cleared(pointer.original, 3)), "Native Ctrl-right did not clear exactly cells0..3");
      else check(cells(pointer.screen).equals(Set.of(4, 5, 6, 7, 15)) && data(pointer.screen).json().equals(pointer.original), "Native selected-origin Ctrl-left did not deselect exactly cells0..3");
    }
    log("PASS CREATIVE443REV POINTER " + pointer.name + " " + action + " inventory/settings isolated");
  }
  private static final class Pointer {
    final String name, items;
    final boolean clear, repeat;
    final Screen parent;
    final CreativeTabsScreen screen;
    final Setting setting;
    final JsonObject original;
    final byte[] disk;
    Pointer(Minecraft mc, String name) throws Exception {
      this.name = name; clear = name.startsWith("clear"); parent = mc.currentScreen; repeat = Keyboard.areRepeatEventsEnabled(); items = inventory(mc);
      PowerConfig.flushPending(); disk = Files.readAllBytes(PowerConfig.path()); setting = setting(); original = setting.value.getAsJsonObject().deepCopy();
      screen = new CreativeTabsScreen(parent, setting, () -> { throw new AssertionError("Native revision editor published"); }); mc.setScreen(screen);
      if (!clear) cells(screen).addAll(SELECTED);
    }
    void logReady(Minecraft mc) throws Exception {
      log("CREATIVE443REV POINTER READY " + name + " GUI=" + screen.width + "x" + screen.height + " display=" + mc.displayWidth + "x" + mc.displayHeight
          + " press=" + x(screen, 0) + "," + y(screen, 0) + " release=" + x(screen, 3) + "," + y(screen, 3)
          + "; Ctrl_L down, " + (clear ? "RIGHT" : "LEFT") + " down, move0to3; optional hold-check; "
          + (name.endsWith("ctrl-first") ? "release Ctrl BEFORE pointer" : "release pointer BEFORE Ctrl")
          + "; check" + (clear ? "; undo verifies one-step restoration" : "") + "; stop");
    }
    void isolated(Minecraft mc) throws Exception {
      check(items.equals(inventory(mc)) && original.equals(setting.value) && Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "Revision pointer changed native inventory/settings");
    }
  }
}
