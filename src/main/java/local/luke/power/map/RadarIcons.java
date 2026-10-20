package local.luke.power.map;

import java.nio.ByteBuffer;
import java.util.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Bounded render-thread head cache. No terrain, entities or shared renderer models are consulted. */
public final class RadarIcons {
  private static final int SIDE = 64, LIMIT = 128;
  private record Key(RadarIconSource source, int texture) {}
  private record Resolution(Key key, long refreshAt) {}
  private static final Map<Key, Integer> TEXTURES = new LinkedHashMap<>(16, .75f, true);
  private static final Map<RadarIconSource, Resolution> RESOLVED = new LinkedHashMap<>(16, .75f, true);
  private static final Map<Integer, Integer> PINNED = new HashMap<>();
  private static final Set<Integer> RETIRED = new HashSet<>();
  private static final Map<String, Double> RADII = new HashMap<>();
  private static final ByteBuffer PIXELS = BufferUtils.createByteBuffer(SIDE * SIDE * 4);
  private static final ByteBuffer OUTLINED = BufferUtils.createByteBuffer(SIDE * SIDE * 4);
  private static int framebuffer, surface, depth, remaining = 1, resolutions = 1;
  private static boolean unavailable;
  private static volatile boolean invalidated;
  private RadarIcons() {}

  /** Called at the native outer frame boundary, shared by both stereo eyes and every map view. */
  public static void beginFrame() {
    if (invalidated) { clear(); invalidated = false; }
    remaining = resolutions = 1;
  }

  public static void invalidate() { invalidated = true; }

  public static double radius(RadarIconSource source) {
    if (source == null) return 0;
    String key = source.type() + (source.wool() ? ".wool" : "");
    Double cached = RADII.get(key);
    if (cached != null) return cached;
    var model = RadarHeadModel.create(source);
    if (model == null) return 0;
    double extent = 0;
    for (var parts : List.of(model.base(), model.overlay())) for (var box : parts) {
      extent = Math.max(extent, Math.max(Math.abs(box.x() - box.dilation()),
          Math.abs(box.x() + box.width() + box.dilation())));
      extent = Math.max(extent, Math.max(Math.abs(box.y() - box.dilation() - model.centerY()),
          Math.abs(box.y() + box.height() + box.dilation() - model.centerY())));
    }
    // One native model unit becomes one GUI pixel; include the half-pixel outline.
    double radius = extent + .5;
    RADII.put(key, radius);
    return radius;
  }

  public static void clear() {
    for (int texture : TEXTURES.values()) retire(texture);
    TEXTURES.clear(); RESOLVED.clear();
    if (surface != 0) GL11.glDeleteTextures(surface);
    if (depth != 0) EXTFramebufferObject.glDeleteRenderbuffersEXT(depth);
    if (framebuffer != 0) EXTFramebufferObject.glDeleteFramebuffersEXT(framebuffer);
    surface = depth = framebuffer = 0; unavailable = false;
  }

  /** View-local deferred heads can overlap the minimap frame, as in the reference renderer. */
  public static final class Batch implements AutoCloseable {
    private record Sprite(int texture, double x, double y, double brightness) {}
    private final List<Sprite> sprites = new ArrayList<>();
    private final Set<Integer> pinned = new HashSet<>();
    private final int limit;
    public Batch(int limit) { this.limit = Math.max(0, limit); }
    private boolean add(int texture, double x, double y, double brightness) {
      if (sprites.size() >= limit) return false;
      if (pinned.add(texture)) PINNED.merge(texture, 1, Integer::sum);
      sprites.add(new Sprite(texture, x, y, brightness));
      return true;
    }
    public void paint() {
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
      try {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        // Keep the enclosing HUD-preview scissor; only the minimap's own edge is bypassed.
        for (var sprite : sprites) paintHead(sprite.texture(), sprite.x(), sprite.y(),
            sprite.brightness(), null, false);
      } finally { GL11.glPopAttrib(); }
    }
    @Override public void close() {
      sprites.clear();
      for (int texture : pinned) {
        int count = PINNED.get(texture) - 1;
        if (count == 0) {
          PINNED.remove(texture);
          if (RETIRED.remove(texture)) GL11.glDeleteTextures(texture);
        } else PINNED.put(texture, count);
      }
      pinned.clear();
    }
  }

