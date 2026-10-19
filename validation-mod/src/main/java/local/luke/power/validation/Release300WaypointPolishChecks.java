package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.api.PlayerWarps;
import local.luke.power.commands.command.vanilla.Help;
import local.luke.power.commands.util.*;
import local.luke.power.config.Catalog;
import local.luke.power.map.*;
import local.luke.power.permissions.CommandPermissions;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Native row actions, editor drafts and command aliases use an unsaved shadow waypoint store. */
final class Release300WaypointPolishChecks {
  private Release300WaypointPolishChecks() {}
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null, "open a disposable singleplayer world first");
    check(Display.isActive(), "focus the disposable client before waypoint input checks");
    check(!local.luke.power.input.DetachedCamera.isActive() && !mc.player.dead && mc.player.health > 0, "use a living player outside freecam for waypoint checks");
    failures = 0;
    Screen previous = mc.currentScreen;
    byte[] preferences = Files.readAllBytes(PowerConfig.path());
    var mapSettings = MapConfig.copy();
    Object context = field(CommandPermissions.class, "context").get(null);
    var rules = new RuleSet();
    try {
      CommandPermissions.context(() -> new CommandPermissions.Context("waypoint-validation", false, rules));
      var map = MapConfig.copy(); map.worldMap = true; MapConfig.preview(map);
      for (int[] size : new int[][]{{320, 180}, {640, 360}}) {
        test("waypoint rows use padded eye targets and permission-aware context actions at " + size[0], () -> rows(mc, size, rules));
        test("waypoint set dropdown, shared New set dialog and abbreviation remain isolated until Done at " + size[0], () -> editor(mc, size));
      }
      test("waypoint aliases share help, completion, CRUD and teleport permission without touching legacy warps", () -> commands(mc, rules));
    } finally {
      CommandPermissions.context((Supplier<CommandPermissions.Context>)context);
      MapConfig.preview(mapSettings);
      mc.setScreen(previous);
    }
    test("waypoint polish fixture leaves global preferences unchanged", () ->
        check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "waypoint fixture wrote global settings"));
    log("RELEASE300 WAYPOINT-POLISH failures=" + failures);
  }
  private static Object get(Object target, String name) throws Exception { return Editor230Checks.get(target, name); }
  private static Object call(Object target, String name, Class<?>[] types, Object... args) throws Exception { return Editor230Checks.invoke(target, name, types, args); }
  private static Object call(Object target, String name) throws Exception { return call(target, name, new Class<?>[0]); }
  private static int number(Object target, String name) throws Exception { return ((Number)call(target, name)).intValue(); }
  private static Field field(Class<?> type, String name) throws Exception {
    for (; type != null; type = type.getSuperclass()) try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
    catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  private static void set(Object target, String name, Object value) throws Exception { field(target.getClass(), name).set(target, value); }
  private static void click(Screen screen, int x, int y, int button) { ((ScreenInput)(Object)screen).power$click(x, y, button); }
  private static void key(Screen screen, int key) { ((ScreenInput)(Object)screen).power$key((char)0, key); }
  private static void open(Minecraft mc, Screen screen, int[] size) { mc.setScreen(screen); screen.init(mc, size[0], size[1]); }
  private static void draw(Minecraft mc, Screen screen) {
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight); GL11.glDisable(GL11.GL_SCISSOR_TEST);
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity(); GL11.glOrtho(0, screen.width, screen.height, 0, 1000, 3000);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity(); GL11.glTranslatef(0, 0, -2000); screen.render(-1, -1, 0);
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(mode);
    }
  }
  private static final class Points implements AutoCloseable {
    final WaypointStore store = Maps.waypoints();
    final Object data, points, destination; final long revision, pointsRevision;
    Points() throws Exception {
      check(store != null && store.ready() && !store.needsMaintenance(), "wait for loaded, idle waypoints before polish checks");
      data = get(store, "data"); revision = ((Number)get(store, "revision")).longValue();
      points = field(Maps.class, "points").get(null); destination = field(Maps.class, "destination").get(null);
      pointsRevision = field(Maps.class, "pointsRevision").getLong(null);
      set(store, "data", new WaypointStore.Data());
    }
    Waypoint point(String name) {
      Waypoint point = MapKeys.newWaypoint(); point.name = name; store.put(point); return point;
    }
    Waypoint current(String id) { return store.points().stream().filter(p -> p.id.equals(id)).findFirst().orElseThrow(); }
    public void close() throws Exception {
      set(store, "data", data); set(store, "revision", revision);
      field(Maps.class, "points").set(null, points); field(Maps.class, "destination").set(null, destination);
      field(Maps.class, "pointsRevision").setLong(null, pointsRevision);
    }
  }
  private static List<String> actions(WaypointsScreen screen) throws Exception { return (List<String>)call(screen, "contextActions"); }
  private static void context(WaypointsScreen screen, String action) throws Exception {
    List<String> actions = actions(screen); int index = actions.indexOf(action); check(index >= 0, "missing row context action " + action);
    set(screen, "contextScroll", Math.max(0, index - number(screen, "menuRows") + 1)); call(screen, "fitMenu");
    int scroll = ((Number)get(screen, "contextScroll")).intValue();
    click(screen, ((Number)get(screen, "contextX")).intValue() + 8, ((Number)get(screen, "contextY")).intValue() + (index - scroll) * 18 + 8, 0);
  }
  private static void rowMenu(WaypointsScreen screen) throws Exception { click(screen, number(screen, "left") + 40, 84, 1); check((boolean)get(screen, "context"), "right row press failed to open context"); }
  private static void rows(Minecraft mc, int[] size, RuleSet rules) throws Exception {
    try (var points = new Points()) {
      rules.set(GameRule.ALLOW_TELEPORTATION, false);
      Waypoint point = points.point("Alpha home"), retained = points.point("Other point");
      var list = new WaypointsScreen(null); open(mc, list, size); draw(mc, list);
      int left = number(list, "left");
      check(number(list, "rowBottom") == list.height - 32, "bottom action row still takes list space");
      check(((Number)get(list, "sort")).intValue() == 0, "default Sort: Name changed");
      click(list, left + 3, 81, 0); check(points.current(point.id).enabled, "row padding toggled visibility");
      click(list, left + 14, 85, 0); check(!points.current(point.id).enabled, "eye hitbox did not toggle waypoint off"); draw(mc, list);
      click(list, left + 14, 85, 0); check(points.current(point.id).enabled, "closed eye could not enable waypoint");
      click(list, left + 14, 85, 1); check(points.current(point.id).enabled && (boolean)get(list, "context"), "right eye press changed visibility instead of opening context");
      check(!actions(list).contains("Teleport") && !actions(list).contains("Move to"), "denied teleport or nonexistent move target was offered");
      check(actions(list).containsAll(List.of("Edit waypoint", "Delete waypoint", "Copy coordinates", "Center here", "Set destination")), "row context lost map actions");
      key(list, Keyboard.KEY_ESCAPE); check(!(boolean)get(list, "context") && mc.currentScreen == list, "context Escape left the list");
      points.store.createSet("Builds"); points.store.select("Default"); rowMenu(list); context(list, "Move to");
      check((boolean)get(list, "moving") && actions(list).contains("Builds") && !actions(list).contains("Default"), "Move to submenu includes current set or loses target");
      context(list, "Builds"); check(points.current(point.id).set.equals("Builds") && points.current(retained.id).set.equals("Default"), "Move to changed the wrong point");
      points.store.move(point.id, "Default"); rowMenu(list); rules.set(GameRule.ALLOW_TELEPORTATION, true);
      check(actions(list).contains("Teleport"), "granted teleport action stayed hidden"); rules.set(GameRule.ALLOW_TELEPORTATION, false);
      context(list, "Center here"); check(mc.currentScreen instanceof WorldMapScreen, "Center here failed to open World Map");
      var view = (MapMath.View)get(mc.currentScreen, "view"); check(view.x == point.x + .5 && view.z == point.z + .5, "Center here missed selected waypoint");
      open(mc, list, size); rowMenu(list); context(list, "Delete waypoint");
      check(points.store.contains(point.id), "first Delete removed point before confirmation");
      set(list, "deleteUntil", System.nanoTime() - 1); check(actions(list).contains("Delete waypoint"), "expired confirmation remained active");
      context(list, "Delete waypoint"); context(list, "Confirm delete?");
      check(!points.store.contains(point.id) && points.store.contains(retained.id), "confirmed Delete removed another point");
      draw(mc, list);
    }
  }
  private static List<TextInput> fields(WaypointEditorScreen editor) throws Exception { return (List<TextInput>)get(editor, "fields"); }
  private static void editor(Minecraft mc, int[] size) throws Exception {
    try (var points = new Points()) {
      points.store.createSet("Builds"); points.store.select("Default"); Waypoint point = points.point("Home");
      var parent = new WaypointsScreen(null); open(mc, parent, size);
      var editor = new WaypointEditorScreen(parent, point); open(mc, editor, size); draw(mc, editor);
      click(editor, number(editor, "fieldLeft", 1) + 8, number(editor, "row", 1) + 8, 0);
      check((boolean)get(editor, "selectingSet"), "editor Set is not a dropdown");
      List<String> sets = (List<String>)call(editor, "sets"); int builds = sets.indexOf("Builds");
      click(editor, number(editor, "fieldLeft", 1) + 8, number(editor, "setTop") + builds * 20 + 8, 0);
      check(fields(editor).get(1).text().equals("Builds") && points.current(point.id).set.equals("Default"), "existing set selection escaped the editor draft");
      click(editor, number(editor, "fieldLeft", 1) + 8, number(editor, "row", 1) + 8, 0);
      int count = ((List<?>)call(editor, "sets")).size(); set(editor, "setScroll", Math.max(0, count + 1 - number(editor, "setRows")));
      click(editor, number(editor, "fieldLeft", 1) + 8, number(editor, "setTop") + (count - ((Number)get(editor, "setScroll")).intValue()) * 20 + 8, 0);
      Object dialog = get(editor, "setDialog"); check((boolean)call(dialog, "active"), "New set option failed to reuse set-name dialog");
      ((TextInput)get(dialog, "input")).setText("Private draft set"); key(editor, Keyboard.KEY_RETURN);
      check(fields(editor).get(1).text().equals("Private draft set") && !points.store.sets().contains("Private draft set"), "new editor set was published before Done");
      fields(editor).get(7).setText("HB");
      var sharedColors = Catalog.JSON.toJsonTree(MenuPreferences.current().colours);
      set(editor, "scroll", Math.max(0, 32 + 6 * 25 + 18 - (editor.height - 43)));
      draw(mc, editor);
      click(editor, number(editor, "left") + 55, number(editor, "row", 6) + 8, 0);
      check(mc.currentScreen instanceof ColourScreen, "waypoint color swatch did not open shared picker");
      var picker = (ColourScreen)mc.currentScreen;
      var colors = (List<String>)get(picker, "colors"); colors.clear(); colors.add("#13579B");
      key(picker, Keyboard.KEY_RETURN);
      check(mc.currentScreen == editor && sharedColors.equals(Catalog.JSON.toJsonTree(MenuPreferences.current().colours)), "nested picker published swatches before waypoint Done");
      click(editor, number(editor, "left") + number(editor, "span") * 3 / 4, editor.height - 16, 0);
      check((boolean)call(get(editor, "exit"), "active"), "dirty waypoint Cancel missed prompt");
      click(editor, editor.width / 2, editor.height / 2 + 18, 0);
      check(mc.currentScreen == parent && points.current(point.id).abbreviation.equals("") && !points.store.sets().contains("Private draft set")
          && sharedColors.equals(Catalog.JSON.toJsonTree(MenuPreferences.current().colours)), "Discard published abbreviation/new set/swatches");
      editor = new WaypointEditorScreen(parent, point); open(mc, editor, size);
      fields(editor).get(7).setText("HB");
      call(editor, "chooseSet", new Class<?>[]{int.class}, ((List<?>)call(editor, "sets")).size());
      dialog = get(editor, "setDialog"); ((TextInput)get(dialog, "input")).setText("Accepted set"); key(editor, Keyboard.KEY_RETURN);
      click(editor, number(editor, "left") + number(editor, "span") / 4, editor.height - 16, 0);
      Waypoint saved = points.current(point.id);
      check(mc.currentScreen == parent && saved.abbreviation.equals("HB") && saved.set.equals("Accepted set") && saved.id.equals(point.id), "Done lost abbreviation/set or changed waypoint identity");
      points.store.remove(point.id); check(points.store.sets().contains("Accepted set"), "accepted editor set disappeared after its last waypoint was removed");
    }
  }
  private static int number(Object target, String name, int argument) throws Exception { return ((Number)call(target, name, new Class<?>[]{int.class}, argument)).intValue(); }
  private static void commands(Minecraft mc, RuleSet rules) throws Exception {
    try (var points = new Points()) {
      String legacy = ((PlayerWarps)mc.player).spc$getWarpString();
      ((PlayerWarps)mc.player).spc$setWarpString("legacy-kept 1.25 80.0 3.5 ");
      try {
        rules.set(GameRule.ALLOW_TELEPORTATION, false);
        List<String> output = new ArrayList<>();
        var source = new SharedCommandSource(mc.player) { @Override public void sendFeedback(String text) { output.add(text); } };
        var listed = new Help().lines(source);
        for (String alias : List.of("waypoint", "way", "wp")) {
          check(listed.stream().anyMatch(line -> line.startsWith("/" + alias + " ")), "help lost /" + alias);
          Command command = RetroChatUtil.commands.stream().filter(c -> c.name().equals(alias)).findFirst().orElseThrow();
          check(Arrays.asList(command.suggestion(source, 1, "", "/" + alias + " ")).containsAll(List.of("add", "remove", "list", "help")), "safe completion missing for " + alias);
          check(!Arrays.asList(command.suggestion(source, 1, "", "/" + alias + " ")).contains("teleport"), "denied teleport completion visible");
          var help = ChatChecks.record(() -> check(RetroChatUtil.handleCommand(source, alias + " help", true), "alias help dispatch failed"));
          check(help.stream().anyMatch(line -> line.contains("/waypoint add")), "alias help did not explain waypoint syntax");
          check(RetroChatUtil.handleCommand(source, alias + " add " + alias + " fixture", true), "alias add dispatch failed");
          check(WaypointCommands.find(points.store, alias + " fixture") != null, "alias add did not reach shared map store");
        }
        var listedPoints = ChatChecks.record(() -> RetroChatUtil.handleCommand(source, "wp list", true));
        check(listedPoints.stream().anyMatch(line -> line.contains("waypoint fixture")) && points.store.points().size() == 3, "list alias did not show shared saved points");
        check(RetroChatUtil.commands.stream().noneMatch(c -> c.name().equals("warp")), "retired warp remains registered");
        check(listed.stream().noneMatch(line -> line.startsWith("/warp ")), "retired warp remains in help");
        Command command = RetroChatUtil.commands.stream().filter(c -> c.name().equals("waypoint")).findFirst().orElseThrow();
        check(Arrays.asList(command.suggestion(source, 2, "way", "/wp remove way")).contains("point fixture"), "waypoint name completion does not share stored names");
        double x = mc.player.x, y = mc.player.y, z = mc.player.z; output.clear();
        RetroChatUtil.handleCommand(source, "wp teleport waypoint fixture", true);
        check(output.stream().anyMatch(line -> line.startsWith("§c")) && mc.player.x == x && mc.player.y == y && mc.player.z == z, "denied waypoint travel changed player");
        rules.set(GameRule.ALLOW_TELEPORTATION, true);
        check(Arrays.asList(command.suggestion(source, 1, "", "/waypoint ")).contains("teleport"), "granted teleport completion stayed hidden");
        rules.set(GameRule.ALLOW_TELEPORTATION, false);
        for (String alias : List.of("waypoint", "way", "wp")) RetroChatUtil.handleCommand(source, alias + " remove " + alias + " fixture", true);
        check(points.store.points().isEmpty(), "remove aliases left saved points");
        check(((PlayerWarps)mc.player).spc$getWarpString().equals("legacy-kept 1.25 80.0 3.5 "), "waypoint commands changed legacy warp data");
      } finally { ((PlayerWarps)mc.player).spc$setWarpString(legacy); }
    }
  }
}
