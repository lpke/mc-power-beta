package local.luke.power.mining;

import local.luke.power.PowerBeta;
import local.luke.power.gameplay.TorchShovel;
import local.luke.power.input.*;
import net.minecraft.SingleplayerInteractionManager;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.lwjgl.opengl.Display;
import static local.luke.power.mining.VeinSearch.Position;

/** Tick-owned search and native mining. Queued blocks never load terrain or survive a world visit. */
public final class VeinMining {
  private static Plan candidate, job;
  private static Plan executing;
  private static Object failedWorld;
  private VeinMining() {}
  public static void reset() { candidate = job = null; failedWorld = null; }
  private static boolean eligible(Minecraft mc) {
    var player = mc.player;
    return VeinConfig.current().enabled && mc.world != null && !mc.world.isRemote && mc.world != failedWorld
        && player != null && player.world == mc.world && !player.dead && player.health > 0
        && mc.interactionManager instanceof SingleplayerInteractionManager && mc.currentScreen == null
        && !mc.paused && Display.isActive() && !MovementOwnership.cameraControlsMovement()
        && !InteractionState.carryingContainer
        && (!(player instanceof PlayerMode mode) || !mode.power$creative() && !mode.power$spectator());
  }
  public static boolean busy() { return job != null && executing == null; }
  public static void prepare(Minecraft mc, int x, int y, int z) {
    if (executing != null || job != null) return;
    if (!eligible(mc)) { candidate = null; return; }
    if (candidate != null && candidate.origin.equals(new Position(x, y, z)) && candidate.valid(mc)
        && candidate.matches(candidate.origin)) return;
    candidate = null;
    var settings = VeinConfig.current();
    if (settings.crouchingOnly && !mc.player.method_1373() || !mc.world.method_239(x, y, z)) return;
    int id = mc.world.getBlockId(x, y, z);
    if (id <= 0 || id >= Block.BLOCKS.length || Block.BLOCKS[id] == null) return;
    Block block = Block.BLOCKS[id];
    int shovelSlot = TorchShovel.slot(mc.player, block);
    ItemStack tool = shovelSlot < 0 ? mc.player.method_502() : mc.player.inventory.main[shovelSlot];
    if (!settings.accepts(id, tool == null ? 0 : tool.itemId) || !mc.player.method_514(block)
        || block.method_1582(mc.player) <= 0 || mc.world.method_1777(x, y, z) != null) return;
    candidate = new Plan(mc, new Position(x, y, z), id, mc.world.method_1778(x, y, z), shovelSlot, tool);
  }
  public static float progress(float nativeProgress, PlayerEntity player, int x, int y, int z) {
    var plan = candidate;
    if (executing != null || plan == null || plan.player != player || !plan.origin.equals(new Position(x, y, z))) return nativeProgress;
    return plan.search.complete() ? (float)(nativeProgress / plan.settings.timeFactor(plan.search.blocks().size())) : 0;
  }
  public static void broken(Minecraft mc, int x, int y, int z) {
    if (executing != null) return;
    Plan plan = candidate; candidate = null;
    if (plan == null || !plan.origin.equals(new Position(x, y, z)) || !plan.search.complete()
        || plan.search.blocks().size() <= 1) return;
    plan.acceptWear();
    if (plan.valid(mc)) { plan.index = 1; job = plan; }
  }
  public static boolean suppressWear(ItemStack stack, Entity entity) {
    return executing != null && !executing.settings.durabilityPerBlock && executing.tool == stack && executing.player == entity;
  }
  public static void tick(Minecraft mc) {
    if (failedWorld != null && failedWorld != mc.world) failedWorld = null;
    if (!eligible(mc)) { candidate = job = null; return; }
    try {
      Plan plan = candidate;
      if (plan != null) {
        if (!plan.valid(mc) || !plan.matches(plan.origin)) candidate = null;
        else {
          long start = System.nanoTime();
          for (int n = 0; n < plan.settings.checksPerTick && !plan.search.complete(); n++) {
            plan.search.step(1, plan::matches);
            if (System.nanoTime() - start >= 1_000_000L) break;
          }
        }
      }
      plan = job;
      if (plan == null) return;
      if (!plan.valid(mc)) { job = null; return; }
      long start = System.nanoTime();
      for (int n = 0; n < plan.settings.blocksPerTick && plan.index < plan.search.blocks().size(); n++) {
        Position pos = plan.search.blocks().get(plan.index++);
        // The saved search is only a plan; every block and tool is checked again at commit time.
        if (!plan.valid(mc)) { job = null; break; }
        if (!plan.matches(pos)) continue;
        boolean broken;
        executing = plan;
        try { broken = mc.interactionManager.method_1716(pos.x(), pos.y(), pos.z(), 1); }
        finally { executing = null; }
        if (!broken) { job = null; break; }
        plan.acceptWear();
        if (System.nanoTime() - start >= 2_000_000L) break;
      }
      if (plan.index >= plan.search.blocks().size()) job = null;
    } catch (RuntimeException failure) {
      candidate = job = executing = null; failedWorld = mc.world;
      PowerBeta.LOG.error("Vein mining stopped for this world visit after a block callback failed", failure);
      mc.inGameHud.addChatMessage("Vein mining stopped after a block error. Reload the world to retry.");
    }
  }
  private static int kind(int id) { return id == 74 ? 73 : id; }
  private static final class Plan {
    final World world;
    final PlayerEntity player;
    final VeinSettings settings;
    final Position origin;
    final VeinSearch search;
    final int id, metadata, selectedSlot, toolSlot, heldId, toolId;
    final ItemStack held, tool;
    final int heldCount, toolCount;
    int damage, index;
    Plan(Minecraft mc, Position origin, int id, int metadata, int toolSlot, ItemStack tool) {
      world = mc.world; player = mc.player; settings = VeinConfig.current(); this.origin = origin;
      this.id = kind(id); this.metadata = metadata; this.toolSlot = toolSlot; this.tool = tool;
      held = player.method_502(); selectedSlot = player.inventory.selectedSlot;
      heldId = held == null ? 0 : held.itemId; heldCount = held == null ? 0 : held.count;
      toolId = tool == null ? 0 : tool.itemId; toolCount = tool == null ? 0 : tool.count;
      search = new VeinSearch(origin, settings.maxBlocks, settings.maxChecks, settings.radius);
      acceptWear();
    }
    void acceptWear() { damage = tool == null ? 0 : tool.getDamage(); }
    boolean valid(Minecraft mc) {
      if (mc.world != world || mc.player != player || VeinConfig.current() != settings || !eligible(mc)
          || settings.crouchingOnly && !player.method_1373() || selectedSlot != player.inventory.selectedSlot
          || player.method_502() != held || held != null && (held.count != heldCount || held.itemId != heldId)
          || tool != null && (tool.count <= 0 || tool.count != toolCount || tool.itemId != toolId || tool.getDamage() != damage)) return false;
      if (toolSlot >= 0 && player.inventory.main[toolSlot] != tool) return false;
      double dx = player.x - origin.x() - .5, dy = player.y - origin.y() - .5, dz = player.z - origin.z() - .5;
      double reach = mc.interactionManager.method_1715() + 2;
      return dx * dx + dy * dy + dz * dz <= reach * reach;
    }
    boolean matches(Position pos) {
      if (!world.method_239(pos.x(), pos.y(), pos.z())) return false;
      int blockId = world.getBlockId(pos.x(), pos.y(), pos.z());
      return kind(blockId) == id && world.method_1778(pos.x(), pos.y(), pos.z()) == metadata
          && world.method_1777(pos.x(), pos.y(), pos.z()) == null;
    }
  }
}
