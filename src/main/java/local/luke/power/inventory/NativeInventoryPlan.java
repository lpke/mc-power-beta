package local.luke.power.inventory;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import local.luke.power.PowerBeta;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.offhand.Offhand;
import local.luke.power.storage.*;
import local.luke.power.world.WorldCompatibility;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.opengl.Display;

/** Native data stays on the game thread; only a durable, still-owned plan can publish. */
public final class NativeInventoryPlan {
  private static Pending pending;
  private static final ArrayList<Pending> retired = new ArrayList<>();
  private static final Map<Path, AppendJournal> journals = new LinkedHashMap<>(8, .75f, true) {
    @Override protected boolean removeEldestEntry(Map.Entry<Path, AppendJournal> entry) { return size() > 8; }
  };
  private static boolean failed;
  private static Path journalPath;
  private enum Replacement { NONE, CREATIVE_HOTBAR, CREATIVE_EQUIPMENT }
  private NativeInventoryPlan() {}

  public record Change(String label, Object owner, int index, Supplier<ItemStack> get,
      Consumer<ItemStack> set, Predicate<ItemStack> accepts, IntSupplier limit,
      boolean wholeOversize, ItemStack next) {
    public static Change main(PlayerInventory inventory, int slot, ItemStack next) {
      if (slot < 0 || slot >= 36) throw new IllegalArgumentException("Invalid main inventory slot");
      return new Change("main." + slot, inventory, slot, () -> inventory.main[slot],
          stack -> { inventory.main[slot] = stack; inventory.markDirty(); }, stack -> true,
          inventory::getMaxCountPerStack, true, next);
    }
    public static Change offhand(PlayerInventory inventory, ItemStack next) {
      return new Change("offhand", inventory, Offhand.RECOVERY_SLOT, () -> Offhand.get(inventory),
          stack -> Offhand.set(inventory, stack), stack -> true, inventory::getMaxCountPerStack, true, next);
    }
    public static Change slot(Slot slot, ItemStack next) {
      var access = (local.luke.power.mixin.DeathSlotAccessor)(Object)slot;
      Object owner = access.power$deathInventory(); int index = access.power$deathIndex();
      if (owner instanceof local.luke.power.offhand.OffhandSlotInventory hand) { owner = hand.owner(); index = Offhand.RECOVERY_SLOT; }
      return new Change("slot." + slot.id, owner, index, slot::getStack,
          slot::setStack, stack -> stack == null ? slot.getStack() == null || slot.canInsert(slot.getStack()) : slot.canInsert(stack),
          slot::getMaxItemCount, false, next);
    }
  }

  public static boolean submit(Minecraft mc, String action, boolean manual, List<Change> changes,
      BooleanSupplier eligible, Runnable committed) {
    return submit(mc, action, manual, changes, eligible, committed, false);
  }

  /** Creative replacement is separate from every item-conserving survival operation. */
  public static boolean submit(Minecraft mc, String action, boolean manual, List<Change> changes,
      BooleanSupplier eligible, Runnable committed, boolean creativeReplacement) {
    return submit(mc, action, manual, changes, eligible, committed,
        creativeReplacement ? Replacement.CREATIVE_HOTBAR : Replacement.NONE, () -> {});
  }

  public static boolean submit(Minecraft mc, String action, boolean manual, List<Change> changes,
      BooleanSupplier eligible, Runnable committed, Runnable rollback) {
    return submit(mc, action, manual, changes, eligible, committed, Replacement.NONE, rollback);
  }

  /** Creative equipment may copy exactly one accepted armor piece, while returning old equipment intact. */
  public static boolean submitEquipment(Minecraft mc, String action, List<Change> changes,
      BooleanSupplier eligible, Runnable committed, boolean copiesEquipment) {
    return submit(mc, action, true, changes, eligible, committed,
        copiesEquipment ? Replacement.CREATIVE_EQUIPMENT : Replacement.NONE, () -> {});
  }

  private static boolean submit(Minecraft mc, String action, boolean manual, List<Change> changes,
      BooleanSupplier eligible, Runnable committed, Replacement replacement, Runnable rollback) {
    return submit(mc, action, manual, changes, eligible, committed, replacement, rollback, null);
  }

