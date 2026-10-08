package local.luke.power.client_fixes.mixin.client.multiplayer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import local.luke.power.client_fixes.client.gui.multiplayer.MultiplayerScreen;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    @Redirect(method = "buttonClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V", ordinal = 2))
    private void onNewGuiMainMenu(Minecraft minecraft, Screen screen) {
        minecraft.setScreen(new MultiplayerScreen(this));
    }
}
