package local.luke.power.controls.mixin.tweaks.frontviewthirdperson;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.controls.ControlFeatures;
import local.luke.power.controls.tweaks.frontviewthirdperson.FrontViewMode;
import local.luke.power.controls.util.ModOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Shadow
    private Minecraft minecraft;
    
    @Unique
    public boolean isFrontViewEnabled() {
        return ModOptions.frontView || (ControlFeatures.USER_INTERFACE_CONFIG.frontViewThirdPerson == FrontViewMode.HIDE_HUD && (minecraft.options.thirdPerson && minecraft.options.hideHud));
    }
    
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V", ordinal = 2))
    public void re(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (ControlFeatures.USER_INTERFACE_CONFIG.frontViewThirdPerson == FrontViewMode.DISABLED) {
            original.call(instance, x, y, u, v, width, height);
            return;
        }
        
        if (!isFrontViewEnabled()) {
            original.call(instance, x, y, u, v, width, height);
        }
    }
}
