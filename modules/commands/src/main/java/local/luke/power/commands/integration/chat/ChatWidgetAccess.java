package local.luke.power.commands.integration.chat;

import local.luke.power.client_fixes.client.text.chat.ChatScreenVariables;

import static local.luke.power.commands.ClientCommands.chatWidgetsAvailable;

public class ChatWidgetAccess {
    public static Integer getCursorPosition() {
        try {
            return ChatScreenVariables.chatCursorPosition;
        } catch (Exception e) {
            chatWidgetsAvailable = false;
            return 0;
        }
    }

    public static String getText() {
        try {
            return ChatScreenVariables.textField.getText();
        } catch (Exception e) {
            chatWidgetsAvailable = false;
            return "";
        }
    }

    public static void setText(String s) {
        if (ChatScreenVariables.textField != null)
            ChatScreenVariables.textField.setText(s);
    }
}
