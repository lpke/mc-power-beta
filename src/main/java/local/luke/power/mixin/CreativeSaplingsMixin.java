package local.luke.power.mixin;

import local.luke.power.gameplay.GameplayConfig;
import local.luke.power.input.PlayerMode;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class CreativeSaplingsMixin {
  @Inject(method = "method_1581", at = @At("HEAD"), cancellable = true)
  private void power$saplings(World world, int x, int y, int z, ItemStack stack, CallbackInfo ci) {
    if ((Object)this != Block.LEAVES || world.isRemote || stack.itemId != Block.SAPLING.id
        || GameplayConfig.current().leavesDropSaplingsInCreative) return;
    Minecraft mc = (Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
    if (mc.world == world && mc.player instanceof PlayerMode mode && mode.power$creative()) ci.cancel();
  }
}
