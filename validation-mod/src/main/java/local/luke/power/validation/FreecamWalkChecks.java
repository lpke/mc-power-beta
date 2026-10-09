package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.util.Map;
import local.luke.power.autowalk.*;
import local.luke.power.building.config.Config;
import local.luke.power.config.*;
import local.luke.power.input.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;

final class FreecamWalkChecks {
  static void run(Minecraft mc, String action) throws Exception {
    Object camera = Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    if (action.equals("state")) {
      log("WALK active=" + AutoWalk.isWalking() + " focus=" + org.lwjgl.opengl.Display.isActive()
          + " forward=" + mc.player.field_161.field_2533 + " camera=" + number(camera, "move")
          + " x=" + mc.player.x + " z=" + mc.player.z); return;
    }
    if (action.equals("focus")) {
      check(!org.lwjgl.opengl.Display.isActive(), "test window still focused");
      mc.player.field_161.method_1942(mc.player);
      check(!AutoWalk.isWalking() && mc.player.field_161.field_2533 == 0 && number(camera, "move") == 0, "focus loss retained movement");
      log("PASS focus loss stops player auto-walk and camera input"); return;
    }
    var old = Config.current().copy();
    var s = old.copy(); s.autoWalk = true; s.inventoryWhileMoving = false; Config.preview(s);
    Object cameraSettings = Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);
    Field enabled = cameraSettings.getClass().getField("enabled"); Object oldEnabled = enabled.get(cameraSettings); enabled.set(cameraSettings, true);
    Bindings.configure(Map.of()); mc.options.forwardKey.code = 17; mc.options.backKey.code = 31;
    mc.options.leftKey.code = 30; mc.options.rightKey.code = 32; mc.options.jumpKey.code = 57; mc.options.sneakKey.code = 42;
    mc.setScreen(null); AutoWalk.tick(mc); mc.player.method_140();
    if (action.equals("physical")) { active(camera, true); startWalk(); return; }
    failures = 0;
    try {
      for (boolean walkFirst : new boolean[] {true, false}) test("freecam and auto-walk activation order " + walkFirst, () -> {
        active(camera, false); AutoWalk.stop(); mc.player.method_140();
        if (walkFirst) { startWalk(); active(camera, true); } else { active(camera, true); startWalk(); }
        check(MovementOwnership.cameraControlsMovement(), "freecam not enabled");
        mc.player.field_161.method_1942(mc.player);
        check(AutoWalk.isWalking() && mc.player.field_161.field_2533 == 1, "auto-walk suppressed or doubled");
        check(number(camera, "move") == 0, "auto-walk moved camera");
      });
      test("camera forward/back/strafe/jump/sneak leave auto-walk independent", () -> {
        active(camera, true); startWalk();
        for (int key : new int[] {17, 31, 30, 32, 57, 42}) {
          mc.player.method_136(key, true); mc.player.field_161.method_1942(mc.player);
          check(AutoWalk.isWalking() && mc.player.field_161.field_2533 == 1, "key=" + key
              + " walking=" + AutoWalk.isWalking() + " forward=" + mc.player.field_161.field_2533
              + " cameraOwns=" + MovementOwnership.cameraControlsMovement() + " cameraMove=" + number(camera, "move"));
          check(mc.player.field_161.field_2532 == 0 && !mc.player.field_161.field_2535 && !mc.player.field_161.field_2536, "camera key moved player");
          if (key == 17 || key == 31) check(number(camera, "move") == (key == 17 ? 1 : -1), "camera forward/back missing");
          if (key == 30 || key == 32) check(number(camera, "strafe") == (key == 30 ? 1 : -1), "camera strafe missing");
          mc.player.method_136(key, false); mc.player.field_161.method_1942(mc.player);
          check(number(camera, "move") == 0 && number(camera, "strafe") == 0, "camera key stuck after release");
        }
        for (int first : new int[] {17, 31}) {
          mc.player.method_136(17, true); mc.player.method_136(31, true); mc.player.field_161.method_1942(mc.player);
          check(number(camera, "move") == 0 && mc.player.field_161.field_2533 == 1, "opposing camera keys affected auto-walk");
          mc.player.method_136(first, false); mc.player.field_161.method_1942(mc.player);
          check(number(camera, "move") == (first == 17 ? -1 : 1), "release order lost remaining key");
          mc.player.method_136(first == 17 ? 31 : 17, false);
        }
      });
      test("player movement mode restores manual cancellation and clears camera controls", () -> {
        mc.player.method_136(17, true); mc.player.field_161.method_1942(mc.player);
        camera.getClass().getField("allowPlayerMovement").setBoolean(camera, true);
        mc.player.method_136(31, true); mc.player.field_161.method_1942(mc.player);
        check(!AutoWalk.isWalking() && number(camera, "move") == 0, "player mode kept stale camera input or auto-walk");
        mc.player.method_140();
      });
      test("exiting freecam preserves auto-walk; menus stop it", () -> {
        active(camera, true); startWalk(); active(camera, false); mc.player.field_161.method_1942(mc.player);
        check(AutoWalk.isWalking() && mc.player.field_161.field_2533 == 1, "exit stopped/doubled walking");
        active(camera, true); mc.setScreen(new ChatScreen()); mc.player.field_161.method_1942(mc.player);
        check(!AutoWalk.isWalking() && mc.player.field_161.field_2533 == 0 && number(camera, "move") == 0, "menu retained movement");
      });
    } finally { active(camera, false); AutoWalk.stop(); mc.player.method_140(); mc.setScreen(null); enabled.set(cameraSettings, oldEnabled); Config.preview(old); }
    log("FREECAM WALK FAILURES " + failures);
  }
  private static void active(Object camera, boolean value) throws Exception { camera.getClass().getMethod("setActive", boolean.class).invoke(camera, value); }
  private static float number(Object camera, String name) throws Exception { return camera.getClass().getField(name).getFloat(camera); }
  private static void startWalk() throws Exception {
    Field field = AutoWalk.class.getDeclaredField("TOGGLE"); field.setAccessible(true);
    WalkToggle toggle = (WalkToggle) field.get(null); toggle.stop(); toggle.update(false, true); toggle.update(true, true); toggle.update(false, true);
  }
}
