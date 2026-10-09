package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.FloatBuffer;
import java.nio.file.*;
import java.util.Arrays;
import local.luke.power.config.*;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.storage.PowerConfig;
import local.luke.power.visual.*;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

final class DamageCameraChecks {
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("saved")) {
      check(VisualConfig.current().damageCameraShake && !VisualConfig.current().fireDamageCameraShake, "damage camera preferences lost");
      log("PASS damage camera preferences survived restart"); return;
    }
    failures = 0;
    ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true); CreativeVehicleChecks.mode(mc, "SURVIVAL");
    mc.player.closeScreen(); mc.player.method_1340(10.5, 110, 10.5); mc.field_2807 = mc.player;
    for (int x = 9; x < 12; x++) for (int z = 9; z < 12; z++) for (int y = 108; y < 114; y++) mc.world.method_200(x,y,z,0);
    test("shake defaults on; Camera / View and Cancel preserve health and settings", () -> {
      configure(true, true); byte[] before = Files.readAllBytes(PowerConfig.path()); int health = mc.player.health;
      var session = SettingsRegistry.open(mc);
      for (String id : new String[] {"visual.damageCameraShake", "visual.fireDamageCameraShake"}) {
        var row = Release110Checks.find(session, id);
        check(row.defaultValue.getAsBoolean() && row.page.equals("Camera") && row.group.equals("View"), "wrong default/location"); row.parse("false");
      }
      session.preview(); check(!VisualConfig.current().damageCameraShake && !VisualConfig.current().fireDamageCameraShake, "preview failed");
      session.discard(); check(VisualConfig.current().damageCameraShake && VisualConfig.current().fireDamageCameraShake, "Cancel failed");
      check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())) && mc.player.health == health, "settings edit mutated save/health");
    });
    test("burn damage is attributed without changing damage or hurt timers", () -> {
      reset(mc); mc.player.fire = 20; mc.player.baseTick();
      check(mc.player.health == 19 && mc.player.hurtTime > 0 && fire(mc), "burn damage attribution failed");
      int health = mc.player.health, hurt = mc.player.hurtTime;
      configure(true, true); check(shakes(mc), "default burn shake missing");
      configure(true, false); check(!shakes(mc), "fire-only toggle retained burn shake");
      configure(false, true); check(!shakes(mc), "global toggle did not override fire");
      check(health == mc.player.health && hurt == mc.player.hurtTime, "shake toggle changed health/timers");
    });
    test("other damage while burning still shakes; rejected damage preserves attribution", () -> {
      configure(true, false); reset(mc); mc.player.fire = 20; mc.player.baseTick();
      check(!mc.player.damage(null, 1) && fire(mc), "rejected damage changed attribution");
      mc.player.field_1613 = 0; mc.player.damage(null, 2);
      check(!fire(mc) && shakes(mc), "non-fire damage while burning incorrectly suppressed");
    });
    test("fire contact suppresses shake; lava and lightning retain it", () -> {
      configure(true, false); reset(mc); mc.world.method_200(10,108,10,1); mc.world.method_200(10,109,10,51);
      mc.player.method_1340(10.5,110.62,10.5); mc.player.move(0,0,0);
      check(fire(mc) && !shakes(mc) && mc.player.health < 20, "fire contact unclassified: block="+mc.world.getBlockId(10,109,10)+" health="+mc.player.health+" attributed="+fire(mc));
      mc.world.method_200(10,109,10,0);
      reset(mc); var lava = Entity.class.getDeclaredMethod("method_1332"); lava.setAccessible(true); lava.invoke(mc.player);
      check(!fire(mc) && shakes(mc) && mc.player.health == 16, "lava misclassified");
      reset(mc); var strike = Entity.class.getDeclaredMethod("method_1392", int.class); strike.setAccessible(true); strike.invoke(mc.player, 5);
      check(!fire(mc) && shakes(mc) && mc.player.health == 15, "lightning helper misclassified");
    });
    test("global toggle handles unattributed multiplayer damage and preserves death tilt", () -> {
      reset(mc); mc.player.damage(null, 1); boolean remote = mc.world.isRemote;
      try {
        mc.world.isRemote = true; configure(true, false); check(shakes(mc), "remote cause guessed");
        configure(false, false); check(!shakes(mc), "global remote shake retained");
      } finally { mc.world.isRemote = remote; }
      mc.player.health = 0; mc.player.field_1041 = 20; check(shakes(mc), "death tilt removed"); reset(mc);
    });
    test("Apply persists independent damage options", () -> {
      var session = SettingsRegistry.open(mc); Release110Checks.find(session, "visual.damageCameraShake").parse("true");
      Release110Checks.find(session, "visual.fireDamageCameraShake").parse("false"); session.save(Path.of("."));
      check(PowerConfig.section("visual").get("damageCameraShake").getAsBoolean()
          && !PowerConfig.section("visual").get("fireDamageCameraShake").getAsBoolean(), "Apply lost damage preferences");
    });
    reset(mc); log("DAMAGE CAMERA FAILURES " + failures);
  }
  private static boolean fire(Minecraft mc) { return ((DamageCameraState)mc.player).power$isFireHurt(); }
  private static void reset(Minecraft mc) { mc.player.health = 20; mc.player.dead = false; mc.player.fire = 0; mc.player.field_1613 = 0; mc.player.hurtTime = 0; mc.player.field_1041 = 0; }
  private static void configure(boolean damage, boolean fire) {
    var s = VisualConfig.copy(); s.damageCameraShake = damage; s.fireDamageCameraShake = fire; VisualConfig.preview(s);
  }
  private static boolean shakes(Minecraft mc) throws Exception {
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      GL11.glLoadIdentity(); var method = class_555.class.getDeclaredMethod("method_1849", float.class); method.setAccessible(true); method.invoke(mc.field_2818, .5F);
      FloatBuffer matrix = BufferUtils.createFloatBuffer(16); GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrix);
      for (int i = 0; i < 16; i++) if (Math.abs(matrix.get(i) - (i % 5 == 0 ? 1F : 0F)) > .0001F) return true;
      return false;
    } finally { GL11.glPopMatrix(); }
  }
}
