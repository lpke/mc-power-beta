package local.luke.power.client_fixes.mixin.client.text.sign;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import local.luke.power.client_fixes.ModHelper;
import local.luke.power.client_fixes.mixinterface.SignBlockEntityAccessor;

import java.util.Arrays;

@Mixin(SignEditScreen.class)
public class SignEditScreenMixin {
    @Shadow
    private SignBlockEntity sign;

    @Shadow
    private int currentRow;

    @Shadow
    private int ticksSinceOpened;

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        TextFieldWidget[] textFields = ((SignBlockEntityAccessor) this.sign).getTextFields();
        textFields[0].setFocused(true);
    }

    @Inject(method = "keyPressed", at = @At(value = "JUMP", opcode = Opcodes.IF_ICMPNE, ordinal = 2), cancellable = true)
    private void onKeyPressed(char character, int keyCode, CallbackInfo ci) {
        TextFieldWidget[] textFields = ((SignBlockEntityAccessor) this.sign).getTextFields();
        for (TextFieldWidget textField : textFields) {
            textField.setFocused(false);
        }
        textFields[this.currentRow].setFocused(true);
        textFields[this.currentRow].keyPressed(character, keyCode);
        this.sign.texts[this.currentRow] = textFields[this.currentRow].getText();
        ci.cancel();
    }

    @Inject(method = "removed", at = @At("RETURN"))
    private void onRemoved(CallbackInfo ci) {
        TextFieldWidget[] textFields = ((SignBlockEntityAccessor) this.sign).getTextFields();
        for (TextFieldWidget textField : textFields) {
            textField.setFocused(false);
        }
    }

    @Redirect(method = "removed", at = @At(value = "FIELD", target = "Lnet/minecraft/block/entity/SignBlockEntity;texts:[Ljava/lang/String;"))
    private String[] getSignText(SignBlockEntity sign) {
        return Arrays.stream(((SignBlockEntityAccessor) sign).getTextFields()).map(TextFieldWidget::getText).toArray(String[]::new);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        ((SignBlockEntityAccessor) this.sign).getTextFields()[this.currentRow].tick();
        ticksSinceOpened = 6;
        ci.cancel();
    }
}