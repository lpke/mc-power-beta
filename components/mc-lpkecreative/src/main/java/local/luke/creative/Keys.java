package local.luke.creative;

import net.minecraft.client.options.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class Keys {
  public static final KeyBinding SPRINT =
      new KeyBinding("lpkecreative.sprint", Keyboard.KEY_LCONTROL);
  public static final KeyBinding PICKER = new KeyBinding("lpkecreative.picker", Keyboard.KEY_F4);
  public static final KeyBinding MODIFIER =
      new KeyBinding("lpkecreative.modifier", Keyboard.KEY_F3);
  public static final KeyBinding[] ALL = {SPRINT, PICKER, MODIFIER};

  private Keys() {}

  public static boolean down(int key) {
    if (key < 0) {
      int b = key + 100;
      return Mouse.isCreated() && b >= 0 && b < Mouse.getButtonCount() && Mouse.isButtonDown(b);
    }
    return key > 0
        && key < Keyboard.KEYBOARD_SIZE
        && Keyboard.isCreated()
        && Keyboard.isKeyDown(key);
  }

  public static String name(int key) {
    if (key == 0) return "Unbound";
    return key < 0 ? "Mouse " + (key + 101) : Keyboard.getKeyName(key);
  }
}
