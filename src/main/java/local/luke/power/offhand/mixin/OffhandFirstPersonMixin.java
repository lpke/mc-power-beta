package local.luke.power.offhand.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.offhand.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_556.class)
public class OffhandFirstPersonMixin {
  @Shadow private Minecraft field_2401;
  @Shadow private ItemStack field_2402;
  @Shadow private class_321 field_2406;
  @Shadow private float field_2403;
  @Shadow private float field_2404;
  @Inject(method="method_1859",at=@At("TAIL"))
  private void power$equip(CallbackInfo ci) { OffhandRenderer.tick(field_2401); }
  @WrapMethod(method="method_1860")
  private void power$render(float delta, Operation<Void> original) {
    // Both hands keep their outgoing rendered stacks until their equip animations lower them.
    boolean map = OffhandRenderer.splitMap(field_2401, field_2402);
    if (!map) original.call(delta);
    OffhandRenderer.firstPerson(field_2401,(class_556)(Object)this,field_2406,delta,map ? field_2402 : null,
        field_2404+(field_2403-field_2404)*delta);
  }
}
