package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonPrimitive;
import java.util.*;
import org.junit.jupiter.api.Test;

class SettingsLayoutTest {
  private static Setting setting(String id, String page, String group) {
    return new Setting(id, "test", page, group, id, "", Setting.Kind.BOOLEAN,
        new JsonPrimitive(false), new JsonPrimitive(false), 0, 1, 1, List.of(), false);
  }

  @Test void everyRuleEditorKeepsRelatedRulesTogether() {
    for (String prefix : List.of("rules.world.", "rules.creation.", "rules.survival.", "rules.cheats.")) {
      var rules = new local.luke.power.rules.RuleSet();
      var rows = SettingsLayout.ordered(local.luke.power.config.backend.RuleSettings.entries("test", prefix, rules, rules));
      var ids = rows.stream().map(row -> row.id.substring(prefix.length())).toList();
      assertEquals(ids.indexOf("allow_creative_mode") + 1, ids.indexOf("allow_spectator_mode"));
      assertEquals(ids.indexOf("player_crop_trampling") + 1, ids.indexOf("mob_crop_trampling"));
      assertEquals(ids.indexOf("creeper_griefing") + 1, ids.indexOf("ghast_griefing"));
      assertEquals(ids.indexOf("explosions_destroy_items") + 1, ids.indexOf("lava_destroys_items"));
      assertEquals(ids.indexOf("lava_destroys_items") + 1, ids.indexOf("cactus_destroys_items"));
    }
  }

  @Test void presentationMovesKeepIdentityValueAndDraftState() {
    Setting sprint = setting("creative.sprintMultiplier", "Creative", "Flight");
    assertEquals("Creative", sprint.page);
    assertEquals("Flight", sprint.group);
    assertEquals("creative.sprintMultiplier", sprint.id);
    assertEquals("test", sprint.backend);
    assertFalse(sprint.value.getAsBoolean());
    assertFalse(sprint.changed());
  }

  @Test void switchesPrecedeDetailsRegardlessOfBackendOrderAndUnknownEntriesSurvive() {
    Setting follow = setting("tweaks.freeLookFollowThirdPerson", "Camera", "Free look");
    Setting enabled = setting("tweaks.freeLookToggle", "Camera", "Free look");
    Setting unknown = setting("external.future", "Camera", "Future camera settings");
    Setting second = setting("external.second", "Camera", "Future camera settings");
    var input = List.of(unknown, follow, second, enabled);
    assertEquals(List.of(enabled, follow, unknown, second), SettingsLayout.ordered(input));
    assertEquals(List.of(unknown, follow, second, enabled), input);
  }

  @Test void movedSharedSprintAndFogSettingsRetainBindingLinks() {
    ConfigSession session = new ConfigSession();
    Setting sprint = setting("keys.power_creative.sprint", "Controls", "Key bindings");
    Setting multiplier = setting("creative.sprintMultiplier", "Creative", "Flight");
    Setting fog = setting("keys.key.fog", "Controls", "Key bindings");
    Setting distances = setting("video.fogCycle", "Video", "Rendering");
    Setting distance = setting("native.renderDistance", "Video", "Rendering");
    var backend = new Backend() {
      public String id() { return "test"; }
      public List<java.nio.file.Path> files() { return List.of(); }
      public void validate(Map<String, com.google.gson.JsonElement> values) {}
      public void apply(Map<String, com.google.gson.JsonElement> values) {}
    };
    session.add(backend, List.of(sprint, multiplier, fog, distances, distance));
    assertTrue(ControlLinks.controls(session, multiplier).contains(sprint));
    assertEquals(distances, ControlLinks.related(session, fog));
    assertTrue(ControlLinks.settings(session, fog).contains(distance));
  }
  @Test void initialCollapsesAreIndependentCopiesAndFollowTheRequestedGroups() {
    var initial = SettingsLayout.initiallyCollapsed();
    assertFalse(initial.contains("Interface/General HUD"));
    assertFalse(initial.contains("Interface/Chat"));
    for (String page : List.of("Gameplay", "Fixes")) for (String group : SettingsLayout.groups(page))
      assertTrue(initial.contains(page + "/" + group));
    for (String group : SettingsLayout.groups("Interface"))
      assertEquals(!Set.of("General HUD", "Chat").contains(group), initial.contains("Interface/" + group));
    assertTrue(initial.containsAll(Set.of("Video/Entity visibility", "Video/Particles", "Audio/Individual sounds", "Advanced/Achievement recovery")));
    initial.clear(); assertFalse(SettingsLayout.initiallyCollapsed().isEmpty());
  }

  @Test void everySleepingEditorUsesTheSameBoundedSlider() {
    for (String prefix : List.of("rules.world.", "rules.creation.", "rules.survival.", "rules.cheats.")) {
      var rules = new local.luke.power.rules.RuleSet();
      var entries = local.luke.power.config.backend.RuleSettings.entries("test", prefix, rules, rules);
      Setting sleep = entries.stream().filter(s -> s.id.equals(prefix + "players_sleeping_percentage")).findFirst().orElseThrow();
      assertTrue(sleep.slider()); assertEquals(0, sleep.min); assertEquals(100, sleep.max);
      assertEquals(entries.indexOf(sleep) - 1, entries.indexOf(entries.stream().filter(s -> s.id.equals(prefix + "allow_sleep")).findFirst().orElseThrow()));
    }
  }
}
