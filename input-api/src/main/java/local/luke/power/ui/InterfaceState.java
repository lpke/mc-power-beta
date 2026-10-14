package local.luke.power.ui;

/** Live presentation preferences shared across module mappings. */
public final class InterfaceState {
  private InterfaceState() {}
  public static boolean blinkingChatCursor;
  public static boolean blinkingUiCursor = true;
  public static int debugTextColor = 0xFFFFFF;
  public static boolean cursorVisible(int ticks) { return !blinkingChatCursor || ticks / 6 % 2 == 0; }
  public static boolean uiCursorVisible() { return !blinkingUiCursor || System.nanoTime() / 500_000_000L % 2 == 0; }
}
