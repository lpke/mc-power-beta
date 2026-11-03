package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import local.luke.power.map.MapTogglePanel;
import org.junit.jupiter.api.Test;

class WorldMapControlsTest {
  private static final String[] LABELS = {"Hide Toggles", "Waypoints", "Settings", "Surface", "Overworld", "Center", "Go to", "Close"};
  @Test void caveSliderAndResetStayBesideTheCurrentModeAtEveryPaneWidth() {
    for (int[] pane : new int[][] {{0,320},{0,400},{0,640},{0,1920},{336,200},{336,464}}) {
      var labels = LABELS.clone(); labels[3] = "Caves";
      var layout = WorldMapControls.at(pane[0],pane[1],240,labels,s->s.length()*6,true);
      var mode = layout.button(WorldMapControls.Action.LAYER).bounds();
      var slider = layout.caveSlider(); var reset = layout.caveReset();
      assertEquals(mode.x()+mode.width()+4,slider.x());
      assertEquals(slider.x()+slider.width()+4,reset.x());
      assertEquals(mode.y(),slider.y()); assertEquals(mode.y(),reset.y());
      assertTrue(slider.width()>=70);
      var boxes = new java.util.ArrayList<WorldMapControls.Box>();
      layout.buttons().forEach(button->boxes.add(button.bounds()));
      boxes.add(slider); boxes.add(reset); boxes.add(layout.scale());
      for (var box : boxes) {
        assertTrue(box.x()>=pane[0]+8 && box.x()+box.width()<=pane[0]+pane[1]-8);
        assertTrue(box.y()>=0 && box.y()+box.height()<=240);
        for (var other : boxes) if (other != box) assertFalse(box.intersects(other));
      }
      var surface = WorldMapControls.at(pane[0],pane[1],240,LABELS,s->s.length()*6,false);
      assertNull(surface.caveSlider()); assertNull(surface.caveReset());
    }
  }
  @Test void navigationAndScaleStayInsideTheirPaneWithoutOverlapping() {
    for(int[] pane:new int[][]{{0,320},{0,400},{0,640},{0,1920},{336,200},{336,464},{403,557}})
      for(int height:new int[]{240,360}) {
        var layout=WorldMapControls.at(pane[0],pane[1],height,LABELS,s->s.length()*6);
        for(var button:layout.buttons()) {
          var b=button.bounds();
          assertTrue(b.x()>=pane[0]+8);
          assertTrue(b.x()+b.width()<=pane[0]+pane[1]-8);
          assertTrue(b.width()>=10);
          assertSame(button,layout.at(b.x(),b.y()));
          assertSame(button,layout.at(b.x()+b.width()-1,b.y()+b.height()-1));
          assertFalse(b.intersects(layout.scale()));
          for(var other:layout.buttons())if(other!=button)assertFalse(b.intersects(other.bounds()));
        }
        var hide=layout.button(WorldMapControls.Action.TOGGLES).bounds();
        assertEquals(pane[0]+8,hide.x());
        assertEquals(height-24,layout.button(WorldMapControls.Action.CLOSE).bounds().y());
        assertEquals(pane[0]+pane[1]-8,layout.button(WorldMapControls.Action.CLOSE).bounds().x()+layout.button(WorldMapControls.Action.CLOSE).bounds().width());
        assertEquals(layout.buttons().stream().filter(b->b.action().ordinal()<5).mapToInt(b->b.bounds().y()).max().orElseThrow(),hide.y());
        if(pane[1]>=200)for(var button:layout.buttons())
          assertTrue(button.bounds().width()>=button.label().length()*6+WorldMapControls.TEXT_PADDING,
              "navigation label clipped: "+button.label());
      }
  }
  @Test void smallMapPaneKeepsToggleRowsAboveNavigationAndBelowReadouts() {
    var controls=WorldMapControls.at(336,200,240,LABELS,s->s.length()*6);
    int bottom=controls.scale().y()-4;
    var toggles=MapTogglePanel.at(344,bottom,184,bottom-64,144,0);
    assertTrue(toggles.bounds().y()>=64);
    assertTrue(toggles.bounds().y()+toggles.bounds().height()<controls.navigationTop());
    assertTrue(toggles.visible()>0&&toggles.visible()<10);
    for(int i=0;i<10;i++) {
      var scrolled=MapTogglePanel.at(344,bottom,184,bottom-64,144,i);
      for(int row=scrolled.first();row<scrolled.first()+scrolled.visible();row++)
        assertEquals(row,scrolled.rowAt(scrolled.row(row).x(),scrolled.row(row).y()));
    }
  }
}
