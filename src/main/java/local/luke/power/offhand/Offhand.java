package local.luke.power.offhand;

import local.luke.power.input.*;
import local.luke.power.inventory.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;

/** Typed native access. Offhand is never a PlayerInventory array index. */
public final class Offhand {
  public static final int RECOVERY_SLOT = 45;
  private Offhand() {}
  @SuppressWarnings("unchecked") private static OffhandInventory<ItemStack> bridge(PlayerInventory inventory) {
    return (OffhandInventory<ItemStack>)(Object)inventory;
  }
  public static ItemStack get(PlayerInventory inventory) { return bridge(inventory).power$offhandStack(); }
  public static void set(PlayerInventory inventory, ItemStack stack) {
    if (stack != null && (stack.count <= 0 || stack.count > 1_000_000))
      throw new IllegalArgumentException("Invalid offhand count");
    bridge(inventory).power$offhandStack(stack); inventory.markDirty();
  }
  public static boolean enabled(PlayerEntity player) {
    return player != null && player.world != null && !player.world.isRemote && OffhandConfig.current().enabled
        && local.luke.power.world.WorldCompatibility.ready(player.world);
  }
  public static boolean visible(PlayerEntity player) {
    return player!=null&&player.world!=null&&!player.world.isRemote&&(enabled(player)||get(player.inventory)!=null);
  }
  public static boolean playable(Minecraft mc) {
    return usable(mc) && !(mc.player instanceof PlayerMode mode && mode.power$spectator());
  }
  /** Manual slot swaps may operate in Spectator's owned inventory, never in the world. */
  public static boolean inventoryPlayable(Minecraft mc) {
    return usable(mc) && !(mc.player instanceof PlayerMode mode && !mode.power$inventoryEditable());
  }
  private static boolean usable(Minecraft mc) {
    return mc != null && enabled(mc.player) && mc.world == mc.player.world && mc.player.health > 0 && !mc.player.dead
        && mc.field_2807 == mc.player && !DetachedCamera.isActive() && !InteractionState.carryingContainer
        && mc.interactionManager != null;
  }
  public static ItemStack stack(PlayerEntity player, HandUse.Hand hand) {
    return hand == HandUse.Hand.OFF ? get(player.inventory) : player.inventory.main[player.inventory.selectedSlot];
  }
  public static void stack(PlayerEntity player, HandUse.Hand hand, ItemStack stack) {
    if (hand == HandUse.Hand.OFF) set(player.inventory, stack);
    else { player.inventory.main[player.inventory.selectedSlot] = stack; player.inventory.markDirty(); }
  }
  /** Explicit manual swap, with full references retained and rollback if a native callback fails. */
  public static boolean swap(Minecraft mc) {
    if (!playable(mc) || mc.currentScreen != null || mc.player.container != mc.player.playerContainer
        || mc.player.inventory.getCursorStack() != null) return false;
    var player = mc.player; var inventory = player.inventory; int slot = inventory.selectedSlot;
    ItemStack main = inventory.main[slot], off = get(inventory);
    if (main == off && main != null) throw new IllegalStateException("Shared hand stack");
    HandUse.swapping();
    if(NativeInventoryPlan.cancelPending(mc,"swap_hands"))return true;
    return NativeInventoryPlan.submit(mc,"swap_hands",true,java.util.List.of(
        NativeInventoryPlan.Change.main(inventory,slot,off),NativeInventoryPlan.Change.offhand(inventory,main)),
        ()->local.luke.power.offhand.OffhandInput.available(mc)&&mc.currentScreen==null,OffhandRenderer::changed);
  }
  public static boolean swapSlot(Minecraft mc,net.minecraft.screen.slot.Slot slot) {
    if(!OffhandInput.swapEligible(mc,slot))return false;
    var access=(local.luke.power.mixin.DeathSlotAccessor)slot;
    if(access.power$deathInventory() instanceof OffhandSlotInventory)return false;
    ItemStack source=slot.getStack(),off=get(mc.player.inventory);
    if(source!=null&&!slot.canInsert(source)||off!=null&&(!slot.canInsert(off)
        ||off.count>Math.min(off.getMaxCount(),slot.getMaxItemCount())))return false;
    HandUse.swapping();
    if(NativeInventoryPlan.cancelPending(mc,"swap_offhand_slot"))return true;
    return NativeInventoryPlan.submit(mc,"swap_offhand_slot",true,java.util.List.of(
        NativeInventoryPlan.Change.slot(slot,off),NativeInventoryPlan.Change.offhand(mc.player.inventory,source)),
        ()->OffhandInput.swapOwner(mc,slot),OffhandRenderer::changed);
  }
}
