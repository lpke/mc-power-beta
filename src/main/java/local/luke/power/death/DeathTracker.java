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
      record.cause = DeathCause.describe(player, attacker); record.items = DeathItems.lost(player.inventory);
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
    String permission = CommandPermissions.denial("deathrecover");
    if (!permission.isEmpty()) return permission;
    try {
      if (!mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString().equals("CREATIVE"))
        return "Death recovery requires creative mode.";
    } catch (ReflectiveOperationException e) { return "Creative mode could not be checked."; }
    if (recoveryFailed) return "Death recovery is disabled after a protection failure. Restart and keep the recovery journal.";
    if (mc.player.dead || mc.player.health <= 0 || mc.player.world != mc.world || mc.field_2807 != mc.player
        || DetachedCamera.isActive() || InteractionState.carryingContainer || mc.player.container != mc.player.playerContainer
        || mc.player.inventory.getCursorStack() != null)
      return "Respawn, close containers and exit freecam before recovering items.";
    return "";
  }

  public static void recover(Minecraft mc, DeathRecord record) throws IOException {
    String denial = recoveryDenial(mc);
    if (!denial.isEmpty()) throw new IllegalArgumentException(denial);
    if (!record.world.equals(CommandContext.world(mc)) || !record.player.equals(mc.player.name))
      throw new IllegalArgumentException("This death belongs to another world or player.");
    DeathJournal data = journal(mc);
    if (!data.state(record.id).equals("available"))
      throw new IllegalArgumentException("Death " + record.id + " was already recovered or has an interrupted recovery. Items were not changed.");
    var player = mc.player; var world = mc.world; var inventory = player.inventory;
    var original = inventory.main; var before = original.clone();
    String beforeData = DeathItems.inventory(before);
    var after = DeathItems.plan(inventory, record.items);
    String afterData = DeathItems.inventory(after);
    boolean pending = false;
    try {
      data.recovery(record.id, beforeData, afterData); pending = true;
      if (mc.player != player || mc.world != world || player.inventory != inventory || inventory.main != original
          || !beforeData.equals(DeathItems.inventory(original)) || !recoveryDenial(mc).isEmpty())
        throw new IOException("Inventory ownership changed before recovery");
      for (int i = 0; i < after.length; i++) inventory.setStack(i, after[i]);
      if (inventory.main != original || !afterData.equals(DeathItems.inventory(original)))
        throw new IOException("Inventory changed during recovery");
      inventory.markDirty(); data.finish(record.id, true);
    } catch (Exception e) {
      recoveryFailed = true;
      System.arraycopy(before, 0, original, 0, before.length);
      inventory.main = original; inventory.markDirty();
      if (pending) try { data.finish(record.id, false); } catch (IOException failure) { e.addSuppressed(failure); }
      PowerBeta.LOG.error("Death recovery rolled back; disabled until restart. Journal: {}", journalPath, e);
      throw new IOException("Recovery failed and was rolled back. Keep the journal and restart before retrying.", e);
    }
  }
}
