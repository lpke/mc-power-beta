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

  @Test void presentationMovesKeepIdentityValueAndDraftState() {
    Setting sprint = setting("creative.sprintMultiplier", "Creative", "Flight");
    assertEquals("Camera", sprint.page);
    assertEquals("Flight sprint", sprint.group);
    assertEquals("creative.sprintMultiplier", sprint.id);
    assertEquals("test", sprint.backend);
    assertFalse(sprint.value.getAsBoolean());
    assertFalse(sprint.changed());
  }

  @Test void switchesPrecedeDetailsRegardlessOfBackendOrderAndUnknownEntriesSurvive() {
    Setting follow = setting("tweaks.freeLookFollowThirdPerson", "Camera", "Free look");
    Setting enabled = setting("tweaks.freeLook", "Camera", "Free look");
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
}
