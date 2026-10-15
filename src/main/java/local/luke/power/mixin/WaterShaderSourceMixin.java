package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.io.InputStream;
import local.luke.power.render.WaterShaderSource;
import net.mine_diver.smoothbeta.client.render.gl.GLImportProcessor;
import net.mine_diver.smoothbeta.client.render.gl.Program;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Program.class, remap = false)
public class WaterShaderSourceMixin {
  @ModifyExpressionValue(method = "loadProgram", at = @At(value = "INVOKE",
      target = "Lnet/modificationstation/stationapi/api/client/texture/TextureUtil;readResourceAsString(Ljava/io/InputStream;)Ljava/lang/String;"))
  private static String power$source(String source, Program.Type type, String name, InputStream input, String domain, GLImportProcessor loader) {
    return type == Program.Type.VERTEX && name.equals("terrain") ? WaterShaderSource.extend(source) : source;
  }
}