  /** One verified native recipe may exchange its exact ingredients for its result and containers. */
  public static boolean submitRecipe(Minecraft mc, List<Change> changes, BooleanSupplier eligible,
      Runnable committed, Runnable rollback, local.luke.power.recipes.RecipeCrafts.Exchange exchange) {
    if (exchange == null || !exchange.allowed(mc)) return false;
    return submit(mc, "recipe_craft", true, changes, eligible, committed, Replacement.NONE, rollback, exchange);
  }

  private static boolean submit(Minecraft mc, String action, boolean manual, List<Change> changes,
      BooleanSupplier eligible, Runnable committed, Replacement replacement, Runnable rollback,
      local.luke.power.recipes.RecipeCrafts.Exchange exchange) {
    if (failed || mc.player == null || mc.world == null || mc.world.isRemote
        || !WorldCompatibility.ready(mc.world) || !Display.isActive() || !eligible.getAsBoolean()) return false;
    if (replacement != Replacement.NONE && !creative(mc)) return false;
    if (pending != null) {
      if (!manual || retired.size() >= 4) return false;
      InventoryActions.cancel(); pending.cancelled = true;
      retired.add(pending); pending = null;
    }
    if (!action.equals("crafting_grid_transfer") && mc.currentScreen instanceof InventoryDrag drag) drag.power$cancelItemDrag();
    if (mc.currentScreen instanceof CreativePaint paint) paint.power$cancelCreativePaint();
    var lease = InventoryActions.begin(mc.world, mc.player, mc.player.container, mc.player.inventory, action, manual);
    if (lease == null) return false;
    try {
      var plan = new Pending(mc, lease, changes, eligible, committed, replacement, rollback, exchange);
      if (plan.changes.isEmpty()) { InventoryActions.finish(lease); return false; }
      String id = ((WorldIdentity)mc.world.method_262()).power$commandWorldId();
      journalPath = FabricLoader.getInstance().getGameDir().resolve("power-beta-data/inventory-recovery")
          .resolve(UUID.nameUUIDFromBytes((id + "\n" + mc.player.name).getBytes(StandardCharsets.UTF_8)) + ".journal");
      plan.durable = JournalQueue.append(journals.computeIfAbsent(journalPath, AppendJournal::new), plan.bytes(id, action));
      pending = plan; return true;
    } catch (RejectedExecutionException busy) {
      InventoryActions.finish(lease); return false;
    } catch (IOException | IllegalArgumentException rejected) {
      InventoryActions.finish(lease);
      PowerBeta.LOG.warn("Inventory plan declined before publication", rejected);
      if (mc.inGameHud != null) mc.inGameHud.addChatMessage("\u00a7cInventory action canceled: " + rejected.getMessage() + "\u00a7r");
      return false;
    } catch (Exception | LinkageError failure) {
      InventoryActions.finish(lease); fail(mc, failure); return false;
    }
  }

  public static boolean busy() { return pending != null; }
  public static boolean failed() { return failed; }
  public static boolean ownsAction(Minecraft mc, String action) {
    return pending != null && !pending.cancelled && pending.lease.action.equals(action) && pending.owns(mc);
  }
  /** A second swap press can cancel its unpublished inverse without touching any native slot. */
  public static boolean cancelPending(Minecraft mc, String action) {
    if (pending == null || pending.cancelled || !pending.lease.action.equals(action) || !pending.owns(mc)) return false;
    try {
      if (!pending.matchesBefore(mc)) return false;
      pending.cancelled = true; InventoryActions.cancel(); return true;
    } catch (IOException failure) { fail(mc, failure); return false; }
  }
  public static void cancel() {
    InventoryActions.cancel(); if (pending != null) pending.cancelled = true;
  }
  public static void tick(Minecraft mc) {
    for (Iterator<Pending> iterator = retired.iterator(); iterator.hasNext();) {
      Pending old = iterator.next();
      if (!old.durable.isDone()) continue;
      iterator.remove();
      try { old.durable.get(); }
      catch (Exception | LinkageError failure) { fail(mc, failure); }
      finally { InventoryActions.finish(old.lease); }
    }
    if (pending == null) return;
    Pending plan = pending;
    if (!plan.owns(mc)) plan.cancelled = true;
    if (!plan.durable.isDone()) return;
    pending = null;
    try {
      plan.durable.get();
      if (!failed && !plan.cancelled && plan.owns(mc) && plan.matchesBefore(mc)) plan.publish(mc);
    } catch (Exception | LinkageError failure) { fail(mc, failure); }
    finally { InventoryActions.finish(plan.lease); }
  }

