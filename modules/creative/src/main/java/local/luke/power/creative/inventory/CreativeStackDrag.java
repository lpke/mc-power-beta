package local.luke.power.creative.inventory;

import java.util.LinkedHashMap;
import java.util.Map;
import local.luke.power.creative.ClientRuntime;
import local.luke.power.rules.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.container.Container;
import net.minecraft.container.slot.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.Display;

/** Creative copies remain previews until the middle-button release commits them together. */
public final class CreativeStackDrag {
  private static boolean failed;
  private final Map<Slot, ItemStack> previews = new LinkedHashMap<>();
  private ItemStack cursor, snapshot;
  private boolean active;

  public boolean active() { return active; }

  public boolean allowed(Minecraft mc, ContainerScreen screen, Container container) {
    return !failed && ClientRuntime.local(mc) && Display.isActive()
        && mc.currentScreen == screen && mc.player.container == container
        && mc.player.creative_isCreative()
        && WorldRules.enabled(mc.level.getProperties(), GameRule.ALLOW_CREATIVE_MODE)
        && container.canUse(mc.player);
  }

  public boolean begin(Minecraft mc, ContainerScreen screen, Container container) {
    cancel();
    if (!allowed(mc, screen, container)) return false;
    ItemStack held = mc.player.inventory.getCursorItem();
    if (held == null || held.count <= 0) return false;
    cursor = held;
    snapshot = held.copy();
    active = true;
    return true;
  }

  public boolean valid(Minecraft mc, ContainerScreen screen, Container container) {
    return active && allowed(mc, screen, container)
        && mc.player.inventory.getCursorItem() == cursor
        && ItemStack.areStacksIdentical(cursor, snapshot);
  }

  public void hover(Minecraft mc, ContainerScreen screen, Container container, Slot slot) {
    if (!valid(mc, screen, container)) { cancel(); return; }
    if (eligible(container, slot)) previews.computeIfAbsent(slot, this::fullStack);
  }

  public ItemStack preview(Minecraft mc, ContainerScreen screen, Container container,
      Slot slot, ItemStack original) {
    return valid(mc, screen, container) && previews.containsKey(slot) && eligible(container, slot)
        ? previews.get(slot) : original;
  }

  private boolean eligible(Container container, Slot slot) {
    if (slot == null || slot.id < 0 || slot.id >= container.slots.size()
        || container.getSlot(slot.id) != slot || !slot.canInsert(snapshot)
        || Math.min(snapshot.getMaxStackSize(), slot.getMaxStackCount()) <= 0) return false;
    ItemStack existing = slot.getItem();
    if (existing == null) return true;
    return existing.isDamageAndIDIdentical(snapshot)
        && existing.count <= Math.min(snapshot.getMaxStackSize(), slot.getMaxStackCount());
  }

  private ItemStack fullStack(Slot slot) {
    ItemStack copy = snapshot.copy();
    copy.count = Math.min(snapshot.getMaxStackSize(), slot.getMaxStackCount());
    return copy;
  }

  public void finish(Minecraft mc, ContainerScreen screen, Container container) {
    Map<Slot, ItemStack> before = new LinkedHashMap<>(), after = new LinkedHashMap<>();
    boolean started = false;
    try {
      if (!valid(mc, screen, container)) return;
      // Prepare complete copies and recheck native slot rules before the first write.
      for (Slot slot : previews.keySet()) if (eligible(container, slot)) {
        before.put(slot, ItemStack.copy(slot.getItem()));
        after.put(slot, fullStack(slot));
      }
      if (after.isEmpty()) return;
      started = true;
      after.forEach(Slot::setStack);
      container.tick();
      if (mc.player.inventory.getCursorItem() != cursor
          || !ItemStack.areStacksIdentical(cursor, snapshot))
        throw new IllegalStateException("Creative drag changed its cursor stack");
    } catch (RuntimeException error) {
      failed = true;
      if (started) {
        for (var entry : before.entrySet()) try { entry.getKey().setStack(entry.getValue()); }
        catch (RuntimeException rollback) { error.addSuppressed(rollback); }
        try {
          if (mc.player.inventory.getCursorItem() != cursor
              || !ItemStack.areStacksIdentical(cursor, snapshot)) mc.player.inventory.setCursorItem(snapshot.copy());
          container.tick();
        } catch (RuntimeException rollback) { error.addSuppressed(rollback); }
      }
      CreativeInventory.LOGGER.error("Creative stack drag restored; disabled until restart", error);
    } finally { cancel(); }
  }

  public void cancel() {
    active = false;
    previews.clear();
    cursor = snapshot = null;
  }
}
