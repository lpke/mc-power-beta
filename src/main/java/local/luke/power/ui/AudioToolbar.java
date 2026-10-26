package local.luke.power.ui;

import java.util.ArrayList;
import java.util.List;

/** Playback positions stay fixed; playlist actions share one responsive right-hand group. */
public final class AudioToolbar {
  public static final int PLAYBACK_BOTTOM = 70;
  public enum Action { PLAY, PREVIOUS, NEXT, QUIET, QUEUE, PLAYLIST, SHUFFLE }
  public record Button(Action action, String label, int x, int y, int width, boolean enabled) {
    public boolean contains(int mouseX, int mouseY) { return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 18; }
  }
  public record Layout(List<Button> buttons, int bottom) {}
  public static Layout playback(int left, int width, boolean playing) {
    return new Layout(layout(left, width, 0, playing, false).buttons().stream()
        .filter(button -> button.action().ordinal() < Action.QUEUE.ordinal()).toList(), PLAYBACK_BOTTOM);
  }
  public static Layout editor(int left, int width, int queued, boolean playing) { return layout(left, width, queued, playing, true); }
  public static int bottom(int width, boolean library) { return layout(0, width, 0, false, library).bottom(); }
  public static Layout layout(int left, int width, int queued, boolean playing, boolean library) {
    boolean compact = width < 380; int gap = compact ? 2 : 4;
    int[] sizes = compact ? new int[]{20,16,16,36} : new int[]{20,50,50,40};
    String[] labels = {"", compact ? "" : "Prev", compact ? "" : "Next", "Quiet"};
    List<Button> result = new ArrayList<>(); int x = left;
    for (int i = 0; i < 4; i++) { result.add(new Button(Action.values()[i], labels[i], x, 52, sizes[i], true)); x += sizes[i] + gap; }
    int playlist = compact ? 72 : 118, queue = compact ? 20 : 74;
    int actions = playlist + gap + 20 + (queued > 0 ? gap + queue : 0);
    int y = left + width - actions >= x + 4 ? 52 : 74;
    int shuffleX = left + width - 20, playlistX = shuffleX - gap - playlist;
    if (queued > 0) result.add(new Button(Action.QUEUE, compact ? "Q" : "Queue (" + queued + ")", playlistX - gap - queue, y, queue, true));
    result.add(new Button(Action.PLAYLIST, "Playlist", playlistX, y, playlist, true));
    result.add(new Button(Action.SHUFFLE, "", shuffleX, y, 20, true));
    return new Layout(List.copyOf(result), y + 18);
  }
}
