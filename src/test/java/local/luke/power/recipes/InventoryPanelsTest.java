package local.luke.power.recipes;

import java.util.List;
import local.luke.power.input.InventoryPanels;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryPanelsTest {
  @Test void extrasAndModalOwnersDoNotReplaceTheRecipeOrigin() {
    Object screen=new Object(),recipe=new Object(),hotbars=new Object();
    InventoryPanels.update(screen,77,List.of(new InventoryPanels.Rect(10,10,20,20)));
    InventoryPanels.extra(screen,hotbars,List.of(new InventoryPanels.Rect(100,100,40,40)));
    InventoryPanels.modal(screen,hotbars,true);
    assertEquals(77,InventoryPanels.offset(screen));assertTrue(InventoryPanels.contains(screen,15,15));assertTrue(InventoryPanels.contains(screen,110,110));
    assertTrue(InventoryPanels.modalOther(screen,recipe));assertFalse(InventoryPanels.modalOther(screen,hotbars));
    InventoryPanels.update(screen,0,List.of());assertTrue(InventoryPanels.contains(screen,110,110));
    InventoryPanels.removeExtra(screen,hotbars);assertFalse(InventoryPanels.contains(screen,110,110));
    InventoryPanels.modal(screen,hotbars,false);assertFalse(InventoryPanels.modal(screen));InventoryPanels.remove(screen);
  }
}
