package local.luke.power.recipes.planner;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class RecipeFillPlannerTest {
  private RecipeStack stack(String item,int count){return new RecipeStack(item,0,item,count,64,true);}
  private RecipeIngredient ingredient(String...items){return new RecipeIngredient(Arrays.stream(items).map(item->new RecipeIngredient.Option(item,0,null)).toList());}
  private List<RecipeStack> inventory(RecipeStack...items){List<RecipeStack> slots=new ArrayList<>(Collections.nCopies(36,null));for(int i=0;i<items.length;i++)slots.set(i,items[i]);return slots;}
  @Test void overlappingAlternativesUseAnAugmentingAssignmentInsteadOfGreedyLoss() {
    var match=RecipeMatcher.match(List.of(ingredient("a","b"),ingredient("a")),List.of(stack("a",1),stack("b",1)),1,Arrays.asList(null,null));
    assertNotNull(match);assertEquals("b",match.supplies().get(match.cells()[0]).representative().item());
    assertEquals("a",match.supplies().get(match.cells()[1]).representative().item());
  }
  @Test void aCellCannotMergeDifferentNativeComponentsToReachItsRequestedCount() {
    var tagged=new RecipeStack("a",0,"a-custom",1,64,true);
    assertNull(RecipeMatcher.match(List.of(ingredient("a")),List.of(stack("a",1),tagged),2,Collections.singletonList(null)));
  }
  @Test void matchingGridAddsOneWhileShiftFillsTheNativeMaximum() {
    var main=inventory(stack("a",20));var grid=List.of(stack("a",3));var cells=List.of(ingredient("a"));
    assertEquals(4,RecipeFillPlanner.plan(main,null,grid,cells,0,true,true,false).amount());
    assertEquals(23,RecipeFillPlanner.plan(main,null,grid,cells,0,true,true,true).amount());
  }
  @Test void offhandArmorCursorAndOutputAreNeverIngredientSources() {
    var plan=RecipeFillPlanner.plan(inventory(),stack("a",64),Collections.singletonList(null),List.of(ingredient("a")),0,true,false,false);
    assertTrue(plan.ghost());assertEquals(64,plan.offhand().count());assertNull(plan.grid().get(0));
  }
  @Test void clearingUsesMatchingOccupiedOffhandButNeverCreatesAnOffhandBuffer() {
    var full=inventory();for(int i=0;i<36;i++)full.set(i,stack("other",64));
    var cells=List.of(ingredient("missing"));var grid=List.of(stack("a",2));
    assertNull(RecipeFillPlanner.plan(full,null,grid,cells,0,true,false,false));
    var returned=RecipeFillPlanner.plan(full,stack("a",60),grid,cells,0,true,false,false);
    assertNotNull(returned);assertTrue(returned.ghost());assertEquals(62,returned.offhand().count());
    assertNull(RecipeFillPlanner.plan(full,stack("a",60),grid,cells,0,false,false,false));
  }
  @Test void matchingGridWithoutGrowthStaysUntouchedEvenIfItsClearWouldMergeIntoOffhand() {
    var full=inventory();for(int i=0;i<36;i++)full.set(i,stack("other",64));
    var plan=RecipeFillPlanner.plan(full,stack("a",60),List.of(stack("a",2)),List.of(ingredient("a")),0,true,true,false);
    assertNotNull(plan);assertFalse(plan.ghost());assertEquals(full,plan.main());
    assertEquals(List.of(stack("a",2)),plan.grid());assertEquals(stack("a",60),plan.offhand());
  }
  @Test void matchingShapelessPermutationUsesCanonicalIngredientOrder() {
    var plan=RecipeFillPlanner.plan(inventory(stack("a",3),stack("b",3)),null,List.of(stack("b",1),stack("a",1)),
        List.of(ingredient("a"),ingredient("b")),0,true,true,false);
    assertNotNull(plan);assertFalse(plan.ghost());assertEquals(List.of(stack("a",2),stack("b",2)),plan.grid());
  }
  @Test void matchingMirroredShapePlacesCanonicalInputs() {
    var plan=RecipeFillPlanner.plan(inventory(stack("a",3),stack("b",3)),null,
        Arrays.asList(stack("b",1),stack("a",1),null,null),
        Arrays.asList(ingredient("a"),ingredient("b"),null,null),0,true,true,false);
    assertNotNull(plan);assertEquals(Arrays.asList(stack("a",2),stack("b",2),null,null),plan.grid());
  }
  @Test void matchingOffsetShapeDoesNotPinInputsToTheirOldSlots() {
    var plan=RecipeFillPlanner.plan(inventory(stack("a",3),stack("b",3)),null,
        Arrays.asList(null,null,null,null,stack("a",1),stack("b",1),null,null,null),
        Arrays.asList(ingredient("a"),ingredient("b"),null,null,null,null,null,null,null),0,true,true,false);
    assertNotNull(plan);assertEquals(Arrays.asList(stack("a",2),stack("b",2),null,null,null,null,null,null,null),plan.grid());
  }
  @Test void matchingUnevenGridCannotShrinkWhenAnyInputCannotGrow() {
    var grid=List.of(stack("a",63),stack("b",1));var cells=List.of(ingredient("a"),ingredient("b"));
    var plain=RecipeFillPlanner.plan(inventory(),null,grid,cells,0,true,true,false);
    var maximum=RecipeFillPlanner.plan(inventory(stack("b",63)),null,grid,cells,0,true,true,true);
    assertEquals(grid,plain.grid());assertEquals(inventory(),plain.main());assertFalse(plain.ghost());
    assertEquals(grid,maximum.grid());assertEquals(inventory(stack("b",63)),maximum.main());assertFalse(maximum.ghost());
    assertEquals(List.of(stack("a",63),stack("b",1)),grid);
  }
  @Test void matchingUnevenGridUsesTheSmallestCountPlusOneWhenEveryInputCanGrow() {
    var plan=RecipeFillPlanner.plan(inventory(stack("a",2),stack("b",2)),null,List.of(stack("a",2),stack("b",1)),
        List.of(ingredient("a"),ingredient("b")),0,true,true,false);
    assertNotNull(plan);assertEquals(List.of(stack("a",2),stack("b",2)),plan.grid());
  }
  @Test void maximumPlacementCanChooseAnotherAcceptedIngredient() {
    var plan=RecipeFillPlanner.plan(inventory(stack("b",32)),null,List.of(stack("a",1)),
        List.of(ingredient("a","b")),0,true,true,true);
    assertNotNull(plan);assertEquals(List.of(stack("b",32)),plan.grid());assertEquals(stack("a",1),plan.main().get(0));
  }
  @Test void unavailableClickClearsTheGridOnlyAfterCompleteCapacityPlanning() {
    var old=List.of(stack("b",1));var main=inventory();
    var plan=RecipeFillPlanner.plan(main,null,old,List.of(ingredient("a")),0,true,false,false);
    assertTrue(plan.ghost());assertEquals("b",plan.main().get(0).item());assertNull(plan.grid().get(0));
    assertEquals(1,old.get(0).count());assertNull(main.get(0));
  }
  @Test void taggedDamagedAndOversizedStacksRetainTheirDataAndCounts() {
    var excluded=new RecipeStack("a",4,"native-damaged",1,1,false);
    var plan=RecipeFillPlanner.plan(inventory(excluded),null,Collections.singletonList(null),List.of(ingredient("a")),0,true,false,true);
    assertTrue(plan.ghost());assertEquals(excluded,plan.main().get(0));
    var oversized=stack("a",100);
    var maximum=RecipeFillPlanner.plan(inventory(oversized),null,Collections.singletonList(null),List.of(ingredient("a")),0,true,false,true);
    assertEquals(64,maximum.amount());assertEquals(36,maximum.main().get(0).count());assertEquals(100,oversized.count());
  }
  @Test void nativeItemLimitsConstrainMaximumFill() {
    var narrow=new RecipeStack("bucket",0,"bucket",16,16,true);
    var plan=RecipeFillPlanner.plan(inventory(narrow),null,Collections.singletonList(null),List.of(ingredient("bucket")),0,true,false,true);
    assertEquals(16,plan.amount());
  }
  @Test void furnaceReturnsFinishedOutputWithFullIdentityAndCount() {
    var output=new RecipeStack("finished",5,"full-native-data",117,64,true);
    var plan=RecipeFillPlanner.plan(inventory(stack("a",1)),null,List.of(stack("old",1)),
        List.of(ingredient("a")),0,true,false,false,output);
    assertNotNull(plan);assertNull(plan.output());assertEquals(List.of(stack("a",1)),plan.grid());
    var returned=plan.main().stream().filter(Objects::nonNull).filter(item->item.identity().equals(output.identity())).toList();
    assertEquals(117,returned.stream().mapToInt(RecipeStack::count).sum());
    assertTrue(returned.stream().allMatch(item->item.damage()==5&&item.components().equals(output.components())&&item.count()<=64));
    assertEquals(117,output.count());
  }
  @Test void returnedFurnaceOutputCanSupplyAnotherRecipeAfterMainStoragePreflight() {
    var output=stack("a",5);
    var plan=RecipeFillPlanner.plan(inventory(),null,Collections.singletonList(null),
        List.of(ingredient("a")),0,true,false,true,output);
    assertNotNull(plan);assertFalse(plan.ghost());assertEquals(List.of(output),plan.grid());
    assertEquals(inventory(),plan.main());assertNull(plan.output());
  }
  @Test void furnaceOutputReturnedOnlyToOffhandCannotSupplyIngredients() {
    var main=inventory();for(int i=0;i<36;i++)main.set(i,stack("other",64));
    var output=stack("a",4);var grid=Collections.<RecipeStack>singletonList(null);
    var plan=RecipeFillPlanner.plan(main,stack("a",60),grid,List.of(ingredient("a")),0,true,false,true,output);
    assertNotNull(plan);assertTrue(plan.ghost());assertEquals(grid,plan.grid());assertEquals(stack("a",64),plan.offhand());
    assertNull(plan.output());assertEquals(main,plan.main());
    var supply=RecipeFillPlanner.supply(main,stack("a",60),grid,0,true,output);
    assertTrue(supply.stream().filter(Objects::nonNull).noneMatch(item->item.item().equals("a")));
  }
  @Test void furnaceOutputPartlyReturnedToOffhandSuppliesOnlyTheMainStoragePart() {
    var plan=RecipeFillPlanner.plan(inventory(),stack("a",60),Collections.singletonList(null),
        List.of(ingredient("a")),0,true,false,true,stack("a",5));
    assertNotNull(plan);assertEquals(List.of(stack("a",1)),plan.grid());assertEquals(stack("a",64),plan.offhand());
    assertEquals(inventory(),plan.main());assertNull(plan.output());
  }
  @Test void furnaceOutputMustFitBeforeAnyInputIsConsumed() {
    var main=inventory();for(int i=0;i<36;i++)main.set(i,stack("a",64));
    assertNull(RecipeFillPlanner.plan(main,null,Collections.singletonList(null),List.of(ingredient("a")),
        0,true,false,false,stack("finished",1)));
    assertEquals(36*64,main.stream().mapToInt(RecipeStack::count).sum());
  }
  @Test void furnaceNoGrowthKeepsFinishedOutputInItsNativeSlot() {
    var output=stack("finished",5);var grid=List.of(stack("a",1));
    var plan=RecipeFillPlanner.plan(inventory(),null,grid,List.of(ingredient("a")),0,true,true,true,output);
    assertNotNull(plan);assertEquals(output,plan.output());assertEquals(grid,plan.grid());assertEquals(inventory(),plan.main());
  }
}
