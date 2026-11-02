package local.luke.power.mixin;

import local.luke.power.bow.ModernBow;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ClientPlayerEntity.class,priority=1100)
public class ModernBowMovementMixin {
  @Inject(method="method_937",at=@At(value="INVOKE",target="Lnet/minecraft/entity/player/PlayerEntity;method_937()V"))
  private void power$usingSlowdown(CallbackInfo ci){
    ClientPlayerEntity player=(ClientPlayerEntity)(Object)this;
    if(ModernBow.hand(player)==null)return;
    player.field_161.field_2532*=.2f;player.field_161.field_2533*=.2f;
  }
}
