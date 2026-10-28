package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
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

/** Native remove-one publication, callbacks and recipe-book ownership in a disposable client. */
public final class Recipe402Checks {
  public static int callbackMode;
  public static Object callbackPlayer;
  public static void craftCallback(net.minecraft.inventory.Inventory inputs, net.minecraft.entity.player.PlayerEntity player, ItemStack result) {
    if (callbackPlayer != player || callbackMode == 0) return;
    if (callbackMode == 1) throw new IllegalStateException("Deliberate native craft callback failure");
    if (callbackMode == 2) result.count++;
    if (callbackMode == 3) for (int i = 0; i < inputs.size(); i++) if (inputs.getStack(i) != null) {
      ItemStack changed = inputs.getStack(i).clone(); changed.count++; inputs.setStack(i, changed); break;
    }
  }
  private Recipe402Checks() {}
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
      test("RECIPE402 right-click removes the last placed batch after its durable flush", () -> {
        state.reset(640); var sticks = recipe(state.book, 280);
        mc.player.inventory.main[9] = DeathChecks.tagged(5, 2, 0, "remove-last");
        choose(state.book, sticks); state.finish(); var totals = totals(mc, state.context); String before = snapshot(mc, state.context);
        check((boolean) invoke(state.book, "removable", sticks), "the published recipe was not tracked");
        rightClick(state.book, sticks);
        check(before.equals(snapshot(mc, state.context)), "removal published before the recovery flush"); state.finish();
        check(state.context.inputs.stream().allMatch(slot -> slot.getStack() == null) && state.context.output.getStack() == null, "last removal left inputs or a derived output");
        check(totals.equals(totals(mc, state.context)) && !NativeInventoryPlan.failed(), "last removal changed native item totals");
      });
      test("RECIPE402 repeated removal keeps a craftable batch and full native NBT", () -> {
        state.reset(640); var sticks = recipe(state.book, 280);
        mc.player.inventory.main[9] = DeathChecks.tagged(5, 6, 0, "remove-repeated");
        choose(state.book, sticks); state.finish(); choose(state.book, sticks); state.finish();
        var totals = totals(mc, state.context); rightClick(state.book, sticks); state.finish();
        check(state.context.inputs.stream().filter(slot -> slot.getStack() != null).allMatch(slot -> slot.getStack().count == 1), "remove-one changed the wrong batch count");
        check(bytes(sticks.output).equals(bytes(state.context.output.getStack())), "remaining inputs lost their native crafting result");
        check(totals.equals(totals(mc, state.context)), "repeated removal changed count, damage or NBT");
        rightClick(state.book, sticks); state.finish();
        check(state.context.inputs.stream().allMatch(slot -> slot.getStack() == null) && state.context.output.getStack() == null, "second removal did not clear the last batch");
        check(totals.equals(totals(mc, state.context)), "second removal changed native item totals");
      });
      test("RECIPE402 a full return inventory is an unchanged capacity failure", () -> {
        state.reset(640); var sticks = recipe(state.book, 280);
        mc.player.inventory.main[9] = new ItemStack(5, 2, 0); choose(state.book, sticks); state.finish();
        for (int i = 0; i < 36; i++) mc.player.inventory.main[i] = new ItemStack(267, 1, 0);
        String before = snapshot(mc, state.context); rightClick(state.book, sticks);
        check(field(state.book, "message").equals("Not enough inventory space") && before.equals(snapshot(mc, state.context)), "capacity failure moved inputs or lost its feedback");
        check(!NativeInventoryPlan.busy() && !NativeInventoryPlan.failed(), "capacity failure queued work or stopped inventory actions");
      });
      test("RECIPE402 matching occupied offhand accepts returned data without supplying ingredients", () -> {
        state.reset(640); var sticks = recipe(state.book, 280);
        mc.player.inventory.main[9] = DeathChecks.tagged(5, 2, 0, "remove-offhand"); choose(state.book, sticks); state.finish();
        for (int i = 0; i < 36; i++) mc.player.inventory.main[i] = new ItemStack(267, 1, 0);
        Offhand.set(mc.player.inventory, DeathChecks.tagged(5, 62, 0, "remove-offhand"));
        var totals = totals(mc, state.context); rightClick(state.book, sticks); state.finish();
        check(Offhand.get(mc.player.inventory).count == 64 && state.context.output.getStack() == null, "matching offhand did not receive exactly the two returned ingredients");
        check(totals.equals(totals(mc, state.context)), "offhand return changed full native data");
      });
      test("RECIPE402 unrelated or manually replaced grid cannot be removed by the book", () -> {
        state.reset(640); var sticks = recipe(state.book, 280);
        mc.player.inventory.main[9] = new ItemStack(5, 4, 0); choose(state.book, sticks); state.finish();
        var first = state.context.inputs.stream().filter(slot -> slot.getStack() != null).findFirst().orElseThrow();
        first.setStack(first.getStack().clone()); String before = snapshot(mc, state.context);
        check(!(boolean) invoke(state.book, "removable", sticks), "replacing a native input kept stale placement ownership");
        rightClick(state.book, sticks); state.finish(); check(before.equals(snapshot(mc, state.context)), "stale book removal touched the grid");
        first.setStack(new ItemStack(17, 1, 0)); before = snapshot(mc, state.context);
        check(RecipeFills.removeResult(mc, state.context, sticks, () -> true, ignored -> {}) == RecipeFills.Result.UNAVAILABLE,
            "a different native recipe was admitted for removal");
        check(before.equals(snapshot(mc, state.context)), "recipe mismatch changed native items");
      });
      test("RECIPE420 recipe-cell right-click crafts one batch after its durable flush", () -> {
        state.reset(640); var sticks = recipe(state.book, 280); mc.player.inventory.main[9] = new ItemStack(5, 6, 0);
        field(state.book, "groups", List.of(List.of(sticks))); field(state.book, "page", 0); String before = snapshot(mc, state.context);
        var cell = state.book.layout().cell(0); check(state.book.mouse(cell.x() + 12, cell.y() + 12, 1), "craft press was not owned");
        check(before.equals(snapshot(mc, state.context)), "crafting changed slots before its durable flush"); state.finish();
        int planks = 0, made = 0; for (var item : mc.player.inventory.main) if (item != null) {
          if (item.itemId == 5) planks += item.count; if (item.itemId == 280) made += item.count;
        }
        check(planks == 4 && made == sticks.output.count && state.context.inputs.stream().allMatch(slot -> slot.getStack() == null)
            && state.context.output.getStack() == null && !NativeInventoryPlan.failed(), "right-click did not craft exactly one complete batch");
      });
      test("RECIPE442 grid wheel stops at zero, click reenables quantity and inversion", () -> {
        state.reset(640); var sticks = recipe(state.book, 280); mc.player.inventory.main[9] = new ItemStack(5, 8, 0);
        Object inventorySettings = Class.forName("local.luke.power.inventory.Config").getField("INVENTORY_TWEAKS_CONFIG").get(null);
        Object mouse = inventorySettings.getClass().getField("MOUSE_TWEAKS_CONFIG").get(inventorySettings);
        Object wheel = mouse.getClass().getField("SCROLL_WHEEL_CONFIG").get(mouse);
        var enabled = wheel.getClass().getField("enableScrollWheelTweaks"); var inverted = wheel.getClass().getField("invertScrollCursorSlotDirection");
        Object previousEnabled = enabled.get(wheel), previousInverted = inverted.get(wheel);
        try {
          enabled.set(wheel, true); inverted.set(wheel, false); choose(state.book, sticks); state.finish();
          rightClick(state.book, sticks); state.finish();
          var input = state.context.inputs.get(0); var layout = state.book.layout();
          int px = layout.inventoryX() + input.x + 8, py = layout.inventoryY() + input.y + 8;
          check(!state.book.ownsWheel(px, py), "empty recipe grid retained wheel ownership");
          state.book.scroll(px, py, 120); state.finish();
          check(state.context.output.getStack() == null && state.context.inputs.stream().allMatch(slot -> slot.getStack() == null), "wheel up refilled from zero");
          choose(state.book, sticks); state.finish();
          check(state.context.output.getStack() != null, "explicit recipe click did not reenable quantity control");
          inverted.set(wheel, true); state.book.scroll(px, py, 120); state.finish();
          check(state.context.output.getStack() == null, "inverted wheel up did not remove the last batch");
        } finally { enabled.set(wheel, previousEnabled); inverted.set(wheel, previousInverted); }
      });
      test("RECIPE440 main storage falls back to 2x2 inputs with crafting storage disabled", () -> {
        state.reset(640);
        Object compatibility = Class.forName("local.luke.power.compat.Config").getField("config").get(null);
        var stored = compatibility.getClass().getField("allowCraftingInventorySlots"); Object previous = stored.get(compatibility);
        var visual = local.luke.power.visual.VisualConfig.copy();
        try {
          var transfer = local.luke.power.visual.VisualConfig.copy(); transfer.shiftClickIntoPlayerCraftingGrid = true; local.luke.power.visual.VisualConfig.preview(transfer);
          stored.set(compatibility, false); mc.player.inventory.main[9] = DeathChecks.tagged(4, 7, 0, "stored-grid");
          state.context.handler.onSlotClick(9, 0, true, mc.player);
          check(mc.player.inventory.main[0] != null && mc.player.inventory.main[9] == null
              && state.context.inputs.stream().allMatch(slot -> slot.getStack() == null), "available hotbar lost native transfer priority");
          for (int i = 0; i < 9; i++) mc.player.inventory.main[i] = new ItemStack(267, 1, 0);
          mc.player.inventory.main[9] = DeathChecks.tagged(4, 7, 0, "stored-grid"); String source = bytes(mc.player.inventory.main[9]);
          state.context.handler.onSlotClick(9, 0, true, mc.player);
          check(source.equals(bytes(mc.player.inventory.main[9])), "2x2 fallback published before its durable flush"); state.finish();
          check(mc.player.inventory.main[9] == null && source.equals(bytes(state.context.inputs.get(0).getStack())), "full hotbar did not fall back into crafting inputs");
          mc.player.inventory.main[10] = DeathChecks.tagged(35, 5, 4, "shift-drag-a");
          mc.player.inventory.main[11] = DeathChecks.tagged(35, 3, 14, "shift-drag-b");
          String first = bytes(mc.player.inventory.main[10]), second = bytes(mc.player.inventory.main[11]);
          state.context.handler.onSlotClick(10, 0, true, mc.player);
          state.context.handler.onSlotClick(11, 0, true, mc.player);
          state.finish(); local.luke.power.inventory.CraftingGridTransfer.tick(mc); state.finish();
          check(mc.player.inventory.main[10] == null && mc.player.inventory.main[11] == null
              && first.equals(bytes(state.context.inputs.get(1).getStack()))
              && second.equals(bytes(state.context.inputs.get(2).getStack())), "queued Shift-drag lost a source or its native data");
        } finally { stored.set(compatibility, previous); local.luke.power.visual.VisualConfig.preview(visual); }
      });
      test("RECIPE420 one native cake batch retains all three bucket containers", () -> {
        state.reset(640); int[] air = null;
        int px = (int)Math.floor(mc.player.x), py = Math.max(1, Math.min(126, (int)Math.floor(mc.player.boundingBox.minY))), pz = (int)Math.floor(mc.player.z);
        for (int dx = -2; dx <= 2 && air == null; dx++) for (int dz = -2; dz <= 2; dz++)
          if (mc.world.method_239(px + dx, py, pz + dz) && mc.world.getBlockId(px + dx, py, pz + dz) == 0) { air = new int[]{px + dx, py, pz + dz}; break; }
        check(air != null, "cake fixture needs one nearby loaded air block");
        var previousScreen = mc.currentScreen; var previousHandler = mc.player.container;
        mc.world.method_154(air[0], air[1], air[2], 58, 0);
        var table = new net.minecraft.class_516(mc.player.inventory, mc.world, air[0], air[1], air[2]);
        RecipeContext tableContext = null;
        try {
          mc.currentScreen = table; mc.player.container = table.container; table.init(mc, 640, 360);
          tableContext = RecipeContext.of(table); check(tableContext != null && tableContext.writable(mc), "cake table is not writable");
          var cake = NativeRecipes.load().stream().filter(recipe -> recipe.kind == RecipeDefinition.Kind.CRAFTING && recipe.output.itemId == 354).findFirst().orElseThrow();
          for (int i = 9; i <= 11; i++) mc.player.inventory.main[i] = new ItemStack(335, 1, 0);
          mc.player.inventory.main[12] = new ItemStack(344, 1, 0); mc.player.inventory.main[13] = new ItemStack(353, 2, 0); mc.player.inventory.main[14] = new ItemStack(296, 3, 0);
          check(RecipeCrafts.craftResult(mc, tableContext, cake, () -> true, ignored -> {}) == RecipeFills.Result.ACCEPTED, "native cake craft was declined"); state.finish();
          int cakes = 0, buckets = 0;
          for (var stack : mc.player.inventory.main) if (stack != null && stack.itemId == 354) cakes += stack.count;
          for (var slot : tableContext.inputs) if (slot.getStack() != null && slot.getStack().itemId == 325) buckets += slot.getStack().count;
          check(cakes == 1 && buckets == 3 && tableContext.output.getStack() == null && !NativeInventoryPlan.failed(), "cake craft lost native bucket containers or duplicated its result");
        } finally {
          NativeInventoryPlan.cancel(); state.finish();
          if (tableContext != null) for (var slot : tableContext.inputs) slot.setStack(null);
          var book = RecipeBooks.book(table); if (book != null) book.removed(); InventoryPanels.remove(table);
          mc.currentScreen = previousScreen; mc.player.container = previousHandler; mc.world.method_154(air[0], air[1], air[2], 0, 0);
        }
      });
      for (int failureMode = 1; failureMode <= 3; failureMode++) {
        final int scenario = failureMode;
        test("RECIPE420 native craft callback refuses mutation and restores owned bytes " + scenario, () -> {
          state.reset(640); var sticks = recipe(state.book, 280); mc.player.inventory.main[9] = new ItemStack(5, 6, 0);
          choose(state.book, sticks); state.finish(); choose(state.book, sticks); state.finish();
          String before = snapshot(mc, state.context); callbackMode = scenario; callbackPlayer = mc.player;
          try {
            check(RecipeCrafts.craftResult(mc, state.context, sticks, () -> true, ignored -> {}) == RecipeFills.Result.ACCEPTED,
                "native callback fixture could not prepare its craft"); state.finish();
            check(NativeInventoryPlan.failed() && before.equals(snapshot(mc, state.context)), "callback mutation escaped or lost original bytes");
          } finally { callbackMode = 0; callbackPlayer = null; }
          var failed = NativeInventoryPlan.class.getDeclaredField("failed"); failed.setAccessible(true); failed.setBoolean(null, false);
        });
      }
      test("RECIPE402 a carried cursor cancels unpublished removal without stopping future work", () -> {
        state.reset(640); var sticks = recipe(state.book, 280);
        mc.player.inventory.main[9] = new ItemStack(5, 2, 0); choose(state.book, sticks); state.finish(); String before = snapshot(mc, state.context);
        rightClick(state.book, sticks); mc.player.inventory.setCursorStack(new ItemStack(3, 1, 0)); state.finish();
        mc.player.inventory.setCursorStack(null);
        check(before.equals(snapshot(mc, state.context)) && !NativeInventoryPlan.failed(), "carried cursor published stale removal or stopped actions");
        rightClick(state.book, sticks); state.finish(); check(state.context.output.getStack() == null, "removal did not recover after ordinary cancellation");
      });
      test("RECIPE402 closing the recipe book cancels its unpublished removal", () -> {
        state.reset(640); var sticks = recipe(state.book, 280);
        mc.player.inventory.main[9] = new ItemStack(5, 2, 0); choose(state.book, sticks); state.finish();
        String before = snapshot(mc, state.context); rightClick(state.book, sticks); state.book.toggle(); state.finish();
        check(before.equals(snapshot(mc, state.context)) && !NativeInventoryPlan.failed(), "book closure published stale removal or stopped actions");
      });
      test("RECIPE402 callback failure restores all inputs before the derived result", () -> {
        state.reset(640); var sticks = recipe(state.book, 280);
        mc.player.inventory.main[9] = DeathChecks.tagged(5, 2, 0, "remove-rollback"); choose(state.book, sticks); state.finish();
        String before = snapshot(mc, state.context);
        check(RecipeFills.removeResult(mc, state.context, sticks, () -> true, ignored -> {
          throw new IllegalStateException("Expected Recipe402 fixture callback failure");
        }) == RecipeFills.Result.ACCEPTED, "callback-failure removal was declined"); state.finish();
        check(NativeInventoryPlan.failed() && before.equals(snapshot(mc, state.context)), "callback failure lost owned inputs or the original derived result");
        // Reset only this deliberate, fully restored fixture failure in the disposable client.
        var failed = NativeInventoryPlan.class.getDeclaredField("failed"); failed.setAccessible(true); failed.setBoolean(null, false);
      });
    }
    log("RECIPE402 failures=" + failures); check(failures == 0, "Recipe remove-one failures=" + failures);
  }
  private static String bytes(ItemStack stack) throws Exception { return stack == null ? null : DeathItems.encode(stack); }
  private static Object invoke(RecipeBookOverlay book, String name, RecipeDefinition recipe) throws Exception {
    var method = RecipeBookOverlay.class.getDeclaredMethod(name, RecipeDefinition.class); method.setAccessible(true); return method.invoke(book, recipe);
  }
  private static void choose(RecipeBookOverlay book, RecipeDefinition recipe) throws Exception {
    var method = RecipeBookOverlay.class.getDeclaredMethod("choose", RecipeDefinition.class, boolean.class); method.setAccessible(true); method.invoke(book, recipe, false);
  }
  private static RecipeDefinition recipe(RecipeBookOverlay book, int output) throws Exception {
    @SuppressWarnings("unchecked") var recipes = (List<RecipeDefinition>) field(field(book, "model"), "recipes");
    return recipes.stream().filter(recipe -> recipe.kind == RecipeDefinition.Kind.CRAFTING && recipe.output.itemId == output && recipe.layout(2, 2) != null).findFirst().orElseThrow();
  }
  private static void rightClick(RecipeBookOverlay book, RecipeDefinition recipe) throws Exception {
    RecipeContext context = (RecipeContext)field(book, "context"); var slot = context.inputs.get(0); var layout = book.layout();
    book.mouse(layout.inventoryX() + slot.x + 8, layout.inventoryY() + slot.y + 8, 1);
  }
  private static List<ItemStack> items(Minecraft mc, RecipeContext context) {
    var items = new ArrayList<>(Arrays.asList(mc.player.inventory.main)); items.add(Offhand.get(mc.player.inventory));
    for (var slot : context.inputs) items.add(slot.getStack()); return items;
  }
  private static String snapshot(Minecraft mc, RecipeContext context) throws Exception {
    var data = new ArrayList<String>(); for (var stack : items(mc, context)) data.add(bytes(stack));
    data.add(bytes(context.output.getStack())); data.add(bytes(mc.player.inventory.getCursorStack())); return data.toString();
  }
  private static Map<String, Long> totals(Minecraft mc, RecipeContext context) throws Exception {
    Map<String, Long> totals = new HashMap<>();
    for (var stack : items(mc, context)) if (stack != null) {
      var identity = DeathItems.decode(bytes(stack)); identity.count = 1; totals.merge(bytes(identity), (long) stack.count, Long::sum);
    }
    return totals;
  }
  static final class State implements AutoCloseable {
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
    private final List<class_585> screens = new ArrayList<>();
    private RecipeContext context;
    private RecipeBookOverlay book;
    State(Minecraft mc) throws Exception {
      this.mc = mc; PowerConfig.flushPending(); recipeDocument = PowerConfig.section("recipeBook"); recipeSettings = RecipeBookConfig.copy(); handSettings = OffhandConfig.copy();
      var inventory = mc.player.inventory; main = inventory.main; armor = inventory.armor; cursor = inventory.getCursorStack(); offhand = Offhand.get(inventory); selected = inventory.selectedSlot;
      check(mc.player.playerContainer.getClass() == class_277.class, "expected the actual native player container");
      grid = ((DeathCraftingAccessor)(Object)((class_277)mc.player.playerContainer).field_1124).power$deathStacks(); inputs = grid.clone(); output = mc.player.playerContainer.method_2084(0).getStack();
      screen = mc.currentScreen; handler = mc.player.container; textFocused = InventoryTextInput.active(screen);
      grabbed = Mouse.isGrabbed(); repeat = Keyboard.areRepeatEventsEnabled(); focused = mc.field_2778; paused = mc.paused;
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
    RecipeContext context() { return context; }
    RecipeBookOverlay book() { return book; }
    void finish() throws Exception { JournalQueue.awaitQueued(); NativeInventoryPlan.tick(mc); check(!NativeInventoryPlan.busy(), "durable inventory action did not finish"); }
    String snapshot() throws Exception { return DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)); }
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
      }
    }
  }
}
