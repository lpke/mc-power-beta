package local.luke.power.mixin;

import local.luke.power.bow.*;
import net.minecraft.Item;
import net.minecraft.class_367;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Live preference changes leave saved bow damage intact, including when hidden by Beta mode. */
@Mixin(Item.class)
public class ModernBowDurabilityMixin {
  @Inject(method="getMaxDamage",at=@At("HEAD"),cancellable=true)
  private void power$durability(CallbackInfoReturnable<Integer> cir) {
    if ((Object)this instanceof class_367 && ModernBow.enabled()) cir.setReturnValue(BowPhysics.DURABILITY);
  }
}
