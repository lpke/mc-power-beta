package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.lang.reflect.Method;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.creative.CreativeTabsData;
import local.luke.power.death.DeathItems;
import local.luke.power.creativeui.CreativeTabsBackend;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;

/** Native editor controls exercise an isolated draft; no gameplay slot or setting is published. */
final class Creative442Checks {
  private static CreativeTabsScreen pointer;
  private static Screen pointerParent;
  private static Setting pointerSetting;
  private static JsonElement pointerOriginal;
  private static String pointerItems;
  private static JsonObject pointerDocument;
  private static void invoke(Object owner, String name, Class<?>[] types, Object... values) throws Exception {
    Method method = owner.getClass().getDeclaredMethod(name, types); method.setAccessible(true); method.invoke(owner, values);
  }
  private static CreativeTabsData data(CreativeTabsScreen screen) throws Exception { return (CreativeTabsData)field(screen, "data"); }
  private static Set<?> selected(CreativeTabsScreen screen) throws Exception { return (Set<?>)field(screen, "cells"); }
  private static void click(CreativeTabsScreen screen, int cell, int button) throws Exception {
    int x = (int)call(screen, "gridLeft") + cell % 8 * 18 + 9, y = (int)call(screen, "gridTop") + cell / 8 * 18 + 9;
    ((ScreenInput)(Object)screen).power$click(x, y, button); ((ScreenInput)(Object)screen).power$release(x, y, button);
  }
  private static void drag(CreativeTabsScreen screen, int from, int to) throws Exception {
    int left = (int)call(screen, "gridLeft"), top = (int)call(screen, "gridTop");
    ((ScreenInput)(Object)screen).power$click(left + from % 8 * 18 + 9, top + from / 8 * 18 + 9, 0);
    ((ScreenInput)(Object)screen).power$release(left + to % 8 * 18 + 9, top + to / 8 * 18 + 9, 0);
  }
  private static String inventory(Minecraft mc) throws Exception {
    return DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
  }
  private static Setting setting() throws Exception {
    var session = new ConfigSession(); CreativeTabsBackend.register(session); var setting = session.settings().get(0);
    var layout = CreativeTabsData.read(CreativeTabsData.defaults()); layout.customized(true);
    List<String> slots = new ArrayList<>(Collections.nCopies(72, ""));
    slots.set(0, "1:0"); slots.set(1, "2:0"); slots.set(3, "missing:retained");
    for (int i = 0; i < 18; i++) layout.append(CreativeTabsData.tab("fixture-" + i, "Fixture " + i, "1", slots));
    setting.value = layout.json(); return setting;
  }
  static void run(Minecraft mc, String action) throws Exception {
    if (!action.isEmpty() && !action.equals("run")) { pointer(mc, action); return; }
    check(pointer == null, "Stop the native pointer editor before running checks");
    Screen parent = mc.currentScreen; String items = inventory(mc); byte[] disk = Files.readAllBytes(PowerConfig.path());
    failures = 0;
    Setting setting = setting(); JsonElement before = setting.value.deepCopy(); int[] accepted = {0};
    var editor = new CreativeTabsScreen(parent, setting, () -> accepted[0]++);
    try {
      mc.setScreen(editor); editor.init(mc, 640, 420);
      test("CREATIVE443 plain drag moves immediately; Ctrl drag selects and keeps multi-item movement", () -> {
        JsonObject original = data(editor).json(); List<String> initial = CreativeTabsData.slots(data(editor).tab(0)); drag(editor, 0, 4);
        check(selected(editor).equals(Set.of(4)) && CreativeTabsData.slots(data(editor).tab(0)).equals(CreativeTabsData.moveCells(initial, Set.of(0), 4)), "Plain occupied-cell drag required prior selection");
        invoke(editor, "undo", new Class<?>[0]);
        var keysField = Keyboard.class.getDeclaredField("keyDownBuffer"); keysField.setAccessible(true); var keys = (ByteBuffer)keysField.get(null);
        byte previous = keys.get(Keyboard.KEY_LCONTROL);
        try { keys.put(Keyboard.KEY_LCONTROL, (byte)1); drag(editor, 0, 3); }
        finally { keys.put(Keyboard.KEY_LCONTROL, previous); }
        check(selected(editor).equals(Set.of(0, 1, 2, 3)) && data(editor).json().equals(original), "Ctrl drag moved data instead of selecting");
        List<String> source = CreativeTabsData.slots(data(editor).tab(0)); drag(editor, 0, 4);
        check(CreativeTabsData.slots(data(editor).tab(0)).equals(CreativeTabsData.moveCells(source, Set.of(0, 1, 2, 3), 4)), "Selected item drag lost cells/holes/unknown IDs");
        invoke(editor, "undo", new Class<?>[0]); check(data(editor).json().equals(original), "Move undo did not restore exact layout");
      });
      test("CREATIVE442 single click selects and native timestamp double-click edits", () -> {
        click(editor, 0, 0); check(mc.currentScreen == editor && selected(editor).equals(Set.of(0)), "Single click opened picker or lost selection");
        long now = Mouse.getEventNanoseconds();
        ((SlotDoubleClick)field(editor, "clicks")).press(0, 0, now - 500_000_000L);
        click(editor, 0, 0); check(mc.currentScreen == editor, "Expired native timestamp pair opened picker");
        ((SlotDoubleClick)field(editor, "clicks")).press(0, 0, now - 100_000_000L);
        click(editor, 0, 0); check(mc.currentScreen instanceof ItemPickerScreen, "Double-click did not open shared item picker");
        invoke(mc.currentScreen, "choose", new Class<?>[]{String.class}, "264");
        check(mc.currentScreen == editor && CreativeTabsData.slots(data(editor).tab(0)).get(0).equals("264:0"), "Picker did not return exactly one draft edit");
        invoke(editor, "undo", new Class<?>[0]); check(CreativeTabsData.slots(data(editor).tab(0)).get(0).equals("1:0"), "Picker undo lost prior item");
      });
      test("CREATIVE442 right clear, row removal and Reset undo exact draft bytes", () -> {
        JsonObject original = data(editor).json(); click(editor, 3, 1);
        check(CreativeTabsData.slots(data(editor).tab(0)).get(3).isEmpty(), "Right-click did not clear unknown item");
        invoke(editor, "undo", new Class<?>[0]); check(data(editor).json().equals(original), "Clear undo lost unknown item ID");
        for (int row = 9; row > 1; row--) invoke(editor, "removeRow", new Class<?>[0]);
        check(CreativeTabsData.slots(data(editor).tab(0)).size() == 8, "Remove row did not allow fewer than eight rows");
        invoke(editor, "removeRow", new Class<?>[0]); check(CreativeTabsData.slots(data(editor).tab(0)).size() == 8, "Remove row removed last row");
        for (int row = 1; row < 9; row++) invoke(editor, "undo", new Class<?>[0]);
        check(data(editor).json().equals(original), "Row removal undo did not restore exact rows");
        var footer = CreativeTabsScreen.class.getDeclaredMethod("footerX", int.class); footer.setAccessible(true); int reset = (int)footer.invoke(editor, 0); ((ScreenInput)(Object)editor).power$click(reset + 4, editor.height - 24, 0);
        check(data(editor).size() == 6, "Reset did not restore default tabs");
        invoke(editor, "undo", new Class<?>[0]); check(data(editor).json().equals(original), "Reset was not undoable");
      });
      test("CREATIVE442 pending and committed titles undo without discarding earlier edits", () -> {
        String original = data(editor).tab(0).get("title").getAsString();
        field(editor, "rename", true); ((TextInput)field(editor, "title")).setText("Pending title");
        invoke(editor, "undo", new Class<?>[0]); check(((TextInput)field(editor, "title")).text().equals(original), "Undo did not cancel pending title");
        field(editor, "rename", true); ((TextInput)field(editor, "title")).setText("Committed title"); invoke(editor, "commitTitle", new Class<?>[0]);
        check(data(editor).tab(0).get("title").getAsString().equals("Committed title"), "Title did not commit");
        invoke(editor, "undo", new Class<?>[0]); check(data(editor).tab(0).get("title").getAsString().equals(original), "Committed title undo failed");
      });
      test("CREATIVE442 rejected title stays editable with feedback and never closes Save", () -> {
        CreativeTabsData draft = data(editor); String oldTitle = draft.tab(0).get("title").getAsString();
        JsonObject full = draft.json(); full.addProperty("fixturePadding", "");
        int available = 1048576 - full.toString().getBytes(StandardCharsets.UTF_8).length - 8;
        full.addProperty("fixturePadding", "a".repeat(available));
        field(editor, "data", CreativeTabsData.read(full)); field(editor, "rename", true);
        String entered = "A longer tab title that must stay visible after rejection";
        ((TextInput)field(editor, "title")).setText(entered);
        try {
          invoke(editor, "save", new Class<?>[0]);
          check(mc.currentScreen == editor && (boolean)field(editor, "rename") && ((TextInput)field(editor, "title")).text().equals(entered), "Rejected title closed Save or discarded the pending text");
          check(!((String)field(editor, "error")).isEmpty() && data(editor).json().equals(full), "Rejected title lost feedback or mutated the draft");
          check(setting.value.equals(before) && accepted[0] == 0, "Rejected title published a setting");
        } finally { field(editor, "data", draft); field(editor, "rename", false); field(editor, "error", ""); ((TextInput)field(editor, "title")).setText(oldTitle); }
      });
      test("CREATIVE442 Fill group popup paints over native item icons and restores depth state", () -> {
        editor.init(mc, 640, 420); Files.createDirectories(Path.of("power-beta-data/reports/creative-442"));
        CreativeTabsData draft = data(editor); JsonObject clear = draft.json();
        for (var value : clear.getAsJsonArray("tabs")) {
          JsonObject tab = value.getAsJsonObject(); tab.addProperty("icon", "missing:retained");
          CreativeTabsData.slots(tab, Collections.nCopies(CreativeTabsData.slots(tab).size(), ""));
        }
        Screen probe = new Screen() {
          @Override public void render(int x, int y, float delta) {
            GL11.glEnable(GL11.GL_DEPTH_TEST); editor.render(x, y, delta);
          }
        };
        probe.init(mc, 640, 420); field(editor, "nativeGroups", true);
        Path icons = Path.of("power-beta-data/reports/creative-442/fill-over-icons.png"), empty = Path.of("power-beta-data/reports/creative-442/fill-no-icons.png");
        try {
          SettingsSnapshot.render(mc, probe, 640, 420, icons);
          check(GL11.glIsEnabled(GL11.GL_DEPTH_TEST), "Popup leaked disabled depth state");
          field(editor, "data", CreativeTabsData.read(clear)); SettingsSnapshot.render(mc, probe, 640, 420, empty);
          BufferedImage a = ImageIO.read(icons.toFile()), b = ImageIO.read(empty.toFile());
          int x = ((int)call(editor, "left") + (int)call(editor, "listWidth")) * 2;
          int span = Math.min(180, editor.width - x / 2) * 2;
          check(Arrays.equals(a.getRGB(x, 104, span, 280, null, 0, span), b.getRGB(x, 104, span, 280, null, 0, span)), "Underlying item icons pierced Fill group popup");
        } finally { field(editor, "data", draft); field(editor, "nativeGroups", false); }
      });
      test("CREATIVE442 scrollbars and compact footer fit narrow and wide screens", () -> {
        Files.createDirectories(Path.of("power-beta-data/reports/creative-442"));
        for (int[] size : new int[][]{{320, 240}, {640, 420}}) {
          editor.init(mc, size[0], size[1]);
          var list = (ScrollBar.Track)call(editor, "listTrack"); var grid = (ScrollBar.Track)call(editor, "gridTrack");
          check(list.visible() && grid.x() + 6 <= editor.width && grid.top() + grid.height() < (int)call(editor, "rowControlsY"), "Scrollbar overlaps content controls or leaves screen");
          SettingsSnapshot.render(mc, editor, size[0], size[1], Path.of("power-beta-data/reports/creative-442/editor-" + size[0] + ".png"));
        }
      });
      check(before.equals(setting.value) && accepted[0] == 0 && items.equals(inventory(mc)), "Draft editor published or changed native inventory");
      check(Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "Draft editor wrote configuration");
    } finally { mc.setScreen(parent); }
    log("CREATIVE442 failures=" + failures); check(failures == 0, "Creative442 failures=" + failures);
  }
  private static void pointer(Minecraft mc, String action) throws Exception {
    if (action.equals("editor")) {
      check(pointer == null, "Stop current pointer editor first"); pointerParent = mc.currentScreen; pointerSetting = setting();
      pointerOriginal = pointerSetting.value.deepCopy(); pointerItems = inventory(mc);
      pointer = new CreativeTabsScreen(pointerParent, pointerSetting, () -> { throw new AssertionError("Pointer draft published"); }); mc.setScreen(pointer);
      pointerDocument = data(pointer).json();
      log("CREATIVE442 POINTER ready GUI=" + pointer.width + "x" + pointer.height + " display=" + mc.displayWidth + "x" + mc.displayHeight
          + " grid=" + call(pointer, "gridLeft") + "," + call(pointer, "gridTop") + " cell18. First Ctrl-left-drag cell0 to3, release Ctrl, check select; then drag0to4, check move; singleclickcell4, check single; doubleclickcell4, check double; stop."); return;
    }
    check(pointer != null, "Run creative-442 editor first");
    if (action.equals("stop")) { try { untouched(mc); } finally { mc.setScreen(pointerParent); pointer = null; } return; }
    check(action.startsWith("check "), "Use editor, check select|move|single|double, stop"); String stage = action.substring(6);
    switch (stage) {
      case "select" -> check(selected(pointer).equals(Set.of(0, 1, 2, 3)) && data(pointer).json().equals(pointerDocument), "Native Ctrl drag moved items");
      case "move" -> {
        List<String> original = CreativeTabsData.slots(CreativeTabsData.read(pointerDocument).tab(0));
        check(CreativeTabsData.slots(data(pointer).tab(0)).equals(CreativeTabsData.moveCells(original, Set.of(0, 1, 2, 3), 4)), "Native selected drag failed");
      }
      case "single" -> check(mc.currentScreen == pointer && selected(pointer).equals(Set.of(4)), "Native singleclick opened picker or failed selection");
      case "double" -> check(mc.currentScreen instanceof ItemPickerScreen, "Native doubleclick failed to open picker");
      default -> throw new IllegalArgumentException(stage);
    }
    untouched(mc); log("PASS CREATIVE442 POINTER " + stage + " native event and isolated inventory/configuration");
  }
  private static void untouched(Minecraft mc) throws Exception {
    check(pointerItems.equals(inventory(mc)) && pointerOriginal.equals(pointerSetting.value), "Pointer editor changed inventory or published draft");
  }
}
