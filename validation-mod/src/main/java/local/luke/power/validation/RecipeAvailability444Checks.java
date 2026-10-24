package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.util.*;
import local.luke.power.inventory.*;
import local.luke.power.recipes.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.Display;

/** Native display timing, completed snapshots and supply invalidation in disposable clients. */
public final class RecipeAvailability444Checks {
  private RecipeAvailability444Checks() {}
  public static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    check(!NativeInventoryPlan.failed() && !NativeInventoryPlan.busy() && !InventoryActions.busy(), "start with healthy idle inventory actions");
    failures = 0;
    try (var state = new Recipe402Checks.State(mc)) {
      test("RECIPE444 startup availability completes and agrees with uncached native validation", () -> {
        state.reset(640); mc.player.inventory.main[9] = new ItemStack(5, 2, 0);
        field(state.book(), "model").set(state.book(), null);
        String before = state.snapshot(); long start = System.nanoTime(); state.book().tick();
        long startupNanos = System.nanoTime() - start;
        Object model = field(state.book(), "model").get(state.book());
        int turns = complete(state.book(), model); assertNative(mc, state.context(), model);
        check(before.equals(state.snapshot()) && !NativeInventoryPlan.busy(), "display availability wrote inventory or queued a transaction");
        @SuppressWarnings("unchecked") var recipes = (List<RecipeDefinition>) field(model, "recipes").get(model);
        long eligible = recipes.stream().filter(recipe -> recipe.kind == state.context().kind && recipe.layout(state.context().width, state.context().height) != null).count();
        log("RECIPE444 availability startup_ms=" + startupNanos / 1_000_000.0 + " extra_ticks=" + turns + " recipes=" + recipes.size()
            + " eligible=" + eligible + " old_fixed_budget_ticks=" + (recipes.size() + 23) / 24);
        mc.player.inventory.main[9] = null; before = state.snapshot();
        // Tick through the same bounded native sampling cadence before awaiting the next complete result.
        for (int tick = 0; tick < 5; tick++) state.book().tick();
        int invalidationTurns = complete(state.book(), model); assertNative(mc, state.context(), model);
        check(before.equals(state.snapshot()) && !NativeInventoryPlan.busy(), "inventory invalidation wrote native data");
        log("RECIPE444 availability invalidation_extra_ticks=" + invalidationTurns);
      });
    }
    log("RECIPE444 AVAILABILITY failures=" + failures); check(failures == 0, "recipe availability failures=" + failures);
  }
  private static int complete(RecipeBookOverlay book, Object model) throws Exception {
    Method current = model.getClass().getDeclaredMethod("current"); current.setAccessible(true);
    int turns = 0; long maximumNanos = 0;
    while (!(boolean) current.invoke(model) && turns < 80) {
      long start = System.nanoTime(); book.tick(); maximumNanos = Math.max(maximumNanos, System.nanoTime() - start); turns++;
    }
    check((boolean) current.invoke(model), "availability never published a completed snapshot");
    log("RECIPE444 availability refresh_ticks=" + turns + " max_tick_ms=" + maximumNanos / 1_000_000.0);
    return turns;
  }
  private static void assertNative(Minecraft mc, RecipeContext context, Object model) throws Exception {
    @SuppressWarnings("unchecked") var recipes = (List<RecipeDefinition>) field(model, "recipes").get(model);
    Method available = model.getClass().getDeclaredMethod("available", RecipeDefinition.class); available.setAccessible(true);
    var supply = RecipeFills.supply(mc, context);
    for (var recipe : recipes) {
      boolean expected = RecipeFills.craftable(context, recipe, supply);
      check(expected == (boolean) available.invoke(model, recipe), "cached/native availability differs for " + recipe.name);
    }
  }
  private static Field field(Object owner, String name) throws Exception {
    Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field;
  }
}
