package local.luke.power.mixin;

import net.minecraft.class_430;
import net.minecraft.class_66;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep visibility, distance and update priority while returning zero for equal entries. */
@Mixin(class_430.class)
public abstract class ChunkUpdateOrderMixin {
  @Shadow private LivingEntity field_1698;

  @Inject(method = "compare(Lnet/minecraft/class_66;Lnet/minecraft/class_66;)I", at = @At("HEAD"), cancellable = true)
  private void power$order(class_66 first, class_66 second, CallbackInfoReturnable<Integer> cir) {
    int order = Boolean.compare(first.field_243, second.field_243);
    if (order == 0) order = Float.compare(second.method_299(field_1698), first.method_299(field_1698));
    if (order == 0) order = Integer.compare(second.field_251, first.field_251);
    cir.setReturnValue(order);
  }
}
