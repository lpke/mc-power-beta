package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import local.luke.power.map.MapSettings;
import local.luke.power.map.MapToggle;
import org.junit.jupiter.api.Test;

class WorldMapMinimapInputTest {
  @Test void leftClickDoesNotNavigateResetOrToggleTheSettingsPreview() {
    Runnable unexpected = () -> fail("Left-click must keep the current map and settings view");
    WorldMapMinimapInput.press(0, MapToggle.MOBS.ordinal(), unexpected, toggle -> unexpected.run(), unexpected);
  }

  @Test void rightClickResetsOnlyZoom() {
    var resets = new AtomicInteger();
    WorldMapMinimapInput.press(1, MapToggle.MOBS.ordinal(), resets::incrementAndGet,
        toggle -> fail("Right-click must not toggle " + toggle), () -> fail("Right-click must not toggle heads"));
    assertEquals(1, resets.get());
  }

  @Test void middleClickDispatchesEveryConfiguredActionToTheOwningDraft() {
    for (MapToggle action : MapToggle.ALL) {
      var edited = new ArrayList<MapToggle>();
      var heads = new AtomicInteger();
      WorldMapMinimapInput.press(2, action.ordinal(), () -> fail("Middle-click must not reset zoom"),
          edited::add, heads::incrementAndGet);
      if (action == MapToggle.HEADS) {
        assertTrue(edited.isEmpty());
        assertEquals(1, heads.get());
      } else {
        assertEquals(java.util.List.of(action), edited);
        assertEquals(0, heads.get());
      }
    }
  }

  @Test void ordinaryMiddleActionCanChangeAnIsolatedPreviewWithoutChangingSavedSettings() {
    var saved = new MapSettings();
    var preview = new MapSettings();
    boolean before = saved.items;
    WorldMapMinimapInput.press(2, MapToggle.ITEMS.ordinal(), () -> fail(),
        toggle -> MapToggle.apply(preview, toggle.changes(preview)), () -> fail());
    assertEquals(!before, preview.items);
    assertEquals(before, saved.items);
  }
}
