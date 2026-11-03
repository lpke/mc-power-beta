package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.Catalog;
import local.luke.power.death.DeathItems;
import local.luke.power.gameplay.*;
import local.luke.power.flexible.*;
import local.luke.power.input.*;
import local.luke.power.rules.RuleSet;
import local.luke.power.storage.PowerConfig;
import local.luke.power.validation.mixin.Client253Access;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.modificationstation.stationapi.api.StationAPI;
import net.modificationstation.stationapi.api.event.entity.player.PlayerEvent;
import org.lwjgl.opengl.Display;

/** Real crouch ticks, native picking and a native right click three blocks down a wall. */
final class CrouchReach473Checks {
  private CrouchReach473Checks() {}
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && Display.isActive()
        && !DetachedCamera.isActive() && !InteractionState.carryingContainer, "focus a disposable singleplayer world outside freecam/carrying");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    check(mc.interactionManager instanceof SingleplayerInteractionManager, "use the native Survival interaction manager");
    check(!local.luke.power.flexible.FlexiblePlacement.modifiersActive(), "release flexible placement modifiers first");
    PowerConfig.flushPending(); byte[] preferences = Files.readAllBytes(PowerConfig.path());
    var owner = mc.player; var camera = mc.field_2807; var screen = mc.currentScreen; var hit = mc.field_2823;
    var gameplay = GameplayConfig.copy(); var building = local.luke.power.building.config.Config.current().copy(); var rules = AccessRules.copy(mc);
    Class<?> creative = Class.forName("local.luke.power.creative.config.Config"); Object before = creative.getMethod("current").invoke(null);
    before = before.getClass().getMethod("copy").invoke(before); var preview = creative.getMethod("preview", before.getClass());
    int x = (int)Math.floor(owner.x) + 5, z = (int)Math.floor(owner.z) + 5, y = 112;
    check(mc.world.method_155(x - 3, y - 4, z - 3, x + 3, y + 4, z + 3), "load the fixture area first");
    List<int[]> blocks = new ArrayList<>(); List<Map<String, Object>> results = new ArrayList<>();
    for (int bx = x - 3; bx <= x + 3; bx++) for (int bz = z - 3; bz <= z + 3; bz++) for (int by = y - 4; by <= y + 4; by++) {
      check(mc.world.method_1777(bx, by, bz) == null, "wall fixture overlaps a block entity");
      blocks.add(new int[]{bx, by, bz, mc.world.getBlockId(bx, by, bz), mc.world.method_1778(bx, by, bz)});
    }
    var player = new ClientPlayerEntity(mc, mc.world, mc.session, owner.dimensionId);
    player.field_161 = new class_41();
    try {
      mc.player = player; mc.field_2807 = player; mc.setScreen(null); AccessRules.restore(mc, RuleSet.cheats()); CreativeVehicleChecks.mode(mc, "SURVIVAL");
      var placement = building.copy(); placement.flexible.placeAgainstContainers = false;
      local.luke.power.building.config.Config.preview(placement);
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], 0, 0);
      for (int by = y - 3; by <= y; by++) mc.world.method_154(x, by, z, 1, 0);
      for (boolean lower : new boolean[]{true, false}) for (int[] normal : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
        configure(true, true, lower); pose(mc, x, y, z, normal, true);
        check(ModernSwimming.crouched(player) && !ModernSwimming.compact(player), "native ticks did not crouch the player");
        double eye = player.y + InteractionEye.offset(player, 1, 0) - player.boundingBox.minY;
        check(Math.abs(eye - (lower ? 1.27 : 1.54)) < 1e-5, "settled chosen eye differs from camera selection");
        double distance = aim(mc, x, y - 3, z, normal);
        check(distance > 4, "wall example must distinguish Beta and modern reach: " + distance);
        check(!target(player.method_929(4, 1), x, y - 3, z), "the old four-block ray already reaches this wall example");
        check(target(player.method_929(4.5, 1), x, y - 3, z) == lower,
            "high-camera fixture must fail with the old scalar 4.5 reach");
        place(mc, x, y, z, normal, "settled-camera-" + lower, results);
        // Change the child while holding crouch. Every easing tick must retain
        // native picking and a flexible plan from the unchanged chosen eye.
        configure(true, true, !lower);
        for (int tick = 0; tick < 32; tick++) {
          player.tick();
          place(mc, x, y, z, normal, "camera-" + !lower + "-tick-" + tick, results);
        }
        player.field_161.field_2536 = false;
        for (int tick = 0; tick < 32; tick++) {
          player.tick(); aim(mc, x, y - 3, z, normal);
          check(Math.abs(mc.interactionManager.method_1715() - 4) < 1e-6, "crouch release retained modern reach");
          mc.field_2818.method_1838(1);
          check(!target(mc.field_2823, x, y - 3, z), "released crouch retained the wall target");
        }
      }
      partialTickPicks(mc, x, y, z, results);
      verticalMotionReach(mc, x, y, z, results);
      for (boolean crawling : new boolean[]{false, true}) for (boolean parent : new boolean[]{false, true})
          for (boolean lower : new boolean[]{false, true}) for (boolean sneak : new boolean[]{false, true}) {
        if (crawling && parent && sneak) continue;
        configure(crawling, parent, lower); pose(mc, x, y, z, new int[]{1,0}, sneak); aim(mc, x, y - 3, z, new int[]{1,0});
        check(Math.abs(mc.interactionManager.method_1715() - 4) < 1e-6, "ordinary/Beta/off reach changed");
        mc.field_2818.method_1838(1); check(!target(mc.field_2823, x, y - 3, z), "off or standing state acquired modern wall reach");
      }
      configure(true, true, false); pose(mc, x, y, z, new int[]{1,0}, true);
      for (float pitch : new float[]{-90, -45, 0}) {
        player.pitch = player.prevPitch = pitch;
        check(range(player, class_212.TILE, 4) == 4.5, "higher camera extended horizontal/upward block reach");
      }
      aim(mc, x, y - 3, z, new int[]{1,0});
      check(range(player, class_212.TILE, 4) > 4.5 && range(player, class_212.TILE, 4) <= 4.85,
          "high chosen eye did not receive bounded downward compensation");
      check(range(player, class_212.ENTITY, 3) == 3, "entity interaction reach changed");
      check(range(player, class_212.TILE, 6) == 6, "custom block reach changed");
      var foreign = new ClientPlayerEntity(mc, mc.world, mc.session, owner.dimensionId);
      ((SwimPlayer)foreign).power$swim().crouched = true;
      check(range(foreign, class_212.TILE, 4) == 4, "foreign player acquired local reach");
      var serverPlayer = new PlayerEntity(mc.world) { public void method_494() {} };
      check(range(serverPlayer, class_212.TILE, 4) == 4, "server-side player acquired local reach");
      mc.world.isRemote = true; try { check(range(player, class_212.TILE, 4) == 4, "multiplayer acquired local reach"); } finally { mc.world.isRemote = false; }
      FreecamPreviewChecks.active(true); try { check(range(player, class_212.TILE, 4) == 4, "detached camera changed physical reach"); } finally { FreecamPreviewChecks.active(false); }
      var state = ((SwimPlayer)player).power$swim(); state.crouched = false; state.compact = true;
      check(range(player, class_212.TILE, 4) == 4, "compact pose acquired the crouching reach adjustment");
      state.compact = false; state.crouched = true;
      for (int configured : new int[]{40, 100}) {
        Object settings = before.getClass().getMethod("copy").invoke(before); settings.getClass().getField("blockReach").setInt(settings, configured); preview.invoke(null, settings);
        CreativeVehicleChecks.mode(mc, "CREATIVE");
        check(Math.abs(mc.interactionManager.method_1715() - configured / 10d) < 1e-6, "Creative configured reach changed");
        CreativeVehicleChecks.mode(mc, "SURVIVAL");
      }
      log("CROUCHREACH473 camera On/Off three-down native placement in four directions, transitions and scope guards PASS");
    } finally {
      FreecamPreviewChecks.active(false); mc.world.isRemote = false;
      GameplayConfig.preview(gameplay); local.luke.power.building.config.Config.preview(building); preview.invoke(null, before);
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[3], b[4]);
      mc.player = owner; mc.field_2807 = camera; mc.field_2823 = hit; AccessRules.restore(mc, rules); mc.setScreen(screen);
      Path report = Path.of("power-beta-data/reports/crouch-reach-473"); Files.createDirectories(report);
      Files.writeString(report.resolve("wall-placement.json"), Catalog.JSON.toJson(results));
    }
    check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "reach fixture changed saved settings");
  }
  private static void configure(boolean crawling, boolean parent, boolean lower) {
    var settings = GameplayConfig.copy(); settings.modernCrawling = crawling;
    settings.crouchCameraHeight = parent; settings.lowerCrouchCamera = lower; GameplayConfig.preview(settings);
  }
  private static void partialTickPicks(Minecraft mc, int x, int y, int z, List<Map<String, Object>> results) throws Exception {
    var player = mc.player;
    configure(true, true, false); pose(mc, x, y, z, new int[]{1,0}, true);
    double eye = player.y + InteractionEye.offset(player, 1, 0) - player.boundingBox.minY;
    for (float delta : new float[]{0, .5f, 1}) {
      float previousPitch = delta == .5f ? 90 : delta == 0 ? 85 : 0;
      float currentPitch = delta == .5f ? 70 : delta == 0 ? 0 : 85;
      float renderedPitch = previousPitch + (currentPitch - previousPitch) * delta;
      player.yaw = player.prevYaw = 90; player.pitch = currentPitch; player.prevPitch = previousPitch;
      var direction = player.method_926(delta);
      double expected = expectedRange(eye, renderedPitch), ordinary = expectedRange(eye, currentPitch);
      double distance = delta == 1 ? expected - .01 : (expected + ordinary) * .5;
      double remainingX = direction.x * (expected - distance);
      // Beta rejects near-parallel plane intersections below a squared 1e-7
      // axis delta. Keep the boundary probe comfortably outside that tolerance.
      check(remainingX * remainingX > 4e-7, "partial-tick fixture is inside the native plane-intersection tolerance");
      // Native method_1341 adds sneak smoothing to the physical feet. Preserve
      // the fixture's actual feet while moving its ray to the reach boundary.
      player.method_1341(x + 1 - direction.x * distance, y + 1 - player.field_1640, z + .5, 90, currentPitch);
      player.prevPitch = previousPitch; player.prevYaw = 90;
      player.prevX = player.x; player.prevY = player.y; player.prevZ = player.z;
      if (delta != 1) check(!target(player.method_929(ordinary, delta), x, y - 3, z),
          "partial-tick downward fixture does not distinguish the old current-only pitch: " + delta);
      var previous = CrouchReach.beginPick(player, delta);
      double scopedReach;
      try { scopedReach = mc.interactionManager.method_1715(); }
      finally { CrouchReach.endPick(previous); }
      String directHit = describe(player.method_929(scopedReach, delta));
      double faceDistance = (x + 1 - player.x) / direction.x;
      double faceY = player.prevY + (player.y - player.prevY) * delta
          + InteractionEye.offset(player, delta, 0) + direction.y * faceDistance;
      mc.field_2818.method_1838(delta);
      log("CROUCHREACH474 partial downward delta=" + delta + " expectedReach=" + expected
          + " scopedReach=" + scopedReach + " currentReach=" + ordinary + " faceDistance=" + faceDistance
          + " faceY=" + faceY + " targetY=" + (y - 3) + " feetY=" + player.boundingBox.minY
          + " direct=" + directHit + " renderer=" + describe(mc.field_2823));
      check(Math.abs(scopedReach - expected) < 1e-5, "scoped reach disagrees with interpolated ray pitch");
      check(target(mc.field_2823, x, y - 3, z), "native interpolated downward picker missed at delta " + delta);
      check(Math.abs(mc.interactionManager.method_1715() - ordinary) < 1e-5,
          "rendered downward reach leaked into ordinary interaction reach");
      results.add(Map.of("phase", "partial-downward", "delta", delta, "previousPitch", previousPitch,
          "currentPitch", currentPitch, "renderedPitch", renderedPitch, "picked", true));
    }
    mc.world.method_154(x + 3, y + 2, z + 3, 1, 0);
    try {
      for (float delta : new float[]{0, .5f, 1}) {
        float previousPitch = delta == .5f ? -85 : delta == 0 ? 0 : 85;
        float currentPitch = delta == 1 ? 0 : 85;
        player.yaw = player.prevYaw = -45; player.pitch = currentPitch; player.prevPitch = previousPitch;
        var direction = player.method_926(delta);
        player.method_1341(x + 3 - direction.x * 4.65, y + 1 - player.field_1640,
            z + 3 - direction.z * 4.65, -45, currentPitch);
        player.prevPitch = previousPitch; player.prevYaw = -45;
        player.prevX = player.x; player.prevY = player.y; player.prevZ = player.z;
        double ordinary = mc.interactionManager.method_1715();
        if (delta != 1) check(target(player.method_929(ordinary, delta), x + 3, y + 2, z + 3),
            "horizontal fixture does not distinguish the old current-only downward reach");
        mc.field_2818.method_1838(delta);
        check(!target(mc.field_2823, x + 3, y + 2, z + 3), "interpolated horizontal ray acquired downward compensation");
        check(Math.abs(mc.interactionManager.method_1715() - ordinary) < 1e-6, "horizontal render scope changed ordinary reach");
        results.add(Map.of("phase", "partial-horizontal", "delta", delta, "previousPitch", previousPitch,
            "currentPitch", currentPitch, "renderedPitch", 0, "extraReachRejected", true));
      }
    } finally { mc.world.method_154(x + 3, y + 2, z + 3, 0, 0); }
  }
  private static double expectedRange(double eye, float pitch) {
    double height = Math.max(0, Math.min(.35, eye - 1.27)), down = Math.sin(Math.toRadians(pitch));
    return pitch <= 0 ? 4.5 : Math.max(4.5, height * down + Math.sqrt(4.5 * 4.5 - height * height * (1 - down * down)));
  }
  private static String describe(class_27 hit) {
    if (hit == null) return "miss";
    if (hit.field_1983 != class_212.TILE) return "entity";
    return hit.field_1984 + "," + hit.field_1985 + "," + hit.field_1986 + "/face=" + hit.field_1987
        + (hit.field_1988 == null ? "" : "/point=" + hit.field_1988.x + "," + hit.field_1988.y + "," + hit.field_1988.z);
  }
  private static void verticalMotionReach(Minecraft mc, int x, int y, int z,
      List<Map<String, Object>> results) throws Exception {
    var player = mc.player;
    boolean third = mc.options.thirdPerson, bob = mc.options.bobView;
    int matrixMode = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_MATRIX_MODE);
    try {
      mc.options.thirdPerson = false; mc.options.bobView = false;
      for (boolean lower : new boolean[]{true, false}) {
        configure(true, true, lower); pose(mc, x, y, z, new int[]{1,0}, true);
        player.pitch = player.prevPitch = 85;
        double expected = expectedRange(lower ? 1.27 : 1.54, 85);
        for (double movement : new double[]{-.42, -.15, .15, .42}) for (float delta : new float[]{0, .5f, 1}) {
          player.prevY = player.y - movement;
          double previousY = player.prevY, currentY = player.y, feet = player.boundingBox.minY;
          double logical = previousY + movement * delta + InteractionEye.offset(player, delta, 0);
          var previous = CrouchReach.beginPick(player, delta);
          double reach;
          try { reach = mc.interactionManager.method_1715(); }
          finally { CrouchReach.endPick(previous); }
          check(Math.abs(reach - expected) < 1e-5,
              "vertical motion changed rendered reach camera=" + lower + " movement=" + movement + " delta=" + delta + " reach=" + reach);
          mc.field_2818.method_1838(delta);
          // The native first-person -0.1 view translation has a vertical
          // component when pitched. Compare the shared eye at neutral pitch,
          // as the camera-origin fixture does; reach above uses the downward ray.
          player.pitch = player.prevPitch = 0;
          try { CameraTarget448Checks.checkOrigins(mc, delta); }
          finally { player.pitch = player.prevPitch = 85; }
          check(player.prevY == previousY && player.y == currentY && player.boundingBox.minY == feet
              && Math.abs(player.method_931(delta).y + InteractionEye.offset(player, delta, 0) - logical) < 1e-9,
              "vertical-motion reach check changed the physical position or native logical ray");
          check(Math.abs(mc.interactionManager.method_1715() - expected) < 1e-5,
              "vertical-motion render scope changed current interaction reach");
          results.add(Map.of("phase", "vertical-motion", "lowerCamera", lower, "movementY", movement,
              "delta", delta, "reach", reach, "nativeOriginPreserved", true));
        }
        player.prevY = player.y;
      }
    } finally {
      mc.options.thirdPerson = third; mc.options.bobView = bob;
      org.lwjgl.opengl.GL11.glMatrixMode(matrixMode);
    }
  }
  private static void place(Minecraft mc, int x, int y, int z, int[] normal, String phase,
      List<Map<String, Object>> results) throws Exception {
    var player = mc.player;
    double feet = player.boundingBox.minY, eye = player.y + InteractionEye.offset(player, 1, 0);
    double distance = aim(mc, x, y - 3, z, normal), reach = mc.interactionManager.method_1715();
    check(distance <= reach && reach <= 4.85, "three-down target exceeds bounded chosen-eye reach: " + distance + "/" + reach);
    player.inventory.selectedSlot = 0; player.inventory.main[0] = DeathChecks.tagged(4, 8, 0, "crouch-wall-reach");
    String identity = DeathItems.encode(player.inventory.main[0]);
    mc.field_2818.method_1838(1); var hit = mc.field_2823;
    check(target(hit, x, y - 3, z), "native picking missed the wall three blocks below the supporting top block: " + phase);
    var plan = PlacementPlan.create(mc, player.inventory.main[0], x, y - 3, z, hit.field_1987, Modes.NONE);
    check(plan != null && plan.valid() && plan.destination().equals(new BlockPos(x + normal[0], y - 3, z + normal[1])),
        "flexible plan disagrees with the native wall target: " + phase);
    ((Client253Access)mc).power$use(1);
    check(mc.world.getBlockId(x + normal[0], y - 3, z + normal[1]) == 4, "native right click did not place beside the wall: " + phase);
    check(player.inventory.main[0] != null && player.inventory.main[0].count == 7, "Survival placement did not consume one block");
    player.inventory.main[0].count = 8;
    check(identity.equals(DeathItems.encode(player.inventory.main[0])), "placement changed native item data");
    check(Math.abs(player.boundingBox.minY - feet) < 1e-7
        && Math.abs(player.y + InteractionEye.offset(player, 1, 0) - eye) < 1e-7, "reach/placement changed the physical feet or chosen eye");
    mc.world.method_154(x + normal[0], y - 3, z + normal[1], 0, 0);
    results.add(Map.of("normalX", normal[0], "normalZ", normal[1], "phase", phase, "feetY", feet,
        "eyeY", eye, "distance", distance, "reach", reach, "placed", true, "flexiblePlanAgrees", true));
  }
  private static void pose(Minecraft mc, int x, int y, int z, int[] normal, boolean sneak) {
    var player = mc.player; var state = ((SwimPlayer)player).power$swim();
    state.swimming = state.compact = state.crouched = state.lowCeiling = false; state.resetCamera(1.62f);
    player.spacingXZ = .6f; player.spacingY = 1.8f; player.field_1631 = 1.62f; player.field_1640 = 0;
    player.method_1341(x + .5 + normal[0] * .7, y + 1, z + .5 + normal[1] * .7, 0, 0);
    player.velocityX = player.velocityY = player.velocityZ = 0; player.field_161.field_2536 = sneak;
    for (int tick = 0; tick < 24; tick++) player.tick();
  }
  private static double aim(Minecraft mc, int x, int y, int z, int[] normal) {
    var player = mc.player; double eye = player.y + InteractionEye.offset(player, 1, 0);
    double dx = x + .5 + normal[0] * .5 - player.x, dz = z + .5 + normal[1] * .5 - player.z, dy = y + .95 - eye;
    player.yaw = player.prevYaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
    player.pitch = player.prevPitch = (float)-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
    player.prevX = player.x; player.prevY = player.y; player.prevZ = player.z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }
  private static boolean target(class_27 hit, int x, int y, int z) { return hit != null && hit.field_1983 == class_212.TILE && hit.field_1984 == x && hit.field_1985 == y && hit.field_1986 == z; }
  private static double range(PlayerEntity player, class_212 type, double ordinary) { return StationAPI.EVENT_BUS.post(PlayerEvent.Reach.builder().player(player).type(type).currentReach(ordinary).build()).currentReach; }
}
