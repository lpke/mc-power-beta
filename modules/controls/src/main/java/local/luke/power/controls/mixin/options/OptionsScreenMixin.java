package local.luke.power.controls.mixin.options;

import local.luke.power.controls.ControlFeatures;
import local.luke.power.controls.tweaks.controls.ControlsScreen;
import local.luke.power.controls.util.ModOptions;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Option;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {

    @Shadow
    private static Option[] RENDER_OPTIONS;

    @Shadow
    private GameOptions options;

    static {
        RENDER_OPTIONS = Arrays.copyOf(RENDER_OPTIONS, RENDER_OPTIONS.length + 1);
        OptionsScreenMixin.RENDER_OPTIONS[OptionsScreenMixin.RENDER_OPTIONS.length - 1] = ModOptions.fovOption;
    }

    @Inject(method = "buttonClicked", at = @At("HEAD"), cancellable = true)
    public void openImprovedControls(ButtonWidget button, CallbackInfo ci) {
        if (ControlFeatures.USER_INTERFACE_CONFIG.improvedControlsMenu) {
            if (button.id == 100) {
                this.minecraft.setScreen(new ControlsScreen(this, this.options));
                ci.cancel();
            }
        }
    }
}
