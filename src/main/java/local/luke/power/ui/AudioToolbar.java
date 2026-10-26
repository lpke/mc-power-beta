package local.luke.power.ui;

import java.util.ArrayList;
import java.util.List;

/** Playback retains its original position; queue and playlist selection share the right edge. */
public final class AudioToolbar {
  public static final int PLAYBACK_BOTTOM = 70;
  public enum Action { PLAY, PREVIOUS, NEXT, QUIET, QUEUE, PLAYLIST }
  public record Button(Action action, String label, int x, int y, int width, boolean enabled) {
    public boolean contains(int mouseX, int mouseY) {
      return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 18;
    }
  }
  public record Layout(List<Button> buttons, int bottom) {}

  public static Layout playback(int left, int width, boolean playing) {
    return new Layout(layout(left, width, 0, playing, true).buttons().stream()
        .filter(button -> button.action().ordinal() < Action.QUEUE.ordinal()).toList(), PLAYBACK_BOTTOM);
  }

  public static Layout editor(int left, int width, int queued, boolean playing) {
    List<Button> buttons = new ArrayList<>(playback(left, width, playing).buttons());
    if (queued > 0) buttons.add(new Button(Action.QUEUE, "Queue (" + queued + ")",
        left + width - 68, 52, 68, true));
    return new Layout(List.copyOf(buttons), PLAYBACK_BOTTOM);
  }

  public static int bottom(int width, boolean library) {
    return width >= (width < 380 ? 268 : 380) ? 70 : 92;
  }

  public static Layout layout(int left, int width, int queued, boolean playing, boolean library) {
    boolean compact = width < 380;
    int gap = compact ? 2 : 4;
    int[] sizes = compact ? new int[]{20,16,16,36,68,92} : new int[]{20,50,50,40,74,118};
    String[] labels = {"", compact ? "" : "Prev", compact ? "" : "Next", "Quiet",
        queued > 0 ? "Queue (" + queued + ")" : "Queue", "Playlist"};
    List<Button> result = new ArrayList<>();
    int x = left;
    for (int i = 0; i < 4; i++) {
      result.add(new Button(Action.values()[i], labels[i], x, 52, sizes[i], true));
      x += sizes[i] + gap;
    }
    x = left + width - sizes[4] - sizes[5] - gap;
    int y = bottom(width, library) - 18;
    for (int i = 4; i < 6; i++) {
      if (i != 4 || queued > 0)
        result.add(new Button(Action.values()[i], labels[i], x, y, sizes[i], true));
      x += sizes[i] + gap;
    }
    return new Layout(List.copyOf(result), y + 18);
  }
}
