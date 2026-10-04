package local.luke.power.permissions;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import java.util.*;
import local.luke.power.permissions.CommandPermissions.*;
import org.junit.jupiter.api.Test;

class CommandPermissionsTest {
  private final Settings settings = new Settings();

  private Command command(String name) {
    return CommandPermissions.COMMANDS.stream()
        .filter(c -> c.name().equals(name))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void acceptsImmutablePermissionMaps() {
    settings.rules = Map.of("warp", Rule.DISABLED);
    settings.worlds = Map.of("world", Map.of("warp", CommandPermissions.Override.ALLOW));
    settings.modeAccess = Map.of("creative", true);
    settings.worldModeAccess =
        Map.of("world", Map.of("spectator", CommandPermissions.Override.BLOCK));
    assertDoesNotThrow(settings::validate);
  }

  @Test
  void everyCombinationOfWorldMasterCommandMasterRuleAndOverride() {
    for (boolean cheats : List.of(false, true))
      for (boolean master : List.of(false, true))
        for (Rule rule : Rule.values())
          for (CommandPermissions.Override override : CommandPermissions.Override.values())
            for (Command command : CommandPermissions.COMMANDS) {
              settings.enabled = master;
              settings.rules.put(command.name(), rule);
              settings.worlds.put("world", Map.of(command.name(), override));
              boolean expected =
                  (!command.cheat() || cheats && master)
                      && (override == CommandPermissions.Override.ALLOW
                          || override == CommandPermissions.Override.INHERIT
                              && rule == Rule.ALLOWED);
              assertEquals(
                  expected,
                  CommandPermissions.denial(settings, command, new Context("world", false, cheats))
                      .isEmpty(),
                  cheats + "/" + master + "/" + rule + "/" + override + "/" + command.name());
            }
  }

  @Test
  void independentWorldsAndModesWithNoSurvivalLockout() {
    settings.modeAccess.put("creative", false);
    settings.worldModeAccess.put("first", Map.of("creative", CommandPermissions.Override.ALLOW));
    assertTrue(
        CommandPermissions.modeDenial(settings, "creative", new Context("first", false, true))
            .isEmpty());
    assertFalse(
        CommandPermissions.modeDenial(settings, "creative", new Context("second", false, true))
            .isEmpty());
    assertTrue(
        CommandPermissions.modeDenial(settings, "spectator", new Context("second", false, true))
            .isEmpty());
    assertFalse(
        CommandPermissions.modeDenial(settings, "creative", new Context("first", false, false))
            .isEmpty());
    settings.enabled = false;
    assertTrue(
        CommandPermissions.modeDenial(settings, "survival", new Context("first", false, false))
            .isEmpty());
    assertFalse(
        CommandPermissions.modeDenial(settings, "creative", new Context("first", false, true))
            .isEmpty());
  }

  @Test
  void disablingKeepsAllSavedRules() {
    settings.worlds.put("world", Map.of("warp", CommandPermissions.Override.ALLOW));
    String before = new Gson().toJson(settings);
    assertFalse(
        CommandPermissions.denial(settings, command("warp"), new Context("world", false, false))
            .isEmpty());
    assertEquals(before, new Gson().toJson(settings));
    assertTrue(
        CommandPermissions.denial(settings, command("warp"), new Context("world", false, true))
            .isEmpty());
  }

  @Test
  void legacyCreativeRulesBecomeAllowedAndExplicitBlocksRemain() {
    Settings old =
        new Gson()
            .fromJson(
                "{\"rules\":{\"warp\":\"CREATIVE_ONLY\",\"give\":\"DISABLED\",\"tp\":\"ANY_MODE\"}}",
                Settings.class);
    assertEquals(Rule.ALLOWED, old.rules.get("warp"));
    assertEquals(Rule.ALLOWED, old.rules.get("tp"));
    assertEquals(Rule.DISABLED, old.rules.get("give"));
  }

  @Test
  void defaultsUnlockAllCommandsOnlyInCheatWorlds() {
    for (Command command : CommandPermissions.COMMANDS) {
      assertTrue(
          CommandPermissions.denial(settings, command, new Context("world", false, true))
              .isEmpty());
      assertEquals(
          !command.cheat(),
          CommandPermissions.denial(settings, command, new Context("world", false, false))
              .isEmpty());
    }
  }

  @Test
  void aliasesSharePolicyAndServerCommandsRemainUnaffected() {
    assertEquals("gamemode", CommandPermissions.canonical("gm"));
    assertEquals("tp", CommandPermissions.canonical("teleport"));
    assertEquals("toggledownfall", CommandPermissions.canonical("weather"));
    settings.enabled = false;
    assertTrue(
        CommandPermissions.denial(settings, command("warp"), new Context("", true, false))
            .isEmpty());
  }
}
