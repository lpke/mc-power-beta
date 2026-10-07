package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonPrimitive;
import java.util.List;
import org.junit.jupiter.api.Test;

class SettingAccessTest {
  @Test void gatePresentationDoesNotOverwriteEnabledPreferences() {
    for (var kind : List.of(Setting.Kind.BOOLEAN, Setting.Kind.CHOICE, Setting.Kind.INTEGER, Setting.Kind.KEY)) {
      var value = kind == Setting.Kind.BOOLEAN ? new JsonPrimitive(true) : new JsonPrimitive(1);
      var setting = new Setting("worldedit.enabled", "worldedit", "World editing", "Access", "Enabled", "",
          kind, value, value, 0, 2, 1, List.of("Default", "Enabled", "Disabled"), false);
      assertEquals(kind == Setting.Kind.BOOLEAN ? "Off" : "Disabled", SettingAccess.lockedValue(setting));
      assertEquals(value, setting.value);
      assertFalse(setting.changed());
    }
  }

  private Setting entry(String id, String backend, Setting.Kind kind) {
    return new Setting(id, backend, "General", "Game", id, "", kind,
        new JsonPrimitive(true), new JsonPrimitive(true), 0, 1, 1, List.of(), false);
  }

  @Test void hiddenCheatRowsPreserveValuesAndNonCheatCommands() {
    ConfigSession session = new ConfigSession();
    Setting cheats = entry("world.cheats", "world", Setting.Kind.BOOLEAN);
    session.add(new Backend() {
      public String id() { return "world"; }
      public java.util.List<java.nio.file.Path> files() { return List.of(); }
      public void validate(java.util.Map<String, com.google.gson.JsonElement> values) {}
      public void apply(java.util.Map<String, com.google.gson.JsonElement> values) {}
    }, List.of(cheats));
    for (boolean enabled : List.of(false, true)) {
      cheats.value = new JsonPrimitive(enabled);
      for (String id : List.of("creative.flight", "worldedit.enabled", "world.daylightCycle", "world.weatherCycle", "commands.enabled", "commands.mode.creative", "commands.worldMode.spectator", "keys.power_creative.modifier", "keys.power_creative.picker")) {
        var setting = entry(id, id.startsWith("creative.") ? "creative" : "test", id.startsWith("keys.") ? Setting.Kind.KEY : Setting.Kind.BOOLEAN);
        assertEquals(enabled, SettingAccess.visible(session, setting), id);
        assertTrue(setting.value.getAsBoolean()); assertFalse(setting.changed());
        assertFalse(SettingAccess.visible(new ConfigSession(), setting), id);
      }
      for (var command : local.luke.power.permissions.CommandPermissions.COMMANDS)
        for (String prefix : List.of("commands.rule.", "commands.world."))
          assertEquals(enabled || !command.cheat(), SettingAccess.visible(session, entry(prefix + command.name(), "commandAccess", Setting.Kind.CHOICE)));
      for (String id : List.of("world.cheats", "world.difficulty", "creative.sprintToggle", "creative.sprintMultiplier", "keys.power_creative.sprint", "tweaks.placement.enabled", "power_camera:config.enabled"))
        assertTrue(SettingAccess.visible(session, entry(id, id.startsWith("creative.") ? "creative" : "test", id.startsWith("keys.") ? Setting.Kind.KEY : Setting.Kind.BOOLEAN)), id);
    }
  }
}
