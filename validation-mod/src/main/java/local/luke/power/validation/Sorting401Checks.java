package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import local.luke.power.config.*;
import local.luke.power.death.DeathItems;
import local.luke.power.inventory.tools.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Native editor input and captures, with fixture presets confined to the local draft. */
final class Sorting401Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/sorting-401");
  private Sorting401Checks() {}

  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && Display.isActive(), "focus a disposable loaded world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable clone before UI checks");
    check(Display.getWidth() >= 1280 && Display.getHeight() >= 720, "resize the disposable client to at least 1280x720");
    check(!Mouse.isButtonDown(0) && !Mouse.isButtonDown(1), "release mouse buttons before UI checks");
    failures = 0; Files.createDirectories(REPORT); PowerConfig.flushPending();
    var config = PowerConfig.document().deepCopy(); byte[] saved = Files.readAllBytes(PowerConfig.path());
    String inventory = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
    var previousScreen = mc.currentScreen; var previousContainer = mc.player.container;
    boolean repeat = Keyboard.areRepeatEventsEnabled(), grabbed = Mouse.isGrabbed(), focused = mc.field_2778, paused = mc.paused;
    var screens = new ArrayList<SortingPreferencesScreen>();
    int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      Mouse.setGrabbed(false); mc.field_2778 = true;
      for (int[] size : new int[][]{{320,240},{640,360}}) {
        var session = new ConfigSession(); InventoryToolsBackend.register(session);
        Setting protectedSetting = setting(session, "inventoryTools.protectedSlots"), preferredSetting = setting(session, "inventoryTools.preferredItems");
        protectedSetting.value = new JsonArray(); preferredSetting.value = new JsonObject();
        JsonElement initialProtected = protectedSetting.value.deepCopy(), initialPreferred = preferredSetting.value.deepCopy();
        AtomicBoolean accepted = new AtomicBoolean();
        var editor = new SortingPreferencesScreen(previousScreen, session, () -> accepted.set(true));
        field(editor, "presets", presets()); field(editor, "presetIndex", 0);
        mc.currentScreen = editor; editor.init(mc, size[0], size[1]); screens.add(editor);
        var input = (ScreenInput)(Object)editor;
        test("SORTING401 control geometry and capture " + size[0], () -> {
          check((int)call(editor, "presetsTop") - ((int)call(editor, "controlsTop") + 58) == 12, "preset controls lost the 12-pixel gap");
          capture(mc, editor, "layout-" + size[0]);
        });
        test("SORTING401 dropdown keyboard selection and Escape " + size[0], () -> {
          open(editor, input); check((boolean)field(editor, "choosingPreset"), "preset selector did not open");
          input.power$key('\0', Keyboard.KEY_DOWN); input.power$key('\0', Keyboard.KEY_ESCAPE);
          check(!(boolean)field(editor, "choosingPreset") && (int)field(editor, "presetIndex") == 0, "Escape committed the focused preset");
          open(editor, input); input.power$key('\0', Keyboard.KEY_END); input.power$key('\0', Keyboard.KEY_RETURN);
          check(!(boolean)field(editor, "choosingPreset") && (int)field(editor, "presetIndex") == 2, "Enter did not select the focused preset");
          check(((Set<?>)field(editor, "protectedSlots")).isEmpty() && ((Map<?,?>)field(editor, "preferredItems")).isEmpty(), "selecting a preset loaded its layout");
        });
        test("SORTING401 dropdown mouse selection and local Load " + size[0], () -> {
          open(editor, input); capture(mc, editor, "dropdown-" + size[0]);
          int top = (int)call(editor, "presetMenuTop"), left = (int)call(editor, "left");
          input.power$click(left + 12, top + 20 + 10, 0);
          check((int)field(editor, "presetIndex") == 1 && !(boolean)field(editor, "choosingPreset"), "mouse did not select the second preset");
          input.power$click(left + 120, (int)call(editor, "presetsTop") + 9, 0);
          check(((Set<?>)field(editor, "protectedSlots")).equals(Set.of(10))
              && ((Map<?,?>)field(editor, "preferredItems")).equals(Map.of(11, "4")), "Load did not copy both preset values into the local layout");
          check(initialProtected.equals(protectedSetting.value) && initialPreferred.equals(preferredSetting.value) && !accepted.get(), "Load published the isolated draft");
        });
        test("SORTING401 preferred item click returns immediately to its local editor " + size[0], () -> {
          field(editor, "selected", 9);
          input.power$click((int)call(editor, "left") + 10, (int)call(editor, "controlsTop") + 29, 0);
          check(mc.currentScreen instanceof ItemPickerScreen, "preferred item button did not open the picker");
          var picker = (ItemPickerScreen)mc.currentScreen; picker.init(mc, size[0], size[1]);
          check(((ItemPickerScreen.Options)field(picker, "options")).instant(), "preferred item picker requires a second confirmation");
          @SuppressWarnings("unchecked") var shown = (List<ItemCatalogue.Entry>)field(picker, "shown");
          check(!shown.isEmpty(), "preferred item picker has no native rows"); String first = shown.get(0).value();
          ((ScreenInput)(Object)picker).power$click((int)call(picker, "left") + 12, 78, 0);
          check(mc.currentScreen == editor && first.equals(((Map<?,?>)field(editor, "preferredItems")).get(9)), "first row click did not return immediately with the preferred item");
          check(initialProtected.equals(protectedSetting.value) && initialPreferred.equals(preferredSetting.value) && !accepted.get(), "picker changed the parent session before Done");
          editor.init(mc, size[0], size[1]); capture(mc, editor, "preferred-item-" + size[0]);
        });
        test("SORTING401 Discard preserves settings presets and gameplay inventory " + size[0], () -> {
          mc.currentScreen = editor; editor.init(mc, size[0], size[1]);
          input.power$click(editor.width / 2 + 12, editor.height - 18, 0);
          check(mc.currentScreen == previousScreen && !accepted.get(), "Discard accepted the draft or returned to the wrong screen");
          check(initialProtected.equals(protectedSetting.value) && initialPreferred.equals(preferredSetting.value), "Discard changed the parent session");
          check(config.equals(PowerConfig.document()) && Arrays.equals(saved, Files.readAllBytes(PowerConfig.path())), "sorting draft wrote settings or saved presets");
          check(inventory.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer))), "sorting UI changed gameplay inventory");
        });
      }
    } finally {
      for (var screen : screens) screen.removed();
      mc.currentScreen = previousScreen; mc.player.container = previousContainer;
      mc.field_2778 = focused; mc.paused = paused; Mouse.setGrabbed(grabbed); Keyboard.enableRepeatEvents(repeat);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(matrixMode);
    }
    log("SORTING401 failures=" + failures + " captures=" + REPORT);
    check(failures == 0, "Sorting editor regression failures=" + failures);
  }

  private static SortingLayoutsData presets() {
    var values = SortingLayoutsData.read(new JsonObject());
    for (int i = 0; i < 3; i++) {
      var protectedSlots = new JsonArray(); protectedSlots.add(9 + i); var preferred = new JsonObject(); preferred.addProperty("11", "4");
      values = values.save("Fixture " + (i + 1), protectedSlots, preferred);
    }
    return values;
  }
  private static Setting setting(ConfigSession session, String id) { return session.settings().stream().filter(value -> value.id.equals(id)).findFirst().orElseThrow(); }
  private static void open(SortingPreferencesScreen editor, ScreenInput input) throws Exception { input.power$click((int)call(editor, "left") + 10, (int)call(editor, "presetsTop") + 9, 0); }
  private static void capture(Minecraft mc, SortingPreferencesScreen editor, String name) throws Exception { SettingsSnapshot.render(mc, editor, editor.width, editor.height, REPORT.resolve(name + ".png")); }
}
