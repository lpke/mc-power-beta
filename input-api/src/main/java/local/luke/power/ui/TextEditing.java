package local.luke.power.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.util.function.IntPredicate;
import local.luke.power.chat.ChatBuffer;
import org.lwjgl.input.Keyboard;

/** Common keyboard editing and clipboard handling, independent of Minecraft mappings. */
public final class TextEditing {
  private TextEditing() {}
  public static boolean shift() {
    return Keyboard.isCreated() && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
  }
  public static boolean control() {
    return Keyboard.isCreated() && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL));
  }
  public static void key(ChatBuffer editor, char character, int key, IntPredicate allowed) {
    key(editor, character, key, control(), shift(), allowed);
  }
  public static void key(ChatBuffer editor, char character, int key, boolean ctrl, boolean shift, IntPredicate allowed) {
    if (ctrl && key == Keyboard.KEY_A) editor.all();
    else if (ctrl && (key == Keyboard.KEY_C || key == Keyboard.KEY_X)) {
      if (editor.selected() && copy(editor.selection()) && key == Keyboard.KEY_X) editor.delete(-1, false);
    } else if (ctrl && key == Keyboard.KEY_V) editor.write(paste(), allowed);
    else if (key == Keyboard.KEY_LEFT || key == Keyboard.KEY_RIGHT) editor.move(key == Keyboard.KEY_LEFT ? -1 : 1, shift, ctrl);
    else if (key == Keyboard.KEY_HOME || key == Keyboard.KEY_END) editor.position(key == Keyboard.KEY_HOME ? 0 : editor.text().length(), shift);
    else if (key == Keyboard.KEY_BACK || key == Keyboard.KEY_DELETE) editor.delete(key == Keyboard.KEY_BACK ? -1 : 1, ctrl);
    else if (!ctrl) editor.write(String.valueOf(character), allowed);
  }
  public static boolean copy(String value) {
    try { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null); return true; }
    catch (RuntimeException e) { return false; }
  }
  private static String paste() {
    try {
      Object text = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
      return text instanceof String value ? value.replace('\r', ' ').replace('\n', ' ') : "";
    } catch (Exception e) { return ""; }
  }
}
