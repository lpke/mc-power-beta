package local.luke.power.hud;

import java.util.*;
import local.luke.power.input.DetachedCamera;
import local.luke.power.ui.MenuPreferences;
import local.luke.power.world.WorldCycles;
import net.minecraft.class_564;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Sample shared values on ticks. Rendering only formats cached text and places clusters. */
public final class InfoHud {
  private static Object world;
  private static String coordinates = "", direction = "", biome = "", time = "", day = "", chunk = "", fps = "";
  private static int ticks, biomeX = Integer.MIN_VALUE, biomeZ = Integer.MIN_VALUE;
  private static boolean detached;
  private InfoHud() {}

  public static void tick(Minecraft mc) {
    if (world != mc.world) {
      world = mc.world; biomeX = Integer.MIN_VALUE; ticks = 0;
      coordinates = direction = biome = time = day = chunk = fps = "";
    }
    var settings = InfoHudConfig.current();
    if (mc.world == null || mc.player == null) return;
    boolean camera = DetachedCamera.isActive();
    if (detached != camera) { detached = camera; ticks = 0; biomeX = Integer.MIN_VALUE; }
    if (!settings.enabled) ticks = 0;
    if (!settings.enabled && !MenuPreferences.debugTimeOfDay()
        && !local.luke.power.map.MapConfig.current().clock) return;
    long daylight = ((WorldCycles)mc.world.method_262()).power$daylightTime();
    time = HudText.time(daylight);
    if (!settings.enabled || ticks++ % 4 != 0) return;
    double vx = camera ? DetachedCamera.x() : mc.player.x;
    double vy = HudCoordinates.y(mc.player);
    double vz = camera ? DetachedCamera.z() : mc.player.z;
    if (!Double.isFinite(vx) || !Double.isFinite(vy) || !Double.isFinite(vz)) return;
    int x = (int)Math.floor(vx), y = (int)Math.floor(vy), z = (int)Math.floor(vz);
    coordinates = HudText.coordinates(x, y, z);
    direction = HudText.direction(camera ? DetachedCamera.facingYaw() : mc.player.yaw);
    day = Long.toString(Math.floorDiv(daylight, 24000L));
    chunk = Math.floorDiv(x, 16) + " / " + Math.floorDiv(z, 16);
    String debug = mc.debugText == null ? "" : mc.debugText;
    int split = debug.indexOf(' ');
    fps = split < 0 ? debug : debug.substring(0, split);
    boolean needsBiome = Arrays.stream(settings.clusters).anyMatch(c -> c.enabled && c.biome);
    if (needsBiome && (biomeX != x || biomeZ != z)) {
      var value = mc.world.method_1781().method_1787(x, z);
      biome = value == null ? "Unknown" : value.field_888;
      biomeX = x; biomeZ = z;
    }
  }

  public static String timeLine() { return time.isEmpty() ? "" : "Time: " + time; }
  public static InfoHudData data() { return new InfoHudData(coordinates, direction, biome, time, day, chunk, fps); }

  public static List<String> lines(InfoHudSettings.Cluster c) {
    return data().lines(c);
  }

  public static void render(Minecraft mc) {
    var settings = InfoHudConfig.current();
    if (!settings.enabled || mc.world == null || mc.player == null || mc.options.hideHud) return;
    var size = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
    int width = size.method_1857(), height = size.method_1858();
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glAlphaFunc(GL11.GL_GREATER, 0);
      for (var cluster : settings.clusters) {
        if (!cluster.enabled || cluster.opacity == 0 || cluster.hideWithDebug && mc.options.debugHud) continue;
        InfoHudRenderer.draw(mc, InfoHudFrame.at(cluster, data(), width, height, mc.textRenderer::getWidth));
      }
    } finally { GL11.glPopAttrib(); }
  }
}
