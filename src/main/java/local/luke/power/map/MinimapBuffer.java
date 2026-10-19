package local.luke.power.map;

import java.util.List;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.*;

/** A north-up terrain buffer separates tick-owned colors from the final frame-owned transform. */
public final class MinimapBuffer {
  private static int framebuffer, texture, side, coverage, maximum;
  private static boolean failed, pending;
  private static MapCache source;
  private static MapSettings sourceSettings, opaqueSettings;
  private static long generation, refreshed;
  private static int style, dimension, layer;
  private static String context = "";
  private static double centerX, centerZ, scale;

  private MinimapBuffer() {}

  public static void clear() {
    if (framebuffer != 0) EXTFramebufferObject.glDeleteFramebuffersEXT(framebuffer);
    if (texture != 0) GL11.glDeleteTextures(texture);
    framebuffer = texture = side = 0;
    source = null;
    sourceSettings = opaqueSettings = null;
    failed = pending = false;
    refreshed = 0;
  }

  public static boolean draw(Minecraft mc, MapProjection view, MapSettings settings) {
    if (failed || !GLContext.getCapabilities().GL_EXT_framebuffer_object) return false;
    if (maximum == 0) maximum = Math.min(2048, GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE));
    double pixelScale = view.scale * MapRenderer.pixelsPerUnit();
    var layout = MapBufferLayout.create(view.width, MapRenderer.pixelsPerUnit(), maximum);
    if (layout == null) return false;
    int target = layout.side();
    int nextStyle =
        java.util.Objects.hash(
            settings.style(), settings.chunkGrid, settings.slimeChunks, settings.lightOverlay);
    double radius = view.width * Math.sqrt(2) / (2 * view.scale) + 2 / pixelScale;
    boolean outside =
        source != Maps.cache()
            || target != side
            || layout.coverage() != coverage
            || scale != pixelScale
            || dimension != Maps.dimension()
            || layer != Maps.layer()
            || !context.equals(Maps.layer() < 0 ? "" : Maps.cache().viewContext())
            || Math.abs(view.x - centerX) + radius >= coverage / (2 * pixelScale)
            || Math.abs(view.z - centerZ) + radius >= coverage / (2 * pixelScale);
    long now = System.nanoTime();
    if (outside
        || nextStyle != style
        || ((pending || generation != Maps.cache().generation())
            && now - refreshed >= 1_000_000_000L / settings.updateRate)) {
      if (!paint(mc, settings, target, layout.coverage(), pixelScale, view.x, view.z)) return false;
      style = nextStyle;
      refreshed = now;
    }
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    GL11.glColor4d(1, 1, 1, settings.opacity);
    double half = side / (2 * scale);
    var a = view.screen(centerX - half, centerZ - half);
    var b = view.screen(centerX + half, centerZ - half);
    var c = view.screen(centerX + half, centerZ + half);
    var d = view.screen(centerX - half, centerZ + half);
    MapRenderer.polygon(
        view.clip(
            List.of(
                new MapProjection.Point(a.x(), a.y(), 0, 1),
                new MapProjection.Point(b.x(), b.y(), 1, 1),
                new MapProjection.Point(c.x(), c.y(), 1, 0),
                new MapProjection.Point(d.x(), d.y(), 0, 0)),
            settings.circular));
    return true;
  }

  private static boolean paint(
      Minecraft mc,
      MapSettings settings,
      int target,
      int painted,
      double pixelScale,
      double x,
      double z) {
    int oldBuffer = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
    int oldMatrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION);
    GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW);
    GL11.glPushMatrix();
    try {
      if (target != side) {
        if (framebuffer != 0) EXTFramebufferObject.glDeleteFramebuffersEXT(framebuffer);
        if (texture != 0) GL11.glDeleteTextures(texture);
        framebuffer = EXTFramebufferObject.glGenFramebuffersEXT();
        texture = GL11.glGenTextures();
        side = target;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA8,
            side,
            side,
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            (java.nio.ByteBuffer) null);
        EXTFramebufferObject.glBindFramebufferEXT(
            EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebuffer);
        EXTFramebufferObject.glFramebufferTexture2DEXT(
            EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
            EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT,
            GL11.GL_TEXTURE_2D,
            texture,
            0);
        if (EXTFramebufferObject.glCheckFramebufferStatusEXT(
                EXTFramebufferObject.GL_FRAMEBUFFER_EXT)
            != EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT) {
          failed = true;
          return false;
        }
      } else
        EXTFramebufferObject.glBindFramebufferEXT(
            EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebuffer);
      GL11.glViewport(0, 0, side, side);
      GL11.glDisable(GL11.GL_SCISSOR_TEST);
      GL11.glColorMask(true, true, true, true);
      GL11.glClearColor(0, 0, 0, 0);
      GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glLoadIdentity();
      GL11.glOrtho(0, side, side, 0, -1, 1);
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glLoadIdentity();
      centerX = Math.floor(x);
      centerZ = Math.floor(z);
      scale = pixelScale;
      dimension = Maps.dimension();
      layer = Maps.layer();
      context = layer < 0 ? "" : Maps.cache().viewContext();
      source = Maps.cache();
      coverage = painted;
      if (sourceSettings != settings) {
        sourceSettings = settings;
        opaqueSettings = local.luke.power.config.Catalog.JSON.fromJson(
            local.luke.power.config.Catalog.JSON.toJson(settings), MapSettings.class);
        opaqueSettings.opacity = 1;
      }
      MapRenderer.terrain(
          source,
          new MapProjection(
              centerX,
              centerZ,
              scale,
              (side - coverage) / 2d,
              (side - coverage) / 2d,
              coverage,
              coverage,
              0),
          dimension,
          layer,
          false,
          opaqueSettings,
          !mc.world.isRemote,
          mc.world.getSeed(),
          1);
      generation = source.generation();
      pending = MapRenderer.pending();
      return true;
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glPopMatrix();
      GL11.glMatrixMode(oldMatrix);
      EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, oldBuffer);
      GL11.glPopAttrib();
    }
  }
}
