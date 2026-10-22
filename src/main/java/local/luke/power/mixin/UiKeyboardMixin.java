package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.input.KeyEventModifiers;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Screen.class)
public abstract class UiKeyboardMixin {
  @WrapMethod(method = "onKeyboardEvent")
  private void power$keyboardModifiers(Operation<Void> original) {
    KeyEventModifiers.begin();
    try {
      var mc = (net.minecraft.client.Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
      if (mc.currentScreen instanceof net.minecraft.client.gui.screen.container.ContainerScreen) {
        int key = org.lwjgl.input.Keyboard.getEventKey();
        local.luke.power.debug.DebugKeys.event(key, org.lwjgl.input.Keyboard.getEventKeyState(), org.lwjgl.input.Keyboard.isRepeatEvent());
        if (local.luke.power.input.DebugInput.suppressNative(key)) return;
      }
      original.call();
    }
    finally { KeyEventModifiers.end(); }
  }
}
