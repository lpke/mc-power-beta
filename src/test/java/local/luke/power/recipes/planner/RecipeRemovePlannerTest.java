package local.luke.power.recipes.planner;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class RecipeRemovePlannerTest {
  private RecipeStack stack(String item,int count){return new RecipeStack(item,0,item,count,64,true);}
  private List<RecipeStack> inventory(RecipeStack...items){
    List<RecipeStack> slots=new ArrayList<>(Collections.nCopies(36,null));
    for(int i=0;i<items.length;i++)slots.set(i,items[i]);return slots;
  }
  private Map<String,Integer> totals(List<RecipeStack> main,RecipeStack offhand,List<RecipeStack> grid){
    Map<String,Integer> totals=new HashMap<>();List<RecipeStack> all=new ArrayList<>(main);all.add(offhand);all.addAll(grid);
    for(var stack:all)if(stack!=null)totals.merge(stack.identity(),stack.count(),Integer::sum);return totals;
  }
  @Test void lastBatchReturnsInputsAndLeavesNoCraftedOutput(){
    var grid=Arrays.asList(stack("a",1),null,stack("b",1),null);
    var plan=RecipeFillPlanner.removeOne(inventory(),null,grid,0,true,true);
    assertNotNull(plan);assertEquals(Collections.nCopies(4,null),plan.grid());assertEquals(0,plan.amount());
    assertNull(plan.output());assertFalse(plan.ghost());
    assertEquals(totals(inventory(),null,grid),totals(plan.main(),plan.offhand(),plan.grid()));
  }
  @Test void repeatedRemovalKeepsMirroredOffsetInputsInTheirNativePositions(){
    var grid=Arrays.asList(null,null,null,null,stack("b",3),stack("a",3),null,null,null);
    var plan=RecipeFillPlanner.removeOne(inventory(),null,grid,0,true,true);
    assertNotNull(plan);assertEquals(Arrays.asList(null,null,null,null,stack("b",2),stack("a",2),null,null,null),plan.grid());
    assertEquals(2,plan.amount());
    var second=RecipeFillPlanner.removeOne(plan.main(),plan.offhand(),plan.grid(),0,true,true);
    assertEquals(1,second.amount());assertEquals(3,grid.get(4).count());
    assertEquals(totals(inventory(),null,grid),totals(second.main(),second.offhand(),second.grid()));
  }
  @Test void fullNativeDamageAndComponentsStaySeparateAndOversizedInputsLoseOnlyOne(){
    var first=new RecipeStack("a",7,"tag-one","native-nbt-one",100,64,false);
    var second=new RecipeStack("a",7,"tag-two","native-nbt-two",3,64,false);
    var grid=List.of(first,second);var plan=RecipeFillPlanner.removeOne(inventory(),null,grid,0,true,true);
    assertNotNull(plan);assertEquals(List.of(first.count(99),second.count(2)),plan.grid());
    assertEquals(first.count(1),plan.main().get(0));assertEquals(second.count(1),plan.main().get(1));
    assertEquals(totals(inventory(),null,grid),totals(plan.main(),plan.offhand(),plan.grid()));
    assertEquals(100,first.count());assertEquals(3,second.count());
  }
  @Test void capacityFailureCannotPartlyReturnAnEarlierIngredient(){
    var full=inventory();for(int i=0;i<36;i++)full.set(i,stack("other",64));full.set(0,stack("a",63));
    var grid=List.of(stack("a",2),stack("b",2));
    assertNull(RecipeFillPlanner.removeOne(full,null,grid,0,true,true));
    assertEquals(63,full.get(0).count());assertEquals(List.of(stack("a",2),stack("b",2)),grid);
  }
  @Test void returnsUseSelectedThenOccupiedOffhandThenMainStorage(){
    var main=inventory(stack("a",62),stack("a",62));main.set(8,stack("a",63));
    var plan=RecipeFillPlanner.removeOne(main,stack("a",63),List.of(stack("a",3),stack("a",3),stack("a",3)),8,true,true);
    assertNotNull(plan);assertEquals(64,plan.main().get(8).count());assertEquals(64,plan.offhand().count());
    assertEquals(63,plan.main().get(0).count());assertEquals(62,plan.main().get(1).count());
    assertEquals(totals(main,stack("a",63),List.of(stack("a",3),stack("a",3),stack("a",3))),totals(plan.main(),plan.offhand(),plan.grid()));
  }
  @Test void offhandOnlyAcceptsMatchingExistingDataWhenEnabled(){
    var full=inventory();for(int i=0;i<36;i++)full.set(i,stack("other",64));var grid=List.of(stack("a",2));
    assertNull(RecipeFillPlanner.removeOne(full,null,grid,0,true,true));
    assertNull(RecipeFillPlanner.removeOne(full,stack("a",63),grid,0,false,true));
    assertNull(RecipeFillPlanner.removeOne(full,new RecipeStack("a",0,"other-tag",63,64,true),grid,0,true,true));
    var plan=RecipeFillPlanner.removeOne(full,stack("a",63),grid,0,true,true);
    assertNotNull(plan);assertEquals(stack("a",64),plan.offhand());assertEquals(full,plan.main());assertEquals(List.of(stack("a",1)),plan.grid());
  }
  @Test void mismatchAndEmptyGridNeverReturnUnrelatedItems(){
    assertNull(RecipeFillPlanner.removeOne(inventory(),null,List.of(stack("unrelated",2)),0,true,false));
    assertNull(RecipeFillPlanner.removeOne(inventory(),null,Collections.nCopies(4,null),0,true,true));
  }
  @Test void unevenGridLosesOnePerOccupiedInputWithoutErasingTheRemainder(){
    var grid=List.of(stack("a",1),stack("b",3));var plan=RecipeFillPlanner.removeOne(inventory(),null,grid,0,true,true);
    assertNotNull(plan);assertEquals(Arrays.asList(null,stack("b",2)),plan.grid());assertEquals(0,plan.amount());
    assertEquals(totals(inventory(),null,grid),totals(plan.main(),plan.offhand(),plan.grid()));
    assertThrows(UnsupportedOperationException.class,()->plan.grid().set(0,stack("a",1)));
    assertThrows(UnsupportedOperationException.class,()->plan.main().set(0,null));
  }
}
