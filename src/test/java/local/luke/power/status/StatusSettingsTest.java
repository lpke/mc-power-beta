package local.luke.power.status;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import local.luke.power.input.TweakIndicators;
import local.luke.power.input.TweakIndicators.Tweak;
import org.junit.jupiter.api.Test;

class StatusSettingsTest {
  @Test void defaultsAndPersistedChoicesRemainIndependentOfLiveStates() {
    StatusSettings s = new Gson().fromJson("{}", StatusSettings.class);
    assertTrue(s.enabled);
    assertEquals(8, s.position);
    for (var tweak : Tweak.values()) assertEquals(tweak != Tweak.SLAB_COMPLETION, s.includes(tweak));
    boolean[] state = {false};
    TweakIndicators.register(Tweak.FAST_PLACEMENT, () -> state[0] ? "" : null);
    assertNull(TweakIndicators.label(Tweak.FAST_PLACEMENT));
    state[0] = true;
    assertEquals("Fast placement", TweakIndicators.label(Tweak.FAST_PLACEMENT));
    s.fastPlacement = false;
    StatusSettings saved = new Gson().fromJson(new Gson().toJson(s), StatusSettings.class);
    assertFalse(saved.includes(Tweak.FAST_PLACEMENT));
    assertTrue(state[0]);
    TweakIndicators.register(Tweak.PLACEMENT_RESTRICTION, () -> "Plane");
    assertEquals("Placement restriction: Plane", TweakIndicators.label(Tweak.PLACEMENT_RESTRICTION));
  }
  @Test void everyAnchorAndExtremeOffsetStaysOnScreen() {
    StatusSettings s = new StatusSettings();
    for (int w : new int[]{320, 854, 1920}) for (int h : new int[]{240, 480})
      for (int p = 0; p < 9; p++) for (int offset : new int[]{-4096, 0, 4096}) {
        s.position = p; s.offsetX = offset; s.offsetY = offset;
        var layout = StatusLayout.at(w, h, 180, 8, s);
        assertTrue(layout.x() >= 2 && layout.x() + 180 <= w - 2);
        assertTrue(layout.y() >= 2 && layout.y() + 80 <= h - 2);
        assertTrue(layout.lineX(180, 50) >= layout.x());
        assertTrue(layout.lineX(180, 50) + 50 <= layout.x() + 180);
      }
  }
  @Test void invalidSettingsAreRejectedAndOpacityIsSeparateFromColour() {
    StatusSettings s = new StatusSettings();
    s.textColor = "#123456"; s.opacity = 50;
    assertEquals(0x80123456, s.argb());
    s.opacity = 0; assertEquals(0x00123456, s.argb());
    s.opacity = 101; assertThrows(IllegalArgumentException.class, s::validate);
    s.opacity = 100; s.offsetX = Integer.MIN_VALUE;
    assertThrows(IllegalArgumentException.class, s::validate);
    s.offsetX = 0; s.textColor = "bad";
    assertThrows(IllegalArgumentException.class, s::validate);
  }
}
