package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.util.Map;
import local.luke.power.autowalk.AutoWalk;
import local.luke.power.building.config.Config;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.recipes.*;
import local.luke.power.ui.TextInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Actual held native W across real game ticks, including measured world displacement. */
final class Creative443MovementChecks {
  private static State active;
  private Creative443MovementChecks() {}
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("sorting")) { sorting(mc); return; }
    if (action.equals("stop")) { stop(mc); return; }
    if (action.equals("check")) {
      check(active != null, "prepare a creative-443-movement case first"); active.checkResult(mc); return;
    }
    String[] words = action.trim().split("\\s+");
    check(words.length == 2 && words[0].equals("prepare") && java.util.Set.of("focused", "focused-flying", "unfocused", "recipe").contains(words[1]),
        "use creative-443-movement prepare focused|focused-flying|unfocused|recipe, check, sorting, or stop");
    stop(mc);
    check(mc.world != null && !mc.world.isRemote && mc.player != null && mc.player.health > 0 && !mc.player.dead
        && Display.isActive() && !DetachedCamera.isActive() && !AutoWalk.isWalking(), "focus a living disposable world outside freecam/Auto-walk");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute disposable client first");
    check(!Keyboard.isKeyDown(Keyboard.KEY_W) && !Mouse.isButtonDown(0) && !Mouse.isButtonDown(1), "release W and mouse buttons before preparing");
    active = new State(mc, words[1]);
    try { active.prepare(mc); }
    catch (Exception | Error failure) { try { stop(mc); } catch (Exception cleanup) { failure.addSuppressed(cleanup); } throw failure; }
  }
  private static void stop(Minecraft mc) throws Exception {
    if (active == null) return;
    active.owner(mc); var previous = active;
    try { previous.close(); } finally { active = null; }
    log("CREATIVE443 MOVEMENT stopped; original inventory, preferences, mode, flight and position restored");
  }
  private static void sorting(Minecraft mc) throws Exception {
    check(active == null && mc.world != null && !mc.world.isRemote && mc.player != null && Display.isActive(), "stop movement checks and focus a disposable world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute disposable client first");
    try (var state = new Inventory442Checks.State(mc)) {
      state.reset(false, false);
      var recipes = RecipeBookConfig.copy(); recipes.enabled = recipes.inventory = recipes.inventoryButton = true; RecipeBookConfig.preview(recipes);
      var book = RecipeBooks.book(state.screen); field(book, "open", false); book.tick();
      boolean repeat = Keyboard.areRepeatEventsEnabled();
      var layer = new local.luke.power.ui.InventorySortingLayer(mc, state.screen);
      check(layer.active() && InventoryPanels.modal(state.screen) && InventoryTextInput.active(state.screen), "current sorting layer did not claim modal/text input");
      layer.key('\0', Keyboard.KEY_ESCAPE);
      check(!layer.active() && !InventoryPanels.modal(state.screen) && !InventoryTextInput.active(state.screen)
          && mc.currentScreen == state.screen && Keyboard.areRepeatEventsEnabled() == repeat, "Escape retained sorting focus or failed to restore its native host");
      field(book, "open", true); book.tick(); var bounds = book.layout().search(); book.mouse(bounds.x() + 4, bounds.y() + 4, 0);
      check(((TextInput)field(book, "search")).focused && InventoryTextInput.active(state.screen), "recipe search did not claim the returned native host");
      layer.close();
      check(InventoryTextInput.active(state.screen), "stale sorting close released live recipe-search ownership");
      ((TextInput)field(book, "search")).focused = false; book.tick();
      check(!InventoryTextInput.active(state.screen), "recipe search did not release its own claim");
      var changed = new local.luke.power.ui.InventorySortingLayer(mc, state.screen);
      mc.currentScreen = null; changed.prepare();
      check(!changed.active() && !InventoryPanels.modal(state.screen) && !InventoryTextInput.active(state.screen), "screen transition retained sorting modal/text ownership");
      mc.currentScreen = state.screen;
    }
    log("PASS CREATIVE443 current sorting Escape/screen cleanup releases only its own focus and preserves returned recipe search");
  }
  private static final class State implements AutoCloseable {
    final Minecraft mc; final Object player, world; final String kind;
    final Inventory442Checks.State inventory;
    final local.luke.power.building.config.Settings building;
    final Map<String, Integer> modifiers;
    final int forward;
    final double x, y, z, vx, vy, vz;
    final float yaw, pitch;
    final boolean flying;
    String before; long ticks; double startX, startY, startZ;
    CreativePaletteBridge.Search search; RecipeBookOverlay book;
    State(Minecraft mc, String kind) throws Exception {
      this.mc = mc; this.kind = kind; player = mc.player; world = mc.world;
      building = Config.current().copy(); forward = mc.options.forwardKey.code; modifiers = Bindings.modifiers();
      x = mc.player.x; y = mc.player.y; z = mc.player.z; yaw = mc.player.yaw; pitch = mc.player.pitch;
      vx = mc.player.velocityX; vy = mc.player.velocityY; vz = mc.player.velocityZ;
      flying = (boolean)mc.player.getClass().getMethod("creative_isFlying").invoke(mc.player);
      inventory = new Inventory442Checks.State(mc);
    }
    void owner(Minecraft mc) { check(mc.player == player && mc.world == world, "keep movement fixture in its original world/player"); }
    void prepare(Minecraft mc) throws Exception {
      inventory.reset(true, true);
      var moving = Config.current().copy(); moving.inventoryWhileMoving = true; Config.preview(moving);
      mc.options.forwardKey.code = Keyboard.KEY_W; Bindings.configure(Map.of());
      mc.player.getClass().getMethod("creative_setFlying", boolean.class).invoke(mc.player, kind.equals("focused-flying"));
      mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
      var recipes = RecipeBookConfig.copy(); recipes.enabled = recipes.inventory = recipes.inventoryButton = true; RecipeBookConfig.preview(recipes);
      book = RecipeBooks.book(inventory.screen); check(book != null, "native recipe-book host is missing");
      if (kind.equals("recipe")) {
        field(inventory.screen, "creative_normalGUI", true); field(book, "open", true); book.tick();
        var bounds = book.layout().search(); book.mouse(bounds.x() + 4, bounds.y() + 4, 0);
        check(((TextInput)field(book, "search")).focused, "recipe positive comparison did not focus");
      } else {
        field(inventory.screen, "creative_normalGUI", false);
        var select = inventory.screen.getClass().getDeclaredMethod("power$selectTab", int.class); select.setAccessible(true); select.invoke(inventory.screen, 6);
        search = (CreativePaletteBridge.Search)field(inventory.screen, "power_search");
        if (kind.equals("unfocused")) search.close();
        check(search.focused() == !kind.equals("unfocused"), "Creative Search preparation has wrong focus");
        // Exercise the exact conflict before returning to native ticks, retaining enabled recipes.
        for (int i = 0; i < 4; i++) book.tick();
        check(InventoryTextInput.active(inventory.screen) == !kind.equals("unfocused"), "disabled recipe cleanup overwrote Creative typing ownership");
      }
      before = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)); ticks = mc.world.getTime();
      startX = mc.player.x; startY = mc.player.y; startZ = mc.player.z;
      log("CREATIVE443 MOVEMENT " + kind + " ready GUI=" + inventory.screen.width + "x" + inventory.screen.height
          + " display=" + mc.displayWidth + "x" + mc.displayHeight + " start=" + mc.player.x + "," + mc.player.z
          + " ACTION hold physical W for at least 1 second, keep holding while running check, then release W and stop");
    }
    void checkResult(Minecraft mc) throws Exception {
      owner(mc); check(mc.currentScreen == inventory.screen && mc.player.container == inventory.screen.container, "movement screen/container changed");
      check(Display.isActive() && Keyboard.isKeyDown(Keyboard.KEY_W), "hold physical W in the focused disposable window");
      long elapsed = mc.world.getTime() - ticks;
      check(elapsed >= 15, "let at least 15 real world ticks elapse while holding W");
      double distance = Math.hypot(mc.player.x - startX, mc.player.z - startZ);
      if (kind.equals("unfocused")) {
        check(!InventoryTextInput.active(inventory.screen) && !search.focused() && mc.player.field_161.field_2533 > 0 && distance > .1,
            "unfocused native held-W positive control failed to move, displacement=" + distance);
      } else {
        check(InventoryTextInput.active(inventory.screen) && mc.player.field_161.field_2533 == 0 && mc.player.field_161.field_2532 == 0 && distance < .001,
            "focused editor admitted native movement, displacement=" + distance);
        String typed = kind.equals("recipe") ? ((TextInput)field(book, "search")).text() : search.text();
        check(typed.toLowerCase(java.util.Locale.ROOT).contains("w"), "held native W did not reach focused text editor");
        if (kind.equals("focused-flying")) check((boolean)mc.player.getClass().getMethod("creative_isFlying").invoke(mc.player)
            && Math.abs(mc.player.y - startY) < .001 && mc.player.velocityX == 0 && mc.player.velocityY == 0 && mc.player.velocityZ == 0,
            "focused flying input retained motion or lost flight during typing");
      }
      check(before.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer))), "movement fixture changed inventory bytes");
      log("PASS CREATIVE443 MOVEMENT " + kind + " realTicks=" + elapsed + " displacement=" + distance + " nativeForward=" + mc.player.field_161.field_2533);
    }
    public void close() throws Exception {
      owner(mc);
      try { inventory.close(); }
      finally {
        Config.preview(building); mc.options.forwardKey.code = forward; Bindings.configure(modifiers);
        mc.player.method_1340(x, y, z); mc.player.prevX = x; mc.player.prevY = y; mc.player.prevZ = z;
        mc.player.yaw = mc.player.prevYaw = yaw; mc.player.pitch = mc.player.prevPitch = pitch;
        mc.player.getClass().getMethod("creative_setFlying", boolean.class).invoke(mc.player, flying);
        mc.player.velocityX = vx; mc.player.velocityY = vy; mc.player.velocityZ = vz;
      }
    }
  }
}
