package local.luke.power.mixin;

import java.util.Random;
import local.luke.power.gameplay.GameplayConfig;
import net.minecraft.class_315;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_315.class)
public class LavaIgnitionMixin {
  // Still-fluid random ticks only attempt lava ignition; flow has its own native path.
  @Inject(method = "method_1602", at = @At("HEAD"), cancellable = true)
  private void power$ignite(World world, int x, int y, int z, Random random, CallbackInfo ci) {
    if (!world.isRemote && !GameplayConfig.current().fireSpread) ci.cancel();
  }
}
