package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.gameplay.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.PowerOptionsScreen;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** The installed 26.3 OPEN + ladder-below + equal-FACING predicate, using native Beta movement. */
final class TrapdoorClimb473Checks {
  private TrapdoorClimb473Checks() {}
  static void run(Minecraft mc, String action) throws Exception {
    check(Display.isActive() && mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "focus and mute a disposable client");
    if (action.equals("capture")) { capture(mc); return; }
    check(action.equals("native") && mc.world != null && !mc.world.isRemote && mc.player != null && mc.player.field_1595 == null,
        "use trapdoor-climb-473 native in a disposable singleplayer world outside vehicles");
    PowerConfig.flushPending(); byte[] config = Files.readAllBytes(PowerConfig.path());
    var old = GameplayConfig.copy(); var p = mc.player; var input = p.field_161; var previous = mc.currentScreen;
    var saved = new net.minecraft.nbt.NbtCompound(); p.write(saved);
    String mode = ((local.luke.power.input.PlayerMode)p).power$spectator() ? "SPECTATOR"
        : ((local.luke.power.input.PlayerMode)p).power$creative() ? "CREATIVE" : "SURVIVAL";
    int x = (int)Math.floor(p.x) + 5, y = 112, z = (int)Math.floor(p.z) + 5;
    check(mc.world.method_155(x - 2, y - 2, z - 2, x + 2, y + 4, z + 2), "load the fixture area first");
    List<int[]> blocks = new ArrayList<>();
    for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = -2; dy <= 4; dy++) {
      check(mc.world.method_1777(x + dx, y + dy, z + dz) == null, "fixture overlaps a block entity");
      blocks.add(new int[]{x + dx, y + dy, z + dz, mc.world.getBlockId(x + dx, y + dy, z + dz), mc.world.method_1778(x + dx, y + dy, z + dz)});
    }
    int predicates = 0;
    try {
      mc.setScreen(null); CreativeVehicleChecks.mode(mc, "SURVIVAL"); p.field_161 = new net.minecraft.class_41();
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[1] == y - 2 ? 1 : 0, 0);
      for (int dy = -1; dy <= 2; dy++) for (int[] side : new int[][]{{0,1},{0,-1},{1,0},{-1,0}}) mc.world.method_154(x + side[0], y + dy, z + side[1], 1, 0);
      LivingEntity mob = new LivingEntity(mc.world) {};
      for (boolean enabled : new boolean[]{false, true}) {
        var settings = GameplayConfig.copy(); settings.modernTrapdoorClimbing = enabled; GameplayConfig.preview(settings);
        for (int trap = 0; trap < 8; trap++) for (int ladder = 0; ladder < 8; ladder++) {
          mc.world.method_154(x, y, z, 96, trap); mc.world.method_154(x, y - 1, z, 65, ladder);
          p.method_1341(x + .5, y + .25, z + .5, 0, 0); mob.method_1340(x + .5, y + .25, z + .5);
          boolean expected = enabled && (trap & 4) != 0 && ladder == (trap & 3) + 2;
          check(p.method_932() == expected && mob.method_932() == expected, "climb predicate enabled=" + enabled + " trap=" + trap + " ladder=" + ladder);
          predicates++;
        }
        mc.world.method_154(x, y, z, 65, 2); p.method_1341(x + .5, y + .25, z + .5, 0, 0);
        check(p.method_932(), "toggle changed ordinary ladders");
      }
      var enabled = GameplayConfig.copy(); enabled.modernTrapdoorClimbing = true; GameplayConfig.preview(enabled);
      mc.world.method_154(x, y, z, 96, 4);
      for (int below : new int[]{0, 1, 96}) {
        mc.world.method_154(x, y - 1, z, below, 4); check(!p.method_932(), "non-ladder below became climbable");
      }
      mc.world.method_154(x, y - 1, z, 65, 2);
      mc.world.isRemote = true;
      try { check(!p.method_932(), "multiplayer acquired a local climb rule"); } finally { mc.world.isRemote = false; }
      for (int orientation = 0; orientation < 4; orientation++) {
        mc.world.method_154(x, y, z, 96, orientation | 4); mc.world.method_154(x, y - 1, z, 65, orientation + 2);
        double[] trap = fall(mc, x, y, z, false), sneak = fall(mc, x, y, z, true);
        mc.world.method_154(x, y, z, 65, orientation + 2);
        double[] ladder = fall(mc, x, y, z, false), ladderSneak = fall(mc, x, y, z, true);
        check(Math.abs(trap[0] - ladder[0]) < 1e-9 && Math.abs(trap[1] - ladder[1]) < 1e-9, "trapdoor changed ladder fall limits");
        check(Math.abs(sneak[0] - ladderSneak[0]) < 1e-9 && Math.abs(sneak[1] - ladderSneak[1]) < 1e-9, "trapdoor changed native sneak holding");
        double[][] reference = shortClimb(mc, x, y, z, orientation);
        mc.world.method_154(x, y, z, 96, orientation | 4);
        double[][] throughLeaf = shortClimb(mc, x, y, z, orientation);
        for (int tick = 0; tick < reference.length; tick++) for (int component = 0; component < reference[tick].length; component++)
          check(Math.abs(reference[tick][component] - throughLeaf[tick][component]) < 1e-9,
              "short native climb differs orientation=" + orientation + " tick=" + tick + " component=" + component
                  + " ladder=" + Arrays.toString(reference[tick]) + " trapdoor=" + Arrays.toString(throughLeaf[tick])
                  + " height=" + p.spacingY + " eyeOffset=" + p.field_1631);
        check(reference[7][1] > 0, "short native ladder fixture did not rise: " + Arrays.deepToString(reference));
        log("TRAPDOOR474 short native climb orientation=" + orientation + " rise=" + throughLeaf[7][1]
            + " trajectory=" + Arrays.deepToString(throughLeaf));
      }
      unsupportedClimb(mc, x, y, z);
      inputPipeline(mc, x, y, z);
      collisionGuards(mc, x, y, z, mob);
      placementChecks(mc, x, y, z);
      log("TRAPDOOR473 predicates=" + predicates + " movement/placement PASS");
    } finally {
      mc.world.isRemote = false; GameplayConfig.preview(old); p.field_161 = input;
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[3], b[4]);
      p.read(saved); CreativeVehicleChecks.mode(mc, mode); mc.setScreen(previous);
    }
    check(Arrays.equals(config, Files.readAllBytes(PowerConfig.path())), "trapdoor fixture changed saved settings");
  }
  private static double[][] shortClimb(Minecraft mc, int x, int y, int z, int orientation) {
    var p = mc.player;
    p.field_1640 = 0; p.field_161.field_2536 = false;
    p.method_1341(x + .5, y + .05, z + .5, new float[]{0,180,-90,90}[orientation], 0);
    p.velocityX = p.velocityY = p.velocityZ = 0;
    p.field_1623 = p.field_1624 = p.field_1625 = p.field_1626 = false;
    double feet = p.boundingBox.minY;
    double[][] result = new double[8][8];
    for (int tick = 0; tick < result.length; tick++) {
      p.method_945(0, 1);
      result[tick] = new double[]{p.x, p.boundingBox.minY - feet, p.z, p.velocityX, p.velocityY, p.velocityZ,
          p.field_1624 ? 1 : 0, p.method_932() ? 1 : 0};
    }
    return result;
  }
  private static double[] fall(Minecraft mc, int x, int y, int z, boolean sneak) {
    var p = mc.player; p.field_161.field_2536 = sneak; p.method_1341(x + .5, y + .6, z + .5, 0, 0);
    p.velocityX = p.velocityZ = 0; p.velocityY = -1; double feet = p.boundingBox.minY;
    p.method_945(0, 0); return new double[]{p.boundingBox.minY - feet, p.velocityY};
  }
  private static void placementChecks(Minecraft mc, int x, int y, int z) {
    for (boolean support : new boolean[]{false, true}) {
      for (int[] side : new int[][]{{0,1},{0,-1},{1,0},{-1,0}})
        mc.world.method_154(x + side[0], y, z + side[1], support ? 1 : 0, 0);
      boolean[] placement = new boolean[6];
      for (boolean choice : new boolean[]{false, true}) {
        var settings = GameplayConfig.copy(); settings.modernTrapdoorClimbing = choice; GameplayConfig.preview(settings);
        // Native placement callbacks may update neighbors. Compare the same empty scene each time.
        mc.world.method_154(x, y, z, 0, 0);
        for (int face = 0; face < 6; face++) {
          boolean allowed = Block.field_1913.method_1561(mc.world, x, y, z, face);
          if (!choice) placement[face] = allowed;
          else check(allowed == placement[face], "climbing changed placement eligibility support=" + support + " face=" + face);
        }
        if (support) for (int face = 2; face < 6; face++) {
          mc.world.method_154(x, y, z, 96, 0);
          Block.field_1913.method_1606(mc.world, x, y, z, face);
          check(mc.world.getBlockId(x, y, z) == 96 && mc.world.method_1778(x, y, z) == face - 2,
              "native placement orientation changed");
        }
      }
    }
  }
  private static void unsupportedClimb(Minecraft mc, int x, int y, int z) throws Exception {
    var settings = GameplayConfig.copy(); settings.modernCrawling = true; GameplayConfig.preview(settings);
    int scenarios = 0;
    double[][][] standingWalk = new double[4][][];
    for (String mode : new String[]{"SURVIVAL", "CREATIVE"}) {
      CreativeVehicleChecks.mode(mc, mode);
      for (int orientation = 0; orientation < 4; orientation++) {
        for (String entry : new String[]{"walk", "slow", "fast", "jump", "sneak", "left-edge", "right-edge", "descent"}) {
          double[][] reference = trajectory(mc, x, y, z, orientation, entry, false);
          double[][] trapdoor = trajectory(mc, x, y, z, orientation, entry, true);
          if (mode.equals("SURVIVAL") && entry.equals("walk")) standingWalk[orientation] = trapdoor;
          boolean rising = false, above = false;
          double maxDifference = 0;
          for (int tick = 0; tick < reference.length; tick++) {
            for (int axis = 0; axis < reference[tick].length; axis++) {
              double difference = Math.abs(reference[tick][axis] - trapdoor[tick][axis]);
              maxDifference = Math.max(maxDifference, difference);
              check(difference < 1e-9, "trapdoor trajectory differs from native ladder mode=" + mode
                  + " orientation=" + orientation + " entry=" + entry + " tick=" + tick
                  + " component=" + axis + " ladder=" + reference[tick][axis] + " trapdoor=" + trapdoor[tick][axis]);
            }
            double feet = trapdoor[tick][1];
            if (!entry.equals("descent")) {
              if (tick > 0 && !above) {
                double rise = feet - trapdoor[tick - 1][1];
                if (rise > .01) rising = true;
                if (rising) check(rise > .01, "stall/kick during ladder-to-leaf ascent entry=" + entry + " tick=" + tick + " rise=" + rise);
              }
              above |= feet > y + 2;
            }
          }
          if (!entry.equals("descent")) check(above, "trajectory did not cross above the unsupported leaf: " + entry);
          else check(trapdoor[trapdoor.length - 1][1] < y + .5, "trajectory did not descend through the leaf");
          log("TRAPDOOR474 trajectory mode=" + mode + " orientation=" + orientation + " entry=" + entry
              + " ticks=" + reference.length + " maxDifference=" + maxDifference);
          scenarios++;
        }
      }
    }
    CreativeVehicleChecks.mode(mc, "SURVIVAL");
    settings.modernCrawling = false; GameplayConfig.preview(settings);
    for (int orientation = 0; orientation < 4; orientation++) {
      double[][] crawlingOff = trajectory(mc, x, y, z, orientation, "walk", true);
      for (int tick = 0; tick < crawlingOff.length; tick++) for (int component = 0; component < crawlingOff[tick].length; component++)
        check(Math.abs(crawlingOff[tick][component] - standingWalk[orientation][tick][component]) < 1e-9,
            "standing trapdoor passage depends on Modern crawling orientation=" + orientation + " tick=" + tick + " component=" + component);
    }
    settings.modernCrawling = true; GameplayConfig.preview(settings);
    log("TRAPDOOR474 native per-tick trajectories=" + scenarios + " plus Modern crawling-off parity in four directions PASS");
  }
  private static double[][] trajectory(Minecraft mc, int x, int y, int z, int orientation, String entry, boolean trapdoor) {
    var p = mc.player;
    scene(mc, x, y, z, orientation, trapdoor);
    int[] side = new int[][]{{0,1},{0,-1},{1,0},{-1,0}}[orientation];
    boolean descending = entry.equals("descent");
    double edge = entry.equals("left-edge") ? -.48 : entry.equals("right-edge") ? .48 : 0;
    double contact = .875 - p.spacingXZ / 2.0;
    double startX = x + .5 + side[1] * edge, startZ = z + .5 - side[0] * edge;
    if (descending) {
      startX += side[0] * (contact - .5);
      startZ += side[1] * (contact - .5);
    }
    p.field_1640 = 0;
    p.field_161.field_2536 = false;
    p.method_1341(startX, descending ? y + 2.05 : y - .95, startZ, 0, 0);
    ModernSwimming.tick(p);
    p.field_161.field_2536 = entry.equals("sneak");
    p.method_1341(startX, descending ? y + 2.05 : y - .95, startZ, new float[]{0,180,-90,90}[orientation], 0);
    p.velocityX = side[0] * (entry.equals("fast") ? .15 : 0);
    p.velocityZ = side[1] * (entry.equals("fast") ? .15 : 0);
    p.velocityY = descending ? -.1 : entry.equals("jump") ? .42f : 0;
    p.field_1623 = p.field_1624 = p.field_1625 = p.field_1626 = false;
    float forward = descending ? 0 : entry.equals("slow") ? .1f : entry.equals("sneak") ? .3f : 1;
    double[][] result = new double[36][11];
    for (int tick = 0; tick < result.length; tick++) {
      ModernSwimming.tick(p);
      if (!entry.equals("sneak")) check(!ModernSwimming.lowPose(p), "open leaf forced a lower pose at tick " + tick);
      p.method_945(0, forward);
      result[tick] = new double[]{p.x, p.boundingBox.minY, p.z, p.velocityX, p.velocityY, p.velocityZ,
          p.field_1624 ? 1 : 0, p.field_1625 ? 1 : 0, p.method_932() ? 1 : 0, p.spacingY, p.field_1631};
    }
    return result;
  }
  private static void scene(Minecraft mc, int x, int y, int z, int orientation, boolean trapdoor) {
    for (int dy = -1; dy <= 3; dy++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
      mc.world.method_154(x + dx, y + dy, z + dz, 0, 0);
    int[] side = new int[][]{{0,1},{0,-1},{1,0},{-1,0}}[orientation];
    for (int dy = -1; dy <= 0; dy++) {
      mc.world.method_154(x + side[0], y + dy, z + side[1], 1, 0);
      mc.world.method_154(x, y + dy, z, 65, orientation + 2);
    }
    mc.world.method_154(x, y + 1, z, trapdoor ? 96 : 65, trapdoor ? orientation | 4 : orientation + 2);
    check(mc.world.getBlockId(x + side[0], y + 1, z + side[1]) == 0, "trajectory fixture has leaf backing");
  }
  private static void inputPipeline(Minecraft mc, int x, int y, int z) throws Exception {
    var owner = mc.player; var view = mc.field_2807; var rules = AccessRules.copy(mc); boolean paused = mc.paused;
    int scenarios = 0;
    try {
      mc.paused = false; AccessRules.restore(mc, local.luke.power.rules.RuleSet.cheats());
      for (String mode : new String[]{"SURVIVAL", "CREATIVE"}) for (int orientation = 0; orientation < 4; orientation++) {
        for (String input : new String[]{"forward", "jump", "sneak"}) {
          double[][] reference = inputTrajectory(mc, owner, x, y, z, orientation, mode, input, false);
          double[][] trapdoor = inputTrajectory(mc, owner, x, y, z, orientation, mode, input, true);
          boolean above = false;
          for (int tick = 0; tick < reference.length; tick++) {
            for (int component = 0; component < reference[tick].length; component++) {
              check(Math.abs(reference[tick][component] - trapdoor[tick][component]) < 1e-8,
                  "native input pipeline diverged mode=" + mode + " orientation=" + orientation + " input=" + input
                      + " tick=" + tick + " component=" + component + " ladder=" + reference[tick][component]
                      + " trapdoor=" + trapdoor[tick][component]);
            }
            above |= trapdoor[tick][1] > y + 2;
          }
          check(above, "native held input did not cross above the leaf mode=" + mode + " orientation=" + orientation + " input=" + input);
          log("TRAPDOOR474 input pipeline mode=" + mode + " orientation=" + orientation + " input=" + input + " ticks=" + reference.length);
          scenarios++;
        }
      }
    } finally {
      mc.player = owner; mc.field_2807 = view; mc.paused = paused; AccessRules.restore(mc, rules);
    }
    log("TRAPDOOR474 native full player.tick input trajectories=" + scenarios + " PASS");
  }
  private static double[][] inputTrajectory(Minecraft mc, net.minecraft.entity.player.ClientPlayerEntity owner,
      int x, int y, int z, int orientation, String mode, String input, boolean trapdoor) throws Exception {
    scene(mc, x, y, z, orientation, trapdoor);
    var player = new net.minecraft.entity.player.ClientPlayerEntity(mc, mc.world, mc.session, owner.dimensionId);
    player.field_161 = new net.minecraft.class_41(); mc.player = player; mc.field_2807 = player;
    CreativeVehicleChecks.mode(mc, mode);
    player.method_1341(x + .5, y - .95, z + .5, new float[]{0,180,-90,90}[orientation], 0);
    player.velocityX = player.velocityY = player.velocityZ = 0;
    player.field_1623 = player.field_1624 = player.field_1625 = player.field_1626 = false;
    double[][] result = new double[48][11];
    for (int tick = 0; tick < result.length; tick++) {
      player.field_161.field_2532 = 0; player.field_161.field_2533 = 1;
      player.field_161.field_2535 = input.equals("jump"); player.field_161.field_2536 = input.equals("sneak");
      player.tick();
      if (!input.equals("sneak")) check(!ModernSwimming.lowPose(player), "held input forced a low pose at an open leaf");
      result[tick] = new double[]{player.x, player.boundingBox.minY, player.z, player.velocityX, player.velocityY, player.velocityZ,
          player.field_1624 ? 1 : 0, player.field_1625 ? 1 : 0, player.method_932() ? 1 : 0, player.spacingY, player.field_1631};
    }
    return result;
  }
  private static void collisionGuards(Minecraft mc, int x, int y, int z, LivingEntity mob) throws Exception {
    var p = mc.player;
    for (int dy = -1; dy <= 3; dy++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
      mc.world.method_154(x + dx, y + dy, z + dz, 0, 0);
    mc.world.method_154(x, y, z, 65, 2);
    for (String guard : new String[]{"valid", "disabled", "closed", "mismatched", "missing", "remote", "solid", "mob"}) {
      var settings = GameplayConfig.copy(); settings.modernTrapdoorClimbing = !guard.equals("disabled"); GameplayConfig.preview(settings);
      mc.world.method_154(x, y - 1, z, 65, 2);
      mc.world.method_154(x, y, z, guard.equals("missing") ? 0 : 65, guard.equals("mismatched") ? 3 : 2);
      mc.world.method_154(x, y + 1, z, guard.equals("solid") ? 1 : 96, guard.equals("closed") ? 0 : 4);
      var entity = guard.equals("mob") ? mob : p;
      entity.method_1341(x + .5, y - .9 - entity.field_1640, z + .875 - entity.spacingXZ / 2.0, 0, 0);
      double feet = entity.boundingBox.minY;
      mc.world.isRemote = guard.equals("remote");
      try { entity.move(0, .5, 0); } finally { mc.world.isRemote = false; }
      double rise = entity.boundingBox.minY - feet;
      check(guard.equals("valid") ? Math.abs(rise - .5) < 1e-9 : rise < .2,
          "trapdoor vertical collision guard=" + guard + " rise=" + rise);
    }
    var settings = GameplayConfig.copy(); settings.modernTrapdoorClimbing = true; GameplayConfig.preview(settings);
    mc.world.method_154(x, y, z, 65, 2); mc.world.method_154(x, y + 1, z, 96, 4);
    CreativeVehicleChecks.mode(mc, "SPECTATOR");
    try {
      p.method_1341(x + .5, y + 1.05 - p.field_1640, z + .5, 0, 0);
      check(((local.luke.power.input.PlayerMode)p).power$spectator(), "spectator collision guard did not enter Spectator");
      var nativeCollisions = mc.world.method_190(p, net.minecraft.util.math.Box.getOrCreate(x, y + 1, z, x + 1, y + 2, z + 1));
      check(!nativeCollisions.isEmpty() && TrapdoorClimbing.movementCollisions(p, nativeCollisions) == nativeCollisions,
          "Spectator acquired player ladder-continuation collision changes");
      check(!p.method_932(), "Spectator acquired trapdoor ladder detection");
    } finally { CreativeVehicleChecks.mode(mc, "SURVIVAL"); }
    p.method_1341(x + .5, y + 1.05 - p.field_1640, z + .5, 0, 0);
    double startZ = p.z; p.move(0, 0, .5);
    check(Math.abs(p.z - (z + .875 - p.spacingXZ / 2.0)) < 1e-9, "qualifying trapdoor lost native ladder contact");
    p.method_1341(x + .5, y + 1.05 - p.field_1640, z + .875 - p.spacingXZ / 2.0, 0, 0);
    startZ = p.z; p.move(0, 0, .1);
    check(Math.abs(p.z - startZ) < 1e-9, "leaf contact allowed movement through the wall");
    p.move(0, 0, -.1);
    check(Math.abs(p.z - startZ + .1) < 1e-9, "trapdoor wall blocked retreat");
    check(!mc.world.method_190(p, p.boundingBox.expand(.05, 0, .05)).isEmpty(), "world collision queries lost native leaf geometry");
    p.method_1341(x + .5, y + 2.1 - p.field_1640, z + .875 - p.spacingXZ / 2.0, 0, 0);
    double feet = p.boundingBox.minY; p.move(0, -.5, 0);
    check(Math.abs(p.boundingBox.minY - feet + .5) < 1e-9, "qualifying trapdoor blocked descent");
    mc.world.method_154(x, y + 3, z, 1, 0);
    // Native positioning adds the current sneak/step offset back to the feet.
    p.method_1341(x + .5, y + 3 - p.spacingY - .1 - p.field_1640, z + .875 - p.spacingXZ / 2.0, 0, 0);
    feet = p.boundingBox.minY;
    double clearance = y + 3 - p.boundingBox.maxY;
    p.move(0, .5, 0);
    check(clearance > 0 && clearance < .11 && Math.abs(p.boundingBox.minY - feet - clearance) < 1e-9
            && Math.abs(p.boundingBox.maxY - (y + 3)) < 1e-9,
        "trapdoor continuation bypassed a separate solid roof rise=" + (p.boundingBox.minY - feet)
            + " startFeet=" + feet + " endFeet=" + p.boundingBox.minY + " head=" + p.boundingBox.maxY
            + " roof=" + (y + 3) + " height=" + p.spacingY + " stepOffset=" + p.field_1640);
    p.method_1341(x + .5, y + 3 - p.spacingY + .1 - p.field_1640, z + .875 - p.spacingXZ / 2.0, 0, 0);
    feet = p.boundingBox.minY; ModernSwimming.tick(p);
    check(ModernSwimming.crouched(p) && p.boundingBox.maxY < y + 3,
        "real roof did not retain a safe crouching pose feet=" + p.boundingBox.minY + " head=" + p.boundingBox.maxY
            + " roof=" + (y + 3) + " height=" + p.spacingY + " stepOffset=" + p.field_1640);
    check(Math.abs(p.boundingBox.minY - feet) < 1e-9, "roof crouching moved the native feet");
    p.move(0, -.4, 0); feet = p.boundingBox.minY; ModernSwimming.tick(p);
    check(!ModernSwimming.lowPose(p) && Math.abs(p.boundingBox.minY - feet) < 1e-9,
        "descent retained a low pose or moved feet after clearing the real roof");
    mc.world.method_154(x, y + 3, z, 0, 0);
    log("TRAPDOOR474 unsupported/native collision and roof/pose guards PASS");
  }
  private static void capture(Minecraft mc) throws Exception {
    var previous = mc.currentScreen; var options = new PowerOptionsScreen(previous); mc.setScreen(options);
    int matrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    Path report = Path.of("power-beta-data/reports/trapdoor-climb-473"); Files.createDirectories(report);
    try {
      for (int[] size : new int[][]{{320, 240}, {854, 480}}) {
        options.init(mc, size[0], size[1]); UiChecks.field(options, "page", "Gameplay"); UiChecks.search(options, "Modern trapdoor climbing");
        check(find(options.session(), "gameplay.modernTrapdoorClimbing").page.equals("Gameplay"), "missing Gameplay setting");
        boolean visible = false;
        for (Object row : (List<?>)UiChecks.field(options, "rows")) {
          var setting = (Setting)UiChecks.call(row, "setting");
          visible |= setting != null && setting.id.equals("gameplay.modernTrapdoorClimbing");
        }
        check(visible, "trapdoor setting missing from search results");
        SettingsSnapshot.render(mc, options, size[0], size[1], report.resolve("setting-" + size[0] + ".png"));
      }
      for (var base : DefaultBases.Base.values()) check(DefaultBases.value(base, "gameplay.modernTrapdoorClimbing", new com.google.gson.JsonPrimitive(false)).getAsBoolean() == (base == DefaultBases.Base.POWER_BETA), "wrong trapdoor default for " + base);
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glPopAttrib(); GL11.glMatrixMode(matrix); mc.setScreen(previous);
    }
    log("TRAPDOOR473 settings/defaults capture PASS");
  }
}
