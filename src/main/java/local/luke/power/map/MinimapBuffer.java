package local.luke.power.map;

import java.util.List;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.*;

/** Two bounded surfaces retain terrain while a cave or zoom replacement is prepared. */
public final class MinimapBuffer {
  private static Surface active, preparing;
  private static int maximum;
  private static final MinimapZoom zoom = new MinimapZoom();
  private static double requestedZoom = Double.NaN, displayedZoom = 1;
  private static MapSettings sourceSettings, opaqueSettings;
  private MinimapBuffer() {}
  public static void clear() {
    if (active != null) active.clear();
    if (preparing != null) preparing.clear();
    active = preparing = null; sourceSettings = opaqueSettings = null;
    zoom.reset(); requestedZoom = Double.NaN;
  }
  private static boolean contextMatches(Surface surface) {
    return surface != null && surface.source == Maps.cache() && surface.dimension == Maps.dimension()
        && surface.layer == Maps.layer()
        && surface.context.equals(Maps.layer() < 0 ? "" : Maps.cache().viewContext());
  }
  /** The marker projection uses the same display scale as the retained terrain. */
  public static MapProjection displayed(MapProjection request) {
    return scaled(request, displayedZoom);
  }
  private static MapProjection scaled(MapProjection view, double scale) {
    return new MapProjection(view.x, view.z, scale, view.left, view.top, view.width, view.height, view.angle);
  }
  public static boolean draw(Minecraft mc, MapProjection view, MapSettings settings) {
    if (Maps.cache() == null) return false;
    if (active != null && (active.source != Maps.cache() || active.dimension != Maps.dimension())) clear();
    long now = System.nanoTime();
    if (active == null) {
      active = new Surface(); zoom.set(view.scale, now); requestedZoom = view.scale;
    }
    if (requestedZoom != view.scale) {
      requestedZoom = view.scale;
      zoom.set(view.scale, now);
    }
    boolean sameContext = contextMatches(active);
    boolean sameZoom = active.texture == 0 || active.requestScale == view.scale;
    if (active.texture == 0 || sameContext && sameZoom) {
      if (preparing != null) { preparing.clear(); preparing = null; }
      if (!active.update(mc, view, settings)) return false;
      zoom.set(view.scale, now);
    } else {
      if (preparing != null && (!contextMatches(preparing) || preparing.requestScale != view.scale)) {
        preparing.clear(); preparing = null;
      }
      if (preparing == null) preparing = new Surface();
      if (!preparing.update(mc, view, settings)) return false;
      boolean caveReady = replacementReady(sameContext, sameZoom, active.knownSamples,
          preparing.renderedSamples, preparing.retainedMissing);
      boolean coversTransition = view.scale <= zoom.value(now) || zoom.value(now) == view.scale;
      if (caveReady && coversTransition) {
        active.clear(); active = preparing; preparing = null; zoom.set(view.scale, now);
      }
    }
    displayedZoom = zoom.value(now);
    var displayed = scaled(view, displayedZoom);
    if (preparing != null && sameContext) {
      preparing.drawReplacement(displayed, settings, active);
    } else active.draw(displayed, settings);
    return true;
  }
  static boolean replacementKnown(boolean sameContext, boolean sameZoom, int previous, int candidate) {
    return sameContext || candidate >= (sameZoom ? Math.max(1, (int)Math.ceil(previous * .9)) : 1);
  }
  static boolean replacementReady(boolean sameContext, boolean sameZoom, int previous, int rendered, int retainedMissing) {
    return sameContext ? rendered > 0 && retainedMissing == 0 : replacementKnown(false, sameZoom, previous, rendered);
  }
  private static final class Surface {
    private int knownSamples, renderedSamples, retainedMissing;
    private final boolean[] rendered = new boolean[81], retainedCells = new boolean[81];
    private double sampleSpan;
    private int framebuffer, texture, side, coverage;
    private boolean failed, pending;
    private MapCache source;
    private long generation, refreshed;
    private int style, dimension, layer;
    private String context = "";
    private double centerX, centerZ, scale, requestScale;

    private Surface() {}

    public void clear() {
      if (framebuffer != 0) EXTFramebufferObject.glDeleteFramebuffersEXT(framebuffer);
      if (texture != 0) GL11.glDeleteTextures(texture);
      framebuffer = texture = side = 0;
      source = null;
      failed = pending = false;
      refreshed = 0;
    }

