package local.luke.power.worldedit.chat;

import java.lang.reflect.Field;
import local.luke.power.worldedit.WorldEditor;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;

/** Shared chat widget access across module mapping versions. */
public final class ChatAccess {
  private static Field widget;

  static {
    try {
      widget =
          Class.forName("local.luke.power.client_fixes.client.text.chat.ChatScreenVariables")
              .getField("textField");
    } catch (ClassNotFoundException ignored) {
    } catch (ReflectiveOperationException e) {
      WorldEditor.LOG.warn("Could not access the chat widget", e);
    }
  }

  private static TextFieldWidget widget() {
    try {
      return widget == null ? null : (TextFieldWidget) widget.get(null);
    } catch (IllegalAccessException e) {
      return null;
    }
  }

  public static boolean nativeHistory() {
    return widget() != null;
  }

  public static String text(String fallback) {
    TextFieldWidget w = widget();
    return w == null ? fallback : w.getText();
  }

  public static void setText(String value) {
    TextFieldWidget w = widget();
    if (w != null) w.setText(value);
  }

}
