package local.luke.power.mixin;

import java.util.Random;
import local.luke.power.rules.*;
import net.minecraft.class_474;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_474.class)
public class FireSpreadMixin {
  // Native age, scheduled ticks, rain and extinguishing run before the first spread attempt.
  @Inject(method = "method_1602", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/class_474;method_1823(Lnet/minecraft/world/World;IIIILjava/util/Random;I)V", ordinal = 0), cancellable = true)
  private void power$spread(World world, int x, int y, int z, Random random, CallbackInfo ci) {
    if (!world.isRemote && !WorldRules.enabled(world.method_262(), GameRule.FIRE_SPREAD)) ci.cancel();
  }

  @Inject(method = "method_1823", at = @At("HEAD"), cancellable = true)
  private void power$unprimedTnt(World world, int x, int y, int z, int chance, Random random, int age, CallbackInfo ci) {
    if (!world.isRemote && world.getBlockId(x, y, z) == Block.field_1887.id
        && !WorldRules.enabled(world.method_262(), GameRule.TNT_EXPLODES)) ci.cancel();
  }
}
