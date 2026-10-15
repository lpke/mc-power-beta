package local.luke.power.mixin;

import local.luke.power.rules.*;
import net.minecraft.class_132;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_132.class)
public class TntRulesMixin {
  @Inject(method = "method_1611", at = @At("HEAD"), cancellable = true)
  private void power$placed(World world, int x, int y, int z, CallbackInfo ci) {
    if (disabled(world)) ci.cancel();
  }
  @Inject(method = "method_1609", at = @At("HEAD"), cancellable = true)
  private void power$powered(World world, int x, int y, int z, int neighbor, CallbackInfo ci) {
    if (disabled(world)) ci.cancel();
  }
  @Inject(method = "method_1613", at = @At("HEAD"), cancellable = true)
  private void power$chain(World world, int x, int y, int z, CallbackInfo ci) {
    if (disabled(world)) ci.cancel();
  }
  @ModifyVariable(method = "method_1612", at = @At("HEAD"), argsOnly = true, ordinal = 3)
  private int power$break(int metadata, World world, int x, int y, int z, int ignored) {
    return disabled(world) ? metadata & ~1 : metadata;
  }
  private static boolean disabled(World world) {
    return !world.isRemote && !WorldRules.enabled(world.method_262(), GameRule.TNT_EXPLODES);
  }
}
