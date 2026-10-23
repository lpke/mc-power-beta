package local.luke.power.inventory;

import java.io.IOException;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.mixin.DeathSlotAccessor;
import local.luke.power.offhand.*;
import local.luke.power.permissions.CommandPermissions;
import local.luke.power.rules.GameRule;
import net.minecraft.class_277;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.opengl.Display;

/** Native Creative deletion is an explicit, flushed removal, never a catalog replacement. */
public final class CreativeDeletion {
  public interface Host { boolean power$deleteSlotAt(int x, int y); }
  static { CreativePaletteBridge.registerDeletion((client, all) -> client instanceof Minecraft mc && destroy(mc, all)); }
  private CreativeDeletion() {}
  public static boolean enabled(Minecraft mc) {
    return mc != null && mc.world != null && !mc.world.isRemote && mc.player != null && mc.player.world == mc.world
        && mc.player instanceof PlayerMode mode && mode.power$creativeInventory() && mode.power$inventoryEditable()
        && CommandPermissions.allowed(GameRule.ALLOW_CREATIVE_MODE)
        && mc.currentScreen instanceof ContainerScreen screen && OffhandContainers.supported(screen)
        && mc.player.container == screen.container && mc.player.playerContainer.getClass() == class_277.class
        && (screen.getClass() != net.minecraft.class_585.class || screen.container == mc.player.playerContainer)
        && OffhandContainers.owner(screen.container) == mc.player.inventory
        && mc.player.health > 0 && !mc.player.dead && screen.container.method_2094(mc.player)
        && mc.field_2807 == mc.player && !DetachedCamera.isActive() && !InteractionState.carryingContainer
        && local.luke.power.world.WorldCompatibility.ready(mc.world);
  }
  public static boolean destroySetting() { return setting("destroySlot"); }
  public static boolean clearSetting() { return setting("shiftClearsInventory"); }
  private static final class Settings {
    static final java.lang.reflect.Method current;
    static final java.lang.reflect.Field destroy, clear;
    static {
      java.lang.reflect.Method getter = null; java.lang.reflect.Field destroyField = null, clearField = null;
      try {
        getter = Class.forName("local.luke.power.creative.config.Config").getMethod("current");
        destroyField = getter.getReturnType().getField("destroySlot"); clearField = getter.getReturnType().getField("shiftClearsInventory");
      } catch (ReflectiveOperationException unavailable) { getter = null; }
      current = getter; destroy = destroyField; clear = clearField;
    }
  }
  private static boolean setting(String name) {
    if (Settings.current == null) return false;
    try {
      Object config = Settings.current.invoke(null);
      return (name.equals("destroySlot") ? Settings.destroy : Settings.clear).getBoolean(config);
    } catch (ReflectiveOperationException unavailable) { return false; }
  }
  public static boolean destroy(Minecraft mc, boolean all) {
    if (!enabled(mc) || !Display.isActive() || InventoryPanels.modal(mc.currentScreen)
        || !destroySetting() && !(mc.currentScreen instanceof InventoryGui gui && gui.power$usesCatalogWheel())
        || NativeInventoryPlan.busy() || NativeInventoryPlan.failed()) return false;
    all &= clearSetting();
    final var screen = (ContainerScreen)mc.currentScreen; final var player = mc.player;
    final var inventory = player.inventory; final var handler = player.playerContainer;
    final boolean clear = all;
    try {
      var layout = new Layout(screen.container); var personalLayout = new Layout(handler);
      List<NativeInventoryPlan.Change> changes = new ArrayList<>();
      changes.add(NativeInventoryPlan.Change.cursor(inventory, null));
      Slot result = handler.method_2084(0); ItemStack originalResult = ItemStack.clone(result.getStack());
      ItemStack[] latest = {result.getStack()}; String[] latestBytes = {encode(latest[0])};
      java.util.function.BooleanSupplier outputOwned = () -> {
        try { return layout.owned() && personalLayout.owned() && player.playerContainer == handler && result == handler.method_2084(0)
            && result.getStack() == latest[0] && Objects.equals(latestBytes[0], encode(result.getStack())); }
        catch (IOException invalid) { return false; }
      };
      if (clear) {
        for (int i = 0; i < 36; i++) changes.add(NativeInventoryPlan.Change.main(inventory, i, null));
        for (int i = 0; i < 4; i++) {
          final int index = i;
          changes.add(new NativeInventoryPlan.Change("armor." + i, inventory, 36 + i, () -> inventory.armor[index],
              stack -> { inventory.armor[index] = stack; inventory.markDirty(); }, Objects::isNull, () -> 1, true, null));
        }
        changes.add(NativeInventoryPlan.Change.offhand(inventory, null));
        for (int i = 1; i <= 4; i++) {
          var base = NativeInventoryPlan.Change.slot(handler.method_2084(i), null);
          changes.add(new NativeInventoryPlan.Change(base.label(), base.owner(), base.index(), base.get(), stack -> {
            try { base.set().accept(stack); }
            finally { latest[0] = result.getStack(); try { latestBytes[0] = encode(latest[0]); }
              catch (IOException invalid) { throw new IllegalStateException(invalid); } }
          }, base.accepts(), base.limit(), base.wholeOversize(), null));
        }
      }
      // Enlist each external native owner without changing it, including furnace outputs.
      var seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
      for (Object entry : screen.container.slots) if (entry instanceof Slot slot) {
        Object owner = ((DeathSlotAccessor)(Object)slot).power$deathInventory();
        if (owner != inventory && !(owner instanceof OffhandSlotInventory) && owner != ((class_277)handler).field_1124
            && owner.getClass() != net.minecraft.class_507.class && seen.add(owner))
          changes.add(NativeInventoryPlan.Change.slot(slot, slot.getStack()));
      }
      return NativeInventoryPlan.submitDeletion(mc, changes,
          () -> mc.player == player && mc.currentScreen == screen && enabled(mc) && (!clear || clearSetting()) && outputOwned.getAsBoolean(),
          () -> { if (!outputOwned.getAsBoolean()) throw new IllegalStateException("Creative deletion lost recipe output ownership"); },
          () -> {
            if (!outputOwned.getAsBoolean()) throw new IllegalStateException("Creative deletion cannot restore a foreign result");
            var access = (DeathSlotAccessor)(Object)result;
            access.power$deathInventory().setStack(access.power$deathIndex(), originalResult);
            latest[0] = originalResult; try { latestBytes[0] = encode(originalResult); }
            catch (IOException invalid) { throw new IllegalStateException(invalid); }
          });
    } catch (IOException invalid) { return false; }
  }
  public static boolean contains(Minecraft mc, ContainerScreen screen, int width, int height, int px, int py) {
    if (mc.currentScreen != screen || !enabled(mc) || !destroySetting() || InventoryPanels.modal(screen)
        || InventoryPanels.contains(screen, px, py)) return false;
    int x = (screen.width - width) / 2 + InventoryPanels.offset(screen), y = (screen.height - height) / 2;
    int hotbar = OffhandContainers.hotbarY(screen.container, mc.player.inventory);
    return InventorySideSlots.containsSlot(px, py, x, y, hotbar - (Offhand.visible(mc.player) ? 22 : 0));
  }
  private static String encode(ItemStack stack) throws IOException { return stack == null ? null : DeathItems.encode(stack); }
  /** Contents alone cannot authorize a write after a native slot changes inventory ownership. */
  private static final class Layout {
    final net.minecraft.inventory.Container handler;
    final List<Slot> slots = new ArrayList<>();
    final List<net.minecraft.inventory.Inventory> owners = new ArrayList<>();
    final List<Integer> indices = new ArrayList<>(), ids = new ArrayList<>();
    Layout(net.minecraft.inventory.Container handler) throws IOException {
      this.handler = handler;
      if (handler.slots.size() > 128) throw new IOException("Unsupported Creative container layout");
      for (Object entry : handler.slots) {
        if (!(entry instanceof Slot slot)) throw new IOException("Unsupported Creative slot");
        var access = (DeathSlotAccessor)(Object)slot;
        slots.add(slot); owners.add(access.power$deathInventory()); indices.add(access.power$deathIndex()); ids.add(slot.id);
      }
    }
    boolean owned() {
      if (handler.slots.size() != slots.size()) return false;
      for (int i = 0; i < slots.size(); i++) {
        Slot slot = slots.get(i); var access = (DeathSlotAccessor)(Object)slot;
        if (handler.slots.get(i) != slot || slot.id != ids.get(i) || access.power$deathInventory() != owners.get(i)
            || access.power$deathIndex() != indices.get(i)) return false;
      }
      return true;
    }
  }
}
