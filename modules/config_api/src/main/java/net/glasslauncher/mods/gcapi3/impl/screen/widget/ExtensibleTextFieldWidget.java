package net.glasslauncher.mods.gcapi3.impl.screen.widget;

import local.luke.power.chat.ChatBuffer;
import local.luke.power.ui.*;
import net.glasslauncher.mods.gcapi3.api.CharacterUtils;
import net.glasslauncher.mods.gcapi3.api.HasDrawable;
import net.glasslauncher.mods.gcapi3.api.HasToolTip;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/** Config inputs share the pack editor while retaining the public gcapi3 widget API. */
public class ExtensibleTextFieldWidget extends DrawContext implements HasDrawable, HasToolTip {
    private final TextRenderer textRenderer;
    private final ChatBuffer editor = new ChatBuffer(32);
    private final TextPointer pointer = new TextPointer(editor);
    public int x, y, width, height;
    private int maxLength = 32;
    private boolean shouldDrawBackground = true, enabled = true, selected;
    public int selectedTextColour = 14737632, deselectedTextColour = 7368816;
    public int errorBorderColour = 0xffc83232;
    private Function<String, List<String>> contentsValidator;
    private Runnable textUpdatedListener;

    public ExtensibleTextFieldWidget(TextRenderer textRenderer) { this.textRenderer = textRenderer; }
    public String getText() { return editor.text(); }
    public int getMaxLength() { return maxLength; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getCursorMax() { return editor.cursor() == editor.start() ? editor.end() : editor.start(); }
    public int getCursorMin() { return editor.cursor(); }
    public void setTextUpdatedListener(Runnable listener) { textUpdatedListener = listener; }
    public boolean isValueValid() { return contentsValidator == null || contentsValidator.apply(getText()) == null; }
    public void tick() {}
    public void setText(String value) { editor.set(value); }
    public String getSelectedText() { return editor.selection(); }
    private void changed(String before) {
        if (!before.equals(getText()) && textUpdatedListener != null) textUpdatedListener.run();
    }
    public void addText(String value) {
        String before = getText();
        if (value.isEmpty()) { if (editor.selected()) editor.delete(-1, false); }
        else editor.write(value, c -> CharacterUtils.isCharacterValid((char)c));
        changed(before);
    }
    public void removeWord(int count) {
        String before = getText();
        if (!editor.selected()) for (int i = 0; i < Math.abs(count); i++) editor.move(Integer.signum(count), true, true);
        if (editor.selected()) editor.delete(-1, false);
        changed(before);
    }
    public void removeRelativeToCursor(int count) {
        String before = getText();
        if (!editor.selected()) editor.position(editor.cursor() + count, true);
        if (editor.selected()) editor.delete(-1, false);
        changed(before);
    }
    public int getWords(int count) { return getWords(count, getCursorMax()); }
    public int getWords(int count, int position) {
        ChatBuffer probe = new ChatBuffer(maxLength); probe.set(getText()); probe.position(position, false);
        for (int i = 0; i < Math.abs(count); i++) probe.move(Integer.signum(count), false, true);
        return probe.cursor();
    }
    public void updateOffsetCursorMax(int delta) { updateCursorMax(getCursorMin() + delta); }
    public void updateCursorMax(int position) { editor.position(position, false); }
    public void updateCursorMax() { updateCursorMax(0); }
    public void onTextChanged() { updateCursorMax(getText().length()); }
    public void updateCursorPosition(int position) { editor.position(position, true); }
    @Override public void keyPressed(char c, int key) {
        if (!enabled || !selected) return;
        String before = getText();
        TextEditing.key(editor, c, key, value -> CharacterUtils.isCharacterValid((char)value));
        changed(before);
    }
    @Override public void setID(int id) {}
    @Override public void mouseClicked(int mouseX, int mouseY, int button) {
        setSelected(enabled && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height);
        if (selected && button == 0) {
            pointer.layout(TextDrag.screen(), textX(), getBackgroundOffset(), textRenderer::getWidth);
            pointer.click(mouseX, () -> selected && enabled);
        }
    }
    @Override public void setXYWH(int x, int y, int width, int height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }
    @Override public int[] getXYWH() { return new int[]{x, y, width, height}; }
    @Override public List<String> getTooltip() {
        if (!enabled) return Collections.singletonList("Server synced, you cannot change this value");
        return contentsValidator == null ? null : contentsValidator.apply(getText());
    }
    private int textX() { return x + (shouldDrawBackground ? 4 : 0); }
    @Override public void draw(int mouseX, int mouseY) {
        if (shouldDrawBackground) {
            fill(x - 1, y - 1, x + width + 1, y + height + 1,
                !isValueValid() ? errorBorderColour : !enabled ? 0xffffca00 : selected ? 0xffaaaaaa : 0xff555555);
            fill(x, y, x + width, y + height, 0xff000000);
        }
        int left = textX(), top = shouldDrawBackground ? y + (height - 8) / 2 : y;
        var view = pointer.layout(TextDrag.screen(), left, getBackgroundOffset(), textRenderer::getWidth);
        String value = getText();
        int from = Math.max(view.start(), editor.start()), to = Math.min(view.end(), editor.end());
        if (selected && from < to)
            fill(left + textRenderer.getWidth(value.substring(view.start(), from)), top - 1,
                left + textRenderer.getWidth(value.substring(view.start(), to)), top + 9, 0xff335577);
        textRenderer.drawWithShadow(value.substring(view.start(), view.end()), left, top,
            !enabled ? 0xaa8b15 : selected ? selectedTextColour : deselectedTextColour);
        if (selected && enabled && InterfaceState.uiCursorVisible()) {
            int caret = left + textRenderer.getWidth(value.substring(view.start(), editor.cursor()));
            fill(caret, top - 1, caret + 1, top + 9, 0xffffffff);
        }
    }
    public void setMaxLength(int length) {
        maxLength = Math.max(0, length); editor.limit(maxLength);
        if (getText().length() > maxLength) editor.set(getText());
    }
    public boolean shouldDrawBackground() { return shouldDrawBackground; }
    public void setShouldDrawBackground(boolean value) { shouldDrawBackground = value; }
    public void setSelected(boolean selected) { this.selected = selected; }
    public boolean isSelected() { return selected; }
    public int getBackgroundOffset() { return width - (shouldDrawBackground ? 8 : 0); }
    public void setValidator(Function<String, List<String>> validator) { contentsValidator = validator; }
}