  private static void retire(int texture) {
    if (PINNED.containsKey(texture)) RETIRED.add(texture);
    else GL11.glDeleteTextures(texture);
  }

  public static boolean draw(Minecraft mc, RadarIconSource source, MapProjection view,
      boolean circle, double x, double y, double heightDifference, MapSettings settings) {
    return draw(mc, source, view, circle, x, y, heightDifference, settings, null);
  }

  public static boolean draw(Minecraft mc, RadarIconSource source, MapProjection view,
      boolean circle, double x, double y, double heightDifference, MapSettings settings, Batch batch) {
    if (source == null || unavailable || !MapKeys.iconsHeld(mc)
        || !GLContext.getCapabilities().GL_EXT_framebuffer_object) return false;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      Resolution resolved = RESOLVED.get(source);
      long now = System.nanoTime();
      boolean skin = source.skinUrl() != null && !source.skinUrl().isBlank();
      // Source lookup can decode a pack image or publish a downloaded skin. Bound it
      // independently and retain resolved IDs instead of repeating it for every dot.
      if ((resolved == null || skin && now >= resolved.refreshAt()) && resolutions > 0
          && (resolved != null || remaining > 0)) {
        resolutions--;
        int texture = source.skinUrl() == null || source.skinUrl().isBlank()
            ? mc.textureManager.getTextureId(source.texture())
            : mc.textureManager.method_1093(source.skinUrl(), source.texture());
        if (texture < 0) return false;
        resolved = new Resolution(new Key(source, texture), now + 500_000_000L);
        RESOLVED.put(source, resolved);
        if (RESOLVED.size() > LIMIT) {
          var oldest = RESOLVED.entrySet().iterator(); oldest.next(); oldest.remove();
        }
      }
      if (resolved == null) return false;
      Key key = resolved.key();
      Integer texture = TEXTURES.get(key);
      if (texture == null) {
        if (remaining == 0) return false;
        remaining--;
        texture = generate(mc, key);
        if (texture == 0) return false;
        TEXTURES.put(key, texture);
        if (TEXTURES.size() > LIMIT) {
          var oldest = TEXTURES.entrySet().iterator();
          retire(oldest.next().getValue()); oldest.remove();
        }
      }
      double brightness = settings.radarDimming
          ? 1 - .65 * Math.min(1, Math.abs(heightDifference) / settings.radarDimDistance) : 1;
      if (batch != null) return batch.add(texture, x, y, brightness);
      paintHead(texture, x, y, brightness, view, circle);
      return true;
    } finally { GL11.glPopAttrib(); }
  }

  private static void paintHead(int texture, double x, double y, double brightness,
      MapProjection view, boolean circle) {
    GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING);
    GL11.glDisable(GL11.GL_CULL_FACE); GL11.glDisable(GL11.GL_FOG); GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
    GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    GL11.glColor4d(brightness, brightness, brightness, 1);
    double half = 15.5, lo = 1d / SIDE, hi = 1 - lo;
    var polygon = List.of(new MapProjection.Point(x - half, y - half, lo, hi),
        new MapProjection.Point(x + half, y - half, hi, hi),
        new MapProjection.Point(x + half, y + half, hi, lo),
        new MapProjection.Point(x - half, y + half, lo, lo));
    MapRenderer.polygon(view == null ? polygon : view.clip(polygon, circle));
  }

  private static int texture() {
    int texture = GL11.glGenTextures();
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, SIDE, SIDE, 0,
        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
    return texture;
  }

  private static int generate(Minecraft mc, Key key) {
    RadarHeadModel.Head model = RadarHeadModel.create(key.source());
    if (model == null) return 0;
    int oldFramebuffer = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
    int oldRenderbuffer = GL11.glGetInteger(EXTFramebufferObject.GL_RENDERBUFFER_BINDING_EXT);
    int oldMatrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      if (framebuffer == 0) {
        framebuffer = EXTFramebufferObject.glGenFramebuffersEXT(); surface = texture();
        depth = EXTFramebufferObject.glGenRenderbuffersEXT();
        EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, depth);
        EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT,
            GL14.GL_DEPTH_COMPONENT24, SIDE, SIDE);
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebuffer);
        EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
            EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, GL11.GL_TEXTURE_2D, surface, 0);
        EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
            EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, depth);
        if (EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT)
            != EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT) { unavailable = true; return 0; }
      } else EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebuffer);
      GL11.glViewport(0, 0, SIDE, SIDE); GL11.glDisable(GL11.GL_SCISSOR_TEST);
      GL11.glDisable(GL11.GL_STENCIL_TEST); GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_FOG);
      GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glDepthFunc(GL11.GL_LEQUAL);
      GL11.glDepthRange(0, 1);
      GL11.glDepthMask(true); GL11.glColorMask(true, true, true, true);
      GL11.glClearColor(0, 0, 0, 0); GL11.glClearDepth(1);
      GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
      GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_ALPHA_TEST);
      GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
      GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
      GL11.glAlphaFunc(GL11.GL_GREATER, .01f); GL11.glEnable(GL11.GL_BLEND);
      GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
          GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity();
      GL11.glOrtho(0, SIDE, SIDE, 0, -100, 100);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity();
      GL11.glTranslated(32, 32, 0); GL11.glScaled(2, 2, -2);
      GL11.glTranslated(0, -model.centerY(), -model.centerZ());
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, key.texture());
      int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
      int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
      int logicalHeight = height >= width ? 64 : 32;
      GL11.glColor4f(1, 1, 1, 1);
      for (var box : model.base()) cuboid(box, logicalHeight);
      if (!model.overlay().isEmpty()) {
        if (!model.overlayTexture().isEmpty())
          GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.textureManager.getTextureId(model.overlayTexture()));
        int rgb = model.overlayColor();
        GL11.glColor4d((rgb >> 16 & 255) / 255d, (rgb >> 8 & 255) / 255d, (rgb & 255) / 255d, 1);
        for (var box : model.overlay()) cuboid(box, model.overlayTexture().isEmpty() ? logicalHeight : 32);
      }
      GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1); GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
      GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0); GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
      GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0); GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
      GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0); GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
      PIXELS.clear();
      GL11.glReadPixels(0, 0, SIDE, SIDE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, PIXELS);
      RadarIconPixels.outline(PIXELS, OUTLINED, SIDE);
      int texture = texture();
      GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, SIDE, SIDE,
          GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, OUTLINED);
      return texture;
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glMatrixMode(oldMatrix);
      EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, oldFramebuffer);
      EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, oldRenderbuffer);
      GL11.glPopClientAttrib(); GL11.glPopAttrib();
    }
  }

  private static void cuboid(RadarHeadModel.Cuboid box, int textureHeight) {
    double d = box.dilation(), x = box.x() - d, y = box.y() - d, z = box.z() - d;
    double x2 = box.x() + box.width() + d, y2 = box.y() + box.height() + d,
        z2 = box.z() + box.depth() + d;
    double[][] v = {{x,y,z}, {x2,y,z}, {x2,y2,z}, {x,y2,z},
        {x,y,z2}, {x2,y,z2}, {x2,y2,z2}, {x,y2,z2}};
    int u = box.u(), t = box.v(), w = box.width(), h = box.height(), l = box.depth();
    GL11.glBegin(GL11.GL_QUADS);
    face(v, 5,1,2,6, u+l+w,t+l,u+l+w+l,t+l+h, textureHeight);
    face(v, 0,4,7,3, u,t+l,u+l,t+l+h, textureHeight);
    face(v, 5,4,0,1, u+l,t,u+l+w,t+l, textureHeight);
    face(v, 2,3,7,6, u+l+w,t,u+l+w+w,t+l, textureHeight);
    face(v, 1,0,3,2, u+l,t+l,u+l+w,t+l+h, textureHeight);
    face(v, 4,5,6,7, u+l+w+l,t+l,u+l+w+l+w,t+l+h, textureHeight);
    GL11.glEnd();
  }
  private static void face(double[][] vertices, int a, int b, int c, int d,
      double u, double v, double u2, double v2, int height) {
    vertex(vertices[a], (u2-.1)/64, (v+.1)/height);
    vertex(vertices[b], (u+.1)/64, (v+.1)/height);
    vertex(vertices[c], (u+.1)/64, (v2-.1)/height);
    vertex(vertices[d], (u2-.1)/64, (v2-.1)/height);
  }
  private static void vertex(double[] vertex, double u, double v) {
    GL11.glTexCoord2d(u, v); GL11.glVertex3d(vertex[0], vertex[1], vertex[2]);
  }
}
