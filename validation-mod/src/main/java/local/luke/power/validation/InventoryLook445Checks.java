package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;

import java.nio.file.Files;
import java.util.*;
import local.luke.power.autowalk.*;
import local.luke.power.building.config.Config;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.recipes.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.class_564;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;

/** Actual model poses, including Creative's canceled native render and recipe offsets. */
public final class InventoryLook445Checks {
  private record Pose(float yaw, float pitch, float body) {}
  private static State prepared;
  private static Minecraft watching;
  private static Pose pose;
  private static long samples;
  private InventoryLook445Checks() {}

  public static void model(Entity entity) {
    if (watching == null || entity != watching.player) return;
    pose = new Pose(watching.player.yaw, watching.player.pitch, watching.player.field_1012);
    samples++;
  }

  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("stop")) { stop(mc); return; }
    if (action.equals("check")) {
      check(prepared != null, "prepare an inventory-look-445 case first");
      prepared.checkPointer(); return;
    }
    stop(mc);
    check(mc.world != null && !mc.world.isRemote && mc.player != null && mc.player.health > 0
        && !mc.player.dead && Display.isActive() && !DetachedCamera.isActive(), "focus a living disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client first");
    check(!Mouse.isButtonDown(0) && !Mouse.isButtonDown(1) && !InventoryLook.active(), "release mouse buttons before checks");
    if (action.startsWith("prepare ")) {
      String kind = action.substring("prepare ".length());
      check(Set.of("survival", "creative", "spectator", "recipe", "palette").contains(kind), "prepare survival|creative|spectator|recipe|palette");
      prepared = new State(mc);
      try {
        prepared.open(kind, 0); watching = mc; pose = null; samples = 0;
        prepared.startYaw = mc.player.yaw; prepared.startPitch = mc.player.pitch;
        log("LOOK445 prepared " + kind + " GUI=" + prepared.screen.width + "x" + prepared.screen.height
            + " display=" + mc.displayWidth + "x" + mc.displayHeight
            + "; right-drag outside all panels, move the pointer and run check while held; release and run check again, then stop");
      } catch (Exception | Error failure) { stop(mc); throw failure; }
      return;
    }
    check(action.isEmpty() || action.equals("run"), "use inventory-look-445 run|prepare CASE|check|stop");
    failures = 0;
    test("LOOK445 native model ignores hidden hover and stale cursor caches in every personal view", () -> {
      try (var state = new State(mc)) {
        for (int width : new int[]{320, 640}) for (String kind : List.of("survival", "creative", "spectator", "recipe", "palette")) {
          state.open(kind, width);
          int x = state.screen.width - 8, y = state.screen.height - 7;
          // Reproduce hidden/stale writes from the previous Creative or native frame.
          field(state.screen, "field_2523", -10000f); field(state.screen, "field_2524", -10000f);
          Pose visible = state.draw(x, y);
          if (!kind.equals("palette")) state.expected(visible, x, y, kind + " first visible frame");
          else check(visible == null, "Creative palette unexpectedly rendered the player");
          InventoryPointer pointer = pointer(); pointer.clear();
          pointer.event(x * mc.displayWidth / state.screen.width,
              (state.screen.height - y - 1) * mc.displayHeight / state.screen.height, 0, 0, false);
          pointer.press(); check(drag().press(state.screen, mc.world, mc.player, true), "could not seed isolated model drag");
          int originX = InventoryLook.cursorX(state.screen, mc), originY = InventoryLook.cursorY(state.screen, mc);
          for (int frame = 0; frame < 3; frame++) {
            pointer.event(0, 0, 13, -9, true);
            Pose hidden = state.draw(-10000, -10000);
            if (!kind.equals("palette")) state.expected(hidden, originX, originY, kind + " hidden frame " + frame);
            else check(hidden == null, "hidden palette unexpectedly rendered the player");
            check(((Number)field(state.screen, "field_2523")).floatValue() == -10000
                && ((Number)field(state.screen, "field_2524")).floatValue() == -10000,
                "model repair made the shared native/Creative hover cache visible");
            if (!kind.equals("palette")) check(inventoryMouseX == -10000 && inventoryMouseY == -10000, "model repair exposed native slot hover");
          }
          drag().stop(); pointer.clear();
          Pose released = state.draw(9, 11);
          if (!kind.equals("palette")) state.expected(released, 9, 11, kind + " first release frame");
          state.preserved();
          log("LOOK445 pose PASS " + kind + " width=" + width + " offset=" + InventoryPanels.offset(state.screen));
        }
      }
    });
    log("LOOK445 failures=" + failures);
    check(failures == 0, "LOOK445 failures=" + failures);
  }

  private static InventoryLookDrag drag() throws Exception {
    var f = InventoryLook.class.getDeclaredField("DRAG"); f.setAccessible(true); return (InventoryLookDrag)f.get(null);
  }
  private static InventoryPointer pointer() throws Exception {
    var f = InventoryLook.class.getDeclaredField("POINTER"); f.setAccessible(true); return (InventoryPointer)f.get(null);
  }
  private static void stop(Minecraft mc) throws Exception {
    watching = null;
    if (prepared == null) return;
    var old = prepared; prepared = null; old.owner(); old.close();
    log("LOOK445 stopped; native inventory, rules, preferences, screen and pointer restored");
  }

  private static final class State implements AutoCloseable {
    final Minecraft mc; final Object player, world;
    final Screen previous; final net.minecraft.inventory.Container container;
    final local.luke.power.rules.RuleSet rules;
    final local.luke.power.building.config.Settings building;
    final RecipeBookSettings recipes;
    final String mode, inventory;
    final byte[] config;
    final boolean grabbed, focused, paused;
    final int mouseX, mouseY;
    final float yaw, pitch, body, prevYaw, prevPitch;
    final List<class_585> screens = new ArrayList<>();
    class_585 screen; String kind;
    float startYaw, startPitch; int heldX = -1, heldY = -1;
    State(Minecraft mc) throws Exception {
      this.mc = mc; player = mc.player; world = mc.world; previous = mc.currentScreen; container = mc.player.container;
      rules = AccessRules.copy(mc); building = Config.current().copy(); recipes = RecipeBookConfig.copy();
      mode = mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
      inventory = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
      config = Files.readAllBytes(PowerConfig.path()); grabbed = Mouse.isGrabbed(); mouseX = Mouse.getX(); mouseY = Mouse.getY();
      focused = mc.field_2778; paused = mc.paused;
      yaw = mc.player.yaw; pitch = mc.player.pitch; body = mc.player.field_1012; prevYaw = mc.player.prevYaw; prevPitch = mc.player.prevPitch;
    }
    void owner() { check(mc.player == player && mc.world == world, "keep LOOK445 in its original world/player"); }
    void open(String kind, int width) throws Exception {
      owner(); watching = null; drag().stop(); pointer().clear();
      for (var previous : screens) { var book = RecipeBooks.book(previous); if (book != null) book.removed(); InventoryPanels.remove(previous); }
      this.kind = kind;
      AccessRules.set(mc, local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE, true);
      AccessRules.set(mc, local.luke.power.rules.GameRule.ALLOW_SPECTATOR_MODE, true);
      CreativeVehicleChecks.mode(mc, kind.equals("spectator") ? "SPECTATOR" : kind.equals("creative") || kind.equals("palette") ? "CREATIVE" : "SURVIVAL");
      var moving = Config.current().copy(); moving.inventoryWhileMoving = moving.inventoryMouseLook = true; Config.preview(moving);
      var bookSettings = RecipeBookConfig.copy(); bookSettings.enabled = bookSettings.inventory = bookSettings.inventoryButton = kind.equals("recipe"); RecipeBookConfig.preview(bookSettings);
      screen = new class_585(mc.player); screens.add(screen); mc.currentScreen = screen; mc.player.container = mc.player.playerContainer;
      Mouse.setGrabbed(false); mc.field_2778 = true; mc.paused = false;
      var size = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
      screen.init(mc, width == 0 ? size.method_1857() : width, width == 0 ? size.method_1858() : width == 320 ? 240 : 420);
      if (!kind.equals("survival") && !kind.equals("recipe")) field(screen, "creative_normalGUI", !kind.equals("palette"));
      if (kind.equals("palette")) {
        var search = (CreativePaletteBridge.Search)field(screen, "power_search"); if (search != null) search.close();
      }
      var book = RecipeBooks.book(screen);
      if (book != null) { field(book, "open", kind.equals("recipe")); book.tick(); }
      check((kind.equals("recipe") && book != null && !book.layout().narrow()) == (InventoryPanels.offset(screen) != 0), "recipe fixture has the wrong native origin");
      if (kind.equals("recipe") && width == 0) check(book != null && !book.layout().narrow(), "use a wider native GUI for a physical recipe drag");
    }
    Pose draw(int x, int y) throws Exception {
      float beforeYaw = mc.player.yaw, beforePitch = mc.player.pitch, beforeBody = mc.player.field_1012;
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glLoadIdentity();
      GL11.glOrtho(0, screen.width, screen.height, 0, 1000, 3000);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix(); GL11.glLoadIdentity(); GL11.glTranslatef(0, 0, -2000);
      pose = null; watching = mc;
      try { screen.render(x, y, 0); return pose; }
      finally {
        watching = null; GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glPopAttrib();
        check(mc.player.yaw == beforeYaw && mc.player.pitch == beforePitch && mc.player.field_1012 == beforeBody,
            "inventory model leaked its temporary pose into gameplay");
      }
    }
    void expected(Pose actual, int x, int y, String context) {
      check(actual != null, context + " did not submit a player model");
      int left = (screen.width - 176) / 2 + InventoryPanels.offset(screen), top = (screen.height - 166) / 2;
      float turn = (float)Math.atan((left + 51 - x) / 40f);
      float tilt = -(float)Math.atan((top + 25 - y) / 40f) * 20;
      check(Math.abs(actual.yaw - turn * 40) < .001 && Math.abs(actual.body - turn * 20) < .001
          && Math.abs(actual.pitch - tilt) < .001, context + " actual=" + actual + " expected cursor=" + x + "," + y);
    }
    void checkPointer() throws Exception {
      owner(); check(mc.currentScreen == screen && Display.isActive(), "keep the prepared inventory focused"); preserved();
      if (Mouse.isButtonDown(1)) {
        check(InventoryLook.active() && Mouse.isGrabbed(), "native right drag did not capture");
        heldX = InventoryLook.cursorX(screen, mc); heldY = InventoryLook.cursorY(screen, mc);
        if (!kind.equals("palette")) expected(pose, heldX, heldY, "physical held drag");
        check(inventoryMouseX == -10000 && inventoryMouseY == -10000, "held native drag exposed hover");
        check(Math.abs(mc.player.yaw - startYaw) > .05 || Math.abs(mc.player.pitch - startPitch) > .05, "move the physical pointer before check");
        log("PASS LOOK445 physical held pose=" + pose + " camera=" + mc.player.yaw + "," + mc.player.pitch
            + " origin=" + heldX + "," + heldY + " samples=" + samples + " helper=" + mc.field_2767.getClass().getName());
      } else {
        check(heldX >= 0 && !InventoryLook.active() && !Mouse.isGrabbed(), "check while held first, then release the same gesture");
        int x = Mouse.getX() * screen.width / mc.displayWidth, y = screen.height - Mouse.getY() * screen.height / mc.displayHeight - 1;
        if (!kind.equals("palette")) expected(pose, x, y, "physical released model");
        log("PASS LOOK445 physical release cursor=" + x + "," + y + " origin=" + heldX + "," + heldY + " pose=" + pose);
      }
    }
    void preserved() throws Exception {
      check(inventory.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer))), "LOOK445 changed native item bytes");
      check(Arrays.equals(config, Files.readAllBytes(PowerConfig.path())), "LOOK445 wrote configuration");
    }
    public void close() throws Exception {
      owner(); watching = null;
      try { InventoryLook.stop(); preserved(); }
      finally {
        for (var value : screens) { var book = RecipeBooks.book(value); if (book != null) book.removed(); InventoryPanels.remove(value); }
        RecipeBookConfig.preview(recipes); Config.preview(building); CreativeVehicleChecks.mode(mc, mode); AccessRules.restore(mc, rules);
        mc.currentScreen = previous; mc.player.container = container; mc.field_2778 = focused; mc.paused = paused;
        mc.player.yaw = yaw; mc.player.prevYaw = prevYaw; mc.player.pitch = pitch; mc.player.prevPitch = prevPitch; mc.player.field_1012 = body;
        Mouse.setCursorPosition(mouseX, mouseY); Mouse.setGrabbed(grabbed);
      }
    }
  }
}
