package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SectionCollapseTest {
  @Test void wholeTabOverridesMixedStatesWithoutChangingOtherTabs() {
    var collapsed = new HashSet<>(Set.of("Interface/HUD", "Audio/Volume"));
    var groups = List.of("Interface/HUD", "Interface/Chat", "Interface/Custom HUD info");
    assertFalse(SectionCollapse.allCollapsed(groups, collapsed));
    SectionCollapse.toggleAll(groups, collapsed);
    assertTrue(SectionCollapse.allCollapsed(groups, collapsed));
    SectionCollapse.toggleAll(groups, collapsed);
    assertEquals(Set.of("Audio/Volume"), collapsed);
    SectionCollapse.toggleAll(List.of(), collapsed);
    assertEquals(Set.of("Audio/Volume"), collapsed);
  }
}
