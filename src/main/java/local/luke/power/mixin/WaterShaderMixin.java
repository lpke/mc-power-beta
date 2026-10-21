package local.luke.power.mixin;

import local.luke.power.render.CameraEnvironment;
import local.luke.power.render.HeldLighting;
import local.luke.power.render.HeldLightShader;
import net.mine_diver.smoothbeta.client.render.Shader;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Shader.class, remap = false)
public class WaterShaderMixin implements HeldLightShader {
  @Shadow private int programId;
  @Shadow private String name;
  @Unique private int power$brightnessLocation = -1;
  @Unique private int power$heldStrength = -1, power$heldEmission = -1, power$heldSource = -1, power$heldCurve = -1;
  @Unique private float power$appliedStrength;

  @Inject(method = "<init>", at = @At(value = "INVOKE",
      target = "Lnet/mine_diver/smoothbeta/client/render/gl/GlProgramManager;linkProgram(Lnet/mine_diver/smoothbeta/client/render/gl/GlShader;)V"))
  private void power$attribute(CallbackInfo ci) {
    if (name.equals("terrain")) {
      GL20.glBindAttribLocation(programId, 5, "PowerWaterColor");
      GL20.glBindAttribLocation(programId, 6, "PowerLightMetaHigh");
    }
  }

  @Inject(method = "<init>", at = @At("RETURN"))
  private void power$uniform(CallbackInfo ci) {
    if (name.equals("terrain")) {
      power$brightnessLocation = GL20.glGetUniformLocation(programId, "PowerWaterBrightness");
      power$heldStrength = GL20.glGetUniformLocation(programId, "PowerHeldStrength");
      power$heldEmission = GL20.glGetUniformLocation(programId, "PowerHeldEmission");
      power$heldSource = GL20.glGetUniformLocation(programId, "PowerHeldSourceRegion");
      power$heldCurve = GL20.glGetUniformLocation(programId, "PowerHeldCurve[0]");
    }
  }

  @Inject(method = "bind", at = @At("RETURN"))
  private void power$brightness(CallbackInfo ci) {
    if (power$brightnessLocation >= 0) GL20.glUniform1f(power$brightnessLocation, (100 - CameraEnvironment.darkness()) / 100F);
    if (power$heldStrength >= 0) {
      // A region explicitly supplies the source before drawing; never retain another region's pose.
      GL20.glUniform1f(power$heldStrength, 0);
      power$appliedStrength = 0;
      if (HeldLighting.strength() > 0) {
        GL20.glUniform1f(power$heldEmission, HeldLighting.emission());
        GL20.glUniform1(power$heldCurve, HeldLighting.curve());
      }
    }
  }

  @Override public void power$heldRegion(int x, int y, int z) {
    if (power$heldStrength < 0) return;
    float strength = HeldLighting.regionStrength(x, y, z);
    if (power$appliedStrength != strength) {
      GL20.glUniform1f(power$heldStrength, strength); power$appliedStrength = strength;
    }
    if (strength == 0) return;
    GL20.glUniform3f(power$heldSource, HeldLighting.regionX(x), HeldLighting.regionY(y), HeldLighting.regionZ(z));
  }
}
