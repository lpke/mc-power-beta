package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import local.luke.power.autowalk.*;
import local.luke.power.building.camera.FreeLook;
import local.luke.power.building.config.Config;
import local.luke.power.config.*;
import local.luke.power.input.*;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;

/** Direction, GUI bounds and physical gesture checks in a disposable client. */
final class Release152Checks {
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("saved")) {
      check(Config.current().inventoryMouseLook, "inventory look preference lost on restart");
      log("PASS inventory look survives restart"); return;
    }
    if (action.equals("save")) {
      var s = Config.current().copy(); s.inventoryMouseLook = true; Config.apply(s);
      log("PASS saved inventory look"); return;
    }
    if (action.equals("setup")) {
      InventoryMovementChecks.run(mc, "setup");
      var s = Config.current().copy(); s.inventoryMouseLook = true; s.freeLook = true;
      s.autoWalkHoldToWalk = true; s.autoWalkHoldMillis = 350; Config.preview(s);
      AutoWalk.stop(); mc.player.yaw = 0; mc.player.pitch = 0;
      mc.options.mouseSensitivity = .5F; mc.options.invertYMouse = false;
      mc.options.cinematicMode = false; FreeLook.KEY.code = 41;
      Bindings.register(mc.options.allKeys); return;
    }
    if (action.equals("raw") || action.equals("native")) {
      mc.field_2767 = action.equals("raw")
          ? (net.minecraft.class_596) Class.forName("local.luke.power.controls.tweaks.rawinput.RawMouseHelper")
              .getConstructor(java.awt.Component.class).newInstance(mc.canvas)
          : new net.minecraft.class_596(mc.canvas);
      log("Mouse helper " + mc.field_2767.getClass().getName()); return;
    }
    if (action.equals("raw-delta")) {
      Class<?> raw = Class.forName("local.luke.power.controls.tweaks.rawinput.RawInputHandler");
      raw.getField("dx").setInt(null, 20); raw.getField("dy").setInt(null, -10); return;
    }
    if (action.equals("state")) {
      JsonObject out = new JsonObject();
      out.addProperty("walking", AutoWalk.isWalking()); out.addProperty("direction", AutoWalk.input());
      out.addProperty("forward", mc.player.field_161.field_2533);
      out.addProperty("x", mc.player.x); out.addProperty("y", mc.player.y); out.addProperty("z", mc.player.z);
      out.addProperty("looking", InventoryLook.active()); out.addProperty("freeLook", FreeLook.active());
      out.addProperty("yaw", mc.player.yaw); out.addProperty("pitch", mc.player.pitch);
      out.addProperty("focused", org.lwjgl.opengl.Display.isActive());
      out.addProperty("grabbed", org.lwjgl.input.Mouse.isGrabbed());
      out.addProperty("mouseX", org.lwjgl.input.Mouse.getX()); out.addProperty("mouseY", org.lwjgl.input.Mouse.getY());
      out.addProperty("displayWidth", mc.displayWidth); out.addProperty("displayHeight", mc.displayHeight);
      out.addProperty("guiMouseX", inventoryMouseX); out.addProperty("guiMouseY", inventoryMouseY);
      out.addProperty("mouseHelper", mc.field_2767.getClass().getName());
      Object camera = camera(); out.addProperty("camera", MovementOwnership.cameraControlsMovement());
      Object pose = camera.getClass().getMethod("getCameraPosition").invoke(camera);
      out.addProperty("cameraYaw", pose.getClass().getField("yaw").getFloat(pose));
      out.addProperty("cameraPitch", pose.getClass().getField("pitch").getFloat(pose));
      out.addProperty("cameraMove", camera.getClass().getField("move").getFloat(camera));
      out.addProperty("cursorCount", mc.player.inventory.getCursorStack() == null ? 0 : mc.player.inventory.getCursorStack().count);
      out.addProperty("screen", mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getName());
      Files.writeString(Path.of("power-beta-152-state.json"), out.toString()); log("STATE " + out); return;
    }
    if (action.equals("camera-on") || action.equals("camera-off")) {
      Object options = Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);
      options.getClass().getField("enabled").set(options, true);
      camera().getClass().getMethod("setActive", boolean.class).invoke(camera(), action.equals("camera-on")); return;
    }
    if (action.equals("cursor")) {
      mc.player.inventory.setCursorStack(new net.minecraft.item.ItemStack(264, 17, 0)); return;
    }
    if (action.equals("click-target")) {
      var stack = new net.minecraft.item.ItemStack(264, 17, 3);
      mc.player.inventory.main[0] = stack; mc.setScreen(new class_585(mc.player));
      var screen = (net.minecraft.client.gui.screen.container.ContainerScreen)mc.currentScreen;
      var slot = (net.minecraft.screen.slot.Slot)screen.container.slots.stream()
          .filter(s -> ((net.minecraft.screen.slot.Slot)s).getStack() == stack).findFirst().orElseThrow();
      JsonObject target = new JsonObject();
      target.addProperty("x", ((screen.width-176)/2 + slot.x+8) * mc.displayWidth / screen.width);
      target.addProperty("y", ((screen.height-166)/2 + slot.y+8) * mc.displayHeight / screen.height);
      Files.writeString(Path.of("power-beta-click-target.json"), target.toString());
      mc.player.inventory.main[0] = null; mc.player.inventory.setCursorStack(stack); return;
    }
    if (action.equals("click-check")) {
      var stack = mc.player.inventory.main[0];
      check(mc.player.inventory.getCursorStack() == null && stack != null && stack.itemId == 264
          && stack.count == 17 && stack.getDamage() == 3, "post-drag click missed slot or changed stack");
      log("PASS post-drag click targets restored coordinates and preserves the stack"); return;
    }
    if (action.equals("creative") || action.equals("survival")) {
      CreativeVehicleChecks.mode(mc, action.equals("creative") ? "CREATIVE" : "SURVIVAL"); return;
    }
    if (action.equals("chord") || action.equals("plain")) {
      Bindings.configure(action.equals("chord") ? Map.of(((Binding)(Object)AutoWalk.KEY).power$id(), Chord.CTRL) : Map.of());
      return;
    }
    if (action.equals("look-off") || action.equals("look-on")) {
      var s = Config.current().copy(); s.inventoryMouseLook = action.equals("look-on"); Config.preview(s); return;
    }
    if (action.equals("invert")) { mc.options.invertYMouse = !mc.options.invertYMouse; return; }
    if (action.equals("menu")) { mc.setScreen(new ChatScreen()); return; }
    if (!action.equals("check")) throw new IllegalArgumentException("Unknown release-152 action " + action);

    var before = Config.current().copy(); var settings = before.copy();
    settings.autoWalk = settings.freeLook = settings.inventoryWhileMoving = settings.inventoryMouseLook = true;
    Object cameraOptions = Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);
    Field enabled = cameraOptions.getClass().getField("enabled"); Object oldEnabled = enabled.get(cameraOptions);
    enabled.set(cameraOptions, true);
    Config.preview(settings); mc.setScreen(null); AutoWalk.tick(mc); failures = 0;
    try {
      test("raw helper retains device gameplay input and clears it in GUIs", () -> {
        var helper = mc.field_2767;
        Class<?> raw = Class.forName("local.luke.power.controls.tweaks.rawinput.RawInputHandler");
        Field dx = raw.getField("dx"), dy = raw.getField("dy");
        try {
          run(mc, "raw"); dx.setInt(null, 20); dy.setInt(null, -10);
          mc.field_2767.method_1972();
          check(mc.field_2767.field_2586 == 20 && mc.field_2767.field_2587 == 10, "gameplay lost raw device deltas");
          mc.field_2767.method_1972();
          check(mc.field_2767.field_2586 == 0 && mc.field_2767.field_2587 == 0, "raw device delta applied twice");
          mc.setScreen(new class_585(mc.player)); dx.setInt(null, 2000); dy.setInt(null, -1000);
          org.lwjgl.input.Mouse.getDX(); org.lwjgl.input.Mouse.getDY();
          mc.field_2767.method_1972();
          check(mc.field_2767.field_2586 == 0 && mc.field_2767.field_2587 == 0, "GUI used stale raw movement");
          check(dx.getInt(null) == 0 && dy.getInt(null) == 0, "GUI retained device input");
          mc.player.closeScreen(); mc.field_2767.method_1972();
          check(mc.field_2767.field_2586 == 0 && mc.field_2767.field_2587 == 0, "GUI exit revived raw movement");
        } finally {
          mc.field_2767 = helper; dx.setInt(null, 0); dy.setInt(null, 0); mc.setScreen(null);
        }
      });
      test("inventory look default, shared settings and Cancel/Apply", () -> {
        check(!new local.luke.power.building.config.Settings().inventoryMouseLook, "default enabled");
        ConfigSession session = SettingsRegistry.open(mc);
        Setting option = find(session, "tweaks.inventoryMouseLook");
        check(option.page.equals("Inventory") && !option.defaultValue.getAsBoolean() && !option.description.isBlank(), "missing metadata");
        option.parse("false"); session.preview(true);
        check(!Config.current().inventoryMouseLook, "preview not applied"); session.discard();
        check(Config.current().inventoryMouseLook, "Cancel did not restore");
        session = SettingsRegistry.open(mc); option = find(session, "tweaks.inventoryMouseLook");
        option.parse("false"); session.save(Path.of("."));
        check(!Config.current().inventoryMouseLook, "Apply lost preference");
        session = SettingsRegistry.open(mc); find(session, "tweaks.inventoryMouseLook").parse("true");
        session.save(Path.of("."));
      });
      for (boolean backwards : new boolean[] {false, true}) {
        test("auto-walk direction and free look backwards=" + backwards, () -> {
          startWalk(backwards); mc.player.method_140(); mc.player.field_161.method_1942(mc.player);
          check(mc.player.field_161.field_2533 == (backwards ? -1 : 1), "wrong walking direction");
          float yaw = mc.player.yaw; FreeLook.begin(mc); FreeLook.turn(mc.player, 70, -30);
          check(mc.player.yaw == yaw && AutoWalk.input() == (backwards ? -1 : 1), "free look changed walking/player");
          FreeLook.reset(mc);
          for (int key : new int[] {mc.options.forwardKey.code, mc.options.backKey.code}) {
            startWalk(backwards); mc.player.method_136(key, true);
            check(!AutoWalk.isWalking(), "manual movement did not cancel"); mc.player.method_136(key, false);
          }
        });
        test("freecam preserves signed player walk backwards=" + backwards, () -> {
          startWalk(backwards); Object camera = camera();
          camera.getClass().getMethod("setActive", boolean.class).invoke(camera, true);
          for (int key : new int[] {mc.options.forwardKey.code, mc.options.backKey.code}) {
            mc.player.method_136(key, true); mc.player.field_161.method_1942(mc.player);
            check(AutoWalk.input() == (backwards ? -1 : 1) && mc.player.field_161.field_2533 == AutoWalk.input(), "freecam changed player direction");
            mc.player.method_136(key, false);
          }
          camera.getClass().getMethod("setActive", boolean.class).invoke(camera, false);
          mc.player.field_161.method_1942(mc.player);
          check(mc.player.field_161.field_2533 == AutoWalk.input(), "freecam exit doubled movement");
          mc.setScreen(new ChatScreen()); check(!AutoWalk.isWalking(), "chat retained auto-walk"); mc.setScreen(null);
        });
      }
      test("inventory bounds at narrow and wide sizes, feature and multiplayer guards", () -> {
        for (int[] size : new int[][] {{320, 240}, {640, 420}}) {
          Screen screen = new class_585(mc.player); mc.setScreen(screen); screen.init(mc, size[0], size[1]);
          int left = (size[0] - 176) / 2, top = (size[1] - 166) / 2;
          check(!InventoryLook.outsideGui(mc, screen, left, top), "top-left GUI edge permits looking");
          check(!InventoryLook.outsideGui(mc, screen, left + 175, top + 165), "bottom-right GUI edge permits looking");
          check(InventoryLook.outsideGui(mc, screen, 0, size[1] - 1), "outside GUI blocked");
          settings.inventoryWhileMoving = false; Config.preview(settings);
          check(!InventoryLook.outsideGui(mc, screen, 0, size[1] - 1), "movement dependency ignored");
          settings.inventoryWhileMoving = true; settings.inventoryMouseLook = false; Config.preview(settings);
          check(!InventoryLook.outsideGui(mc, screen, 0, size[1] - 1), "look switch ignored");
          settings.inventoryMouseLook = true; Config.preview(settings);
          boolean remote = mc.world.isRemote; mc.world.isRemote = true;
          try { check(!InventoryLook.outsideGui(mc, screen, 0, size[1] - 1), "multiplayer guard ignored"); }
          finally { mc.world.isRemote = remote; }
          mc.player.closeScreen();
        }
        check(!InventoryLook.outsideGui(mc, new ChatScreen(), 0, 0), "chat permits looking");
        check(!InventoryLook.outsideGui(mc, new class_585(mc.player) {}, 0, 0), "unknown subclass permits looking");
      });
      test("creative catalogue, category tabs, view tabs and destroy GUI exclude looking", () -> {
        CreativeVehicleChecks.mode(mc, "CREATIVE");
        for (int[] size : new int[][] {{320, 240}, {640, 420}}) {
          Screen screen = new class_585(mc.player); mc.setScreen(screen); screen.init(mc, size[0], size[1]);
          Field normal = screen.getClass().getDeclaredField("creative_normalGUI"); normal.setAccessible(true); normal.setBoolean(screen, false);
          int left = (size[0] - 176) / 2, top = (size[1] - 166) / 2;
          for (int[] point : new int[][] {{left+10,top-10}, {left+10,Math.max(0,top-41)}, {left+180,top+140}, {left-10,top+145}})
            check(!InventoryLook.outsideGui(mc, screen, point[0], point[1]), "creative GUI permits looking at " + java.util.Arrays.toString(point));
          mc.player.closeScreen();
        }
        CreativeVehicleChecks.mode(mc, "SURVIVAL");
      });
      test("chest, furnace, dispenser and crafting table share look bounds", () -> {
        for (int block : new int[] {54, 61, 23, 58}) {
          mc.world.method_200(0, 101, 0, block); mc.player.method_1340(.5, 103, -2.5);
          Object inventory = mc.world.method_1777(0, 101, 0);
          if (block == 54) mc.player.method_486((net.minecraft.inventory.Inventory) inventory);
          if (block == 61) mc.player.method_487((net.minecraft.class_138) inventory);
          if (block == 23) mc.player.method_485((net.minecraft.class_137) inventory);
          if (block == 58) mc.player.method_484(0, 101, 0);
          Screen screen = mc.currentScreen;
          check(InventoryLook.outsideGui(mc, screen, 0, screen.height - 1), "container blocked outside look " + block);
          check(!InventoryLook.outsideGui(mc, screen, screen.width / 2, screen.height / 2), "container body permits look " + block);
          mc.player.closeScreen();
        }
      });
    } finally {
      camera().getClass().getMethod("setActive", boolean.class).invoke(camera(), false);
      AutoWalk.stop(); FreeLook.reset(mc); mc.player.method_140(); mc.setScreen(null); Config.preview(before);
      enabled.set(cameraOptions, oldEnabled);
    }
    log("RELEASE 152 FAILURES " + failures);
  }

  private static Object camera() throws Exception {
    return Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
  }
  private static void startWalk(boolean backwards) throws Exception {
    Field f = AutoWalk.class.getDeclaredField("TOGGLE"); f.setAccessible(true); WalkToggle toggle = (WalkToggle) f.get(null);
    toggle.stop(); toggle.update(false, true);
    toggle.update(true, true, false, 0, 350, backwards); toggle.update(false, true);
  }
}
