package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import com.google.gson.*;
import java.nio.file.*;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
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

/** Regression for Beta motion releases, final pointer coordinates and isolated editor actions. */
final class Creative443Checks {
  private static Pointer pointer;
  private static CreativeTabsData data(CreativeTabsScreen screen) throws Exception { return (CreativeTabsData)field(screen, "data"); }
  private static ScrollBar.Track track(CreativeTabsScreen screen, String kind) throws Exception { return (ScrollBar.Track)call(screen, kind + "Track"); }
  private static boolean dragging(CreativeTabsScreen screen, String kind) throws Exception { return (boolean)field(field(screen, kind + "Bar"), "dragging"); }
  private static int scroll(CreativeTabsScreen screen, String kind) throws Exception { return (int)field(screen, kind + "Scroll"); }
  private static int maximum(CreativeTabsScreen screen, String kind) throws Exception {
    return kind.equals("list") ? (int)Math.round(track(screen, kind).maximum() / 23) : (int)track(screen, kind).maximum();
  }
  private static int footerX(CreativeTabsScreen screen, int index) throws Exception {
    Method method = screen.getClass().getDeclaredMethod("footerX", int.class); method.setAccessible(true);
    return (int)method.invoke(screen, index) + (int)call(screen, "footerWidth") / 2;
  }
  private static void click(CreativeTabsScreen screen, int x, int y) { screen.layerPress(x, y, 0); screen.layerRelease(x, y, 0); }
  private static void itemDrag(CreativeTabsScreen screen, int from, int to) throws Exception {
    int x = (int)call(screen, "gridLeft"), y = (int)call(screen, "gridTop");
    screen.layerPress(x + from % 8 * 18 + 9, y + from / 8 * 18 + 9, 0);
    screen.layerRelease(x + to % 8 * 18 + 9, y + to / 8 * 18 + 9, -1);
    screen.layerRelease(x + to % 8 * 18 + 9, y + to / 8 * 18 + 9, 0);
  }
  private static String inventory(Minecraft mc) throws Exception { return DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)); }
  private static Setting setting() {
    var session = new ConfigSession(); CreativeTabsBackend.register(session); var setting = session.settings().get(0);
    CreativeTabsData draft = CreativeTabsData.read(CreativeTabsData.defaults()); draft.customized(true);
    var slots = new ArrayList<>(Collections.nCopies(32 * 8, "")); slots.set(0, "1:0"); slots.set(8, "missing:retained");
    for (int tab = 0; tab < 30; tab++) draft.append(CreativeTabsData.tab("scroll-" + tab, "Scroll " + tab, "1", slots));
    setting.value = draft.json(); return setting;
  }
  static void run(Minecraft mc, String action) throws Exception {
    if (!action.isEmpty() && !action.equals("run")) { nativePointer(mc, action); return; }
    check(pointer == null, "Stop the Creative443 pointer editor first"); require(mc);
    check(!Mouse.isButtonDown(0), "Release the left button before direct polling regression");
    failures = 0; Screen parent = mc.currentScreen; boolean repeat = Keyboard.areRepeatEventsEnabled();
    PowerConfig.flushPending(); byte[] config = Files.readAllBytes(PowerConfig.path()); String items = inventory(mc);
    Path reports = Path.of("power-beta-data/reports/creative-443"); Files.createDirectories(reports);
    try {
      for (int[] size : new int[][]{{320, 240}, {640, 420}}) for (String kind : List.of("list", "grid")) {
        test("CREATIVE443 " + kind + " motion/nonleft releases preserve ownership and final coordinates at " + size[0], () -> {
          Setting setting = setting(); JsonElement original = setting.value.deepCopy();
          var editor = new CreativeTabsScreen(parent, setting, () -> { throw new AssertionError("Scrollbar published editor draft"); });
          mc.setScreen(editor); editor.init(mc, size[0], size[1]); JsonObject before = data(editor).json();
          ScrollBar.Track track = track(editor, kind); check(track.visible(), "Fixture scrollbar does not overflow");
          int x = track.x() + 1, start = (int)track.y() + track.thumb() / 2, middle = track.top() + track.height() / 2, end = track.top() + track.height() - 2;
          editor.layerPress(x, start, 0); check(dragging(editor, kind), "Left press did not own scrollbar");
          for (int button : new int[]{-1, 1, 2}) {
            editor.layerRelease(x, middle, button);
            check(dragging(editor, kind) && scroll(editor, kind) == 0, "Motion/nonleft event ended drag or applied release coordinates: " + button);
          }
          check(!Mouse.isButtonDown(0), "Direct render test requires physical polling false");
          SettingsSnapshot.render(mc, editor, size[0], size[1], reports.resolve(kind + "-held-" + size[0] + ".png"), x, middle);
          int interim = scroll(editor, kind);
          check(dragging(editor, kind) && interim > 0, "Physical polling false canceled native press ownership");
          editor.layerRelease(x, end, -1); check(dragging(editor, kind) && scroll(editor, kind) == interim, "Beta motion event canceled held scrollbar");
          editor.layerRelease(x, end, 0);
          check(!dragging(editor, kind) && scroll(editor, kind) == maximum(editor, kind), "Final left release did not apply final coordinates");
          editor.layerRelease(x, start, -1);
          SettingsSnapshot.render(mc, editor, size[0], size[1], reports.resolve(kind + "-released-" + size[0] + ".png"), x, start);
          check(!dragging(editor, kind) && scroll(editor, kind) == maximum(editor, kind), "Released scrollbar resumed on later motion");
          check(before.equals(data(editor).json()) && original.equals(setting.value), "Scrollbar changed editor draft or setting");
        });
      }
      test("CREATIVE443 plain occupied-cell drag moves immediately and empty drag stays quiet", () -> {
        Setting setting = setting(); var editor = new CreativeTabsScreen(parent, setting, () -> { throw new AssertionError("Item drag published"); });
        mc.setScreen(editor); editor.init(mc, 640, 420); JsonObject original = data(editor).json();
        List<String> source = CreativeTabsData.slots(data(editor).tab(0));
        itemDrag(editor, 0, 4);
        check(CreativeTabsData.slots(data(editor).tab(0)).equals(CreativeTabsData.moveCells(source, Set.of(0), 4))
            && ((Set<?>)field(editor, "cells")).equals(Set.of(4)), "Plain drag did not move an initially unselected item");
        click(editor, footerX(editor, 2), editor.height - 18); check(data(editor).json().equals(original), "Immediate move Undo lost draft bytes");
        ((Set<?>)field(editor, "cells")).clear(); itemDrag(editor, 2, 5);
        check(data(editor).json().equals(original) && !((Set<?>)field(editor, "cells")).equals(Set.of(2, 3, 4, 5)), "Empty plain drag painted selection or changed items");
      });
      test("CREATIVE443 Ctrl paint owns the whole gesture after Ctrl release", () -> {
        Setting setting = setting(); var editor = new CreativeTabsScreen(parent, setting, () -> { throw new AssertionError("Ctrl paint published"); });
        mc.setScreen(editor); editor.init(mc, 640, 420); JsonObject original = data(editor).json();
        var keyField = Keyboard.class.getDeclaredField("keyDownBuffer"); keyField.setAccessible(true); var keys = (ByteBuffer)keyField.get(null);
        byte ctrl = keys.get(Keyboard.KEY_LCONTROL);
        int x = (int)call(editor, "gridLeft"), y = (int)call(editor, "gridTop") + 9;
        try {
          keys.put(Keyboard.KEY_LCONTROL, (byte)1); editor.layerPress(x + 9, y, 0);
          check((boolean)field(editor, "selectingCells") && !(boolean)field(editor, "movingCells"), "Ctrl press did not choose selection ownership");
          // Beta drains its mouse batch before queued key events from the same physical chord.
          editor.layerKey('\0', Keyboard.KEY_LCONTROL);
          check((int)field(editor, "dragCell") == 0 && (boolean)field(editor, "selectingCells"), "Queued Ctrl key-down canceled its own captured mouse gesture");
          keys.put(Keyboard.KEY_LCONTROL, (byte)0); editor.layerRelease(x + 4 * 18 + 9, y, -1);
          editor.layerRelease(x + 4 * 18 + 9, y, 0);
          check(((Set<?>)field(editor, "cells")).equals(Set.of(0, 1, 2, 3, 4)) && data(editor).json().equals(original), "Releasing Ctrl before mouse changed paint into item movement");
          editor.layerPress(x + 9, y, 0);
          check((boolean)field(editor, "movingCells"), "Plain selected item press did not capture movement");
          for (int modifier : new int[]{Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT, Keyboard.KEY_LMENU, Keyboard.KEY_RMENU}) {
            editor.layerKey('\0', modifier);
            check((int)field(editor, "dragCell") == 0 && (boolean)field(editor, "movingCells") && !(boolean)field(editor, "selectingCells"), "Queued Shift/Alt changed or canceled captured item movement");
          }
          editor.layerRelease(x + 9, y + 18, -1); editor.layerRelease(x + 9, y + 18, 0);
          check(CreativeTabsData.slots(data(editor).tab(0)).equals(CreativeTabsData.moveCells(CreativeTabsData.slots(CreativeTabsData.read(original).tab(0)), Set.of(0, 1, 2, 3, 4), 8)), "Plain drag did not move the Ctrl-painted selection together");
        } finally { keys.put(Keyboard.KEY_LCONTROL, ctrl); }
      });
      test("CREATIVE443 visible name field and pencil edit, commit and Undo", () -> {
        Setting setting = setting(); var editor = new CreativeTabsScreen(parent, setting, () -> { throw new AssertionError("Name edit published without Save"); });
        mc.setScreen(editor); editor.init(mc, 640, 420);
        String original = data(editor).tab(0).get("title").getAsString();
        int pencil = (int)call(editor, "left") + (int)call(editor, "span") - 9;
        click(editor, pencil, 68);
        check((boolean)field(editor, "rename") && ((TextInput)field(editor, "title")).focused, "Pencil did not focus visible tab name input");
        ((TextInput)field(editor, "title")).setText("Edited through pencil"); click(editor, pencil, 68);
        check(!(boolean)field(editor, "rename") && data(editor).tab(0).get("title").getAsString().equals("Edited through pencil"), "Check button did not commit title");
        click(editor, footerX(editor, 2), editor.height - 18);
        check(data(editor).tab(0).get("title").getAsString().equals(original), "Footer Undo did not restore name");
        int name = (int)call(editor, "left") + (int)call(editor, "listWidth") + 30;
        click(editor, name, 68);
        check((boolean)field(editor, "rename") && ((TextInput)field(editor, "title")).focused, "Visible name field itself was not editable");
        editor.layerKey('\0', Keyboard.KEY_ESCAPE);
        check(!(boolean)field(editor, "rename") && ((TextInput)field(editor, "title")).text().equals(original), "Name Escape did not restore pending input");
        SettingsSnapshot.render(mc, editor, 640, 420, reports.resolve("name-footer-640.png"));
      });
      test("CREATIVE443 Reset Cancel Undo Save footer preserves drafts and commits once", () -> {
        for (int[] size : new int[][]{{320, 240}, {640, 420}}) {
          Setting setting = setting(); JsonElement before = setting.value.deepCopy(); int[] accepted = {0};
          var editor = new CreativeTabsScreen(parent, setting, () -> accepted[0]++); mc.setScreen(editor); editor.init(mc, size[0], size[1]);
          JsonObject draft = data(editor).json(); int y = editor.height - 18;
          int previous = -1;
          for (int index = 0; index < 4; index++) { int x = footerX(editor, index); check(x > previous && x < editor.width, "Footer order/bounds overlap"); previous = x; }
          click(editor, footerX(editor, 0), y);
          check(data(editor).size() == 6 && accepted[0] == 0 && setting.value.equals(before), "Footer Reset bypassed draft isolation");
          click(editor, footerX(editor, 1), y);
          check(mc.currentScreen == editor && (boolean)call(field(editor, "exit"), "active"), "Cancel bypassed dirty exit prompt");
          editor.layerKey('\0', Keyboard.KEY_ESCAPE); click(editor, footerX(editor, 2), y);
          check(data(editor).json().equals(draft), "Footer Undo failed to reverse Reset");
          click(editor, footerX(editor, 0), y); click(editor, footerX(editor, 3), y);
          check(mc.currentScreen == parent && accepted[0] == 1 && CreativeTabsData.read(setting.value.getAsJsonObject()).size() == 6, "Footer Save did not return exactly one draft");
        }
      });
      check(items.equals(inventory(mc)) && Arrays.equals(config, Files.readAllBytes(PowerConfig.path())), "Creative443 editor changed native inventory or persisted settings");
    } finally { mc.setScreen(parent); Keyboard.enableRepeatEvents(repeat); }
    log("CREATIVE443 failures=" + failures + " captures=" + reports); check(failures == 0, "Creative443 failures=" + failures);
  }
  private static void require(Minecraft mc) {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "Focus a disposable singleplayer world");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "Mute the disposable client");
  }
  private static void nativePointer(Minecraft mc, String action) throws Exception {
    if (action.startsWith("prepare ")) {
      check(pointer == null, "Run creative-443 stop first"); require(mc);
      String kind = action.substring(8).trim(); check(Set.of("list", "grid", "item", "paint").contains(kind), "Use prepare list|grid|item|paint");
      pointer = new Pointer(mc, kind); pointer.logReady(mc); return;
    }
    check(pointer != null, "Run creative-443 prepare list|grid|item|paint first");
    if (action.equals("stop")) {
      Pointer old = pointer;
      try { old.unchanged(mc); } finally { pointer = null; mc.setScreen(old.parent); Keyboard.enableRepeatEvents(old.repeat); }
      log("CREATIVE443 POINTER stopped with inventory, draft, setting and disk unchanged"); return;
    }
    check(action.equals("check") || action.equals("hold-check"), "Use check, hold-check or stop");
    check(mc.currentScreen == pointer.screen, "Keep native pointer editor open");
    if (pointer.kind.equals("item") || pointer.kind.equals("paint")) {
      pointer.isolated(mc);
      if (action.equals("hold-check")) {
        check((int)field(pointer.screen, "dragCell") >= 0 && data(pointer.screen).json().equals(pointer.draft), "Native drag published before left release");
        check((boolean)field(pointer.screen, pointer.kind.equals("item") ? "movingCells" : "selectingCells"), "Native press chose the wrong drag owner");
      } else {
        check(!Mouse.isButtonDown(0) && (int)field(pointer.screen, "dragCell") == -1, "Release left button before check");
        CreativeTabsData value = CreativeTabsData.read(pointer.draft);
        if (pointer.kind.equals("item")) CreativeTabsData.slots(value.tab(0), CreativeTabsData.moveCells(CreativeTabsData.slots(value.tab(0)), Set.of(0), 4));
        JsonObject expected = value.json();
        check(data(pointer.screen).json().equals(expected), "Native drag changed unexpected cells");
        Set<Integer> cells = pointer.kind.equals("item") ? Set.of(4) : Set.of(0, 1, 2, 3, 4);
        check(((Set<?>)field(pointer.screen, "cells")).equals(cells), "Native drag selection differs from captured gesture");
        pointer.expectedDraft = expected;
      }
      log("PASS CREATIVE443 POINTER " + pointer.kind + " " + action + " native drag owner and isolated inventory/settings"); return;
    }
    pointer.unchanged(mc);
    if (action.equals("hold-check")) {
      check(dragging(pointer.screen, pointer.kind) && scroll(pointer.screen, pointer.kind) > 0, "Native motion ended scrollbar ownership");
    } else {
      check(!Mouse.isButtonDown(0) && !dragging(pointer.screen, pointer.kind), "Release the left button before check");
      check(scroll(pointer.screen, pointer.kind) == maximum(pointer.screen, pointer.kind), "Native pointer drag did not reach final scroll position");
    }
    log("PASS CREATIVE443 POINTER " + pointer.kind + " " + action + " scroll=" + scroll(pointer.screen, pointer.kind) + " isolated inventory/draft/settings");
  }
  private static final class Pointer {
    final String kind, items;
    final Screen parent;
    final CreativeTabsScreen screen;
    final Setting setting;
    final JsonObject draft;
    JsonObject expectedDraft;
    final JsonElement original;
    final byte[] disk;
    final boolean repeat;
    Pointer(Minecraft mc, String kind) throws Exception {
      this.kind = kind; parent = mc.currentScreen; repeat = Keyboard.areRepeatEventsEnabled(); items = inventory(mc);
      PowerConfig.flushPending(); disk = Files.readAllBytes(PowerConfig.path()); setting = setting(); original = setting.value.deepCopy();
      screen = new CreativeTabsScreen(parent, setting, () -> { throw new AssertionError("Pointer scrollbar published"); }); mc.setScreen(screen); draft = data(screen).json(); expectedDraft = draft.deepCopy();
    }
    void logReady(Minecraft mc) throws Exception {
      if (kind.equals("item") || kind.equals("paint")) {
        int x = (int)call(screen, "gridLeft"), y = (int)call(screen, "gridTop") + 9;
        log("CREATIVE443 POINTER READY " + kind + " GUI=" + screen.width + "x" + screen.height + " display=" + mc.displayWidth + "x" + mc.displayHeight
            + " press=" + (x + 9) + "," + y + " release=" + (x + 4 * 18 + 9) + "," + y
            + (kind.equals("paint") ? "; Ctrl_L down BEFORE leftpress; move with Ctrl, optional hold-check, RELEASE Ctrl BEFORE leftbutton, check, stop" : "; plain leftdrag without prior selection; optional hold-check, releaseleft, check, stop")); return;
      }
      ScrollBar.Track track = track(screen, kind); check(track.visible(), "Native pointer scrollbar is not overflowing");
      log("CREATIVE443 POINTER READY " + kind + " GUI=" + screen.width + "x" + screen.height + " display=" + mc.displayWidth + "x" + mc.displayHeight
          + " press=" + (track.x() + 1) + "," + ((int)track.y() + track.thumb() / 2)
          + " move=" + (track.x() + 1) + "," + (track.top() + track.height() / 2)
          + " release=" + (track.x() + 1) + "," + (track.top() + track.height() - 2)
          + " expectedScroll=" + maximum(screen, kind) + "; drag left, optional hold-check mid-drag, check after release; stop");
    }
    void unchanged(Minecraft mc) throws Exception {
      isolated(mc); check(expectedDraft.equals(data(screen).json()), "Pointer gesture changed unexpected draft cells");
    }
    void isolated(Minecraft mc) throws Exception {
      check(items.equals(inventory(mc)) && original.equals(setting.value)
          && Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "Pointer editor changed native inventory or persisted settings");
    }
  }
}
