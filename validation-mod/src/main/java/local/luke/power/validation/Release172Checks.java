package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import local.luke.power.config.SettingsRegistry;
import net.minecraft.class_27;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public final class Release172Checks {
  public static boolean captureOutline;
  public static int outlines;

  static void run(Minecraft mc) throws Exception {
    failures = 0;
    mc.setScreen(null);
    FreecamPreviewChecks.active(false);
    var cheats = AccessRules.of(mc.world.method_262());
    boolean beforeCheats = cheats.allAllowed();
    String beforeMode = mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    var session = SettingsRegistry.open(mc);
    try {
      cheats.allAllowed(true);
      test("spectator outlines have no setting", () -> {
        check(session.settings().stream().noneMatch(s -> s.id.equals("visual.spectatorBlockOutlines") || s.id.equals("power_camera:config.showCamera")
            || s.id.equals("power_client_fixes:config.enableWoodenSignChanges")), "removed setting remains");
      });
      test("native outlines remain off in Spectator without changing targeting", () -> {
        mc.world.method_154(32, 110, 32, 1, 0);
        var hit = new class_27(32, 110, 32, 1, Vec3d.createCached(32.5, 111, 32.5));
        Object target = mc.field_2823;
        captureOutline = true;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
          for (String mode : new String[] {"SURVIVAL", "CREATIVE", "SPECTATOR"}) {
            CreativeVehicleChecks.mode(mc, mode);
              outlines = 0;
              mc.worldRenderer.method_1554(mc.player, hit, 0, null, 1);
              check(outlines == (mode.equals("SPECTATOR") ? 0 : 1),
                  "outline draw differs: " + mode + "/" + outlines);
              check(mc.field_2823 == target, "interaction target changed");
              check(GL11.glGetError() == 0, "outline GL error");
          }
        } finally { GL11.glPopAttrib(); captureOutline = false; }
      });
    } finally {
      captureOutline = false; session.discard();
      CreativeVehicleChecks.mode(mc, beforeMode); cheats.allAllowed(beforeCheats);
    }
    log("RELEASE 172 FAILURES " + failures);
  }
}
