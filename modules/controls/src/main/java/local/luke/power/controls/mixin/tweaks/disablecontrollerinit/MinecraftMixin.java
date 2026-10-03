package local.luke.power.controls.mixin.tweaks.disablecontrollerinit;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import local.luke.power.controls.ControlFeatures;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @WrapWithCondition(method = "init", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Controllers;create()V"))
    public boolean disableControllerInit() {
        return !ControlFeatures.GENERAL_CONFIG.disableControllerInit;
    }
}
