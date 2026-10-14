package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import local.luke.power.config.SettingsRegistry;
import local.luke.power.visual.VisualConfig;
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
    var cheats = (local.luke.power.permissions.CheatWorld) mc.world.method_262();
    boolean beforeCheats = cheats.power$cheatsEnabled();
    String beforeMode = mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    var session = SettingsRegistry.open(mc);
    var setting = find(session, "visual.spectatorBlockOutlines");
    try {
      cheats.power$cheatsEnabled(true);
      test("spectator outlines default off; removed settings are absent", () -> {
        check(!setting.defaultValue.getAsBoolean(), "outlines default on");
        check(setting.page.equals("Creative") && setting.group.equals("Spectator"), "wrong location");
        check(session.settings().stream().noneMatch(s -> s.id.equals("power_camera:config.showCamera")
            || s.id.equals("power_client_fixes:config.enableWoodenSignChanges")), "removed setting remains");
      });
      test("native outline draw follows mode and live preference without changing targeting", () -> {
        mc.world.method_154(32, 110, 32, 1, 0);
        var hit = new class_27(32, 110, 32, 1, Vec3d.createCached(32.5, 111, 32.5));
        Object target = mc.field_2823;
        captureOutline = true;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
          for (String mode : new String[] {"SURVIVAL", "CREATIVE", "SPECTATOR"}) {
            CreativeVehicleChecks.mode(mc, mode);
            for (boolean enabled : new boolean[] {false, true}) {
              setting.parse(Boolean.toString(enabled)); session.preview(true);
              outlines = 0;
              mc.worldRenderer.method_1554(mc.player, hit, 0, null, 1);
              check(outlines == (mode.equals("SPECTATOR") && !enabled ? 0 : 1),
                  "outline draw differs: " + mode + "/" + enabled + "/" + outlines);
              check(mc.field_2823 == target, "interaction target changed");
              check(GL11.glGetError() == 0, "outline GL error");
            }
          }
        } finally { GL11.glPopAttrib(); captureOutline = false; }
      });
      test("Cancel restores spectator outline preview", () -> {
        setting.parse(Boolean.toString(!setting.original().getAsBoolean())); session.preview(true);
        session.discard();
        check(VisualConfig.current().spectatorBlockOutlines == setting.original().getAsBoolean(), "Cancel left preview active");
      });
    } finally {
      captureOutline = false; session.discard();
      CreativeVehicleChecks.mode(mc, beforeMode); cheats.power$cheatsEnabled(beforeCheats);
    }
    log("RELEASE 172 FAILURES " + failures);
  }
}
