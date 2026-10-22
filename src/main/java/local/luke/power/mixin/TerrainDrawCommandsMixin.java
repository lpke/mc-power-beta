package local.luke.power.mixin;

import java.nio.IntBuffer;
import local.luke.power.render.TerrainDrawCommands;
import net.mine_diver.smoothbeta.client.render.VboPool;
import net.mine_diver.smoothbeta.client.render.VertexFormat;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VboPool.class, remap = false)
public class TerrainDrawCommandsMixin {
  @Shadow private IntBuffer bufferIndirect;

  @Redirect(method = {"<init>", "expandVbo"}, at = @At(value = "INVOKE",
      target = "Lnet/minecraft/class_214;method_745(I)Ljava/nio/IntBuffer;", remap = true))
  private IntBuffer power$commandStorage(int vertexSizedWords) {
    // Vertex-pool growth must neither allocate vertex-count-sized command storage
    // nor discard already submitted commands.
    return bufferIndirect == null ? TerrainDrawCommands.initial() : bufferIndirect;
  }

  @Inject(method = "upload", at = @At("HEAD"))
  private void power$commandRoom(VertexFormat.DrawMode mode, VboPool.Pos position, CallbackInfo ci) {
    bufferIndirect = TerrainDrawCommands.room(bufferIndirect);
  }
}
