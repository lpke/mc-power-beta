package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DebugOverlayTest {
  @Test void overlappingProvidersAndNarrowColumnsStaySeparate() {
    for (int width : new int[]{320, 640}) {
      DebugOverlay.begin();
      DebugOverlay.add("Version and frame information", 2, 2);
      DebugOverlay.add("Memory information", width - 110, 2);
      DebugOverlay.add("Facing: north", 2, 144);
      DebugOverlay.add("Music: track", 2, 144);
      DebugOverlay.add("Culling", 2, 146);
      var rows = DebugOverlay.layout(width, text -> text.length() * 6);
      assertEquals(5, rows.size());
      for (int a=0; a<rows.size(); a++) for (int b=a+1; b<rows.size(); b++) {
        var x=rows.get(a); var y=rows.get(b);
        assertFalse(x.x()<y.x()+y.width() && x.x()+x.width()>y.x() && x.y()<y.y()+9 && x.y()+9>y.y());
      }
      DebugOverlay.begin();
      assertTrue(DebugOverlay.layout(width, text -> text.length()*6).isEmpty());
    }
  }
  @Test void cursorIsSteadyUnlessBlinkingIsEnabled() {
    try {
      InterfaceState.blinkingChatCursor=false;
      for(int tick=0;tick<36;tick++) assertTrue(InterfaceState.cursorVisible(tick));
      InterfaceState.blinkingChatCursor=true;
      assertTrue(InterfaceState.cursorVisible(0));
      assertFalse(InterfaceState.cursorVisible(6));
      assertTrue(InterfaceState.cursorVisible(12));
    } finally { InterfaceState.blinkingChatCursor=false; }
  }
}
