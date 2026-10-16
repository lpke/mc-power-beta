package local.luke.power.validation.mixin;

import net.minecraft.class_60;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class Explosion210Capture {
  @Inject(method = "method_188", at = @At("HEAD"))
  private void power$record(Entity source, double x, double y, double z, float power, boolean flames,
      CallbackInfoReturnable<class_60> cir) {
    local.luke.power.validation.ExplosionProbe.calls++;
    local.luke.power.validation.ExplosionProbe.fire = flames;
  }
}
