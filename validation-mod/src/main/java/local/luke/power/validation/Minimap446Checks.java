package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.Catalog;
import local.luke.power.death.DeathItems;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.input.*;
import local.luke.power.map.*;
import local.luke.power.mixin.DeathCraftingAccessor;
import local.luke.power.offhand.Offhand;
import local.luke.power.recipes.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.class_277;
import net.minecraft.class_564;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Session-head checks in a focused, muted disposable client. Does not save map preferences. */
public final class Minimap446Checks {
  private static final int HOLD_KEY = 118, OTHER_KEY = 117;
  private static Physical physical;
  private static final Path REPORT = Path.of("power-beta-data/reports/minimap-446");
  private Minimap446Checks() {}

  public static void run(Minecraft mc, String action) throws Exception {
    action = action.trim();
    if (action.equals("stop")) {
      if (physical != null) { Physical previous = physical; physical = null; previous.close(mc); }
      return;
    }
    if (action.startsWith("prepare ")) {
      check(physical == null, "stop the previous physical check first");
      String kind = action.substring(8).trim();
      check(kind.equals("keyboard") || kind.equals("mouse"), "use prepare keyboard|mouse");
      physical = new Physical(mc, kind); return;
    }
    if (action.startsWith("check-")) {
      check(physical != null, "prepare keyboard|mouse first");
      physical.checkState(mc, action.substring(6)); return;
    }
    check(physical == null, "stop the physical check before automated checks");
    boolean capture = action.equals("capture");
    check(action.isEmpty() || action.equals("run") || capture, "use minimap-446 capture|run");
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(),
        "focus a disposable singleplayer world");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    check(!Keyboard.isKeyDown(HOLD_KEY), "release F24 before this check");
    failures = 0;
    var before = MapConfig.copy(); var chords = Bindings.modifiers(); int binding = MapKeys.RADAR_ICONS.code;
    boolean hidden = mc.options.hideHud, debug = mc.options.debugHud;
    try (var state = new Recipe402Checks.State(mc)) {
      var configured = new HashMap<>(chords); configured.remove(MapKeys.RADAR_ICONS.translationKey);
      Bindings.configure(configured); MapKeys.RADAR_ICONS.code = HOLD_KEY;
      state.reset(640); mc.options.hideHud = mc.options.debugHud = false;
      var style = MapConfig.copy();
      style.minimap = style.worldMap = style.showInInventory = style.radar = true;
      style.players = style.hostile = style.animals = true;
      style.middleClickAction = 5; style.worldMapRadarIcons = false;
      style.size = 64; style.corner = 1; style.offsetX = style.offsetY = 6;
      MapConfig.preview(style); Maps.tick(mc); reset();
      Mouse.setGrabbed(false);
      PowerConfig.flushPending();
      String document = PowerConfig.document().toString(), preferences = Catalog.JSON.toJson(MapConfig.current());
      byte[] bytes = Files.readAllBytes(PowerConfig.path()); String inventory = state.snapshot();
      var inventoryScreen = mc.currentScreen;

      if (capture) {
        Files.createDirectories(REPORT);
        for (boolean pause : new boolean[]{false, true}) {
          if (pause) { mc.setScreen(new net.minecraft.class_525()); mc.currentScreen.init(mc, 640, 360); }
          else mc.currentScreen = inventoryScreen;
          Mouse.setGrabbed(false); reset();
          for (int tick = 0; tick < 4; tick++) Maps.tick(mc);
          capture(mc, REPORT.resolve(pause ? "pause-dots.png" : "inventory-dots.png"));
          int[] point = clearPoint(mc);
          Minimap.inventoryMiddle(mc, point[0], point[1]);
          for (int tick = 0; tick < 4; tick++) Maps.tick(mc);
          capture(mc, REPORT.resolve(pause ? "pause-mob-heads.png" : "inventory-mob-heads.png"));
          log("MINIMAP446 capture " + (pause ? "pause" : "inventory") + " native head sources="
              + Maps.radar(true).stream().filter(entity -> entity.icon() != null).count());
        }
        return;
      }

      test("MINIMAP446 inventory and pause heads remain session-only through ticks and renders", () -> {
        for (boolean pause : new boolean[]{false, true}) {
          if (pause) { mc.setScreen(new net.minecraft.class_525()); mc.currentScreen.init(mc, 640, 360); }
          else mc.currentScreen = inventoryScreen;
          Mouse.setGrabbed(false); reset(); int[] point = clearPoint(mc);
          check(Minimap.inventoryMiddle(mc, point[0], point[1]), "middle-click was not owned");
          check(MapKeys.iconsShown(mc, true, style), "session heads did not turn on");
          check(!MapKeys.iconsHeld(mc), "session toggle fabricated a hold");
          Maps.tick(mc); Minimap.renderInventory(mc, 1); Minimap.renderInventory(mc, 1);
          check(MapKeys.iconsShown(mc, true, style), "tick or repeated render cleared the latch");
          var worldMap = new local.luke.power.ui.WorldMapScreen(); mc.setScreen(worldMap); worldMap.init(mc, 640, 360);
          check(!MapKeys.iconsShown(mc, false, style), "minimap latch leaked into World Map");
          mc.currentScreen = pause ? new net.minecraft.class_525() : inventoryScreen;
          mc.currentScreen.init(mc, 640, 360); Mouse.setGrabbed(false); point = clearPoint(mc);
          check(MapKeys.iconsShown(mc, true, style), "menu visit lost the latch");
          check(Minimap.inventoryMiddle(mc, point[0], point[1]), "second middle-click was not owned");
          check(!MapKeys.iconsShown(mc, true, style), "second middle-click did not turn heads off");
        }
      });
      test("MINIMAP446 text and modal owners reject the hold; accepted fast taps restore hold behavior", () -> {
        mc.currentScreen = inventoryScreen; Mouse.setGrabbed(false); reset(); toggle();
        InventoryTextInput.focus(inventoryScreen, true);
        Bindings.keyPress(HOLD_KEY); InventoryTextInput.clear();
        check(MapKeys.iconsShown(mc, true, style), "text input canceled the session latch");
        Object owner = new Object(); InventoryPanels.modal(inventoryScreen, owner, true);
        try { Bindings.keyPress(HOLD_KEY); }
        finally { InventoryPanels.modal(inventoryScreen, owner, false); }
        check(MapKeys.iconsShown(mc, true, style), "another modal canceled the session latch");
        Bindings.keyPress(OTHER_KEY); Maps.tick(mc);
        check(MapKeys.iconsShown(mc, true, style), "nonmatching key canceled the session latch");
        Bindings.keyPress(HOLD_KEY);
        check(MapKeys.iconsHeld(mc), "accepted key-down did not publish held state");
        Maps.tick(mc);
        check(!MapKeys.iconsShown(mc, true, style), "fast tap retained a latched or held head");
      });

      test("MINIMAP446 disabled radar/maps keep snapshots empty and preserve the session choice", () -> {
        reset(); toggle(); var disabled = MapConfig.copy(); disabled.radar = false; MapConfig.preview(disabled);
        Bindings.keyPress(HOLD_KEY); Maps.tick(mc);
        check(!MapKeys.iconsShown(mc, true, disabled), "disabled radar displayed heads");
        check(Maps.radar(true).isEmpty() && Maps.radar(false).isEmpty(), "disabled radar retained tracked entities");
        disabled.radar = true; disabled.minimap = disabled.worldMap = false; MapConfig.preview(disabled);
        Maps.tick(mc);
        check(!MapKeys.iconsShown(mc, true, disabled), "disabled maps displayed heads");
        check(Maps.radar(true).isEmpty() && Maps.radar(false).isEmpty(), "disabled maps retained tracked entities");
        MapConfig.preview(style); Maps.tick(mc);
        check(MapKeys.iconsShown(mc, true, style), "disable/re-enable cleared the session choice");
      });

      test("MINIMAP446 session actions preserve settings bytes and native inventory; world exit clears heads", () -> {
        check(preferences.equals(Catalog.JSON.toJson(MapConfig.current())), "session actions changed map preferences");
        PowerConfig.flushPending();
        check(document.equals(PowerConfig.document().toString()), "session actions changed settings document");
        check(Arrays.equals(bytes, Files.readAllBytes(PowerConfig.path())), "session actions wrote settings bytes");
        check(inventory.equals(state.snapshot()), "session actions changed native inventory");
        Maps.leave(); check(!MapKeys.iconsShown(mc, true, style), "world exit retained session heads");
      });
    } finally {
      reset(); MapConfig.preview(before); MapKeys.RADAR_ICONS.code = binding; Bindings.configure(chords);
      mc.options.hideHud = hidden; mc.options.debugHud = debug;
    }
    log("MINIMAP446 failures=" + failures); check(failures == 0, "minimap-446 failures=" + failures);
  }

  private static void capture(Minecraft mc, Path file) throws Exception {
    int nativeScale = mc.options.guiScale;
    Class<?> options = Class.forName("local.luke.power.controls.util.ModOptions");
    var scaleField = options.getField("guiScale");
    var realScaleField = options.getField("realGuiScale");
    float scale = scaleField.getFloat(null), realScale = realScaleField.getFloat(null);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      mc.options.guiScale = 2;
      scaleField.setFloat(null, 2 / 8f); realScaleField.setFloat(null, 2 / 8f);
      SettingsSnapshot.render(mc, mc.currentScreen, 640, 360, file);
    } finally {
      mc.options.guiScale = nativeScale;
      scaleField.setFloat(null, scale); realScaleField.setFloat(null, realScale);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib();
    }
  }

  private static int[] clearPoint(Minecraft mc) {
    var frame = Minimap.layout(MapConfig.current(), mc.currentScreen.width, mc.currentScreen.height, false);
    for (int y = frame.y() + 4; y < frame.y() + frame.size() - 4; y++)
      for (int x = frame.x() + 4; x < frame.x() + frame.size() - 4; x++)
        if (Minimap.inventoryContains(mc, x, y) && mc.currentScreen instanceof InventoryGui gui
            && !gui.power$containsGuiSpace(x, y)) return new int[]{x, y};
    throw new AssertionError("no minimap terrain outside native GUI controls");
  }

  private static void reset() throws Exception { call("resetIcons"); }
  private static void toggle() throws Exception { call("toggleMinimapHeads"); }
  private static void call(String name) throws Exception {
    Method method = MapKeys.class.getDeclaredMethod(name); method.setAccessible(true); method.invoke(null);
  }

  private static Field field(Class<?> type, String name) throws Exception {
    Field value = type.getDeclaredField(name); value.setAccessible(true); return value;
  }

  private static final class Physical {
    final MapSettings map = MapConfig.copy();
    final RecipeBookSettings recipes = RecipeBookConfig.copy();
    final Map<String, Integer> modifiers = Bindings.modifiers();
    final Map<KeyBinding, Integer> codes = new IdentityHashMap<>();
    final Screen screen;
    final Container container;
    final ItemStack[] main, armor, grid, inputs;
    final ItemStack cursor, offhand, output;
    final int selected, hold;
    final boolean hidden, debug, grabbed, repeat, focused, paused;
    final String inventoryBefore, document;
    final byte[] bytes;
    class_585 host;
    String isolated, preferences;

    Physical(Minecraft mc, String kind) throws Exception {
      check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(),
          "focus a disposable singleplayer world");
      check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
      check(!NativeInventoryPlan.busy(), "finish the inventory transaction first");
      hold = kind.equals("keyboard") ? HOLD_KEY : -97;
      check(!Bindings.physical(hold), "release the hold binding first");
      check(hold > 0 || Mouse.getButtonCount() > 3, "native pointer has no extra button 3");
      check(mc.player.playerContainer.getClass() == class_277.class, "expected native player container");
      PowerConfig.flushPending();
      inventoryBefore = snapshot(mc); document = PowerConfig.document().toString(); bytes = Files.readAllBytes(PowerConfig.path());
      screen = mc.currentScreen; container = mc.player.container;
      var inventory = mc.player.inventory;
      main = inventory.main; armor = inventory.armor; selected = inventory.selectedSlot;
      cursor = inventory.getCursorStack(); offhand = Offhand.get(inventory);
      grid = ((DeathCraftingAccessor)(Object)((class_277)mc.player.playerContainer).field_1124).power$deathStacks();
      inputs = grid.clone(); output = mc.player.playerContainer.method_2084(0).getStack();
      hidden = mc.options.hideHud; debug = mc.options.debugHud; grabbed = Mouse.isGrabbed();
      repeat = Keyboard.areRepeatEventsEnabled(); focused = mc.field_2778; paused = mc.paused;
      for (var binding : mc.options.allKeys) codes.put(binding, binding.code);
      boolean prepared = false;
      try {
        inventory.main = new ItemStack[36]; inventory.armor = new ItemStack[4]; inventory.selectedSlot = 0;
        Arrays.fill(grid, null); mc.player.playerContainer.method_2084(0).setStack(null);
        inventory.setCursorStack(null); Offhand.set(inventory, null);
        var disabled = RecipeBookConfig.copy(); disabled.enabled = false; RecipeBookConfig.preview(disabled);
        for (var binding : codes.keySet()) if (binding.code == hold) binding.code = 0;
        MapKeys.RADAR_ICONS.code = hold; var chords = new HashMap<>(modifiers);
        chords.remove(MapKeys.RADAR_ICONS.translationKey); Bindings.configure(chords); Bindings.register(mc.options.allKeys);
        var style = MapConfig.copy(); style.minimap = style.worldMap = style.showInInventory = style.radar = true;
        style.players = style.hostile = style.animals = true; style.worldMapRadarIcons = false;
        style.middleClickAction = 5; style.size = 64; style.corner = 1; style.offsetX = style.offsetY = 6;
        MapConfig.preview(style); mc.options.hideHud = mc.options.debugHud = false;
        var resolution = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
        host = new class_585(mc.player); host.container = mc.player.playerContainer;
        mc.currentScreen = host; mc.player.container = mc.player.playerContainer;
        host.init(mc, resolution.method_1857(), resolution.method_1858()); Mouse.setGrabbed(false);
        Maps.tick(mc); reset(); isolated = snapshot(mc); preferences = Catalog.JSON.toJson(MapConfig.current());
        int[] point = clearPoint(mc);
        log("MINIMAP446 READY " + kind + " binding=" + hold + " GUI=" + host.width + "x" + host.height
            + " display=" + mc.displayWidth + "x" + mc.displayHeight + " map GUI=" + point[0] + "," + point[1]
            + " window pixels=" + point[0] * mc.displayWidth / host.width + "," + point[1] * mc.displayHeight / host.height);
        log("MINIMAP446 middle-click on; hold -> check-held; middle while held -> check-latched-held;"
            + " lose focus -> check-inactive; return still held -> check-latched-held; release -> check-on; fresh hold/release -> check-off");
        prepared = true;
      } finally { if (!prepared) close(mc); }
    }

    void checkState(Minecraft mc, String expected) throws Exception {
      check(Set.of("on", "off", "held", "latched-held", "inactive").contains(expected), "unknown physical state " + expected);
      check(mc.currentScreen == host && mc.player.container == mc.player.playerContainer, "native inventory owner changed");
      boolean active = Display.isActive(), down = Bindings.physical(hold), held = MapKeys.iconsHeld(mc);
      Object heads = field(MapKeys.class, "HEADS").get(null);
      boolean latch = field(heads.getClass(), "toggled").getBoolean(heads);
      boolean shown = MapKeys.iconsShown(mc, true, MapConfig.current());
      log("MINIMAP446 observed " + expected + " active=" + active + " pointerGrabbed=" + Mouse.isGrabbed()
          + " pointerPixels=" + Mouse.getX() + "," + (mc.displayHeight - Mouse.getY() - 1)
          + " physical=" + down + " held=" + held + " latch=" + latch + " shown=" + shown);
      check(active == !expected.equals("inactive"), "native window focus differs from expected state");
      check(!Mouse.isGrabbed(), "native inventory pointer unexpectedly grabbed");
      check(latch == Set.of("on", "latched-held", "inactive").contains(expected), "unexpected session latch");
      check(held == (expected.equals("held") || expected.equals("latched-held")), "unexpected accepted held state");
      if (active) check(down == held, "physical hold differs from accepted native input");
      check(shown == (!expected.equals("off") && !expected.equals("inactive")), "unexpected visible heads");
      preserved(mc); log("PASS MINIMAP446 physical " + expected);
    }

    void preserved(Minecraft mc) throws Exception {
      check(isolated.equals(snapshot(mc)), "physical input changed native inventory");
      check(preferences.equals(Catalog.JSON.toJson(MapConfig.current())), "physical input changed map preferences");
      check(document.equals(PowerConfig.document().toString()) && Arrays.equals(bytes, Files.readAllBytes(PowerConfig.path())),
          "physical check changed persisted settings");
    }

    void close(Minecraft mc) throws Exception {
      try { if (isolated != null) preserved(mc); }
      finally {
        if (host != null) { var book = RecipeBooks.book(host); if (book != null) book.removed(); InventoryPanels.remove(host); }
        mc.player.inventory.main = main; mc.player.inventory.armor = armor; mc.player.inventory.selectedSlot = selected;
        System.arraycopy(inputs, 0, grid, 0, inputs.length); mc.player.playerContainer.method_2084(0).setStack(output);
        mc.player.inventory.setCursorStack(cursor); Offhand.set(mc.player.inventory, offhand);
        codes.forEach((key, code) -> key.code = code); Bindings.configure(modifiers); Bindings.register(mc.options.allKeys);
        RecipeBookConfig.preview(recipes); MapConfig.preview(map); reset();
        mc.currentScreen = screen; mc.player.container = container; mc.field_2778 = focused; mc.paused = paused;
        mc.options.hideHud = hidden; mc.options.debugHud = debug; Mouse.setGrabbed(grabbed); Keyboard.enableRepeatEvents(repeat);
        check(inventoryBefore.equals(snapshot(mc)), "restoration lost native inventory data");
      }
      log("MINIMAP446 physical state restored without saving settings");
    }
  }

  private static String snapshot(Minecraft mc) throws Exception {
    return DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
  }
}
