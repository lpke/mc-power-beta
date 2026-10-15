package local.luke.power.mixin;

import local.luke.power.rules.*;
import net.minecraft.class_104;
import net.minecraft.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_104.class)
public class SpawnerRulesMixin extends BlockEntity {
  @Inject(method = "method_1076", at = @At("HEAD"), cancellable = true)
  private void power$spawner(CallbackInfo ci) {
    if (!world.isRemote && !WorldRules.enabled(world.method_262(), GameRule.SPAWNER_BLOCKS_WORK)) ci.cancel();
  }
}
