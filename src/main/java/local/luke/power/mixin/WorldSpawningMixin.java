package local.luke.power.mixin;

import local.luke.power.permissions.CheatWorld;
import local.luke.power.world.WorldSpawning;
import net.minecraft.class_567;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(class_567.class)
public abstract class WorldSpawningMixin {
  private static boolean allowed(World world, boolean hostile) {
    if (world.isRemote || !((CheatWorld) world.method_262()).power$cheatsEnabled()) return true;
    WorldSpawning rules = (WorldSpawning) world.method_262();
    return hostile ? rules.power$hostileSpawning() : rules.power$passiveSpawning();
  }
  @ModifyVariable(method = "method_1870", at = @At("HEAD"), argsOnly = true, ordinal = 0)
  private static boolean power$hostile(boolean enabled, World world, boolean hostile, boolean passive) {
    return enabled && allowed(world, true);
  }
  @ModifyVariable(method = "method_1870", at = @At("HEAD"), argsOnly = true, ordinal = 1)
  private static boolean power$passive(boolean enabled, World world, boolean hostile, boolean passive) {
    return enabled && allowed(world, false);
  }
  @Inject(method = "method_1869", at = @At("HEAD"), cancellable = true)
  private static void power$nightmare(World world, List players, CallbackInfoReturnable<Boolean> cir) {
    if (!allowed(world, true)) cir.setReturnValue(false);
  }
}
