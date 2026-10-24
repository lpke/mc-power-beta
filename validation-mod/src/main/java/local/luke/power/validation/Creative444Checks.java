package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;

import com.google.gson.JsonObject;
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
import org.lwjgl.opengl.*;

/** Isolated editor gestures and framing checks for the 4.4.4 review. */
final class Creative444Checks {
  private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
    Method method = target.getClass().getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(target, args);
  }
  private static CreativeTabsData data(CreativeTabsScreen screen) throws Exception { return (CreativeTabsData)field(screen, "data"); }
  @SuppressWarnings("unchecked") private static Set<Integer> cells(CreativeTabsScreen screen) throws Exception { return (Set<Integer>)field(screen, "cells"); }
  private static int x(CreativeTabsScreen screen, int cell) throws Exception { return (int)call(screen, "gridLeft") + cell % 8 * 18 + 9; }
  private static int y(CreativeTabsScreen screen, int cell) throws Exception { return (int)call(screen, "gridTop") + cell / 8 * 18 + 9; }
  private static void motion(CreativeTabsScreen screen, int x, int y) throws Exception {
    screen.layerRelease(x, y, -1); invoke(screen, "updateDrag", new Class<?>[]{int.class, int.class}, x, y);
  }
  private static Setting setting() {
    ConfigSession session = new ConfigSession(); CreativeTabsBackend.register(session); Setting setting = session.settings().get(0);
    CreativeTabsData draft = CreativeTabsData.read(CreativeTabsData.defaults()); draft.customized(true);
    List<String> slots = new ArrayList<>(Collections.nCopies(128, ""));
    for (int cell = 0; cell < 16; cell++) slots.set(cell, cell == 3 || cell == 15 ? "missing:retained" : (cell + 1) + ":0");
    slots.set(2, "35:14"); slots.set(9, "18:0");
    draft.append(CreativeTabsData.tab("444-fixture", "Editable tab", "2", slots)); setting.value = draft.json(); return setting;
  }
  private static CreativeTabsScreen open(Minecraft mc, Screen parent, Setting setting, int width, int height) {
    CreativeTabsScreen screen = new CreativeTabsScreen(parent, setting, () -> { throw new AssertionError("Fixture published an editor draft"); });
    mc.setScreen(screen); screen.init(mc, width, height); return screen;
  }
  private static String inventory(Minecraft mc) throws Exception {
    return DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
  }
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "Focus a disposable singleplayer world");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "Mute the disposable client");
    check(!Mouse.isButtonDown(0) && !Mouse.isButtonDown(1), "Release pointer buttons before direct checks");
    check(Display.getWidth() >= 1280 && Display.getHeight() >= 840, "Resize the disposable client to at least 1280x840 for captures");
    var keyField = Keyboard.class.getDeclaredField("keyDownBuffer"); keyField.setAccessible(true); ByteBuffer keys = (ByteBuffer)keyField.get(null);
    Map<Integer, Byte> modifiers = new LinkedHashMap<>();
    for (int key : new int[]{Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL, Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT}) {
      modifiers.put(key, keys.get(key)); keys.put(key, (byte)0);
    }
    Screen parent = mc.currentScreen; boolean repeat = Keyboard.areRepeatEventsEnabled(); int matrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    PowerConfig.flushPending(); byte[] config = Files.readAllBytes(PowerConfig.path()); String inventory = inventory(mc); failures = 0;
    Path reports = Path.of("power-beta-data/reports/creative-444"); Files.createDirectories(reports);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      for (int[] size : new int[][]{{320, 240}, {640, 420}}) {
        test("CREATIVE444 direct name field and focused check at " + size[0], () -> {
          Setting setting = setting(); CreativeTabsScreen screen = open(mc, parent, setting, size[0], size[1]);
          JsonObject original = data(screen).json(); TextInput title = (TextInput)field(screen, "title");
          int full = (int)call(screen, "span") - (int)call(screen, "listWidth") - 24;
          check(!(boolean)field(screen, "rename") && !title.focused && (int)call(screen, "nameWidth") == full,
              "Idle name field reserves a pencil/check button or starts focused");
          SettingsSnapshot.render(mc, screen, size[0], size[1], reports.resolve("name-idle-" + size[0] + ".png"));
          int nameLeft = (int)call(screen, "left") + (int)call(screen, "listWidth") + 24;
          screen.layerPress(nameLeft + 3, 66, 0); screen.layerRelease(nameLeft + 3, 66, 0);
          check((boolean)field(screen, "rename") && title.focused && (int)call(screen, "nameWidth") == full - 22,
              "Clicking the name did not focus it and reserve only the check control");
          title.setText("Renamed fixture");
          SettingsSnapshot.render(mc, screen, size[0], size[1], reports.resolve("name-focused-" + size[0] + ".png"));
          check(data(screen).json().equals(original) && setting.value.equals(original), "Pending name escaped its draft");
          int checkX = (int)call(screen, "left") + (int)call(screen, "span") - 9;
          screen.layerPress(checkX, 66, 0); screen.layerRelease(checkX, 66, 0);
          check(!(boolean)field(screen, "rename") && !title.focused && (int)call(screen, "nameWidth") == full
              && data(screen).tab(0).get("title").getAsString().equals("Renamed fixture"), "Focused check did not commit and restore idle width");
          call(screen, "undo"); check(data(screen).json().equals(original) && setting.value.equals(original), "Name Undo or draft isolation failed");
          call(screen, "addTab"); int selected = (int)field(screen, "selected");
          check(data(screen).tab(selected).get("icon").getAsString().equals("264"), "New tab did not use the diamond icon");
          check(CreativeTabsData.slots(data(screen).tab(selected)).stream().allMatch(String::isEmpty), "New tab contains unexpected items");
          call(screen, "undo"); check(data(screen).json().equals(original), "New-tab Undo changed prior tab data");
        });
        for (String gesture : List.of("plain", "ctrl-first", "ctrl-last")) {
          test("CREATIVE444 right clear " + gesture + " commits once on owner release at " + size[0], () -> {
            Setting setting = setting(); CreativeTabsScreen screen = open(mc, parent, setting, size[0], size[1]);
            JsonObject original = data(screen).json(); keys.put(Keyboard.KEY_LCONTROL, (byte)(gesture.equals("plain") ? 0 : 1));
            try {
              screen.layerPress(x(screen, 0), y(screen, 0), 1);
              motion(screen, x(screen, 3), y(screen, 3)); motion(screen, (int)call(screen, "gridLeft") - 6, y(screen, 3));
              motion(screen, x(screen, 2), y(screen, 2)); motion(screen, x(screen, 6), y(screen, 6));
              screen.layerRelease(x(screen, 9), y(screen, 9), 0); screen.layerRelease(x(screen, 9), y(screen, 9), 2);
              check((boolean)field(screen, "clearingCells") && data(screen).json().equals(original)
                  && !(boolean)call(field(screen, "history"), "available"), "Clear wrote before the owner release or lost ownership");
              if (gesture.equals("ctrl-first")) keys.put(Keyboard.KEY_LCONTROL, (byte)0);
              screen.layerRelease(x(screen, 7), y(screen, 7), 1); keys.put(Keyboard.KEY_LCONTROL, (byte)0);
              CreativeTabsData expected = CreativeTabsData.read(original); List<String> slots = CreativeTabsData.slots(expected.tab(0));
              for (int cell = 0; cell <= 7; cell++) slots.set(cell, ""); CreativeTabsData.slots(expected.tab(0), slots);
              check(data(screen).json().equals(expected.json()) && !(boolean)field(screen, "clearingCells"),
                  "Plain/Ctrl clear omitted final release coordinates, cleared extra cells or lost unavailable values");
              check(setting.value.equals(original), "Clear published a setting");
              call(screen, "undo"); check(data(screen).json().equals(original), "One Undo did not restore the whole gesture and unknown IDs");
              call(screen, "undo"); check(data(screen).json().equals(original), "Clear created multiple Undo records");
              screen.layerPress((int)call(screen, "gridLeft") - 5, y(screen, 0), 1);
              motion(screen, x(screen, 3), y(screen, 3)); screen.layerRelease(x(screen, 3), y(screen, 3), 1);
              check(data(screen).json().equals(original), "Right drag starting outside the grid cleared items");
            } finally { keys.put(Keyboard.KEY_LCONTROL, (byte)0); }
          });
        }
      }
      test("CREATIVE444 grouped preview retains every source, spacing and one-drop Undo", () -> {
        Setting setting = setting(); CreativeTabsScreen screen = open(mc, parent, setting, 640, 420); JsonObject original = data(screen).json();
        Set<Integer> selected = Set.of(0, 2, 9); cells(screen).addAll(selected); List<String> source = CreativeTabsData.slots(data(screen).tab(0));
        screen.layerPress(x(screen, 0), y(screen, 0), 0); int px = x(screen, 4), py = y(screen, 4);
        motion(screen, px, py);
        check((boolean)field(screen, "dragging") && (boolean)field(screen, "movingCells")
            && field(screen, "dragSelection").equals(selected) && field(screen, "dragSource").equals(source), "Grouped preview lost a source item");
        SettingsSnapshot.render(mc, screen, 640, 420, reports.resolve("group-preview-640.png"), 230, 180);
        check(data(screen).json().equals(original) && setting.value.equals(original), "Preview moved items before release");
        log("CREATIVE444 preview icon origins=" + List.of((px - 8) + "," + (py - 8), (px + 28) + "," + (py - 8), (px + 10) + "," + (py + 10)));
        screen.layerRelease(px, py, 0);
        check(CreativeTabsData.slots(data(screen).tab(0)).equals(CreativeTabsData.moveCells(source, selected, 4))
            && cells(screen).equals(Set.of(4, 6, 13)), "Grouped drop lost spacing, source values or selection");
        call(screen, "undo"); check(data(screen).json().equals(original), "Grouped drop Undo failed");
        call(screen, "undo"); check(data(screen).json().equals(original), "Grouped drop created multiple Undo records");
      });
      test("CREATIVE444 photo reference framing and resize retain zoom and daylight snapshot", () -> photo(mc, parent));
      check(inventory.equals(inventory(mc)) && Arrays.equals(config, Files.readAllBytes(PowerConfig.path())),
          "Creative/photo fixtures changed gameplay inventory or persisted preferences");
    } finally {
      mc.setScreen(parent); Keyboard.enableRepeatEvents(repeat);
      for (var entry : modifiers.entrySet()) keys.put(entry.getKey(), entry.getValue());
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(matrix);
    }
    log("CREATIVE444 failures=" + failures); check(failures == 0, "Creative444 failures=" + failures);
  }
  private static void photo(Minecraft mc, Screen parent) throws Exception {
    Class<?> projection = Class.forName("local.luke.power.input.PhotoProjection");
    Method width = projection.getMethod("width", int.class, int.class), scale = projection.getMethod("pointerScale", int.class);
    check(width.invoke(null, 1920, 1080).equals(width.invoke(null, 3840, 2160))
        && width.invoke(null, 2560, 1080).equals(width.invoke(null, 5120, 2160)), "Photo framing changes with physical resolution");
    check((double)scale.invoke(null, 1080) * 100 == (double)scale.invoke(null, 2160) * 200,
        "Equal fractional photo drags change with physical resolution");
    Class<?> type = Class.forName("local.luke.power.controls.tweaks.photomode.PhotoModeScreen");
    Screen photo = (Screen)type.getConstructor(Screen.class).newInstance(parent);
    Class<?> options = Class.forName("local.luke.power.controls.util.ModOptions");
    var fog = options.getField("photoModeFogMultiplier"); float previousFog = fog.getFloat(null);
    var cycles = (local.luke.power.world.WorldCycles)mc.world.method_262(); long daylight = cycles.power$daylightTime();
    try {
      mc.setScreen(photo); photo.init(mc, 320, 240);
      type.getField("zoom").setFloat(photo, 2.5f); type.getField("zoomGoal").setFloat(photo, 3f);
      long original = (long)field(photo, "originalTOD"); photo.init(mc, 640, 420);
      check(type.getField("zoom").getFloat(photo) == 2.5f && type.getField("zoomGoal").getFloat(photo) == 3f,
          "Photo resize reset current or saved target zoom");
      check((long)field(photo, "originalTOD") == original && cycles.power$daylightTime() == daylight,
          "Photo resize changed the original daylight snapshot");
    } finally { mc.setScreen(parent); cycles.power$daylightTime(daylight); fog.setFloat(null, previousFog); }
  }
}
