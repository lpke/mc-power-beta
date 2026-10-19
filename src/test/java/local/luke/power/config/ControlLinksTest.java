package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;

class ControlLinksTest {
  private Setting setting(String id) {
    boolean key = id.startsWith("keys.");
    return new Setting(id, "fixture", "Camera", "Controls", id, "", key ? Setting.Kind.KEY : Setting.Kind.BOOLEAN,
        key ? new JsonPrimitive(0) : new JsonPrimitive(false), new JsonPrimitive(false), 0, 1, 1, List.of(), false);
  }
  private ConfigSession session(List<Setting> settings) {
    var session = new ConfigSession();
    session.add(new Backend() {
      public String id() { return "fixture"; }
      public List<Path> files() { return List.of(); }
      public void validate(Map<String, JsonElement> values) {}
      public void apply(Map<String, JsonElement> values) {}
    }, settings);
    return session;
  }
  @Test void directCounterpartsDoNotExpandToOtherPreferencesInTheSameFeature() {
    var pairs = Map.ofEntries(
        Map.entry("keys.Auto-walk (toggle)", "tweaks.autoWalkHoldToWalk"),
        Map.entry("keys.key.powerbeta.free_look", "tweaks.freeLookToggle"),
        Map.entry("keys.Fast place (toggle)", "tweaks.placement.enabled"),
        Map.entry("keys.Fake sneak (toggle)", "tweaks.sneak.enabled"),
        Map.entry("keys.power_building.hotbar.base", "tweaks.hotbar.overlay"),
        Map.entry("keys.power_building.hotbar.scroll", "tweaks.hotbar.reverseScroll"),
        Map.entry("keys.power_creative.sprint", "creative.sprintToggle"),
        Map.entry("keys.Change Freecam Speed", "power_camera:config.speed"),
        Map.entry("keys.power_beta.entities", "visual.hideEntities"),
        Map.entry("keys.power_beta.clickMining", "tweaks.clickMining"),
        Map.entry("keys.powerbeta.lightOverlay", "lightOverlay.enabled"),
        Map.entry("keys.powerbeta.infoHud", "infoHud.enabled"),
        Map.entry("keys.key.fog", "video.fogCycle"),
        Map.entry("keys.Custom Photo", "power_capture:config.customResolutionPhotoWidth"),
        Map.entry("keys.Isometric Photo", "power_capture:config.isometricPhotoScale"),
        Map.entry("keys.key.power_controls.third_person", "power_controls:userinterface.frontViewThirdPerson"),
        Map.entry("keys.key.power_controls.toggle_raw_input", "power_controls:general.rawInput"));
    for (var pair : pairs.entrySet()) {
      var key = setting(pair.getKey());
      var direct = setting(pair.getValue());
      var neighbour = setting(pair.getValue() + ".neighbour");
      var session = session(List.of(neighbour, key, direct));
      assertSame(direct, ControlLinks.related(session, key), key.id);
      assertEquals(List.of(direct), ControlLinks.settings(session, key), key.id);
      assertEquals(List.of(key), ControlLinks.controls(session, direct), direct.id);
      assertTrue(ControlLinks.controls(session, neighbour).isEmpty(), neighbour.id);
      assertFalse(ControlLinks.hasControls(neighbour), neighbour.id);
      assertTrue(ControlLinks.enabled(session, key), key.id);
    }
  }
  @Test void actionsWithoutAnExactSettingDoNotLinkToIncidentalTuningOrMessages() {
    var preferences = List.of("creative.doubleTapTicks", "creative.flight", "creative.blockReach",
        "power_camera:config.speed", "power_camera:config.playerMovementMessages", "tweaks.flexible.showOverlay",
        "tweaks.hotbar.overlay", "maps.waypoints", "maps.size", "native.fov", "visual.fullBrightMessages",
        "tweaks.autoMineAnnounceToggle", "activeTweaks.cinematicCameraMessages", "power_controls:general.rawInput",
        "power_controls:userinterface.photoModeConfig.enablePhotoModeButton", "power_capture:config.addBasicScreenshotsToClipboard");
    for (String id : List.of("keys.key.jump", "keys.key.sneak", "keys.powerbeta.containerPreview", "keys.Toggle Freecam",
        "keys.Toggle Player Movement", "keys.Save Or Load Camera Position", "keys.Open Camera Position Gui",
        "keys.Placement offset (hold)", "keys.Placement adjacent (hold)", "keys.Placement rotation (hold)",
        "keys.Placement reverse (hold)", "keys.Placement into face (hold)", "keys.power_building.hotbar.row1",
        "keys.power_building.hotbar.row2", "keys.power_building.hotbar.row3", "keys.powerbeta.waypoint",
        "keys.powerbeta.waypoints", "keys.powerbeta.enlargeMap", "keys.power_beta.fullBright", "keys.Auto-mine (toggle)",
        "keys.key.power_controls.zoom", "keys.key.power_controls.cinematic_camera", "keys.key.power_controls.photo_mode",
        "keys.key.power_controls.take_screenshot", "keys.key.power_controls.rescan", "keys.powerbeta.clearMapDestination",
        "keys.powerbeta.mapSettings")) {
      Setting key = setting(id);
      List<Setting> all = new ArrayList<>(preferences.stream().map(this::setting).toList());
      all.add(key);
      assertNull(ControlLinks.related(session(all), key), id);
    }
  }
  @Test void mapLinksRemainExactAndZoomUsesOnePredictableReverseCounterpart() {
    var pairs = Map.of("powerbeta.map", "maps.worldMap", "powerbeta.minimap", "maps.minimap",
        "powerbeta.toggleWorldWaypoints", "maps.worldWaypoints", "powerbeta.toggleMapWaypoints", "maps.waypoints",
        "powerbeta.toggleMapRadar", "maps.radar", "powerbeta.toggleMapCaves", "maps.caveMode",
        "powerbeta.toggleMapGrid", "maps.chunkGrid");
    List<Setting> all = new ArrayList<>();
    pairs.forEach((key, value) -> { all.add(setting("keys." + key)); all.add(setting(value)); });
    Setting range = setting("maps.waypointDistance");
    all.add(range);
    var mapSession = session(all);
    pairs.forEach((id, target) -> {
      Setting key = all.stream().filter(s -> s.id.equals("keys." + id)).findFirst().orElseThrow();
      assertEquals(target, ControlLinks.related(mapSession, key).id, id);
      assertEquals(1, ControlLinks.settings(mapSession, key).size(), id);
    });
    assertTrue(ControlLinks.controls(mapSession, range).isEmpty());
    assertFalse(ControlLinks.hasControls(range));
    Setting zoom = setting("maps.zoom");
    Setting in = setting("keys.powerbeta.mapZoomIn"), out = setting("keys.powerbeta.mapZoomOut"), reset = setting("keys.powerbeta.mapZoomReset");
    var zoomSession = session(List.of(out, reset, zoom, in));
    assertEquals(List.of(in), ControlLinks.controls(zoomSession, zoom));
    for (Setting key : List.of(in, out, reset)) assertEquals(List.of(zoom), ControlLinks.settings(zoomSession, key));
    assertEquals(List.of(out), ControlLinks.controls(session(List.of(zoom, out)), zoom));
  }
  @Test void groundSprintUsesItsWorldRuleWithoutLinkingTheRuleOrOtherTuning() {
    var key = setting("keys.powerbeta.sprint");
    var activation = setting("sprint.activation");
    var speed = setting("sprint.speed");
    var rule = setting("rules.world.allow_sprinting");
    var session = session(List.of(key, activation, speed, rule));
    assertFalse(ControlLinks.enabled(session, key));
    assertFalse(SettingAccess.visible(session, key));
    rule.value = new JsonPrimitive(true);
    assertTrue(ControlLinks.enabled(session, key));
    assertTrue(SettingAccess.visible(session, key));
    assertSame(activation, ControlLinks.related(session, key));
    assertEquals(List.of(activation), ControlLinks.settings(session, key));
    assertTrue(ControlLinks.controls(session, speed).isEmpty());
    assertTrue(ControlLinks.controls(session, rule).isEmpty());
    assertTrue(ControlLinks.enabled(session(List.of(key, activation, speed)), key));
  }
  @Test void sharedFlightSprintRemainsAvailableWithoutRetiredFeatureSwitches() {
    var key = setting("keys.power_creative.sprint");
    assertTrue(ControlLinks.enabled(session(List.of(key)), key));
  }
}
