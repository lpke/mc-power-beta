package local.luke.power.client_fixes.mixin.client.misc;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import local.luke.power.client_fixes.client.gui.CallbackButtonWidget;
import local.luke.power.client_fixes.mixin.client.ScreenAccessor;

@Mixin(Screen.class)
public class ScreenMixin {
    @Redirect(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;buttonClicked(Lnet/minecraft/client/gui/widget/ButtonWidget;)V"))
    private void onActionPerformed(Screen screen, ButtonWidget button) {
        if (button instanceof CallbackButtonWidget) {
            CallbackButtonWidget buttonWidget = (CallbackButtonWidget) button;
            buttonWidget.onPress();
            return;
        }
        ((ScreenAccessor) screen).callButtonClicked(button);
    }
}
