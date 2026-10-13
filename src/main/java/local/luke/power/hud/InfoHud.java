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
    if (!settings.enabled && !MenuPreferences.debugTimeOfDay()) return;
    long daylight = ((WorldCycles)mc.world.method_262()).power$daylightTime();
    time = "Time: " + HudText.time(daylight);
    if (!settings.enabled || ticks++ % 4 != 0) return;
    double vx = camera ? DetachedCamera.x() : mc.player.x;
    double vy = camera ? DetachedCamera.y() : mc.player.boundingBox.minY;
    double vz = camera ? DetachedCamera.z() : mc.player.z;
    if (!Double.isFinite(vx) || !Double.isFinite(vy) || !Double.isFinite(vz)) return;
    int x = (int)Math.floor(vx), y = (int)Math.floor(vy), z = (int)Math.floor(vz);
    coordinates = HudText.coordinates(x, y, z);
    direction = HudText.direction(camera ? DetachedCamera.facingYaw() : mc.player.yaw);
    day = "Day: " + Math.floorDiv(daylight, 24000L);
    chunk = "Chunk: " + Math.floorDiv(x, 16) + " / " + Math.floorDiv(z, 16);
    String debug = mc.debugText == null ? "" : mc.debugText;
    int split = debug.indexOf(' ');
    fps = "FPS: " + (split < 0 ? debug : debug.substring(0, split));
    boolean needsBiome = Arrays.stream(settings.clusters).anyMatch(c -> c.enabled && c.biome);
    if (needsBiome && (biomeX != x || biomeZ != z)) {
      var value = mc.world.method_1781().method_1787(x, z);
      biome = "Biome: " + (value == null ? "Unknown" : value.field_888);
      biomeX = x; biomeZ = z;
    }
  }

  public static String timeLine() { return time; }

  public static List<String> lines(InfoHudSettings.Cluster c) {
    var lines = new ArrayList<String>(7);
    if (c.coordinates || c.direction) lines.add(HudText.location(coordinates, direction, c.coordinates, c.direction));
    if (c.biome) lines.add(biome);
    if (c.timeOfDay) lines.add(time);
    if (c.day) lines.add(day);
    if (c.chunk) lines.add(chunk);
    if (c.fps) lines.add(fps);
    return lines;
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
        List<String> lines = lines(cluster);
        int maxWidth = Math.max(1, (int)((width - 8) / cluster.textSize));
        int maxRows = Math.max(0, (int)((height - 8) / (10 * cluster.textSize)));
        if (lines.size() > maxRows) lines = lines.subList(0, maxRows);
        int textWidth = 0;
        for (int i = 0; i < lines.size(); i++) {
          String line = lines.get(i);
          while (!line.isEmpty() && mc.textRenderer.getWidth(line) > maxWidth) line = line.substring(0, line.length() - 1);
          lines.set(i, line); textWidth = Math.max(textWidth, mc.textRenderer.getWidth(line));
        }
        var layout = InfoHudLayout.at(width, height, textWidth, lines.size(), cluster);
        int color = cluster.argb();
        GL11.glPushMatrix();
        try {
          GL11.glTranslated(layout.x(), layout.y(), 0);
          GL11.glScaled(cluster.textSize, cluster.textSize, 1);
          for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            mc.textRenderer.drawWithShadow(line, layout.lineX(textWidth, mc.textRenderer.getWidth(line)), i * 10, color);
          }
        } finally { GL11.glPopMatrix(); }
      }
    } finally { GL11.glPopAttrib(); }
  }
}
