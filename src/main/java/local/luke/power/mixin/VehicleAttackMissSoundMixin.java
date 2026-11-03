package local.luke.power.mixin;

import local.luke.power.audio.AudioConfig;
import local.luke.power.audio.VehicleAttackSounds;
import net.minecraft.class_212;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A native accepted miss resets modern sound charge; suppressed presses and use do not. */
@Mixin(Minecraft.class)
public abstract class VehicleAttackMissSoundMixin {
  @Inject(method = "method_2107", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/entity/player/ClientPlayerEntity;method_500()V", ordinal = 0, shift = At.Shift.AFTER))
  private void power$miss(int button, CallbackInfo ci) {
    if (button != 0 || !AudioConfig.current().vehicleAttackSounds) return;
    Minecraft mc = (Minecraft) (Object) this;
    if (mc.world == null || mc.world.isRemote || mc.player == null) return;
    var hit = mc.field_2823;
    if (hit == null || hit.field_1983 == class_212.TILE
        && mc.world.getBlockId(hit.field_1984, hit.field_1985, hit.field_1986) == 0)
      VehicleAttackSounds.reset(mc.player);
  }
}
