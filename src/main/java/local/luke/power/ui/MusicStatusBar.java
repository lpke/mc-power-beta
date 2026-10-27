package local.luke.power.ui;

import local.luke.power.audio.*;
import org.lwjgl.input.Mouse;

/** A single status/scrub line shared by Audio settings, library and queue. */
final class MusicStatusBar {
  private boolean dragging;
  private double fraction;
  private String track = "";
  private int barX, barWidth, top, labelX, labelWidth;
  private boolean available;

  boolean overTrack(int x, int y) {
    return overLabel(x, y) && !AudioController.navigationTrackId().isEmpty();
  }
  boolean overLabel(int x, int y) { return x >= labelX && x < labelX + labelWidth && y >= top && y < top + 15; }

  boolean press(int x, int y, int button) {
    if (button != 0 || !available || x < barX || x >= barX + barWidth || y < top || y >= top + 16)
      return false;
    dragging = true;
    track = AudioController.currentTrackId();
    slide(x);
    return true;
  }

  private void slide(int x) {
    fraction = new MusicScrubLayout(barX, barWidth).fraction(x);
  }

  void cancel() {
    dragging = false;
  }

  void render(UiScreen screen, int left, int y, int width, int mx, int my) {
    render(screen, left, y, width, mx, my, true);
  }

  void renderScrubber(UiScreen screen, int left, int y, int width, int mx, int my) {
    render(screen, left, y, width, mx, my, false);
  }

  private void render(UiScreen screen, int left, int y, int width, int mx, int my, boolean label) {
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
    int reserved = available ? (label ? Math.min(160, width / 2) : width) : 0;
    top = y;
    labelX = left;
    labelWidth = width - reserved;
    if (label) screen.text(
        screen.fit(AudioController.status(), Math.max(10, width - reserved - 6)),
        left,
        y + 4,
        overTrack(mx, my) ? 0xffffa0 : 0xaaaaaa);
    if (!available) return;
    var bounds = MusicScrubLayout.at(left, width, label);
    barWidth = bounds.width();
    barX = bounds.x();
    double position =
        dragging ? fraction * duration : Math.min(duration, AudioController.position());
    screen.rectangle(barX, y + 7, barX + barWidth, y + 10, 0xff333333);
    int end = barX + (int) Math.round(barWidth * Math.min(1, position / duration));
    screen.rectangle(barX, y + 7, end, y + 10, 0xffaaaaaa);
    int thumb = bounds.thumbLeft(position / duration);
    screen.rectangle(thumb, y + 5, thumb + MusicScrubLayout.THUMB_WIDTH, y + 12, 0xffdddddd);
    screen.text(time(position), barX + barWidth + 5, y + 4, 0xaaaaaa);
  }

  static String time(double seconds) {
    int value = (int) Math.max(0, seconds);
    return value / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", value % 60);
  }
}
