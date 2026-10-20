package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import local.luke.power.map.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Native pixel proof that a coarse fallback never becomes unexplored fine terrain. */
public final class Release300MapRefinement {
  private static final int GUI = 128, PIXELS = GUI * 2;
  private static final int BACKGROUND = 0x182435, COARSE = 0xd04a35, FINE = 0x35c18a;
  private Release300MapRefinement() {}
  private static void check(boolean condition, String message) { Validation.check(condition, message); }

  public static void check(Minecraft mc) throws Exception {
    test("coarse-to-fine refinement after a pending pan restores unexplored pixels to background", () -> render(mc));
  }

  @SuppressWarnings("unchecked")
  private static void render(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote, "open a disposable singleplayer world first");
    check(GLContext.getCapabilities().GL_EXT_framebuffer_object, "native framebuffer support is required");
    var originalCache = Maps.cache();
    var originalScreen = mc.currentScreen;
    int oldWidth = mc.displayWidth, oldHeight = mc.displayHeight, nativeScale = mc.options.guiScale;
    var options = Class.forName("local.luke.power.controls.util.ModOptions");
    Field guiScale = options.getField("guiScale"), realGuiScale = options.getField("realGuiScale");
    float oldGui = guiScale.getFloat(null), oldRealGui = realGuiScale.getFloat(null);
    int oldFramebuffer = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
    int oldMatrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    int oldActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    boolean blit = GLContext.getCapabilities().GL_EXT_framebuffer_blit;
    int oldRead = blit ? GL11.glGetInteger(EXTFramebufferBlit.GL_READ_FRAMEBUFFER_BINDING_EXT) : oldFramebuffer;
    int oldDraw = blit ? GL11.glGetInteger(EXTFramebufferBlit.GL_DRAW_FRAMEBUFFER_BINDING_EXT) : oldFramebuffer;
    Map<MapMath.Key, Object> liveTextures = (Map<MapMath.Key, Object>)field(MapRenderer.class, "TEXTURES").get(null);
    var savedTextures = new LinkedHashMap<>(liveTextures);
    var renderer = new LinkedHashMap<String, Object>();
    for (String name : List.of("cache", "remaining", "delta", "pixelsPerUnit", "pending")) renderer.put(name, field(MapRenderer.class, name).get(null));
    // Detach live texture ownership before MapRenderer changes source. These textures remain valid.
    liveTextures.clear(); field(MapRenderer.class, "cache").set(null, null);
    var buffer = new WorldMapBuffer();
    Path unusedRoot = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("power-beta-validation-memory-only-refinement");
    var cache = new MapCache(unusedRoot, 256, message -> { throw new AssertionError(message); });
    Map<MapMath.Key, MapTile> resident = (Map<MapMath.Key, MapTile>)field(MapCache.class, "tiles").get(cache);
    Map<MapMath.Key, CompletableFuture<MapTile>> loading = (Map<MapMath.Key, CompletableFuture<MapTile>>)field(MapCache.class, "loading").get(cache);
    var fineLoads = new LinkedHashMap<MapMath.Key, CompletableFuture<MapTile>>();
    Canvas canvas = null;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    long submissions = MapIO.submitted();
    try {
      mc.displayWidth = mc.displayHeight = PIXELS; mc.options.guiScale = 2;
      guiScale.setFloat(null, 2 / 8f); realGuiScale.setFloat(null, 2 / 8f);
      field(Maps.class, "cache").set(null, cache);
      int dimension = mc.world.dimension.id;
      for (int z = -4; z <= 4; z++) for (int x = -4; x <= 4; x++) {
        var key = new MapMath.Key(dimension, -1, 6, x, z);
        var tile = new MapTile(key);
        for (int i = 0; i < MapMath.PIXELS; i++) tile.put(i, COARSE, COARSE, 64, 15, 1, 0);
        resident.put(key, persisted(tile));
      }
      for (int z = -2; z <= 3; z++) for (int x = -2; x <= 4; x++) {
        var key = new MapMath.Key(dimension, -1, 0, x, z);
        var future = new CompletableFuture<MapTile>(); fineLoads.put(key, future); loading.put(key, future);
      }
      // Keep the native cache's outstanding-read bound occupied even after visible leaves publish.
      // Unexpected coordinates cannot start a real worker read from the synthetic path.
      for (int i = 0; i < 16; i++) loading.put(new MapMath.Key(dimension, -1, 0, 1000 + i, 1000), new CompletableFuture<>());
      var settings = MapConfig.copy();
      settings.automaticDetail = true; settings.mapSmoothing = false; settings.cacheTiles = 256;
      settings.heightShading = settings.slopeShading = settings.caveLighting = false;
      settings.chunkGrid = settings.slimeChunks = settings.lightOverlay = false;
      settings.opacity = 1; settings.mapOpenBudget = 8;
      canvas = new Canvas();
      boolean nativeBlit = GLContext.getCapabilities().GL_EXT_framebuffer_blit;
      for (boolean useBlit : new boolean[]{nativeBlit, false}) {
        Field capability = field(ContextCapabilities.class, "GL_EXT_framebuffer_blit");
        try {
          capability.setBoolean(GLContext.getCapabilities(), useBlit);
          pendingPans(mc, canvas, buffer, resident, loading, dimension, settings, 0);
          pendingPans(mc, canvas, buffer, resident, loading, dimension, settings, 48);
        } finally { capability.setBoolean(GLContext.getCapabilities(), nativeBlit); }
      }
      if (nativeBlit) {
        var odd = new Canvas(GUI, GUI, PIXELS + 1, PIXELS + 3);
        try { pendingPans(mc, odd, buffer, resident, loading, dimension, settings, 48); }
        finally { odd.close(); }
      }
      mc.displayWidth = mc.displayHeight = PIXELS;
      fineLoads.forEach(loading::put);
      var coarseView = new MapProjection(32, 32, 1d / 128, 0, 0, GUI, GUI, 0);
      int coarseFrames = settle(mc, canvas, buffer, coarseView, dimension, settings, 6);
      int coarsePixel = canvas.rgb(64, 64);
      color(coarsePixel, COARSE, "coarse fixture did not visibly render its known aggregate");

      var fineView = new MapProjection(32, 32, 1, 0, 0, GUI, GUI, 0);
      frame(mc, canvas, buffer, fineView, dimension, settings);
      check(field(WorldMapBuffer.class, "pending").get(buffer) != null, "fine data was not pending before the pan");
      var panned = new MapProjection(112, 32, 1, 0, 0, GUI, GUI, 0);
      frame(mc, canvas, buffer, panned, dimension, settings);
      frame(mc, canvas, buffer, panned, dimension, settings);
      check(field(WorldMapBuffer.class, "pending").get(buffer) != null, "fixture did not pan while fine terrain was pending");

      for (var entry : fineLoads.entrySet()) {
        var tile = new MapTile(entry.getKey());
        // This one surveyed patch is the positive control; the rest of each leaf is unexplored.
        if (entry.getKey().x() == 1 && entry.getKey().z() == 0)
          for (int z = 24; z <= 40; z++) for (int x = 72; x <= 88; x++)
            tile.put(MapMath.index(x, z), FINE, FINE, 64, 15, 1, 0);
        entry.getValue().complete(persisted(tile));
      }
      int fineFrames = settle(mc, canvas, buffer, panned, dimension, settings, 0);
      int knownPixel = canvas.rgb(32, 64);
      color(knownPixel, 0x38d5a1, "surveyed fine terrain did not replace its coarse aggregate");
      int[] unknownPixels = {canvas.rgb(96, 48), canvas.rgb(96, 64), canvas.rgb(112, 80)};
      for (int pixel : unknownPixels) color(pixel, BACKGROUND, "unexplored fine terrain retained coarse/frame-copy pixels after settling");
      Object active = field(WorldMapBuffer.class, "active").get(buffer);
      var change = active.getClass().getDeclaredMethod("changed", MapMath.Key.class);
      change.setAccessible(true);
      int missing = 0;
      var visibleKey = new MapMath.Key(dimension, -1, 0, 1, 0);
      for (var key : fineLoads.keySet()) {
        if (key.equals(visibleKey) || missing >= 12) continue;
        resident.remove(key); loading.put(key, new CompletableFuture<>());
        change.invoke(active, key); missing++;
      }
      var replacement = new MapTile(visibleKey);
      for (int z = 24; z <= 40; z++) for (int x = 72; x <= 88; x++)
        replacement.put(MapMath.index(x, z), 0x4679e1, 0x4679e1, 64, 15, 1, 0);
      resident.put(visibleKey, persisted(replacement)); change.invoke(active, visibleKey);
      int changedFrames = 0;
      for (; changedFrames < 8; changedFrames++) {
        frame(mc, canvas, buffer, panned, dimension, settings, 1);
        if (canvas.rgb(32, 64) == 0x4b85ff) break;
      }
      color(canvas.rgb(32, 64), 0x4b85ff, "stationary visible update stalled behind missing tiles with one upload per frame");
      log("300 STATIONARY visible-update frames=" + (changedFrames + 1) + " missing-prefix=" + missing + " uploads-per-frame=1");
      check(MapIO.submitted() == submissions, "synthetic refinement submitted real map storage work");
      check(GL11.glGetError() == GL11.GL_NO_ERROR, "native refinement pixel check produced an OpenGL error");
      log("300 REFINEMENT coarseLevel=6 fineLevel=0 panWhilePending=true coarseFrames=" + coarseFrames
          + " fineFrames=" + fineFrames + " coarse=" + hex(coarsePixel) + " known=" + hex(knownPixel)
          + " unknown=" + Arrays.toString(Arrays.stream(unknownPixels).mapToObj(Release300MapRefinement::hex).toArray())
          + " background=" + hex(BACKGROUND));
    } finally {
      // No native sampling or cache tick occurred. All outstanding futures belong to this fixture.
      loading.clear(); cache.retire();
      try { check(cache.releaseIfIdle(), "synthetic cache did not release its read-only leaves"); }
      finally {
        buffer.clear(); MapRenderer.clear();
        liveTextures.putAll(savedTextures);
        for (var entry : renderer.entrySet()) field(MapRenderer.class, entry.getKey()).set(null, entry.getValue());
        field(Maps.class, "cache").set(null, originalCache);
        mc.currentScreen = originalScreen; mc.displayWidth = oldWidth; mc.displayHeight = oldHeight;
        mc.options.guiScale = nativeScale; guiScale.setFloat(null, oldGui); realGuiScale.setFloat(null, oldRealGui);
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, oldFramebuffer);
        if (canvas != null) canvas.close();
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
        GL11.glPopClientAttrib(); GL11.glPopAttrib();
        GL13.glActiveTexture(oldActiveTexture); GL11.glMatrixMode(oldMatrix);
        if (blit) {
          EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferBlit.GL_READ_FRAMEBUFFER_EXT, oldRead);
          EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferBlit.GL_DRAW_FRAMEBUFFER_EXT, oldDraw);
        } else EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, oldFramebuffer);
      }
    }
  }

  private static void pendingPans(Minecraft mc, Canvas canvas, WorldMapBuffer buffer,
      Map<MapMath.Key, MapTile> resident,
      Map<MapMath.Key, CompletableFuture<MapTile>> loading, int dimension,
      MapSettings settings, int left) throws Exception {
    var tile = new MapTile(new MapMath.Key(dimension, -1, 0, 0, 0));
    for (int i = 0; i < MapMath.PIXELS; i++) tile.put(i, FINE, FINE, 64, 15, 1, 0);
    resident.put(tile.key, persisted(tile));
    var pane = new MapProjection(32, 32, 1, left, 0, GUI - left, GUI, 0);
    frame(mc, canvas, buffer, pane, dimension, settings);
    var point = pane.screen(48, 32);
    color(canvas.rgb((int)point.x(), (int)point.y()), 0x38d5a1, "pan landmark did not render");
    resident.remove(tile.key); loading.put(tile.key, new CompletableFuture<>());
    for (double[] center : new double[][]{{72,32},{32,72},{72,32},{32,32}}) {
      pane = new MapProjection(center[0], center[1], 1, left, 0, GUI - left, GUI, 0);
      frame(mc, canvas, buffer, pane, dimension, settings, 0);
      point = pane.screen(48, 32);
      color(canvas.rgb((int)point.x(), (int)point.y()), 0x38d5a1,
          "same-level pan compressed outside pixels into retained map terrain");
    }
    buffer.clear(); MapRenderer.clear();
    log("PASS retained pending pans left=" + left + " pixels=" + canvas.pixelWidth + "x"
        + canvas.pixelHeight + " blit=" + GLContext.getCapabilities().GL_EXT_framebuffer_blit);
  }

  private static Field field(Class<?> type, String name) throws Exception {
    Field result = type.getDeclaredField(name); result.setAccessible(true); return result;
  }
  private static MapTile persisted(MapTile tile) throws Exception {
    MapTile decoded = MapCodec.decode(MapCodec.encode(tile), tile.key); decoded.readOnly = true; return decoded;
  }
  private static int settle(Minecraft mc, Canvas canvas, WorldMapBuffer buffer, MapProjection view,
      int dimension, MapSettings settings, int level) throws Exception {
    for (int frame = 1; frame <= 128; frame++) {
      frame(mc, canvas, buffer, view, dimension, settings);
      Object active = field(WorldMapBuffer.class, "active").get(buffer);
      if (active != null && field(WorldMapBuffer.class, "pending").get(buffer) == null
          && ((MapSurfacePlan)field(active.getClass(), "plan").get(active)).level() == level) return frame;
    }
    throw new AssertionError("synthetic map did not settle at level " + level + " within 128 render calls");
  }
  private static void frame(Minecraft mc, Canvas canvas, WorldMapBuffer buffer, MapProjection view,
      int dimension, MapSettings settings) throws Exception {
    frame(mc, canvas, buffer, view, dimension, settings, 16);
  }
  private static void frame(Minecraft mc, Canvas canvas, WorldMapBuffer buffer, MapProjection view,
      int dimension, MapSettings settings, int uploads) throws Exception {
    mc.displayWidth = canvas.pixelWidth; mc.displayHeight = canvas.pixelHeight;
    canvas.bind(); MapRenderer.beginFrame(1);
    // This 256px offscreen target intentionally sits below Beta's minimum GUI window.
    // Its known 2x projection owns the physical scale, independently of ScreenScaler's clamp.
    field(MapRenderer.class, "pixelsPerUnit").setDouble(null, (double)canvas.pixelWidth / canvas.guiWidth);
    MapRenderer.uploadBudget(uploads);
    check(buffer.draw(mc, view, dimension, -1, settings, canvas.guiWidth, canvas.guiHeight), "native WorldMapBuffer fell back instead of rendering");
  }
  private static String hex(int value) { return String.format(Locale.ROOT, "#%06X", value & 0xffffff); }
  private static void color(int actual, int expected, String message) {
    boolean equal = true;
    for (int shift : new int[]{0, 8, 16}) equal &= Math.abs((actual >> shift & 255) - (expected >> shift & 255)) <= 2;
    check(equal, message + "; expected=" + hex(expected) + " actual=" + hex(actual));
  }

  static final class Canvas {
    final int texture = GL11.glGenTextures(), framebuffer = EXTFramebufferObject.glGenFramebuffersEXT();
    final ByteBuffer pixel = BufferUtils.createByteBuffer(4);
    final int guiWidth, guiHeight, pixelWidth, pixelHeight;
    Canvas() { this(GUI, GUI, PIXELS, PIXELS); }
    Canvas(int guiWidth, int guiHeight, int pixelWidth, int pixelHeight) {
      this.guiWidth = guiWidth; this.guiHeight = guiHeight;
      this.pixelWidth = pixelWidth; this.pixelHeight = pixelHeight;
      GL13.glActiveTexture(GL13.GL_TEXTURE0);
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
      GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, pixelWidth, pixelHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
      EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebuffer);
      EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
          EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, GL11.GL_TEXTURE_2D, texture, 0);
      if (EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT)
          != EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT) {
        close(); throw new IllegalStateException("synthetic pixel canvas is unavailable");
      }
    }
    void bind() {
      EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebuffer);
      GL11.glViewport(0, 0, pixelWidth, pixelHeight);
      GL11.glDisable(GL11.GL_SCISSOR_TEST); GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glColorMask(true, true, true, true);
      GL11.glClearColor((BACKGROUND >> 16 & 255) / 255f, (BACKGROUND >> 8 & 255) / 255f, (BACKGROUND & 255) / 255f, 1);
      GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity(); GL11.glOrtho(0, guiWidth, guiHeight, 0, -1, 1);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity();
    }
    int rgb(int x, int y) {
      EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebuffer);
      GL11.glReadBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
      GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1); GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
      GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0); GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
      pixel.clear(); GL11.glReadPixels((int)((x + .5) * pixelWidth / guiWidth), pixelHeight - (int)((y + .5) * pixelHeight / guiHeight) - 1, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
      return (pixel.get(0) & 255) << 16 | (pixel.get(1) & 255) << 8 | pixel.get(2) & 255;
    }
    void close() { GL11.glDeleteTextures(texture); EXTFramebufferObject.glDeleteFramebuffersEXT(framebuffer); }
  }
}
