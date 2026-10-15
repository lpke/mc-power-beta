package local.luke.power.mixin;

import local.luke.power.render.CameraEnvironment;
import net.mine_diver.smoothbeta.client.render.Shader;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Shader.class, remap = false)
public class WaterShaderMixin {
  @Shadow private int programId;
  @Shadow private String name;
  @Unique private int power$brightnessLocation = -1;

  @Inject(method = "<init>", at = @At(value = "INVOKE",
      target = "Lnet/mine_diver/smoothbeta/client/render/gl/GlProgramManager;linkProgram(Lnet/mine_diver/smoothbeta/client/render/gl/GlShader;)V"))
  private void power$attribute(CallbackInfo ci) {
    if (name.equals("terrain")) GL20.glBindAttribLocation(programId, 5, "PowerWaterColor");
  }

  @Inject(method = "<init>", at = @At("RETURN"))
  private void power$uniform(CallbackInfo ci) {
    if (name.equals("terrain")) power$brightnessLocation = GL20.glGetUniformLocation(programId, "PowerWaterBrightness");
  }

  @Inject(method = "bind", at = @At("RETURN"))
  private void power$brightness(CallbackInfo ci) {
    if (power$brightnessLocation >= 0) GL20.glUniform1f(power$brightnessLocation, (100 - CameraEnvironment.darkness()) / 100F);
  }
}
