package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.render.TerrainVertexColors;
import net.mine_diver.smoothbeta.client.render.VboPool;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VboPool.class, remap = false)
public class WaterVboPoolMixin {
  @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE",
      target = "Lnet/mine_diver/smoothbeta/client/render/VertexFormat;getVertexSizeByte()I"))
  private int power$stride(int stride) { return TerrainVertexColors.STRIDE; }

  @ModifyConstant(method = "drawAll", constant = @Constant(intValue = 28))
  private int power$drawStride(int stride) { return TerrainVertexColors.STRIDE; }

  @Inject(method = "drawAll", at = @At(value = "INVOKE",
      target = "Lnet/mine_diver/smoothbeta/client/render/IndexBuffer;getSequentialBuffer(Lnet/mine_diver/smoothbeta/client/render/VertexFormat$DrawMode;)Lnet/mine_diver/smoothbeta/client/render/IndexBuffer;"))
  private void power$bindColor(CallbackInfo ci) {
    GL20.glEnableVertexAttribArray(5);
    GL20.glVertexAttribPointer(5, 4, GL11.GL_UNSIGNED_BYTE, true, TerrainVertexColors.STRIDE, (long)TerrainVertexColors.WATER_COLOR);
  }

  @Inject(method = "drawAll", at = @At("RETURN"))
  private void power$unbindColor(CallbackInfo ci) { GL20.glDisableVertexAttribArray(5); }
}
