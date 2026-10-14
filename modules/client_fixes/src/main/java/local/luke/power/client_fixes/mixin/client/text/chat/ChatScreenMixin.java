package local.luke.power.client_fixes.mixin.client.text.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.CharacterUtils;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import local.luke.power.client_fixes.client.ClientFixesClientMod;
import local.luke.power.client_fixes.mixinterface.ChatScreenAccessor;
import static local.luke.power.client_fixes.client.text.chat.ChatScreenVariables.*;

@Mixin(ChatScreen.class)
public class ChatScreenMixin extends Screen implements ChatScreenAccessor {
    @Shadow protected String text;
    @Unique private final local.luke.power.ui.TextPointer power$pointer = new local.luke.power.ui.TextPointer(editor);

    public ChatScreen setInitialMessage(String message) {
        initialMessage = message;
        return (ChatScreen) (Object) this;
    }
    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        text = initialMessage;
        initialMessage = "";
        textField = new TextFieldWidget(this, textRenderer, 2, height - 14, width - 2, height - 2, text);
        textField.setFocused(true);
        textField.setMaxLength(100);
        editor.reset(text);
        chatHistoryPosition = chatCursorPosition = 0;
    }
    @Unique private void power$sync() {
        textField.setText(editor.text()); text = editor.text();
        chatCursorPosition = editor.cursor() - text.length();
        edited.run();
    }
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void power$edit(char character, int key, CallbackInfo ci) {
        editor.sync(textField.getText());
        if (completion.test(character, key)) { ci.cancel(); return; }
        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            text = editor.text();
            String message = text.trim();
            if (!message.isEmpty() && (CHAT_HISTORY.isEmpty() || !CHAT_HISTORY.get(CHAT_HISTORY.size() - 1).equals(message))) {
                CHAT_HISTORY.add(message);
                if (CHAT_HISTORY.size() > 100) CHAT_HISTORY.remove(0);
            }
            if (key == Keyboard.KEY_NUMPADENTER) {
                if (!message.isEmpty() && !minecraft.isCommand(message)) minecraft.player.sendChatMessage(message);
                if (minecraft.currentScreen == (Object)this) minecraft.setScreen(null);
                else ClientFixesClientMod.cancelSetScreenNull = false;
                ci.cancel();
            }
            return;
        }
        if (key == Keyboard.KEY_ESCAPE) return;
        if (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN) editor.history(CHAT_HISTORY, key == Keyboard.KEY_UP);
        else local.luke.power.ui.TextEditing.key(editor, character, key, ctrl, shift,
            c -> CharacterUtils.VALID_CHARACTERS.indexOf(c) >= 0);
        power$sync(); ci.cancel();
    }
    @Redirect(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V", ordinal = 1))
    private void onSetScreen(Minecraft instance, Screen screen) {
        if (instance.currentScreen != (Object)this) {
            // Commands may open a screen. Closing the submitted chat must keep it.
            ClientFixesClientMod.cancelSetScreenNull = false;
        } else if (ClientFixesClientMod.cancelSetScreenNull) {
            ClientFixesClientMod.cancelSetScreenNull = false;
            editor.reset(""); power$sync();
        } else instance.setScreen(screen);
    }
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void power$click(int x, int y, int button, CallbackInfo ci) {
        if (button == 0 && y >= height - 14 && y < height - 2) {
            editor.sync(textField.getText());
            int left = 4 + textRenderer.getWidth("> ");
            power$pointer.layout(minecraft.currentScreen, left, Math.max(1, width - left - 4), textRenderer::getWidth);
            power$pointer.click(x, () -> minecraft.currentScreen == (Object)this);
            power$sync(); ci.cancel();
        }
    }
    @Override public void onMouseEvent() {
        int cursor = editor.cursor(), start = editor.start(), end = editor.end();
        super.onMouseEvent();
        if (cursor != editor.cursor() || start != editor.start() || end != editor.end()) power$sync();
    }
}
