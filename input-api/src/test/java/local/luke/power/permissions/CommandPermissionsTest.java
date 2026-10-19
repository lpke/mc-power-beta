package local.luke.power.permissions;

import java.util.*;
import local.luke.power.rules.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static local.luke.power.rules.GameRule.*;

class CommandPermissionsTest {
  private void world(RuleSet rules) { CommandPermissions.context(() -> new CommandPermissions.Context("world", false, rules)); }
  @Test void eachPermissionUnlocksOnlyItsOwnedActions() {
    List<String> commands = List.of("give", "summon", "kill", "killall", "ride", "heal", "god", "repair", "clear", "spawn", "tp", "waypoint", "spawnpoint", "setworldspawn", "time", "weather", "remove", "butcher", "up", "thru");
    for (GameRule enabled : GameRule.values()) {
      RuleSet rules = new RuleSet(); if (!enabled.access()) continue;
      rules.set(enabled, true); world(rules);
      for (String command : commands) assertEquals(enabled == CommandPermissions.owner(command), CommandPermissions.allowed(command), enabled + ": " + command);
    }
  }
  @Test void safeUtilitiesAndSubcommandsRemainAvailableWithoutAccess() {
    world(new RuleSet());
    for (String command : List.of("help", "hat", "difficulty", "death", "clock", "seed", "gamerule", "calc")) assertTrue(CommandPermissions.allowed(command), command);
    for (String text : List.of("time query daytime", "time get", "waypoint add home", "wp list", "way remove home", "waypoint dest", "wp destination 1 64 2", "way DEST 1 64 2", "clear @s stone 0", "repair help")) {
      String[] args = text.split(" "); assertTrue(CommandPermissions.denial(args[0], args).isEmpty(), text);
    }
    for (String text : List.of("time set day", "time add 5", "way teleport home", "clear", "clear @s stone 1", "repair")) {
      String[] args = text.split(" "); assertFalse(CommandPermissions.denial(args[0], args).isEmpty(), text);
    }
  }
  @Test void modesAreSeparateAndSurvivalIsAlwaysAvailable() {
    RuleSet rules = new RuleSet(); world(rules);
    assertTrue(CommandPermissions.modeDenial("survival").isEmpty());
    assertFalse(CommandPermissions.modeDenial("creative").isEmpty());
    rules.set(ALLOW_SPECTATOR_MODE, true);
    assertTrue(CommandPermissions.modeDenial("spectator").isEmpty());
    assertFalse(CommandPermissions.modeDenial("creative").isEmpty());
    assertFalse(CommandPermissions.modeDenial("bogus").isEmpty());
  }
  @Test void worldEditingCannotBypassEntityOrTeleportRules() {
    RuleSet rules = new RuleSet(); rules.set(ALLOW_WORLD_EDITING, true); world(rules);
    for (String command : List.of("remove", "rem", "rement", "butcher", "up", "ceil", "ascend", "descend", "unstuck", "thru", "jumpto")) assertFalse(CommandPermissions.allowed(command), command);
    rules.set(ALLOW_WORLD_EDITING, false); rules.set(ALLOW_ENTITY_CONTROL, true); rules.set(ALLOW_TELEPORTATION, true);
    assertTrue(CommandPermissions.allowed("remove")); assertTrue(CommandPermissions.allowed("up"));
  }
  @Test void multiplayerLeavesCommandsToTheServerAndLocalToolsStayUnavailable() {
    CommandPermissions.context(() -> new CommandPermissions.Context("", true, new RuleSet()));
    assertTrue(CommandPermissions.denial("give").isEmpty());
    assertFalse(CommandPermissions.allowed(ALLOW_WORLD_EDITING));
  }
  @Test void aliasesSharePolicyAndWorldChangesDoNotInheritPermissions() {
    world(RuleSet.cheats()); assertTrue(CommandPermissions.allowed("/teleport"));
    world(new RuleSet()); assertFalse(CommandPermissions.allowed("tp")); assertFalse(CommandPermissions.allowed("TELEPORT"));
    assertEquals("waypoint", CommandPermissions.canonical("way"));
    assertEquals("waypoint", CommandPermissions.canonical("wp"));
    assertEquals("gamemode", CommandPermissions.canonical("gm"));
    assertEquals("toggledownfall", CommandPermissions.canonical("weather"));
  }
}
