package local.luke.power.ui;

import local.luke.power.audio.*;
import org.lwjgl.input.Mouse;

/** A single status/scrub line shared by Audio settings, library and queue. */
final class MusicStatusBar {
  private boolean dragging;
  private double fraction;
  private String track = "";
  private int barX, barWidth, top;
  private boolean available;

  boolean press(int x, int y, int button) {
    if (button != 0 || !available || x < barX || x >= barX + barWidth || y < top || y >= top + 16)
      return false;
    dragging = true;
    track = AudioController.currentTrackId();
    slide(x);
    return true;
  }

  private void slide(int x) {
    fraction = Math.max(0, Math.min(1, (x - barX) / (double) Math.max(1, barWidth)));
  }

  void cancel() {
    dragging = false;
  }

  void render(UiScreen screen, int left, int y, int width, int mx, int my) {
    double duration = AudioController.duration();
    available = duration > 0 && width >= 180;
    if (dragging) {
      if (!track.equals(AudioController.currentTrackId()) || !available) dragging = false;
      else if (Mouse.isButtonDown(0)) slide(mx);
      else {
        dragging = false;
        AudioController.seek(fraction);
      }
    }
    int reserved = available ? Math.min(160, width / 2) : 0;
    screen.text(
        screen.fit(AudioController.status(), Math.max(10, width - reserved - 6)),
        left,
        y + 4,
        0xaaaaaa);
    if (!available) return;
    top = y;
    barWidth = reserved - 40;
    barX = left + width - reserved;
    double position =
        dragging ? fraction * duration : Math.min(duration, AudioController.position());
    screen.rectangle(barX, y + 7, barX + barWidth, y + 10, 0xff333333);
    int end = barX + (int) Math.round(barWidth * Math.min(1, position / duration));
    screen.rectangle(barX, y + 7, end, y + 10, 0xffaaaaaa);
    screen.rectangle(
        Math.max(barX, end - 1), y + 5, Math.min(barX + barWidth, end + 1), y + 12, 0xffdddddd);
    screen.text(time(position), barX + barWidth + 5, y + 4, 0xaaaaaa);
  }

  static String time(double seconds) {
    int value = (int) Math.max(0, seconds);
    return value / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", value % 60);
  }
}
