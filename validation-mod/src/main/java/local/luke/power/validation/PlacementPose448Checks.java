package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import local.luke.power.building.config.Config;
import local.luke.power.flexible.*;
import local.luke.power.gameplay.*;
import local.luke.power.input.InteractionEye;
import local.luke.power.storage.PowerConfig;
import net.minecraft.class_212;
import net.minecraft.class_27;
import net.minecraft.class_41;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Deterministic pose snapshots exercise native picking, the visible grid and native placement. */
final class PlacementPose448Checks {
  private record Pose(String name, boolean crouched, boolean compact, boolean lowCeiling,
      boolean lowerCamera, float previousEye, float eye, float smoothing) {}

  private static final Pose[] POSES = {
    new Pose("standing", false, false, false, false, 1.62f, 1.62f, 0),
    new Pose("crouch native camera", true, false, false, false, 1.62f, 1.62f, .08f),
    new Pose("crouch lowered camera", true, false, false, true, 1.27f, 1.27f, 0),
    new Pose("forced crouch", true, false, true, false, 1.27f, 1.27f, 0),
    new Pose("compact", false, true, true, false, .4f, .4f, 0),
    new Pose("entering crouch", true, false, false, true, 1.62f, 1.445f, 0),
    new Pose("leaving compact", false, false, false, false, .4f, 1.01f, 0)
  };

  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && Display.isActive(),
        "focus a muted disposable singleplayer world");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    check(mc.player.field_1595 == null && mc.player.field_1594 == null,
        "placement scene requires an unmounted player");
    check(!local.luke.power.input.DetachedCamera.isActive()
        && !local.luke.power.input.InteractionState.carryingContainer,
        "finish detached camera and carry operations first");
    failures = 0;
    var player = mc.player;
    var saved = new NbtCompound(); player.write(saved);
    var gameplay = GameplayConfig.copy(); var building = Config.current().copy();
    var visual = local.luke.power.visual.VisualConfig.copy();
    var rules = AccessRules.copy(mc);
    var input = player.field_161; var screen = mc.currentScreen;
    var view = mc.field_2807; var hit = mc.field_2823;
    boolean paused = mc.paused, focused = mc.field_2778;
    String mode = Class.forName("local.luke.power.creative.api.ModePlayer")
        .getMethod("power_mode").invoke(player).toString();
    Class<?> creativeConfig = Class.forName("local.luke.power.creative.config.Config");
    Object creative = creativeConfig.getMethod("current").invoke(null);
    Object creativeBefore = creative.getClass().getMethod("copy").invoke(creative);
    Method creativePreview = creativeConfig.getMethod("preview", creative.getClass());
    PowerConfig.flushPending(); byte[] preferences = Files.readAllBytes(PowerConfig.path());
    int x = ((int)Math.floor(player.x) >> 4) * 16 + 8;
    int z = ((int)Math.floor(player.z) >> 4) * 16 + 8, y = 112;
    var blocks = new ArrayList<int[]>();
    check(mc.world.method_155(x - 9, y - 2, z - 9, x + 9, y + 5, z + 9),
        "placement scene requires already loaded neighboring chunks");
    for (int bx = x - 7; bx <= x + 7; bx++) for (int bz = z - 7; bz <= z + 7; bz++)
      for (int by = y - 1; by <= y + 5; by++) {
        check(mc.world.method_1777(bx, by, bz) == null, "placement scene contains a block entity");
        blocks.add(new int[]{bx, by, bz, mc.world.getBlockId(bx, by, bz), mc.world.method_1778(bx, by, bz)});
      }
    try {
      mc.setScreen(null); mc.paused = false; mc.field_2778 = true; mc.field_2807 = player;
      AccessRules.restore(mc, local.luke.power.rules.RuleSet.cheats());
      creative.getClass().getField("flight").setBoolean(creative, true);
      creative.getClass().getField("blockReach").setInt(creative, 100);
      creative.getClass().getField("entityReach").setInt(creative, 100);
      creativePreview.invoke(null, creative);
      CreativeVehicleChecks.mode(mc, "CREATIVE");
      Class.forName("local.luke.power.creative.inventory.interfaces.CreativePlayer")
          .getMethod("creative_setFlying", boolean.class).invoke(player, false);
      var options = GameplayConfig.copy(); options.modernCrawling = options.modernWaterMovement = true;
      GameplayConfig.preview(options);
      var preview = local.luke.power.visual.VisualConfig.copy();
      preview.containerBlockReach = preview.containerEntityReach = 10;
      local.luke.power.visual.VisualConfig.preview(preview);
      var placement = building.copy(); placement.placement.enabled = false;
      placement.sneak.enabled = false; placement.slabs.enabled = false;
      placement.flexible.showOverlay = placement.flexible.showPreview = true;
      Config.preview(placement);
      player.field_161 = new class_41(); player.inventory.selectedSlot = 0;
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], 0, 0);
      mc.world.method_154(x, y, z, 1, 0);

      for (Pose pose : POSES) test("native placement face bands: " + pose.name(), () -> {
        pose(mc, pose);
        for (int pitch : new int[]{20, 30, 45}) for (float delta : new float[]{0, .5f, 1})
          for (int axis = 0; axis < 2; axis++) for (double edge : new double[]{.24, .26, .74, .76}) {
            double u = axis == 0 ? edge : .5, v = axis == 1 ? edge : .5;
            aim(mc, x, y, z, u, v, pitch, delta, 1.7);
            class_27 target = nativeHit(mc, x, y, z, delta);
            near(target.field_1988.x, x + 1 - u, 2e-5, "native face U");
            near(target.field_1988.z, z + v, 2e-5, "native face V");
            FaceGrid.Part expected = FaceGrid.part(u, v);
            PlacementPlan plan = plan(mc, target);
            check(plan != null && plan.part() == expected && plan.valid(),
                "wrong grid/preview at " + pose.name() + " pitch=" + pitch + " delta=" + delta
                    + " u=" + u + " v=" + v + " plan=" + plan);
            if (delta == 1) {
              independentRays(mc, target);
              PlacementOverlay.render(player, target, delta);
              check(GL11.glGetError() == GL11.GL_NO_ERROR, "placement preview left a GL error");
              var before = new NbtCompound(); player.write(before);
              double feet = player.boundingBox.minY; float height = player.spacingY;
              check(mc.interactionManager.method_1713(player, mc.world, player.inventory.getSelectedItem(),
                  target.field_1984, target.field_1985, target.field_1986, target.field_1987),
                  "native placement rejected its preview");
              BlockPos at = plan.destination();
              check(mc.world.getBlockId(at.x(), at.y(), at.z()) == 4, "placement disagrees with preview");
              mc.world.method_154(at.x(), at.y(), at.z(), 0, 0);
              near(player.boundingBox.minY, feet, 1e-8, "placement changed feet");
              near(player.spacingY, height, 1e-8, "placement changed physical pose");
              var after = new NbtCompound(); player.write(after);
              near(((net.minecraft.nbt.NbtDouble)after.getList("Pos").get(1)).value,
                  ((net.minecraft.nbt.NbtDouble)before.getList("Pos").get(1)).value, 1e-8,
                  "placement changed normalized saved height");
            }
          }
      });
      test("corrected reach accepts a hit just inside the configured limit", () -> {
        Object limited = creativeBefore.getClass().getMethod("copy").invoke(creativeBefore);
        limited.getClass().getField("blockReach").setInt(limited, 50);
        limited.getClass().getField("entityReach").setInt(limited, 50);
        limited.getClass().getField("flight").setBoolean(limited, true);
        creativePreview.invoke(null, limited);
        pose(mc, POSES[6]);
        double reach = mc.interactionManager.method_1715();
        near(reach, 5, 1e-6, "configured block reach");
        aim(mc, x, y, z, .5, .5, 45, 1, (reach - .001) * Math.sin(Math.PI / 4));
        var target = nativeHit(mc, x, y, z, 1);
        var plan = plan(mc, target);
        check(plan != null && plan.valid(), "raw physical origin rejected the corrected near-limit hit");
      });
      test("placement pose checks preserve global preferences", () ->
          check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "placement fixture wrote preferences"));
    } finally {
      FlexiblePlacement.interrupt();
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[3], b[4]);
      GameplayConfig.preview(gameplay); Config.preview(building); creativePreview.invoke(null, creativeBefore);
      local.luke.power.visual.VisualConfig.preview(visual);
      AccessRules.restore(mc, local.luke.power.rules.RuleSet.cheats());
      CreativeVehicleChecks.mode(mc, mode); AccessRules.restore(mc, rules);
      player.read(saved); player.field_161 = input;
      mc.field_2807 = view; mc.field_2823 = hit; mc.paused = paused; mc.field_2778 = focused; mc.setScreen(screen);
    }
    log("PLACEMENT POSE 448 FAILURES " + failures);
  }

  private static void pose(Minecraft mc, Pose pose) {
    var player = mc.player; var state = ((SwimPlayer)player).power$swim();
    var settings = GameplayConfig.copy(); settings.crouchCameraHeight = pose.lowerCamera();
    GameplayConfig.preview(settings);
    state.swimming = false; state.crouched = pose.crouched(); state.compact = pose.compact();
    state.lowCeiling = pose.lowCeiling(); state.previousEyeHeight = pose.previousEye(); state.eyeHeight = pose.eye();
    player.spacingXZ = .6f; player.spacingY = pose.compact() ? .6f : pose.crouched() ? 1.5f : 1.8f;
    player.field_1631 = pose.compact() ? .4f : pose.crouched() ? 1.27f : 1.62f;
    player.field_1640 = pose.smoothing();
    player.field_161.field_2536 = pose.crouched();
    player.velocityX = player.velocityY = player.velocityZ = 0;
    player.inventory.main[0] = new ItemStack(4, 64, 0);
    FlexiblePlacement.beginTick(mc, Modes.NONE, true);
    FlexiblePlacement.beginTick(mc, new Modes(false, true, false, false, false), true);
    check(FlexiblePlacement.canOperate(mc), "native placement fixture cannot operate");
  }

  private static void aim(Minecraft mc, int x, int y, int z, double u, double v,
      float pitch, float delta, double verticalDistance) {
    var player = mc.player; var state = ((SwimPlayer)player).power$swim();
    double expectedOffset = state.cameraEye(delta) - player.field_1631;
    near(InteractionEye.offset(player, delta, 0), expectedOffset, 1e-7, "logical eye correction");
    double rawY = y + 1 + verticalDistance - expectedOffset;
    player.yaw = player.prevYaw = 0; player.pitch = player.prevPitch = pitch;
    // Match Beta's float lookup table instead of Java's continuous trig functions.
    Vec3d look = player.method_926(delta);
    double distance = verticalDistance / -look.y;
    player.method_1340(x + 1 - u - look.x * distance, rawY, z + v - look.z * distance);
    player.prevX = player.field_1637 = player.x;
    player.prevY = player.field_1638 = player.y;
    player.prevZ = player.field_1639 = player.z;
  }

  private static class_27 nativeHit(Minecraft mc, int x, int y, int z, float delta) {
    mc.field_2818.method_1838(delta);
    class_27 hit = mc.field_2823;
    check(hit != null && hit.field_1983 == class_212.TILE && hit.field_1984 == x
        && hit.field_1985 == y && hit.field_1986 == z && hit.field_1987 == 1,
        "crosshair missed the expected top face");
    return hit;
  }

  private static PlacementPlan plan(Minecraft mc, class_27 hit) {
    return FlexiblePlacement.plan(mc, mc.player.inventory.getSelectedItem(), hit.field_1984,
        hit.field_1985, hit.field_1986, hit.field_1987);
  }

  private static void independentRays(Minecraft mc, class_27 expected) throws Exception {
    Class<?> selection = Class.forName("local.luke.power.worldedit.SelectionTarget");
    Method origin = selection.getDeclaredMethod("origin", Minecraft.class); origin.setAccessible(true);
    Vec3d start = (Vec3d)origin.invoke(null, mc);
    near(start.y, mc.player.y + InteractionEye.offset(mc.player, 1, 0), 1e-8, "WorldEdit ray origin");
    Method find = selection.getDeclaredMethod("find", Minecraft.class); find.setAccessible(true);
    Object position = find.invoke(null, mc);
    check(position != null && (int)position.getClass().getMethod("x").invoke(position) == expected.field_1984
        && (int)position.getClass().getMethod("y").invoke(position) == expected.field_1985
        && (int)position.getClass().getMethod("z").invoke(position) == expected.field_1986,
        "WorldEdit ray disagrees with native pick");
    Class<?> container = Class.forName("local.luke.power.inventory.ContainerTarget");
    Method pick = container.getDeclaredMethod("find", Minecraft.class, float.class); pick.setAccessible(true);
    class_27 hit = (class_27)pick.invoke(null, mc, 1f);
    check(hit != null && hit.field_1983 == class_212.TILE && hit.field_1984 == expected.field_1984
        && hit.field_1985 == expected.field_1985 && hit.field_1986 == expected.field_1986,
        "container preview ray disagrees with native pick");
  }

  private static void near(double actual, double expected, double tolerance, String label) {
    check(Math.abs(actual - expected) <= tolerance, label + ": " + actual + " expected " + expected);
  }
}
