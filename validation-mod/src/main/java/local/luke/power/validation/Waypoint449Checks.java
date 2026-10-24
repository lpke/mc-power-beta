package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.api.PlayerWarps;
import local.luke.power.commands.util.*;
import local.luke.power.input.ReportedPosition;
import local.luke.power.map.*;
import local.luke.power.mixin.CullingChunkCacheAccessor;
import local.luke.power.permissions.CommandPermissions;
import local.luke.power.rules.RuleSet;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

/** Relative command/editor checks keep every waypoint mutation in a restored in-memory store. */
final class Waypoint449Checks {
  private Waypoint449Checks() {}

  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null, "open a disposable singleplayer world first");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    WaypointStore store = Maps.waypoints();
    check(store != null && store.ready() && !store.needsMaintenance(), "wait for loaded, idle waypoints first");
    failures = 0;
    Object data = field(WaypointStore.class, "data").get(store);
    long revision = store.revision();
    Object permissions = field(CommandPermissions.class, "context").get(null);
    Object reportedOwner = field(ReportedPosition.class, "owner").get(null);
    Object reportedSource = field(ReportedPosition.class, "source").get(null);
    Object destination = field(Maps.class, "destination").get(null);
    boolean arrival = field(Maps.class, "destinationArrivalArmed").getBoolean(null);
    var screen = mc.currentScreen;
    String legacy = ((PlayerWarps)mc.player).spc$getWarpString();
    byte[] preferences = Files.readAllBytes(PowerConfig.path());
    var chunks = (CullingChunkCacheAccessor)mc.world.method_259();
    var loaded = new HashSet<>(chunks.power$loadedChunks().keySet());
    double x = mc.player.x, y = mc.player.y, z = mc.player.z;
    var pose = new ReportedPosition.Pose[]{new ReportedPosition.Pose(-16.25, 83.75, 29.5, 0, 0)};
    int[] samples = {0};
    try {
      field(WaypointStore.class, "data").set(store, new WaypointStore.Data());
      store.createSet("Relative coordinate fixtures");
      CommandPermissions.context(() -> new CommandPermissions.Context("waypoint-449", false, new RuleSet()));
      ReportedPosition.register(() -> mc.player, () -> { samples[0]++; return pose[0]; });

      test("WAYPOINT449 all aliases resolve relative add and destination coordinates without teleport permission", () -> {
        for (String alias : List.of("waypoint", "way", "wp")) {
          accepted(mc, alias + " add " + alias + " Example Waypoint ~100 ~ 100");
          var point = WaypointCommands.find(store, alias + " Example Waypoint");
          check(point.coordinates().equals("83 83 100") && point.dimension == mc.world.dimension.id
              && point.set.equals("Relative coordinate fixtures"), "relative add lost position, name, dimension or set");
          for (String action : List.of("dest", "destination")) {
            accepted(mc, alias + " " + action + " ~.5 ~-.8 ~-30");
            check(destination().coordinates().equals("-16 82 -1"), "destination did not floor after adding offsets");
          }
        }
      });

      test("WAYPOINT449 relative origins remain unbounded until offsets resolve", () -> {
        pose[0] = new ReportedPosition.Pose(-32000100.25, 1000.75, 32000100.5, 0, 0);
        accepted(mc, "wp add Beyond height ~101 ~-900 ~-101");
        check(WaypointCommands.find(store, "Beyond height").coordinates().equals("-32000000 100 31999999"),
            "relative origin was clamped before resolution");
        accepted(mc, "way destination 10 127 -20");
        check(destination().coordinates().equals("10 127 -20"), "explicit coordinates changed with reported pose");
        pose[0] = new ReportedPosition.Pose(-16.25, 83.75, 29.5, 0, 0);
      });

      test("WAYPOINT449 malformed, incomplete and out-of-bounds coordinates never write", () -> {
        for (String suffix : List.of("~", "~ ~", "~ ~ ~oops", "~ foo 10", "~NaN ~ ~", "~Infinity ~ ~",
            "~1e309 ~ ~", "~32000100 ~ ~", "~ ~128 ~", "~ ~ ~-32000100")) {
          long before = store.revision(); int count = store.points().size();
          var previous = destination();
          for (String command : List.of("wp add Invalid waypoint " + suffix, "way dest " + suffix))
            check(command(mc, command).stream().anyMatch(line -> line.startsWith("§c")), "invalid input accepted: " + command);
          check(store.revision() == before && store.points().size() == count && destination() == previous,
              "invalid coordinates changed waypoints or destination");
        }
      });

      test("WAYPOINT449 help and coordinate completion explain relatives through every alias", () -> {
        long before = store.revision(); var previous = destination();
        for (String alias : List.of("waypoint", "way", "wp")) {
          for (String help : List.of(alias, alias + " help", "help " + alias))
            check(accepted(mc, help).stream().anyMatch(line -> line.contains("~offset")), "help omitted relative coordinates");
          Command command = RetroChatUtil.commands.stream().filter(c -> c.name().equals(alias)).findFirst().orElseThrow();
          for (String action : List.of("add", "dest", "destination")) {
            int index = action.equals("add") ? 3 : 2;
            String text = "/" + alias + " " + action + (action.equals("add") ? " Name " : " ");
            check(Arrays.asList(command.suggestion(new SharedCommandSource(mc.player), index, "", text)).contains("~"),
                "completion omitted relative coordinate for " + text);
          }
        }
        check(store.revision() == before && destination() == previous, "help/completion changed map state");
      });

      test("WAYPOINT449 editor resolves one fresh reported pose on Done and preserves the draft until then", () -> {
        pose[0] = new ReportedPosition.Pose(1.25, 64.75, -1.25, 0, 0);
        var initial = MapKeys.newWaypoint(); initial.name = "Editor relative point";
        store.put(initial); long before = store.revision();
        var editor = open(mc, initial); var fields = fields(editor);
        fields.get(2).setText("~.5"); fields.get(3).setText("~-900"); fields.get(4).setText("~-30");
        check(store.revision() == before && WaypointCommands.find(store, initial.name).coordinates().equals(initial.coordinates()),
            "typing relative coordinates escaped the editor draft");
        pose[0] = new ReportedPosition.Pose(-16.25, 1000.75, 29.5, 0, 0); samples[0] = 0;
        key(editor, Keyboard.KEY_RETURN);
        check(samples[0] == 1, "editor did not capture exactly one commit-time reported pose");
        var point = WaypointCommands.find(store, initial.name);
        check(point.coordinates().equals("-16 100 -1") && point.id.equals(initial.id), "Done used the old or clamped origin");
        check(mc.currentScreen == null, "successful Done did not leave the editor");
      });

      test("WAYPOINT449 invalid editor input retains its fields and leaves points, sets and swatches untouched", () -> {
        var initial = WaypointCommands.find(store, "Editor relative point"); var editor = open(mc, initial);
        var fields = fields(editor); fields.get(1).setText("Unaccepted draft set");
        fields.get(2).setText("~"); fields.get(3).setText("~oops"); fields.get(4).setText("~");
        var colors = (local.luke.power.config.Setting)field(WaypointEditorScreen.class, "savedColors").get(editor);
        var draftColors = new com.google.gson.JsonArray(); draftColors.add("#13579B"); colors.value = draftColors;
        var sharedColors = local.luke.power.config.Catalog.JSON.toJsonTree(MenuPreferences.current().colours);
        long before = store.revision(); key(editor, Keyboard.KEY_RETURN);
        check(mc.currentScreen == editor && !((String)field(WaypointEditorScreen.class, "error").get(editor)).isEmpty(),
            "invalid editor coordinates did not retain the editor and error");
        check(fields.get(3).text().equals("~oops") && store.revision() == before && !store.sets().contains("Unaccepted draft set"),
            "invalid editor coordinates changed fields, waypoints or sets");
        check(sharedColors.equals(local.luke.power.config.Catalog.JSON.toJsonTree(MenuPreferences.current().colours)),
            "invalid coordinates published draft swatches");
        check(WaypointCommands.find(store, initial.name).coordinates().equals(initial.coordinates()), "invalid editor input changed saved position");
        key(editor, Keyboard.KEY_ESCAPE);
        editor.layerPress(editor.width / 2, editor.height / 2 + 18, 0);
        check(mc.currentScreen == null && store.revision() == before && !store.sets().contains("Unaccepted draft set"),
            "Discard changed the store or retained the editor");
        check(sharedColors.equals(local.luke.power.config.Catalog.JSON.toJsonTree(MenuPreferences.current().colours)),
            "Discard published draft swatches");
      });

      test("WAYPOINT449 commands and editor preserve player, terrain, preferences and legacy warps", () -> {
        check(mc.player.x == x && mc.player.y == y && mc.player.z == z, "waypoint coordinates moved the player");
        check(loaded.equals(chunks.power$loadedChunks().keySet()), "waypoint coordinates loaded terrain");
        check(Objects.equals(legacy, ((PlayerWarps)mc.player).spc$getWarpString()), "waypoint coordinates changed legacy warp NBT");
        check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "waypoint coordinates wrote global preferences");
      });
    } finally {
      field(WaypointStore.class, "data").set(store, data);
      field(WaypointStore.class, "revision").setLong(store, revision);
      field(CommandPermissions.class, "context").set(null, permissions);
      field(ReportedPosition.class, "owner").set(null, reportedOwner);
      field(ReportedPosition.class, "source").set(null, reportedSource);
      field(Maps.class, "destination").set(null, destination);
      field(Maps.class, "destinationArrivalArmed").setBoolean(null, arrival);
      mc.setScreen(screen);
    }
    log("WAYPOINT449 FAILURES " + failures);
    check(failures == 0, "waypoint 449 failures=" + failures);
  }

  private static WaypointEditorScreen open(Minecraft mc, Waypoint initial) {
    var editor = new WaypointEditorScreen(null, initial); mc.setScreen(editor); editor.init(mc, 360, 240); return editor;
  }
  @SuppressWarnings("unchecked")
  private static List<TextInput> fields(WaypointEditorScreen editor) throws Exception {
    return (List<TextInput>)field(WaypointEditorScreen.class, "fields").get(editor);
  }
  private static void key(WaypointEditorScreen screen, int key) {
    screen.layerKey('\0', key);
  }
  private static List<String> accepted(Minecraft mc, String text) throws Exception {
    var output = command(mc, text);
    check(output.stream().noneMatch(line -> line.startsWith("§c")), "command failed: " + text + " " + output);
    return output;
  }
  private static List<String> command(Minecraft mc, String text) throws Exception {
    List<String> output = new ArrayList<>();
    var source = new SharedCommandSource(mc.player) {
      @Override public void sendFeedback(String line) { output.add(line); }
    };
    output.addAll(ChatChecks.record(() -> check(RetroChatUtil.handleCommand(source, text, true), "unhandled command " + text)));
    return output;
  }
  private static Waypoint destination() throws Exception {
    return (Waypoint)field(Maps.class, "destination").get(null);
  }
  private static Field field(Class<?> type, String name) throws Exception {
    Field value = type.getDeclaredField(name); value.setAccessible(true); return value;
  }
}
