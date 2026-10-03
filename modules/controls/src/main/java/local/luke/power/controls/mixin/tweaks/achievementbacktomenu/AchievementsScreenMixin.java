package local.luke.power.controls.mixin.tweaks.achievementbacktomenu;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.client.gui.screen.AchievementsScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AchievementsScreen.class)
public class AchievementsScreenMixin extends Screen {
    @Inject(method = "buttonClicked", at = @At(value = "HEAD"), cancellable = true)
    public void redirectToGameMenu(ButtonWidget button, CallbackInfo ci) {
        if (ControlFeatures.USER_INTERFACE_CONFIG.achievementBackToMenu) {
            if (button.id == 1) {
                this.minecraft.setScreen(new GameMenuScreen());
                ci.cancel();
            }
        }
    }
}
