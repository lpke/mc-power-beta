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
import net.minecraft.client.Minecraft;

/** Native command dispatch with an unsaved shadow waypoint store and reported-position source. */
final class Waypoint448Checks {
  private Waypoint448Checks() {}

  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null, "open a disposable singleplayer world first");
    WaypointStore store = Maps.waypoints();
    check(store != null && store.ready() && !store.needsMaintenance(), "wait for loaded, idle waypoints first");
    failures = 0;
    Object data = field(WaypointStore.class, "data").get(store);
    long revision = store.revision();
    Object permissionContext = field(CommandPermissions.class, "context").get(null);
    Object reportedOwner = field(ReportedPosition.class, "owner").get(null);
    Object reportedSource = field(ReportedPosition.class, "source").get(null);
    Object destination = field(Maps.class, "destination").get(null);
    String legacy = ((PlayerWarps)mc.player).spc$getWarpString();
    byte[] preferences = Files.readAllBytes(PowerConfig.path());
    var chunks = (CullingChunkCacheAccessor)mc.world.method_259();
    var loaded = new HashSet<>(chunks.power$loadedChunks().keySet());
    double x = mc.player.x, y = mc.player.y, z = mc.player.z;
    try {
      field(WaypointStore.class, "data").set(store, new WaypointStore.Data());
      store.createSet("Coordinate fixtures");
      CommandPermissions.context(() -> new CommandPermissions.Context("waypoint-448", mc.world.isRemote, new RuleSet()));
      ReportedPosition.register(() -> mc.player, () -> new ReportedPosition.Pose(-16.25, 83.75, 29.5, 0, 0));

      test("WAYPOINT448 all aliases save explicit coordinates or current reported position without teleport permission", () -> {
        for (String alias : List.of("waypoint", "way", "wp")) {
          accepted(mc, alias + " add " + alias + " reported home");
          var current = WaypointCommands.find(store, alias + " reported home");
          check(current.coordinates().equals("-17 83 29") && current.dimension == mc.world.dimension.id
              && current.set.equals("Coordinate fixtures"), "omitted coordinates lost reported pose, dimension or selected set");
          accepted(mc, alias + " add " + alias + " distant base -32000000 127 32000000");
          var explicit = WaypointCommands.find(store, alias + " distant base");
          check(explicit.coordinates().equals("-32000000 127 32000000") && explicit.dimension == mc.world.dimension.id
              && explicit.set.equals("Coordinate fixtures"), "coordinate suffix lost name, bounds, dimension or set");
        }
        var list = accepted(mc, "wp list");
        check(list.stream().anyMatch(line -> line.contains("-32000000 127 32000000")), "list lost explicit waypoint coordinates");
      });

      test("WAYPOINT448 invalid coordinate suffixes and duplicate names leave the store unchanged", () -> {
        for (String command : List.of("wp add invalid 1.5 64 3", "wp add invalid NaN 64 3",
            "wp add invalid Infinity 64 3", "wp add invalid 1 128 3", "wp add invalid -32000001 64 3",
            "wp add invalid 1 64 -2147483648", "wp add WAYPOINT DISTANT BASE 1 64 3")) {
          long before = store.revision(); int count = store.points().size();
          check(command(mc, command).stream().anyMatch(line -> line.startsWith("§c")), "invalid add was accepted: " + command);
          check(store.revision() == before && store.points().size() == count, "invalid add changed saved points");
        }
      });

      test("WAYPOINT448 help and completion share safe actions and never add waypoints or destinations", () -> {
        long before = store.revision();
        for (String alias : List.of("waypoint", "way", "wp")) {
          for (String help : List.of(alias, alias + " help", "help " + alias)) {
            var output = accepted(mc, help);
            check(output.stream().anyMatch(line -> line.contains("/waypoint add <name> [x y z]")), "help lost optional coordinate syntax");
            check(output.stream().noneMatch(line -> line.contains("/waypoint teleport")), "denied teleport appeared in help");
          }
          Command command = RetroChatUtil.commands.stream().filter(c -> c.name().equals(alias)).findFirst().orElseThrow();
          var suggestions = Arrays.asList(command.suggestion(new SharedCommandSource(mc.player), 1, "", "/" + alias + " "));
          check(suggestions.containsAll(List.of("add", "remove", "list", "destination", "dest", "help"))
              && !suggestions.contains("teleport") && !suggestions.contains("tp"), "completion lost safe actions or exposed teleport");
        }
        check(store.revision() == before && field(Maps.class, "destination").get(null) == destination, "help/completion changed map state");
      });

      test("WAYPOINT448 local coordinate additions remain safe in multiplayer and preserve full-name removal", () -> {
        mc.world.isRemote = true;
        try {
          accepted(mc, "wp add Remote named home 15 64 -20");
          var point = WaypointCommands.find(store, "Remote named home");
          check(point.coordinates().equals("15 64 -20") && point.dimension == mc.world.dimension.id, "remote local marker changed coordinates or dimension");
          accepted(mc, "way remove Remote named home");
          check(!store.contains(point.id), "full-name remove failed");
        } finally { mc.world.isRemote = false; }
      });

      test("WAYPOINT448 commands never move the player, load terrain or rewrite legacy warps/preferences", () -> {
        check(mc.player.x == x && mc.player.y == y && mc.player.z == z, "saving waypoints moved the player");
        check(loaded.equals(chunks.power$loadedChunks().keySet()), "saving distant waypoints loaded terrain");
        check(Objects.equals(legacy, ((PlayerWarps)mc.player).spc$getWarpString()), "waypoint command changed legacy warp NBT");
        check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "waypoint command wrote global preferences");
      });
    } finally {
      mc.world.isRemote = false;
      field(WaypointStore.class, "data").set(store, data);
      field(WaypointStore.class, "revision").setLong(store, revision);
      field(CommandPermissions.class, "context").set(null, permissionContext);
      field(ReportedPosition.class, "owner").set(null, reportedOwner);
      field(ReportedPosition.class, "source").set(null, reportedSource);
    }
    log("WAYPOINT448 FAILURES " + failures);
    check(failures == 0, "waypoint 448 failures=" + failures);
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
    var chat = ChatChecks.record(() -> check(RetroChatUtil.handleCommand(source, text, true), "unhandled command " + text));
    output.addAll(chat);
    return output;
  }

  private static Field field(Class<?> type, String name) throws Exception {
    Field value = type.getDeclaredField(name); value.setAccessible(true); return value;
  }
}