    public boolean update(Minecraft mc, MapProjection view, MapSettings settings) {
      return update(mc, view, settings, true);
    }
    private boolean update(Minecraft mc, MapProjection view, MapSettings settings, boolean ownsInterest) {
      if (failed || !GLContext.getCapabilities().GL_EXT_framebuffer_object) return false;
      if (maximum == 0) maximum = Math.min(2048, GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE));
      double pixelScale = view.scale * MapRenderer.pixelsPerUnit();
      var layout = MapBufferLayout.create(view.width, MapRenderer.pixelsPerUnit(), maximum);
      if (layout == null) return false;
      int target = layout.side();
      int nextStyle =
          java.util.Objects.hash(
              MapRenderer.style(settings), settings.chunkGrid, settings.slimeChunks);
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
        if (!paint(mc, settings, target, layout.coverage(), pixelScale, view.x, view.z, ownsInterest)) return false;
        requestScale = view.scale;
        style = nextStyle;
        refreshed = now;
      }
      return true;
    }

    private void draw(MapProjection view, MapSettings settings) {
      double half = side / (2 * scale);
      draw(view, settings, centerX - half, centerZ - half, centerX + half, centerZ + half);
    }
    private void drawReplacement(MapProjection view, MapSettings settings, Surface retained) {
      double half = side / (2 * scale);
      double retainedSpan = retained.renderedSamples == 0 ? retained.side / retained.scale : retained.sampleSpan;
      double x0 = retained.centerX - retainedSpan / 2, x1 = x0 + retainedSpan;
      double z0 = retained.centerZ - retainedSpan / 2, z1 = z0 + retainedSpan;
      // Disjoint regions apply opacity once, including while the wider image is incomplete.
      draw(view, settings, centerX - half, centerZ - half, centerX + half, z0);
      draw(view, settings, centerX - half, z1, centerX + half, centerZ + half);
      draw(view, settings, centerX - half, z0, x0, z1);
      draw(view, settings, x1, z0, centerX + half, z1);
      if (retained.renderedSamples == 0) { retained.draw(view, settings); return; }
      double step = retainedSpan / 9;
      for (int i = 0; i < retainedCells.length; i++) {
        double x = x0 + i % 9 * step, z = z0 + i / 9 * step;
        (retainedCells[i] ? retained : this).draw(view, settings, x, z, x + step, z + step);
      }
    }
    private void draw(MapProjection view, MapSettings settings, double x0, double z0, double x1, double z1) {
      double half = side / (2 * scale);
      x0 = Math.max(x0, centerX - half); x1 = Math.min(x1, centerX + half);
      z0 = Math.max(z0, centerZ - half); z1 = Math.min(z1, centerZ + half);
      if (x0 >= x1 || z0 >= z1) return;
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
      GL11.glColor4d(1, 1, 1, settings.opacity);
      double u0 = (x0 - centerX + half) / (2 * half), u1 = (x1 - centerX + half) / (2 * half);
      double v0 = 1 - (z0 - centerZ + half) / (2 * half), v1 = 1 - (z1 - centerZ + half) / (2 * half);
      var a = view.screen(x0, z0);
      var b = view.screen(x1, z0);
      var c = view.screen(x1, z1);
      var d = view.screen(x0, z1);
      MapRenderer.polygon(
          view.clip(
              List.of(
                  new MapProjection.Point(a.x(), a.y(), u0, v0),
                  new MapProjection.Point(b.x(), b.y(), u1, v0),
                  new MapProjection.Point(c.x(), c.y(), u1, v1),
                  new MapProjection.Point(d.x(), d.y(), u0, v1)),
              settings.circular));

    }

    private boolean paint(
        Minecraft mc,
        MapSettings settings,
        int target,
        int painted,
        double pixelScale,
        double x,
        double z, boolean ownsInterest) {
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
        int probeLevel = MapCoverage.select(new MapProjection(centerX, centerZ, pixelScale, 0, 0, painted, painted, 0),
            false, settings.cacheTiles, 1).level();
        sampleSpan = painted / pixelScale;
        prepareSamples(mc, probeLevel);
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
            1,
            ownsInterest && !(mc.currentScreen instanceof local.luke.power.ui.WorldMapScreen));
        generation = source.generation();
        pending = MapRenderer.pending() || renderedSamples == 0 || renderedSamples < knownSamples || retainedMissing > 0;
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
    private boolean known(int level, double x, double z) {
      int wx = (int)Math.floor(x), wz = (int)Math.floor(z);
      var tile = source.peek(source.viewKey(MapMath.key(dimension, layer, level, wx, wz)));
      return tile != null && tile.known(MapMath.index(Math.floorDiv(wx, 1 << level), Math.floorDiv(wz, 1 << level)));
    }
    private boolean uploaded(Minecraft mc, int level, double x, double z) {
      // The peek guard admits resident tiles only, so tile() cannot schedule a visible read
      // or replace the world map's interest when this minimap is a secondary preview.
      return known(level, x, z) && MapRenderer.tile(source,
          MapMath.key(dimension, layer, level, (int)Math.floor(x), (int)Math.floor(z)),
          opaqueSettings, !mc.world.isRemote, mc.world.getSeed()) != 0;
    }
    private void prepareSamples(Minecraft mc, int level) {
      knownSamples = renderedSamples = retainedMissing = 0;
      java.util.Arrays.fill(rendered, false);
      java.util.Arrays.fill(retainedCells, false);
      // Center-first probes share the renderer's upload budget. Disk reads remain asynchronous.
      for (int radius = 0; radius <= 4; radius++) for (int sz = -radius; sz <= radius; sz++) for (int sx = -radius; sx <= radius; sx++) {
        if (Math.max(Math.abs(sx), Math.abs(sz)) != radius) continue;
        double x = centerX + sx * sampleSpan / 9, z = centerZ + sz * sampleSpan / 9;
        if (known(level, x, z)) knownSamples++;
        if (uploaded(mc, level, x, z)) { rendered[(sz + 4) * 9 + sx + 4] = true; renderedSamples++; }
      }
      if (this == active || !contextMatches(active)) return;
      double step = active.sampleSpan / 9;
      for (int i = 0; i < rendered.length; i++) if (active.rendered[i]) {
        double x = active.centerX + (i % 9 - 4) * step, z = active.centerZ + (i / 9 - 4) * step;
        boolean inside = Math.abs(x - centerX) <= sampleSpan / 2 && Math.abs(z - centerZ) <= sampleSpan / 2;
        retainedCells[i] = !inside || !uploaded(mc, level, x, z);
        if (inside && retainedCells[i]) retainedMissing++;
      }
    }
  }
}
