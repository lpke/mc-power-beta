package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import local.luke.power.bow.ModernBow;
import local.luke.power.config.Catalog;
import local.luke.power.flexible.*;
import local.luke.power.gameplay.*;
import local.luke.power.input.*;
import local.luke.power.rules.RuleSet;
import local.luke.power.sprint.SprintController;
import local.luke.power.storage.PowerConfig;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Actual crouch ticks and native camera, targeting, placement and launch consumers. */
final class Crouch474Checks {
  private Crouch474Checks() {}

  static void capture(Minecraft mc) throws Exception {
    check(Display.isActive() && mc.options.soundVolume == 0 && mc.options.musicVolume == 0,
        "focus and mute the disposable client");
    PowerConfig.flushPending(); byte[] preferences = Files.readAllBytes(PowerConfig.path());
    String live = Catalog.JSON.toJson(GameplayConfig.current());
    var previous = mc.currentScreen; var screen = new local.luke.power.ui.PowerOptionsScreen(previous);
    Path report = Path.of("power-beta-data/reports/crouch-474"); Files.createDirectories(report);
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    List<Map<String, Object>> rows = new ArrayList<>();
    try {
      screen.init(mc, 854, 480);
      var parent = find(screen.session(), "gameplay.crouchCameraHeight");
      var child = find(screen.session(), "gameplay.lowerCrouchCamera");
      check(parent.label.equals("Modern crouching") && child.label.equals("Lower camera when crouching"), "crouch labels changed");
      UiChecks.field(screen, "page", "Gameplay"); ((Set<?>) UiChecks.field(screen, "collapsed")).clear();
      // These values stay in the isolated Options draft. No changed/apply path is invoked.
      for (boolean[] selection : new boolean[][]{{false, true}, {true, true}, {true, false}}) {
        parent.value = new com.google.gson.JsonPrimitive(selection[0]); child.value = new com.google.gson.JsonPrimitive(selection[1]);
        for (int[] size : new int[][]{{320, 240}, {854, 480}}) {
          screen.init(mc, size[0], size[1]); UiChecks.search(screen, "crouch");
          List<?> listed = (List<?>) UiChecks.field(screen, "rows");
          Object parentRow = settingRow(listed, parent), childRow = settingRow(listed, child);
          check(parentRow != null && (childRow != null) == selection[0], "crouch search bypassed parent visibility");
          double firstY = ((Number) UiChecks.call(parentRow, "y")).doubleValue();
          if (childRow != null) firstY = Math.min(firstY, ((Number) UiChecks.call(childRow, "y")).doubleValue());
          double scroll = Math.max(0, firstY - 6);
          UiChecks.field(screen, "scroll", scroll); UiChecks.call(screen, "layout");
          int top = (int) UiChecks.call(screen, "top"), bottom = (int) UiChecks.call(screen, "bottom");
          for (Object row : new Object[]{parentRow, childRow}) if (row != null) {
            int at = top + ((Number) UiChecks.call(row, "y")).intValue() - ((Number) UiChecks.field(screen, "scroll")).intValue();
            check(at >= top && at + ((Number) UiChecks.call(row, "height")).intValue() <= bottom,
                "crouch control is clipped at width " + size[0]);
          }
          String name = "parent-" + selection[0] + "-camera-" + selection[1] + "-" + size[0] + ".png";
          SettingsSnapshot.render(mc, screen, size[0], size[1], report.resolve(name));
          rows.add(Map.of("file", name, "parent", selection[0], "camera", selection[1], "childVisible", childRow != null));
          UiChecks.search(screen, "Lower camera when crouching");
          check((settingRow((List<?>) UiChecks.field(screen, "rows"), child) != null) == selection[0],
              "exact child search exposed a hidden control");
          check(child.value.getAsBoolean() == selection[1], "hiding the child changed its draft value");
        }
      }
    } finally {
      if (screen.session() != null) screen.session().discard();
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(mode); mc.setScreen(previous);
    }
    check(live.equals(Catalog.JSON.toJson(GameplayConfig.current())), "UI draft changed live gameplay settings");
    check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "UI capture changed saved preferences");
    Files.writeString(report.resolve("settings-rows.json"), Catalog.JSON.toJson(rows));
    log("CROUCH474 settings capture at320/854 and conditional search PASS");
  }

  private static Object settingRow(List<?> rows, local.luke.power.config.Setting setting) throws Exception {
    for (Object row : rows) if (UiChecks.call(row, "setting") == setting) return row;
    return null;
  }

  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive()
        && !DetachedCamera.isActive() && !local.luke.power.building.camera.FreeLook.active()
        && mc.player.field_1595 == null && !InteractionState.carryingContainer,
        "focus a disposable singleplayer world outside vehicles and detached cameras");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    check(mc.interactionManager instanceof SingleplayerInteractionManager, "use the native Survival interaction manager");
    check(ModernBow.hand(mc.player) == null && !FlexiblePlacement.modifiersActive(), "finish bow and placement actions first");
    PowerConfig.flushPending(); byte[] preferences = Files.readAllBytes(PowerConfig.path());
    var owner = mc.player; var view = mc.field_2807; var screen = mc.currentScreen; var hit = mc.field_2823;
    var gameplay = GameplayConfig.copy(); var rules = AccessRules.copy(mc);
    boolean paused = mc.paused, third = mc.options.thirdPerson, bob = mc.options.bobView;
    var frontField = Class.forName("local.luke.power.controls.util.ModOptions").getField("frontView");
    boolean front = frontField.getBoolean(null);
    var useField = UseInput.class.getDeclaredField("held"); useField.setAccessible(true);
    var use = (BooleanSupplier) useField.get(null);
    var player = new ClientPlayerEntity(mc, mc.world, mc.session, owner.dimensionId);
    int x = ((int) Math.floor(owner.x) >> 4) * 16 + 8, z = ((int) Math.floor(owner.z) >> 4) * 16 + 8, y = 112;
    check(mc.world.method_155(x - 8, y - 1, z - 8, x + 8, y + 4, z + 8), "load the crouch fixture area first");
    List<int[]> blocks = new ArrayList<>(); List<Entity> spawned = new ArrayList<>();
    List<Map<String, Object>> results = new ArrayList<>();
    for (int bx = x - 6; bx <= x + 6; bx++) for (int bz = z - 6; bz <= z + 6; bz++) for (int by = y - 1; by <= y + 3; by++) {
      check(mc.world.method_1777(bx, by, bz) == null, "crouch fixture overlaps a block entity");
      blocks.add(new int[]{bx, by, bz, mc.world.getBlockId(bx, by, bz), mc.world.method_1778(bx, by, bz)});
    }
    Method transform = class_555.class.getDeclaredMethod("method_1851", float.class); transform.setAccessible(true);
    FloatBuffer matrix = BufferUtils.createFloatBuffer(16); int originalMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    try {
      mc.player = player; mc.field_2807 = player; mc.setScreen(null); mc.paused = false; mc.options.bobView = false;
      AccessRules.restore(mc, RuleSet.cheats()); CreativeVehicleChecks.mode(mc, "SURVIVAL");
      for (int[] block : blocks) mc.world.method_154(block[0], block[1], block[2], block[1] == y - 1 ? 1 : 0, 0);
      for (boolean parent : new boolean[]{false, true}) for (boolean lower : new boolean[]{false, true}) {
        options(parent, lower); reset(player, x, y, z);
        double maxEyeError = 0;
        for (int tick = 0; tick < 72; tick++) {
          player.field_161.field_2536 = tick < 36; player.tick();
          near(player.boundingBox.minY, y, 1e-5, "crouch transition moved feet");
          for (float delta : new float[]{0, .5f, 1}) {
            double logical = player.prevY + (player.y - player.prevY) * delta + InteractionEye.offset(player, delta, 0);
            for (int perspective = 0; perspective < 3; perspective++) {
              mc.options.thirdPerson = perspective != 0; frontField.setBoolean(null, perspective == 2);
              double actual = cameraY(mc, delta, transform, matrix);
              maxEyeError = Math.max(maxEyeError, Math.abs(actual - logical));
              near(actual, logical, 3e-5, "native perspective camera differs from interaction eye");
            }
            mc.options.thirdPerson = false; frontField.setBoolean(null, false);
            CameraTarget448Checks.checkOrigins(mc, delta);
          }
          if (tick == 35) {
            check(ModernSwimming.crouched(player) && !ModernSwimming.compact(player), "native sneak did not select crouch");
            near(mc.interactionManager.method_1715(), parent ? 4.5 : 4, 1e-6, "child changed Survival reach");
            near(cameraY(mc, 1, transform, matrix), y + (parent && lower ? 1.27 : 1.54), 3e-5, "settled selected camera");
            physicalProjectiles(player);
            var tag = new NbtCompound(); player.write(tag); player.read(tag); player.tick();
            near(player.boundingBox.minY, y, 1e-5, "saved crouch moved feet");
            near(((SwimPlayer) player).power$swim().cameraEye(0), parent && lower ? 1.27 : 1.62, 2e-6, "first reload camera target");
            for (int settle = 0; settle < 24; settle++) player.tick();
            near(cameraY(mc, 1, transform, matrix), y + (parent && lower ? 1.27 : 1.54), 3e-5, "reload selected camera");
          }
        }
        near(mc.interactionManager.method_1715(), 4, 1e-6, "standing retained crouch reach");
        near(cameraY(mc, 1, transform, matrix), y + 1.62, 3e-5, "release retained lowered camera");
        // Toggle both values while the native player remains crouched, then prove
        // that actual native picks and flexible placement use each resulting eye.
        player.field_161.field_2536 = true;
        for (boolean nextLower : new boolean[]{!lower, lower}) {
          options(parent, nextLower); for (int tick = 0; tick < 32; tick++) player.tick();
          near(cameraY(mc, 1, transform, matrix), y + (parent && nextLower ? 1.27 : 1.54), 3e-5, "live child toggle camera");
          near(mc.interactionManager.method_1715(), parent ? 4.5 : 4, 1e-6, "live child toggle changed reach");
          placement(mc, x, y, z);
        }
        modernBow(mc, spawned, transform, matrix);
        var roundTrip = Catalog.JSON.fromJson(Catalog.JSON.toJson(GameplayConfig.current()), GameplaySettings.class);
        check(roundTrip.crouchCameraHeight == parent && roundTrip.lowerCrouchCamera == lower, "setting round trip changed independent values");
        results.add(Map.of("modernCrouching", parent, "lowerCamera", lower, "maxCameraInteractionError", maxEyeError,
            "reach", parent ? 4.5 : 4, "cameraAboveFeet", parent && lower ? 1.27 : 1.54));
      }
      options(true, true);
      var disabledCrawling = GameplayConfig.copy(); disabledCrawling.modernCrawling = false; GameplayConfig.preview(disabledCrawling);
      reset(player, x, y, z); player.field_161.field_2536 = true;
      for (int tick = 0; tick < 32; tick++) player.tick();
      check(!ModernSwimming.crouched(player), "disabled crawling selected a modern physical crouch");
      near(mc.interactionManager.method_1715(), 4, 1e-6, "disabled crawling retained modern reach");
      near(cameraY(mc, 1, transform, matrix), y + 1.54, 3e-5, "disabled crawling changed Beta camera");
      options(false, false); reset(player, x, y + .5, z);
      mc.world.method_154(x, y, z, 44, 0); mc.world.method_154(x, y + 2, z, 1, 0);
      for (int tick = 0; tick < 32; tick++) player.tick();
      check(ModernSwimming.crouched(player), "low roof did not force safe crouching");
      near(cameraY(mc, 1, transform, matrix), y + .5 + 1.27, 3e-5, "disabled switches raised forced camera into roof");
      mc.world.method_154(x, y, z, 0, 0); mc.world.method_154(x, y + 2, z, 0, 0); mc.world.method_154(x, y + 1, z, 1, 0);
      reset(player, x, y, z); for (int tick = 0; tick < 32; tick++) player.tick();
      check(ModernSwimming.compact(player), "one-block roof did not select compact pose");
      near(cameraY(mc, 1, transform, matrix), y + .4, 3e-5, "disabled switches raised compact camera");
      check(GL11.glGetError() == GL11.GL_NO_ERROR, "crouch checks left an OpenGL error");
    } finally {
      ModernBow.cancel(); UseInput.register(use);
      for (Entity entity : spawned) { entity.markDead(); entity.method_1340(x + 30, y + 8, z + 30); }
      for (int[] block : blocks) mc.world.method_154(block[0], block[1], block[2], block[3], block[4]);
      GameplayConfig.preview(gameplay); AccessRules.restore(mc, rules); mc.player = owner; mc.field_2807 = view;
      mc.field_2823 = hit; mc.options.thirdPerson = third; mc.options.bobView = bob; frontField.setBoolean(null, front);
      mc.paused = paused; mc.setScreen(screen); SprintController.reset(); GL11.glMatrixMode(originalMatrixMode);
    }
    check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "crouch fixture changed saved preferences");
    Path report = Path.of("power-beta-data/reports/crouch-474"); Files.createDirectories(report);
    Files.writeString(report.resolve("matrix.json"), Catalog.JSON.toJson(results));
    log("CROUCH474 native camera/reach/targeting/placement/projectile matrix PASS " + results);
  }

  private static void options(boolean parent, boolean lower) {
    var settings = GameplayConfig.copy(); settings.modernCrawling = true;
    settings.crouchCameraHeight = parent; settings.lowerCrouchCamera = lower; settings.modernBowHandling = true;
    GameplayConfig.preview(settings);
  }

  private static void reset(ClientPlayerEntity player, int x, double feet, int z) {
    var state = ((SwimPlayer) player).power$swim();
    state.swimming = state.compact = state.crouched = state.lowCeiling = false; state.previousAmount = state.amount = 0;
    state.resetCamera(1.62f); player.spacingXZ = .6f; player.spacingY = 1.8f; player.field_1631 = 1.62f; player.field_1640 = 0;
    player.field_161 = new class_41(); player.method_1341(x + .5, feet, z + .5, 0, 0);
    player.velocityX = player.velocityZ = 0; player.velocityY = -.08 * (double) .98f; player.field_1623 = true;
    SprintController.reset();
  }

  private static double cameraY(Minecraft mc, float delta, Method transform, FloatBuffer matrix) throws Exception {
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      GL11.glLoadIdentity(); transform.invoke(mc.field_2818, delta); matrix.clear(); GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrix);
      return mc.player.prevY + (mc.player.y - mc.player.prevY) * delta
          - (matrix.get(4) * matrix.get(12) + matrix.get(5) * matrix.get(13) + matrix.get(6) * matrix.get(14));
    } finally { GL11.glPopMatrix(); GL11.glMatrixMode(mode); }
  }

  private static void physicalProjectiles(ClientPlayerEntity player) {
    Entity[] shots = {new class_410(player.world, player), new class_499(player.world, player), new class_484(player.world, player)};
    for (Entity shot : shots) { near(shot.y, player.y + player.method_1378() - (double) .1f, 2e-6, "native physical projectile origin"); shot.markDead(); }
    var previous = player.fishHook;
    try { var bobber = new FishingBobberEntity(player.world, player); near(bobber.y, player.y - (double) .1f, 2e-6, "physical fishing origin"); bobber.markDead(); }
    finally { player.fishHook = previous; }
  }

  private static void placement(Minecraft mc, int x, int y, int z) {
    var player = mc.player; mc.world.method_154(x, y, z + 2, 1, 0);
    try {
      double eye = player.y + InteractionEye.offset(player, 1, 0);
      player.yaw = player.prevYaw = 0;
      player.pitch = player.prevPitch = (float) -Math.toDegrees(Math.atan2(y + .7 - eye, 1.5));
      mc.field_2818.method_1838(1); var target = mc.field_2823;
      check(target != null && target.field_1983 == class_212.TILE && target.field_1984 == x
          && target.field_1985 == y && target.field_1986 == z + 2, "selected camera missed native placement target");
      var plan = PlacementPlan.create(mc, new ItemStack(4, 1, 0), x, y, z + 2, target.field_1987, Modes.NONE);
      check(plan != null && plan.valid(), "flexible placement rejected the native eye/reach target");
    } finally { mc.world.method_154(x, y, z + 2, 0, 0); player.pitch = player.prevPitch = 0; }
  }

  private static void modernBow(Minecraft mc, List<Entity> spawned, Method transform, FloatBuffer matrix) throws Exception {
    var player = mc.player; CreativeVehicleChecks.mode(mc, "CREATIVE");
    double eye = cameraY(mc, 1, transform, matrix); var bow = new ItemStack(261, 1, 0);
    player.inventory.selectedSlot = 0; player.inventory.main[0] = bow;
    Set<Entity> before = Collections.newSetFromMap(new IdentityHashMap<>());
    for (Object entity : mc.world.field_198) before.add((Entity) entity);
    int previousCount = spawned.size();
    UseInput.register(() -> true); ModernBow.begin(player, bow);
    check(ModernBow.using(player, bow), "native bow draw did not start");
    for (int tick = 0; tick < 20; tick++) ModernBow.tick(mc);
    ModernBow.release(mc);
    for (Object entity : mc.world.field_198) if (entity instanceof class_410 arrow && !before.contains(arrow)) spawned.add(arrow);
    check(spawned.size() == previousCount + 1, "bow release did not spawn exactly one arrow");
    near(spawned.get(spawned.size() - 1).y, eye - (double) .1f, 2e-5, "modern bow ignored selected camera");
    CreativeVehicleChecks.mode(mc, "SURVIVAL");
  }

  private static void near(double actual, double expected, double tolerance, String label) {
    check(Math.abs(actual - expected) <= tolerance, label + ": " + actual + " expected " + expected);
  }
}
