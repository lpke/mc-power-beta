package local.luke.power.mixin;

import local.luke.power.rules.*;
import net.minecraft.class_142;
import net.minecraft.class_329;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_329.class)
public class CactusItemsMixin {
  @Inject(method = "method_1615", at = @At("HEAD"), cancellable = true)
  private void power$protectItem(World world, int x, int y, int z, Entity entity, CallbackInfo ci) {
    if (world != null && !world.isRemote && entity instanceof class_142
        && !WorldRules.enabled(world.method_262(), GameRule.CACTUS_DESTROYS_ITEMS)) ci.cancel();
  }
}
