package local.luke.power.worldedit.chat;

import java.util.*;
import local.luke.power.permissions.CommandPermissions;
import local.luke.power.rules.GameRule;

/** Every catalog entry has a deliberate owner; new commands fail closed. */
public final class CommandAccess {
  private CommandAccess() {}
  public static String canonical(String name) {
    return switch (name.toLowerCase(Locale.ROOT).replaceFirst("^/+", "")) {
      case "asc" -> "ascend"; case "desc" -> "descend"; case "!" -> "unstuck";
      case "j" -> "jumpto"; case "rem", "rement" -> "remove"; case "re" -> "replace";
      case "desel", "deselect" -> "sel"; case "outline" -> "faces";
      case "we", "worldedit" -> "help"; default -> name.toLowerCase(Locale.ROOT).replaceFirst("^/+", "");
    };
  }
  public static GameRule owner(String name) {
    String command = canonical(name);
    if (Set.of("calc", "help", "pos1", "pos2", "hpos1", "hpos2", "sel", "chunk", "size", "drawsel",
        "expand", "contract", "shift", "outset", "inset", "gmask", "count", "distr", "copy",
        "clearclipboard", "rotate", "flip", "clearhistory", "cancel", "status", "toggleplace", "toggleeditwand", "countentities").contains(command)) return null;
    GameRule shared = CommandPermissions.owner(command);
    return shared == null ? GameRule.ALLOW_WORLD_EDITING : shared;
  }
  public static boolean allowed(String name) { GameRule owner = owner(name); return owner == null || CommandPermissions.allowed(owner); }
  public static String requirement(String name) {
    GameRule owner = owner(name);
    return owner == null ? "Available without an access rule." : "Requires " + owner.id() + ".";
  }
}
