package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import local.luke.power.gameplay.*;
import local.luke.power.input.DetachedCamera;
import local.luke.power.rules.*;
import local.luke.power.sprint.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.class_41;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import org.lwjgl.opengl.Display;

/** Real stair collision and player travel, with native walking as the delay control. */
final class SprintStairs451Checks {
  private static final int Y = 94, TICKS = 24;

  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null
        && Display.isActive() && !DetachedCamera.isActive(), "focus a muted disposable singleplayer world");
    failures = 0;
    var p = mc.player; var saved = new NbtCompound(); p.write(saved);
    var input = p.field_161; var screen = mc.currentScreen; boolean paused = mc.paused;
    float stepHeight = p.field_1641;
    var gameplay = GameplayConfig.copy(); var sprint = SprintConfig.copy(); var rules = AccessRules.copy(mc);
    String mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(p).toString();
    byte[] disk = Files.readAllBytes(PowerConfig.path());
    int x = ((int)Math.floor(p.x) >> 4) * 16 + 8, z = ((int)Math.floor(p.z) >> 4) * 16 + 8;
    var blocks = new ArrayList<int[]>();
    try {
      check(mc.world.method_155(x - 3, 0, z - 3, x + 15, 127, z + 15), "stair scene needs already loaded neighboring chunks");
      for (int bx = x - 2; bx <= x + 13; bx++) for (int bz = z - 2; bz <= z + 13; bz++)
        for (int by = Y - 1; by <= Y + 15; by++)
          blocks.add(new int[]{bx, by, bz, mc.world.getBlockId(bx, by, bz), mc.world.method_1778(bx, by, bz)});
      var permittedRules = RuleSet.cheats(); permittedRules.set(GameRule.ALLOW_SPRINTING, true);
      mc.setScreen(null); mc.paused = false; AccessRules.restore(mc, permittedRules); CreativeVehicleChecks.mode(mc, "SURVIVAL");
      var options = GameplayConfig.copy(); options.modernCrawling = false; options.modernWaterMovement = false; GameplayConfig.preview(options);
      var settings = SprintConfig.copy(); settings.stamina = false; settings.noSprintInCombat = false;
      settings.activation = 1; settings.doubleTap = false; settings.stopAtWalls = true; settings.speed = 130;
      settings.particles = false; SprintConfig.preview(settings);

      test("native sprint climbs wooden and stone stairs at the flat-ground pace in both axes", () -> {
        for (int axis = 0; axis < 2; axis++) for (int block : new int[]{53, 67}) for (int speed : new int[]{100, 130, 200}) {
          var tuning = SprintConfig.copy(); tuning.speed = speed; SprintConfig.preview(tuning);
          scene(mc, x, z, axis, 0); reset(mc, x, z, axis, true);
          double flat = travel(mc, axis, false);
          scene(mc, x, z, axis, block); reset(mc, x, z, axis, true);
          double climbed = travel(mc, axis, true);
          near(climbed, flat, 3e-5, "stair pace axis=" + axis + " block=" + block + " speed=" + speed);
          check(p.boundingBox.minY > Y + 3, "sprint did not climb the staircase");
          log("SPRINT STAIRS axis=" + axis + " block=" + block + " speed=" + speed + " flat=" + flat + " stairs=" + climbed);
        }
      });
      test("walking and revoked sprint retain Beta's step delay while sprint retains native camera smoothing", () -> {
        scene(mc, x, z, 0, 53); reset(mc, x, z, 0, false);
        int nativeStops = direct(p, 0, 20);
        check(nativeStops > 0, "native delay control did not reproduce the stair collision");
        reset(mc, x, z, 0, true); int sprintStops = direct(p, 0, 20);
        check(sprintStops == 0 && p.field_1640 > .05f, "sprint retained the delay or discarded visual step smoothing");
        var denied = AccessRules.copy(mc); denied.set(GameRule.ALLOW_SPRINTING, false); AccessRules.restore(mc, denied);
        try {
          reset(mc, x, z, 0, false); ((SprintPlayer)p).power$sprint().sprinting = true;
          check(direct(p, 0, 20) == nativeStops, "revoked sprint changed native stair stepping");
        } finally { AccessRules.restore(mc, permittedRules); }
        log("SPRINT STAIRS nativeCollisions=" + nativeStops + " sprintCollisions=" + sprintStops);
      });
      test("sprint still collides with full blocks and stops at real walls", () -> {
        scene(mc, x, z, 0, 0); mc.world.method_154(x + 1, Y, z, 1, 0);
        reset(mc, x, z, 0, true); p.field_1640 = .51f;
        p.move(.8, -.0784, 0);
        check(p.field_1624 && p.x <= x + .70001, "sprint climbed a full-height wall");
        near(p.boundingBox.minY, Y, 1e-6, "full wall moved feet");
        for (int tick = 0; tick < 3; tick++) { p.field_161.field_2533 = 1; p.tick(); }
        check(!((SprintPlayer)p).power$sprint().sprinting, "real wall no longer cancels sprint");
      });
      test("sprint stair fixture leaves durable preferences unchanged", () ->
          check(Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "stair fixture wrote preferences"));
    } finally {
      mc.world.isRemote = false;
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[3], b[4]);
      AccessRules.restore(mc, RuleSet.cheats()); CreativeVehicleChecks.mode(mc, mode);
      GameplayConfig.preview(gameplay); SprintConfig.preview(sprint); AccessRules.restore(mc, rules);
      p.read(saved); p.field_161 = input; p.field_1641 = stepHeight;
      mc.paused = paused; mc.setScreen(screen); SprintController.reset();
    }
    log("SPRINT STAIRS 451 FAILURES " + failures);
  }

  private static void scene(Minecraft mc, int x, int z, int axis, int stair) {
    for (int bx = x - 2; bx <= x + 13; bx++) for (int bz = z - 2; bz <= z + 13; bz++)
      for (int by = Y - 1; by <= Y + 15; by++) mc.world.method_154(bx, by, bz, by == Y - 1 ? 1 : 0, 0);
    if (stair == 0) return;
    for (int step = 1; step <= 12; step++) for (int side = -1; side <= 1; side++) {
      int bx = x + (axis == 0 ? step : side), bz = z + (axis == 0 ? side : step);
      for (int by = Y; by < Y + step - 1; by++) mc.world.method_154(bx, by, bz, 1, 0);
      mc.world.method_154(bx, Y + step - 1, bz, stair, axis == 0 ? 0 : 2);
    }
  }

  private static void reset(Minecraft mc, int x, int z, int axis, boolean sprint) {
    var p = mc.player; var swim = ((SwimPlayer)p).power$swim();
    swim.compact = swim.crouched = swim.swimming = false; swim.amount = swim.previousAmount = 0; swim.resetCamera(1.62f);
    p.spacingXZ = .6f; p.spacingY = 1.8f; p.field_1631 = 1.62f; p.field_1640 = 0; p.field_1641 = .5f;
    p.health = 20; p.dead = false; p.method_1341(x + .5, Y, z + .5, axis == 0 ? 270 : 0, 0);
    p.velocityX = p.velocityZ = 0; p.velocityY = -.08 * (double).98f;
    p.field_1623 = true; p.field_1624 = false; p.field_161 = new class_41(); p.field_161.field_2533 = 1;
    SprintController.reset(); SprintController.movement(p);
    var state = ((SprintPlayer)p).power$sprint(); state.cancel(false);
    if (sprint) {
      state.land(SprintConfig.current(), true, 1, false, false, false);
      check(SprintController.active(p), "stair fixture did not establish eligible ground sprint");
    }
  }

  private static double travel(Minecraft mc, int axis, boolean stairs) {
    var p = mc.player; double start = axis == 0 ? p.x : p.z;
    for (int tick = 0; tick < TICKS; tick++) {
      p.field_161.field_2533 = 1; p.tick();
      check(((SprintPlayer)p).power$sprint().sprinting, "sprint stopped at tick " + tick + " stairs=" + stairs);
      check(!p.field_1624, "unexpected horizontal collision at tick " + tick + " stairs=" + stairs);
    }
    return (axis == 0 ? p.x : p.z) - start;
  }

  private static int direct(Entity p, int axis, int ticks) {
    int collisions = 0;
    for (int tick = 0; tick < ticks; tick++) {
      p.move(axis == 0 ? .2806 : 0, -.0784, axis == 0 ? 0 : .2806);
      if (p.field_1624) collisions++;
    }
    return collisions;
  }

  private static void near(double actual, double expected, double tolerance, String label) {
    check(Math.abs(actual - expected) <= tolerance, label + ": " + actual + " expected " + expected);
  }
}
