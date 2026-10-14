package local.luke.power.client_fixes.mixin.client.text;

import local.luke.power.chat.ChatBuffer;
import local.luke.power.ui.*;
import local.luke.power.client_fixes.mixinterface.TextFieldWidgetAccessor;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.CharacterUtils;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin extends DrawContext implements TextFieldWidgetAccessor {
    @Shadow public boolean focused, enabled;
    @Shadow private String text;
    @Shadow private int maxLength;
    @Shadow @Final private int x, y, width, height;
    @Shadow @Final private TextRenderer textRenderer;
    @Shadow private Screen parent;
    @Unique private final ChatBuffer power$editor = new ChatBuffer(Integer.MAX_VALUE);
    @Unique private final TextPointer power$pointer = new TextPointer(power$editor);

    @Unique private void power$sync() {
        power$editor.limit(maxLength == 0 ? Integer.MAX_VALUE : maxLength);
        power$editor.sync(text);
    }
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void power$key(char character, int key, CallbackInfo ci) {
        ci.cancel();
        if (!enabled || !focused) return;
        if (character == '\t') { if (parent != null) parent.handleTab(); return; }
        power$sync();
        TextEditing.key(power$editor, character, key, value -> CharacterUtils.VALID_CHARACTERS.indexOf(value) >= 0);
        text = power$editor.text();
    }
    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void power$click(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (!enabled || !focused || button != 0) return;
        power$sync();
        power$pointer.layout(TextDrag.screen(), x + 4, width - 8, textRenderer::getWidth);
        power$pointer.click(mouseX, () -> focused && enabled);
    }
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void power$render(CallbackInfo ci) {
        ci.cancel();
        power$sync();
        fill(x - 1, y - 1, x + width + 1, y + height + 1, focused ? 0xffaaaaaa : 0xff555555);
        fill(x, y, x + width, y + height, 0xff000000);
        var view = power$pointer.layout(TextDrag.screen(), x + 4, width - 8, textRenderer::getWidth);
        int top = y + (height - 8) / 2;
        int from = Math.max(view.start(), power$editor.start()), to = Math.min(view.end(), power$editor.end());
        if (focused && from < to)
            fill(x + 4 + textRenderer.getWidth(text.substring(view.start(), from)), top - 1,
                x + 4 + textRenderer.getWidth(text.substring(view.start(), to)), top + 9, 0xff335577);
        textRenderer.drawWithShadow(text.substring(view.start(), view.end()), x + 4, top, enabled ? 0xe0e0e0 : 0x707070);
        if (enabled && focused && InterfaceState.uiCursorVisible()) {
            int caret = x + 4 + textRenderer.getWidth(text.substring(view.start(), power$editor.cursor()));
            fill(caret, top - 1, caret + 1, top + 9, 0xffffffff);
        }
    }
    public String getDisplayText() { return text; }
}