  private static boolean creative(Minecraft mc) {
    return mc.player instanceof PlayerMode mode && mode.power$creativeInventory()
        && local.luke.power.permissions.CommandPermissions.allowed(local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE);
  }
  private static void fail(Minecraft mc, Throwable failure) {
    failed = true;
    PowerBeta.LOG.error("Inventory automation stopped; immutable recovery evidence: " + journalPath, failure);
    if (mc.inGameHud != null) mc.inGameHud.addChatMessage("\u00a7cInventory action stopped. Preserve power-beta-data/inventory-recovery for recovery.\u00a7r");
  }
  private static String encode(ItemStack item) throws IOException { return item == null ? null : DeathItems.encode(item); }
  private static ItemStack decode(String value) throws IOException { return value == null ? null : DeathItems.decode(value); }
  private static String identity(ItemStack item) throws IOException {
    if (item == null) return null;
    ItemStack copy = item.clone(); copy.count = 1; return DeathItems.encode(copy);
  }
  private static void total(Map<String, Long> totals, ItemStack item, int sign) throws IOException {
    if (item != null) totals.merge(identity(item), (long)sign * item.count, Long::sum);
  }

  private static final class Pending {
    final InventoryActions.Lease lease;
    final Object world, player, handler, screen;
    final PlayerInventory inventory;
    final ItemStack[] main, armor, references;
    final String inventoryBefore;
    final String[] protectedBefore, protectedAfter;
    final ItemStack[] protectedAfterReferences;
    final Map<net.minecraft.inventory.Inventory, OwnerSnapshot> external = new IdentityHashMap<>();
    final Set<String> oversizedBefore = new HashSet<>();
    final int selected;
    final List<Change> changes = new ArrayList<>();
    final List<ItemStack> originals = new ArrayList<>(), next = new ArrayList<>();
    final List<String> before = new ArrayList<>(), after = new ArrayList<>();
    final BooleanSupplier eligible;
    final Runnable committed, rollback;
    final Replacement replacement;
    Future<?> durable;
    boolean cancelled;
    int encodedSize;

