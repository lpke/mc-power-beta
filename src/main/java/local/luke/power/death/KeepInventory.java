package local.luke.power.death;

import java.io.IOException;
import java.util.Arrays;
import local.luke.power.PowerBeta;
import local.luke.power.world.WorldInventory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;

public final class KeepInventory {
  private KeepInventory() {}
  public static boolean kept(PlayerEntity player) {
    return player.world != null && !player.world.isRemote && (player.health <= 0 || player.dead)
        && ((KeptPlayer)player).power$keptInventory() != null;
  }
  public static void begin(PlayerEntity player) {
    Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
    ((KeptPlayer)player).power$keptInventory(null);
    if (player != mc.player || player.world != mc.world || player.world.isRemote
        || !((WorldInventory)player.world.method_262()).power$keepInventory()) return;
    try { ((KeptPlayer)player).power$keptInventory(KeptInventory.capture(player)); }
    catch (IOException e) { throw new IllegalStateException("Could not protect kept inventory; death stopped", e); }
  }
  public static void transfer(Minecraft mc, PlayerEntity previous, KeptInventory saved) {
    if (previous == null || saved == null) return;
    if (mc.world == null || mc.world.isRemote || mc.player == null || mc.player == previous || !mc.player.name.equals(previous.name))
      throw new IllegalStateException("Kept inventory ownership changed during respawn");
    var replacement = mc.player; var world = mc.world; var source = previous.inventory;
    var main = source.main; var armor = source.armor; var container = previous.playerContainer;
    var crafting = ((net.minecraft.class_277)container).field_1124;
    var grid = ((local.luke.power.mixin.DeathCraftingAccessor)crafting).power$deathStacks();
    try {
      saved.restore(replacement, () -> mc.player == replacement && mc.world == world
          && previous.inventory == source && source.main == main && source.armor == armor
          && previous.playerContainer == container && ((net.minecraft.class_277)container).field_1124 == crafting
          && ((local.luke.power.mixin.DeathCraftingAccessor)crafting).power$deathStacks() == grid && saved.matches(previous));
      Arrays.fill(main, null); Arrays.fill(armor, null); source.setCursorStack(null);
      local.luke.power.offhand.Offhand.set(source, null);
      // The retired player's inputs have transferred. Do not run recipe callbacks a second time.
      Arrays.fill(grid, null);
      ((KeptPlayer)previous).power$keptInventory(null); ((KeptPlayer)replacement).power$keptInventory(null);
    } catch (IOException e) {
      // Keep a durable snapshot on the new player even if a third-party slot callback fails.
      ((KeptPlayer)replacement).power$keptInventory(saved);
      PowerBeta.LOG.error("Kept inventory transfer failed; snapshot retained on player", e);
      throw new IllegalStateException("Could not restore kept inventory; keep the world backup", e);
    }
  }
}
