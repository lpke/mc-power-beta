package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.rules.*;
import net.minecraft.block.Material;
import net.minecraft.class_142;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Skip only the native lava bounce. Gravity, fluid contact and item data stay native. */
@Mixin(class_142.class)
public abstract class LavaItemMotionMixin {
  @WrapOperation(method = "tick", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_1779(III)Lnet/minecraft/block/Material;"))
  private Material power$sink(World world, int x, int y, int z, Operation<Material> original) {
    Material material = original.call(world, x, y, z);
    return material == Material.LAVA && !world.isRemote
        && !WorldRules.enabled(world.method_262(), GameRule.LAVA_DESTROYS_ITEMS)
        ? Material.AIR : material;
  }
}
