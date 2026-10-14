package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.gameplay.TorchShovel;
import net.minecraft.SingleplayerInteractionManager;
import net.minecraft.block.Block;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SingleplayerInteractionManager.class)
public abstract class TorchShovelMiningMixin extends InteractionManager {
  protected TorchShovelMiningMixin(Minecraft minecraft) { super(minecraft); }
  @WrapMethod(method = "method_1716")
  private boolean power$dig(int x, int y, int z, int face, Operation<Boolean> original) {
    Block block = Block.BLOCKS[minecraft.world.getBlockId(x, y, z)];
    var player = minecraft.player;
    int slot = TorchShovel.slot(player, block);
    ItemStack shovel = slot < 0 ? null : player.inventory.main[slot];
    boolean broken = original.call(x, y, z, face);
    if (broken && shovel != null) TorchShovel.wear(player, block, slot, shovel, x, y, z);
    if (broken) local.luke.power.mining.VeinMining.broken(minecraft, x, y, z);
    return broken;
  }
}
