package local.luke.power.input;

import java.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Immediate key-down/held-state matching. There is no chord timer or key-up action queue. */
public final class Bindings {
  private static volatile Map<String, Integer> required = Map.of();
  private static final Map<String, java.util.function.IntConsumer> mouseListeners = new LinkedHashMap<>();
  private static boolean hasMouseBindings;
  private static final PressModifiers presses = new PressModifiers();
  private static Object[] registered = new Object[0];

  private Bindings() {}

  public static void register(Object[] bindings) {
    registered = bindings;
    hasMouseBindings = false;
    int held = heldModifiers();
    for (Object object : bindings) if (object instanceof Binding b) {
      presses.update(b.power$code(), physical(b.power$code()), held);
      hasMouseBindings |= b.power$code() < 0;
    }
  }
  public static boolean hasMouseBindings() { return hasMouseBindings; }
  public static void onMousePress(String owner, java.util.function.IntConsumer listener) {
    mouseListeners.put(owner, listener);
  }
  public static void mousePress(int key) {
    for (var listener : mouseListeners.values()) listener.accept(key);
  }
  public static boolean claimsMouse(int key) {
    if (key >= 0) return false;
    int pressedWith = presses.update(key, physical(key), heldModifiers());
    for (Object object : registered) if (object instanceof Binding b && b.power$code() == key
        && (pressedWith & modifiers(b.power$id())) == modifiers(b.power$id())) return true;
    return false;
  }

  public static Map<String, Integer> modifiers() { return required; }
  public static int modifiers(String id) { return required.getOrDefault(id, 0); }
  public static void configure(Map<String, Integer> values) {
    Map<String, Integer> checked = new HashMap<>();
    values.forEach((id, mask) -> {
      if (id == null || id.length() > 256 || mask == null || mask < 0 || mask > 7)
        throw new IllegalArgumentException("Invalid modifier binding");
      if (mask != 0) checked.put(id, mask);
    });
    required = Map.copyOf(checked);
  }

  public static int heldModifiers() {
    if (!Keyboard.isCreated()) return 0;
    return (Keyboard.isKeyDown(29) || Keyboard.isKeyDown(157) ? Chord.CTRL : 0)
        | (Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54) ? Chord.SHIFT : 0)
        | (Keyboard.isKeyDown(56) || Keyboard.isKeyDown(184) ? Chord.ALT : 0);
  }

  public static boolean matches(Object keyBinding, int eventKey) {
    return keyBinding instanceof Binding b && b.power$code() == eventKey && active(b);
  }

  public static int eventCode(Object keyBinding) {
    return keyBinding instanceof Binding b && active(b) ? b.power$code() : -1000;
  }

  public static boolean down(String id) {
    for (Object object : registered)
      if (object instanceof Binding b && b.power$id().equals(id)) return down(object);
    return false;
  }

  public static boolean down(Object keyBinding) {
    return keyBinding instanceof Binding b && active(b) && physical(b.power$code());
  }

  private static boolean active(Binding b) {
    int key = b.power$code(), mask = modifiers(b.power$id()), held = heldModifiers();
    int pressedWith = presses.update(key, physical(key), held);
    if (key == 0 || (held & mask) != mask || (pressedWith & mask) != mask) return false;
    // A more specific binding takes precedence over an overlapping plain key.
    // Unrelated movement keys still work while Ctrl/Shift/Alt are held.
    for (Object other : registered) {
      if (!(other instanceof Binding candidate) || candidate.power$code() != key) continue;
      int otherMask = modifiers(candidate.power$id());
      if (Integer.bitCount(otherMask) > Integer.bitCount(mask) && (pressedWith & otherMask) == otherMask)
        return false;
    }
    return true;
  }

  public static boolean physical(int key) {
    if (key < 0) {
      int button = key + 100;
      return Mouse.isCreated() && button >= 0 && button < Mouse.getButtonCount()
          && Mouse.isButtonDown(button);
    }
    return key > 0 && key < Keyboard.KEYBOARD_SIZE && Keyboard.isCreated()
        && Keyboard.isKeyDown(key);
  }
}
