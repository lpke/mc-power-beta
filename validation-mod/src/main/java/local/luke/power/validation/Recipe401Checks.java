package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.*;
import local.luke.power.mixin.DeathCraftingAccessor;
import local.luke.power.offhand.*;
import local.luke.power.recipes.*;
import local.luke.power.storage.*;
import net.minecraft.class_277;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Reproduces the empty-offhand publication failure and recipe feedback in a disposable client. */
final class Recipe401Checks {
  private Recipe401Checks() {}
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable loaded singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable clone before checks");
    check(mc.player instanceof PlayerMode mode && !mode.power$spectator() && !mode.power$creative(), "use actual Survival mode");
    check(mc.field_2807 == mc.player && !DetachedCamera.isActive() && !InteractionState.carryingContainer, "use the physical player without freecam or carrying");
    check(!NativeInventoryPlan.busy() && !InventoryActions.busy() && !NativeInventoryPlan.failed(), "start with healthy idle inventory actions");
    check(!Mouse.isButtonDown(0) && !Mouse.isButtonDown(1) && !Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
        && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT), "release mouse buttons and Shift");
    failures = 0;
    try (var state = new State(mc)) {
      RecipeDefinition sticks = NativeRecipes.load().stream().filter(recipe -> recipe.kind == RecipeDefinition.Kind.CRAFTING
          && recipe.output.itemId == 280 && recipe.layout(2, 2) != null).findFirst().orElseThrow();
      test("RECIPE401 moving a full native stack into an empty offhand keeps publication ownership", () -> {
        state.reset(640); mc.player.inventory.main[9] = DeathChecks.tagged(5, 64, 0, "empty-offhand");
        String before = bytes(mc.player.inventory.main[9]); var source = mc.player.playerContainer.method_2084(9);
        check(state.book.enabled() && state.book.layout().open() && OffhandInput.swapEligible(mc, source), "wide open book did not admit the source slot");
        check(Offhand.swapSlot(mc, source), "native offhand swap was declined");
        check(before.equals(bytes(mc.player.inventory.main[9])) && Offhand.get(mc.player.inventory) == null, "swap published before its journal flush");
        state.finish();
        check(!NativeInventoryPlan.failed() && mc.player.inventory.main[9] == null && before.equals(bytes(Offhand.get(mc.player.inventory))), "empty intermediate hand state stopped or changed the swap");
        check(Offhand.swapSlot(mc, source), "inverse offhand swap was declined"); state.finish();
        check(!NativeInventoryPlan.failed() && Offhand.get(mc.player.inventory) == null && before.equals(bytes(mc.player.inventory.main[9])), "inverse swap changed the native stack");
      });
      test("RECIPE401 a native outside drop cancels an unpublished recipe without stopping future fills", () -> {
        state.reset(640); mc.player.inventory.main[9] = DeathChecks.tagged(5, 64, 0, "drop-source");
        mc.player.inventory.main[10] = new ItemStack(5, 64, 0); String dropped = bytes(mc.player.inventory.main[9]);
        boolean[] selected = {false};
        check(RecipeFills.fill(mc, state.context, sticks, false, () -> true, ghost -> selected[0] = true), "recipe plan was declined");
        Set<Object> before = state.entities();
        mc.interactionManager.clickSlot(mc.player.playerContainer.syncId, 9, 0, false, mc.player);
        mc.interactionManager.clickSlot(mc.player.playerContainer.syncId, -999, 0, false, mc.player);
        state.finish();
        check(!NativeInventoryPlan.failed() && !selected[0] && mc.player.inventory.getCursorStack() == null, "ordinary drop published a stale recipe or stopped inventory actions");
        var drops = state.drops(before); check(drops.size() == 1 && dropped.equals(bytes(drops.get(0).field_564)), "native outside drop changed count or NBT");
        check(RecipeFills.fillResult(mc, state.context, sticks, false, () -> true, ghost -> check(!ghost, "remaining ingredients became a ghost")) == RecipeFills.Result.ACCEPTED, "recipe did not recover after ordinary cancellation");
        state.finish(); check(!NativeInventoryPlan.failed() && state.context.output.getStack() != null && state.context.output.getStack().itemId == 280, "next native recipe did not publish");
      });
      test("RECIPE401 narrowing the book after a committed fill does not invalidate publication", () -> {
        state.reset(320); mc.player.inventory.main[9] = new ItemStack(5, 64, 0);
        check(state.book.replacesNative(), "expected the narrow recipe view"); choose(state.book, sticks); state.finish();
        check(!NativeInventoryPlan.failed() && !state.book.layout().open() && state.context.output.getStack() != null
            && state.context.output.getStack().itemId == 280, "closing the narrow book falsely failed the committed fill");
      });
      test("RECIPE401 bounded pending work reports busy without a capacity error or inventory writes", () -> {
        state.reset(640); mc.player.inventory.main[9] = new ItemStack(5, 64, 0); String before = state.snapshot();
        for (int i = 0; i < 5; i++) check(RecipeFills.fillResult(mc, state.context, sticks, false, () -> true, ghost -> {}) == RecipeFills.Result.ACCEPTED, "manual recipe replacement was declined early");
        check(RecipeFills.fillResult(mc, state.context, sticks, false, () -> true, ghost -> {}) == RecipeFills.Result.BUSY, "bounded pending work was reported as missing capacity");
        NativeInventoryPlan.cancel(); state.finish();
        check(before.equals(state.snapshot()) && !NativeInventoryPlan.failed(), "normal busy cancellation changed items or stopped automation");
      });
      test("RECIPE401 canceled admission and real capacity failure have distinct feedback", () -> {
        state.reset(640);
        // Distinct native objects are required even for a full-inventory capacity probe.
        for (int i = 0; i < 36; i++) mc.player.inventory.main[i] = new ItemStack(267, 1, 0);
        state.context.inputs.get(0).setStack(new ItemStack(17, 64, 0)); String before = state.snapshot();
        check(RecipeFills.fillResult(mc, state.context, sticks, false, () -> false, ghost -> {}) == RecipeFills.Result.UNAVAILABLE, "canceled admission was reported as full inventory");
        check(RecipeFills.fillResult(mc, state.context, sticks, false, () -> true, ghost -> {}) == RecipeFills.Result.NO_SPACE, "a genuinely full return inventory lost its capacity error");
        choose(state.book, sticks);
        check(field(state.book, "message").equals("Not enough inventory space") && before.equals(state.snapshot()) && !NativeInventoryPlan.failed(), "capacity failure mutated items or stopped automation");
      });
      test("RECIPE401 real callback failure retains fail-stop and is never reported as inventory capacity", () -> {
        state.reset(640); mc.player.inventory.main[0] = DeathChecks.tagged(267, 1, 73, "expected-callback-failure");
        String before = state.snapshot();
        check(NativeInventoryPlan.submit(mc, "fixture_recipe_feedback_failure", true, List.of(
            NativeInventoryPlan.Change.main(mc.player.inventory, 0, null), NativeInventoryPlan.Change.offhand(mc.player.inventory, mc.player.inventory.main[0])),
            () -> true, () -> { throw new IllegalStateException("Expected Recipe401 fixture callback failure"); }), "callback-failure fixture was declined");
        state.finish();
        check(NativeInventoryPlan.failed() && before.equals(state.snapshot()), "genuine callback failure did not stop after owned rollback");
        check(RecipeFills.fillResult(mc, state.context, sticks, false, () -> true, ghost -> {}) == RecipeFills.Result.RECOVERY_REQUIRED, "permanent failure was reported as missing capacity");
        choose(state.book, sticks);
        check(((String)field(state.book, "message")).contains("stopped") && !((String)field(state.book, "message")).contains("space"), "recipe UI hid recovery failure behind a space message");
        // Only this deliberate, verified callback injection may be reset in this disposable fixture.
        var failed = NativeInventoryPlan.class.getDeclaredField("failed"); failed.setAccessible(true); failed.setBoolean(null, false);
        check(!NativeInventoryPlan.failed(), "expected fixture failure could not be reset");
      });
    }
    log("RECIPE401 failures=" + failures);
    check(failures == 0, "Recipe drop regression failures=" + failures);
  }
  private static String bytes(ItemStack stack) throws Exception { return stack == null ? null : DeathItems.encode(stack); }
  private static void choose(RecipeBookOverlay book, RecipeDefinition recipe) throws Exception {
    var choose = RecipeBookOverlay.class.getDeclaredMethod("choose", RecipeDefinition.class, boolean.class); choose.setAccessible(true); choose.invoke(book, recipe, false);
  }

  private static final class State implements AutoCloseable {
    private final Minecraft mc;
    private final ItemStack[] main, armor, grid, inputs;
    private final ItemStack cursor, offhand, output;
    private final int selected;
    private final net.minecraft.client.gui.screen.Screen screen;
    private final net.minecraft.inventory.Container handler;
    private final boolean grabbed, repeat, focused, paused, textFocused;
    private final RecipeBookSettings recipeSettings;
    private final com.google.gson.JsonObject recipeDocument;
    private final OffhandSettings handSettings;
    private final Set<Object> existing;
    private final List<class_585> screens = new ArrayList<>();
    private RecipeContext context;
    private RecipeBookOverlay book;
    State(Minecraft mc) throws Exception {
      this.mc = mc; PowerConfig.flushPending(); recipeDocument = PowerConfig.section("recipeBook"); recipeSettings = RecipeBookConfig.copy(); handSettings = OffhandConfig.copy();
      var inventory = mc.player.inventory; main = inventory.main; armor = inventory.armor; cursor = inventory.getCursorStack(); offhand = Offhand.get(inventory); selected = inventory.selectedSlot;
      check(mc.player.playerContainer.getClass() == class_277.class, "expected the actual native player container");
      grid = ((DeathCraftingAccessor)(Object)((class_277)mc.player.playerContainer).field_1124).power$deathStacks(); inputs = grid.clone(); output = mc.player.playerContainer.method_2084(0).getStack();
      screen = mc.currentScreen; handler = mc.player.container; textFocused = InventoryTextInput.active(screen);
      grabbed = Mouse.isGrabbed(); repeat = Keyboard.areRepeatEventsEnabled(); focused = mc.field_2778; paused = mc.paused; existing = entities();
      inventory.main = new ItemStack[36]; inventory.armor = new ItemStack[4]; inventory.selectedSlot = 0;
    }
    void reset(int width) throws Exception {
      NativeInventoryPlan.cancel(); JournalQueue.awaitQueued(); NativeInventoryPlan.tick(mc);
      check(!NativeInventoryPlan.failed(), "a previous native inventory action stopped unexpectedly");
      if (book != null) book.removed(); InventoryTextInput.clear();
      Arrays.fill(mc.player.inventory.main, null); Arrays.fill(mc.player.inventory.armor, null); Arrays.fill(grid, null);
      mc.player.playerContainer.method_2084(0).setStack(null); mc.player.inventory.setCursorStack(null); Offhand.set(mc.player.inventory, null);
      var recipes = RecipeBookConfig.copy(); recipes.enabled = recipes.inventory = recipes.inventoryButton = recipes.craftingOpen = true; RecipeBookConfig.preview(recipes);
      var hands = OffhandConfig.copy(); hands.enabled = true; OffhandConfig.preview(hands);
      Mouse.setGrabbed(false); mc.field_2778 = true; mc.paused = false;
      var current = new class_585(mc.player); current.container = mc.player.playerContainer;
      mc.currentScreen = current; mc.player.container = mc.player.playerContainer; current.init(mc, width, 360); screens.add(current);
      context = RecipeContext.of(current); book = RecipeBooks.book(current); check(context != null && context.writable(mc) && book != null, "native recipe context unavailable"); book.tick();
    }
    void finish() throws Exception { JournalQueue.awaitQueued(); NativeInventoryPlan.tick(mc); check(!NativeInventoryPlan.busy(), "durable inventory action did not finish"); }
    String snapshot() throws Exception { return DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)); }
    Set<Object> entities() { Set<Object> result = Collections.newSetFromMap(new IdentityHashMap<>()); result.addAll(mc.world.field_198); return result; }
    List<net.minecraft.class_142> drops(Set<Object> before) {
      var result = new ArrayList<net.minecraft.class_142>();
      for (Object entity : mc.world.field_198) if (entity instanceof net.minecraft.class_142 drop && !before.contains(entity) && !drop.dead) result.add(drop);
      return result;
    }
    public void close() throws Exception {
      try { NativeInventoryPlan.cancel(); finish(); }
      finally {
        for (var current : screens) { var attached = RecipeBooks.book(current); if (attached != null) attached.removed(); InventoryPanels.remove(current); }
        mc.player.inventory.main = main; mc.player.inventory.armor = armor; mc.player.inventory.selectedSlot = selected;
        System.arraycopy(inputs, 0, grid, 0, inputs.length); mc.player.playerContainer.method_2084(0).setStack(output);
        mc.player.inventory.setCursorStack(cursor); Offhand.set(mc.player.inventory, offhand);
        RecipeBookConfig.preview(recipeSettings); OffhandConfig.preview(handSettings); PowerConfig.saveDeferred("recipeBook", recipeDocument); PowerConfig.flushPending();
        mc.currentScreen = screen; mc.player.container = handler; mc.field_2778 = focused; mc.paused = paused; Mouse.setGrabbed(grabbed); Keyboard.enableRepeatEvents(repeat);
        InventoryTextInput.clear(); if (textFocused) InventoryTextInput.focus(screen, true);
        for (var drop : drops(existing)) drop.markDead();
      }
    }
  }
}
