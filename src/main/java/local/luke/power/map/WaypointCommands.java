package local.luke.power.map;

import java.lang.reflect.Proxy;
import java.util.*;
import local.luke.power.chat.HelpOutput;
import local.luke.power.input.ReportedPosition;
import local.luke.power.permissions.CommandPermissions;
import local.luke.power.rules.GameRule;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** The map store owns saved positions; legacy player warp NBT is never read or rewritten here. */
public final class WaypointCommands {
  private WaypointCommands() {}

  public static void register() {
    try {
      Class<?> api = Class.forName("local.luke.power.commands.api.Command");
      for (String name : List.of("waypoint", "way", "wp")) {
        Object command = Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, (proxy, method, args) -> {
          Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
          return switch (method.getName()) {
            case "name" -> name;
            case "needsPermissions", "disableInSingleplayer" -> false;
            case "helpGroup" -> "";
            case "available" -> mc.world != null && mc.player != null;
            case "command" -> { execute(mc, (String[])args[1]); yield null; }
            case "manual" -> { help(mc); yield null; }
            case "suggestion" -> suggestions(mc, (Integer)args[1], (String)args[2], (String)args[3]);
            case "toString" -> "Waypoint command /" + name;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException(method.getName());
          };
        });
        Class.forName("local.luke.power.commands.api.CommandRegistry").getMethod("add", api).invoke(null, command);
      }
    } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Could not register /waypoint", failure); }
  }

  public static void execute(Minecraft mc, String[] args) {
    if (mc.world == null || mc.player == null) throw new IllegalArgumentException("Open a world first.");
    if (args.length < 2 || args[1].equalsIgnoreCase("help")) { help(mc); return; }
    String action = args[1].toLowerCase(Locale.ROOT);
    if (!List.of("add", "remove", "list", "dest", "destination", "teleport", "tp").contains(action))
      throw new IllegalArgumentException("Use /waypoint add, remove, list, destination or teleport. Aliases: /way and /wp.");
    if (action.equals("dest") || action.equals("destination")) {
      Waypoint point = destinationPoint(MapKeys.newWaypoint(), args, WaypointCoordinates.current(mc));
      Maps.destination(point);
      chat(mc, "§aDestination set to §f" + point.coordinates() + "§a.");
      return;
    }
    if (action.equals("teleport") || action.equals("tp")) {
      String denial = CommandPermissions.denial(GameRule.ALLOW_TELEPORTATION, CommandPermissions.context());
      if (!denial.isEmpty()) throw new IllegalArgumentException(denial);
      if (mc.world.isRemote) throw new IllegalArgumentException("Waypoint teleportation is available only in singleplayer.");
    }
    var existing = Maps.waypoints();
    var store = existing == null ? Maps.ensureWaypoints(mc) : existing;
    if (store == null || !store.ready()) throw new IllegalArgumentException("Waypoints are loading or protected after a storage error. Try again after loading finishes.");
    if (action.equals("list")) {
      if (args.length > 3) throw new IllegalArgumentException("Use /waypoint list [page].");
      var points = visible(store);
      var lines = points.stream().sorted(Comparator.comparing(p -> p.name.toLowerCase(Locale.ROOT)))
          .map(WaypointCommands::listEntry).toList();
      int pages = Math.max(1, (lines.size() + 5) / 6), page = 1;
      if (args.length == 3) try { page = Integer.parseInt(args[2]); }
      catch (NumberFormatException failure) { throw new IllegalArgumentException("Choose a waypoint list page from 1 to " + pages + "."); }
      if (page < 1 || page > pages) throw new IllegalArgumentException("Choose a waypoint list page from 1 to " + pages + ".");
      HelpOutput.print(line -> chat(mc, line), "Waypoints", lines, page, 6, args.length < 3 && HelpOutput.scrolling());
      if (lines.isEmpty()) chat(mc, "No waypoints in this view. Use /waypoint add <name>.");
      return;
    }
    if (action.equals("add")) {
      Waypoint point = addPoint(MapKeys.newWaypoint(), args, WaypointCoordinates.current(mc));
      if (store.points().stream().anyMatch(p -> p.set.equals(store.selectedSet()) && p.name.equalsIgnoreCase(point.name)))
        throw new IllegalArgumentException("That waypoint name already exists in this set. Choose another name.");
      point.set = store.selectedSet(); store.put(point);
      chat(mc, "§aAdded waypoint " + point.name + "."); return;
    }
    if (args.length < 3) throw new IllegalArgumentException("Use /waypoint " + action + " <name or ID>.");
    String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).strip();
    Waypoint point = find(store, name);
    if (action.equals("remove")) { store.remove(point.id); chat(mc, "§aRemoved waypoint " + point.name + "."); return; }
    if (!Maps.canTeleport(mc, point)) throw new IllegalArgumentException("Waypoint teleportation requires the same dimension and Allow teleportation.");
    Maps.teleport(mc, point); chat(mc, "§aTeleported to " + point.name + ".");
  }

  static String listEntry(Waypoint point) {
    return "§b" + point.name + " §f" + point.coordinates() + " §7· " + MapDimensions.name(point.dimension)
        + " · " + point.set + " §8[" + point.id + "]";
  }

  static Waypoint destinationPoint(Waypoint current, String[] args) {
    return destinationPoint(current, args, origin(current));
  }

  static Waypoint destinationPoint(Waypoint current, String[] args, ReportedPosition.Pose origin) {
    if (args.length != 2 && args.length != 5)
      throw new IllegalArgumentException("Use /waypoint destination [x y z]. Alias: /waypoint dest.");
    Waypoint point = args.length == 5 ? coordinates(current, args, 2, origin) : current.copy();
    point.validate();
    return point;
  }

  static Waypoint addPoint(Waypoint current, String[] args) {
    return addPoint(current, args, origin(current));
  }

  static Waypoint addPoint(Waypoint current, String[] args, ReportedPosition.Pose origin) {
    if (args.length < 3) throw new IllegalArgumentException("Use /waypoint add <name> [x y z].");
    int end = args.length;
    boolean explicit = end >= 6 && WaypointCoordinates.candidate(args[end - 3])
        && WaypointCoordinates.candidate(args[end - 2]) && WaypointCoordinates.candidate(args[end - 1]);
    if (!explicit && Arrays.stream(args, 3, end).anyMatch(value -> value.startsWith("~")))
      throw new IllegalArgumentException("Specify all three waypoint coordinates: x y z. Use ~ for the current coordinate.");
    if (explicit) end -= 3;
    Waypoint point = explicit ? coordinates(current, args, end, origin) : current.copy();
    point.name = String.join(" ", Arrays.copyOfRange(args, 2, end)).strip();
    point.validate();
    return point;
  }

  private static ReportedPosition.Pose origin(Waypoint current) {
    return new ReportedPosition.Pose(current.x, current.y, current.z, 0, 0);
  }

  private static Waypoint coordinates(Waypoint current, String[] args, int start, ReportedPosition.Pose origin) {
    return WaypointCoordinates.resolve(current, args[start], args[start + 1], args[start + 2], origin);
  }

  public static Waypoint find(WaypointStore store, String text) {
    var exact = store.points().stream().filter(p -> p.id.equals(text)).findFirst();
    if (exact.isPresent()) return exact.get();
    var matches = visible(store).stream().filter(p -> p.name.equalsIgnoreCase(text)).toList();
    if (matches.size() > 1) throw new IllegalArgumentException("More than one waypoint has that name. Use its ID from /waypoint list.");
    if (matches.isEmpty()) throw new IllegalArgumentException("Waypoint not found: " + text + ". Use /waypoint list.");
    return matches.get(0);
  }
  private static List<Waypoint> visible(WaypointStore store) {
    return store.points().stream().filter(p -> store.allSets() || p.set.equals(store.selectedSet())).toList();
  }
  public static String[] suggestions(Minecraft mc, int index, String prefix, String total) {
    var choices = new ArrayList<String>();
    String[] words = total.stripLeading().split("\\s+");
    if (words.length > 1 && (words[1].equalsIgnoreCase("add") && index >= 3
        || (words[1].equalsIgnoreCase("dest") || words[1].equalsIgnoreCase("destination")) && index >= 2 && index <= 4))
      choices.add("~");
    if (index == 1) {
      choices.addAll(List.of("add", "remove", "list", "dest", "destination", "help"));
      if (mc.world != null && !mc.world.isRemote && CommandPermissions.allowed(GameRule.ALLOW_TELEPORTATION)) choices.addAll(List.of("teleport", "tp"));
    } else if (index >= 2 && Maps.waypoints() != null && Maps.waypoints().ready()) {
      if (words.length > 1 && (words[1].equalsIgnoreCase("remove") || (words[1].equalsIgnoreCase("teleport") || words[1].equalsIgnoreCase("tp"))
          && mc.world != null && !mc.world.isRemote && CommandPermissions.allowed(GameRule.ALLOW_TELEPORTATION))) {
        String earlier = index <= 2 ? "" : String.join(" ", Arrays.copyOfRange(words, 2, Math.min(index, words.length))) + " ";
        for (Waypoint point : visible(Maps.waypoints())) if (point.name.regionMatches(true, 0, earlier, 0, earlier.length())) choices.add(point.name.substring(earlier.length()));
        if (index == 2) Maps.waypoints().points().forEach(p -> choices.add(p.id));
      }
    }
    return choices.stream().distinct().filter(v -> v.regionMatches(true, 0, prefix, 0, prefix.length())).sorted().map(v -> v.substring(prefix.length())).toArray(String[]::new);
  }
  private static void help(Minecraft mc) {
    helpLines(canTeleport(mc)).forEach(line -> chat(mc, line));
  }
  static List<String> helpLines(boolean teleport) {
    var lines = new ArrayList<>(List.of(
        "§b/waypoint add <name> [x y z] §7Save whole coordinates, or the current reported position, in the selected set.",
        "§7Names may contain spaces; a final three-coordinate group sets X Y Z.",
        "§7Use ~ for the current reported coordinate, or ~offset to add an offset. Relative results round down.",
        "§b/waypoint remove <name or ID> §7Remove a saved waypoint.",
        "§b/waypoint list [page] §7List waypoints in the current set view, including their IDs.",
        "§b/waypoint destination [x y z] §7Set a temporary destination at coordinates, or at the current reported position. Alias: dest."));
    if (teleport) lines.add("§b/waypoint teleport <name or ID> §7Travel within this dimension.");
    lines.add("§7Aliases: /way and /wp.");
    return List.copyOf(lines);
  }
  private static boolean canTeleport(Minecraft mc) {
    return mc.world != null && !mc.world.isRemote && CommandPermissions.allowed(GameRule.ALLOW_TELEPORTATION);
  }
  private static void chat(Minecraft mc, String line) { mc.inGameHud.addChatMessage(line + "§r"); }
}
