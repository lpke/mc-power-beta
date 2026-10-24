package local.luke.power.recipes.planner;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class RecipeMatcherTest {
  private RecipeStack stack(String identity,int count){return new RecipeStack("stone",0,identity,count,64,true);}
  private RecipeIngredient ingredient(){return new RecipeIngredient(List.of(new RecipeIngredient.Option("stone",0,null)));}
  @Test void preparedSupplyCopiesAndGroupsCountsWithoutCombiningNativeComponents() {
    var source=new ArrayList<>(List.of(stack("ordinary",2),stack("ordinary",3),stack("tagged",1)));
    var prepared=RecipeMatcher.prepare(source);source.clear();
    assertEquals(2,prepared.supplies().size());assertEquals(5,prepared.supplies().get(0).count());
    assertNotNull(RecipeMatcher.matchPrepared(List.of(ingredient()),prepared,5,Collections.singletonList(null)));
    assertNull(RecipeMatcher.matchPrepared(List.of(ingredient()),prepared,6,Collections.singletonList(null)));
    assertThrows(UnsupportedOperationException.class,()->prepared.supplies().clear());
  }
  @Test void repeatedPreparedQueriesRetainTheOriginalCountsAndAssignment() {
    var source=List.of(stack("ordinary",2),stack("tagged",1));var prepared=RecipeMatcher.prepare(source);
    var cells=List.of(ingredient(),ingredient());var required=Arrays.<String>asList(null,null);
    var expected=RecipeMatcher.match(cells,source,1,required);
    for(int query=0;query<100;query++) {
      var actual=RecipeMatcher.matchPrepared(cells,prepared,1,required);
      assertNotNull(actual);assertEquals(expected.supplies(),actual.supplies());assertArrayEquals(expected.cells(),actual.cells());
    }
    assertEquals(2,prepared.supplies().get(0).count());
    assertNull(RecipeMatcher.matchPrepared(cells,prepared,2,required));
  }
}
