package local.luke.power.input;

import org.lwjgl.input.Keyboard;

public final class FullscreenKey {
  private static final PressLatch PRESS = new PressLatch();
  private FullscreenKey() {}

  public static void releaseEvent(int key, boolean down) { if (!down) PRESS.release(key); }

  public static int eventCode(Object binding) {
    int code = Bindings.eventCode(binding);
    if (code <= 0 || code != Keyboard.getEventKey() || !Keyboard.getEventKeyState()
        || Keyboard.isRepeatEvent() || !PRESS.claim(code)) return -1000;
    return code;
  }
}
