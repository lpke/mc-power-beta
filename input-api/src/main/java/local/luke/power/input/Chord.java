package local.luke.power.input;

/** The low signed word stores the native keyboard/mouse code; three bits store modifiers. */
public record Chord(int key, int modifiers) {
  public static final int CTRL = 1, SHIFT = 2, ALT = 4;

  public Chord {
    if (key < -100 || key > 255 || modifiers < 0 || modifiers > 7)
      throw new IllegalArgumentException("Invalid key combination");
    if (key == 0 && modifiers != 0)
      throw new IllegalArgumentException("An unbound action cannot have modifiers");
  }

  public int encoded() { return modifiers == 0 ? key : (modifiers << 16) | (key & 65535); }

  public static Chord decode(int code) {
    return code < 0 ? new Chord(code, 0) : new Chord((short)code, code >>> 16);
  }

  public boolean matches(int eventKey, int heldModifiers) {
    return key != 0 && key == eventKey && (heldModifiers & modifiers) == modifiers;
  }

  public static int modifier(int key) {
    return switch (key) {
      case 29, 157 -> CTRL;
      case 42, 54 -> SHIFT;
      case 56, 184 -> ALT;
      default -> 0;
    };
  }
}