    Pending(Minecraft mc, InventoryActions.Lease lease, List<Change> requested,
        BooleanSupplier eligible, Runnable committed, Replacement replacement, Runnable rollback,
        local.luke.power.recipes.RecipeCrafts.Exchange exchange) throws IOException {
      if (requested == null || requested.size() > 128) throw new IOException("Inventory plan exceeds its slot limit");
      this.lease = lease; world = mc.world; player = mc.player; handler = mc.player.container; screen = mc.currentScreen;
      inventory = mc.player.inventory; main = inventory.main; armor = inventory.armor; selected = inventory.selectedSlot;
      references = DeathItems.snapshot(inventory, mc.player.playerContainer);
      protectedBefore = new String[references.length];
      var unique = new IdentityHashMap<ItemStack, Boolean>();
      for (int i = 0; i < references.length; i++) {
        if (references[i] != null && unique.put(references[i], true) != null) throw new IOException("Shared protected inventory stack");
        protectedBefore[i] = encode(references[i]); account(protectedBefore[i]);
      }
      protectedAfter = protectedBefore.clone(); protectedAfterReferences = references.clone();
      inventoryBefore = new Gson().toJson(protectedBefore);
      this.eligible = eligible; this.committed = Objects.requireNonNull(committed); this.rollback = Objects.requireNonNull(rollback); this.replacement = replacement;
      var owners = new IdentityHashMap<Object, Set<Integer>>(); var totals = new HashMap<String, Long>();
      var logicalTargets = new HashSet<Integer>();
      for (Change change : requested) if (change != null && logical(change, mc) < 0
          && change.owner instanceof net.minecraft.inventory.Inventory owner && !external.containsKey(owner)) {
        var snapshot = new OwnerSnapshot(owner, unique); external.put(owner, snapshot);
      }
      for (Change change : requested) {
        if (change == null || change.owner == null || change.label == null || change.label.length() > 256
            || !owners.computeIfAbsent(change.owner, ignored -> new HashSet<>()).add(change.index))
          throw new IOException("Aliased or invalid inventory target");
        if (replacement == Replacement.CREATIVE_HOTBAR && (change.owner != inventory || change.index < 0 || change.index > 8))
          throw new IOException("Creative hotbar replacement cannot change other inventory regions");
        int logical = logical(change, mc);
        if (logical >= 0 && !logicalTargets.add(logical)) throw new IOException("Inventory adapters share a physical target");
        ItemStack original = change.get.get(); String saved = encode(original), planned = encode(change.next);
        if (logical >= 0 && original != references[logical]) throw new IOException("Inventory target has the wrong native owner");
        if (logical < 0 && !(change.owner instanceof net.minecraft.inventory.Inventory))
          throw new IOException("Unsupported native inventory owner");
        if (original != null && original.count > Math.min(original.getMaxCount(), change.limit.getAsInt())) oversizedBefore.add(saved);
        if (Objects.equals(saved, planned)) continue;
        account(saved); account(planned);
        changes.add(change); originals.add(original); before.add(saved); after.add(planned); next.add(decode(planned));
        if (logical >= 0) { protectedAfter[logical] = planned; protectedAfterReferences[logical] = next.get(next.size() - 1); }
        else external.get((net.minecraft.inventory.Inventory)change.owner).stage(change.index, next.get(next.size() - 1), planned);
        total(totals, original, 1); total(totals, change.next, -1);
      }
      if (replacement == Replacement.CREATIVE_EQUIPMENT) {
        Change armor = null;
        for (Change change : changes) {
          int slot = logical(change, mc);
          if (slot >= 36 && slot < 40) {
            if (armor != null || change.next == null || change.next.count != 1)
              throw new IOException("Creative equipment must change exactly one armor slot");
            armor = change;
          } else if (!(change.owner == inventory && (slot >= 0 && slot < 36 || slot == 45)))
            throw new IOException("Creative equipment cannot change other inventory regions");
        }
        if (armor == null) throw new IOException("Creative equipment has no armor target");
        totals.merge(identity(armor.next), 1L, Long::sum);
      }
      if (exchange != null) {
        if (!exchange.allowed(mc)) throw new IOException("The native crafting recipe changed");
        for (var item : exchange.consumed()) total(totals, local.luke.power.recipes.RecipeNative.nativeStack(item), -1);
        for (var item : exchange.produced()) total(totals, local.luke.power.recipes.RecipeNative.nativeStack(item), 1);
      }
      if (replacement != Replacement.CREATIVE_HOTBAR && totals.values().stream().anyMatch(value -> value != 0))
        throw new IOException("Inventory plan does not conserve native items");
      for (int i = 0; i < changes.size(); i++) {
        Change change = changes.get(i); ItemStack item = next.get(i);
        if (!admitted(i)) throw new IOException("Inventory target rejects the planned item or its count");
      }
    }

    void account(String encoded) throws IOException {
      encodedSize += encoded == null ? 0 : encoded.length();
      if (encodedSize > AppendJournal.MAX_RECORD / 2) throw new IOException("Inventory recovery data exceeds the bounded snapshot size");
    }
    int logical(Change change, Minecraft mc) {
      if (change.owner == inventory && (change.index >= 0 && change.index < 40 || change.index == Offhand.RECOVERY_SLOT)) return change.index;
      for (int i = 1; i <= 4; i++) {
        var slot = (local.luke.power.mixin.DeathSlotAccessor)(Object)mc.player.playerContainer.method_2084(i);
        if (slot.power$deathInventory() == change.owner && slot.power$deathIndex() == change.index) return 40 + i;
      }
      return -1;
    }
    boolean admitted(int i) {
      Change change = changes.get(i); ItemStack item = next.get(i);
      if (!change.accepts.test(item)) return false;
      if (item == null || item.count <= Math.min(item.getMaxCount(), change.limit.getAsInt())) return true;
      ItemStack original = originals.get(i);
      try {
        if (original != null && item.count <= original.count && identity(item).equals(identity(original))) return true;
      } catch (IOException invalid) { return false; }
      return change.wholeOversize && (replacement == Replacement.CREATIVE_HOTBAR || oversizedBefore.contains(after.get(i)));

    }

