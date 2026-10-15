package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.input.*;
import net.minecraft.client.Minecraft;

/** Bounded frame capture for physical ascent/descent and render-origin regressions. */
public final class FreecamMotionChecks {
  private static final List<String> frames = new ArrayList<>();
  private static boolean recording;
  private static long frameTime;
  public static void beginFrame() { frameTime = System.nanoTime(); }
  private static Object camera() throws Exception {
    return Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
  }
  public static void frame(Minecraft mc) {
    if (!recording || frames.size() >= 3000 || mc.world == null) return;
    try {
      Object camera = camera();
      frames.add(frameTime + "," + DetachedCamera.y() + "," + RenderedView.y() + ","
          + camera.getClass().getField("velocityY").getDouble(camera) + ","
          + camera.getClass().getField("jumping").getBoolean(camera) + ","
          + camera.getClass().getField("sneaking").getBoolean(camera) + ","
          + mc.player.y + "," + mc.player.prevY + "," + mc.player.field_161.field_2535 + ","
          + mc.player.field_161.field_2536);
    } catch (Exception e) { recording = false; throw new IllegalStateException("Motion capture failed", e); }
  }
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("setup")) {
      FreecamPreviewChecks.run(mc, "setup");
      Object camera = camera();
      camera.getClass().getMethod("setCameraPositionAndRotation", double.class, double.class,
          double.class, float.class, float.class, float.class).invoke(camera, 32.5, 120.0, 35.5, 0F, 0F, 0F);
      mc.options.thirdPerson = false;
      Object config = Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);
      config.getClass().getField("speed").set(config, 12F);
      config.getClass().getField("drag").set(config, 10F);
      config.getClass().getField("collision").set(config, false);
      mc.options.jumpKey.code = 57; mc.options.sneakKey.code = 42;
      return;
    }
    if (action.equals("start")) { frames.clear(); recording = true; return; }
    if (action.equals("stop")) {
      recording = false;
      Files.write(Path.of("power-beta-freecam-motion.csv"), frames);
      log("Captured " + frames.size() + " freecam frames"); return;
    }
    throw new IllegalArgumentException("Use setup, start or stop");
  }
}
