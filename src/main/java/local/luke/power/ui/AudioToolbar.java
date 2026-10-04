package local.luke.power.ui;

import java.util.ArrayList;
import java.util.List;

/** Fixed-width controls at each edge, with space between playback and library actions. */
public final class AudioToolbar {
  public enum Action { PLAY, PREVIOUS, NEXT, QUEUE, RELOAD, LIBRARY }
  public record Button(Action action, String label, int x, int width, boolean icon) {
    public boolean contains(int mouseX) { return mouseX >= x && mouseX < x + width; }
  }

  public static List<Button> layout(int left, int width, boolean queued, boolean playing, boolean library) {
    boolean compact = width < 360, tiny = width < 230;
    int gap = compact ? 2 : 4;
    int[] sizes = tiny ? new int[]{34,16,16,34,18,46}
        : compact ? new int[]{38,20,20,36,44,52} : new int[]{50,58,42,46,54,60};
    String[] labels = {playing ? "Pause" : "Play", compact ? "<<" : "Previous",
        compact ? ">>" : "Next", "Queue", "Reload", library ? "Settings" : "Library"};
    List<Button> result = new ArrayList<>();
    int x = left;
    for (int i = 0; i < 3; i++) {
      result.add(new Button(Action.values()[i], labels[i], x, sizes[i], false));
      x += sizes[i] + gap;
    }
    x = left + width - sizes[5] - sizes[4] - gap - (queued ? sizes[3] + gap : 0);
    for (int i = queued ? 3 : 4; i < 6; i++) {
      result.add(new Button(Action.values()[i], labels[i], x, sizes[i], tiny && i == 4));
      x += sizes[i] + gap;
    }
    return List.copyOf(result);
  }
}
