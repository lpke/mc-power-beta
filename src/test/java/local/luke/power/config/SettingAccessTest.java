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
        assertEquals(owner == enabled || id.equals("creative.flight") && enabled == GameRule.ALLOW_FLIGHT, SettingAccess.visible(session, setting), id + " / " + enabled);
        assertTrue(setting.value.getAsBoolean()); assertFalse(setting.changed());
      });
      rule(session, enabled, false);
    }
  }

  @Test void modeFlightPreferencesHaveIndependentPermissions() {
    var session = world();
    for (GameRule permission : List.of(GameRule.ALLOW_CREATIVE_MODE, GameRule.ALLOW_SPECTATOR_MODE, GameRule.ALLOW_FLIGHT)) {
      rule(session, permission, true);
      for (String id : List.of("creative.flightSpeed", "creative.sprintToggle", "creative.sprintMultiplier"))
        assertEquals(permission != GameRule.ALLOW_SPECTATOR_MODE, SettingAccess.visible(session, entry(id)), id);
      assertEquals(permission == GameRule.ALLOW_SPECTATOR_MODE, SettingAccess.visible(session, entry("creative.spectatorSprintToggle")));
      for (String id : List.of("keys.power_creative.modifier", "keys.power_creative.picker"))
        assertEquals(permission != GameRule.ALLOW_FLIGHT, SettingAccess.visible(session, entry(id)));
      for (String id : List.of("keys.power_creative.sprint", "creative.blockReach", "creative.entityReach", "power_camera:config.speed", "world.difficulty"))
        assertTrue(SettingAccess.visible(session, entry(id)), id);
      rule(session, permission, false);
    }
  }

  @Test void hudPositionModesHideInactiveGroupsWithoutChangingDrafts() {
    var session = world();
    Backend backend = new Backend() {
      public String id() { return "test"; }
      public List<java.nio.file.Path> files() { return List.of(); }
      public void validate(Map<String, JsonElement> values) {}
      public void apply(Map<String, JsonElement> values) {}
    };
    Setting mode = new Setting("power_hud:hudpositions.hudPositioningSystem", "test", "Interface", "HUD layout", "HUD positioning", "",
        Setting.Kind.CHOICE, new JsonPrimitive(1), new JsonPrimitive(1), 0, 2, 1, List.of("Vanilla", "Combined", "Separate"), false);
    Setting combined = entry("power_hud:hudpositions.SIMPLE_HUD_POSITION_CONFIG.enableVisibility");
    Setting separate = entry("power_hud:hudpositions.ADVANCED_HUD_POSITIONS_CONFIG.HOTBAR_POSITION_CONFIG.enableVisibility");
    session.add(backend, List.of(mode, combined, separate));
    for (int n : new int[]{1, 2, 0, 1}) {
      mode.value = new JsonPrimitive(n);
      assertEquals(n == 1, SettingAccess.visible(session, combined));
      assertEquals(n == 2, SettingAccess.visible(session, separate));
      assertTrue(combined.value.getAsBoolean()); assertTrue(separate.value.getAsBoolean());
      assertFalse(combined.changed()); assertFalse(separate.changed());
    }
  }

  @Test void allRulesRemainEditableAndMainMenuShowsGlobalToolPreferences() {
    var session = world();
    session.settings().forEach(s -> assertTrue(SettingAccess.visible(session, s), s.id));
    for (String id : List.of("creative.flight", "creative.spectatorSpeed", "worldedit.maxBlocks"))
      assertTrue(SettingAccess.visible(new ConfigSession(), entry(id)), id);
    var number = session.settings().stream().filter(s -> s.id.endsWith("players_sleeping_percentage")).findFirst().orElseThrow();
    assertTrue(number.slider());
    number.parse("100");
    assertEquals(100, number.value.getAsInt());
    assertThrows(IllegalArgumentException.class, () -> number.parse("101"));
  }

  @Test void titleStylesRetainRetroDraftsWhileHidingTheirEditors() {
    var session = world();
    Setting style = new Setting("logo.style", "test", "Interface", "Title screen", "Title style", "",
        Setting.Kind.CHOICE, new JsonPrimitive(0), new JsonPrimitive(0), 0, 4, 1,
        List.of("Retro", "Beta", "2017", "2020", "Modern"), false);
    session.add(new Backend() {
      public String id() { return "test"; }
      public List<java.nio.file.Path> files() { return List.of(); }
      public void validate(Map<String, JsonElement> values) {}
      public void apply(Map<String, JsonElement> values) {}
    }, List.of(style));
    for (int n : new int[]{0, 1, 2, 3, 4, 0}) {
      style.value = new JsonPrimitive(n);
      for (String id : List.of("logo.logo.lines", "logo.blocks.map", "logo.logo.animation.enabled")) {
        Setting setting = entry(id);
        assertEquals(n == 0, SettingAccess.visible(session, setting));
        assertTrue(setting.value.getAsBoolean());
      }
      assertTrue(SettingAccess.visible(session, entry("logo.scrollingBackground.enabled")));
      assertTrue(SettingAccess.visible(session, style));
    }
  }
}
