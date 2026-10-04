package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AudioToolbarTest {
  @Test void buttonsNeverOverlapAndLeaveSpaceBetweenGroups() {
    for (int width = 200; width <= 1600; width++) {
      for (boolean queue : new boolean[]{false,true}) {
        var buttons = AudioToolbar.layout(37,width,queue,true,true);
        for (int i = 1; i < buttons.size(); i++)
          assertTrue(buttons.get(i).x() > buttons.get(i-1).x()+buttons.get(i-1).width(), "width " + width);
        assertTrue(buttons.get(3).x() - buttons.get(2).x() - buttons.get(2).width() >= 8);
        var last = buttons.get(buttons.size()-1);
        assertEquals(37+width,last.x()+last.width());
        assertTrue(last.width() <= 60);
      }
    }
  }

  @Test void queueAppearingNeverMovesOtherActions() {
    for (int width : new int[]{200,224,230,359,360,666}) {
      var hidden = AudioToolbar.layout(0,width,false,false,false);
      var shown = AudioToolbar.layout(0,width,true,false,false);
      assertEquals(5,hidden.size());assertEquals(6,shown.size());
      for (var button : hidden) assertTrue(shown.contains(button));
      assertEquals(AudioToolbar.Action.QUEUE,shown.get(3).action());
    }
  }
}
