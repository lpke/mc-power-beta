package local.luke.power.input;

/** UI keyboard shortcuts see modifiers at the queued event, rather than at the later poll. */
public final class KeyEventModifiers {
  private static int down, depth;
  private KeyEventModifiers() {}

  public static void event(int key, boolean pressed) {
    int bit = switch (key) {
      case 29 -> 1; case 157 -> 2;
      case 42 -> 4; case 54 -> 8;
      case 56 -> 16; case 184 -> 32;
      default -> 0;
    };
    if (pressed) down |= bit; else down &= ~bit;
  }

  /** Reconcile only after the native queue is drained; each bit represents one physical side. */
  public static void reconcile(int physical) { down = physical & 63; }
  public static void begin() { depth++; }
  public static void end() { if (depth > 0) depth--; }
  public static int mask(int polled) {
    if (depth == 0) return polled;
    return ((down & 3) != 0 ? Chord.CTRL : 0)
        | ((down & 12) != 0 ? Chord.SHIFT : 0)
        | ((down & 48) != 0 ? Chord.ALT : 0);
  }
}
