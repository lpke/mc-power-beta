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
    Setting activation = setting("creative.sprintToggle", "Creative", "Flight");
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
    session.add(backend, List.of(sprint, activation, multiplier, fog, distances, distance));
    assertEquals(List.of(sprint), ControlLinks.controls(session, activation));
    assertTrue(ControlLinks.controls(session, multiplier).isEmpty());
    assertEquals(distances, ControlLinks.related(session, fog));
    assertEquals(List.of(distances), ControlLinks.settings(session, fog));
    assertTrue(ControlLinks.controls(session, distance).isEmpty());
  }
  @Test void gameplayMapAndInterfaceBindingsHaveSpecificGroups() {
    Map<String, List<String>> groups = Map.of(
        "Movement", List.of("powerbeta.sprint", "key.forward", "key.back", "key.left", "key.right", "key.jump", "key.sneak", "Auto-walk (toggle)", "Fake sneak (toggle)", "key.power_controls.dismount"),
        "Maps", List.of("powerbeta.map", "powerbeta.minimap", "powerbeta.waypoint", "powerbeta.waypoints", "powerbeta.enlargeMap", "powerbeta.mapZoomIn", "powerbeta.mapZoomOut", "powerbeta.toggleWorldWaypoints", "powerbeta.toggleMapWaypoints", "powerbeta.toggleMapRadar", "powerbeta.toggleMapCaves", "powerbeta.toggleMapGrid", "powerbeta.clearMapDestination", "powerbeta.mapSettings", "powerbeta.mapZoomReset"),
        "Interface", List.of("powerbeta.openCommands", "powerbeta.debugGraph", "powerbeta.infoHud", "key.chat", "playerList"));
    groups.forEach((group, keys) -> {
      assertTrue(SettingsLayout.groups("Controls").contains(group));
      for (String key : keys) {
        Setting row = setting("keys." + key, "Controls", "Key bindings");
        assertEquals("Controls", row.page, key);
        assertEquals(group, row.group, key);
      }
    });
  }

  @Test void mapAppearanceBelongsToTheHudWhileSamplingControlsStayInMap() {
    for (String name : List.of("zoom", "colourMode", "radar",
        "players", "hostile", "animals", "items", "otherEntities", "entityNames", "entityHeights", "radarHeight",
        "waypoints", "waypointNames", "waypointDistances", "waypointDistance", "chunkGrid", "slimeChunks",
        "lightOverlay", "footsteps", "pathColor", "pathOpacity", "playerIcon", "playerColor", "playerOpacity", "playerSize")) {
      Setting row = setting("maps." + name, "Map", "Terrain");
      assertEquals("Interface", row.page, name);
      assertEquals("Minimap", row.group, name);
      assertTrue(local.luke.power.ui.HudEditorSettings.contains(row), name);
      assertEquals("maps." + name, row.id);
    }
    for (String name : List.of("caveMode", "caveTop", "roofSize", "caveAbove", "caveBelow", "automaticDetail", "mapSmoothing",
        "radarLimit", "columnsPerTick", "cacheTiles", "uploadsPerFrame", "refreshSeconds", "updateRate", "mapOpenColumns", "mapOpenUploads", "mapOpenBudget")) {
      Setting row = setting("maps." + name, "Map", "Terrain");
      assertEquals("Map", row.page, name);
      assertFalse(local.luke.power.ui.HudEditorSettings.contains(row), name);
    }
  }

  @Test void initialCollapsesAreIndependentCopiesAndFollowTheRequestedGroups() {
    var initial = SettingsLayout.initiallyCollapsed();
    assertFalse(initial.contains("Interface/"));
    assertFalse(initial.contains("Interface/Title screen"));
    for (String page : List.of("Gameplay", "Fixes")) for (String group : SettingsLayout.groups(page))
      assertTrue(initial.contains(page + "/" + group));
    for (String group : SettingsLayout.groups("Interface"))
      assertEquals(!Set.of("", "General", "Title screen").contains(group), initial.contains("Interface/" + group));
    assertTrue(initial.containsAll(Set.of("Video/Entity visibility", "Video/Particles", "Audio/Individual sounds", "Advanced/Achievement recovery", "Map/Performance")));
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
  @Test void playlistChoicesLeadTheRemainingMusicSettings() {
    var ids=List.of("audio.dimensionMusic", "audio.musicDirectories", "audio.menuPlaylist",
        "audio.menuMusic", "audio.playlist", "audio.recursive");
    var entries=ids.stream().map(id -> setting(id,"Audio","Music playback")).toList();
    assertEquals(List.of("audio.playlist","audio.menuPlaylist","audio.musicDirectories","audio.menuMusic",
        "audio.recursive","audio.dimensionMusic"),SettingsLayout.ordered(entries).stream().map(s -> s.id).toList());
  }
}