    boolean owns(Minecraft mc) {
      return mc.world == world && mc.player == player && mc.currentScreen == screen && Display.isActive()
          && mc.player != null && lease.valid(mc.world, mc.player, mc.player.container, mc.player.inventory)
          && mc.player.inventory == inventory && inventory.main == main && inventory.armor == armor
          && inventory.selectedSlot == selected && mc.player.health > 0 && !mc.player.dead
          && WorldCompatibility.ready(mc.world) && eligible.getAsBoolean() && (replacement == Replacement.NONE || creative(mc));
    }
    boolean matchesBefore(Minecraft mc) throws IOException {
      ItemStack[] current = DeathItems.snapshot(inventory, mc.player.playerContainer);
      if (current.length != references.length) return false;
      for (int i = 0; i < current.length; i++) if (current[i] != references[i]) return false;
      for (int i = 0; i < current.length; i++) if (!Objects.equals(protectedBefore[i], encode(current[i]))) return false;
      for (OwnerSnapshot owner : external.values()) if (!owner.matches(false)) return false;
      for (int i = 0; i < changes.size(); i++)
        if (!admitted(i) || changes.get(i).get.get() != originals.get(i) || !Objects.equals(encode(changes.get(i).get.get()), before.get(i))) return false;
      return true;
    }
    byte[] bytes(String worldId, String action) throws IOException {
      JsonObject record = new JsonObject(); record.addProperty("format", 1); record.addProperty("phase", "prepared");
      record.addProperty("transaction", UUID.randomUUID().toString()); record.addProperty("world", worldId);
      record.addProperty("action", action); record.addProperty("time", System.currentTimeMillis());
      record.addProperty("player", ((net.minecraft.entity.player.PlayerEntity)player).name);
      record.addProperty("handler", handler.getClass().getName());
      var nativePlayer = (net.minecraft.entity.player.PlayerEntity)player;
      record.addProperty("x", nativePlayer.x); record.addProperty("y", nativePlayer.y); record.addProperty("z", nativePlayer.z);
      record.addProperty("selected", selected); record.addProperty("playerBefore", inventoryBefore);
      JsonArray slots = new JsonArray();
      for (int i = 0; i < changes.size(); i++) {
        JsonObject slot = new JsonObject(); slot.addProperty("target", changes.get(i).label);
        slot.addProperty("before", before.get(i)); slot.addProperty("after", after.get(i)); slots.add(slot);
      }
      record.add("slots", slots); byte[] bytes = record.toString().getBytes(StandardCharsets.UTF_8);
      JsonArray owners = new JsonArray();
      for (OwnerSnapshot owner : external.values()) {
        JsonObject saved = new JsonObject(); saved.addProperty("type", owner.owner.getClass().getName());
        saved.addProperty("name", owner.owner.getName());
        saved.add("before", new Gson().toJsonTree(owner.before)); saved.add("after", new Gson().toJsonTree(owner.after));
        owners.add(saved);
      }
      record.add("external", owners); bytes = record.toString().getBytes(StandardCharsets.UTF_8);
      if (bytes.length > AppendJournal.MAX_RECORD) throw new IOException("Inventory recovery snapshot exceeds 1 MiB");
      return bytes;
    }
    void publish(Minecraft mc) throws IOException {
      int written = 0;
      try {
        for (int i = 0; i < changes.size(); i++) {
          Change change = changes.get(i);
          if (!owns(mc) || !admitted(i) || change.get.get() != originals.get(i) || !Objects.equals(encode(change.get.get()), before.get(i)))
            throw new IOException("Inventory changed during publication");
          written = i + 1; change.set.accept(next.get(i));
        }
        verifyPublished(mc);
        committed.run();
        verifyPublished(mc);
      } catch (Exception | LinkageError failure) {
        for (int i = written - 1; i >= 0; i--) {
          Change change = changes.get(i);
          try {
            if (mc.world == world && mc.player == player && inventory.main == main && inventory.armor == armor
                && change.get.get() == next.get(i) && Objects.equals(encode(change.get.get()), after.get(i)))
              restore(change, decode(before.get(i)));
          } catch (Exception | LinkageError rollback) { failure.addSuppressed(rollback); }
        }
        try {
          // Derived recipe output may be restored only after every owned input was restored.
          // A callback that replaced an input or changed its owner retains its recovery evidence.
          if (restoredBefore(mc)) rollback.run();
          else failure.addSuppressed(new IOException("Inventory rollback lost ownership; derived output was not restored"));
        } catch (Exception | LinkageError rollbackFailure) { failure.addSuppressed(rollbackFailure); }
        throw new IOException("Inventory publication failed; original recovery evidence retained", failure);
      }
    }

