package local.luke.power.worldedit.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import local.luke.power.worldedit.EditScope;
import net.minecraft.block.Block;
import net.minecraft.class_43;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_43.class)
public abstract class ChunkMixin {
  @WrapWithCondition(
      method = "method_861",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/block/Block;method_1630(Lnet/minecraft/world/World;III)V"))
  private boolean worldedit$remove(Block block, World world, int x, int y, int z) {
    return !EditScope.active(world);
  }

  @WrapWithCondition(
      method = "method_861",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/block/Block;method_1611(Lnet/minecraft/world/World;III)V"))
  private boolean worldedit$place(Block block, World world, int x, int y, int z) {
    return !EditScope.active(world);
  }
}
