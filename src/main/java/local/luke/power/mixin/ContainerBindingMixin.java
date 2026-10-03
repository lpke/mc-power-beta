package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.input.Bindings;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ContainerScreen.class)
public class ContainerBindingMixin extends net.minecraft.client.gui.screen.Screen {
  @org.spongepowered.asm.mixin.injection.Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
  private void power$closeWithMouse(int x, int y, int button, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
    if (Bindings.matches(minecraft.options.inventoryKey, button - 100)) {
      minecraft.player.closeScreen();
      ci.cancel();
    }
  }

  @WrapOperation(method = "keyPressed", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/KeyBinding;code:I", opcode = 180))
  private int power$eventCode(KeyBinding binding, Operation<Integer> original) {
    return Bindings.eventCode(binding);
  }
}
