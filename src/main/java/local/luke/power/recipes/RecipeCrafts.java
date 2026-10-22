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

/** One native recipe batch, with capacity and the exact item exchange journaled first. */
public final class RecipeCrafts {
  private RecipeCrafts() {}
  public static final class Exchange {
    private final RecipeContext context;
    private final RecipeDefinition recipe;
    private final List<RecipeStack> inputs, remaining, consumed, produced;
    private Exchange(RecipeContext context, RecipeDefinition recipe, List<RecipeStack> inputs) throws IOException {
      this.context = context; this.recipe = recipe;
      this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
      List<RecipeStack> left = new ArrayList<>(), used = new ArrayList<>(), made = new ArrayList<>();
      made.add(RecipeNative.stack(recipe.output));
      for (RecipeStack input : inputs) {
        if (input == null) { left.add(null); continue; }
        used.add(input.count(1)); ItemStack nativeInput = RecipeNative.nativeStack(input);
        if (nativeInput.getItem().method_468()) {
          if (input.count() != 1) throw new IOException("A stacked container ingredient cannot be crafted safely");
          RecipeStack container = RecipeNative.stack(new ItemStack(nativeInput.getItem().method_467()));
          left.add(container); made.add(container);
        } else left.add(input.count(input.count() - 1));
      }
      remaining = Collections.unmodifiableList(left); consumed = List.copyOf(used); produced = List.copyOf(made);
    }
    public List<RecipeStack> consumed() { return consumed; }
    public List<RecipeStack> produced() { return produced; }
    public boolean allowed(Minecraft mc) {
      try { return context.writable(mc) && recipe.kind == RecipeDefinition.Kind.CRAFTING
          && context.kind == recipe.kind && Objects.equals(RecipeNative.stack(recipe.output), produced.get(0))
          && RecipeFills.valid(recipe, context, inputs); }
      catch (IOException | RuntimeException invalid) { return false; }
    }
    private void callbacks(Minecraft mc) throws IOException {
      Container callbacks = new Container() {
        @Override public boolean method_2094(net.minecraft.entity.player.PlayerEntity player) { return false; }
      };
      class_159 scratch = new class_159(callbacks, context.width, context.height);
      for (int i = 0; i < inputs.size(); i++) scratch.setStack(i, RecipeNative.nativeStack(inputs.get(i)));
      // Native item, achievement and StationAPI callbacks run once, against owned copies.
      ItemStack callbackResult = RecipeNative.nativeStack(produced.get(0));
      new class_557(mc.player, scratch, new class_507(), 0, 0, 0).onCrafted(callbackResult);
      if (!Objects.equals(RecipeNative.stack(callbackResult), produced.get(0)))
        throw new IOException("Native crafting callbacks changed the planned result");
      List<RecipeStack> actual = new ArrayList<>();
      for (int i = 0; i < inputs.size(); i++) actual.add(RecipeNative.stack(scratch.getStack(i)));
      if (!remaining.equals(actual)) throw new IOException("Native crafting callbacks changed the planned ingredients");
    }
  }
  public static RecipeFills.Result craftResult(Minecraft mc, RecipeContext context, RecipeDefinition recipe,
      BooleanSupplier eligible, Consumer<Boolean> selected) throws IOException {
    if (NativeInventoryPlan.failed()) return RecipeFills.Result.RECOVERY_REQUIRED;
    if (!context.writable(mc) || !eligible.getAsBoolean() || context.kind != RecipeDefinition.Kind.CRAFTING
        || recipe.kind != context.kind || context.output.getClass() != class_557.class) return RecipeFills.Result.UNAVAILABLE;
    var cells = recipe.layout(context.width, context.height); if (cells == null) return RecipeFills.Result.UNAVAILABLE;
    List<RecipeStack> main = new ArrayList<>(), grid = new ArrayList<>();
    for (ItemStack item : mc.player.inventory.main) main.add(RecipeNative.stack(item));
    for (var slot : context.inputs) grid.add(RecipeNative.stack(slot.getStack()));
    RecipeStack off = RecipeNative.stack(Offhand.get(mc.player.inventory));
    var placed = RecipeFills.valid(recipe, context, grid)
        ? new RecipeFillPlanner.Plan(main, off, grid, 1, false)
        : RecipeFillPlanner.plan(main, off, grid, cells, mc.player.inventory.selectedSlot, Offhand.enabled(mc.player), false, false);
    if (placed == null) return RecipeFills.Result.NO_SPACE;
    if (placed.ghost() || !RecipeFills.valid(recipe, context, placed.grid())) return RecipeFills.Result.UNAVAILABLE;
    Exchange exchange = new Exchange(context, recipe, placed.grid());
    var result = RecipeFillPlanner.receive(placed.main(), placed.offhand(), exchange.remaining, exchange.produced.get(0),
        mc.player.inventory.selectedSlot, Offhand.enabled(mc.player));
    if (result == null) return RecipeFills.Result.NO_SPACE;
    var output = new RecipeOutputGuard(mc, context); List<NativeInventoryPlan.Change> changes = new ArrayList<>();
    for (int i = 0; i < 36; i++) if (!Objects.equals(main.get(i), result.main().get(i)))
      changes.add(NativeInventoryPlan.Change.main(mc.player.inventory, i, RecipeNative.nativeStack(result.main().get(i))));
    if (!Objects.equals(off, result.offhand())) changes.add(NativeInventoryPlan.Change.offhand(mc.player.inventory, RecipeNative.nativeStack(result.offhand())));
    for (int i = 0; i < grid.size(); i++) if (!Objects.equals(grid.get(i), result.grid().get(i)))
      changes.add(output.input(context.inputs.get(i), RecipeNative.nativeStack(result.grid().get(i))));
    String expectedOutput = RecipeFills.craftingOutput(context, result.grid());
    Runnable committed = () -> {
      try {
        exchange.callbacks(mc);
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
