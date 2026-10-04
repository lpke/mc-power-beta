package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AudioToolbarTest {
  @Test void buttonsStayInsideThePanelWithoutOverlapping() {
    for (int width = 200; width <= 1600; width++) {
      for (boolean library : new boolean[]{false,true}) {
        var layout = AudioToolbar.layout(37,width,256,true,library);
        var buttons = layout.buttons();
        for (int i = 0; i < buttons.size(); i++) {
          var button = buttons.get(i);
          assertTrue(button.x() >= 37 && button.x()+button.width() <= 37+width);
          assertTrue(button.y()+18 <= layout.bottom());
          for (int j = i+1; j < buttons.size(); j++) {
            var other = buttons.get(j);
            if(button.y()==other.y()) assertTrue(other.x() > button.x()+button.width(),"width "+width);
          }
        }
        var next=buttons.stream().filter(b -> b.action()==AudioToolbar.Action.NEXT).findFirst().orElseThrow();
        var queue=buttons.stream().filter(b -> b.action()==AudioToolbar.Action.QUEUE).findFirst().orElseThrow();
        if(next.y()==queue.y()) assertTrue(queue.x() - next.x() - next.width() >= 12);
        assertEquals(AudioToolbar.bottom(width,library),layout.bottom());
        var last=buttons.get(buttons.size()-1);
        assertEquals(37+width,last.x()+last.width());
        assertEquals(AudioToolbar.Action.RELOAD,last.action());
      }
    }
  }

  @Test void queueCountNeverMovesActionsAndEmptyQueueIsDisabled() {
    for (int width : new int[]{200,244,291,342,365,366,465,466,666}) {
      for(boolean library : new boolean[]{false,true}) {
        var empty = AudioToolbar.layout(0,width,0,false,library).buttons();
        var full = AudioToolbar.layout(0,width,256,false,library).buttons();
        assertEquals(library?7:6,empty.size());
        for(int i=0;i<empty.size();i++) {
          var a=empty.get(i);var b=full.get(i);
          assertEquals(a.x(),b.x());assertEquals(a.y(),b.y());assertEquals(a.width(),b.width());
          if(a.action()==AudioToolbar.Action.QUEUE) {
            assertFalse(a.enabled());assertTrue(b.enabled());
            assertEquals("Queue (0)",a.label());assertEquals("Queue (256)",b.label());
          }
        }
        assertEquals(AudioToolbar.Action.QUEUE,empty.get(empty.size()-3).action());
        assertEquals(AudioToolbar.Action.LIBRARY,empty.get(empty.size()-2).action());
        if(library) assertEquals("Back to Settings",empty.get(0).label());
      }
    }
  }
}
