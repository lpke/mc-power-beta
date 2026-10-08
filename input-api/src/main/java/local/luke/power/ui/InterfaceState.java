package local.luke.power.ui;

/** Live presentation preferences shared across module mappings. */
public final class InterfaceState {
  private InterfaceState() {}
  public static boolean blinkingChatCursor;
  public static int debugTextColor = 0xFFFFFF;
  public static boolean cursorVisible(int ticks) { return !blinkingChatCursor || ticks / 6 % 2 == 0; }
}
