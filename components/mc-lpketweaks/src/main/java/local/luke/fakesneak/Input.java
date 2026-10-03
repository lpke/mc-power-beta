package local.luke.fakesneak;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class Input {
  private Input() {}

  public static boolean down(int key) {
    if (key < 0) {
      int button = key + 100;
      return Mouse.isCreated()
          && button >= 0
          && button < Mouse.getButtonCount()
          && Mouse.isButtonDown(button);
    }
    return Keyboard.isCreated()
        && key > 0
        && key < Keyboard.KEYBOARD_SIZE
        && Keyboard.isKeyDown(key);
  }

  public static String name(int key) {
    if (key == 0) return "Unbound";
    if (key < 0) return "Mouse " + (key + 101);
    if (key >= Keyboard.KEYBOARD_SIZE) return "Unbound";
    String name = Keyboard.getKeyName(key);
    return name == null ? "Unbound" : name;
  }
}
