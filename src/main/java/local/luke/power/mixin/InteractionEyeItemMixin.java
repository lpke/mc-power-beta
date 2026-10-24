package local.luke.power.mixin;

import local.luke.power.input.InteractionEye;
import net.minecraft.class_220;
import net.minecraft.class_483;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Buckets and boats build independent rays with Beta's upright eye correction. */
@Mixin({class_220.class,class_483.class})
public class InteractionEyeItemMixin {
  @ModifyArgs(method="method_451",at=@At(value="INVOKE",
      target="Lnet/minecraft/util/math/Vec3d;createCached(DDD)Lnet/minecraft/util/math/Vec3d;",ordinal=0))
  private void power$itemPickOrigin(Args args,ItemStack stack,World world,PlayerEntity player) {
    double nativeOffset=1.62-player.field_1631;
    args.set(1,(double)args.get(1)+InteractionEye.offset(player,1,nativeOffset)-nativeOffset);
  }
}
