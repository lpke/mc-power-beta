package local.luke.power.recipes.planner;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.*;
import org.junit.jupiter.api.Test;

class RecipeCraftPlannerTest {
  private RecipeStack item(String identity, int count) { return new RecipeStack(identity, 0, identity, count, 64, true); }
  private List<RecipeStack> inventory() { return new ArrayList<>(Collections.nCopies(36, null)); }
  private RecipeFillPlanner.Plan placed(List<RecipeStack> main, RecipeStack... grid) { return new RecipeFillPlanner.Plan(main, null, Arrays.asList(grid), 1, false); }
  private RecipeCraftPlanner.Exchange simple(RecipeStack result) {
    return inputs -> inputs.stream().allMatch(Objects::isNull) ? null : RecipeCraftPlanner.step(inputs, result, Map.of());
  }
  @Test void maximumConsumesTheWholeMaximalGridInOneImmutablePlan() throws Exception {
    var main = inventory(); main.set(9, item("planks", 128));
    var ingredient = new RecipeIngredient(List.of(new RecipeIngredient.Option("planks", 0, null)));
    var filled = RecipeFillPlanner.forCraft(main, null, Arrays.asList(null, null), List.of(ingredient, ingredient), 0, false);
    assertEquals(64, filled.grid().get(0).count());
    var plan = RecipeCraftPlanner.plan(filled, 0, false, true, simple(item("sticks", 4)));
    assertEquals(64, plan.steps().size()); assertTrue(plan.inventory().grid().stream().allMatch(Objects::isNull));
    assertEquals(256, plan.inventory().main().stream().filter(Objects::nonNull).mapToInt(RecipeStack::count).sum());
    assertEquals(128, main.get(9).count()); assertNull(main.get(0));
    assertThrows(UnsupportedOperationException.class, () -> plan.steps().clear());
    assertThrows(UnsupportedOperationException.class, () -> plan.steps().get(0).inputs().clear());
  }
  @Test void singleCraftStillConsumesOneNativeRecipeBatch() throws Exception {
    var plan = RecipeCraftPlanner.plan(placed(inventory(), item("a", 17)), 0, false, false, simple(item("result", 4)));
    assertEquals(1, plan.steps().size()); assertEquals(16, plan.inventory().grid().get(0).count()); assertEquals(4, plan.inventory().main().get(0).count());
  }
  @Test void partialCapacityAdmitsOnlyCompleteResultsAndKeepsRemainingInputs() throws Exception {
    var main = inventory(); for (int i = 0; i < 36; i++) main.set(i, item("occupied", 64)); main.set(0, item("result", 54));
    var plan = RecipeCraftPlanner.plan(placed(main, item("a", 64)), 0, false, true, simple(item("result", 4)));
    assertEquals(2, plan.steps().size()); assertEquals(62, plan.inventory().main().get(0).count()); assertEquals(62, plan.inventory().grid().get(0).count());
    assertEquals(54, main.get(0).count());
  }
  @Test void zeroCapacityHasNoPlanAndNeverConsumesPartOfABatch() throws Exception {
    var main = inventory(); for (int i = 0; i < 36; i++) main.set(i, item("occupied", 64)); main.set(0, item("result", 62));
    assertNull(RecipeCraftPlanner.plan(placed(main, item("a", 64)), 0, false, true, simple(item("result", 4))));
    assertEquals(62, main.get(0).count());
  }
  @Test void nativeDataAndContainerReturnsRemainExact() throws Exception {
    var tagged = new RecipeStack("milk", 7, "tagged-milk", "native-components", 1, 1, true);
    var bucket = new RecipeStack("bucket", 0, "bucket-identity", "bucket-components", 1, 16, true);
    var output = new RecipeStack("cake", 0, "cake-identity", "cake-components", 1, 1, true);
    var plan = RecipeCraftPlanner.plan(placed(inventory(), tagged), 0, false, true,
        inputs -> tagged.equals(inputs.get(0)) ? RecipeCraftPlanner.step(inputs, output, Map.of(tagged.identity(), bucket)) : null);
    assertEquals(1, plan.steps().size()); assertEquals(List.of(tagged), plan.steps().get(0).consumed());
    assertEquals(List.of(output, bucket), plan.steps().get(0).produced()); assertEquals(bucket, plan.inventory().grid().get(0));
    assertEquals(output, plan.inventory().main().get(0));
  }
  @Test void maximalFillUsesFullMatchingGridWithoutRequiringAnEmptyReturnSlot() {
    var main = inventory(); for (int i = 0; i < 36; i++) main.set(i, item("occupied", 64));
    var ingredient = new RecipeIngredient(List.of(new RecipeIngredient.Option("a", 0, null)));
    var grid = List.of(item("a", 64));
    assertNull(RecipeFillPlanner.plan(main, null, grid, List.of(ingredient), 0, false, true, true));
    var craft = RecipeFillPlanner.forCraft(main, null, grid, List.of(ingredient), 0, false);
    assertNotNull(craft); assertEquals(grid, craft.grid()); assertEquals(main, craft.main());
  }
  @Test void maximalCraftNormalizesUnevenMatchingInputsWithoutRequiringGrowth() {
    var main = inventory(); main.set(9, item("b", 63));
    var cells = List.of(new RecipeIngredient(List.of(new RecipeIngredient.Option("a", 0, null))), new RecipeIngredient(List.of(new RecipeIngredient.Option("b", 0, null))));
    var plan = RecipeFillPlanner.forCraft(main, null, List.of(item("a", 64), item("b", 1)), cells, 0, false);
    assertEquals(List.of(item("a", 64), item("b", 64)), plan.grid()); assertTrue(plan.main().stream().allMatch(Objects::isNull));
  }
  @Test void reusableContainersAreBoundedAndStackedContainersRejected() throws Exception {
    var reusable = item("reusable", 1);
    var plan = RecipeCraftPlanner.plan(placed(inventory(), reusable), 0, false, true,
        inputs -> RecipeCraftPlanner.step(inputs, item("result", 1), Map.of(reusable.identity(), reusable)));
    assertEquals(64, plan.steps().size()); assertEquals(reusable, plan.inventory().grid().get(0));
    assertThrows(IllegalArgumentException.class, () -> RecipeCraftPlanner.step(List.of(reusable.count(2)), item("result", 1), Map.of(reusable.identity(), reusable)));
  }
  @Test void aNativeAdapterFailureNeverReturnsAnEarlierPartialPlan() {
    int[] calls = {0};
    assertThrows(IOException.class, () -> RecipeCraftPlanner.plan(placed(inventory(), item("a", 4)), 0, false, true, inputs -> {
      if (++calls[0] == 2) throw new IOException("native recipe changed"); return RecipeCraftPlanner.step(inputs, item("result", 1), Map.of());
    }));
  }
}
