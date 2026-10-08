package local.luke.power.commands.integration.chat;

import local.luke.power.chat.ChatBuffer;
import local.luke.power.client_fixes.client.text.chat.ChatScreenVariables;

/** Both chat modules edit the same buffer; the widget remains a compatibility bridge. */
public final class ChatWidgetAccess {
    public static ChatBuffer editor() {
        ChatBuffer editor = ChatScreenVariables.editor;
        if (ChatScreenVariables.textField != null) editor.sync(ChatScreenVariables.textField.getText());
        return editor;
    }
    public static int getCursorPosition() { return editor().cursor() - editor().text().length(); }
    public static String getText() { return editor().text(); }
    public static void onInput(java.util.function.BiPredicate<Character, Integer> callback) { ChatScreenVariables.completion = callback; }
    public static void onChange(Runnable callback) { ChatScreenVariables.edited = callback; }
    public static void setText(String text) {
        editor().set(text); sync();
    }
    public static void complete(String suffix) { editor().complete(suffix); sync(); }
    private static void sync() {
        var editor = ChatScreenVariables.editor;
        if (ChatScreenVariables.textField != null) ChatScreenVariables.textField.setText(editor.text());
        ChatScreenVariables.chatCursorPosition = editor.cursor() - editor.text().length();
    }
}
