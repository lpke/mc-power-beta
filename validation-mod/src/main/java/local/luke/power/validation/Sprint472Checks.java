package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.rules.GameRule;
import local.luke.power.sprint.*;
import local.luke.power.ui.PowerOptionsScreen;
import net.minecraft.class_41;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.nbt.NbtCompound;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Real movement and jump vectors under an actual held sprint binding. */
final class Sprint472Checks {
  private static KeyBinding binding; private static int originalCode;
  private Sprint472Checks() {}
  static void run(Minecraft mc, String phase) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable world");
    if (phase.equals("capture")) { capture(mc); return; }
    if (phase.equals("arm")) {
      check(binding == null, "sprint fixture already armed");
      binding = Arrays.stream(mc.options.allKeys).filter(key -> key instanceof Binding b && b.power$id().equals("powerbeta.sprint")).findFirst().orElseThrow();
      originalCode = binding.code; binding.code = Keyboard.KEY_F9; Bindings.register(mc.options.allKeys); mc.setScreen(null);
      log("SPRINT472 hold F9 for native checks"); return;
    }
    if (phase.equals("finish")) {
      if (binding != null) { binding.code = originalCode; binding = null; Bindings.register(mc.options.allKeys); }
      return;
    }
    check(phase.equals("native") && binding != null && Keyboard.isKeyDown(Keyboard.KEY_F9), "arm then hold F9");
    failures = 0; var p = mc.player; var saved = new NbtCompound(); p.write(saved);
    var input = p.field_161; var settings = SprintConfig.copy(); var rules = AccessRules.copy(mc);
    int x = (int)Math.floor(p.x), z = (int)Math.floor(p.z), y = 108;
    for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
      check(mc.world.getBlockId(x + dx, y - 1, z + dz) == 0, "sprint fixture needs an empty elevated floor");
    try {
      var permitted = AccessRules.copy(mc); permitted.set(GameRule.ALLOW_SPRINTING, true); AccessRules.restore(mc, permitted);
      for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) mc.world.method_154(x + dx, y - 1, z + dz, 1, 0);
      var choice = SprintConfig.copy(); choice.multidirectional = true; choice.activation = 1; choice.doubleTap = false;
      choice.stopAtWalls = false; choice.stamina = choice.noSprintInCombat = false; choice.speed = 130; choice.jumpBoost = true;
      choice.particles = false; SprintConfig.preview(choice);
      for (int forward = -1; forward <= 1; forward++) for (int sideways = -1; sideways <= 1; sideways++) {
        if (forward == 0 && sideways == 0) continue;
        final int f = forward, s = sideways;
        test("SPRINT472 native movement/jump forward=" + f + " sideways=" + s, () -> {
          reset(mc, x, y, z, f, s); var state = ((SprintPlayer)p).power$sprint(); state.cancel(false);
          double beforeX = p.x, beforeZ = p.z; p.method_945(s, f);
          double walking = Math.hypot(p.x - beforeX, p.z - beforeZ);
          reset(mc, x, y, z, f, s); SprintController.movement(p);
          check(state.sprinting && SprintController.active(p), "direction did not activate sprint");
          beforeX = p.x; beforeZ = p.z; p.method_945(s, f);
          double sprinting = Math.hypot(p.x - beforeX, p.z - beforeZ);
          check(walking > 0 && Math.abs(sprinting / walking - 1.3) < .02, "wrong native speed ratio " + sprinting / walking);
          p.velocityX = p.velocityZ = 0; SprintController.jumped(p);
          double length = Math.hypot(p.velocityX, p.velocityZ);
          check(Math.abs(length - .2) < 1e-6 && p.velocityX * s + p.velocityZ * f > 0, "jump boost opposes movement or gains diagonal speed");
        });
      }
      test("SPRINT472 disabled preference keeps forward-only activation", () -> {
        var off = SprintConfig.copy(); off.multidirectional = false; SprintConfig.preview(off);
        for (int[] direction : new int[][]{{0,1},{0,-1},{-1,0},{-1,1},{1,1}}) {
          reset(mc, x, y, z, direction[0], direction[1]); SprintController.movement(p);
          check(((SprintPlayer)p).power$sprint().sprinting == (direction[0] > 0), "disabled preference changed direction policy");
        }
      });
      test("SPRINT472 revoked sprint rule blocks sideways sprint", () -> {
        SprintConfig.preview(choice); var denied = AccessRules.copy(mc); denied.set(GameRule.ALLOW_SPRINTING, false); AccessRules.restore(mc, denied);
        reset(mc, x, y, z, 0, 1); SprintController.movement(p);
        check(!((SprintPlayer)p).power$sprint().sprinting, "multidirectional sprint bypassed rule");
      });
    } finally {
      SprintConfig.preview(settings); AccessRules.restore(mc, rules); p.read(saved); p.field_161 = input; SprintController.reset();
      for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) mc.world.method_154(x + dx, y - 1, z + dz, 0, 0);
    }
    log("SPRINT472 failures=" + failures); check(failures == 0, "multidirectional sprint failures=" + failures);
  }
  private static void reset(Minecraft mc, int x, int y, int z, int forward, int sideways) {
    var p = mc.player; p.method_1341(x + .5, y, z + .5, 0, 0); p.velocityX = p.velocityY = p.velocityZ = 0;
    p.field_1623 = true; p.field_1624 = false; p.field_161 = new class_41();
    p.field_161.field_2533 = forward; p.field_161.field_2532 = sideways;
    ((SprintPlayer)p).power$sprint().cancel(false); SprintController.reset();
  }
  private static void capture(Minecraft mc) throws Exception {
    var rules = AccessRules.copy(mc); var allowed = AccessRules.copy(mc); allowed.set(GameRule.ALLOW_SPRINTING, true);
    try { AccessRules.restore(mc, allowed); captureAllowed(mc); }
    finally { AccessRules.restore(mc, rules); }
  }
  private static void captureAllowed(Minecraft mc) throws Exception {
    var previous = mc.currentScreen; var screen = new PowerOptionsScreen(previous); screen.init(mc, 854, 480);
    Setting setting = screen.session().settings().stream().filter(s -> s.id.equals("sprint.multidirectional")).findFirst().orElseThrow();
    field(screen, "page", setting.page); ((Set<?>)field(screen, "collapsed")).clear(); field(screen, "scroll", 0d); call(screen, "layout");
    boolean found = false;
    for (Object row : (List<?>)field(screen, "rows")) if (call(row, "setting") == setting) { field(screen, "scroll", Math.max(0, ((Number)call(row, "y")).doubleValue() - 28)); found = true; break; }
    check(found, "multidirectional setting hidden from capture");
    call(screen, "layout"); Path report = Path.of("power-beta-data/reports/sprint-472"); Files.createDirectories(report);
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try { SettingsSnapshot.render(mc, screen, 854, 480, report.resolve("setting-854.png")); }
    finally { GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glPopAttrib(); GL11.glMatrixMode(mode); mc.setScreen(previous); }
    for (var base : DefaultBases.Base.values()) check(DefaultBases.value(base, setting.id, setting.powerBetaDefault).getAsBoolean() == (base == DefaultBases.Base.POWER_BETA), "multidirectional base default");
    log("SPRINT472 settings/defaults capture PASS");
  }
}
