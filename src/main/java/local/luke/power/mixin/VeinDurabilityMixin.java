package local.luke.power.mixin;

import local.luke.power.mining.VeinMining;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class VeinDurabilityMixin {
  @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
  private void power$wear(int amount, Entity entity, CallbackInfo ci) {
    if (VeinMining.suppressWear((ItemStack)(Object)this, entity)) ci.cancel();
  }
}
