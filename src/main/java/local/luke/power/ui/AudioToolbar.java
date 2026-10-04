package local.luke.power.ui;

import java.util.ArrayList;
import java.util.List;

/** Fixed-width controls at each edge, with space between playback and library actions. */
public final class AudioToolbar {
  public enum Action { SETTINGS, PLAY, PREVIOUS, NEXT, QUEUE, LIBRARY, RELOAD }
  public record Button(Action action, String label, int x, int y, int width, boolean enabled) {
    public boolean contains(int mouseX, int mouseY) {
      return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 18;
    }
  }
  public record Layout(List<Button> buttons, int bottom) {}

  public static int bottom(int width, boolean library) {
    boolean compact = width < (library ? 410 : 366);
    int left = compact ? (library ? 106 : 70) : (library ? 202 : 158);
    int right = compact ? 162 : 196;
    return width >= left + right + 12 ? 70 : 92;
  }

  public static Layout layout(int left, int width, int queued, boolean playing, boolean library) {
    boolean compact = width < (library ? 410 : 366);
    int gap = compact ? 2 : 4;
    int[] sizes = compact ? new int[]{34,34,16,16,68,48,42} : new int[]{40,50,50,50,74,60,54};
    String[] labels = {"Back", playing ? "Pause" : "Play", compact ? "" : "Prev",
        compact ? "" : "Next", queued > 0 ? "Queue (" + queued + ")" : "Queue", "Library", "Reload"};
    List<Button> result = new ArrayList<>();
    int x = left;
    for (int i = library ? 0 : 1; i < 4; i++) {
      result.add(new Button(Action.values()[i], labels[i], x, 52, sizes[i], true));
      x += sizes[i] + gap;
    }
    int rightGroup = left + width - sizes[4] - sizes[5] - sizes[6] - 2 * gap;
    int y = bottom(width, library) - 18;
    x = rightGroup;
    for (int i = 4; i < 7; i++) {
      result.add(new Button(Action.values()[i], labels[i], x, y, sizes[i], i != 4 || queued > 0));
      x += sizes[i] + gap;
    }
    return new Layout(List.copyOf(result), y + 18);
  }
}
