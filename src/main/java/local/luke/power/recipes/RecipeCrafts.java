package local.luke.power.recipes;

import java.io.IOException;
import java.util.*;
import java.util.function.*;
import local.luke.power.death.DeathItems;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.offhand.Offhand;
import local.luke.power.recipes.planner.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

/** Native recipe batches, with capacity and the complete item exchange journaled before publication. */
public final class RecipeCrafts {
  private RecipeCrafts() {}
  @FunctionalInterface public interface Guard { void verify() throws IOException; }
  public static final class Exchange {
    private final RecipeContext context;
    private final RecipeDefinition recipe;
    private final List<RecipeCraftPlanner.Step> steps;
    private final List<RecipeStack> consumed, produced;
    private Exchange(RecipeContext context, RecipeDefinition recipe, List<RecipeCraftPlanner.Step> steps) {
      this.context = context; this.recipe = recipe; this.steps = List.copyOf(steps);
      consumed = steps.stream().flatMap(step -> step.consumed().stream()).toList();
      produced = steps.stream().flatMap(step -> step.produced().stream()).toList();
    }
    public List<RecipeStack> consumed() { return consumed; }
    public List<RecipeStack> produced() { return produced; }
    public boolean allowed(Minecraft mc) {
      try { return context.writable(mc) && recipe.kind == RecipeDefinition.Kind.CRAFTING
          && context.kind == recipe.kind && !steps.isEmpty() && steps.size() <= RecipeCraftPlanner.MAX_BATCHES
          && Objects.equals(RecipeNative.stack(recipe.output), steps.get(0).produced().get(0))
          && RecipeFills.valid(recipe, context, steps.get(0).inputs()); }
      catch (IOException | RuntimeException invalid) { return false; }
    }
    /** The shared plan checks every protected native owner around each individual callback. */
    public void callbacks(Minecraft mc, Guard guard) throws IOException {
      for (RecipeCraftPlanner.Step step : steps) {
        guard.verify();
        if (!RecipeFills.valid(recipe, context, step.inputs())) throw new IOException("Native recipe changed during crafting");
        Container callbacks = new Container() {
          @Override public boolean method_2094(net.minecraft.entity.player.PlayerEntity player) { return false; }
        };
        class_159 scratch = new class_159(callbacks, context.width, context.height);
        for (int i = 0; i < step.inputs().size(); i++) scratch.setStack(i, RecipeNative.nativeStack(step.inputs().get(i)));
        ItemStack callbackResult = RecipeNative.nativeStack(step.produced().get(0));
        guard.verify();
        new class_557(mc.player, scratch, new class_507(), 0, 0, 0).onCrafted(callbackResult);
        guard.verify();
        if (!Objects.equals(RecipeNative.stack(callbackResult), step.produced().get(0)))
          throw new IOException("Native crafting callbacks changed the planned result");
        List<RecipeStack> actual = new ArrayList<>();
        for (int i = 0; i < step.inputs().size(); i++) actual.add(RecipeNative.stack(scratch.getStack(i)));
        if (!step.remaining().equals(actual)) throw new IOException("Native crafting callbacks changed the planned ingredients");
      }
    }
  }
  public static RecipeFills.Result craftResult(Minecraft mc, RecipeContext context, RecipeDefinition recipe,
      BooleanSupplier eligible, Consumer<Boolean> selected) throws IOException {
    return craftResult(mc, context, recipe, false, eligible, selected);
  }
  public static RecipeFills.Result craftResult(Minecraft mc, RecipeContext context, RecipeDefinition recipe,
      boolean maximum, BooleanSupplier eligible, Consumer<Boolean> selected) throws IOException {
    if (NativeInventoryPlan.failed()) return RecipeFills.Result.RECOVERY_REQUIRED;
    if (!context.writable(mc) || !eligible.getAsBoolean() || context.kind != RecipeDefinition.Kind.CRAFTING
        || recipe.kind != context.kind || context.output.getClass() != class_557.class) return RecipeFills.Result.UNAVAILABLE;
    var cells = recipe.layout(context.width, context.height); if (cells == null) return RecipeFills.Result.UNAVAILABLE;
    List<RecipeStack> main = new ArrayList<>(), grid = new ArrayList<>();
    for (ItemStack item : mc.player.inventory.main) main.add(RecipeNative.stack(item));
    for (var slot : context.inputs) grid.add(RecipeNative.stack(slot.getStack()));
    RecipeStack off = RecipeNative.stack(Offhand.get(mc.player.inventory));
    boolean matching = RecipeFills.valid(recipe, context, grid);
    var placed = maximum ? RecipeFillPlanner.forCraft(main, off, grid, cells, mc.player.inventory.selectedSlot, Offhand.enabled(mc.player))
        : matching ? new RecipeFillPlanner.Plan(main, off, grid, 1, false)
        : RecipeFillPlanner.plan(main, off, grid, cells, mc.player.inventory.selectedSlot, Offhand.enabled(mc.player), false, false);
    // An uneven matching grid may have no room for its canonical leftovers. It can still craft in place.
    if ((placed == null || placed.ghost()) && matching) placed = new RecipeFillPlanner.Plan(main, off, grid, 1, false);
    if (placed == null) return RecipeFills.Result.NO_SPACE;
    if (placed.ghost() || !RecipeFills.valid(recipe, context, placed.grid())) return RecipeFills.Result.UNAVAILABLE;
    var turn = RecipeCraftPlanner.plan(placed, mc.player.inventory.selectedSlot, Offhand.enabled(mc.player), maximum, inputs -> {
      if (!RecipeFills.valid(recipe, context, inputs)) return null;
      Map<String, RecipeStack> containers = new HashMap<>();
      for (RecipeStack input : inputs) if (input != null) {
        ItemStack nativeInput = RecipeNative.nativeStack(input);
        if (nativeInput.getItem().method_468()) containers.put(input.identity(), RecipeNative.stack(new ItemStack(nativeInput.getItem().method_467())));
      }
      return RecipeCraftPlanner.step(inputs, RecipeNative.stack(recipe.output), containers);
    });
    if (turn == null) return RecipeFills.Result.NO_SPACE;
    Exchange exchange = new Exchange(context, recipe, turn.steps()); var result = turn.inventory();
    var output = new RecipeOutputGuard(mc, context); List<NativeInventoryPlan.Change> changes = new ArrayList<>();
    for (int i = 0; i < 36; i++) if (!Objects.equals(main.get(i), result.main().get(i)))
      changes.add(NativeInventoryPlan.Change.main(mc.player.inventory, i, RecipeNative.nativeStack(result.main().get(i))));
    if (!Objects.equals(off, result.offhand())) changes.add(NativeInventoryPlan.Change.offhand(mc.player.inventory, RecipeNative.nativeStack(result.offhand())));
    // Enlist the complete external crafting owner even when a reusable ingredient ends unchanged.
    for (int i = 0; i < grid.size(); i++)
      changes.add(output.input(context.inputs.get(i), RecipeNative.nativeStack(result.grid().get(i))));
    String expectedOutput = RecipeFills.craftingOutput(context, result.grid());
    Runnable committed = () -> {
      try {
        ItemStack actual = context.output.getStack();
        if (!output.matches() || !Objects.equals(actual == null ? null : DeathItems.encode(actual), expectedOutput))
          throw new IOException("Native crafting result changed during publication");
        selected.accept(false);
      } catch (IOException failure) { throw new IllegalStateException("Recipe craft requires recovery", failure); }
    };
    if (NativeInventoryPlan.submitRecipe(mc, changes, () -> eligible.getAsBoolean() && exchange.allowed(mc) && output.matches(),
        committed, output::restore, exchange)) return RecipeFills.Result.ACCEPTED;
    return NativeInventoryPlan.failed() ? RecipeFills.Result.RECOVERY_REQUIRED
        : NativeInventoryPlan.busy() || local.luke.power.inventory.InventoryActions.busy() ? RecipeFills.Result.BUSY : RecipeFills.Result.UNAVAILABLE;
  }
}
