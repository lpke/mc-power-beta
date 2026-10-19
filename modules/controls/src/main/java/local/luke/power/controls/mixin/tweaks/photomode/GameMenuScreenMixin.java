package local.luke.power.controls.mixin.tweaks.photomode;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unchecked")
@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin extends Screen {
    @Inject(at = @At("RETURN"), method = "init")
    public void arrangeMenuButtons(CallbackInfo info) {
        ButtonWidget options = null;
        ButtonWidget statistics = null;
        ButtonWidget quit = null;
        for (Object entry : this.buttons.toArray()) {
            ButtonWidget button = (ButtonWidget) entry;
            if (button.id == 0) options = button;
            else if (button.id == 6) statistics = button;
            else if (button.id == 1) quit = button;
        }
        if (options == null) return;
        // Remove the empty vanilla row while retaining the normal button spacing.
        if (statistics != null) options.y = statistics.y + 24;
        if (quit != null) quit.y = options.y + 24;
    }
}
