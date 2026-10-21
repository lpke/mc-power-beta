package local.luke.power.inventory.tools.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.inventory.tools.SavedCreativeHotbars;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class, priority = 1200)
public abstract class SavedCreativeHotbarKeysMixin {
  @Inject(method = "tick", at = @At("HEAD"))
  private void power$hotbarKeysTick(CallbackInfo ci) { SavedCreativeHotbars.reconcileKeys(); }

  @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;getEventKey()I", remap = false))
  private int power$hotbarNumber(Operation<Integer> original) {
    int key = original.call();
    return SavedCreativeHotbars.key((Minecraft)(Object)this, key) ? 0 : key;
  }
}
