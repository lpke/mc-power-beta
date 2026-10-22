package local.luke.power.recipes.planner;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class RecipeCraftCapacityTest {
  private RecipeStack stack(String id, int count) { return new RecipeStack(id, 0, id, count, 64, true); }
  private List<RecipeStack> main() { return new ArrayList<>(Collections.nCopies(36, null)); }
  @Test void craftedBatchMergesBeforeTakingEmptyInventorySpace() {
    var main = main(); main.set(0, stack("result", 62));
    var plan = RecipeFillPlanner.receive(main, stack("result", 63), Collections.nCopies(4, null), stack("result", 4), 0, true);
    assertNotNull(plan); assertEquals(64, plan.main().get(0).count()); assertEquals(64, plan.offhand().count());
    assertEquals(stack("result", 1), plan.main().get(1)); assertEquals(62, main.get(0).count());
  }
  @Test void incompleteOutputCapacityDeclinesTheWholeBatch() {
    var main = main(); for (int i = 0; i < 36; i++) main.set(i, stack("full", 64));
    main.set(0, stack("result", 62));
    assertNull(RecipeFillPlanner.receive(main, stack("result", 63), List.of(stack("input", 2)), stack("result", 4), 0, true));
    assertEquals(62, main.get(0).count());
  }
  @Test void outputKeepsNativeIdentityAndNeverFillsAnEmptyOffhand() {
    var result = new RecipeStack("result", 7, "full-native-data", "components", 4, 64, false);
    var plan = RecipeFillPlanner.receive(main(), null, Collections.nCopies(4, null), result, 0, true);
    assertNotNull(plan); assertEquals(result, plan.main().get(0)); assertNull(plan.offhand());
    assertThrows(UnsupportedOperationException.class, () -> plan.main().set(0, null));
  }
}
