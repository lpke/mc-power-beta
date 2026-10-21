package local.luke.power.inventory.tools;

import java.io.IOException;
import java.util.*;
import local.luke.power.PowerBeta;
import local.luke.power.death.DeathItems;
import local.luke.power.inventory.*;
import local.luke.power.offhand.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/** Only a native item-use or successful block break can request a refill. */
public final class Restock {
  private static final EnumMap<HandUse.Hand, Action> ready = new EnumMap<>(HandUse.Hand.class);
  private static Action publishing;
  static { InventoryRefills.waiting((inventory, hand, original) -> {
    Action action = ready.get(hand);
    return action != null && action.inventory == inventory && action.original == original
        || publishing != null && publishing.inventory == inventory && publishing.hand == hand
            && publishing.original == original && NativeInventoryPlan.busy();
  }); }
  private Restock() {}
  public record Action(Object world, Object player, Object inventory, HandUse.Hand hand, int selected,
      ItemStack original, int count, String identity, boolean tool, long hands, long press) {}
  public static Action before(Minecraft mc, ItemStack stack) {
    return before(mc, stack, mc.player == null ? HandUse.Hand.MAIN : HandUse.hand(mc.player.inventory));
  }
  public static Action before(Minecraft mc, ItemStack stack, HandUse.Hand hand) {
    if (!InventoryToolsConfig.current().restock || !InventoryAutomation.survival(mc)
        || stack == null || stack.count <= 0) return null;
    if (Offhand.stack(mc.player, hand) != stack) return null;
    try {
      return new Action(mc.world, mc.player, mc.player.inventory, hand, mc.player.inventory.selectedSlot,
          stack, stack.count, identity(stack), stack.isDamageable(), HandUse.manualRevision(), OffhandUse.pressRevision());
    } catch (IOException invalid) { PowerBeta.LOG.warn("Could not prepare an item refill", invalid); return null; }
  }
  public static void after(Minecraft mc, Action action) {
    if (action == null || mc.player != action.player || mc.world != action.world
        || action.original.count >= action.count) return;
    var settings = InventoryToolsConfig.current();
    // Pause repeated placement only when this consumption actually needs a refill.
    // Higher untouched counts must not turn fast placement into one placement per tick.
    if (action.original.count > 0 && (action.tool || !settings.restockBeforeEmpty
        || action.original.count > settings.restockThreshold) || action.tool && !settings.restockTools) return;
    ItemStack held = Offhand.stack(mc.player, action.hand);
    if (held != null && held != action.original) return;
    ready.put(action.hand, action);
  }
  /** Held-use repeats wait for their own durable refill; a fresh press supersedes it. */
  public static boolean waitingForUse(Minecraft mc) {
    if (!InventoryToolsConfig.current().restock) return false;
    if (publishing != null && NativeInventoryPlan.ownsAction(mc, "restock") && sameUse(mc, publishing)) return true;
    for (Action action : ready.values()) if (sameUse(mc, action)) return true;
    return false;
  }
  private static boolean sameUse(Minecraft mc, Action action) {
    return action != null
        && action.world == mc.world && action.player == mc.player && mc.player != null
        && action.inventory == mc.player.inventory && action.selected == mc.player.inventory.selectedSlot
        && action.hands == HandUse.manualRevision() && action.press == OffhandUse.pressRevision()
        && InventoryAutomation.survival(mc);
  }
  public static void clear() { ready.clear(); publishing = null; }
  public static void tick(Minecraft mc) {
    if (ready.isEmpty()) return;
    if (!InventoryToolsConfig.current().restock || !InventoryAutomation.survival(mc)) { clear(); return; }
    if (NativeInventoryPlan.busy() || InventoryActions.busy()) return;
    var iterator = ready.values().iterator();
    if (!iterator.hasNext()) return;
    Action action = iterator.next(); iterator.remove();
    if (action.world != mc.world || action.player != mc.player || action.inventory != mc.player.inventory
        || action.selected != mc.player.inventory.selectedSlot || action.hands != HandUse.manualRevision()
        || action.hand == HandUse.Hand.OFF && !Offhand.enabled(mc.player)) return;
    try { prepare(mc, action); }
    catch (IOException invalid) { PowerBeta.LOG.warn("Could not validate an item refill", invalid); }
  }
  private static void prepare(Minecraft mc, Action action) throws IOException {
    var settings = InventoryToolsConfig.current();
    ItemStack held = Offhand.stack(mc.player, action.hand);
    // Native container replacements such as an empty bucket are successful outcomes, not depletion.
    if (held != null && held != action.original) return;
    if (held != null && held.count > 0 && (!identity(held).equals(action.identity) || action.tool
        || !settings.restockBeforeEmpty || held.count > settings.restockThreshold)) return;
    if (action.tool && !settings.restockTools) return;
    var inventory = mc.player.inventory;
    int source = -1;
    for (int i = settings.restockOtherHotbar ? 0 : 9; i < 36; i++) {
      ItemStack candidate = inventory.main[i];
      if (i == action.selected || candidate == null || candidate.count <= 0 || !identity(candidate).equals(action.identity)) continue;
      if (source < 0 || action.tool && candidate.getDamage() > inventory.main[source].getDamage()) source = i;
      if (!action.tool) break;
    }
    if (source < 0) return;
    ItemStack from = inventory.main[source], next = clone(from), remainder = null;
    int currentCount = held == null ? 0 : Math.max(0, held.count);
    if (currentCount > 0) {
      int amount = Math.min(from.count, Math.max(0, Math.min(from.getMaxCount(), inventory.getMaxCountPerStack()) - currentCount));
      if (amount == 0) return;
      next = clone(held); next.count += amount;
      if (amount < from.count) { remainder = clone(from); remainder.count -= amount; }
    }
    // A zero-count native source is normalized by its caller before this tick.
    if (held != null && held.count <= 0) return;
    var changes = List.of(NativeInventoryPlan.Change.main(inventory, source, remainder),
        action.hand == HandUse.Hand.OFF ? NativeInventoryPlan.Change.offhand(inventory, next)
            : NativeInventoryPlan.Change.main(inventory, action.selected, next));
    if (NativeInventoryPlan.submit(mc, "restock", false, changes,
        () -> InventoryAutomation.survival(mc) && InventoryToolsConfig.current().restock
            && action.hands == HandUse.manualRevision()
            && (action.hand != HandUse.Hand.OFF || Offhand.enabled(mc.player)),
        () -> {
          InventoryRefills.replaced(inventory, action.hand, action.original, Offhand.stack(mc.player, action.hand));
          publishing = null;
          if (action.hand == HandUse.Hand.OFF) OffhandRenderer.changed();
        })) publishing = action;
  }
  private static ItemStack clone(ItemStack item) throws IOException { return DeathItems.decode(DeathItems.encode(item)); }
  private static String identity(ItemStack item) throws IOException {
    ItemStack copy = clone(item); copy.count = 1;
    if (copy.isDamageable()) copy.setDamage(0);
    return DeathItems.encode(copy);
  }
}
