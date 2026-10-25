package local.luke.power.offhand;

import java.io.IOException;
import local.luke.power.death.DeathItems;
import local.luke.power.inventory.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Observe native main-hand behavior, then offer the same native callbacks to the offhand. */
public final class OffhandUse implements AutoCloseable {
  private static OffhandUse current;
  private static Object vehiclePlayer, vehicleWorld;
  private static boolean vehicleUsed;
  private static long pressRevision;
  private final Minecraft mc;
  private final Object world, player, inventory, handler;
  private final InventoryActions.Lease lease;
  private final Thread thread = Thread.currentThread();
  private boolean consumed, effect, callback, nativePath;
  private HandUse.Hand hand = HandUse.Hand.MAIN;
  private final int selection;
  private OffhandUse(Minecraft mc) {
    this.mc = mc; world = mc.world; player = mc.player; inventory = mc.player.inventory;
    handler = mc.player.container; selection = mc.player.inventory.selectedSlot;
    lease = InventoryActions.begin(world, player, handler, inventory, "use_hands", true);
    current = this;
  }
  public static OffhandUse begin(Minecraft mc, int button) {
    return button == 1 && current == null && Offhand.playable(mc) && mc.currentScreen == null ? new OffhandUse(mc) : null;
  }
  public static boolean active() { return current != null; }
  public static void newPress(Minecraft mc) { vehiclePlayer=mc.player;vehicleWorld=mc.world;vehicleUsed=false;pressRevision++; }
  public static long pressRevision() { return pressRevision; }
  public static boolean heldVehicle(Minecraft mc) {
    return vehicleUsed&&vehiclePlayer==mc.player&&vehicleWorld==mc.world&&Offhand.playable(mc)
        &&mc.currentScreen==null&&mc.field_2778;
  }
  public static void nativePath() { if (current != null) current.nativePath = true; }
  public static boolean offhand() { return current != null && current.hand == HandUse.Hand.OFF; }
  public static boolean suppressAir() { return current != null && current.consumed; }
  public static void effect(World world) {
    if (current != null && current.thread == Thread.currentThread() && current.callback && current.world == world) current.effect = true;
  }
  public static void entityResult(boolean handled) { if (current != null && handled) current.consumed = true; }
  public static boolean skipBlock(PlayerEntity player,net.minecraft.block.Block block) {
    if (current == null) return false;
    if (current.hand == HandUse.Hand.OFF) {
      // Beta TNT ignition lives in a block callback and needs the effective hand.
      ItemStack held = Offhand.stack(player, current.hand);
      return block.id!=46||held == null || !(held.getItem() instanceof class_539);
    }
    return player.method_1334() && (Offhand.get(player.inventory) != null || player.inventory.getSelectedItem() != null);
  }
  private boolean valid() {
    return mc.player != null && lease != null && lease.valid(mc.world, mc.player, mc.player.container, mc.player.inventory)
        && Offhand.playable(mc) && mc.currentScreen == null && mc.player.inventory.selectedSlot == selection
        && mc.player.inventory.getCursorStack() == null;
  }
  private boolean ownsNative() {
    return mc.world==world&&mc.player==player&&mc.player!=null&&mc.player.inventory==inventory&&mc.player.world==world;
  }
  private static String data(ItemStack stack) {
    // Native block use leaves a depleted stack in its hand until Minecraft's caller clears it.
    try { return stack == null || stack.count == 0 ? "" : DeathItems.encode(stack); }
    catch (IOException failure) { throw new IllegalStateException("Unsafe hand item", failure); }
  }
  /** Unknown extensions can have effects without changing their stack. Stop conservatively. */
  private static boolean nativeType(Object object) { return object.getClass().getName().startsWith("net.minecraft."); }
  private static boolean nativeBlock(Object block) {
    // The bundled spawner subclass adds an item factory, with no use override.
    return nativeType(block) || block.getClass().getName().equals(
        "local.luke.power.commands.optionaldep.stapi.block.SpecialMobSpawnerBlock");
  }
  public static void blockResult(boolean result, ItemStack stack) {
    if (current != null && (result || stack != null && !nativeType(stack.getItem()))) current.consumed = true;
    if(current!=null&&result&&stack!=null&&stack.getItem() instanceof class_381) current.vehicle();
  }
  public static boolean block(Minecraft mc,ItemStack stack,int x,int y,int z,java.util.function.BooleanSupplier nativeUse) {
    OffhandUse use=current;if(use==null)return nativeUse.getAsBoolean();
    int id=mc.world.getBlockId(x,y,z);Object block=id>0?net.minecraft.block.Block.BLOCKS[id]:null;
    String before=data(stack);use.effect=false;use.callback=true;
    boolean result;
    try { result=nativeUse.getAsBoolean(); } finally { use.callback=false; }
    if(!use.ownsNative()){use.consumed=true;return result;}
    blockResult(result,stack);
    use.consumed|=use.effect||!before.equals(data(Offhand.stack(mc.player,use.hand)))||block!=null&&!nativeBlock(block);
    // Modern block items report a terminal placement failure rather than passing to the other hand.
    if(!result&&stack!=null&&stack.getItem() instanceof class_533)use.consumed=true;
    return result;
  }
  private void vehicle() { vehiclePlayer=mc.player;vehicleWorld=mc.world;vehicleUsed=true; }
  public static boolean air(Minecraft mc, ItemStack stack, java.util.function.BooleanSupplier nativeUse) {
    OffhandUse use = current;
    if (use == null) return nativeUse.getAsBoolean();
    if (use.consumed) return false;
    Boolean equipment=OffhandEquipment.use(mc,use.hand,stack);
    if(equipment!=null){use.consumed|=equipment;return equipment;}
    String before = data(stack); use.effect = false; use.callback = true;
    boolean result;
    try { result = nativeUse.getAsBoolean(); }
    finally { use.callback = false; }
    if(!use.ownsNative()){use.consumed=true;return result;}
    ItemStack after = Offhand.stack(mc.player, use.hand);
    use.consumed |= result || use.effect || !before.equals(data(after)) || !nativeType(stack.getItem());
    // Casting/reeling is successful even when creative durability/count protection hides its change.
    if (stack.getItem() instanceof class_607) use.consumed = true;
    if(use.consumed&&(stack.getItem() instanceof class_483||stack.getItem() instanceof class_381))use.vehicle();
    return result;
  }
  public static void entity(Minecraft mc, Entity target, Runnable nativeUse) {
    OffhandUse use = current;
    if (use == null) { nativeUse.run(); return; }
    ItemStack beforeStack = Offhand.stack(mc.player, use.hand); String before = data(beforeStack);
    boolean sheared = target instanceof class_613 sheep && sheep.method_2048();
    use.effect = false; use.callback = true;
    try { nativeUse.run(); } finally { use.callback = false; }
    if(!use.ownsNative()){use.consumed=true;return;}
    use.consumed |= use.effect || !before.equals(data(Offhand.stack(mc.player, use.hand)))
        || target instanceof class_613 sheep && sheep.method_2048() != sheared || !nativeType(target)
        || beforeStack != null && !nativeType(beforeStack.getItem()) || mc.currentScreen != null;
  }
  /** Called after the complete native MAIN path, including its item fallback. */
  public void offerOffhand() {
    if (!nativePath || consumed || !valid()) return;
    ItemStack stack = Offhand.get(mc.player.inventory);
    if (stack == null || !nativeType(stack.getItem())) return;
    hand = HandUse.Hand.OFF; consumed = false;
    var target = mc.field_2823;
    if (target != null && target.field_1983 == class_212.ENTITY) {
      Entity entity = target.field_1989;
      if(!nativeType(entity))return;
      try (HandUse ignored = HandUse.begin(mc.player.inventory, hand, nativeType(entity))) {
        entity(mc, entity, () -> mc.interactionManager.interactEntity(mc.player, entity));
      }
    } else if (target != null && target.field_1983 == class_212.TILE) {
      boolean result;
      try (HandUse ignored = HandUse.begin(mc.player.inventory, hand, false)) {
        ItemStack held=stack;
        result = block(mc,held,target.field_1984,target.field_1985,target.field_1986,
            ()->mc.interactionManager.method_1713(mc.player, mc.world, held,
                target.field_1984, target.field_1985, target.field_1986, target.field_1987));
      }
      if(!ownsNative())return;
      blockResult(result, stack);
      if (result) swing(mc.player);
      if (stack.count <= 0 && Offhand.get(mc.player.inventory) == stack) Offhand.set(mc.player.inventory, null);
    }
    if (!consumed && valid() && (stack = Offhand.get(mc.player.inventory)) != null) {
      ItemStack held = stack;
      air(mc, held, () -> {
        var refill = local.luke.power.inventory.tools.Restock.before(mc, held, HandUse.Hand.OFF);
        try {
        int count = held.count; ItemStack result;
        try (HandUse ignored = HandUse.begin(mc.player.inventory, hand, false)) {
          result = held.method_698(mc.world, mc.player);
        }
        if(!ownsNative()||Offhand.get(mc.player.inventory)!=held)return true;
        if (result != held || result != null && result.count != count) {
          Offhand.set(mc.player.inventory, result == null || result.count <= 0 ? null : result);
          return true;
        }
        if (held.count <= 0) Offhand.set(mc.player.inventory, null);
        return false;
        } finally { local.luke.power.inventory.tools.Restock.after(mc, refill); }
      });
    }
    if (consumed) OffhandRenderer.changed();
  }
  public static void swing(PlayerEntity player) { OffhandRenderer.swing(player); }
  @Override public void close() {
    if (current != this) throw new IllegalStateException("Hand use ownership changed");
    current = null; InventoryActions.finish(lease);
  }
}
