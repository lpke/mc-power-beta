package local.luke.power.client_fixes.mixin.client.text.sign;

import local.luke.power.client_fixes.mixinterface.SignBlockEntityAccessor;
import net.glasslauncher.mods.gcapi3.api.HasDrawable;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Arrays;

@Mixin(SignEditScreen.class)
public abstract class SignEditScreenMixin extends Screen {
    @Shadow private SignBlockEntity sign;
    @Shadow private int currentRow;
    @Shadow protected String title;
    @Unique private TextFieldWidget[] power$fields() { return ((SignBlockEntityAccessor)sign).getTextFields(); }
    @Unique private int power$top() { return height / 2 - 48; }
    @Inject(method = "init", at = @At("RETURN"))
    private void power$init(CallbackInfo ci) {
        var fields = power$fields();
        for (int i = 0; i < fields.length; i++) {
            ((HasDrawable)fields[i]).setXYWH(width / 2 - 100, power$top() + i * 22, 200, 18);
            fields[i].setFocused(i == currentRow);
        }
        ((net.minecraft.client.gui.widget.ButtonWidget)buttons.get(0)).y = power$top() + 96;
    }
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void power$key(char character, int key, CallbackInfo ci) {
        ci.cancel();
        if (key == Keyboard.KEY_ESCAPE) { sign.markDirty(); minecraft.setScreen(null); return; }
        if (key == Keyboard.KEY_UP) currentRow = currentRow - 1 & 3;
        else if (key == Keyboard.KEY_DOWN || key == Keyboard.KEY_RETURN || key == Keyboard.KEY_TAB)
            currentRow = currentRow + (key == Keyboard.KEY_TAB && local.luke.power.ui.TextEditing.shift() ? -1 : 1) & 3;
        else power$fields()[currentRow].keyPressed(character, key);
        for (int i = 0; i < 4; i++) {
            power$fields()[i].setFocused(i == currentRow);
            sign.texts[i] = power$fields()[i].getText();
        }
    }
    @Override protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        var fields = power$fields();
        for (int i = 0; i < fields.length; i++) {
            fields[i].mouseClicked(x, y, button);
            if (fields[i].focused) currentRow = i;
        }
    }
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void power$render(int x, int y, float delta, CallbackInfo ci) {
        ci.cancel();
        renderBackground();
        drawCenteredTextWithShadow(textRenderer, title, width / 2, power$top() - 24, 0xffffff);
        for (TextFieldWidget field : power$fields()) field.render();
        super.render(x, y, delta);
    }
    @Inject(method = "removed", at = @At("RETURN"))
    private void power$removed(CallbackInfo ci) {
        for (TextFieldWidget field : power$fields()) field.setFocused(false);
    }
    @Redirect(method = "removed", at = @At(value = "FIELD", target = "Lnet/minecraft/block/entity/SignBlockEntity;texts:[Ljava/lang/String;"))
    private String[] power$text(SignBlockEntity sign) {
        return Arrays.stream(power$fields()).map(TextFieldWidget::getText).toArray(String[]::new);
    }
}
