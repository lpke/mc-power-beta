package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.*;
import local.luke.power.mixin.DeathCraftingAccessor;
import local.luke.power.recipes.*;
import local.luke.power.storage.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** One durable crafting turn, real callbacks and visit-local recipe gestures. Disposable clients only. */
public final class Recipe441Checks {
  private static PlayerEntity callbackPlayer;
  private static RecipeContext callbackContext;
  private static int callbacks, failureMode;
  private static Physical physical;
  private Recipe441Checks() {}
  public static void callback(Inventory scratch, PlayerEntity player, ItemStack result) {
    if (player != callbackPlayer) return;
    callbacks++;
    if (failureMode >= 1 && failureMode <= 3 && callbacks == 2) {
      if (failureMode == 1) throw new IllegalStateException("Expected second-batch callback failure");
      if (failureMode == 2) result.count++;
      if (failureMode == 3) for (int i = 0; i < scratch.size(); i++) if (scratch.getStack(i) != null) { scratch.getStack(i).count++; break; }
    }
    if (failureMode == 4 && callbacks == 1) player.inventory.armor[0] = new ItemStack(3, 1, 0);
    if (failureMode == 5 && callbacks == 1)
      ((DeathCraftingAccessor)(Object)callbackContext.crafting).power$deathStacks()[8] = new ItemStack(3, 1, 0);
  }
  private static void capture(Minecraft mc, RecipeContext context, int mode) {
    callbackPlayer = mc.player; callbackContext = context; callbacks = 0; failureMode = mode;
  }
  private static void clearCapture() { callbackPlayer = null; callbackContext = null; failureMode = 0; }
  public static void run(Minecraft mc) throws Exception {
    check(physical == null, "stop the staged recipe-441 check first");
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    check(mc.player instanceof PlayerMode mode && !mode.power$spectator() && !mode.power$creative(), "use actual Survival");
    check(!NativeInventoryPlan.failed() && !NativeInventoryPlan.busy() && !InventoryActions.busy(), "start with healthy idle inventory actions");
    check(!Mouse.isButtonDown(0) && !Mouse.isButtonDown(1), "release mouse buttons");
    failures = 0;
    try (var state = new Recipe402Checks.State(mc)) {
      test("RECIPE441 Alt-click variants preserves native items and Shift-right-click crafts maximum", () -> {
        state.reset(640); var variantsBook = state.book();
        var variants = variants(variantsBook); groups(variantsBook, List.of(variants)); String before = snapshot(mc, state.context());
        for (int button : new int[]{0, 1}) {
          modifier(Keyboard.KEY_LMENU, () -> click(variantsBook, button));
          check(((List<?>)field(variantsBook, "popup")).size() == variants.size(), "Alt-click did not open variants");
          check(before.equals(snapshot(mc, state.context())) && !NativeInventoryPlan.busy(), "opening variants wrote native items");
          variantsBook.key('\0', Keyboard.KEY_ESCAPE);
        }
        state.reset(640); var book = state.book(); var sticks = recipe(book, 280); groups(book, List.of(List.of(sticks)));
        mc.player.inventory.main[9] = DeathChecks.tagged(5, 128, 0, "maximum-native-source");
        before = snapshot(mc, state.context()); capture(mc, state.context(), 0);
        final var selectedBook = book;
        try (var slow = new AutoTools400State.SlowWriter()) {
          modifier(Keyboard.KEY_LSHIFT, () -> click(selectedBook, 1)); slow.checkBlocked();
          check(NativeInventoryPlan.busy() && callbacks == 0 && before.equals(snapshot(mc, state.context())), "maximum craft wrote before the durable flush");
          slow.release(); state.finish();
        } finally { clearCapture(); }
        check(callbacks == 64 && total(mc, 280) == 256 && total(mc, 5) == 0, "maximum crafting did not complete exactly 64 native batches in one publication");
        check(state.context().inputs.stream().allMatch(slot -> slot.getStack() == null) && !NativeInventoryPlan.busy() && !NativeInventoryPlan.failed(), "maximum craft left a repeated/pending operation");
      });
      test("RECIPE441 hovering an untouched recipe scrolls pages, chosen icons retain zero-to-one wheel control", () -> {
        state.reset(640); var book = state.book(); var sticks = recipe(book, 280);
        List<List<RecipeDefinition>> pages = new ArrayList<>(); for (int i = 0; i < 21; i++) pages.add(List.of(sticks)); groups(book, pages);
        mc.player.inventory.main[9] = new ItemStack(5, 12, 0); String before = snapshot(mc, state.context()); var cell = book.layout().cell(0);
        Object inventory = Class.forName("local.luke.power.inventory.Config").getField("INVENTORY_TWEAKS_CONFIG").get(null);
        Object mouse = inventory.getClass().getField("MOUSE_TWEAKS_CONFIG").get(inventory);
        Object wheel = mouse.getClass().getField("SCROLL_WHEEL_CONFIG").get(mouse);
        var enabled = wheel.getClass().getField("enableScrollWheelTweaks"); var inverted = wheel.getClass().getField("invertScrollCursorSlotDirection");
        Object oldEnabled = enabled.get(wheel), oldInverted = inverted.get(wheel);
        try {
          enabled.set(wheel, true); inverted.set(wheel, false);
          book.scroll(cell.x() + 10, cell.y() + 10, -120);
          check((int)field(book, "page") == 1 && before.equals(snapshot(mc, state.context())) && !NativeInventoryPlan.busy(), "untouched hover changed ingredient amounts instead of page");
          groups(book, List.of(List.of(sticks))); click(book, 0); state.finish();
          book.scroll(cell.x() + 10, cell.y() + 10, -120); state.finish();
          check(state.context().inputs.stream().allMatch(slot -> slot.getStack() == null), "wheel did not remove the last batch");
          book.scroll(cell.x() + 10, cell.y() + 10, 120); state.finish();
          check(state.context().output.getStack() != null, "clicked recipe lost wheel ownership at zero");
        } finally { enabled.set(wheel, oldEnabled); inverted.set(wheel, oldInverted); }
      });
      test("RECIPE441 partial capacity crafts whole batches and keeps tagged leftover ingredients", () -> {
        state.reset(640); var sticks = recipe(state.book(), 280); full(mc, 54); grid(state.context(), sticks, DeathChecks.tagged(5, 64, 0, "partial-components"));
        capture(mc, state.context(), 0);
        check(RecipeCrafts.craftResult(mc, state.context(), sticks, true, () -> true, ignored -> {}) == RecipeFills.Result.ACCEPTED, "partial maximum declined"); state.finish(); clearCapture();
        check(callbacks == 2 && mc.player.inventory.main[0].count == 62, "partial capacity consumed an incomplete output batch");
        for (var slot : state.context().inputs) if (slot.getStack() != null) {
          ItemStack expected = DeathChecks.tagged(5, 62, 0, "partial-components");
          check(DeathItems.encode(expected).equals(DeathItems.encode(slot.getStack())), "partial craft changed ingredient NBT/count");
        }
      });
      test("RECIPE441 zero capacity and a stale inventory owner publish nothing", () -> {
        state.reset(640); var sticks = recipe(state.book(), 280); full(mc, 62); grid(state.context(), sticks, new ItemStack(5, 64, 0));
        String before = snapshot(mc, state.context()); capture(mc, state.context(), 0);
        check(RecipeCrafts.craftResult(mc, state.context(), sticks, true, () -> true, ignored -> {}) == RecipeFills.Result.NO_SPACE
            && before.equals(snapshot(mc, state.context())) && callbacks == 0 && !NativeInventoryPlan.busy(), "zero capacity changed inventory");
        state.reset(640); sticks = recipe(state.book(), 280); mc.player.inventory.main[9] = new ItemStack(5, 8, 0); before = snapshot(mc, state.context());
        check(RecipeCrafts.craftResult(mc, state.context(), sticks, true, () -> true, ignored -> {}) == RecipeFills.Result.ACCEPTED, "stale owner plan was not prepared");
        var original = mc.player.inventory.main; mc.player.inventory.main = original.clone();
        try { state.finish(); } finally { mc.player.inventory.main = original; }
        check(before.equals(snapshot(mc, state.context())) && callbacks == 0 && !NativeInventoryPlan.failed(), "stale owner published or stopped healthy actions"); clearCapture();
      });
      for (int mode = 1; mode <= 4; mode++) {
        final int failure = mode;
        test("RECIPE441 each batch callback retains full guard and owned rollback " + failure, () -> {
          state.reset(640); var sticks = recipe(state.book(), 280); mc.player.inventory.main[9] = DeathChecks.tagged(5, 8, 0, "rollback-components");
          String before = snapshot(mc, state.context()); capture(mc, state.context(), failure);
          try {
            check(RecipeCrafts.craftResult(mc, state.context(), sticks, true, () -> true, ignored -> {}) == RecipeFills.Result.ACCEPTED, "callback rollback plan was not prepared"); state.finish();
            check(NativeInventoryPlan.failed(), "native callback mutation was accepted");
            check(callbacks == (failure == 4 ? 1 : 2), "another native callback ran after a failed guard");
            if (failure == 4) { check(mc.player.inventory.armor[0] != null, "rollback overwrote an unowned callback mutation"); mc.player.inventory.armor[0] = null; }
            check(before.equals(snapshot(mc, state.context())), "failed turn lost owned counts, NBT or derived result");
            resetFailure();
          } finally { clearCapture(); }
        });
      }
      test("RECIPE441 table containers, unchanged external owners and unsafe subclasses", () -> {
        state.reset(640); int[] air = air(mc); mc.world.method_154(air[0], air[1], air[2], 58, 0);
        var table = new class_516(mc.player.inventory, mc.world, air[0], air[1], air[2]); RecipeContext context = null;
        try {
          mc.currentScreen = table; mc.player.container = table.container; table.init(mc, 640, 360); context = RecipeContext.of(table);
          check(context != null && context.writable(mc), "table context is not writable");
          var cake = NativeRecipes.load().stream().filter(value -> value.kind == RecipeDefinition.Kind.CRAFTING && value.output.itemId == 354).findFirst().orElseThrow();
          for (int i = 9; i <= 11; i++) mc.player.inventory.main[i] = DeathChecks.tagged(335, 1, 0, "milk-" + i);
          mc.player.inventory.main[12] = new ItemStack(344, 1, 0); mc.player.inventory.main[13] = new ItemStack(353, 2, 0); mc.player.inventory.main[14] = new ItemStack(296, 3, 0);
          capture(mc, context, 0);
          check(RecipeCrafts.craftResult(mc, context, cake, true, () -> true, ignored -> {}) == RecipeFills.Result.ACCEPTED, "maximum cake craft declined"); state.finish(); clearCapture();
          check(callbacks == 1 && total(mc, 354) == 1 && context.inputs.stream().filter(slot -> slot.getStack() != null && slot.getStack().itemId == 325).mapToInt(slot -> slot.getStack().count).sum() == 3, "native cake container returns were changed");
          Arrays.fill(mc.player.inventory.main, null); for (var slot : context.inputs) slot.setStack(null);
          var sticks = NativeRecipes.load().stream().filter(value -> value.kind == RecipeDefinition.Kind.CRAFTING && value.output.itemId == 280).findFirst().orElseThrow();
          mc.player.inventory.main[9] = new ItemStack(5, 8, 0); String before = snapshot(mc, context); capture(mc, context, 5);
          check(RecipeCrafts.craftResult(mc, context, sticks, true, () -> true, ignored -> {}) == RecipeFills.Result.ACCEPTED, "external-owner guard fixture was not prepared"); state.finish();
          check(NativeInventoryPlan.failed() && callbacks == 1 && context.inputs.get(8).getStack() != null, "unchanged external crafting owner escaped per-batch validation");
          ((DeathCraftingAccessor)(Object)context.crafting).power$deathStacks()[8] = null;
          check(before.equals(snapshot(mc, context)), "external callback mutation lost owned bytes"); resetFailure(); clearCapture();
          var unsupported = new UnsupportedRecipe(); @SuppressWarnings("unchecked") var nativeRecipes = (List<Object>)class_148.method_538().method_541(); nativeRecipes.add(unsupported);
          try { check(NativeRecipes.load().stream().noneMatch(value -> value.nativeRecipe == unsupported), "unknown recipe subclass received a guessed adapter"); }
          finally { nativeRecipes.remove(unsupported); }
        } finally {
          clearCapture(); NativeInventoryPlan.cancel(); state.finish();
          if (context != null) for (var slot : context.inputs) slot.setStack(null);
          var book = RecipeBooks.book(table); if (book != null) book.removed(); InventoryPanels.remove(table);
          mc.currentScreen = null; mc.player.container = mc.player.playerContainer; mc.world.method_154(air[0], air[1], air[2], 0, 0);
        }
      });
    } finally { clearCapture(); }
    log("RECIPE441 failures=" + failures); check(failures == 0, "recipe-441 failures=" + failures);
  }
  /** Deliberately never synthesizes input: the operator supplies native queued mouse/key events. */
  public static void physical(Minecraft mc, String command) throws Exception {
    command = command.trim();
    if (command.equals("stop")) { stopPhysical(mc); return; }
    if (command.equals("check")) {
      check(physical != null, "prepare a recipe-441 physical check first");
      physical.checkResult(mc); return;
    }
    String[] words = command.split("\\s+");
    check(words.length == 2 && words[0].equals("prepare") && Set.of("variants", "max", "wheel").contains(words[1]),
        "use recipe-441 prepare variants|max|wheel, check, or stop");
    stopPhysical(mc);
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    check(mc.player instanceof PlayerMode mode && !mode.power$spectator() && !mode.power$creative(), "use actual Survival");
    check(!NativeInventoryPlan.failed() && !NativeInventoryPlan.busy() && !InventoryActions.busy(), "start with healthy idle inventory actions");
    check(!Mouse.isButtonDown(0) && !Mouse.isButtonDown(1), "release mouse buttons");
    physical = new Physical(mc, words[1]);
    try { physical.prepare(mc); }
    catch (Exception | Error failure) {
      try { stopPhysical(mc); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
      throw failure;
    }
  }
  private static void stopPhysical(Minecraft mc) throws Exception {
    if (physical == null) return;
    physical.owner(mc);
    var previous = physical;
    try { previous.close(); }
    finally { physical = null; clearCapture(); }
    log("RECIPE441 PHYSICAL stopped; original inventory and recipe settings restored");
  }
  private static final class Physical implements AutoCloseable {
    private final Recipe402Checks.State state;
    private final Object player, world;
    private final String kind;
    private RecipeBookOverlay book;
    private RecipeDefinition recipe;
    private net.minecraft.client.gui.screen.Screen screen;
    private String before;
    private int cell, page, expectedPage, stage;
    private Object wheel, oldEnabled, oldInverted;
    private java.lang.reflect.Field enabled, inverted;
    Physical(Minecraft mc, String kind) throws Exception {
      player = mc.player; world = mc.world; this.kind = kind; state = new Recipe402Checks.State(mc);
    }
    void owner(Minecraft mc) { check(mc.player == player && mc.world == world, "staged recipe check changed world/player; retain its original owner for restoration"); }
    void prepare(Minecraft mc) throws Exception {
      var size = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
      state.reset(size.method_1857()); screen = mc.currentScreen;
      screen.init(mc, size.method_1857(), size.method_1858()); book = RecipeBooks.book(screen);
      check(book != null && !book.layout().narrow(), "enlarge the disposable window or reduce GUI scale for a wide recipe book, then prepare again");
      set(book, "category", 0); set(book, "filter", false);
      ((local.luke.power.ui.TextInput)field(book, "search")).setText(""); book.tick();
      var groups = liveGroups();
      int index = -1;
      if (kind.equals("variants")) {
        for (int i = 0; i < groups.size(); i++) if (groups.get(i).size() > 1) { index = i; break; }
      } else {
        recipe = recipe(book, 280);
        for (int i = 0; i < groups.size(); i++) if (groups.get(i).contains(recipe)) { index = i; break; }
        mc.player.inventory.main[9] = DeathChecks.tagged(5, kind.equals("max") ? 128 : 12, 0, "physical-" + kind);
      }
      check(index >= 0, "native recipe group was not displayed"); page = index / 20; cell = index % 20; set(book, "page", page);
      before = snapshot(mc, state.context()); capture(mc, state.context(), 0);
      if (kind.equals("wheel")) {
        check(groups.size() > 20, "physical wheel check needs multiple native recipe pages");
        Object inventory = Class.forName("local.luke.power.inventory.Config").getField("INVENTORY_TWEAKS_CONFIG").get(null);
        Object mouse = inventory.getClass().getField("MOUSE_TWEAKS_CONFIG").get(inventory);
        wheel = mouse.getClass().getField("SCROLL_WHEEL_CONFIG").get(mouse);
        enabled = wheel.getClass().getField("enableScrollWheelTweaks"); inverted = wheel.getClass().getField("invertScrollCursorSlotDirection");
        oldEnabled = enabled.get(wheel); oldInverted = inverted.get(wheel); enabled.set(wheel, true); inverted.set(wheel, false);
        expectedPage = page < (groups.size() - 1) / 20 ? page + 1 : page - 1;
      }
      instruction(mc, kind.equals("variants") ? "Alt+left-click; release; check" : kind.equals("max") ? "Shift+right-click; release; wait for publication; check"
          : (expectedPage > page ? "wheel DOWN once" : "wheel UP once") + " over untouched icon; check");
    }
    @SuppressWarnings("unchecked") private List<List<RecipeDefinition>> liveGroups() throws Exception { return (List<List<RecipeDefinition>>)field(book, "groups"); }
    void instruction(Minecraft mc, String action) throws Exception {
      var bounds = book.layout().cell(cell); int x = bounds.x() + 12, y = bounds.y() + 12;
      int px = (int)((x + .5) * mc.displayWidth / screen.width), py = (int)((y + .5) * mc.displayHeight / screen.height);
      log("RECIPE441 PHYSICAL " + kind + " stage=" + stage + " display=" + mc.displayWidth + "x" + mc.displayHeight
          + " scaled=" + screen.width + "x" + screen.height + " gui=" + x + "," + y + " client=" + px + "," + py + " page=" + page + " ACTION " + action);
    }
    void checkResult(Minecraft mc) throws Exception {
      owner(mc); check(mc.currentScreen == screen && RecipeBooks.book(screen) == book, "physical recipe screen changed");
      check(Display.isActive() && !Mouse.isButtonDown(0) && !Mouse.isButtonDown(1), "focus window and release mouse buttons before check");
      check(!NativeInventoryPlan.busy() && !NativeInventoryPlan.failed(), "wait for the healthy durable recipe publication before check");
      if (kind.equals("variants")) {
        check(((List<?>)field(book, "popup")).size() > 1, "native Alt-click did not open variants");
        check(before.equals(snapshot(mc, state.context())) && callbacks == 0, "physical variant popup changed native items");
      } else if (kind.equals("max")) {
        check(callbacks == 64 && total(mc, 280) == 256 && total(mc, 5) == 0, "native Shift-right-click did not craft all 64 batches");
        check(state.context().inputs.stream().allMatch(slot -> slot.getStack() == null), "physical maximum left crafting ingredients");
      } else {
        if (stage == 0) {
          check((int)field(book, "page") == expectedPage && before.equals(snapshot(mc, state.context())) && callbacks == 0,
              "untouched native wheel changed ingredients or failed to change pages");
          set(book, "page", page); stage++; instruction(mc, "plain left-click once; wait for publication; check"); return;
        }
        if (stage == 1 || stage == 3) {
          check(field(book, "lastRecipe") == recipe && state.context().output.getStack() != null,
              "physical recipe choice or wheel zero-to-one did not fill the requested recipe");
          check(state.context().inputs.stream().filter(slot -> slot.getStack() != null).allMatch(slot -> slot.getStack().count == 1), "physical recipe did not fill exactly one batch");
          check(total(mc, 5) == 10 && total(mc, 280) == 0 && callbacks == 0, "physical one-batch fill changed ingredient/result totals");
          if (stage == 1) { stage++; instruction(mc, "wheel DOWN once on the clicked icon; wait for publication; check"); return; }
        } else if (stage == 2) {
          check(state.context().inputs.stream().allMatch(slot -> slot.getStack() == null) && state.context().output.getStack() == null
              && total(mc, 5) == 12 && callbacks == 0, "physical wheel did not remove the last batch to zero");
          stage++; instruction(mc, "wheel UP once on the same icon; wait for publication; check"); return;
        }
      }
      log("PASS RECIPE441 PHYSICAL " + kind + "; use stop to restore or prepare the next case");
    }
    public void close() throws Exception {
      try { if (wheel != null && oldEnabled != null) { enabled.set(wheel, oldEnabled); inverted.set(wheel, oldInverted); } }
      finally { state.close(); }
    }
  }
  private static final class UnsupportedRecipe extends class_193 {
    UnsupportedRecipe() { super(1, 1, new ItemStack[]{new ItemStack(3, 1, 0)}, new ItemStack(264, 1, 0)); }
  }
  private static void full(Minecraft mc, int resultCount) { for (int i = 0; i < 36; i++) mc.player.inventory.main[i] = new ItemStack(1, 64, 0); mc.player.inventory.main[0] = new ItemStack(280, resultCount, 0); }
  private static void grid(RecipeContext context, RecipeDefinition recipe, ItemStack template) {
    var cells = recipe.layout(context.width, context.height); for (int i = 0; i < cells.size(); i++) context.inputs.get(i).setStack(cells.get(i) == null ? null : template.clone());
  }
  private static int total(Minecraft mc, int id) { int count = 0; for (var item : mc.player.inventory.main) if (item != null && item.itemId == id) count += item.count; return count; }
  private static String snapshot(Minecraft mc, RecipeContext context) throws Exception {
    List<String> values = new ArrayList<>(); values.add(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)));
    for (var slot : context.inputs) values.add(slot.getStack() == null ? null : DeathItems.encode(slot.getStack()));
    values.add(context.output.getStack() == null ? null : DeathItems.encode(context.output.getStack())); return values.toString();
  }
  private static RecipeDefinition recipe(RecipeBookOverlay book, int output) throws Exception { return recipes(book).stream().filter(value -> value.kind == RecipeDefinition.Kind.CRAFTING && value.output.itemId == output && value.layout(2, 2) != null).findFirst().orElseThrow(); }
  @SuppressWarnings("unchecked") private static List<RecipeDefinition> recipes(RecipeBookOverlay book) throws Exception { return (List<RecipeDefinition>)field(field(book, "model"), "recipes"); }
  private static List<RecipeDefinition> variants(RecipeBookOverlay book) throws Exception {
    Map<String, List<RecipeDefinition>> grouped = new LinkedHashMap<>();
    for (var recipe : recipes(book)) if (recipe.kind == RecipeDefinition.Kind.CRAFTING && recipe.layout(2, 2) != null) grouped.computeIfAbsent(recipe.group, ignored -> new ArrayList<>()).add(recipe);
    return grouped.values().stream().filter(values -> values.size() > 1).findFirst().orElseThrow();
  }
  private static void groups(RecipeBookOverlay book, List<List<RecipeDefinition>> groups) throws Exception { set(book, "groups", groups); set(book, "page", 0); }
  private static void click(RecipeBookOverlay book, int button) { var cell = book.layout().cell(0); check(book.mouse(cell.x() + 10, cell.y() + 10, button), "recipe click was not owned"); }
  private static void modifier(int key, Check action) throws Exception {
    var down = KeyEventModifiers.class.getDeclaredField("down"); down.setAccessible(true); int old = down.getInt(null);
    KeyEventModifiers.reconcile(0); KeyEventModifiers.event(key, true); KeyEventModifiers.begin();
    try { action.run(); } finally { KeyEventModifiers.end(); KeyEventModifiers.reconcile(old); }
  }
  private static Object field(Object target, String name) throws Exception { var field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target); }
  private static void set(Object target, String name, Object value) throws Exception { var field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value); }
  private static void resetFailure() throws Exception { var failed = NativeInventoryPlan.class.getDeclaredField("failed"); failed.setAccessible(true); failed.setBoolean(null, false); }
  private static int[] air(Minecraft mc) {
    int x = (int)Math.floor(mc.player.x), y = Math.max(1, Math.min(126, (int)Math.floor(mc.player.boundingBox.minY))), z = (int)Math.floor(mc.player.z);
    for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) if (mc.world.method_239(x + dx, y, z + dz) && mc.world.getBlockId(x + dx, y, z + dz) == 0 && mc.world.method_1777(x + dx, y, z + dz) == null) return new int[]{x + dx, y, z + dz};
    throw new AssertionError("recipe fixture needs one nearby loaded air block");
  }
}
