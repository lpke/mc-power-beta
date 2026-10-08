package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.ui.DebugOverlay;
import local.luke.power.ui.InterfaceState;
import local.luke.power.ui.MenuPreferences;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.class_564;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class DebugHudMixin {
  @Shadow private Minecraft minecraft;

  @WrapMethod(method = "render")
  private void power$debugFrame(float delta, boolean focused, int mx, int my, Operation<Void> original) {
    MenuPreferences.syncPresentation();
    DebugOverlay.begin();
    try { original.call(delta, focused, mx, my); }
    finally { DebugOverlay.capturing = false; }
    if (!minecraft.options.debugHud) return;
    int width = new class_564(minecraft.options, minecraft.displayWidth, minecraft.displayHeight).method_1857();
    for (var line : DebugOverlay.layout(width, minecraft.textRenderer::getWidth))
      minecraft.textRenderer.drawWithShadow(line.text(), line.x(), line.y(), InterfaceState.debugTextColor);
  }

  @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;drawWithShadow(Ljava/lang/String;III)V", ordinal = 0))
  private void power$beginDebug(float delta, boolean focused, int mx, int my, CallbackInfo ci) {
    DebugOverlay.capturing = true;
  }

  @Inject(method = "render", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPopMatrix()V", ordinal = 0, remap = false),
      slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;debugHud:Z")))
  private void power$endDebug(float delta, boolean focused, int mx, int my, CallbackInfo ci) {
    DebugOverlay.capturing = false;
  }
}
