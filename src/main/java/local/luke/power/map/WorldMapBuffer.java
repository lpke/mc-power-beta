package local.luke.power.map;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.*;

/** Two bounded physical-resolution surfaces. Completed terrain survives CPU-cache eviction. */
public final class WorldMapBuffer {
  private Surface active, pending, spare;
  private MapCache source;
  private int dimension, layer, style;
  private String context = "";
  private boolean failed;
  private int maximum;

  private static final class Surface {
    final int framebuffer, texture;
    MapSurfacePlan plan;
    MapProjection view;
    MapCoverage.Bounds bounds;
    int centerX, centerZ;
    long sweepCount;
    final ArrayDeque<MapMath.Key> requests = new ArrayDeque<>();
    final ArrayDeque<MapMath.Key> resident = new ArrayDeque<>();
    final MapRefreshQueue changed = new MapRefreshQueue();
    final Consumer<MapMath.Key> listener = this::changed;
    final MapCache source;
    final int dimension, layer;
    final String context;
    long cursor, completed;
    double visibleArea;

    Surface(MapCache source, MapSurfacePlan plan, MapProjection requested, int dimension, int layer) {
      this.source = source;
      this.plan = plan;
      this.dimension = dimension;
      this.layer = layer;
      context = layer < 0 ? "" : source.viewContext();
      texture = GL11.glGenTextures();
      framebuffer = EXTFramebufferObject.glGenFramebuffersEXT();
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
      GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, plan.width(), plan.height(),
          0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
      EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebuffer);
      EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
          EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, GL11.GL_TEXTURE_2D, texture, 0);
      if (EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT)
          != EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT) {
        GL11.glDeleteTextures(texture);
        EXTFramebufferObject.glDeleteFramebuffersEXT(framebuffer);
        throw new IllegalStateException("World map framebuffer is unavailable");
      }
      reset(plan, requested);
      source.listen(listener);
    }

    void reset(MapSurfacePlan plan, MapProjection requested) {
      this.plan = plan;
      view = new MapProjection(requested.x, requested.z, plan.scale(), 0, 0,
          plan.width(), plan.height(), 0);
      bounds = MapCoverage.bounds(view, false, plan.level());
      int span = MapMath.SIZE << plan.level();
      centerX = Math.max(bounds.minX(), Math.min(bounds.maxX(), (int) Math.floor(view.x / span)));
      centerZ = Math.max(bounds.minZ(), Math.min(bounds.maxZ(), (int) Math.floor(view.z / span)));
      long radius = Math.max(Math.max(centerX - bounds.minX(), bounds.maxX() - centerX),
          Math.max(centerZ - bounds.minZ(), bounds.maxZ() - centerZ));
      sweepCount = (radius * 2 + 1) * (radius * 2 + 1);
      cursor = completed = 0;
      visibleArea = 0;
      requests.clear(); changed.clear();
      resident.clear();
      source.resident().stream()
          .map(tile -> tile.key)
          .filter(key -> key.dimension() == dimension && key.layer() == layer
              && key.context().equals(context) && key.level() == plan.level()
              && Math.abs(key.minX() + key.span() / 2d - view.x) <= view.width / (2 * view.scale) + key.span() / 2d
              && Math.abs(key.minZ() + key.span() / 2d - view.z) <= view.height / (2 * view.scale) + key.span() / 2d)
          .sorted(Comparator.comparingDouble(key -> Math.hypot(key.minX() - view.x, key.minZ() - view.z)))
          .limit(64).forEach(resident::add);
      bind();
      GL11.glClearColor(0, 0, 0, 0);
      GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    void bind() {
      EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebuffer);
      GL11.glViewport(0, 0, plan.width(), plan.height());
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glLoadIdentity();
      GL11.glOrtho(0, plan.width(), plan.height(), 0, -1, 1);
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glLoadIdentity();
    }

    void changed(MapMath.Key key) {
      if (key.dimension() != dimension || key.layer() != layer || !key.context().equals(context) || key.level() != plan.level()
          || key.x() < bounds.minX() || key.x() > bounds.maxX()
          || key.z() < bounds.minZ() || key.z() > bounds.maxZ()) return;
      if (changed.size() < 2048) changed.add(key);
      else { cursor = completed = 0; visibleArea = 0; changed.clear(); }
    }

    void paint(Minecraft mc, MapSettings settings, MapProjection requested, long deadline) {
      if (resident.isEmpty() && requests.isEmpty() && cursor >= sweepCount && changed.isEmpty()) return;
      bind();
      changed.requestMissing(k -> source.peek(k) != null, source::visible);
      boolean updatesFirst = changed.updatesFirst(!resident.isEmpty() || !requests.isEmpty() || cursor < sweepCount);
      if (updatesFirst) paintChanges(mc, settings, requested, deadline, 2);
      // Show nearby terrain at the requested detail before issuing more disk reads.
      for (int i = 0; i < 16 && !resident.isEmpty() && System.nanoTime() < deadline; i++) {
        if (source.peek(resident.peekFirst()) == null) { resident.removeFirst(); continue; }
        if (!paintTile(resident.peekFirst(), mc, settings)) return;
        resident.removeFirst();
      }
      if (!resident.isEmpty()) return;
      // Enumeration and outstanding requests stay bounded even at manual level zero and tiny zoom.
      for (int i = 0; i < 32 && requests.size() < 16 && cursor < sweepCount; i++, cursor++) {
        var cell = MapTileSweep.cell(cursor);
        int x = centerX + cell.x(), z = centerZ + cell.z();
        if (x < bounds.minX() || x > bounds.maxX() || z < bounds.minZ() || z > bounds.maxZ()) continue;
        requests.add(new MapMath.Key(dimension, layer, plan.level(),
            x, z, context));
      }
      int attempts = requests.size();
      for (int i = 0; i < attempts && System.nanoTime() < deadline; i++) {
        var key = requests.removeFirst();
        if (paintTile(key, mc, settings)) {
          completed++;
          var a = view.screen(key.minX(), key.minZ());
          double side = key.span() * view.scale;
          double x0 = Math.max(64, a.x()), z0 = Math.max(64, a.y());
          double x1 = Math.min(plan.width() - 64, a.x() + side);
          double z1 = Math.min(plan.height() - 64, a.y() + side);
          visibleArea += Math.max(0, x1 - x0) * Math.max(0, z1 - z0);
        } else requests.addLast(key);
      }
      paintChanges(mc, settings, requested, deadline, updatesFirst ? 6 : 8);
    }

    void paintChanges(Minecraft mc, MapSettings settings, MapProjection requested, long deadline, int maximum) {
      for (int i = 0; i < maximum && !changed.isEmpty() && System.nanoTime() < deadline; i++) {
        var key = changed.take(k -> source.peek(k) != null, requested);
        if (key == null) break;
        if (!paintTile(key, mc, settings)) { changed.add(key); break; }
      }
    }

    boolean paintTile(MapMath.Key key, Minecraft mc, MapSettings settings) {
      int tile = MapRenderer.tile(source, key, settings, !mc.world.isRemote, mc.world.getSeed());
      if (tile == 0) return false;
      var p = view.screen(key.minX(), key.minZ());
      double side = key.span() * view.scale;
      // Transparent unobserved pixels preserve the compatible image used to seed this surface.
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, tile);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
      GL11.glDisable(GL11.GL_BLEND);
      GL11.glEnable(GL11.GL_ALPHA_TEST);
      GL11.glAlphaFunc(GL11.GL_GREATER, 0);
      MapRenderer.polygon(List.of(new MapProjection.Point(p.x(), p.y(), 0, 0),
          new MapProjection.Point(p.x() + side, p.y(), 1, 0),
          new MapProjection.Point(p.x() + side, p.y() + side, 1, 1),
          new MapProjection.Point(p.x(), p.y() + side, 0, 1)));
      return true;
    }

    boolean contains(MapProjection requested) {
      double dx = requested.width / (2 * requested.scale), dz = requested.height / (2 * requested.scale);
      return Math.abs(view.x - requested.x) + dx <= view.width / (2 * view.scale)
          && Math.abs(view.z - requested.z) + dz <= view.height / (2 * view.scale);
    }

    boolean ready() {
      return completed == bounds.count()
          || visibleArea >= .85 * (plan.width() - 128) * (plan.height() - 128);
    }

    void seeded() { resident.removeIf(key -> key.level() != plan.level()); }

    boolean useful(MapProjection requested) {
      return Math.abs(view.x - requested.x) * requested.scale < requested.width / 2
          && Math.abs(view.z - requested.z) * requested.scale < requested.height / 2;
    }

    void draw(MapProjection target, boolean smoothing) {
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
      int filter = smoothing ? GL11.GL_LINEAR : GL11.GL_NEAREST;
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
      double halfX = view.width / (2 * view.scale), halfZ = view.height / (2 * view.scale);
      var a = target.screen(view.x - halfX, view.z - halfZ);
      var b = target.screen(view.x + halfX, view.z + halfZ);
      MapRenderer.polygon(List.of(new MapProjection.Point(a.x(), a.y(), 0, 1),
          new MapProjection.Point(b.x(), a.y(), 1, 1),
          new MapProjection.Point(b.x(), b.y(), 1, 0),
          new MapProjection.Point(a.x(), b.y(), 0, 0)));
    }

    void close() {
      source.unlisten(listener);
      GL11.glDeleteTextures(texture);
      EXTFramebufferObject.glDeleteFramebuffersEXT(framebuffer);
    }
  }

  public void clear() {
    if (active != null) active.close();
    if (pending != null) pending.close();
    if (spare != null) spare.close();
    active = pending = spare = null;
    source = null;
  }

  public boolean draw(Minecraft mc, MapProjection view, int dimension, int layer, MapSettings settings,
      int guiWidth, int guiHeight) {
    if (failed || !GLContext.getCapabilities().GL_EXT_framebuffer_object) return false;
    if (maximum == 0) maximum = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
    var plan = MapSurfacePlan.create(view, MapRenderer.pixelsPerUnit(), maximum, settings.automaticDetail);
    if (plan == null) return false;
    int style = Objects.hash(MapRenderer.style(settings), settings.chunkGrid, settings.slimeChunks,
        mc.world.getSeed());
    String context = layer < 0 ? "" : Maps.cache().viewContext();
    boolean restyle = this.style != style;
    if (source != Maps.cache() || this.dimension != dimension || this.layer != layer
        || !this.context.equals(context)
        || wrongSize(active, plan) || wrongSize(pending, plan) || wrongSize(spare, plan)) {
      clear();
      source = Maps.cache();
      this.dimension = dimension;
      this.layer = layer;
      this.style = style;
      this.context = context;
    }
    this.style = style;
    long deadline = System.nanoTime() + (long) (settings.mapOpenBudget * 1_000_000);
    // Disk workers can publish between ticks; native sampling, parent merges and saves stay tick-owned.

    int oldBuffer = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
    int oldMatrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    boolean preserve = pending != null && (restyle || pending.plan.level() != plan.level()
        || !pending.useful(view) || !pending.contains(view));
    // A coarse aggregate may cover unexplored fine pixels. Keep it as a temporary fallback,
    // never bake it into a finer surface where transparent unknown pixels cannot replace it.
    boolean copyCompatible = (active == null || active.plan.level() <= plan.level())
        && (pending == null || pending.plan.level() <= plan.level());
    // The window is a temporary composite target, before markers/chrome. No third full-size texture.
    if (preserve) display(view, false);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      GL11.glDisable(GL11.GL_SCISSOR_TEST);
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glDisable(GL11.GL_ALPHA_TEST);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glColorMask(true, true, true, true);
      GL11.glColor4f(1, 1, 1, 1);
      boolean need = restyle || active == null || active.plan.level() != plan.level() || !active.contains(view)
          || plan.scale() / active.plan.scale() > 1.5 || plan.scale() / active.plan.scale() < .75;
      if (pending != null && (restyle || pending.plan.level() != plan.level() || !pending.useful(view) || !pending.contains(view))) {
        pending.reset(plan, view);
        if (active != null && active.plan.level() <= plan.level()) {
          GL11.glDisable(GL11.GL_BLEND);
          active.draw(pending.view, false);
          pending.seeded();
        }
        if (preserve && copyCompatible) copyDisplayed(mc, pending, oldBuffer, view, guiWidth, guiHeight);
      }
      if (need && pending == null) {
        if (spare != null) { pending = spare; spare = null; pending.reset(plan, view); }
        else pending = new Surface(source, plan, view, dimension, layer);
        if (active != null && active.plan.level() <= plan.level()) {
          GL11.glDisable(GL11.GL_BLEND);
          active.draw(pending.view, false);
          pending.seeded();
        }
      }
      var filling = pending != null ? pending : active;
      if (filling != null) source.visibleView(dimension, layer, filling.bounds);
      source.publish(4, deadline);
      if (pending != null) pending.paint(mc, settings, view, deadline);
      else if (active != null) active.paint(mc, settings, view, deadline);
      if (pending != null && pending.ready() && pending.contains(view)) {
        spare = active;
        active = pending; pending = null;
      } else if (pending != null && pending.ready() && !pending.contains(view)) {
        // Preserve every visible progressive patch, including portions outside the older image.
        pending.reset(plan, view);
        GL11.glDisable(GL11.GL_BLEND);
        if (active != null && active.plan.level() <= plan.level()) active.draw(pending.view, false);
        if (copyCompatible) copyDisplayed(mc, pending, oldBuffer, view, guiWidth, guiHeight);
      }
    } catch (RuntimeException failure) {
      clear(); failed = true;
      Maps.error("World map buffer unavailable: " + MapCache.reason(failure));
      return false;
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glMatrixMode(oldMatrix);
      EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, oldBuffer);
      GL11.glPopAttrib();
    }
    display(view, settings.mapSmoothing);
    return true;
  }

  private static boolean wrongSize(Surface surface, MapSurfacePlan plan) {
    return surface != null && (surface.plan.width() != plan.width() || surface.plan.height() != plan.height());
  }

  private static void copyDisplayed(Minecraft mc, Surface target, int originalFramebuffer,
      MapProjection view, int guiWidth, int guiHeight) {
    var pixels = MapPixelBounds.of(view, guiWidth, guiHeight, mc.displayWidth, mc.displayHeight);
    if (pixels.width() == 0 || pixels.height() == 0) return;
    target.seeded();
    if (GLContext.getCapabilities().GL_EXT_framebuffer_blit) {
      EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferBlit.GL_READ_FRAMEBUFFER_EXT, originalFramebuffer);
      GL11.glReadBuffer(originalFramebuffer == 0 ? GL11.GL_BACK : EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
      EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferBlit.GL_DRAW_FRAMEBUFFER_EXT, target.framebuffer);
      // Independent axes correct GUI rounding at non-divisible window sizes without CPU readback.
      EXTFramebufferBlit.glBlitFramebufferEXT(pixels.x(), pixels.y(),
          pixels.x() + pixels.width(), pixels.y() + pixels.height(),
          64, 64, target.plan.width() - 64, target.plan.height() - 64,
          GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
    } else if (pixels.width() == target.plan.width() - 128 && pixels.height() == target.plan.height() - 128) {
      EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, originalFramebuffer);
      GL11.glReadBuffer(originalFramebuffer == 0 ? GL11.GL_BACK : EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, target.texture);
      GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 64, 64,
          pixels.x(), pixels.y(), pixels.width(), pixels.height());
    } else return;
    target.bind();
    GL11.glEnable(GL11.GL_SCISSOR_TEST);
    GL11.glScissor(64, 64, target.plan.width() - 128, target.plan.height() - 128);
    GL11.glColorMask(false, false, false, true);
    GL11.glClearColor(0, 0, 0, 1);
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    GL11.glColorMask(true, true, true, true);
    GL11.glDisable(GL11.GL_SCISSOR_TEST);
  }

  private void display(MapProjection view, boolean smoothing) {
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glDisable(GL11.GL_ALPHA_TEST);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glColor4f(1, 1, 1, 1);
      if (active != null) active.draw(view, smoothing);
      if (pending != null) pending.draw(view, smoothing);
    } finally { GL11.glPopAttrib(); }
  }
}
