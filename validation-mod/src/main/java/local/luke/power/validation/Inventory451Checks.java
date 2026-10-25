package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.*;
import local.luke.power.visual.VisualConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Hotbar-to-personal-grid fallback on isolated native inventory arrays. */
final class Inventory451Checks {
  private static State staged;
  private Inventory451Checks() {}

  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("stop")) {
      if (staged != null) { staged.inventory.owner(mc); try { staged.close(); } finally { staged = null; } }
      log("INVENTORY451 stopped; original inventory/settings/mode restored"); return;
    }
    if (action.equals("prepare shift")) {
      check(staged == null, "stop the previous inventory-451 physical check"); guard(mc); staged = new State(mc);
      try {
        staged.reset(true); var handler = mc.player.playerContainer;
        mc.player.inventory.main[0] = DeathChecks.tagged(4, 7, 0, "hotbar-first");
        mc.player.inventory.main[1] = DeathChecks.tagged(35, 5, 4, "hotbar-second");
        mc.player.inventory.main[2] = DeathChecks.tagged(35, 3, 14, "hotbar-third");
        staged.sources = List.of(bytes(mc.player.inventory.main[0]), bytes(mc.player.inventory.main[1]), bytes(mc.player.inventory.main[2]));
        for (int id = 36; id < 39; id++) coordinates(mc, staged.inventory.screen, handler.method_2084(id), "source" + id);
        log("INVENTORY451 ACTION hold Shift, press left on source36, pause for publication, drag through source37 and38 with a pause at each; release both; check");
      } catch (Exception | Error failure) { try { staged.close(); } finally { staged = null; } throw failure; }
      return;
    }
    if (action.equals("check")) {
      check(staged != null, "prepare inventory-451 physical shift first"); staged.inventory.owner(mc);
      check(!Mouse.isButtonDown(0) && !NativeInventoryPlan.busy() && !NativeInventoryPlan.failed(), "release mouse and wait for durable publication");
      check(mc.currentScreen == staged.inventory.screen, "physical native screen changed");
      for (int i = 0; i < 3; i++) {
        check(mc.player.inventory.main[i] == null, "physical Shift-drag left hotbar source " + i);
        check(staged.sources.get(i).equals(bytes(staged.inventory.inventory.context().inputs.get(i).getStack())), "physical drag lost native source data " + i);
      }
      check(mc.player.inventory.getCursorStack() == null && Boolean.FALSE.equals(staged.inventory.storage.get(staged.inventory.compatibility)), "physical drag changed cursor/storage setting");
      log("PASS INVENTORY451 physical Shift-drag moved three hotbar sources with crafting storage disabled"); return;
    }
    check(action.equals("test"), "use inventory-451 test|prepare shift|check|stop");
    check(staged == null, "stop the physical check before direct tests"); guard(mc); failures = 0;
    try (var state = new State(mc)) {
      test("INVENTORY451 native hotbar Shift-click waits for its journal and preserves full data", () -> {
        state.reset(false); ItemStack source = DeathChecks.tagged(35, 17, 4, "hotbar-native"); mc.player.inventory.main[0] = source;
        String before = bytes(source);
        try (var slow = new AutoTools400State.SlowWriter()) {
          mc.player.playerContainer.onSlotClick(36, 0, true, mc.player); slow.checkBlocked();
          check(mc.player.inventory.main[0] == source && before.equals(bytes(source)) && state.inputs().stream().allMatch(slot -> slot.getStack() == null), "fallback wrote before journal flush");
          slow.release(); state.finish();
        }
        check(mc.player.inventory.main[0] == null && before.equals(bytes(state.inputs().get(0).getStack()))
            && mc.player.inventory.getCursorStack() == null && !NativeInventoryPlan.failed(), "native fallback lost source count/damage/NBT");
      });
      test("INVENTORY451 normal storage keeps priority for empty and matching partial slots", () -> {
        for (boolean empty : new boolean[]{true, false}) {
          state.reset(false); var source = DeathChecks.tagged(35, 7, 4, "storage-first"); mc.player.inventory.main[0] = source;
          mc.player.inventory.main[9] = empty ? null : copy(source, 63); String before = state.inventory.inventory.snapshot(), sourceData = bytes(source);
          check(!CraftingGridTransfer.transfer(mc.player.playerContainer, 36, mc.player) && !NativeInventoryPlan.busy()
              && before.equals(state.inventory.inventory.snapshot()), "fallback claimed available native storage");
          if (empty) {
            mc.player.playerContainer.onSlotClick(36, 0, true, mc.player);
            check(mc.player.inventory.main[0] == null && sourceData.equals(bytes(mc.player.inventory.main[9]))
                && state.inputs().stream().allMatch(slot -> slot.getStack() == null), "normal storage transfer used personal grid");
          }
        }
      });
      test("INVENTORY451 partial grid capacity merges exact NBT and leaves native leftovers", () -> {
        state.reset(false); var source = DeathChecks.tagged(35, 12, 4, "partial-grid"); mc.player.inventory.main[0] = source;
        state.inputs().get(0).setStack(copy(source, 62));
        state.inputs().get(1).setStack(DeathChecks.tagged(35, 1, 4, "different-nbt"));
        state.inputs().get(2).setStack(DeathChecks.tagged(35, 1, 14, "different-damage"));
        state.inputs().get(3).setStack(new ItemStack(267, 1, 0));
        String foreign = bytes(state.inputs().get(1).getStack()), damage = bytes(state.inputs().get(2).getStack());
        check(CraftingGridTransfer.transfer(mc.player.playerContainer, 36, mc.player), "partial fallback declined"); state.finish();
        check(bytes(copy(source, 10)).equals(bytes(mc.player.inventory.main[0])) && bytes(copy(source, 64)).equals(bytes(state.inputs().get(0).getStack()))
            && foreign.equals(bytes(state.inputs().get(1).getStack())) && damage.equals(bytes(state.inputs().get(2).getStack()))
            && mc.player.inventory.getCursorStack() == null && !NativeInventoryPlan.failed(), "partial fallback changed native data or counts");
      });
      test("INVENTORY451 full grid and disabled fallback preserve source bytes", () -> {
        state.reset(false); mc.player.inventory.main[0] = DeathChecks.tagged(4, 7, 0, "full-grid");
        for (var input : state.inputs()) input.setStack(new ItemStack(267, 1, 0));
        String before = state.inventory.inventory.snapshot(); mc.player.playerContainer.onSlotClick(36, 0, true, mc.player);
        check(before.equals(state.inventory.inventory.snapshot()) && !NativeInventoryPlan.busy(), "full grid changed source");
        state.reset(false); mc.player.inventory.main[0] = DeathChecks.tagged(4, 7, 0, "disabled-grid");
        var settings = VisualConfig.copy(); settings.shiftClickIntoCraftingGrid = false; VisualConfig.preview(settings);
        before = state.inventory.inventory.snapshot(); mc.player.playerContainer.onSlotClick(36, 0, true, mc.player);
        check(before.equals(state.inventory.inventory.snapshot()) && !NativeInventoryPlan.busy(), "disabled fallback moved items");
      });
      test("INVENTORY451 queued hotbar transfers acknowledge publication and reject foreign visited mutations", () -> {
        for (boolean foreign : new boolean[]{false, true}) {
          state.reset(false); var handler = mc.player.playerContainer;
          mc.player.inventory.main[0] = DeathChecks.tagged(4, 7, 0, "queued-hotbar-first");
          mc.player.inventory.main[1] = DeathChecks.tagged(35, 5, 4, "queued-hotbar-second");
          mc.player.inventory.main[2] = DeathChecks.tagged(5, 3, 0, "visited-hotbar-third");
          String first = bytes(mc.player.inventory.main[0]), second = bytes(mc.player.inventory.main[1]);
          Object ownership = inheritedField(state.inventory.screen, "powerInventory$dragOwnership");
          ownership.getClass().getMethod("capture", Object.class, ItemStack.class, Iterable.class)
              .invoke(ownership, handler, null, List.of(handler.method_2084(36), handler.method_2084(37), handler.method_2084(38)));
          check(CraftingGridTransfer.transfer(handler, 36, mc.player) && CraftingGridTransfer.transfer(handler, 37, mc.player), "hotbar queue declined");
          state.finish();
          check(mc.player.inventory.main[0] == null && first.equals(bytes(state.inputs().get(0).getStack())), "first hotbar publication failed");
          if (foreign) mc.player.inventory.main[2].count++;
          CraftingGridTransfer.tick(mc); state.finish();
          check(foreign ? second.equals(bytes(mc.player.inventory.main[1])) && mc.player.inventory.main[2].count == 4
              : mc.player.inventory.main[1] == null && second.equals(bytes(state.inputs().get(1).getStack())), "hotbar acknowledgment accepted foreign bytes or canceled its own queue");
          check(!NativeInventoryPlan.failed(), "ordinary drag cancellation stopped inventory actions");
        }
      });
      test("INVENTORY451 changed storage cancels an unpublished hotbar fallback", () -> {
        state.reset(false); mc.player.inventory.main[0] = DeathChecks.tagged(4, 7, 0, "stale-storage"); String source = bytes(mc.player.inventory.main[0]);
        check(CraftingGridTransfer.transfer(mc.player.playerContainer, 36, mc.player), "stale-storage fixture declined");
        mc.player.inventory.main[9] = null; state.finish();
        check(source.equals(bytes(mc.player.inventory.main[0])) && state.inputs().stream().allMatch(slot -> slot.getStack() == null)
            && !NativeInventoryPlan.failed(), "stale storage baseline published or stopped actions");
      });
    }
    log("INVENTORY451 failures=" + failures); check(failures == 0, "inventory-451 failures=" + failures);
  }

  private static void guard(Minecraft mc) {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable native singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute disposable client");
    check(!NativeInventoryPlan.busy() && !NativeInventoryPlan.failed() && !Mouse.isButtonDown(0) && !Mouse.isButtonDown(1), "finish native actions and release mouse");
  }
  private static ItemStack copy(ItemStack stack, int count) { ItemStack result = ItemStack.clone(stack); result.count = count; return result; }
  private static String bytes(ItemStack stack) throws Exception { return stack == null ? null : DeathItems.encode(stack); }
  private static Object inheritedField(Object target, String name) throws Exception {
    for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
      try { var field = type.getDeclaredField(name); field.setAccessible(true); return field.get(target); }
      catch (NoSuchFieldException inherited) { /* Continue through the native screen hierarchy. */ }
    }
    throw new NoSuchFieldException(name);
  }
  private static void coordinates(Minecraft mc, net.minecraft.client.gui.screen.container.ContainerScreen screen, Slot slot, String label) throws Exception {
    var bounds = (local.luke.power.validation.mixin.ContainerInput)(Object)screen;
    int x = (screen.width - bounds.power$width()) / 2 + InventoryPanels.offset(screen) + slot.x + 8;
    int y = (screen.height - bounds.power$height()) / 2 + slot.y + 8;
    log("INVENTORY451 " + label + " client=" + (int)((x + .5) * mc.displayWidth / screen.width) + "," + (int)((y + .5) * mc.displayHeight / screen.height));
  }
  private static final class State implements AutoCloseable {
    final Minecraft mc; final Inventory442Checks.State inventory;
    final Object mouse; final java.lang.reflect.Field drag; final Object previousDrag;
    List<String> sources;
    State(Minecraft mc) throws Exception {
      this.mc = mc;
      Object config = Class.forName("local.luke.power.inventory.Config").getField("INVENTORY_TWEAKS_CONFIG").get(null);
      mouse = config.getClass().getField("MOUSE_TWEAKS_CONFIG").get(config); drag = mouse.getClass().getField("LMBTweakShiftClickAny"); previousDrag = drag.get(mouse);
      inventory = new Inventory442Checks.State(mc);
    }
    void reset(boolean nativeSize) throws Exception {
      inventory.reset(false, nativeSize); drag.set(mouse, true);
      for (int i = 9; i < 36; i++) mc.player.inventory.main[i] = new ItemStack(267, 1, 0);
    }
    List<Slot> inputs() { return inventory.inventory.context().inputs; }
    void finish() throws Exception { inventory.inventory.finish(); }
    public void close() throws Exception { try { inventory.close(); } finally { drag.set(mouse, previousDrag); } }
  }
}
