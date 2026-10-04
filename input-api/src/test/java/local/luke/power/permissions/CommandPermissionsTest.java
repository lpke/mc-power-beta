package local.luke.power.permissions;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class CommandPermissionsTest {
  private final CommandPermissions.Command warp =
      CommandPermissions.COMMANDS.stream()
          .filter(c -> c.name().equals("warp"))
          .findFirst()
          .orElseThrow();
  private final CommandPermissions.Settings settings = new CommandPermissions.Settings();

  private String denial(String world, boolean creative) {
    return CommandPermissions.denial(
        settings, warp, new CommandPermissions.Context(world, creative, false));
  }

  @Test
  void checksEveryCombinationOfMasterGlobalWorldAndMode() {
    for (boolean master : List.of(true, false))
      for (var global : CommandPermissions.Rule.values())
        for (var override : CommandPermissions.Override.values())
          for (boolean creative : List.of(true, false)) {
            settings.enabled = master;
            settings.rules.put("warp", global);
            settings.worlds.put("world", Map.of("warp", override));
            boolean allowed =
                master
                    && (override == CommandPermissions.Override.ALLOW
                        || override == CommandPermissions.Override.INHERIT
                            && (global == CommandPermissions.Rule.ANY_MODE
                                || global == CommandPermissions.Rule.CREATIVE_ONLY && creative));
            assertEquals(
                allowed,
                denial("world", creative).isEmpty(),
                master + "/" + global + "/" + override + "/" + creative);
          }
  }

  @Test
  void worldsAndCommandsDoNotInheritEachOthersOverrides() {
    settings.worlds.put("first", Map.of("warp", CommandPermissions.Override.ALLOW));
    assertTrue(denial("first", false).isEmpty());
    assertFalse(denial("second", false).isEmpty());
    var give =
        CommandPermissions.COMMANDS.stream()
            .filter(c -> c.name().equals("give"))
            .findFirst()
            .orElseThrow();
    assertFalse(
        CommandPermissions.denial(
                settings, give, new CommandPermissions.Context("first", false, false))
            .isEmpty());
  }

  @Test
  void disablingDoesNotChangeAnyOverridesOrRules() {
    settings.worlds.put("first", new HashMap<>(Map.of("warp", CommandPermissions.Override.ALLOW)));
    var before = new com.google.gson.Gson().toJson(settings.worlds);
    settings.enabled = false;
    assertFalse(denial("first", true).isEmpty());
    assertEquals(before, new com.google.gson.Gson().toJson(settings.worlds));
    settings.enabled = true;
    assertTrue(denial("first", false).isEmpty());
  }

  @Test
  void singleplayerPolicyNeverControlsServerCommands() {
    settings.enabled = false;
    assertTrue(
        CommandPermissions.denial(settings, warp, new CommandPermissions.Context("", false, true))
            .isEmpty());
  }

  @Test
  void aliasesSharePolicyAndModeEntryIsAvailableByDefault() {
    assertEquals("gamemode", CommandPermissions.canonical("gm"));
    var mode =
        CommandPermissions.COMMANDS.stream()
            .filter(c -> c.name().equals("gamemode"))
            .findFirst()
            .orElseThrow();
    assertTrue(
        CommandPermissions.denial(
                settings, mode, new CommandPermissions.Context("world", false, false))
            .isEmpty());
    assertFalse(denial("world", false).isEmpty());
  }
}
