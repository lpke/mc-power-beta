package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WorldMapControlsTest {
  private static final String[] LABELS = {"Light: Off", "Slime: Off", "Mobs: Off", "Recent path: Off", "Waypoints", "Settings", "Surface", "Overworld", "Center", "Go to", "Close"};

  @Test void nativeAndSidecarWidthsKeepBothRowsAndRightActionsInsideTheirPane() {
    for (int[] pane : new int[][]{{0,320},{0,400},{0,640},{0,1920},{336,464},{403,557},{430,1490}}) {
      var layout=WorldMapControls.at(pane[0],pane[1],360,LABELS,s->s.length()*6);
      for (var button:layout.buttons()) {
        var b=button.bounds();
        assertTrue(b.x()>=pane[0]+8);
        assertTrue(b.x()+b.width()<=pane[0]+pane[1]-8);
        assertTrue(b.width()>=button.label().length()*6+12,button.label());
        assertSame(button,layout.at(b.x(),b.y()));
        assertSame(button,layout.at(b.x()+b.width()-1,b.y()+b.height()-1));
        assertFalse(b.intersects(layout.scale()),"scale overlaps "+button.label());
        for(var other:layout.buttons())if(other!=button)assertFalse(b.intersects(other.bounds()),button.label()+" overlaps "+other.label());
      }
      var waypoint=layout.button(WorldMapControls.Action.WAYPOINTS).bounds();
      var light=layout.button(WorldMapControls.Action.LIGHT).bounds();
      var close=layout.button(WorldMapControls.Action.CLOSE).bounds();
      assertEquals(pane[0]+8,waypoint.x()); assertEquals(waypoint.x(),light.x());
      assertEquals(22,waypoint.y()-light.y()); assertEquals(336,close.y());
      assertEquals(pane[0]+pane[1]-8,close.x()+close.width());
      var slime=layout.button(WorldMapControls.Action.SLIME).bounds();
      var mobs=layout.button(WorldMapControls.Action.MOBS).bounds();
      assertEquals(slime.x()+slime.width()+4,mobs.x()); assertEquals(slime.y(),mobs.y());
    }
  }

  @Test void closeSettingsRetainsPaddingAndAWholeLeftBlockWrapsWhenNeeded() {
    String[] labels=LABELS.clone();labels[5]="Close settings";
    var narrow=WorldMapControls.at(0,320,360,labels,s->s.length()*6);
    assertEquals(314,narrow.button(WorldMapControls.Action.WAYPOINTS).bounds().y());
    assertEquals(336,narrow.button(WorldMapControls.Action.CLOSE).bounds().y());
    assertEquals(96,narrow.button(WorldMapControls.Action.SETTINGS).bounds().width());
    var wide=WorldMapControls.at(0,960,360,labels,s->s.length()*6);
    assertEquals(336,wide.button(WorldMapControls.Action.WAYPOINTS).bounds().y());
    assertEquals(314,wide.button(WorldMapControls.Action.LIGHT).bounds().y());
  }
}
