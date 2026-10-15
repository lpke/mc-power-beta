package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.input.RenderedView;
import local.luke.power.render.CameraEnvironment;
import local.luke.power.render.RenderControls;
import net.minecraft.client.Minecraft;
import net.minecraft.class_555;
import net.minecraft.block.Material;
import net.minecraft.entity.LivingEntity;
import local.luke.power.visual.VisualConfig;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.class_555.class)
public class FullBrightRenderMixin {
  @Shadow private boolean field_2330;
  @Shadow private Minecraft field_2349;

  // Enclose StationAPI's fog hooks too, so colour and distance use the same material.
  @WrapMethod(method = "method_1842")
  private void power$fogDistance(int pass, float delta, Operation<Void> original) {
    RenderControls.beginFog(!field_2330);
    try { original.call(pass, delta); }
    finally { RenderControls.endFog(); }
  }

  @WrapMethod(method = "method_1852")
  private void power$fogColour(float delta, Operation<Void> original) {
    RenderControls.beginFog(!field_2330);
    try { original.call(delta); }
    finally { RenderControls.endFog(); }
  }

  @WrapOperation(method = "method_1848", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/entity/LivingEntity;isInFluid(Lnet/minecraft/block/Material;)Z"))
  private boolean power$waterFov(LivingEntity camera, Material material, Operation<Boolean> original) {
    return VisualConfig.current().narrowUnderwaterFov && CameraEnvironment.inFluid(camera, material);
  }

  @Shadow private void method_1852(float delta) {}

  // Beta computes fog before positioning the camera. Defer it until this eye's
  // transform is ready, including third person, free look, bobbing and freecam.
  @WrapOperation(method = "method_1841", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_555;method_1852(F)V"))
  private void power$deferColour(class_555 renderer, float delta, Operation<Void> original) {}

  @WrapOperation(method = "method_1841", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glClear(I)V", ordinal = 0, remap = false))
  private void power$deferColourClear(int mask, Operation<Void> original) {
    original.call(mask & ~GL11.GL_COLOR_BUFFER_BIT);
  }

  @WrapOperation(method = "method_1841", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_555;method_1840(FI)V"))
  private void power$cameraFog(class_555 renderer, float delta, int eye, Operation<Void> original) {
    original.call(renderer, delta, eye);
    CameraEnvironment.updateLighting(field_2349);
    method_1852(delta);
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
  }

  // The first 0.1 is cloud fog; only the second belongs to water.
  @ModifyConstant(method = "method_1842", constant = @Constant(floatValue = 0.1F, ordinal = 1))
  private float power$waterDensity(float density) {
    return density * RenderControls.waterFogScale();
  }

  @WrapMethod(method = "method_1841")
  private void power$render(float delta, long end, Operation<Void> original) {
    RenderedView.clear();
    RenderControls.beginRender(false);
    try { original.call(delta, end); }
    finally { RenderControls.endRender(); RenderedView.clear(); }
  }
}
