package local.luke.power.recipes;

import java.util.*;
import local.luke.power.recipes.planner.RecipeStack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecipeAvailabilityTest {
  private static List<RecipeStack> supply(int count){return List.of(new RecipeStack("stone",0,"stone",count,64,true));}
  @Test void changedInventoryPublishesAllRecipesTogetherWithoutBlankingTheOldList() {
    var availability=new RecipeAvailability<>(List.of("first","last"));
    availability.sample(supply(2));availability.refresh(2,(recipe,supply)->true);
    assertTrue(availability.current());
    availability.sample(supply(1));
    availability.refresh(1,(recipe,supply)->false);
    assertFalse(availability.current());
    assertTrue(availability.available("first"));assertTrue(availability.available("last"));
    availability.refresh(1,(recipe,supply)->false);
    assertTrue(availability.current());
    assertFalse(availability.available("first"));assertFalse(availability.available("last"));
  }
  @Test void repeatedFillsRetainCraftableRowsAndDoNotRestartAnUnfinishedBatch() {
    var availability=new RecipeAvailability<>(List.of("first","middle","last"));
    availability.sample(supply(3));availability.refresh(3,(recipe,supply)->true);
    List<Integer> counts=new ArrayList<>();
    availability.sample(supply(2));availability.refresh(1,(recipe,supply)->{counts.add(supply.get(0).count());return true;});
    availability.sample(supply(1));availability.refresh(2,(recipe,supply)->{counts.add(supply.get(0).count());return true;});
    assertEquals(List.of(2,2,2),counts);assertFalse(availability.current());
    for(String recipe:List.of("first","middle","last"))assertTrue(availability.available(recipe));
    availability.refresh(3,(recipe,supply)->supply.get(0).count()==1);
    assertTrue(availability.current());assertTrue(availability.available("last"));
  }
  @Test void unsupportedInventoryClearsAvailabilityAndCanRecoverOnTheSameSupply() {
    var availability=new RecipeAvailability<>(List.of("recipe"));
    availability.sample(supply(1));availability.refresh(1,(recipe,supply)->true);
    availability.unavailable();assertFalse(availability.current());assertFalse(availability.available("recipe"));
    availability.sample(supply(1));availability.refresh(1,(recipe,supply)->true);
    assertTrue(availability.current());assertTrue(availability.available("recipe"));
  }
}
