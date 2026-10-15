package local.luke.power.mixin;

import java.nio.ByteBuffer;
import local.luke.power.render.WaterTerrain;
import net.mine_diver.smoothbeta.client.render.gl.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VertexBuffer.class, remap = false)
public class WaterVertexBufferMixin {
  @Inject(method = "upload", at = @At("HEAD"), cancellable = true)
  private void power$colors(ByteBuffer data, CallbackInfo ci) {
    if (WaterTerrain.upload((VertexBuffer)(Object)this, data)) ci.cancel();
  }
}
