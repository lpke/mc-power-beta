package local.luke.power.controls.mixin.tweaks.renderdistance;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.controls.ControlFeatures;
import local.luke.power.controls.util.ModOptions;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    private float viewDistance;

    @Inject(
            method = "renderWorld",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.PUTFIELD,
                    target = "Lnet/minecraft/client/render/GameRenderer;viewDistance:F",
                    shift = At.Shift.AFTER
            )
    )
    public void overrideFarPlaneDistance(float i, int par2, CallbackInfo ci) {
        this.viewDistance = ModOptions.getGameRendererChunks() * 16;
    }

    @WrapOperation(method = "renderFrame", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/option/GameOptions;viewDistance:I"))
    public int skyFog(GameOptions instance, Operation<Integer> original) {
        return ModOptions.getRenderDistanceChunks() > 7 ? 0 : 3;
    }


    @Unique
    public float originalViewDistance = 0F;

    @Inject(method = "applyFog", at = @At(value = "HEAD"))
    public void injectViewDistance(int tickDelta, float par2, CallbackInfo ci) {
        originalViewDistance = this.viewDistance;
        this.viewDistance = this.viewDistance * ModOptions.getFogMultiplier();
    }

    @Inject(method = "applyFog", at = @At(value = "TAIL"))
    public void returnOriginalViewDistance(int tickDelta, float par2, CallbackInfo ci) {
        this.viewDistance = originalViewDistance;
    }
}
