package local.luke.power.controls.mixin.tweaks.fpslimitslider;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.controls.ControlFeatures;
import local.luke.power.controls.util.ModOptions;
import net.minecraft.client.render.GameRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @ModifyExpressionValue(method = "onFrameUpdate", at = @At(value = "CONSTANT", args = "intValue=200", ordinal = 0))
    public int modifyFpsTarget(int original) {
        return ModOptions.getFpsLimitValue();
    }

    @ModifyExpressionValue(method = "onFrameUpdate", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/option/GameOptions;fpsLimit:I", ordinal = 0))
    public int overridePerformanceLevel1(int original) {
        return -1;
    }

    @ModifyExpressionValue(method = "onFrameUpdate", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/option/GameOptions;fpsLimit:I", ordinal = 1))
    public int overridePerformanceLevel2(int original) {
        return -1;
    }

    @ModifyExpressionValue(method = "onFrameUpdate", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/option/GameOptions;fpsLimit:I", ordinal = 2))
    public int overridePerformanceLevel0(int original) {
        return ModOptions.isFramerateLimited() ? 2 : 0;
    }

    @ModifyExpressionValue(method = "onFrameUpdate", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/option/GameOptions;fpsLimit:I", ordinal = 3))
    public int nukeSleep(int original) {
        return -1;
    }

    @ModifyExpressionValue(method = "onFrameUpdate", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/option/GameOptions;fpsLimit:I", ordinal = 4))
    public int redirectMenuFpsLimit(int original) {
        return ModOptions.isFramerateLimited() ? 2 : 0;
    }
}
