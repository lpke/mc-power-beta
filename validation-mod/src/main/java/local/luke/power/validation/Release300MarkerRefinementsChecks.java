package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.map.*;
import local.luke.power.mixin.CullingChunkCacheAccessor;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.class_555;
import net.minecraft.class_564;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Native marker pixels and strings, using only in-memory terrain and unsaved point snapshots. */
final class Release300MarkerRefinementsChecks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-300/marker-refinements");
  private static final long DEATH_TIME = LocalDateTime.of(2026, 10, 20, 13, 37)
      .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
  private static final String DEATH_DETAIL = "20/10/2026 01:37 PM", DEATH_COMPACT = "20/10 01:37";
  private Release300MarkerRefinementsChecks() {}

  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && !mc.player.dead,
        "open a disposable singleplayer world with a living player");
    check(Display.isActive() && !DetachedCamera.isActive(), "focus the disposable client and leave freecam");
    check(Maps.waypoints() != null && Maps.waypoints().ready() && !Maps.waypoints().needsMaintenance(),
        "wait for loaded, idle waypoints before marker checks");
    failures = 0;
    Screen previous = mc.currentScreen;
    var previousView = mc.field_2807;
    var map = MapConfig.copy(); boolean auto = MenuPreferences.current().autoApply;
    byte[] preferences = Files.readAllBytes(PowerConfig.path());
    String points = Catalog.JSON.toJson(Maps.waypoints().points());
    var chunks = (CullingChunkCacheAccessor)mc.world.method_259(); var loaded = new HashSet<>(chunks.power$loadedChunks().keySet());
    long submitted = MapIO.submitted();
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    int framebuffer = GLContext.getCapabilities().GL_EXT_framebuffer_object
        ? GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT) : 0;
    Object cameraCorrection = getStatic(WaypointMarkers.class, "CAMERA");
    Class<?> scales = Class.forName("local.luke.power.controls.util.ModOptions");
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try (var mapState = new Saved(Maps.class, "cache", "points", "x", "y", "z", "dimension", "layer", "world", "radarPreview");
        var markers = new Saved(WaypointMarkers.class); var correction = new Saved(cameraCorrection);
        var renderer = new Saved(MapRenderer.class); var minimap = new Saved(MinimapBuffer.class);
        var screenshot = new Saved(MapScreenshot.class);
        var renderedView = new Saved(RenderedView.class); var player = new Saved(mc.player, true);
        var options = new Saved(mc.options, true); var gameRenderer = new Saved(mc.field_2818, true);
        var scale = new Saved(scales, "guiScale", "realGuiScale");
        var memory = new Saved(Class.forName("local.luke.power.ui.HudEditorMemory"))) {
      try {
        // Detach existing GPU resources before either renderer sees the fixture cache.
        ((Map<?, ?>)getStatic(MapRenderer.class, "TEXTURES")).clear(); set(MapRenderer.class, "cache", null);
        set(MinimapBuffer.class, "active", null); set(MinimapBuffer.class, "preparing", null);
        MenuPreferences.current().autoApply = false;
        mc.options.guiScale = 2; set(scales, "guiScale", 2 / 8f); set(scales, "realGuiScale", 2 / 8f);
        mc.options.hideHud = false; mc.options.thirdPerson = false; mc.options.anaglyph3d = false;
        mc.setScreen(null); mc.field_2807 = mc.player;
        set(Maps.class, "x", mc.player.x); set(Maps.class, "y", mc.player.boundingBox.minY); set(Maps.class, "z", mc.player.z);
        set(Maps.class, "dimension", mc.world.dimension.id); set(Maps.class, "layer", 0); set(Maps.class, "world", mc.world);
        set(Maps.class, "cache", terrain(mc));
        var settings = MapConfig.copy(); settings.minimap = settings.worldMap = settings.worldWaypoints = settings.waypoints = settings.deathpoints = true;
        settings.waypointNames = true; settings.waypointDistances = false; settings.waypointDistance = 0;
        settings.radar = settings.footsteps = false; settings.playerOpacity = 0; settings.waypointDistanceScaling = false;
        settings.waypointTextSize = 100; settings.destinationArrowColor = 0xffcc33; settings.destinationArrowOpacity = 1;
        MapConfig.preview(settings);
        test("native camera applies only the selected waypoint bob mode through smooth walking phases", () -> bob(mc));
        test("actual world marker renderer expands aimed death time and preserves destination coordinates", () -> world(mc));
        test("world-map size scales symbols and text while minimap pixels stay independent", () -> sizes(mc));
        test("shared skull shadow and faceted diamonds produce native pixels", () -> symbols(mc));
        test("world-map hover uses native text for XYZ, full death time and three-dimensional distance", () -> hover(mc));
        test("HUD cave lighting previews its native minimap draft immediately and Cancel restores it", () -> caveLighting(mc));
      } finally {
        mc.setScreen(null);
        MinimapBuffer.clear(); MapRenderer.clear();
      }
    } finally {
      MenuPreferences.current().autoApply = auto; MapConfig.preview(map); mc.field_2807 = previousView; mc.setScreen(previous);
      if (GLContext.getCapabilities().GL_EXT_framebuffer_object)
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebuffer);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glPopAttrib(); GL11.glMatrixMode(mode);
    }
    test("marker fixture preserves preference bytes, saved waypoints, loaded chunks and worker admission", () -> {
      check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "marker fixture wrote global preferences");
      check(points.equals(Catalog.JSON.toJson(Maps.waypoints().points())), "marker fixture changed saved waypoints");
      check(loaded.equals(chunks.power$loadedChunks().keySet()), "marker fixture loaded native terrain");
      check(submitted == MapIO.submitted(), "marker fixture submitted map disk work");
      check(GL11.glGetError() == GL11.GL_NO_ERROR, "marker fixture left an OpenGL error");
    });
    log("RELEASE300 MARKER-REFINEMENTS failures=" + failures + " captures=" + REPORT);
  }

  /** Inert futures fill admission without starting a worker; resident tiles remain readable. */
  @SuppressWarnings("unchecked")
  private static MapCache terrain(Minecraft mc) throws Exception {
    var cache = new MapCache(REPORT.resolve("unused-terrain"), 128, text -> { throw new AssertionError(text); });
    var loading = (Map<MapMath.Key, CompletableFuture<MapTile>>)get(cache, "loading");
    for (int i = 0; i < 16; i++) loading.put(new MapMath.Key(mc.world.dimension.id, -1, 0, 10000 + i, 10000), new CompletableFuture<>());
    var tiles = (Map<MapMath.Key, MapTile>)get(cache, "tiles");
    int cx = Math.floorDiv((int)Math.floor(mc.player.x), MapMath.SIZE), cz = Math.floorDiv((int)Math.floor(mc.player.z), MapMath.SIZE);
    for (int layer : new int[]{-1, 0}) for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) {
      var key = new MapMath.Key(mc.world.dimension.id, layer, 0, x, z); var tile = new MapTile(key);
      for (int i = 0; i < MapMath.PIXELS; i++) tile.put(i, 0x68914b, 0x68914b, 63, 2, 1, 4);
      tile.savedRevision = tile.revision; tiles.put(key, tile);
    }
    return cache;
  }

  private static Waypoint point(String name, int dx, int dz) {
    var p = new Waypoint(); p.name = name; p.abbreviation = "FR"; p.set = Maps.waypoints().selectedSet();
    p.x = (int)Math.floor(Maps.x()) + dx; p.y = 70; p.z = (int)Math.floor(Maps.z()) + dz;
    p.dimension = Maps.dimension(); p.global = true; p.colour = 0x66ddee; return p;
  }
  private static List<Waypoint> points() {
    Waypoint ordinary = point("Fixture refuge", -45, -14), death = point("Death", 0, 14), destination = point("Destination", 45, -14);
    death.death = true; death.created = DEATH_TIME; destination.id = "temporary-destination";
    return List.of(ordinary, death, destination);
  }
  private static void publish(List<Waypoint> points) throws Exception { set(Maps.class, "points", List.copyOf(points)); }
  private static MapProjection projection(Minecraft mc, double scale) {
    var size = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
    return new MapProjection(Maps.x(), Maps.z(), scale, 16, 40, size.method_1857() - 32, size.method_1858() - 80, 0);
  }

  private static Map<String, double[]> labels() throws Exception {
    var result = new LinkedHashMap<String, double[]>();
    for (Object label : (List<?>)getStatic(WaypointMarkers.class, "LABELS")) {
      Waypoint p = (Waypoint)call(label, "point");
      result.put(p.id, new double[]{number(label, "x"), number(label, "y")});
    }
    return result;
  }
  private static Map<String, double[]> frame(Minecraft mc, Method camera, boolean bob, float phase) throws Exception {
    mc.options.bobView = bob; mc.player.field_1634 = phase - .4f; mc.player.field_1635 = phase;
    mc.player.field_524 = mc.player.field_525 = .6f; mc.player.field_1043 = mc.player.field_1044 = 2;
    camera.invoke(mc.field_2818, .5f, 0); WaypointMarkers.project(mc, .5f); return labels();
  }
  private static void bob(Minecraft mc) throws Exception {
    Method camera = class_555.class.getDeclaredMethod("method_1840", float.class, int.class); camera.setAccessible(true);
    try (var player = new Saved(mc.player, true); var options = new Saved(mc.options, true)) {
      mc.setScreen(null); mc.player.x = mc.player.field_1637 = Math.floor(mc.player.x) + .5;
      mc.player.z = mc.player.field_1639 = Math.floor(mc.player.z) + .5;
      mc.player.y = mc.player.field_1638 = Math.max(4, Math.min(122, Math.floor(mc.player.y)));
      mc.player.yaw = mc.player.prevYaw = mc.player.pitch = mc.player.prevPitch = 0;
      var points = new ArrayList<Waypoint>();
      for (int distance : new int[]{48, 800}) {
        var p = point("Camera " + distance, 0, distance); p.x = (int)Math.floor(mc.player.x);
        p.z = (int)Math.floor(mc.player.z) + distance; p.y = (int)Math.floor(mc.player.y) - 1; points.add(p);
      }
      publish(points); GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
      var config = MapConfig.copy(); config.waypointBobbing = 0; MapConfig.preview(config);
      var baseline = frame(mc, camera, false, .2f); check(baseline.size() == 2, "native camera clipped a 48m/800m marker");
      for (int bob = 0; bob <= 1; bob++) {
        config.waypointBobbing = bob; MapConfig.preview(config);
        Map<String, double[]> previous = null; boolean moved = false;
        for (int step = 0; step < 40; step++) {
          var projected = frame(mc, camera, true, .2f + step * .05f);
          check(projected.keySet().equals(baseline.keySet()), "walking phase lost a marker");
          for (String id : baseline.keySet()) for (int axis = 0; axis < 2; axis++) {
            double value = projected.get(id)[axis], difference = Math.abs(value - baseline.get(id)[axis]);
            check(Double.isFinite(value), "walking phase projected a non-finite marker"); moved |= difference > .0001;
            if (bob == 0) check(difference < .00003, "stable marker moved with native bob");
            if (previous != null) check(Math.abs(value - previous.get(id)[axis]) < .03, "walking marker snapped between adjacent phases");
          }
          previous = projected;
        }
        check(bob == 0 || moved, "native-bob mode ignored actual walking transforms");
        canvas(mc, () -> WaypointMarkers.render(mc)); capture(mc, "world-bob-mode-" + bob);
      }
      config.waypointBobbing = 0; MapConfig.preview(config);
    }
  }

  private static void world(Minecraft mc) throws Exception {
    Method camera = class_555.class.getDeclaredMethod("method_1840", float.class, int.class); camera.setAccessible(true);
    try (var player = new Saved(mc.player, true)) {
      mc.setScreen(null); mc.player.x = mc.player.field_1637 = Math.floor(mc.player.x) + .5;
      mc.player.z = mc.player.field_1639 = Math.floor(mc.player.z) + .5;
      mc.player.y = mc.player.field_1638 = Math.max(4, Math.min(122, Math.floor(mc.player.y)));
      mc.player.yaw = mc.player.prevYaw = mc.player.pitch = mc.player.prevPitch = 0;
      Waypoint death = point("Death", 0, 800), ordinary = point("Fixture refuge", -150, 800), destination = point("Destination", 150, 800);
      for (var p : List.of(death, ordinary, destination)) { p.x = (int)Math.floor(mc.player.x) + (p == ordinary ? -150 : p == destination ? 150 : 0); p.y = (int)Math.floor(mc.player.y) - 1; p.z = (int)Math.floor(mc.player.z) + 800; }
      death.death = true; death.created = DEATH_TIME; destination.id = "temporary-destination";
      publish(List.of(death, ordinary, destination));
      var text = text(() -> canvas(mc, () -> {
        frame(mc, camera, false, .2f); gui(mc); WaypointMarkers.render(mc);
      }));
      check(text.contains(DEATH_DETAIL) && !text.contains(DEATH_COMPACT), "aimed world death marker omitted full local date/time: " + text);
      check(text.contains("FR") && text.contains(destination.x + "," + destination.y + "," + destination.z), "ordinary/destination world marker content diverged: " + text);
      capture(mc, "world-aimed-death");
    }
  }

  private static BufferedImage mapPixels(Minecraft mc, List<Waypoint> points, boolean mini, int size) throws Exception {
    publish(points); var settings = MapConfig.copy(); settings.worldMapWaypointSize = size;
    canvas(mc, () -> { MapRenderer.beginFrame(.5f); MapRenderer.markers(mc, projection(mc, 2), Maps.dimension(), false, mini, settings); });
    return pixels(mc);
  }
  private static void sizes(Minecraft mc) throws Exception {
    var ordinary = point("Fixture refuge", 0, 0);
    BufferedImage small = mapPixels(mc, List.of(ordinary), false, 50), large = mapPixels(mc, List.of(ordinary), false, 150);
    int[] a = bounds(small), b = bounds(large);
    check(b[0] > a[0] * 2 && b[1] > a[1] * 2, "world-map size did not scale native symbol and label pixels together");
    var miniSmall = mapPixels(mc, List.of(ordinary), true, 50); var miniLarge = mapPixels(mc, List.of(ordinary), true, 150);
    check(Arrays.equals(rgb(miniSmall), rgb(miniLarge)), "world-map size changed minimap pixels");
    var points = points();
    var labels = text(() -> mapPixels(mc, points, false, 100));
    check(labels.contains(DEATH_COMPACT) && labels.contains("Fixture refuge")
        && labels.contains(points.get(2).x + "," + points.get(2).y + "," + points.get(2).z), "world-map native marker labels changed: " + labels);
    capture(mc, "world-map-markers");
    labels = text(() -> mapPixels(mc, points, true, 150));
    check(!labels.contains(DEATH_COMPACT) && labels.contains(WaypointTimes.time(DEATH_TIME)) && labels.contains("FR"), "minimap lost time-only death label or abbreviations");
    canvas(mc, () -> {
      var size = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
      var minimap = MapConfig.copy(); minimap.circular = minimap.rotate = false; minimap.size = 180;
      MapRenderer.beginFrame(.5f); Minimap.draw(mc, minimap, size.method_1857(), size.method_1858(), false, true);
    });
    capture(mc, "minimap-markers");
  }

  private static void symbols(Minecraft mc) throws Exception {
    var death = point("Death", 0, 0); death.death = true;
    canvas(mc, () -> WaypointSymbols.draw(mc, death, 80, 80, 18, 0xffffff, 1));
    var shadowed = pixels(mc);
    canvas(mc, () -> {
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.textureManager.getTextureId("/assets/powerbeta/map/deathpoint.png"));
      GL11.glColor4f(1, 1, 1, 1); GL11.glBegin(GL11.GL_QUADS);
      GL11.glTexCoord2f(0, 0); GL11.glVertex2d(62, 62); GL11.glTexCoord2f(1, 0); GL11.glVertex2d(98, 62);
      GL11.glTexCoord2f(1, 1); GL11.glVertex2d(98, 98); GL11.glTexCoord2f(0, 1); GL11.glVertex2d(62, 98); GL11.glEnd();
    });
    var plain = pixels(mc); int addedShadow = 0, background = plain.getRGB(0, 0);
    for (int y = 0; y < plain.getHeight(); y++) for (int x = 0; x < plain.getWidth(); x++)
      if (plain.getRGB(x, y) == background && shadowed.getRGB(x, y) != background) addedShadow++;
    check(addedShadow > 8, "shared skull renderer added no shadow beyond the glyph");
    canvas(mc, () -> WaypointSymbols.diamond(160, 80, 18, 0xff5555, 1));
    var diamond = pixels(mc); var colors = new HashSet<Integer>(); int backgroundColor = diamond.getRGB(0, 0);
    for (int color : rgb(diamond)) if (color != backgroundColor && (color & 0xffffff) != 0) colors.add(color);
    check(colors.size() >= 3, "native shared diamond rendered without shaded facets");
    canvas(mc, () -> {
      WaypointSymbols.draw(mc, death, 80, 80, 18, 0xffffff, 1);
      WaypointSymbols.diamond(160, 80, 18, 0xff5555, 1);
      WaypointSymbols.diamond(240, 80, 18, 0x66ddee, 1);
    });
    capture(mc, "shared-symbols");
  }

  private static void hover(Minecraft mc) throws Exception {
    var points = points(); publish(points);
    var screen = new WorldMapScreen(); mc.setScreen(screen);
    var scaled = new class_564(mc.options, mc.displayWidth, mc.displayHeight); screen.init(mc, scaled.method_1857(), scaled.method_1858());
    try {
      MapProjection view = (MapProjection)call(screen, "projection");
      var cursor = view.screen(points.get(1).x + .5, points.get(1).z + .5);
      int mx = (int)Math.round(cursor.x()), my = (int)Math.round(cursor.y());
      var labels = text(() -> canvas(mc, () -> screen.render(mx, my, .5f)));
      var death = points.get(1); double dx = death.x + .5 - mc.player.x, dy = death.y - mc.player.boundingBox.minY, dz = death.z + .5 - mc.player.z;
      check(labels.contains(DEATH_DETAIL) && labels.contains(death.x + ", " + death.y + ", " + death.z)
          && labels.contains(Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz)) + "m"), "native waypoint hover omitted full time, XYZ or distance: " + labels);
      var at = view.world(mx, my);
      check(labels.stream().anyMatch(value -> value.startsWith((int)Math.floor(at.x()) + ", 63, " + (int)Math.floor(at.y()))), "map cursor readout did not use cached Y between X and Z: " + labels);
      capture(mc, "world-map-death-hover");
      screen.init(mc, 320, 180);
      String unbroken = "W".repeat(64);
      var wrapped = text(() -> canvas(mc, () -> call(screen, "tooltip",
          new Class<?>[]{String.class, int.class, int.class}, unbroken, 310, 70)));
      check(String.join("", wrapped).equals(unbroken) && wrapped.size() > 1
          && wrapped.stream().allMatch(value -> mc.textRenderer.getWidth(value) <= 280),
          "long waypoint tooltip lost text or escaped narrow window: " + wrapped);
    } finally { mc.setScreen(null); }
  }

  private static void caveLighting(Minecraft mc) throws Exception {
    var options = new PowerOptionsScreen(null); mc.setScreen(options);
    var original = Catalog.JSON.toJsonTree(MapConfig.current()); Setting parent = find(options.session(), "maps.caveLighting"); var parentValue = parent.value.deepCopy();
    try {
      var hud = new HudEditorScreen(options, parent.id); mc.setScreen(hud);
      set(hud, "background", false);
      var size = new class_564(mc.options, mc.displayWidth, mc.displayHeight); hud.init(mc, size.method_1857(), size.method_1858());
      var draft = (SettingsDraft)get(hud, "draft"); Setting lighting = draft.get(parent.id);
      check(HudEditorSettings.contains(lighting) && ((List<?>)call(hud, "controls")).contains(lighting), "cave lighting is absent from actual HUD controls");
      call(hud, "revealControl", new Class<?>[]{Setting.class}, lighting);
      int x = ((Number)call(hud, "controlX", new Class<?>[]{Setting.class}, lighting)).intValue();
      int y = ((Number)call(hud, "controlY", new Class<?>[]{Setting.class}, lighting)).intValue();
      check(y >= number(hud, "controlsTop") && y + 18 <= number(hud, "controlsBottom"), "cave-lighting row could not be exposed");
      boolean before = lighting.value.getAsBoolean();
      ((ScreenInput)(Object)hud).power$click(x + 8, y + 8, 0); ((ScreenInput)(Object)hud).power$release(x + 8, y + 8, 0);
      var preview = (MapSettings)get(get(hud, "preview"), "map");
      check(lighting.value.getAsBoolean() != before && preview.caveLighting != before,
          "native cave-lighting click did not rebuild minimap preview immediately");
      check(original.equals(Catalog.JSON.toJsonTree(MapConfig.current())) && parent.value.equals(parentValue), "cave preview escaped its HUD draft");
      canvas(mc, () -> hud.render(-1, -1, .5f)); capture(mc, "hud-cave-lighting");
      for (Object button : (List<?>)call(hud, "footer")) if (call(button, "label").equals("Cancel")) {
        ((ScreenInput)(Object)hud).power$click((int)number(button, "x") + 8, hud.height - 19, 0); break;
      }
      check((boolean)call(get(hud, "exit"), "active"), "dirty cave lighting Cancel omitted its prompt");
      ((ScreenInput)(Object)hud).power$click(hud.width / 2, hud.height / 2 + 18, 0);
      check(mc.currentScreen == options && parent.value.equals(parentValue) && original.equals(Catalog.JSON.toJsonTree(MapConfig.current())), "Cancel retained cave-lighting draft changes");
    } finally { options.session().discard(); mc.setScreen(null); }
  }

  private static void gui(Minecraft mc) {
    var size = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity(); GL11.glOrtho(0, size.method_1857(), size.method_1858(), 0, 1000, 3000);
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity(); GL11.glTranslatef(0, 0, -2000);
  }
  private static void canvas(Minecraft mc, Check draw) throws Exception {
    GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight); GL11.glDisable(GL11.GL_SCISSOR_TEST);
    GL11.glClearColor(32 / 255f, 35 / 255f, 42 / 255f, 1); GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    gui(mc); GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_CULL_FACE);
    GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glEnable(GL11.GL_ALPHA_TEST); GL11.glAlphaFunc(GL11.GL_GREATER, .01f); GL11.glColor4f(1, 1, 1, 1);
    draw.run(); GL11.glFinish();
  }
  private static BufferedImage pixels(Minecraft mc) {
    var bytes = BufferUtils.createByteBuffer(mc.displayWidth * mc.displayHeight * 4);
    GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
    try {
      GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1); GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
      GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0); GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
      GL11.glReadPixels(0, 0, mc.displayWidth, mc.displayHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bytes);
    } finally { GL11.glPopClientAttrib(); }
    var result = new BufferedImage(mc.displayWidth, mc.displayHeight, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < mc.displayHeight; y++) for (int x = 0; x < mc.displayWidth; x++) {
      int at = ((mc.displayHeight - y - 1) * mc.displayWidth + x) * 4;
      result.setRGB(x, y, (bytes.get(at) & 255) << 16 | (bytes.get(at + 1) & 255) << 8 | bytes.get(at + 2) & 255);
    }
    return result;
  }
  private static int[] rgb(BufferedImage image) { return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()); }
  private static int[] bounds(BufferedImage image) {
    int minX = image.getWidth(), minY = image.getHeight(), maxX = -1, maxY = -1, background = image.getRGB(0, 0);
    for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) if (image.getRGB(x, y) != background) {
      minX = Math.min(minX, x); minY = Math.min(minY, y); maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
    }
    return new int[]{maxX - minX + 1, maxY - minY + 1};
  }
  private static void capture(Minecraft mc, String name) throws Exception { Files.createDirectories(REPORT); ImageIO.write(pixels(mc), "PNG", REPORT.resolve(name + ".png").toFile()); }
  private static List<String> text(Check render) throws Exception {
    boolean recording = Release240Checks.captureTitle; var previous = List.copyOf(Release240Checks.titleText);
    try { Release240Checks.titleText.clear(); Release240Checks.captureTitle = true; render.run(); return List.copyOf(Release240Checks.titleText); }
    finally { Release240Checks.captureTitle = recording; Release240Checks.titleText.clear(); Release240Checks.titleText.addAll(previous); }
  }
  private static Object get(Object owner, String name) throws Exception { return field(owner instanceof Class<?> type ? type : owner.getClass(), name).get(owner instanceof Class<?> ? null : owner); }
  private static Object getStatic(Class<?> owner, String name) throws Exception { return get(owner, name); }
  private static void set(Object owner, String name, Object value) throws Exception { field(owner instanceof Class<?> type ? type : owner.getClass(), name).set(owner instanceof Class<?> ? null : owner, value); }
  private static Field field(Class<?> owner, String name) throws Exception {
    for (; owner != null; owner = owner.getSuperclass()) try { Field field = owner.getDeclaredField(name); field.setAccessible(true); return field; } catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  private static Object call(Object owner, String name) throws Exception { return call(owner, name, new Class<?>[0]); }
  private static Object call(Object owner, String name, Class<?>[] types, Object... values) throws Exception { return Editor230Checks.invoke(owner, name, types, values); }
  private static double number(Object owner, String name) throws Exception { return ((Number)call(owner, name)).doubleValue(); }

  /** Preserves mutable render buffers as well as primitive/reference fields without replacing finals. */
  private static final class Saved implements AutoCloseable {
    private record Value(Field field, Object value) {}
    final Object target; final List<Value> values = new ArrayList<>();
    Saved(Object owner, String... names) throws Exception {
      target = owner instanceof Class<?> ? null : owner;
      Class<?> type = owner instanceof Class<?> clazz ? clazz : owner.getClass();
      for (String name : names) remember(field(type, name));
      if (names.length == 0) scan(type, false);
    }
    Saved(Object owner, boolean primitive) throws Exception { target = owner; scan(owner.getClass(), primitive); }
    private void scan(Class<?> type, boolean primitive) throws Exception {
      for (; type != null; type = type.getSuperclass()) for (Field f : type.getDeclaredFields())
        if (Modifier.isStatic(f.getModifiers()) == (target == null) && (!primitive || f.getType().isPrimitive())) { f.setAccessible(true); remember(f); }
    }
    private void remember(Field f) throws Exception {
      Object value = f.get(target);
      if (value instanceof FloatBuffer b) { float[] copy = new float[b.capacity()]; for (int i = 0; i < copy.length; i++) copy[i] = b.get(i); value = copy; }
      else if (value instanceof IntBuffer b) { int[] copy = new int[b.capacity()]; for (int i = 0; i < copy.length; i++) copy[i] = b.get(i); value = copy; }
      else if (value instanceof ByteBuffer b) { byte[] copy = new byte[b.capacity()]; for (int i = 0; i < copy.length; i++) copy[i] = b.get(i); value = copy; }
      else if (value instanceof double[] a) value = a.clone();
      else if (Modifier.isFinal(f.getModifiers()) && value instanceof Map<?, ?> m) value = new LinkedHashMap<>(m);
      else if (Modifier.isFinal(f.getModifiers()) && value instanceof Set<?> s) value = new LinkedHashSet<>(s);
      else if (Modifier.isFinal(f.getModifiers()) && value instanceof List<?> l) value = new ArrayList<>(l);
      else if (Modifier.isFinal(f.getModifiers())) return;
      values.add(new Value(f, value));
    }
    @SuppressWarnings({"unchecked", "rawtypes"}) public void close() throws Exception {
      for (Value saved : values) {
        Object current = saved.field.get(target), value = saved.value;
        if (current instanceof FloatBuffer b && value instanceof float[] a) for (int i = 0; i < a.length; i++) b.put(i, a[i]);
        else if (current instanceof IntBuffer b && value instanceof int[] a) for (int i = 0; i < a.length; i++) b.put(i, a[i]);
        else if (current instanceof ByteBuffer b && value instanceof byte[] a) for (int i = 0; i < a.length; i++) b.put(i, a[i]);
        else if (current instanceof double[] a && value instanceof double[] copy) System.arraycopy(copy, 0, a, 0, copy.length);
        else if (Modifier.isFinal(saved.field.getModifiers()) && current instanceof Map m && value instanceof Map copy) { m.clear(); m.putAll(copy); }
        else if (Modifier.isFinal(saved.field.getModifiers()) && current instanceof Collection c && value instanceof Collection copy) { c.clear(); c.addAll(copy); }
        else saved.field.set(target, value);
      }
    }
  }
}
