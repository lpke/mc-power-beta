package local.luke.power.worldedit.core;

import local.luke.power.worldedit.chat.*;
import local.luke.power.rules.*;
import local.luke.power.permissions.CommandPermissions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommandAccessTest {
  @Test void detachedCommandsSharePermissionsHelpCompletionAndExecution() {
    RuleSet rules = RuleSet.cheats();
    CommandPermissions.context(() -> new CommandPermissions.Context("world", false, rules));
    local.luke.power.input.DetachedCamera.register(() -> true);
    try {
      for (String name : CommandCatalog.COMMANDS)
        assertTrue(CommandAccess.allowed(name), name);
      var complete = CommandCatalog.complete("//");
      assertTrue(complete.contains("//pos1")); assertTrue(complete.contains("//pos2"));
      assertTrue(complete.contains("//hpos1")); assertTrue(complete.contains("//chunk"));
      assertTrue(complete.contains("//set")); assertTrue(complete.contains("//wand"));
      var help = new java.util.ArrayList<String>();
      CommandHelp.index(1, true, CommandAccess::allowed, help::add);
      assertTrue(help.stream().anyMatch(s -> s.replaceAll("§.", "").contains("//pos1 ")));
      assertTrue(help.stream().anyMatch(s -> s.replaceAll("§.", "").contains("//set ")));
    } finally {
      local.luke.power.input.DetachedCamera.register(() -> false);
      CommandPermissions.context(() -> new CommandPermissions.Context("", false, new RuleSet()));
    }
  }
  @Test void everyCommandAndAliasHasHelpAndAnExplicitPermissionOwner() {
    for (String name : CommandCatalog.COMMANDS) {
      assertNotNull(CommandHelp.line(name), name);
      if (CommandAccess.owner(name) == null) assertTrue(java.util.Set.of("calc", "help", "pos1", "pos2", "hpos1", "hpos2", "sel", "chunk", "size", "drawsel", "expand", "contract", "shift", "outset", "inset", "gmask", "count", "distr", "copy", "clearclipboard", "rotate", "flip", "clearhistory", "cancel", "status", "toggleplace", "toggleeditwand", "countentities").contains(CommandAccess.canonical(name)), name);
    }
    assertEquals(GameRule.ALLOW_WORLD_EDITING, CommandAccess.owner("future-command"));
    assertEquals(GameRule.ALLOW_TELEPORTATION, CommandAccess.owner("j"));
    assertEquals(GameRule.ALLOW_ENTITY_CONTROL, CommandAccess.owner("rem"));
  }
  @Test void helpAndCompletionObserveTheSameRulesAsExecution() {
    RuleSet rules = new RuleSet();
    CommandPermissions.context(() -> new CommandPermissions.Context("world", false, rules));
    assertFalse(CommandCatalog.complete("//").contains("//set"));
    assertTrue(CommandCatalog.complete("//").contains("//copy"));
    rules.set(GameRule.ALLOW_WORLD_EDITING, true);
    assertTrue(CommandCatalog.complete("//").contains("//set"));
    assertFalse(CommandCatalog.complete("/").contains("/remove"));
    rules.set(GameRule.ALLOW_WORLD_EDITING, false); rules.set(GameRule.ALLOW_ENTITY_CONTROL, true);
    assertTrue(CommandCatalog.complete("/").contains("/remove"));
    var lines = new java.util.ArrayList<String>();
    CommandHelp.index(1, true, CommandAccess::allowed, lines::add);
    assertTrue(lines.stream().anyMatch(s -> s.replaceAll("§.", "").contains("//remove ")));
    assertFalse(lines.stream().anyMatch(s -> s.replaceAll("§.", "").contains("//set ")));
  }
}
