package local.luke.power.title.mixin;

import local.luke.power.title.TitleFeatures;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void power_title$get(CallbackInfo ci) {
        TitleFeatures.minecraft = (Minecraft) (Object) this;
    }
}
