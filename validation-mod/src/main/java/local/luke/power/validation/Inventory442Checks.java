package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.*;
import local.luke.power.offhand.*;
import local.luke.power.recipes.*;
import local.luke.power.validation.mixin.*;
import local.luke.power.visual.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Native Creative deletion and physical Shift-drag, with isolated inventory arrays. */
final class Inventory442Checks {
  private static State staged;
  private Inventory442Checks() {}
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("stop")) {
      if (staged != null) { staged.owner(mc); try { staged.close(); } finally { staged = null; } }
      log("INVENTORY442 stopped; original inventory/settings/mode restored"); return;
    }
    if (action.equals("prepare shift")) {
      check(staged == null, "stop the previous inventory-442 physical check"); guard(mc);
      staged = new State(mc);
      try {
        staged.reset(false, true); var context = staged.inventory.context();
        for (int i = 0; i < 9; i++) mc.player.inventory.main[i] = new ItemStack(267, 1, 0);
        mc.player.inventory.main[9] = DeathChecks.tagged(4, 7, 0, "physical-first");
        mc.player.inventory.main[10] = DeathChecks.tagged(35, 5, 4, "physical-second");
        mc.player.inventory.main[11] = DeathChecks.tagged(35, 3, 14, "physical-third");
        staged.sources = List.of(bytes(mc.player.inventory.main[9]), bytes(mc.player.inventory.main[10]), bytes(mc.player.inventory.main[11]));
        for (int id = 9; id <= 11; id++) coordinates(mc, (ContainerScreen)mc.currentScreen, context.handler.method_2084(id), "source" + id);
        log("INVENTORY442 ACTION hold Shift, press left on source9, pause for publication, drag through source10 and11 with a pause at each; release both; check");
      } catch (Exception | Error failure) { try { staged.close(); } finally { staged = null; } throw failure; }
      return;
    }
    if (action.equals("check")) {
      check(staged != null, "prepare inventory-442 physical shift first"); staged.owner(mc);
      check(!Mouse.isButtonDown(0) && !NativeInventoryPlan.busy() && !NativeInventoryPlan.failed(), "release mouse and wait for durable publication");
      check(mc.currentScreen == staged.screen, "physical native screen changed");
      for (int i = 0; i < 3; i++) {
        check(mc.player.inventory.main[9 + i] == null, "physical Shift-drag left source " + (9 + i));
        check(staged.sources.get(i).equals(bytes(staged.inventory.context().inputs.get(i).getStack())), "physical drag lost source NBT/count " + i);
      }
      check(mc.player.inventory.getCursorStack() == null && Boolean.FALSE.equals(staged.storage.get(staged.compatibility)), "physical drag changed cursor/storage setting");
      log("PASS INVENTORY442 physical queued Shift-drag moved all three full-data sources after separate durable publications with crafting storage off"); return;
    }
    check(action.equals("test"), "use inventory-442 test|prepare shift|check|stop");
    check(staged == null, "stop the physical check before direct tests"); guard(mc); failures = 0;
    try (var state = new State(mc)) {
      test("INVENTORY442 drag acknowledgment admits only verified slots and rejects unrelated changes", () -> {
        state.reset(false, false); var handler = mc.player.playerContainer; Slot owned = handler.method_2084(9), foreign = handler.method_2084(10);
        owned.setStack(DeathChecks.tagged(4, 7, 0, "owned-drag")); foreign.setStack(DeathChecks.tagged(35, 5, 4, "foreign-drag"));
        var type = Class.forName("local.luke.power.inventory.DragOwnership"); Object guard = type.getConstructor().newInstance();
        var capture = type.getMethod("capture", Object.class, ItemStack.class, Iterable.class);
        var accept = type.getMethod("accept", Object.class, ItemStack.class, Iterable.class);
        var current = type.getMethod("current", Object.class, ItemStack.class);
        capture.invoke(guard, handler, null, List.of(owned, foreign)); owned.setStack(null);
        check((boolean)accept.invoke(guard, handler, null, List.of(owned)) && (boolean)current.invoke(guard, handler, null), "owned publication canceled drag guard");
        capture.invoke(guard, handler, null, List.of(owned, foreign)); foreign.getStack().count++;
        check(!(boolean)accept.invoke(guard, handler, null, List.of(owned)), "acknowledgment swallowed an unrelated visited-slot mutation");
        capture.invoke(guard, handler, null, List.of(owned, foreign)); mc.player.inventory.setCursorStack(new ItemStack(3, 1, 0));
        check(!(boolean)accept.invoke(guard, handler, mc.player.inventory.getCursorStack(), List.of(owned)), "acknowledgment swallowed a foreign cursor");
      });
      test("INVENTORY442 cursor deletion waits for immutable journal and preserves every other slot", () -> {
        state.reset(true, false); mc.player.inventory.main[9] = DeathChecks.tagged(35, 17, 4, "kept");
        var cursor = DeathChecks.tagged(267, 1, 71, "delete-cursor"); mc.player.inventory.setCursorStack(cursor);
        String inventory = bytes(mc.player.inventory.main[9]);
        try (var slow = new AutoTools400State.SlowWriter()) {
          check(CreativeDeletion.destroy(mc, false), "native cursor deletion declined"); slow.checkBlocked();
          check(mc.player.inventory.getCursorStack() == cursor && bytes(mc.player.inventory.main[9]).equals(inventory), "deletion wrote before journal flush");
          slow.release(); state.inventory.finish();
        }
        check(mc.player.inventory.getCursorStack() == null && inventory.equals(bytes(mc.player.inventory.main[9])), "cursor deletion changed unrelated bytes");
      });
      test("INVENTORY442 queued Shift transfers reject earlier visited-slot mutations before preparing another baseline", () -> {
        for (boolean afterFirstPublication : new boolean[]{false, true}) {
          state.reset(false, false); var handler = mc.player.playerContainer;
          for (int i = 0; i < 9; i++) mc.player.inventory.main[i] = new ItemStack(267, 1, 0);
          mc.player.inventory.main[9] = DeathChecks.tagged(4, 7, 0, "queued-first");
          mc.player.inventory.main[10] = DeathChecks.tagged(35, 5, 4, "queued-second");
          mc.player.inventory.main[11] = DeathChecks.tagged(5, 3, 0, "foreign-visited");
          String first = bytes(mc.player.inventory.main[9]), second = bytes(mc.player.inventory.main[10]);
          Object ownership = inheritedField(state.screen, "powerInventory$dragOwnership");
          ownership.getClass().getMethod("capture", Object.class, ItemStack.class, Iterable.class)
              .invoke(ownership, handler, null, List.of(handler.method_2084(9), handler.method_2084(10), handler.method_2084(11)));
          check(CraftingGridTransfer.transfer(handler, 9, mc.player) && CraftingGridTransfer.transfer(handler, 10, mc.player), "queued mutation fixture did not prepare both sources");
          if (afterFirstPublication) state.inventory.finish();
          mc.player.inventory.main[11].count++;
          state.inventory.finish(); CraftingGridTransfer.tick(mc); state.inventory.finish();
          check(second.equals(bytes(mc.player.inventory.main[10])) && mc.player.inventory.main[11].count == 4 && !NativeInventoryPlan.failed(),
              "queued plan accepted a foreign visited-slot baseline or overwrote its data");
          if (afterFirstPublication) check(mc.player.inventory.main[9] == null && first.equals(bytes(state.inventory.context().inputs.get(0).getStack())), "valid first publication lost its owned acknowledgement");
          else check(first.equals(bytes(mc.player.inventory.main[9])) && state.inventory.context().inputs.stream().allMatch(slot -> slot.getStack() == null), "stale first publication moved its source");
        }
      });
      test("INVENTORY442 clear inventory includes equipment, offhand and personal inputs with owned derived output", () -> {
        state.reset(true, false); mc.player.inventory.main[9] = DeathChecks.tagged(4, 17, 0, "main");
        mc.player.inventory.armor[0] = DeathChecks.tagged(298, 1, 17, "armor");
        mc.player.inventory.setCursorStack(DeathChecks.tagged(264, 3, 0, "cursor")); Offhand.set(mc.player.inventory, DeathChecks.tagged(35, 11, 5, "hand"));
        state.inventory.context().inputs.get(0).setStack(new ItemStack(17, 3, 0));
        check(state.inventory.context().output.getStack() != null, "clear fixture has no derived output");
        check(CreativeDeletion.destroy(mc, true), "native clear declined"); state.inventory.finish();
        check(Arrays.stream(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)).allMatch(Objects::isNull)
            && state.inventory.context().output.getStack() == null, "clear left owned player items or stale output");
      });
      test("INVENTORY442 clear callback failure rolls back original full native input and output", () -> {
        state.reset(true, false); mc.player.inventory.main[9] = DeathChecks.tagged(4, 17, 0, "rollback-main");
        state.inventory.context().inputs.get(0).setStack(DeathChecks.tagged(5, 3, 0, "rollback-grid"));
        String before = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
        String output = bytes(state.inventory.context().output.getStack()); var handler = mc.player.playerContainer;
        Slot original = handler.method_2084(3); var access = (local.luke.power.mixin.DeathSlotAccessor)(Object)original;
        var faulty = new Slot(access.power$deathInventory(), access.power$deathIndex(), original.x, original.y) {
          boolean first = true;
          @Override public void setStack(ItemStack item) { super.setStack(item); if (first) { first = false; throw new IllegalStateException("Expected clear callback failure"); } }
        };
        faulty.id = 3; handler.slots.set(3, faulty);
        try {
          // Ensure this setter has a real owned change to publish.
          access.power$deathInventory().setStack(access.power$deathIndex(), DeathChecks.tagged(5, 2, 0, "rollback-second"));
          before = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)); output = bytes(state.inventory.context().output.getStack());
          check(CreativeDeletion.destroy(mc, true), "clear rollback plan declined"); state.inventory.finish();
          check(NativeInventoryPlan.failed() && before.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)))
              && Objects.equals(output, bytes(state.inventory.context().output.getStack())), "clear rollback lost owned native bytes/output");
        } finally { handler.slots.set(3, original); var failed = NativeInventoryPlan.class.getDeclaredField("failed"); failed.setAccessible(true); failed.setBoolean(null, false); }
      });
      test("INVENTORY442 every supported native container shares actual hotbar delete geometry and external owner guard", () -> {
        state.reset(true, false);
        var chest = new TestInventory(27); chest.setStack(0, DeathChecks.tagged(35, 11, 5, "external"));
        int[] dispenser = state.block(23), furnace = state.block(61), table = state.block(58);
        List<ContainerScreen> screens = List.of(new class_264(mc.player.inventory, chest),
            new class_264(mc.player.inventory, new class_320("Inventory442 double", chest, new TestInventory(27))),
            new class_235(mc.player.inventory, (class_137)mc.world.method_1777(dispenser[0], dispenser[1], dispenser[2])),
            new class_524(mc.player.inventory, (class_138)mc.world.method_1777(furnace[0], furnace[1], furnace[2])),
            new class_516(mc.player.inventory, mc.world, table[0], table[1], table[2]));
        for (var screen : screens) {
          state.open(screen, false); int hotbar = OffhandContainers.hotbarY(screen.container, mc.player.inventory);
          var bounds = (ContainerInput)(Object)screen; int x = (screen.width - bounds.power$width()) / 2 + InventoryPanels.offset(screen);
          int y = (screen.height - bounds.power$height()) / 2;
          check(((CreativeDeletion.Host)screen).power$deleteSlotAt(x - 12, y + hotbar - 14), "delete hit region does not follow native hotbar");
          check(((InventoryGui)screen).power$containsGuiSpace(x - 12, y + hotbar - 14), "delete region became inventory-look/outside space");
          var hands = OffhandConfig.copy(); var noHand = OffhandConfig.copy(); noHand.enabled = false; OffhandConfig.preview(noHand);
          try {
            check(((CreativeDeletion.Host)screen).power$deleteSlotAt(x - 12, y + hotbar + 8), "delete slot did not occupy native hotbar row with offhand disabled");
            var paddingCursor = DeathChecks.tagged(35, 11, 5, "delete-frame-padding"); mc.player.inventory.setCursorStack(paddingCursor);
            String paddingBytes = bytes(paddingCursor);
            ((ScreenInput)screen).power$click(x - 19, y + hotbar - 5, 0);
            ((ScreenInput)screen).power$release(x - 19, y + hotbar - 5, 0);
            check(mc.player.inventory.getCursorStack() == paddingCursor && paddingBytes.equals(bytes(paddingCursor)), "delete-only frame padding dropped/changed cursor");
            check(OffhandSlotClick.nativeSlot(mc, -999) == -999, "delete-frame outside-click scope leaked after its press");
          }
          finally { OffhandConfig.preview(hands); }
          mc.player.inventory.setCursorStack(DeathChecks.tagged(264, 3, 0, "delete-external"));
          String external = bytes(chest.getStack(0));
          check(CreativeDeletion.destroy(mc, true), "native container deletion declined"); state.inventory.finish();
          check(mc.player.inventory.getCursorStack() == null, "container deletion retained cursor");
          check(Objects.equals(external, bytes(chest.getStack(0))), "clear inventory deleted external chest contents");
        }
        state.open(screens.get(0), false); var retained = DeathChecks.tagged(264, 3, 0, "stale-external"); mc.player.inventory.setCursorStack(retained);
        check(CreativeDeletion.destroy(mc, false), "stale external deletion was not prepared"); chest.setStack(1, new ItemStack(3, 1, 0));
        state.inventory.finish(); check(mc.player.inventory.getCursorStack() == retained && chest.getStack(1) != null && !NativeInventoryPlan.failed(), "stale external owner published or overwrote foreign data");
        check(CreativeDeletion.destroy(mc, false), "external layout-swap deletion was not prepared");
        Slot original = state.screen.container.method_2084(0); var replacement = new Slot(new TestInventory(27), 0, original.x, original.y); replacement.id = original.id;
        state.screen.container.slots.set(0, replacement);
        try {
          state.inventory.finish();
          check(mc.player.inventory.getCursorStack() == retained && state.screen.container.method_2084(0) == replacement && !NativeInventoryPlan.failed(),
              "pending deletion accepted a replaced external slot owner");
        } finally { state.screen.container.slots.set(0, original); }
      });
      test("INVENTORY442 Survival, remote worlds, unsupported screens and revoked permission cannot delete", () -> {
        state.reset(false, false); var cursor = DeathChecks.tagged(264, 3, 0, "denied"); mc.player.inventory.setCursorStack(cursor);
        check(!CreativeDeletion.destroy(mc, true), "Survival admitted Creative deletion");
        CreativeVehicleChecks.mode(mc, "CREATIVE"); boolean remote = mc.world.isRemote;
        try { mc.world.isRemote = true; check(!CreativeDeletion.destroy(mc, true), "remote admitted local deletion"); }
        finally { mc.world.isRemote = remote; }
        AccessRules.set(mc, local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE, false);
        check(!CreativeDeletion.destroy(mc, true) && mc.player.inventory.getCursorStack() == cursor, "revoked permission deleted items");
        AccessRules.set(mc, local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE, true);
        check(CreativeDeletion.destroy(mc, false), "permission-revocation fixture did not prepare");
        AccessRules.set(mc, local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE, false); state.inventory.finish();
        check(mc.player.inventory.getCursorStack() == cursor && !NativeInventoryPlan.failed(), "revoked pending deletion published or disabled healthy actions");
        AccessRules.set(mc, local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE, true);
        var unsupported = new class_264(mc.player.inventory, new TestInventory(27)) {}; state.open(unsupported, false);
        check(!CreativeDeletion.destroy(mc, true) && mc.player.inventory.getCursorStack() == cursor, "custom screen admitted deletion");
      });
    }
    log("INVENTORY442 failures=" + failures); check(failures == 0, "inventory-442 failures=" + failures);
  }
  private static void guard(Minecraft mc) {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable native singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute disposable client");
    check(!NativeInventoryPlan.busy() && !NativeInventoryPlan.failed() && !Mouse.isButtonDown(0) && !Mouse.isButtonDown(1), "finish native actions and release mouse");
  }
  private static String bytes(ItemStack stack) throws Exception { return stack == null ? null : DeathItems.encode(stack); }
  private static Object inheritedField(Object target, String name) throws Exception {
    for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
      try { var field = type.getDeclaredField(name); field.setAccessible(true); return field.get(target); }
      catch (NoSuchFieldException inherited) { /* Continue through the native screen hierarchy. */ }
    }
    throw new NoSuchFieldException(name);
  }
  private static void coordinates(Minecraft mc, ContainerScreen screen, Slot slot, String label) throws Exception {
    var bounds = (ContainerInput)(Object)screen;
    int x = (screen.width - bounds.power$width()) / 2 + InventoryPanels.offset(screen) + slot.x + 8;
    int y = (screen.height - bounds.power$height()) / 2 + slot.y + 8;
    log("INVENTORY442 " + label + " GUI=" + screen.width + "x" + screen.height + " display=" + mc.displayWidth + "x" + mc.displayHeight
        + " client=" + (int)((x + .5) * mc.displayWidth / screen.width) + "," + (int)((y + .5) * mc.displayHeight / screen.height));
  }
  static final class State implements AutoCloseable {
    final Minecraft mc; final Object player, world;
    final local.luke.power.rules.RuleSet rules; final String mode; final VisualSettings visual;
    final Object compatibility, creative; final java.lang.reflect.Field storage, destroy, clear; final Object previousStorage;
    final boolean previousDestroy, previousClear; final Recipe402Checks.State inventory;
    final List<ContainerScreen> screens = new ArrayList<>(); List<String> sources; ContainerScreen screen;
    final List<int[]> blocks = new ArrayList<>();
    State(Minecraft mc) throws Exception {
      this.mc = mc; player = mc.player; world = mc.world; rules = AccessRules.copy(mc);
      mode = mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString(); visual = VisualConfig.copy();
      compatibility = Class.forName("local.luke.power.compat.Config").getField("config").get(null);
      storage = compatibility.getClass().getField("allowCraftingInventorySlots"); previousStorage = storage.get(compatibility);
      creative = Class.forName("local.luke.power.creative.config.Config").getMethod("current").invoke(null);
      destroy = creative.getClass().getField("destroySlot"); clear = creative.getClass().getField("shiftClearsInventory");
      previousDestroy = destroy.getBoolean(creative); previousClear = clear.getBoolean(creative);
      inventory = new Recipe402Checks.State(mc);
    }
    void owner(Minecraft mc) { check(mc.player == player && mc.world == world, "keep the staged inventory check in its original world/player"); }
    void reset(boolean creativeMode, boolean nativeSize) throws Exception {
      AccessRules.set(mc, local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE, true); CreativeVehicleChecks.mode(mc, creativeMode ? "CREATIVE" : "SURVIVAL");
      destroy.setBoolean(creative, true); clear.setBoolean(creative, true); storage.set(compatibility, false);
      var settings = VisualConfig.copy(); settings.shiftClickIntoPlayerCraftingGrid = true; VisualConfig.preview(settings);
      inventory.reset(640); open((ContainerScreen)mc.currentScreen, nativeSize);
      if (creativeMode) field(screen, "creative_normalGUI", true);
    }
    void open(ContainerScreen value, boolean nativeSize) throws Exception {
      var recipes = RecipeBookConfig.copy(); recipes.enabled = false; RecipeBookConfig.preview(recipes);
      mc.currentScreen = value; mc.player.container = value.container; screen = value; screens.add(value);
      var size = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
      value.init(mc, nativeSize ? size.method_1857() : 640, nativeSize ? size.method_1858() : 360);
    }
    int[] block(int id) {
      int x = (int)Math.floor(mc.player.x), y = Math.max(1, Math.min(126, (int)Math.floor(mc.player.boundingBox.minY))), z = (int)Math.floor(mc.player.z);
      for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
        if (mc.world.method_239(x + dx, y, z + dz) && mc.world.getBlockId(x + dx, y, z + dz) == 0 && mc.world.method_1777(x + dx, y, z + dz) == null) {
          int[] point = {x + dx, y, z + dz, id}; blocks.add(point); mc.world.method_154(point[0], point[1], point[2], id, 0); return point;
        }
      throw new AssertionError("inventory-442 needs three nearby loaded air blocks for native containers");
    }
    public void close() throws Exception {
      owner(mc);
      try { inventory.close(); }
      finally {
        for (var value : screens) { var book = RecipeBooks.book(value); if (book != null) book.removed(); InventoryPanels.remove(value); }
        VisualConfig.preview(visual); storage.set(compatibility, previousStorage); destroy.setBoolean(creative, previousDestroy); clear.setBoolean(creative, previousClear);
        CreativeVehicleChecks.mode(mc, mode); AccessRules.restore(mc, rules);
        for (int[] point : blocks) if (mc.world.getBlockId(point[0], point[1], point[2]) == point[3]) mc.world.method_154(point[0], point[1], point[2], 0, 0);
      }
    }
  }
  private static final class TestInventory implements Inventory {
    final ItemStack[] items; TestInventory(int size) { items = new ItemStack[size]; }
    public int size() { return items.length; } public ItemStack getStack(int index) { return items[index]; }
    public ItemStack removeStack(int index, int count) { var item = items[index]; if (item == null) return null; if (item.count <= count) { items[index] = null; return item; } return item.split(count); }
    public void setStack(int index, ItemStack item) { items[index] = item; } public String getName() { return "Inventory442"; }
    public int getMaxCountPerStack() { return 64; } public void markDirty() {} public boolean canPlayerUse(PlayerEntity player) { return true; }
  }
}
