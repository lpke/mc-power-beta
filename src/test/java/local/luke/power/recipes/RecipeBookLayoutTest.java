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
    var pause=new RecipeBookLayout(800,400,true,true);assertEquals(0,pause.offset());assertEquals((800-147)/2,pause.x());
  }
}
