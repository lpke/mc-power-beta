package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.Field;
import java.util.*;
import local.luke.power.config.Catalog;
import local.luke.power.map.*;
import local.luke.power.ui.WorldMapScreen;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Native map interaction checks. Asynchronous checks only observe ordinary client ticks. */
final class Release300MapInteractions {
  private static final long FOUR_SECONDS = 4_000_000_000L;
  private static FocusDrag focus;
  private static PausedSampling paused;
  private Release300MapInteractions() {}

  private static final class FocusDrag {
    final Screen previous;
    final WorldMapScreen screen = new WorldMapScreen();
    boolean sawFocusLoss;
    int ticks;
    FocusDrag(Minecraft mc) { previous = mc.currentScreen; }
  }
  private static final class PausedSampling {
    final Screen previous;
    final MapSettings settings = MapConfig.copy();
    final boolean wasPaused;
    final WorldMapScreen screen = new WorldMapScreen();
    MapSampler sampler;
    int ticks, stable;
    long worldTime, samplerTicks, columns;
    PausedSampling(Minecraft mc) { previous = mc.currentScreen; wasPaused = mc.paused; }
  }

  static boolean pending() { return focus != null || paused != null; }
  static Map<String, Object> state() {
    return Map.of("focusArmed", focus != null, "focusLossSeen", focus != null && focus.sawFocusLoss,
        "pausedArmed", paused != null, "pausedTicks", paused == null ? 0 : paused.stable,
        "pausedColumns", paused == null ? 0 : paused.columns);
  }

