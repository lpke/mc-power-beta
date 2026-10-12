package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.input.PlayerMode;
import net.minecraft.block.Block;
import net.minecraft.client.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InteractionManager.class)
public class CreativeTntMixin {
  @Shadow @Final protected Minecraft minecraft;
  @WrapOperation(method = "method_1716", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;method_1612(Lnet/minecraft/world/World;IIII)V"))
  private void power$break(Block block, World world, int x, int y, int z, int meta, Operation<Void> original) {
    if (block == Block.field_1887 && !world.isRemote && minecraft.player instanceof PlayerMode mode && mode.power$creative()) return;
    original.call(block, world, x, y, z, meta);
  }
}
