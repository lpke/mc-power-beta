package local.luke.power.map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MinimapQuickMenuLayoutTest {
  @Test void circularGearSitsOnTheTenThirtyEdgeAndUsesItsVisibleCircle() {
    var layout=MinimapQuickMenuLayout.at(new MinimapGeometry.Layout(200,10,108),true,320,240,130);
    assertEquals(216,layout.gear().x()); assertEquals(26,layout.gear().y());
    assertTrue(layout.gear().contains(216,26));
    assertTrue(layout.gear().contains(223,26));
    assertFalse(layout.gear().contains(223,33));
  }
  @Test void everyCornerAndGuiSizeKeepsRowsInsideTheWindow() {
    for(int width:new int[]{240,320,640})for(int height:new int[]{180,240,360})
      for(boolean circular:new boolean[]{false,true})for(int corner=0;corner<4;corner++) {
        var settings=new MapSettings();settings.corner=corner;
        var layout=MinimapQuickMenuLayout.at(MinimapGeometry.layout(settings,width,height,false),circular,width,height,130);
        var menu=layout.menu();
        assertTrue(menu.x()>=0&&menu.y()>=0&&menu.x()+menu.width()<=width&&menu.y()+menu.height()<=height);
        for(int row=0;row<4;row++) {
          var bounds=layout.row(row);
          assertEquals(row,layout.rowAt(bounds.x(),bounds.y()));
          assertEquals(row,layout.rowAt(bounds.x()+bounds.width()-1,bounds.y()+bounds.height()-1));
        }
        assertEquals(-1,layout.rowAt(menu.x(),menu.y()));
      }
  }
}
