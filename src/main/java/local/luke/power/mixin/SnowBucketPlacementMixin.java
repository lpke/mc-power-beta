package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.gameplay.GameplayConfig;
import net.minecraft.block.Block;
import net.minecraft.class_212;
import net.minecraft.class_220;
import net.minecraft.class_224;
import net.minecraft.class_27;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_220.class)
public abstract class SnowBucketPlacementMixin {
  @Shadow private int field_842;

  @WrapOperation(method = "method_451", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_161(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Z)Lnet/minecraft/class_27;"))
  private class_27 power$replaceClickedSnow(World world, Vec3d from, Vec3d to, boolean fluids,
      Operation<class_27> original) {
    class_27 hit = original.call(world, from, to, fluids);
    if (world.isRemote || !GameplayConfig.current().modernSnowPlacement
        || ((Object) this).getClass() != class_220.class
        || ((Object) this != net.minecraft.Item.field_450 && (Object) this != net.minecraft.Item.field_451)
        || (field_842 != Block.FLOWING_WATER.id && field_842 != Block.FLOWING_LAVA.id)
        || hit == null || hit.field_1983 != class_212.TILE
        || world.getBlockId(hit.field_1984, hit.field_1985, hit.field_1986) != 78
        || Block.BLOCKS[78] != Block.field_1867 || Block.field_1867.getClass() != class_224.class) return hit;
    // An unshifted hit keeps native permissions, evaporation and bucket exchange in charge.
    return new class_27(hit.field_1984, hit.field_1985, hit.field_1986, -1, hit.field_1988);
  }
}
