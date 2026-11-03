package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import java.lang.reflect.*;
import java.nio.FloatBuffer;
import java.nio.file.*;
import java.util.*;
import local.luke.power.input.DetachedCamera;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.class_555;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Verifies real native bob matrices and movement before/after flight takes over a fall. */
final class Flight474Checks {
  private static net.minecraft.nbt.NbtCompound armed;
  private static String armedMode;
  private static double armedY;
  private static local.luke.power.rules.RuleSet armedRules;
  static void run(Minecraft mc, String action) throws Exception {
    check(Set.of("native", "arm", "check", "state").contains(action), "use flight-474 native|arm|check|state");
    check(mc.world != null && !mc.world.isRemote && mc.player != null && Display.isActive()
        && !DetachedCamera.isActive() && mc.player.field_1595 == null, "focus a disposable world outside vehicles/freecam");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    if (action.equals("state")) { state(mc); return; }
    if (action.equals("arm")) {
      check(armed == null, "finish the previous flight input check first");
      armed = new net.minecraft.nbt.NbtCompound(); mc.player.write(armed);
      var mode = (local.luke.power.input.PlayerMode)mc.player;
      armedMode = mode.power$spectator() ? "SPECTATOR" : mode.power$creative() ? "CREATIVE" : "SURVIVAL";
      armedRules = AccessRules.copy(mc); AccessRules.restore(mc, local.luke.power.rules.RuleSet.cheats());
      CreativeVehicleChecks.mode(mc, "CREATIVE");
      mc.setScreen(null);
      mc.player.getClass().getMethod("creative_setFlying", boolean.class).invoke(mc.player, false);
      mc.player.method_1340(mc.player.x, 220, mc.player.z); armedY = mc.player.y;
      mc.player.field_1623 = false; mc.player.velocityY = -1;
      state(mc);
      log("FLIGHT474 armed: double-tap the native jump binding, then flight-474 check"); return;
    }
    if (action.equals("check")) {
      check(armed != null, "arm the flight input check first");
      try {
        state(mc);
        check((boolean)mc.player.getClass().getMethod("creative_isFlying").invoke(mc.player), "native double-tap did not enable flight");
        check(mc.player.velocityY >= 0 && armedY - mc.player.y < 15, "native flight input retained its fall");
        log("FLIGHT474 native double-tap PASS");
      } finally {
        mc.player.read(armed); CreativeVehicleChecks.mode(mc, armedMode);
        AccessRules.restore(mc, armedRules); armed = null; armedRules = null;
      }
      return;
    }
    PowerConfig.flushPending(); byte[] config = Files.readAllBytes(PowerConfig.path());
    var p = mc.player; var saved = new net.minecraft.nbt.NbtCompound(); p.write(saved);
    var previousScreen = mc.currentScreen; var input = p.field_161; var view = mc.field_2807;
    boolean bob = mc.options.bobView;
    float[] animation = {p.field_1043, p.field_1044, p.field_524, p.field_525};
    Method flying = p.getClass().getMethod("creative_setFlying", boolean.class);
    Method isFlying = p.getClass().getMethod("creative_isFlying");
    Field fall = net.minecraft.entity.Entity.class.getDeclaredField("field_1636"); fall.setAccessible(true);
    Class<?> settingsClass = Class.forName("local.luke.power.creative.config.Config");
    Object settings = settingsClass.getMethod("current").invoke(null);
    Object originalSettings = settings.getClass().getMethod("copy").invoke(settings);
    Method preview = settingsClass.getMethod("preview", settings.getClass());
    String oldMode = ((local.luke.power.input.PlayerMode)p).power$spectator() ? "SPECTATOR"
        : ((local.luke.power.input.PlayerMode)p).power$creative() ? "CREATIVE" : "SURVIVAL";
    List<String> results = new ArrayList<>();
    try {
      mc.setScreen(null); mc.field_2807 = p; p.field_161 = new net.minecraft.class_41();
      CreativeVehicleChecks.mode(mc, "CREATIVE");
      for (int acceleration : new int[]{0, 5}) for (int glide : new int[]{0, 5}) for (double falling : new double[]{-.4, -2, -3.8}) {
        Object next = settings.getClass().getMethod("copy").invoke(originalSettings);
        field(next, "flight", true); field(next, "acceleration", acceleration); field(next, "glide", glide); preview.invoke(null, next);
        flying.invoke(p, false); p.method_1340(p.x, 220, p.z); p.field_1623 = false;
        p.velocityX = .08; p.velocityY = falling; p.velocityZ = -.06; fall.setFloat(p, 20);
        p.field_1043 = p.field_1044 = (float)Math.atan(-falling * .2) * 15;
        p.field_524 = p.field_525 = .08f;
        float tilt = p.field_1044;
        float[] before = matrix(mc, .5f);
        flying.invoke(p, true);
        float[] after = matrix(mc, .5f);
        check(Arrays.equals(before, after), "flight instantly removed the previous falling camera transform");
        check(p.velocityY == 0 && fall.getFloat(p) == 0, "resuming flight retained falling velocity/distance");
        check(p.velocityX == .08 && p.velocityZ == -.06, "flight transition discarded horizontal motion");
        check(p.field_1044 == tilt && p.field_1043 == tilt, "flight transition rewrote camera interpolation endpoints");
        double startY = p.y;
        for (int tick = 0; tick < 24; tick++) {
          float prior = p.field_1044;
          float priorWalk = p.field_525;
          p.tick();
          check(Math.abs(p.field_1043 - prior) < 1e-5, "native tick lost the previous tilt endpoint");
          check(Math.abs(p.field_1044 - prior * .2) < 1e-4, "flight introduced a new vertical tilt instead of settling");
          check(Math.abs(p.field_524 - priorWalk) < 1e-5 && Math.abs(p.field_525 - priorWalk * .6f) < 1e-5,
              "flight snapped or continued its walking bob");
          check(p.y >= startY - 1e-5, "hover retained falling displacement");
          for (float delta : new float[]{0, .25f, .5f, .75f, 1}) {
            // Isolate the vertical tilt after verifying native walking endpoints above.
            float walk = p.field_525, prevWalk = p.field_524;
            p.field_524 = p.field_525 = 0;
            float expected = p.field_1043 + (p.field_1044 - p.field_1043) * delta;
            float[] actual = matrix(mc, delta);
            p.field_525 = walk; p.field_524 = prevWalk;
            float observed = (float)Math.toDegrees(Math.atan2(actual[6], actual[5]));
            check(Math.abs(observed - expected) < 1e-4, "native camera did not interpolate the residual falling tilt");
          }
        }
        p.velocityY = .17; fall.setFloat(p, 3);
        flying.invoke(p, true);
        check(p.velocityY == .17 && fall.getFloat(p) == 3, "repeated flying state reset movement");
        flying.invoke(p, false); check(p.velocityY == 0 && fall.getFloat(p) == 0, "flight exit retained vertical momentum");
        p.velocityY = falling; fall.setFloat(p, 3); flying.invoke(p, false);
        check(p.velocityY == falling && fall.getFloat(p) == 3, "repeated nonflight state changed native falling");
        p.field_1044 = p.field_1043 = 0; p.tick();
        check(p.field_1044 > 0 && p.velocityY < falling, "ordinary Creative falling lost native camera tilt or gravity");
        results.add("acceleration=" + acceleration + " glide=" + glide + " falling=" + falling + " PASS");
      }
      flying.invoke(p, true); p.velocityY = .17;
      var airborne = new net.minecraft.nbt.NbtCompound(); p.write(airborne);
      flying.invoke(p, false); p.read(airborne);
      check((boolean)isFlying.invoke(p) && p.velocityY == .17, "loading a flying player discarded saved vertical motion");
      flying.invoke(p, false);
      check(!(boolean)isFlying.invoke(p), "fixture did not finish its nonflight checks");
      log("FLIGHT474 COMPLETE " + results.size() + " transitions, native matrix continuity/interpolation, stopping falls, no-op writes and native nonflight gravity");
    } finally {
      preview.invoke(null, originalSettings); p.field_161 = input; p.read(saved); CreativeVehicleChecks.mode(mc, oldMode);
      p.field_1043 = animation[0]; p.field_1044 = animation[1]; p.field_524 = animation[2]; p.field_525 = animation[3];
      mc.field_2807 = view; mc.options.bobView = bob; mc.setScreen(previousScreen);
      Path report = Path.of("power-beta-data/reports/flight-474"); Files.createDirectories(report); Files.write(report.resolve("results.txt"), results);
    }
    check(Arrays.equals(config, Files.readAllBytes(PowerConfig.path())), "flight checks changed saved preferences");
  }

