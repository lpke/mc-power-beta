package local.luke.power.client_fixes.mixin.client.multiplayer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import local.luke.power.client_fixes.client.gui.multiplayer.MultiplayerScreen;

@Mixin({DisconnectedScreen.class, ConnectScreen.class})
public class ReturnToMainMenuMixin {
    @Redirect(method = "buttonClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V"))
    private void redirectSetScreen(Minecraft minecraft, Screen screen) {
        minecraft.setScreen(new MultiplayerScreen());
    }
}
