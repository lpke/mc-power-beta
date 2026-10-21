package local.luke.power.offhand.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.offhand.OffhandUse;
import local.luke.power.inventory.HandUse;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class OffhandEntityUseMixin {
  @WrapOperation(method="interact", at=@At(value="INVOKE", target="Lnet/minecraft/entity/Entity;method_1323(Lnet/minecraft/entity/player/PlayerEntity;)Z"))
  private boolean power$entityResult(Entity target, PlayerEntity player, Operation<Boolean> original) {
    boolean result = original.call(target, player); OffhandUse.entityResult(result); return result;
  }
  @Inject(method="method_500", at=@At("HEAD"), cancellable=true)
  private void power$leftSwing(CallbackInfo ci) {
    PlayerEntity player = (PlayerEntity)(Object)this;
    if (HandUse.hand(player.inventory) == HandUse.Hand.OFF) { OffhandUse.swing(player); ci.cancel(); }
  }
}
