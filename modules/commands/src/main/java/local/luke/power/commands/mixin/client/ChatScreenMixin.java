package local.luke.power.commands.mixin.client;

import local.luke.power.commands.ClientCommands;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.integration.chat.ChatWidgetAccess;
import local.luke.power.commands.util.RetroChatUtil;
import local.luke.power.commands.util.SharedCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import local.luke.power.chat.ChatBuffer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static local.luke.power.commands.ClientCommands.*;

@Mixin(value = ChatScreen.class, priority = 1100)
public abstract class ChatScreenMixin extends Screen {
    @Shadow protected String text;
    @Shadow private int focusedTicks;

    @Unique private final ChatBuffer power$fallback = new ChatBuffer(100);
    @Unique private int power$viewStart;
    @Unique private String suggestionInput = "";
    @Unique private String[] suggestions = new String[0];
    @Unique private int chosen = 0;
    @Unique private int textWidthPixels = 0;
    @Unique private int textWidthPixelsBeforeCurrentWord = 0;
    @Unique private String currentWord = "";

    @Unique
    void setText(String s) {
        if (chatWidgetsAvailable) {
            ChatWidgetAccess.setText(s);
        }

        if (text != null)
            text = s;
    }

    @Unique
    String getText() {
        String result;
        if (chatWidgetsAvailable) {
            result = ChatWidgetAccess.getText();
        } else {
            result = text;
        }

        if (result == null)
            result = "";

        return result;
    }

    @Unique private ChatBuffer power$editor() {
        if (chatWidgetsAvailable) return ChatWidgetAccess.editor();
        power$fallback.sync(text == null ? "" : text); return power$fallback;
    }
    @Unique private String power$input() {
        var editor = power$editor(); return editor.text().substring(0, editor.cursor());
    }
    @Inject(method = "init", at = @At("RETURN"))
    private void power$connect(CallbackInfo ci) {
        if (chatWidgetsAvailable) {
            ChatWidgetAccess.onInput(this::power$complete);
            ChatWidgetAccess.onChange(this::refreshSuggestions);
        }
    }
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void keyPressedInit(char character, int key, CallbackInfo ci) {
        if (!chatWidgetsAvailable && power$complete(character, key)) ci.cancel();
    }
    @Unique private boolean power$complete(char character, int key) {
        if (chatWidgetsAvailable) ChatWidgetAccess.onChange(this::refreshSuggestions);
        if (!power$input().equals(suggestionInput)) refreshSuggestions();
        var editor = power$editor();
        if (key == Keyboard.KEY_TAB && suggestions.length > 0 && !editor.selected()) {
            ensureChosenIsInRange();
            String token = power$completionToken(suggestions[chosen]);
            if (chatWidgetsAvailable) { ChatWidgetAccess.completeToken(token); text = getText(); }
            else { editor.completeToken(token); setText(editor.text()); }
            resetValues(); suggestionInput = power$input(); return true;
        }
        boolean ctrl = local.luke.power.ui.TextEditing.control();
        if (!ctrl && !editor.browsing() && !editor.selected() && suggestions.length > 1
                && (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN)) {
            adjustChosenSuggestion(key); return true;
        }
        return false;
    }