    private boolean restoredBefore(Minecraft mc) throws IOException {
      if (mc.world != world || mc.player != player || mc.currentScreen != screen
          || mc.player == null || mc.player.container != handler || mc.player.inventory != inventory
          || inventory.main != main || inventory.armor != armor || inventory.selectedSlot != selected
          || !lease.valid(mc.world, mc.player, mc.player.container, inventory)) return false;
      for (int i = 0; i < changes.size(); i++)
        if (!Objects.equals(encode(changes.get(i).get.get()), before.get(i))) return false;
      ItemStack[] complete = DeathItems.snapshot(inventory, mc.player.playerContainer);
      if (complete.length != protectedBefore.length) return false;
      for (int i = 0; i < complete.length; i++)
        if (!Objects.equals(encode(complete[i]), protectedBefore[i])) return false;
      for (OwnerSnapshot owner : external.values()) {
        if (owner.owner.size() != owner.before.length) return false;
        for (int i = 0; i < owner.before.length; i++)
          if (!Objects.equals(encode(owner.owner.getStack(i)), owner.before[i])) return false;
      }
      return true;
    }

    private void verifyPublished(Minecraft mc) throws IOException {
      if (!owns(mc)) throw new IOException("Inventory owner changed during publication");
        for (int i = 0; i < changes.size(); i++)
          if (changes.get(i).get.get() != next.get(i) || !Objects.equals(encode(changes.get(i).get.get()), after.get(i)))
            throw new IOException("Native callback changed an inventory result");
        ItemStack[] complete = DeathItems.snapshot(inventory, mc.player.playerContainer);
        if (complete.length != protectedAfter.length) throw new IOException("Protected inventory layout changed");
        for (int i = 0; i < complete.length; i++)
          if (complete[i] != protectedAfterReferences[i] || !Objects.equals(encode(complete[i]), protectedAfter[i]))
            throw new IOException("Native callback changed an unrelated protected slot");
        for (OwnerSnapshot owner : external.values()) if (!owner.matches(true))
          throw new IOException("Native callback changed another container slot");
    }

    private void restore(Change change, ItemStack stack) {
      if (change.owner instanceof net.minecraft.class_159 crafting) {
        ItemStack[] raw = ((local.luke.power.mixin.DeathCraftingAccessor)(Object)crafting).power$deathStacks();
        if (change.index < 0 || change.index >= raw.length) throw new IllegalStateException("Crafting layout changed");
        raw[change.index] = stack;
      } else if (change.owner.getClass() == net.minecraft.class_138.class) {
        ItemStack[] raw = ((local.luke.power.recipes.mixin.RecipeFurnaceAccessor)change.owner).power$recipeStacks();
        if (change.index < 0 || change.index >= raw.length) throw new IllegalStateException("Furnace layout changed");
        raw[change.index] = stack;
      } else change.set.accept(stack);
    }

    private final class OwnerSnapshot {
      final net.minecraft.inventory.Inventory owner;
      final ItemStack[] beforeReferences, afterReferences;
      final String[] before, after;
      OwnerSnapshot(net.minecraft.inventory.Inventory owner, IdentityHashMap<ItemStack, Boolean> unique) throws IOException {
        int size = owner.size();
        if (size < 0 || size > 128) throw new IOException("Unsupported native inventory size");
        this.owner = owner; beforeReferences = new ItemStack[size]; before = new String[size];
        for (int i = 0; i < size; i++) {
          ItemStack item = owner.getStack(i);
          if (item != null && unique.put(item, true) != null) throw new IOException("Container slots share a native stack");
          beforeReferences[i] = item; before[i] = encode(item); account(before[i]);
        }
        after = before.clone(); afterReferences = beforeReferences.clone();
      }
      void stage(int index, ItemStack next, String bytes) throws IOException {
        if (index < 0 || index >= after.length) throw new IOException("Invalid container slot index");
        after[index] = bytes; afterReferences[index] = next;
      }
      boolean matches(boolean published) throws IOException {
        if (owner.size() != before.length) return false;
        for (int i = 0; i < before.length; i++) {
          ItemStack item = owner.getStack(i);
          if (item != (published ? afterReferences[i] : beforeReferences[i])
              || !Objects.equals(encode(item), published ? after[i] : before[i])) return false;
        }
        return true;
      }
    }
  }
}
