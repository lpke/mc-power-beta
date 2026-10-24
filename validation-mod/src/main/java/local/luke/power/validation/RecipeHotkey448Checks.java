package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Method;
import java.util.*;
import local.luke.power.input.*;
import local.luke.power.inventory.tools.InventoryFeatureKeys;
import local.luke.power.recipes.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Exercise the native container handler and gameplay dispatcher without changing items. */
final class RecipeHotkey448Checks {
  private static State physical;
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("stop")) { stop(); return; }
    if (action.equals("check")) {
      check(physical != null, "prepare a physical hotkey case first");
      physical.checkResult(); return;
    }
    if (action.startsWith("prepare ")) {
      stop(); require(mc); physical = new State(mc);
      try { physical.prepare(action.substring(8)); }
      catch (Exception | Error failure) { stop(); throw failure; }
      return;
    }
    check(action.equals("run"), "use recipe-hotkey-448 run|prepare CASE|check|stop");
    require(mc); failures = 0;
    try (var state = new State(mc)) {
      for (String kind : List.of("gameplay", "pause", "chest")) {
        state.prepare(kind);
        check(!InventoryFeatureKeys.available(mc, "powerbeta.recipeBook"), kind + " reserved the hotkey");
        var before = mc.currentScreen;
        state.dispatch(); check(mc.currentScreen == before, kind + " opened a screen");
        state.key.code = -100; Bindings.register(new Object[]{state.key});
        check(!Bindings.claimsMouse(-100), kind + " claimed an unavailable mouse binding");
        state.key.code = Keyboard.KEY_F12; Bindings.register(state.keys);
        log("PASS RECIPE448 inactive hotkey and mouse ownership: " + kind);
      }
      for (String kind : List.of("inventory", "table", "furnace")) {
        state.prepare(kind); var book = RecipeBooks.book(mc.currentScreen);
        check(book != null && InventoryFeatureKeys.available(mc, "powerbeta.recipeBook"), kind + " lost hotkey");
        check(!book.layout().open(), "book must start closed");
        ((ScreenInput)mc.currentScreen).power$key('\0', Keyboard.KEY_F12);
        check(book.layout().open(), kind + " failed to open");
        ((ScreenInput)mc.currentScreen).power$key('\0', Keyboard.KEY_F12);
        check(!book.layout().open(), kind + " failed to close");
        book.toggle();
        var search = (local.luke.power.ui.TextInput)UiChecks.field(book, "search"); search.focused = true; book.tick();
        ((ScreenInput)mc.currentScreen).power$key('\0', Keyboard.KEY_F12);
        check(book.layout().open() && search.focused && !InventoryFeatureKeys.available(mc, "powerbeta.recipeBook"),
            "recipe search lost keyboard ownership");
        search.focused = false; book.tick();
        Object modal = new Object(); InventoryPanels.modal(mc.currentScreen, modal, true);
        try {
          ((ScreenInput)mc.currentScreen).power$key('\0', Keyboard.KEY_F12);
          check(book.layout().open() && !InventoryFeatureKeys.available(mc, "powerbeta.recipeBook"), "modal lost keyboard ownership");
        } finally { InventoryPanels.modal(mc.currentScreen, modal, false); }
        book.toggle();
        var disabled = RecipeBookConfig.copy(); disabled.inventory = false; RecipeBookConfig.preview(disabled);
        check(!InventoryFeatureKeys.available(mc, "powerbeta.recipeBook"), "disabled inventory reserved hotkey");
        log("PASS RECIPE448 native open/close and disabled ownership: " + kind);
      }
    }
    log("RECIPE448 completed");
  }
  private static void require(Minecraft mc) {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && Display.isActive(), "focus a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    check(mc.player instanceof PlayerMode mode && !mode.power$creative() && !mode.power$spectator(), "use Survival");
  }
  private static void stop() throws Exception { if (physical != null) { var old = physical; physical = null; old.close(); log("RECIPE448 physical state restored"); } }
  private static final class State implements AutoCloseable {
    final Minecraft mc; final Recipe402Checks.State inventory; final Object player, world;
    final KeyBinding key = local.luke.power.input.GameplayKeys.RECIPE_BOOK;
    final int previousKey; final Map<String,Integer> modifiers; final Object[] keys;
    final List<Screen> hosts = new ArrayList<>();
    String kind; Screen host; RecipeBookOverlay book; int[] placed;
    State(Minecraft mc) throws Exception {
      this.mc = mc; player = mc.player; world = mc.world; previousKey = key.code; modifiers = Bindings.modifiers(); keys = mc.options.allKeys;
      inventory = new Recipe402Checks.State(mc);
      key.code = Keyboard.KEY_F12; var plain = new HashMap<>(modifiers); plain.remove("powerbeta.recipeBook"); Bindings.configure(plain); Bindings.register(keys);
    }
    void prepare(String kind) throws Exception {
      restoreBlock(); this.kind = kind; inventory.reset(640);
      var settings = RecipeBookConfig.copy(); settings.enabled = settings.inventory = settings.pauseMenu = true;
      settings.craftingOpen = settings.furnaceOpen = false; RecipeBookConfig.preview(settings);
      host = switch (kind) {
        case "gameplay" -> null;
        case "pause" -> new Screen() {};
        case "chest" -> { place(54); yield new class_264(mc.player.inventory, (net.minecraft.inventory.Inventory)mc.world.method_1777(placed[0], placed[1], placed[2])); }
        case "inventory", "search" -> mc.currentScreen;
        case "table" -> { place(58); yield new class_516(mc.player.inventory, mc.world, placed[0], placed[1], placed[2]); }
        case "furnace" -> { place(61); yield new class_524(mc.player.inventory, (class_138)mc.world.method_1777(placed[0], placed[1], placed[2])); }
        default -> throw new IllegalArgumentException("gameplay|pause|chest|inventory|table|furnace|search");
      };
      mc.currentScreen = host; mc.player.container = mc.player.playerContainer;
      if (host != null) { host.init(mc, 640, 360); hosts.add(host); if (host instanceof net.minecraft.client.gui.screen.container.ContainerScreen container) mc.player.container = container.container; }
      Mouse.setGrabbed(false); InventoryTextInput.clear(); book = RecipeBooks.book(host);
      if (book != null) { UiChecks.field(book, "open", false); book.tick(); if (kind.equals("search")) {
        book.toggle(); ((local.luke.power.ui.TextInput)UiChecks.field(book, "search")).focused = true; book.tick();
      } }
      log("RECIPE448 physical prepared " + kind + "; press F12 once, release, then check");
    }
    private void place(int id) {
      int x = (int)Math.floor(mc.player.x), y = Math.max(1, Math.min(126, (int)Math.floor(mc.player.boundingBox.minY) + 1)), z = (int)Math.floor(mc.player.z);
      for (int[] offset : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
        int bx = x + offset[0], bz = z + offset[1];
        if (mc.world.method_239(bx, y, bz) && mc.world.getBlockId(bx, y, bz) == 0
            && mc.world.method_1777(bx, y, bz) == null) {
          placed = new int[]{bx, y, bz, mc.world.method_1778(bx, y, bz)};
          mc.world.method_154(bx, y, bz, id, 0); return;
        }
      }
      throw new AssertionError("fixture requires one nearby loaded air block");
    }
    private void restoreBlock() {
      if (placed != null) { mc.world.method_154(placed[0], placed[1], placed[2], 0, placed[3]); placed = null; }
    }
    void dispatch() throws Exception {
      Method method = local.luke.power.input.GameplayKeys.class.getDeclaredMethod("inventoryKey", int.class);
      method.setAccessible(true); method.invoke(null, Keyboard.KEY_F12);
    }
    void checkResult() throws Exception {
      check(mc.player == player && mc.world == world && mc.currentScreen == host, "hotkey changed screen or owner in " + kind + "; expected " + (host == null ? "gameplay" : host.getClass().getName()) + ", actual " + (mc.currentScreen == null ? "gameplay" : mc.currentScreen.getClass().getName()));
      if (book != null) check(book.layout().open(), "native inventory hotkey did not open the recipe book");
      else check(!InventoryFeatureKeys.available(mc, "powerbeta.recipeBook"), "unavailable screen owns hotkey");
      log("PASS RECIPE448 physical " + kind);
    }
    @Override public void close() throws Exception {
      check(mc.player == player && mc.world == world, "restore the original owner");
      for (var host : hosts) { var attached = RecipeBooks.book(host); if (attached != null) attached.removed(); InventoryPanels.remove(host); }
      restoreBlock(); key.code = previousKey; Bindings.configure(modifiers); Bindings.register(keys); inventory.close();
    }
  }
}