  private static void state(Minecraft mc) throws Exception {
    Class<?> controller = Class.forName("local.luke.power.creative.FlightController");
    List<String> values = new ArrayList<>();
    for (String name : List.of("ticks", "lastJump", "jumpDown")) {
      Field field = controller.getDeclaredField(name); field.setAccessible(true); values.add(name + "=" + field.get(null));
    }
    Class<?> runtime = Class.forName("local.luke.power.creative.ClientRuntime");
    Class<?> modes = Class.forName("local.luke.power.creative.Modes");
    Object config = Class.forName("local.luke.power.creative.config.Config").getMethod("current").invoke(null);
    log("FLIGHT474 input " + values + " active=" + runtime.getMethod("active", Minecraft.class).invoke(null, mc)
        + " canEnable=" + modes.getMethod("canEnableFlight", Object.class).invoke(null, mc.player)
        + " binding=" + mc.options.jumpKey.code + " down=" + local.luke.power.input.Bindings.down(mc.options.jumpKey)
        + " input=" + mc.player.field_161.getClass().getSimpleName() + " focus=" + mc.field_2778
        + " screen=" + mc.currentScreen + " flight=" + field(config, "flight"));
  }

  private static float[] matrix(Minecraft mc, float delta) throws Exception {
    Method bob = class_555.class.getDeclaredMethod("method_1850", float.class); bob.setAccessible(true);
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      GL11.glLoadIdentity(); bob.invoke(mc.field_2818, delta);
      FloatBuffer values = BufferUtils.createFloatBuffer(16); GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, values);
      float[] result = new float[16]; values.get(result); return result;
    } finally { GL11.glPopMatrix(); GL11.glMatrixMode(mode); }
  }
}
