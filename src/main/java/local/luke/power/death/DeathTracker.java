package local.luke.power.death;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.PowerBeta;
import local.luke.power.commands.CommandContext;
import local.luke.power.input.*;
import local.luke.power.permissions.CommandPermissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public final class DeathTracker {
  private static Path journalPath;
  private static DeathJournal journal;
  private static boolean recoveryFailed;
  private DeathTracker() {}

  public static boolean available(Minecraft mc) {
    return DeathConfig.current().enabled && mc.world != null && !mc.world.isRemote && mc.player != null;
  }
  public static DeathJournal journal(Minecraft mc) throws IOException {
    String identity = CommandContext.world(mc) + "\n" + mc.player.name;
    Path path = FabricLoader.getInstance().getGameDir().resolve("power-beta-data/deaths")
        .resolve(UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)) + ".journal");
    if (!path.equals(journalPath)) { journal = new DeathJournal(path); journalPath = path; }
    return journal;
  }

  public static void died(PlayerEntity player, Entity attacker) {
    Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
    if (!available(mc) || player != mc.player || player.world != mc.world) return;
    try {
      DeathJournal data = journal(mc);
      DeathRecord record = new DeathRecord(); record.id = data.nextId();
      record.world = CommandContext.world(mc); record.player = player.name;
      record.x = (int)Math.floor(player.x); record.y = (int)Math.floor(player.boundingBox.minY); record.z = (int)Math.floor(player.z);
      record.dimension = player.dimensionId; record.date = System.currentTimeMillis();
      record.cause = DeathCause.describe(player, attacker); DeathItems.capture(player, record);
      record.kept = KeepInventory.kept(player);
      data.death(record);
      if (DeathConfig.current().announceLocation) DeathCommands.location(mc, record);
      if (DeathConfig.current().announceItems) DeathCommands.items(mc, record, false);
    } catch (Exception e) {
      PowerBeta.LOG.error("Could not protect death snapshot; original journal retained", e);
      mc.inGameHud.addChatMessage("§cDeath record could not be saved. Check the game log.§r");
    }
  }

  public static String recoveryDenial(Minecraft mc) {
    if (!available(mc)) return "Death records are disabled or no local world is open.";
    if (!CommandPermissions.allowed(local.luke.power.rules.GameRule.ALLOW_DEATH_RECOVERY)) return "Enable Allow death recovery in Options > Game rules.";
    if (recoveryFailed) return "Death recovery is disabled after a protection failure. Restart and keep the recovery journal.";
    if (mc.player.dead || mc.player.health <= 0 || mc.player.world != mc.world || mc.field_2807 != mc.player
        || DetachedCamera.isActive() || InteractionState.carryingContainer || mc.player.container != mc.player.playerContainer
        || mc.player.inventory.getCursorStack() != null)
      return "Respawn, close containers and exit freecam before recovering items.";
    return "";
  }

  public static void recover(Minecraft mc, DeathRecord record) throws IOException {
    recover(mc, record, null);
  }

  public static void recover(Minecraft mc, DeathRecord record, int index) throws IOException {
    recover(mc, record, List.of(index));
  }

  private static void recover(Minecraft mc, DeathRecord record, List<Integer> indices) throws IOException {
    String denial = recoveryDenial(mc);
    if (!denial.isEmpty()) throw new IllegalArgumentException(denial);
    if (!record.world.equals(CommandContext.world(mc)) || !record.player.equals(mc.player.name))
      throw new IllegalArgumentException("This death belongs to another world or player.");
    DeathJournal data = journal(mc);
    if (!Set.of("available", "partial").contains(data.state(record.id)))
      throw new IllegalArgumentException("Death " + record.id + " was already recovered or has an interrupted recovery. Items were not changed.");
    if (indices == null) indices = data.remaining(record.id);
    if (!data.remaining(record.id).containsAll(indices) || new HashSet<>(indices).size() != indices.size())
      throw new IllegalArgumentException("These items have already been recovered.");
    var player = mc.player; var world = mc.world; var inventory = player.inventory;
    var container = player.playerContainer;
    var before = DeathItems.snapshot(inventory, container);
    String beforeData = DeathItems.inventory(before);
    var after = DeathItems.plan(inventory, player.playerContainer, record, indices);
    String afterData = DeathItems.inventory(after);
    boolean pending = false;
    var lease = local.luke.power.inventory.InventoryActions.begin(world, player, player.container, inventory, "death_recovery", true);
    if (lease == null) throw new IOException("Death recovery could not acquire inventory ownership");
    DeathInventoryRestore restoration;
    try {
      restoration = new DeathInventoryRestore(player, () -> mc.player == player && mc.world == world
          && lease.valid(mc.world, mc.player, player.container, player.inventory));
    } catch (IOException | RuntimeException failure) {
      local.luke.power.inventory.InventoryActions.finish(lease); throw failure;
    }
    try {
      data.recovery(record.id, indices, beforeData, afterData); pending = true;
      if (!beforeData.equals(DeathItems.inventory(DeathItems.snapshot(inventory, container))) || !recoveryDenial(mc).isEmpty())
        throw new IOException("Inventory ownership changed before recovery");
      restoration.publish(after, inventory.selectedSlot);
      if (!recoveryDenial(mc).isEmpty())
        throw new IOException("Inventory changed during recovery");
      data.finish(record.id, true);
    } catch (Exception e) {
      recoveryFailed = true;
      boolean restored = restoration.rollback(e);
      if (pending && restored) try { data.finish(record.id, false); } catch (IOException failure) { e.addSuppressed(failure); }
      PowerBeta.LOG.error("Death recovery failed; disabled until restart. Complete rollback: {}. Journal: {}", restored, journalPath, e);
      throw new IOException(restored ? "Recovery failed and was rolled back. Keep the journal and restart before retrying."
          : "Recovery lost inventory ownership. The claim remains blocked; keep the journal and world backup.", e);
    } finally { local.luke.power.inventory.InventoryActions.finish(lease); }
  }
}