    @Unique private String power$completionToken(String suffix) {
        String input = power$input();
        int start = input.lastIndexOf(' ') + 1;
        String completed = suffix;
        if (start == 0) {
            for (Command command : RetroChatUtil.commands)
                if (("/" + command.name()).equalsIgnoreCase(completed)) return "/" + command.name();
            for (String command : vanillaNoOPCommands)
                if (("/" + command).equalsIgnoreCase(completed)) return "/" + command;
            return completed;
        }
        String[] words = input.split(" ");
        int parameter = input.endsWith(" ") ? words.length : words.length - 1;
        String name = words[0].substring(1);
        Command command = RetroChatUtil.commands.stream().filter(c -> c.name().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (command == null) return completed;
        // Query full choices only on Tab. Keep native player and saved-name spelling.
        String prefix = command.name().equals("give") && parameter == 2 && completed.contains(":")
            ? completed.substring(0, completed.indexOf(':') + 1).toLowerCase(java.util.Locale.ROOT) : "";
        try {
            SharedCommandSource source = new SharedCommandSource(minecraft.player);
            for (String choice : command.suggestion(source, parameter, prefix, input.substring(0, start) + prefix)) {
                String token = prefix + choice;
                if (token.equalsIgnoreCase(completed)) return token;
            }
        } catch (RuntimeException ignored) {}
        return completed;
    }

    @Inject(method = "keyPressed", at = @At("TAIL"))
    private void processInput(char character, int key, CallbackInfo ci) { refreshSuggestions(); }

    @Unique
    private void refreshSuggestions() {
        resetValues();
        suggestionInput = power$input();

        if (power$input().isEmpty()) {
            return;
        }

        String[] sections = power$input().split(" ");
        if (sections.length == 0) {
            return;
        }

        currentWord = power$input().endsWith(" ") ? "" : sections[sections.length - 1];
        fetchSuggestionsForCurrentWord(sections);

        if (power$input().endsWith(" "))
            currentWord = "";

        if (suggestions.length > 0) {
            calculateTextWidthPixelsBeforeCurrentWord(sections);
        }
    }

    @Unique
    private void resetValues() {
        currentWord = "";
        suggestions = new String[0];
        chosen = 0;
        textWidthPixelsBeforeCurrentWord = 0;
        textWidthPixels = this.textRenderer.getWidth("> " + power$input());
    }

    @Unique
    private static final List<String> vanillaNoOPCommands = Collections.unmodifiableList(
            new ArrayList<String>() {{
                add("me");
                add("kill");
                add("tell");
            }});

    @Unique
    private void fetchSuggestionsForCurrentWord(String[] sections) {
        try {
            if (!power$input().startsWith("/")) return;
            List<String> choices = new ArrayList<>();
            if (sections.length == 1 && !power$input().endsWith(" ")) {
                if (minecraft.world.isRemote && !ClientCommands.mp_rc) {
                    vanillaNoOPCommands.forEach(name -> choices.add("/" + name));
                } else {
                    SharedCommandSource source = new SharedCommandSource(minecraft.player);
                    RetroChatUtil.commands.stream()
                        .filter(c -> !ClientCommands.disabled_commands.contains(c.name()))
                        .filter(c -> local.luke.power.commands.util.CommandAccess.available(c, source))
                        .forEach(c -> choices.add("/" + c.name()));
                }
            } else {
                Command command = RetroChatUtil.commands.stream()
                    .filter(c -> c.name().equalsIgnoreCase(sections[0].substring(1)))
                    .filter(c -> local.luke.power.commands.util.CommandAccess.available(c, new SharedCommandSource(minecraft.player)))
                    .findFirst().orElse(null);
                if (command != null && (!command.disableInSingleplayer() || minecraft.world.isRemote)) {
                    int parameter = power$input().endsWith(" ") ? sections.length : sections.length - 1;
                    // Keep structured block-state completion contextual. Plain item names
                    // ask for full choices so fuzzy matches can replace the entire token.
                    String prefix = currentWord.contains("[") ? currentWord
                        : currentWord.contains(":") ? currentWord.substring(0, currentWord.indexOf(':') + 1) : "";
                    String before = power$input().substring(0, power$input().length() - currentWord.length());
                    for (String suffix : command.suggestion(new SharedCommandSource(minecraft.player), parameter, prefix, before + prefix))
                        choices.add(prefix + suffix);
                }
            }
            suggestions = local.luke.power.chat.ChatCompletion.ranked(currentWord, choices);
            textWidthPixels = this.textRenderer.getWidth("> " + power$input());
        } catch (RuntimeException ignored) {
            suggestions = new String[0];
        }
    }

    @Unique
    private boolean adjustChosenSuggestion(int par2) {
        int initial = chosen;
        if (par2 == 200) { chosen++; }
        if (par2 == 208) { chosen--; }
        if (chosen < 0) { chosen = suggestions.length - 1; }
        if (chosen >= suggestions.length) { chosen = 0; }
        return initial != chosen;
    }

    @Unique
    private void calculateTextWidthPixelsBeforeCurrentWord(String[] sections) {
        for (int j = 0; j < sections.length - 1; j++) {
            textWidthPixelsBeforeCurrentWord += textRenderer.getWidth(sections[j] + " ");
        }
    }

    @Unique
    boolean tryMatch(String s) {
        try {
            AtomicBoolean valid = new AtomicBoolean(false);
            RetroChatUtil.commands.stream().forEach(a -> {
                if (a.name().equalsIgnoreCase(s) && local.luke.power.commands.util.CommandAccess.available(a, new SharedCommandSource(minecraft.player))) {
                    if (!minecraft.world.isRemote) {
                        valid.set(true);
                    } else {
                        if (mp_op) {
                            valid.set(true);
                        } else {
                            if (!a.needsPermissions()) {
                                valid.set(true);
                            }
                        }
                    }
                }
            });
            if (valid.get()) {
                 return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void replace(int mouseX, int mouseY, float delta, CallbackInfo ci) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        try {
            var editor = power$editor();
            if (chatWidgetsAvailable) ChatWidgetAccess.onChange(this::refreshSuggestions);
            if (!power$input().equals(suggestionInput)) refreshSuggestions();
            fill(2, height - 14, width - 2, height - 2, Integer.MIN_VALUE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            int left = 4 + textRenderer.getWidth("> ");
            var view = editor.view(Math.max(1, width - left - 4), textRenderer::getWidth);
            power$viewStart = view.start();
            String visible = editor.text().substring(view.start(), view.end());
            int a = Math.max(view.start(), Math.min(view.end(), editor.start()));
            int b = Math.max(view.start(), Math.min(view.end(), editor.end()));
            if (a != b) local.luke.power.ui.TextSelection.background(left + textRenderer.getWidth(editor.text().substring(view.start(), a)), height - 13,
                    left + textRenderer.getWidth(editor.text().substring(view.start(), b)), height - 4);
            drawTextWithShadow(textRenderer, "> ", 4, height - 12, 0xE0E0E0);
            boolean invalid = editor.text().startsWith("/") && !tryMatch(editor.text().split(" ")[0].substring(1));
            drawTextWithShadow(textRenderer, visible, left, height - 12, invalid ? 0xFC5454 : 0xE0E0E0);
            if (!editor.selected()) renderSuggestions(mouseX, mouseY, delta);
            if (local.luke.power.ui.InterfaceState.cursorVisible(focusedTicks)) {
                int x = left + textRenderer.getWidth(editor.text().substring(view.start(), editor.cursor()));
                fill(x, height - 13, x + 1, height - 3, 0xFFE0E0E0);
            }
            super.render(mouseX, mouseY, delta);
        } finally { GL11.glPopAttrib(); }
        ci.cancel();
    }

    @Unique
    public void renderSuggestions(int mouseX, int mouseY, float delta) {
        try {
            if (suggestions.length > 0) {
                ensureChosenIsInRange();
                renderChosenSuggestion();
                renderMultipleSuggestions();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Unique
    private void ensureChosenIsInRange() {
        if (chosen < 0 || chosen >= suggestions.length) {
            chosen = 0;
        }
    }

    @Unique
    private void renderChosenSuggestion() {
        var editor = power$editor();
        if (editor.cursor() != editor.text().length()) return;
        int x = 4 + textRenderer.getWidth("> " + editor.text().substring(power$viewStart));
        String choice = suggestions[chosen];
        if (!local.luke.power.chat.ChatCompletion.startsWith(choice, currentWord)) return;
        String ghost = choice.substring(currentWord.length());
        while (!ghost.isEmpty() && x + textRenderer.getWidth(ghost) > width - 4) ghost = ghost.substring(0, ghost.length() - 1);
        drawTextWithShadow(textRenderer, ghost, x, height - 12, 0xAAAAAA);
    }

    @Unique
    private void renderMultipleSuggestions() {
        int listWidth = Math.min(width - 8, getMaxSuggestionWidth() + 4);
        int wordStart = Math.max(power$viewStart, power$input().length() - currentWord.length());
        int x = Math.min(width - 4 - listWidth, 4 + textRenderer.getWidth("> " + power$input().substring(power$viewStart, wordStart)));
        int count = Math.min(suggestions.length, Math.max(1, Math.min(10, (height - 20) / 10)));
        int from = Math.max(0, Math.min(chosen - count + 1, suggestions.length - count));
        fill(x, height - 14 - 10 * count, x + listWidth, height - 14, 0xFF000000);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        for (int i = from; i < from + count; i++) {
            String label = suggestions[i];
            while (!label.isEmpty() && textRenderer.getWidth(label) > listWidth) label = label.substring(0, label.length() - 1);
            drawTextWithShadow(textRenderer, label, x, height - 12 - 10 * (i - from + 1), i == chosen ? 0xFCFC00 : 0xFFFFFF);
        }
    }
    @Unique private int getMaxSuggestionWidth() {
        int width = 0;
        for (String suggestion : suggestions) width = Math.max(width, textRenderer.getWidth(suggestion));
        return width;
    }
}
