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
    var hud = setting(HudEditorSettings.ENTRY, "[]"); assertEquals("General", hud.page); assertEquals("Interface", hud.group);
    assertTrue(HudEditorSettings.contains(hud));
    assertTrue(HudEditorSettings.contains(setting("infoHud.clusters.2.offsetX", "[]")));
    assertFalse(HudEditorSettings.contains(setting("activeTweaks.cinematicCameraMessages", "[]")));
    assertTrue(HudEditorSettings.lettering(setting("logo.presets", "[]")));
    assertFalse(HudEditorSettings.lettering(setting("logo.style", "[]")));
  }
  @Test void undoSkipsCancelledEditsAndRestoresAWholeReset() {
    var draft = new SettingsDraft(List.of(setting("test.a", "[1]"), setting("test.b", "[2]")), s -> true);
    var before = draft.snapshot(); draft.checkpoint(); draft.settings().forEach(Setting::reset);
    assertTrue(draft.canUndo()); draft.checkpoint();
    draft.undo(); assertEquals(before, draft.snapshot()); assertFalse(draft.canUndo());
    draft.checkpoint(); assertFalse(draft.canUndo());
    draft.get("test.a").value = new JsonArray();
    assertTrue(draft.canUndo()); draft.undo(); assertEquals(before, draft.snapshot());
  }
  @Test void auditedHudControlsRouteWithoutTakingOverCameraEffectsOrGameplay() {
    for (String id : List.of(HudEditorSettings.ITEM_NAMES, HudEditorSettings.ITEM_FADE, HudEditorSettings.TOAST,
        "power_hud:config.disableCrosshair", "power_hud:config.drawXboxXAndYButtons", "power_hud:config.enableChatScroll",
        "power_hud:config.chatHistorySize", "power_hud:config.chatFadeTime", "interface.blinkingChatCursor"))
      assertTrue(HudEditorSettings.contains(setting(id, "[]")), id);
    for (String id : List.of("power_hud:config.disableVignette", "power_hud:config.enableHotbarBlockRenderingFix",
        "interface.blinkingUiCursor", "activeTweaks.fakeSneakMessages", "power_controls:userinterface.showQuitButton"))
      assertFalse(HudEditorSettings.contains(setting(id, "[]")), id);
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
  @Test void minimapDraftIncludesEveryAppearanceControlAndUndoKeepsOptionsUntouched() {
    List<String> included = List.of("rotate", "circular", "hideWithDebug", "size", "corner", "offsetX", "offsetY", "opacity", "zoom",
        "coordinates", "biome", "clock", "compass", "colourMode", "heightShading", "slopeShading", "caveLighting",
        "radar", "players", "hostile", "animals", "items", "otherEntities", "entityNames", "entityHeights", "radarHeight",
        "waypoints", "waypointNames", "waypointDistances", "waypointDistance", "chunkGrid", "slimeChunks", "lightOverlay", "footsteps",
        "pathColor", "pathOpacity", "playerIcon", "playerColor", "playerOpacity", "playerSize",
        "directionTextSize", "belowTextSize", "minimapArrowSize", "minimapWaypointArrows", "minimapDestinationArrow", "minimapDeathpointArrows",
        "destinationArrowColor", "destinationArrowOpacity");
    var source = included.stream().map(id -> setting("maps." + id, "[1]")).toList();
    var draft = new SettingsDraft(source, HudEditorSettings::contains);
    assertEquals(source.size(), draft.settings().size());
    var before = draft.snapshot();
    draft.checkpoint();
    draft.settings().forEach(s -> s.value = JsonParser.parseString("[2]"));
    assertTrue(draft.changed());
    assertTrue(source.stream().allMatch(s -> s.value.toString().equals("[1]")));
    draft.undo();
    assertEquals(before, draft.snapshot());
    assertFalse(draft.changed());
    for (String id : List.of("minimap", "worldMap", "hudEditor", "caveMode", "caveTop", "caveAbove", "caveBelow", "roofSize", "worldWaypoints",
        "deathpoints", "keepOldDeaths", "pauseMap", "columnsPerTick", "cacheTiles", "uploadsPerFrame", "refreshSeconds", "updateRate", "radarLimit"))
      assertFalse(HudEditorSettings.contains(setting("maps." + id, "[]")), id);
  }
  @Test void refreshedMinimapPositionDraftMovesImmediatelyWithoutChangingLiveSettingsOrParent() {
    var source = new local.luke.power.map.MapSettings();
    var x = new Setting("maps.offsetX", "maps", "Interface", "Minimap", "X", "", Setting.Kind.INTEGER,
        new JsonPrimitive(6), new JsonPrimitive(6), 0, 500, 1, List.of(), false);
    var y = new Setting("maps.offsetY", "maps", "Interface", "Minimap", "Y", "", Setting.Kind.INTEGER,
        new JsonPrimitive(6), new JsonPrimitive(6), 0, 500, 1, List.of(), false);
    var draft = new SettingsDraft(List.of(x, y), HudEditorSettings::contains);
    var before = local.luke.power.map.MinimapGeometry.layout(HudPreview.minimapDraft(draft, source, true), 640, 360, false);
    draft.checkpoint(); draft.get(x.id).value = new JsonPrimitive(26); draft.get(y.id).value = new JsonPrimitive(14);
    var moved = local.luke.power.map.MinimapGeometry.layout(HudPreview.minimapDraft(draft, source, true), 640, 360, false);
    assertEquals(-20, moved.x() - before.x()); assertEquals(8, moved.y() - before.y());
    assertEquals(6, source.offsetX); assertEquals(6, source.offsetY);
    assertEquals(6, x.value.getAsInt()); assertEquals(6, y.value.getAsInt());
    draft.undo();
    assertEquals(before, local.luke.power.map.MinimapGeometry.layout(HudPreview.minimapDraft(draft, source, true), 640, 360, false));
  }
  @Test void toastOnlyHidesForMinimapAndTheSecondHumanNumberedCluster() {
    assertTrue(HudEditorSettings.suppressesToast("minimap"));
    assertTrue(HudEditorSettings.suppressesToast("cluster.1"));
    for (String selected : List.of("layout", "cluster.0", "cluster.2", "cluster.3", "debug", "tweaks"))
      assertFalse(HudEditorSettings.suppressesToast(selected), selected);
  }
}
