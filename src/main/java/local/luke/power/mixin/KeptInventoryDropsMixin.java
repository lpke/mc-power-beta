package local.luke.power.mixin;

import local.luke.power.death.KeepInventory;
import net.minecraft.entity.player.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public abstract class KeptInventoryDropsMixin {
  @Shadow public PlayerEntity player;
  @Inject(method = "method_688", at = @At("HEAD"), cancellable = true)
  private void power$keepStacks(CallbackInfo ci) { if (KeepInventory.kept(player)) ci.cancel(); }
}
