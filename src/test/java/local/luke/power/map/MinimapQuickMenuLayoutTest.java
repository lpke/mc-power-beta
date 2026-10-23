package local.luke.power.map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MinimapQuickMenuLayoutTest {
  @Test void controlsFaceAwayFromTheMapAtAllFourAnchors() {
    for(int corner=0;corner<4;corner++) {
      var s=new MapSettings();s.corner=corner;
      var map=MinimapGeometry.layout(s,800,600,false);
      var layout=MinimapQuickMenuLayout.at(map,true,corner,800,600,130);
      boolean left=corner%2==0, top=corner<2;
      assertEquals(left,layout.gear().x()>map.x()+map.size()/2);
      assertEquals(top,layout.gear().y()<map.y()+map.size()/2);
      assertTrue(left?layout.menu().x()>=layout.gear().x()+layout.gear().width()
          :layout.menu().x()+layout.menu().width()<=layout.gear().x());
    }
  }
  @Test void everyCornerAndGuiSizeKeepsRowsInsideTheWindow() {
    for(int width:new int[]{240,320,640})for(int height:new int[]{180,240,360})
      for(boolean circular:new boolean[]{false,true})for(int corner=0;corner<4;corner++) {
        var settings=new MapSettings();settings.corner=corner;
        var layout=MinimapQuickMenuLayout.at(MinimapGeometry.layout(settings,width,height,false),circular,corner,width,height,130);
        var menu=layout.menu();
        assertTrue(menu.x()>=0&&menu.y()>=0&&menu.x()+menu.width()<=width&&menu.y()+menu.height()<=height);
        for(int row=0;row<MinimapQuickMenuLayout.ROWS;row++) {
          var bounds=layout.row(row);
          assertEquals(row,layout.rowAt(bounds.x(),bounds.y()));
          assertEquals(row,layout.rowAt(bounds.x()+bounds.width()-1,bounds.y()+bounds.height()-1));
        }
        assertEquals(-1,layout.rowAt(menu.x(),menu.y()));
      }
  }
}
