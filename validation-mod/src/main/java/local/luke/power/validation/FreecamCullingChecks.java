package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.Arrays;
import local.luke.power.input.DetachedCamera;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;

final class FreecamCullingChecks {
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    Object camera = Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    Object settings = Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);
    Field enabled = settings.getClass().getField("enabled"); Object oldEnabled = enabled.get(settings);
    Class<?> culling = Class.forName("dev.tr7zw.entityculling.EntityCullingMod");
    Field global = culling.getField("enabled"); boolean oldGlobal = global.getBoolean(null);
    byte[] before = Files.readAllBytes(PowerConfig.path());
    var mob = new net.minecraft.class_443(mc.world);
    var sign = new net.minecraft.block.entity.SignBlockEntity();
    try {
      enabled.set(settings, true); global.setBoolean(null, true); active(camera, false);
      for (Object entity : new Object[] {mob, sign}) {
        Class<?> type = entity.getClass();
        Method culled = type.getMethod("setCulled", boolean.class), out = type.getMethod("setOutOfCamera", boolean.class);
        test("freecam bypasses and restores culling for " + type.getSimpleName(), () -> {
          culled.invoke(entity, true); out.invoke(entity, true);
          for (int i = 0; i < 4; i++) {
            active(camera, false); check(flags(entity, true), "normal culling lost");
            active(camera, true); check(DetachedCamera.isActive() && flags(entity, false), "freecam retained culling");
            camera.getClass().getField("allowPlayerMovement").setBoolean(camera, true);
            check(flags(entity, false), "player movement mode restored culling too early");
            culled.invoke(entity, false); culled.invoke(entity, true);
            check(flags(entity, false), "worker update hid an entity in freecam");
            active(camera, false); check(!DetachedCamera.isActive() && flags(entity, true), "exit did not immediately restore culling");
          }
          active(camera, true); enabled.set(settings, false);
          check(!DetachedCamera.isActive() && flags(entity, true), "disabling freecam did not restore culling");
          enabled.set(settings, true);
          culled.invoke(entity, false); out.invoke(entity, false); active(camera, true); active(camera, false);
          check(flags(entity, false), "visible entity became hidden on exit");
        });
      }
      test("real mob renderer skips culled mobs normally and renders them in freecam", () -> {
        Object mod = culling.getField("instance").get(null);
        Field skipped = culling.getField("skippedEntities"), rendered = culling.getField("renderedEntities");
        mob.getClass().getMethod("setCulled", boolean.class).invoke(mob, true);
        Thread.sleep(120); // Let the vendor's forced-visible grace period expire.
        var renderer = net.minecraft.client.render.entity.EntityRenderDispatcher.field_2489;
        renderer.method_1917(mc.world, mc.textureManager, mc.textRenderer, mc.player, mc.options, 0F);
        int oldSkipped = skipped.getInt(mod), oldRendered = rendered.getInt(mod);
        for (boolean freecam : new boolean[] {false, true, false}) {
          active(camera, freecam);
          renderer.method_1920(mob, 0D, 0D, -3D, 0F, 0F);
          if (freecam) oldRendered++; else oldSkipped++;
          check(skipped.getInt(mod) == oldSkipped && rendered.getInt(mod) == oldRendered,
              "renderer ignored culling state: freecam=" + freecam);
        }
      });
      test("freecam respects globally disabled culling and leaves settings untouched", () -> {
        mob.getClass().getMethod("setCulled", boolean.class).invoke(mob, true);
        global.setBoolean(null, false); active(camera, true); active(camera, false);
        check(!(boolean) mob.getClass().getMethod("isCulled").invoke(mob), "freecam enabled disabled culling");
        check(!global.getBoolean(null), "global culling flag changed");
        check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())), "freecam wrote settings");
      });
      test("world exit restores camera and culling", () -> {
        global.setBoolean(null, true); active(camera, true); mc.setWorld(null);
        mc.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
        check(!DetachedCamera.isActive(), "camera survived world exit");
        check((boolean) mob.getClass().getMethod("isCulled").invoke(mob), "culling bypass survived world exit");
      });
    } finally { active(camera, false); enabled.set(settings, oldEnabled); global.setBoolean(null, oldGlobal); }
    log("FREECAM CULLING FAILURES " + failures);
  }
  private static boolean flags(Object entity, boolean expected) throws Exception {
    return (boolean) entity.getClass().getMethod("isCulled").invoke(entity) == expected
        && (boolean) entity.getClass().getMethod("isOutOfCamera").invoke(entity) == expected;
  }
  private static void active(Object camera, boolean value) throws Exception {
    camera.getClass().getMethod("setActive", boolean.class).invoke(camera, value);
  }
}
