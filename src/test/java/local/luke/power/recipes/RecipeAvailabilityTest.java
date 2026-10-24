package local.luke.power.recipes;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
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
  @Test void cheapStartupPassCompletesHundredsOfRecipesInOneTickInsteadOfTwenty() {
    var recipes=IntStream.range(0,480).boxed().toList();
    var legacy=new RecipeAvailability<>(recipes);legacy.sample(supply(1));
    int legacyTicks=0;
    while(!legacy.current()){legacy.refresh(24,(recipe,supply)->recipe==479);legacyTicks++;}
    assertEquals(20,legacyTicks);
    var bounded=new RecipeAvailability<>(recipes);bounded.sample(supply(1));
    var clock=new AtomicLong();
    bounded.refresh(2048,1_500_000,clock::get,(recipe,supply)->{clock.addAndGet(1_000);return recipe==479;});
    assertTrue(bounded.current());assertTrue(bounded.available(479));assertFalse(bounded.available(0));
  }
  @Test void expensiveChecksYieldAtTheTimeLimitAndPublishOnlyACompletedSnapshot() {
    var availability=new RecipeAvailability<>(List.of("first","middle","last"));
    availability.sample(supply(1));var clock=new AtomicLong();var checked=new ArrayList<String>();
    for(int turn=0;turn<3;turn++) {
      availability.refresh(2048,1_500_000,clock::get,(recipe,supply)->{checked.add(recipe);clock.addAndGet(2_000_000);return true;});
      assertEquals(turn+1,checked.size());
      assertEquals(turn==2,availability.current());assertEquals(turn==2,availability.available("first"));
    }
    assertEquals(List.of("first","middle","last"),checked);
  }
  @Test void snapshotCopiesItsSourceAndUnchangedSamplingDoesNotRescan() {
    var availability=new RecipeAvailability<>(List.of("recipe"));var source=new ArrayList<>(supply(1));
    availability.sample(source);source.set(0,supply(2).get(0));
    availability.refresh(2048,(recipe,supply)->supply.get(0).count()==1);
    assertTrue(availability.current());assertTrue(availability.available("recipe"));
    availability.sample(supply(1));
    availability.refresh(2048,(recipe,supply)->{fail("unchanged supply must retain completed results");return false;});
    assertTrue(availability.current());
  }
}
