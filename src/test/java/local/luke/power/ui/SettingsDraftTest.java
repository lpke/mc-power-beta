package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.util.*;
import local.luke.power.config.*;
import org.junit.jupiter.api.Test;

class SettingsDraftTest {
  private Setting setting(String id, String value) {
    return new Setting(id, "test", "Interface", "", id, "", Setting.Kind.LIST,
        JsonParser.parseString(value), new JsonArray(), 0, 0, 1, List.of(), false);
  }
  @Test void editorCopiesAndUndoNeverChangeTheParentOrSnapshots() {
    var original = setting("test.grid", "[\"*\"]");
    var draft = new SettingsDraft(List.of(original), s -> true);
    var snapshot = draft.snapshot(); draft.checkpoint();
    draft.get(original.id).value.getAsJsonArray().add("*");
    assertEquals(1, original.value.getAsJsonArray().size());
    assertEquals(1, snapshot.get(original.id).getAsJsonArray().size());
    assertTrue(draft.changed()); draft.undo(); assertFalse(draft.changed());
    for (int i = 0; i < 100; i++) { draft.checkpoint(); draft.get(original.id).value.getAsJsonArray().add(i); }
    int undos = 0; while (draft.canUndo()) { draft.undo(); undos++; }
    assertEquals(64, undos);
  }
  @Test void editorRoutingRetainsGameplaySettingsAndExcludesMessages() {
    var hud = setting(HudEditorSettings.ENTRY, "[]"); assertEquals("General HUD", hud.group);
    assertTrue(HudEditorSettings.contains(hud));
    assertTrue(HudEditorSettings.contains(setting("infoHud.clusters.2.offsetX", "[]")));
    assertFalse(HudEditorSettings.contains(setting("activeTweaks.cinematicCameraMessages", "[]")));
    assertTrue(HudEditorSettings.lettering(setting("logo.presets", "[]")));
    assertFalse(HudEditorSettings.lettering(setting("logo.style", "[]")));
  }
  @Test void validationChecksTheCompleteEditBeforeAnyBackendWrites() {
    ConfigSession session = new ConfigSession(); int[] writes = {0};
    session.add(new Backend() {
      public String id() { return "test"; }
      public List<java.nio.file.Path> files() { return List.of(); }
      public void validate(Map<String, JsonElement> values) { if (values.size() != 2) throw new IllegalArgumentException("linked edit"); }
      public void apply(Map<String, JsonElement> values) { writes[0]++; }
    }, List.of(setting("test.a", "[]"), setting("test.b", "[]")));
    session.settings().get(0).value = JsonParser.parseString("[1]");
    assertThrows(IllegalArgumentException.class, session::validate);
    session.settings().get(1).value = JsonParser.parseString("[2]");
    assertDoesNotThrow(session::validate); assertEquals(0, writes[0]);
  }
}
