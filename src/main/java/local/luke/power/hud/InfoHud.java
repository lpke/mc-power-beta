package local.luke.power.hud;

import java.util.List;
import java.util.function.ToIntFunction;
import local.luke.power.input.DetachedCamera;
import local.luke.power.map.MapConfig;
import local.luke.power.map.Minimap;
import local.luke.power.ui.MenuPreferences;
import local.luke.power.ui.WorldMapScreen;
import local.luke.power.world.WorldCycles;
import net.minecraft.class_564;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Sample visible consumers on ticks and retain measured cluster frames between changes. */
public final class InfoHud {
  private static Object world, font;
  private static String coordinates = "", direction = "", biome = "", time = "", day = "", chunk = "", fps = "", timeLine = "";
  private static String fpsSource = "";
  private static int ticks, biomeX = Integer.MIN_VALUE, biomeZ = Integer.MIN_VALUE, demand;
  private static int coordinateX = Integer.MIN_VALUE, coordinateY, coordinateZ, chunkX = Integer.MIN_VALUE, chunkZ;
  private static long minute = Long.MIN_VALUE, daylightDay = Long.MIN_VALUE;
  private static boolean detached;
  private static InfoHudData snapshot = new InfoHudData("", "", "", "", "", "", "");
  private static ToIntFunction<String> measure;
  private static final InfoHudFrame.Cache[] FRAMES = {
    new InfoHudFrame.Cache(), new InfoHudFrame.Cache(), new InfoHudFrame.Cache(), new InfoHudFrame.Cache()
  };
  private InfoHud() {}

  public static void invalidateFrames() {
    font = null;
    for (var frame : FRAMES) frame.clear();
  }

  public static void tick(Minecraft mc) {
    if (world != mc.world) {
      world = mc.world; biomeX = coordinateX = chunkX = Integer.MIN_VALUE; ticks = demand = 0;
      minute = daylightDay = Long.MIN_VALUE;
      coordinates = direction = biome = time = day = chunk = fps = timeLine = fpsSource = "";
    }
    if (mc.world == null || mc.player == null) return;
    var settings = InfoHudConfig.current();
    var maps = MapConfig.current();
    boolean clock = !mc.options.hideHud && mc.options.debugHud && MenuPreferences.debugTimeOfDay()
        && MenuPreferences.debugLine("time") || maps.clock && (Minimap.radarActive(mc, maps)
            || mc.currentScreen instanceof WorldMapScreen screen && screen.minimapRadarActive());
    int nextDemand = InfoHudDemand.of(settings, mc.options.hideHud, mc.options.debugHud, clock);
    boolean camera = DetachedCamera.isActive();
    if (detached != camera || demand != nextDemand) {
      detached = camera; demand = nextDemand; ticks = 0; biomeX = Integer.MIN_VALUE;
    }
    if (demand == 0) { ticks = 0; return; }
    long daylight = (demand & (InfoHudDemand.TIME | InfoHudDemand.DAY)) == 0 ? 0
        : ((WorldCycles)mc.world.method_262()).power$daylightTime();
    if ((demand & InfoHudDemand.TIME) != 0) {
      long nextMinute = Math.floorMod(daylight, 24000L) * 60 / 1000;
      if (minute != nextMinute) {
        minute = nextMinute; time = HudText.time(daylight); timeLine = "Time: " + time;
      }
    }
    if ((demand & ~InfoHudDemand.TIME) == 0 || ticks++ % 4 != 0) return;
    boolean needsPosition = (demand & (InfoHudDemand.COORDINATES | InfoHudDemand.BIOME | InfoHudDemand.CHUNK)) != 0;
    if (needsPosition) {
      double vx = camera ? DetachedCamera.x() : mc.player.x,
          vy = HudCoordinates.y(mc.player), vz = camera ? DetachedCamera.z() : mc.player.z;
      if (!Double.isFinite(vx) || !Double.isFinite(vy) || !Double.isFinite(vz)) return;
      int x = (int)Math.floor(vx), y = (int)Math.floor(vy), z = (int)Math.floor(vz);
      if ((demand & InfoHudDemand.COORDINATES) != 0 && (coordinateX != x || coordinateY != y || coordinateZ != z)) {
        coordinates = HudText.coordinates(x, y, z); coordinateX = x; coordinateY = y; coordinateZ = z;
      }
      if ((demand & InfoHudDemand.CHUNK) != 0) {
        int cx = Math.floorDiv(x, 16), cz = Math.floorDiv(z, 16);
        if (chunkX != cx || chunkZ != cz) { chunk = cx + " / " + cz; chunkX = cx; chunkZ = cz; }
      }
      if ((demand & InfoHudDemand.BIOME) != 0 && (biomeX != x || biomeZ != z)) {
        var value = mc.world.method_1781().method_1787(x, z);
        biome = value == null ? "Unknown" : value.field_888; biomeX = x; biomeZ = z;
      }
    }
    if ((demand & InfoHudDemand.DIRECTION) != 0)
      direction = HudText.direction(camera ? DetachedCamera.facingYaw() : mc.player.yaw);
    if ((demand & InfoHudDemand.DAY) != 0) {
      long nextDay = Math.floorDiv(daylight, 24000L);
      if (daylightDay != nextDay) { daylightDay = nextDay; day = Long.toString(nextDay); }
    }
    if ((demand & InfoHudDemand.FPS) != 0) {
      String debug = mc.debugText == null ? "" : mc.debugText;
      if (!debug.equals(fpsSource)) {
        fpsSource = debug; int split = debug.indexOf(' '); fps = split < 0 ? debug : debug.substring(0, split);
      }
    }
  }

  public static String timeLine() { return timeLine; }
  public static InfoHudData data() {
    if (!coordinates.equals(snapshot.coordinates()) || !direction.equals(snapshot.direction()) || !biome.equals(snapshot.biome())
        || !time.equals(snapshot.time()) || !day.equals(snapshot.day()) || !chunk.equals(snapshot.chunk()) || !fps.equals(snapshot.fps()))
      snapshot = new InfoHudData(coordinates, direction, biome, time, day, chunk, fps);
    return snapshot;
  }
  public static List<String> lines(InfoHudSettings.Cluster c) { return data().lines(c); }

  public static void render(Minecraft mc) {
    var settings = InfoHudConfig.current();
    if (!settings.enabled || mc.world == null || mc.player == null || mc.options.hideHud) return;
    boolean visible = false;
    for (var cluster : settings.clusters)
      visible |= InfoHudDemand.visible(cluster, false, mc.options.debugHud) && InfoHudDemand.fields(cluster) != 0;
    if (!visible) return;
    if (font != mc.textRenderer) { font = mc.textRenderer; measure = mc.textRenderer::getWidth; }
    var size = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
    int width = size.method_1857(), height = size.method_1858();
    InfoHudData data = data();
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glAlphaFunc(GL11.GL_GREATER, 0);
      for (int i = 0; i < settings.clusters.length; i++) {
        var cluster = settings.clusters[i];
        if (!InfoHudDemand.visible(cluster, false, mc.options.debugHud) || InfoHudDemand.fields(cluster) == 0) continue;
        InfoHudRenderer.draw(mc, FRAMES[i].get(cluster, data, width, height, font, measure));
      }
    } finally { GL11.glPopAttrib(); }
  }
}
