package local.luke.power.mechanics.mixin.client;

import local.luke.power.mechanics.Config;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(SignEditScreen.class)
public class EditSignMixin extends Screen {

    @Shadow private int currentRow;

    @Shadow private SignBlockEntity sign;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void powerMechanics_finishOnEscape(char character, int keyCode, CallbackInfo ci) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            // Match Done; normal screen removal also sends multiplayer edits and resets key repeat.
            this.sign.markDirty();
            this.minecraft.setScreen(null);
            ci.cancel();
        }
    }

    @ModifyConstant(method = "keyPressed", constant = @Constant(intValue = 15))
    protected int powerMechanics_keyPressed(int value) {
        if (!Config.config.INTERACTIVE_BLOCK_CONFIG.enableColorSignsWithDye) {
            return 15;
        } else {
            int lineLimit = 15;

            /** - Allow first instance of a section character with its modifier */
            if (this.sign.texts[this.currentRow].contains("§")) {
                lineLimit = lineLimit + 2;
            }

            return lineLimit;
        }
    }
}
