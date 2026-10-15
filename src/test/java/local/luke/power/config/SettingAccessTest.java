package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.util.*;
import local.luke.power.rules.*;
import local.luke.power.config.backend.RuleSettings;
import org.junit.jupiter.api.Test;

class SettingAccessTest {
  private Setting entry(String id) {
    return new Setting(id, id.startsWith("creative.") ? "creative" : "test", "General", "Game", id, "",
        Setting.Kind.BOOLEAN, new JsonPrimitive(true), new JsonPrimitive(true), 0, 1, 1, List.of(), false);
  }
  private ConfigSession world() {
    ConfigSession session = new ConfigSession();
    session.add(new Backend() {
      public String id() { return "world"; }
      public List<java.nio.file.Path> files() { return List.of(); }
      public void validate(Map<String, JsonElement> values) {}
      public void apply(Map<String, JsonElement> values) {}
    }, RuleSettings.entries("world", "rules.world.", new RuleSet(), new RuleSet()));
    return session;
  }
  private void rule(ConfigSession session, GameRule rule, boolean value) {
    session.settings().stream().filter(s -> s.id.equals("rules.world." + rule.id()))
        .findFirst().orElseThrow().value = new JsonPrimitive(value);
  }

  @Test void eachPermissionOnlyExposesItsOwnSettingsAndPreservesPreferences() {
    var session = world();
    var owners = Map.of("creative.flight", GameRule.ALLOW_CREATIVE_MODE,
        "worldedit.maxBlocks", GameRule.ALLOW_WORLD_EDITING,
        "creative.spectatorScroll", GameRule.ALLOW_SPECTATOR_MODE,
        "creative.showSpectatorSpeed", GameRule.ALLOW_SPECTATOR_MODE,
        "visual.spectatorFullBright", GameRule.ALLOW_SPECTATOR_MODE);
    for (var enabled : GameRule.values()) if (enabled.access()) {
      rule(session, enabled, true);
      owners.forEach((id, owner) -> {
        Setting setting = entry(id);
        assertEquals(owner == enabled, SettingAccess.visible(session, setting), id + " / " + enabled);
        assertTrue(setting.value.getAsBoolean()); assertFalse(setting.changed());
      });
      rule(session, enabled, false);
    }
  }

  @Test void eitherModeExposesPickerAndFlightSprintWhileFreecamControlsRemainIndependent() {
    var session = world();
    for (GameRule rule : List.of(GameRule.ALLOW_CREATIVE_MODE, GameRule.ALLOW_SPECTATOR_MODE)) {
      for (boolean enabled : List.of(false, true)) {
        rule(session, rule, enabled);
        for (String id : List.of("creative.sprintFlight", "keys.power_creative.modifier", "keys.power_creative.picker"))
          assertEquals(enabled, SettingAccess.visible(session, entry(id)), id);
        for (String id : List.of("creative.sprintToggle", "creative.sprintMultiplier", "keys.power_creative.sprint",
            "creative.blockReach", "creative.entityReach", "power_camera:config.speed", "world.difficulty"))
          assertTrue(SettingAccess.visible(session, entry(id)), id);
      }
      rule(session, rule, false);
    }
  }

  @Test void allRulesRemainEditableAndMainMenuShowsGlobalToolPreferences() {
    var session = world();
    session.settings().forEach(s -> assertTrue(SettingAccess.visible(session, s), s.id));
    for (String id : List.of("creative.flight", "creative.spectatorSpeed", "worldedit.maxBlocks"))
      assertTrue(SettingAccess.visible(new ConfigSession(), entry(id)), id);
    var number = session.settings().stream().filter(s -> s.id.endsWith("players_sleeping_percentage")).findFirst().orElseThrow();
    assertFalse(number.slider());
    number.parse("2147483647");
    assertEquals(Integer.MAX_VALUE, number.value.getAsInt());
  }
}
