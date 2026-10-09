package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.visual.*;
import net.minecraft.class_555;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_555.class)
public abstract class DamageCameraMixin {
  @WrapOperation(method = "method_1849", at = @At(value = "FIELD",
      target = "Lnet/minecraft/entity/LivingEntity;hurtTime:I"))
  private int power$damageShake(LivingEntity entity, Operation<Integer> original) {
    var settings = VisualConfig.current();
    // Beta's multiplayer health packets omit the cause; never guess from being on fire.
    boolean fire = !entity.world.isRemote && ((DamageCameraState) entity).power$isFireHurt();
    return settings.damageCameraShake && (settings.fireDamageCameraShake || !fire)
        ? original.call(entity) : -1;
  }
}
