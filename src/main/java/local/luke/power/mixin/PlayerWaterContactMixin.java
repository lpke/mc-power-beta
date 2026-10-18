package local.luke.power.mixin;

import local.luke.power.gameplay.ModernWater;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class PlayerWaterContactMixin {
  @Inject(method="method_1393",at=@At("HEAD"),cancellable=true)
  private void power$playerCurrent(CallbackInfoReturnable<Boolean> cir){
    if(ModernWater.eligible((Entity)(Object)this))cir.setReturnValue(ModernWater.push((ClientPlayerEntity)(Object)this));
  }
}
