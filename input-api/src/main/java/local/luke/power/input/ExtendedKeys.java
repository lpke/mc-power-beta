package local.luke.power.input;

/** Stable unused LWJGL slots for F20–F24, within its 256-key state buffer. */
public final class ExtendedKeys {
  private ExtendedKeys() {}
  public static int linux(long keysym) {
    if (keysym == '\\' || keysym == '|') return 43;
    if (keysym >= 0xffca && keysym <= 0xffcf) return 100 + (int)(keysym - 0xffca); // F13–F18
    if (keysym >= 0xffd0 && keysym <= 0xffd5) return 113 + (int)(keysym - 0xffd0); // F19–F24
    return 0;
  }
  public static String name(int key) { return key >= 114 && key <= 118 ? "F" + (key - 94) : null; }
  public static int code(String name) {
    for (int key=114;key<=118;key++) if (name(key).equals(name)) return key;
    return 0;
  }
}
