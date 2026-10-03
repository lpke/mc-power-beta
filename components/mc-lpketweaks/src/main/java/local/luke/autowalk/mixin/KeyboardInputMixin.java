package local.luke.autowalk.mixin;

import local.luke.autowalk.AutoWalk;
import local.luke.autowalk.WalkToggle;
import net.minecraft.class_41;
import net.minecraft.class_413;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Barn build 8 leaves KeyboardInput and its movement fields unnamed. */
@Mixin(class_413.class)
public abstract class KeyboardInputMixin extends class_41 {
  @Shadow private boolean[] field_1661;
  @Shadow private GameOptions field_1662;

  @Inject(method = "method_1941", at = @At("HEAD"))
  private void cancelOnManualMovement(int keyCode, boolean pressed, CallbackInfo ci) {
    if (pressed && (keyCode == field_1662.forwardKey.code || keyCode == field_1662.backKey.code)) {
      AutoWalk.stop();
    }
  }

  @Inject(method = "method_1942", at = @At("TAIL"))
  private void applyAutoWalk(PlayerEntity player, CallbackInfo ci) {
    if (AutoWalk.isWalking()) {
      // forward input, physical forward key, sneak state; preserve vanilla strafing/jumping.
      field_2533 = WalkToggle.forwardInput(field_2533, field_1661[0], field_2536);
    }
  }
}
