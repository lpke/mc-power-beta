package local.luke.power.offhand;

import local.luke.power.mixin.DeathSlotAccessor;
import net.minecraft.*;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Container;
import net.minecraft.screen.slot.Slot;

/** Native transport slots stay independent of offhand's stable recovery index. */
public final class OffhandContainers {
  private OffhandContainers() {}

  public static boolean supported(Container container) {
    if (container == null) return false;
    Class<?> type = container.getClass();
    return type == class_277.class || type == class_506.class || type == class_407.class
        || type == class_196.class || type == class_337.class;
  }

  public static boolean supported(ContainerScreen screen) {
    if (screen == null || !supported(screen.container)) return false;
    Class<?> type = screen.getClass(), handler = screen.container.getClass();
    return type == class_585.class && handler == class_277.class
        || type == class_264.class && handler == class_506.class
        || type == class_235.class && handler == class_407.class
        || type == class_516.class && handler == class_196.class
        || type == class_524.class && handler == class_337.class;
  }

  public static OffhandNativeSlot slot(Container container, PlayerInventory owner) {
    if (!supported(container) || owner == null || container.slots.isEmpty()) return null;
    Object last = container.slots.get(container.slots.size() - 1);
    return last instanceof OffhandNativeSlot hand && hand.owner() == owner
        && hand.id == container.slots.size() - 1 ? hand : null;
  }

  public static int hotbarY(Container container, PlayerInventory owner) {
    for (Object entry : container.slots) if (entry instanceof Slot nativeSlot) {
      var access = (DeathSlotAccessor)(Object)nativeSlot;
      if (access.power$deathInventory() == owner && access.power$deathIndex() == 0) return nativeSlot.y;
    }
    throw new IllegalArgumentException("The native hotbar is missing");
  }

  /** Chest draws 17 + rows * 18 + 96 pixels, one less than its declared height. */
  public static int frameHeight(ContainerScreen screen, int height) {
    return screen.getClass() == class_264.class ? height - 1 : height;
  }

  public static PlayerInventory owner(Container container) {
    if (!supported(container)) return null;
    PlayerInventory owner = null;
    boolean[] indices = new boolean[36];
    for (Object entry : container.slots) if (entry instanceof Slot nativeSlot) {
      var access = (DeathSlotAccessor)(Object)nativeSlot;
      if (!(access.power$deathInventory() instanceof PlayerInventory inventory)) continue;
      int index = access.power$deathIndex();
      if (index < 0 || index >= 36) continue;
      if (nativeSlot.getClass() != Slot.class || indices[index] || owner != null && owner != inventory) return null;
      owner = inventory; indices[index] = true;
    }
    for (boolean present : indices) if (!present) return null;
    return owner;
  }
}
