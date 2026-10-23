package local.luke.power.recipes.planner;

import java.io.IOException;
import java.util.*;

/** A whole crafting turn is planned before publication; no callback or native slot runs here. */
public final class RecipeCraftPlanner {
  public static final int MAX_BATCHES = 64;
  private RecipeCraftPlanner() {}
  public record Step(List<RecipeStack> inputs, List<RecipeStack> remaining,
      List<RecipeStack> consumed, List<RecipeStack> produced) {
    public Step {
      inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
      remaining = Collections.unmodifiableList(new ArrayList<>(remaining));
      consumed = List.copyOf(consumed); produced = List.copyOf(produced);
    }
  }
  public record Plan(RecipeFillPlanner.Plan inventory, List<Step> steps) {
    public Plan { steps = List.copyOf(steps); }
  }
  @FunctionalInterface public interface Exchange { Step prepare(List<RecipeStack> inputs) throws IOException; }

  /** Native container items remain in their ingredient cells, just as the result slot leaves them. */
  public static Step step(List<RecipeStack> inputs, RecipeStack output, Map<String, RecipeStack> containers) {
    List<RecipeStack> remaining = new ArrayList<>(), consumed = new ArrayList<>(), produced = new ArrayList<>();
    produced.add(Objects.requireNonNull(output));
    for (RecipeStack input : inputs) {
      if (input == null) { remaining.add(null); continue; }
      consumed.add(input.count(1)); RecipeStack container = containers.get(input.identity());
      if (container != null) {
        if (input.count() != 1) throw new IllegalArgumentException("A stacked container ingredient cannot be crafted safely");
        remaining.add(container); produced.add(container);
      } else remaining.add(input.count(input.count() - 1));
    }
    return new Step(inputs, remaining, consumed, produced);
  }

  /** Admit complete native recipe batches only; a partial result never consumes ingredients. */
  public static Plan plan(RecipeFillPlanner.Plan placed, int selected, boolean offhandWritable,
      boolean maximum, Exchange exchange) throws IOException {
    RecipeFillPlanner.Plan current = placed; List<Step> steps = new ArrayList<>();
    for (int i = 0; i < (maximum ? MAX_BATCHES : 1); i++) {
      Step step = exchange.prepare(current.grid());
      if (step == null) break;
      var next = RecipeFillPlanner.receive(current.main(), current.offhand(), step.remaining(), step.produced().get(0), selected, offhandWritable);
      if (next == null) break;
      steps.add(step); current = next;
    }
    return steps.isEmpty() ? null : new Plan(current, steps);
  }
}
