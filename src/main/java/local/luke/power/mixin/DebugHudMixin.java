package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.input.DetachedCamera;
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

  @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/ClientPlayerEntity;x:D"))
  private double power$debugX(double value) { return DetachedCamera.isActive() ? DetachedCamera.x() : value; }

  @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/ClientPlayerEntity;y:D"))
  private double power$debugY(double value) { return minecraft.player == null ? value : local.luke.power.hud.HudCoordinates.y(minecraft.player); }

  @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/ClientPlayerEntity;z:D"))
  private double power$debugZ(double value) { return DetachedCamera.isActive() ? DetachedCamera.z() : value; }

  @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/ClientPlayerEntity;yaw:F"))
  private float power$debugFacing(float value) { return DetachedCamera.isActive() ? DetachedCamera.facingYaw() : value; }

  @WrapMethod(method = "render")
  private void power$debugFrame(float delta, boolean focused, int mx, int my, Operation<Void> original) {
    MenuPreferences.syncPresentation();
    DebugOverlay.begin();
    try { original.call(delta, focused, mx, my); }
    finally { DebugOverlay.capturing = false; }
    local.luke.power.hud.InfoHud.render(minecraft);
    if (!minecraft.options.debugHud) return;
    if (MenuPreferences.debugTimeOfDay() && !local.luke.power.hud.InfoHud.timeLine().isEmpty())
      DebugOverlay.add(local.luke.power.hud.InfoHud.timeLine(), 2, 144);
    int width = new class_564(minecraft.options, minecraft.displayWidth, minecraft.displayHeight).method_1857();
    local.luke.power.ui.DebugTextRenderer.draw(minecraft,
        DebugOverlay.layoutOrdered(width, minecraft.textRenderer::getWidth, MenuPreferences.debugOrder(),
            MenuPreferences::debugLine, MenuPreferences.debugTextSize()), MenuPreferences.debugTextSize(), InterfaceState.debugTextColor);
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
