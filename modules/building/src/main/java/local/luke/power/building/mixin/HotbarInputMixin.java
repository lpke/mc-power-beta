package local.luke.power.building.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.building.hotbar.Hotbars;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class HotbarInputMixin {
  @ModifyExpressionValue(
      method = "tick",
      at =
          @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButton()I", remap = false))
  private int power$mouse(int value) {
    return Hotbars.mouse((Minecraft) (Object) this, value);
  }

  @Inject(method = "method_2107", at = @At("HEAD"), cancellable = true)
  private void power$click(int button, CallbackInfo ci) {
    if (Hotbars.consumesMouse((Minecraft) (Object) this, button)) ci.cancel();
  }

  @Inject(method = "method_2110", at = @At("HEAD"), cancellable = true)
  private void power$heldClick(int button, boolean held, CallbackInfo ci) {
    if (Hotbars.consumesMouse((Minecraft) (Object) this, button)) ci.cancel();
  }

  @Inject(method = "tick", at = @At("HEAD"))
  private void power$hotbars(CallbackInfo ci) {
    Hotbars.tick((Minecraft) (Object) this);
  }

  @ModifyExpressionValue(
      method = "tick",
      at =
          @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false))
  private int power$scroll(int value) {
    return Hotbars.wheel((Minecraft) (Object) this, value);
  }

  @ModifyExpressionValue(
      method = "tick",
      at =
          @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;getEventKey()I", remap = false))
  private int power$rowKey(int value) {
    return Hotbars.key((Minecraft) (Object) this, value);
  }
}
