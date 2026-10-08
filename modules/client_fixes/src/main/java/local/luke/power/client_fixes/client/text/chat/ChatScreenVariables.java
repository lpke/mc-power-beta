package local.luke.power.client_fixes.client.text.chat;

import net.minecraft.client.gui.widget.TextFieldWidget;

import java.util.ArrayList;
import java.util.List;

public class ChatScreenVariables {
    public static final local.luke.power.chat.ChatBuffer editor = new local.luke.power.chat.ChatBuffer(100);
    public static java.util.function.BiPredicate<Character, Integer> completion = (character, key) -> false;
    public static Runnable edited = () -> {};
    public static TextFieldWidget textField;
    public static String initialMessage = "";
    public static int chatHistoryPosition;
    public static int chatCursorPosition;
    public static final List<String> CHAT_HISTORY = new ArrayList<>();
}
