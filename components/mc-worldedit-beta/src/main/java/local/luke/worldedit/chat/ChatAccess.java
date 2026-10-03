package local.luke.worldedit.chat;

import java.lang.reflect.Field;
import local.luke.worldedit.WorldEditBeta;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;

/** Optional MojangFix text widget; field access avoids hard linking either mod. */
public final class ChatAccess {
  private static Field widget;
  private static Field suggestions;
  private static boolean searched;

  static {
    try {
      widget =
          Class.forName("pl.telvarost.mojangfixstationapi.client.text.chat.ChatScreenVariables")
              .getField("textField");
    } catch (ClassNotFoundException ignored) {
    } catch (ReflectiveOperationException e) {
      WorldEditBeta.LOG.warn("Could not access MojangFix chat", e);
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

  public static void clearSuggestions(ChatScreen screen) {
    // RetroCommands 0.5.10 consumes ordinary keys when several suggestions exist.
    // Only clear its stale selection state for our commands; Tab is handled below.
    if (!searched) {
      searched = true;
      for (Field f : ChatScreen.class.getDeclaredFields())
        if (f.getType() == String[].class
            && (f.getName().equals("suggestions") || f.getName().endsWith("$suggestions"))) {
          suggestions = f;
          suggestions.setAccessible(true);
          break;
        }
    }
    try {
      if (suggestions != null) suggestions.set(screen, new String[0]);
    } catch (IllegalAccessException e) {
      suggestions = null;
      WorldEditBeta.LOG.warn("Could not reset RetroCommands suggestions", e);
    }
  }
}
