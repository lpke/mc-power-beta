package local.luke.power.controls.mixin.tweaks.ingameversiontext;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = InGameHud.class, priority = 900)
public class InGameHudMixin extends DrawContext {
    @ModifyConstant(method = "render", constant = @Constant(stringValue = "Minecraft Beta 1.7.3 ("), require = 0)
    public String changeVersionText(String constant) {
        if (ControlFeatures.USER_INTERFACE_CONFIG.versionTextConfig.enableCustomVersionText) {
            return ControlFeatures.USER_INTERFACE_CONFIG.versionTextConfig.customVersionText + " (";
        }
        return constant;
    }
}
