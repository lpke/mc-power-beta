package local.luke.power.ui;

import java.util.ArrayList;
import java.util.List;

/** Playback stays on the left; queue, library and reload stay together on the right. */
public final class AudioToolbar {
  public enum Action { PLAY, PREVIOUS, NEXT, QUIET, QUEUE, LIBRARY, RELOAD }
  public record Button(Action action, String label, int x, int y, int width, boolean enabled) {
    public boolean contains(int mouseX, int mouseY) {
      return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 18;
    }
  }
  public record Layout(List<Button> buttons, int bottom) {}

  public static int bottom(int width, boolean library) {
    return width >= (width < 380 ? 268 : 380) ? 70 : 92;
  }

  public static Layout layout(int left, int width, int queued, boolean playing, boolean library) {
    boolean compact = width < 380;
    int gap = compact ? 2 : 4;
    int[] sizes = compact ? new int[]{20,16,16,36,68,48,42} : new int[]{20,50,50,40,74,60,54};
    String[] labels = {"", compact ? "" : "Prev", compact ? "" : "Next", "Quiet",
        queued > 0 ? "Queue (" + queued + ")" : "Queue", "Library", "Reload"};
    List<Button> result = new ArrayList<>();
    int x = left;
    for (int i = 0; i < 4; i++) {
      result.add(new Button(Action.values()[i], labels[i], x, 52, sizes[i], true));
      x += sizes[i] + gap;
    }
    x = left + width - sizes[4] - sizes[5] - sizes[6] - 2 * gap;
    int y = bottom(width, library) - 18;
    for (int i = 4; i < 7; i++) {
      result.add(new Button(Action.values()[i], labels[i], x, y, sizes[i], i != 4 || queued > 0));
      x += sizes[i] + gap;
    }
    return new Layout(List.copyOf(result), y + 18);
  }
}
