package local.luke.power.recipes;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecipeBookLayoutTest {
  @Test void modernNarrowThresholdAndNativeOffsetShareTheirHitGeometry() {
    var narrow=new RecipeBookLayout(378,240,true,false);var wide=new RecipeBookLayout(379,240,true,false);
    assertTrue(narrow.narrow());assertEquals(0,narrow.offset());assertFalse(wide.narrow());assertEquals(77,wide.offset());
    assertEquals((379-176)/2+77,wide.inventoryX());assertEquals((379-147)/2-86,wide.x());
    assertTrue(wide.cell(19).contains(wide.x()+111,wide.y()+106));
  }
  @Test void closedAndPauseViewsDoNotShiftNativeInventories() {
    assertEquals(0,new RecipeBookLayout(800,400,false,false).offset());
    var pause=new RecipeBookLayout(800,400,true,true);assertEquals(0,pause.offset());assertEquals((800-311)/2+30,pause.x());
  }
  @Test void offhandExtensionHasRoomBesideTheBookAndRaisesTheWideThreshold() {
    var narrow=new RecipeBookLayout(400,240,true,false,22);
    var threshold=new RecipeBookLayout(401,240,true,false,22);
    assertTrue(narrow.narrow());assertEquals(0,narrow.offset());assertEquals((400-147)/2,narrow.x());
    assertFalse(threshold.narrow());assertEquals(88,threshold.offset());assertTrue(threshold.tab(0).x()>=0);
    for(int width:new int[]{401,640}){
      var layout=new RecipeBookLayout(width,360,true,false,22);
      var ordinary=new RecipeBookLayout(width,360,true,false);
      assertEquals(ordinary.x()-11,layout.x());assertEquals(ordinary.inventoryX()+11,layout.inventoryX());
      int extensionLeft=layout.inventoryX()-22,bookRight=layout.background().x()+layout.background().width();
      assertTrue(bookRight<=extensionLeft,"Book must leave the offhand extension visible");
      assertEquals(ordinary.inventoryX()-(ordinary.x()+147)+22,layout.inventoryX()-bookRight);
      assertTrue(layout.inventoryX()+176<=width);
    }
    assertEquals(0,new RecipeBookLayout(640,360,false,false,22).offset());
    assertEquals(new RecipeBookLayout(640,360,true,true).x(),new RecipeBookLayout(640,360,true,true,22).x());
  }
  @Test void pauseViewerKeepsJoinedPanelsTabsAndDoneInsideSmallAndWideWindows() {
    for(int[] size:new int[][]{{320,240},{427,240},{854,480}}){
      var layout=new RecipeBookLayout(size[0],size[1],true,true);
      assertTrue(layout.tab(0).x()>=0);assertTrue(layout.detail().x()+layout.detail().width()<=size[0]);
      assertEquals(layout.background().x()+layout.background().width()-4,layout.detail().x());
      assertEquals(layout.background().y(),layout.detail().y());assertEquals(layout.background().height(),layout.detail().height());
      assertTrue(layout.kindTab(0).y()>=22);assertTrue(layout.kindTab(1).x()+layout.kindTab(1).width()<size[0]);
      assertTrue(layout.kindTab(0).y()+layout.kindTab(0).height()<=layout.background().y());
      assertTrue(layout.background().y()+layout.background().height()<=layout.done().y());
      assertTrue(layout.done().y()+layout.done().height()<=size[1]);
    }
  }
  @Test void wheelPagesInBothDirectionsAndStopsAtTheEnds() {
    assertEquals(1,RecipeBookLayout.pageAfterScroll(0,61,-120));
    assertEquals(1,RecipeBookLayout.pageAfterScroll(2,61,120));
    assertEquals(0,RecipeBookLayout.pageAfterScroll(0,61,120));
    assertEquals(3,RecipeBookLayout.pageAfterScroll(3,61,-120));
    assertEquals(0,RecipeBookLayout.pageAfterScroll(0,0,-120));
    assertEquals(0,RecipeBookLayout.pageAfterScroll(0,20,-120));
    assertEquals(1,RecipeBookLayout.pageAfterScroll(1,61,0));
  }
}