  static void run(Minecraft mc, String action) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null, "open a disposable singleplayer world first");
    if (action.equals("stop")) { finishFocus(mc, false); finishPaused(mc, null, false); return; }
    if (action.equals("focus-check")) { finishFocus(mc, true); return; }
    check(focus == null && paused == null, "finish the armed interaction check or use map-interactions stop");
    check(MapConfig.current().worldMap && Maps.cache() != null, "enable the World Map and wait for its cache before interaction checks");
    if (action.equals("focus-arm")) {
      check(Display.isActive(), "focus the disposable client before arming scale-line focus loss");
      focus = new FocusDrag(mc);
      try { mc.setScreen(focus.screen); pressScale(mc, focus.screen); }
      catch (Exception | AssertionError failure) { finishFocus(mc, false); throw failure; }
      log("300 MAP FOCUS ARMED; physically move focus away, then run release-300 map-interactions focus-check");
      return;
    }
    if (action.equals("paused")) {
      paused = new PausedSampling(mc);
      var settings = MapConfig.copy(); settings.worldMap = settings.pauseMap = true; settings.updateRate = 20;
      MapConfig.preview(settings); Maps.tick(mc); mc.setScreen(paused.screen);
      log("300 MAP PAUSED ARMED; wait for RELEASE300 MAP PAUSED COMPLETE across actual native ticks");
      return;
    }
    check(action.equals("checks"), "use map-interactions [focus-arm|focus-check|paused|stop]");
    Screen previous = mc.currentScreen;
    try {
      test("scale-line drag owns its press, follows native pointer offsets and resets only on left release", () -> scale(mc));
      test("destination context first item clears the destination without touching saved waypoints", () -> destination(mc));
      test("waypoint delete confirmation expires after four seconds and two timely clicks remove only its target", () -> delete(mc));
      test("screenshot notice has a four-second deadline and expires without reopening the map", Release300MapInteractions::screenshotExpiry);
    } finally { mc.setScreen(previous); }
    log("RELEASE300 MAP-INTERACTIONS failures=" + failures);
  }

  private static Object get(Object target, String field) throws Exception { return Editor230Checks.get(target, field); }
  private static Object call(Object target, String method, Class<?>[] types, Object... args) throws Exception {
    return Editor230Checks.invoke(target, method, types, args);
  }
  private static Object call(Object target, String method) throws Exception { return call(target, method, new Class<?>[0]); }
  private static Field field(Class<?> type, String name) throws Exception {
    Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
  }
  private static void set(Object target, String name, Object value) throws Exception { field(target.getClass(), name).set(target, value); }
  private static double value(Object target, String name) throws Exception { return ((Number)get(target, name)).doubleValue(); }
  private static void click(Screen screen, int x, int y, int button) { ((ScreenInput)(Object)screen).power$click(x, y, button); }
  private static void release(Screen screen, int x, int y, int button) { ((ScreenInput)(Object)screen).power$release(x, y, button); }

  private static void draw(Minecraft mc, Screen screen) {
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
      GL11.glDisable(GL11.GL_SCISSOR_TEST);
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity();
      GL11.glOrtho(0, screen.width, screen.height, 0, 1000, 3000);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity(); GL11.glTranslatef(0, 0, -2000);
      screen.render(-1, -1, 0);
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib();
    }
  }

  private static void pressScale(Minecraft mc, WorldMapScreen screen) throws Exception {
    int scaleY = ((Number)call(screen, "scaleY")).intValue();
    double pointerX = Mouse.getX() * (double)screen.width / mc.displayWidth;
    int x = Math.abs(pointerX - (screen.width - 120)) < 8 ? screen.width - 16 : screen.width - 120;
    int y = scaleY - 8;
    click(screen, x, y, 0);
    check((boolean)get(screen, "scaleDragging") && !(boolean)get(screen, "dragging"), "scale press leaked into terrain panning");
    draw(mc, screen);
    double expectedX = Mouse.getX() * (double)screen.width / mc.displayWidth - x;
    double expectedY = screen.height - Mouse.getY() * (double)screen.height / mc.displayHeight - 1 - y;
    check(Math.abs(value(screen, "scaleDx") - expectedX) < 1e-6 && Math.abs(value(screen, "scaleDy") - expectedY) < 1e-6,
        "scale line did not follow the native pointer delta");
    check(Math.abs(expectedX) >= 8, "fixture did not produce a meaningful scale-line displacement");
  }
  private static void scale(Minecraft mc) throws Exception {
    check(Display.isActive(), "focus the disposable client before scale-line checks");
    var screen = new WorldMapScreen(); mc.setScreen(screen);
    var view = (MapMath.View)get(screen, "view"); double x = view.x, z = view.z;
    pressScale(mc, screen);
    double dx = value(screen, "scaleDx"), dy = value(screen, "scaleDy");
    release(screen, 0, 0, 1);
    check((boolean)get(screen, "scaleDragging") && value(screen, "scaleDx") == dx && value(screen, "scaleDy") == dy,
        "unrelated right release canceled the left-owned scale drag");
    release(screen, 0, 0, 0);
    check(!(boolean)get(screen, "scaleDragging") && value(screen, "scaleDx") == 0 && value(screen, "scaleDy") == 0,
        "left release retained the scale-line drag or offsets");
    draw(mc, screen);
    check(!(boolean)get(screen, "scaleDragging") && value(screen, "scaleDx") == 0 && value(screen, "scaleDy") == 0,
        "a released scale drag restarted on the next frame");
    check(view.x == x && view.z == z, "scale-line drag panned the terrain");
  }

  /** Temporary native store data and point snapshots never reach an asynchronous save. */
  private static final class Points implements AutoCloseable {
    final WaypointStore store = Maps.waypoints();
    final Object originalData, originalPoints, originalDestination;
    final long revision, pointsRevision;
    Points() throws Exception {
      check(store != null && store.ready() && !store.needsMaintenance(), "wait for idle, loaded waypoints before interaction checks");
      originalData = get(store, "data"); revision = ((Number)get(store, "revision")).longValue();
      originalPoints = field(Maps.class, "points").get(null);
      originalDestination = field(Maps.class, "destination").get(null);
      pointsRevision = field(Maps.class, "pointsRevision").getLong(null);
      set(store, "data", WaypointStore.decode(WaypointStore.encode((WaypointStore.Data)originalData)));
    }
    void publish(Waypoint... preferred) throws Exception {
      var values = new ArrayList<Waypoint>();
      Set<String> preferredIds = new HashSet<>();
      for (Waypoint point : preferred) { values.add(point.copy()); preferredIds.add(point.id); }
      store.points().stream().filter(point -> !preferredIds.contains(point.id)).forEach(values::add);
      var destination = (Waypoint)field(Maps.class, "destination").get(null);
      if (destination != null) values.add(0, destination.copy());
      field(Maps.class, "points").set(null, List.copyOf(values));
      field(Maps.class, "pointsRevision").setLong(null, store.revision());
    }
    public void close() throws Exception {
      set(store, "data", originalData); set(store, "revision", revision);
      field(Maps.class, "points").set(null, originalPoints);
      field(Maps.class, "destination").set(null, originalDestination);
      field(Maps.class, "pointsRevision").setLong(null, pointsRevision);
    }
  }
  private static int[] marker(WorldMapScreen screen, Waypoint point) throws Exception {
    var projection = (MapProjection)call(screen, "projection");
    var xy = projection.screen(point.x + .5, point.z + .5);
    return new int[]{(int)Math.round(xy.x()), (int)Math.round(xy.y())};
  }
  private static void contextRow(WorldMapScreen screen, int row) throws Exception {
    click(screen, ((Number)get(screen, "contextX")).intValue() + 8,
        ((Number)get(screen, "contextY")).intValue() + row * 18 + 8, 0);
  }
  private static Waypoint point(int dx, int dz) {
    Waypoint point = MapKeys.newWaypoint();
    point.x = (int)MapMath.bounded(point.x + dx, -MapMath.BORDER, MapMath.BORDER);
    point.z = (int)MapMath.bounded(point.z + dz, -MapMath.BORDER, MapMath.BORDER);
    point.name = "Validation interaction " + UUID.randomUUID(); return point;
  }
  private static void destination(Minecraft mc) throws Exception {
    try (var points = new Points()) {
      String saved = Catalog.JSON.toJson(points.store.points());
      Waypoint destination = point(24, -20); Maps.destination(destination); points.publish();
      var screen = new WorldMapScreen(); mc.setScreen(screen);
      int[] hit = marker(screen, destination); click(screen, hit[0], hit[1], 1);
      check((boolean)get(screen, "context"), "destination marker right-click did not open its context");
      check(((Waypoint)get(screen, "target")).id.equals("temporary-destination"), "destination marker hit the wrong context target");
      check(((List<?>)call(screen, "contextActions")).get(0).equals("Clear destination"), "destination Clear is not the first item");
      contextRow(screen, 0);
      check(field(Maps.class, "destination").get(null) == null && !(boolean)get(screen, "context"), "first destination item failed to clear and close");
      check(saved.equals(Catalog.JSON.toJson(points.store.points())), "clearing destination changed saved waypoints");
    }
  }
  private static void delete(Minecraft mc) throws Exception {
    try (var points = new Points()) {
      Waypoint target = point(20, 20), retained = point(-20, 20);
      points.store.put(target); points.store.put(retained); points.publish(target, retained);
      String expected = Catalog.JSON.toJson(points.store.points().stream().filter(p -> !p.id.equals(target.id)).toList());
      var screen = new WorldMapScreen(); mc.setScreen(screen);
      int[] hit = marker(screen, target); click(screen, hit[0], hit[1], 1);
      check(((Waypoint)get(screen, "target")).id.equals(target.id), "waypoint right-click selected the wrong target");
      var actions = (List<?>)call(screen, "contextActions"); int deleteRow = actions.indexOf("Delete waypoint");
      check(deleteRow >= 0, "saved waypoint is missing Delete");
      long before = System.nanoTime(); contextRow(screen, deleteRow); long after = System.nanoTime();
      long until = ((Number)get(screen, "deleteUntil")).longValue();
      check(until >= before + FOUR_SECONDS && until <= after + FOUR_SECONDS, "delete confirmation deadline is not four seconds");
      check(points.store.contains(target.id) && (boolean)get(screen, "context"), "first Delete removed the point or closed confirmation");
      check(((List<?>)call(screen, "contextActions")).get(deleteRow).equals("Confirm delete?"), "Delete did not become a confirmation");
      set(screen, "deleteUntil", System.nanoTime() - 6_000_000_000L);
      check(((List<?>)call(screen, "contextActions")).get(deleteRow).equals("Delete waypoint"), "expired delete confirmation remained armed");
      contextRow(screen, deleteRow);
      check(points.store.contains(target.id), "click after confirmation expiry deleted without rearming");
      contextRow(screen, deleteRow);
      check(!points.store.contains(target.id) && !(boolean)get(screen, "context"), "two timely Delete clicks failed to remove only the target");
      check(points.store.contains(retained.id) && expected.equals(Catalog.JSON.toJson(points.store.points())), "Delete changed another waypoint");
    }
  }
  private static void screenshotExpiry() throws Exception {
    MapScreenshot.status();
    check(field(MapScreenshot.class, "writing").get(null) == null, "wait for the existing screenshot to finish before notice checks");
    Object original = field(MapScreenshot.class, "status").get(null);
    long originalExpires = field(MapScreenshot.class, "expires").getLong(null);
    try {
      var method = MapScreenshot.class.getDeclaredMethod("message", String.class); method.setAccessible(true);
      long before = System.nanoTime(); method.invoke(null, "Validation screenshot notice"); long after = System.nanoTime();
      long expires = field(MapScreenshot.class, "expires").getLong(null);
      check(expires >= before + FOUR_SECONDS && expires <= after + FOUR_SECONDS, "screenshot notice deadline is not four seconds");
      check(MapScreenshot.status().equals("Validation screenshot notice"), "fresh screenshot notice was hidden");
      field(MapScreenshot.class, "expires").setLong(null, System.nanoTime() - 6_000_000_000L);
      check(MapScreenshot.status().isEmpty(), "screenshot notice survived expiry without reopening");
    } finally {
      field(MapScreenshot.class, "status").set(null, original);
      field(MapScreenshot.class, "expires").setLong(null, originalExpires);
    }
  }

  static void observe(Minecraft mc) {
    if (focus != null) { focus.ticks++; if (!Display.isActive()) focus.sawFocusLoss = true; }
    PausedSampling task = paused;
    if (task == null) return;
    try {
      check(++task.ticks <= 400, "paused-map sampling did not progress within 400 native ticks");
      check(mc.currentScreen == task.screen, "paused sampling lost its World Map screen");
      if (!mc.paused) return;
      if (task.sampler == null) {
        task.sampler = Maps.sampler(); check(task.sampler != null, "paused map has no sampler");
        task.worldTime = mc.world.getTime(); task.samplerTicks = ((Number)get(task.sampler, "ticks")).longValue();
        int x = (int)Math.floor(Maps.x()), z = (int)Math.floor(Maps.z());
        task.sampler.invalidate(x, z, x, z);
        return;
      }
      check(mc.world.getTime() == task.worldTime, "World Map did not pause the simulation clock");
      check(Maps.sampler() == task.sampler, "paused map replaced its sampler");
      task.columns += task.sampler.columnsLastTick; task.stable++;
      long ticks = ((Number)get(task.sampler, "ticks")).longValue() - task.samplerTicks;
      if (task.stable >= 40 && ticks >= 20 && task.columns > 0) finishPaused(mc, null, true);
    } catch (Throwable failure) {
      try { finishPaused(mc, failure, true); } catch (Exception restore) { restore.printStackTrace(); }
    }
  }
  private static void finishFocus(Minecraft mc, boolean verify) throws Exception {
    FocusDrag task = focus;
    if (task == null) { if (verify) throw new AssertionError("run map-interactions focus-arm first"); return; }
    focus = null;
    try {
      if (verify) test("physical focus loss cancels the owned scale drag and resets its offsets", () -> {
        check(task.sawFocusLoss, "the observer did not see physical display focus loss");
        check(mc.currentScreen == task.screen && !(boolean)get(task.screen, "scaleDragging")
            && value(task.screen, "scaleDx") == 0 && value(task.screen, "scaleDy") == 0, "focus loss retained scale drag ownership or offsets");
      });
    } finally { mc.setScreen(task.previous); }
    log("RELEASE300 MAP FOCUS COMPLETE failures=" + failures + " nativeTicks=" + task.ticks);
  }
  private static void finishPaused(Minecraft mc, Throwable failure, boolean report) throws Exception {
    PausedSampling task = paused;
    if (task == null) return;
    paused = null;
    try { MapConfig.preview(task.settings); mc.paused = task.wasPaused; mc.setScreen(task.previous); }
    catch (Throwable restore) { if (failure == null) failure = restore; else failure.addSuppressed(restore); }
    if (failure != null) { failures++; log("FAIL 300 paused World Map sampling " + failure); }
    else if (report) log("PASS paused World Map keeps simulation time fixed while native ticks continue terrain sampling");
    if (report) log("RELEASE300 MAP PAUSED COMPLETE failures=" + failures + " nativeTicks=" + task.stable + " sampledColumns=" + task.columns);
  }
}
