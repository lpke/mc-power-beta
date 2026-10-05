package local.luke.power.ui;

/** One geometry for rendering, hit testing and slider dragging. */
public record MusicRowLayout(
    int nameX,
    int nameWidth,
    int controlsY,
    int volumeX,
    int volumeWidth,
    int favouriteX,
    int previewX,
    int playX,
    int queueX,
    int upX,
    int downX,
    int height,
    int buttonWidth) {
  public static int sliderWidth(int width) {
    return Math.max(54, Math.min(120, width / 4));
  }

  public static MusicRowLayout at(int left, int width, boolean queue) {
    boolean inline = width >= (queue ? 320 : 440);
    int button = width < 260 ? 14 : 20, gap = 2;
    int right = left + width, actions = right - (button + gap) * (queue ? 5 : 4) + gap;
    int volumeEnd = actions - 4;
    int volumeWidth = sliderWidth(width);
    int volumeX = volumeEnd - volumeWidth;
    int nameX = left + (queue ? 2 : 24);
    int nameWidth = inline ? Math.max(12, volumeX - nameX - 6) : width - (queue ? 4 : 26);
    return new MusicRowLayout(
        nameX,
        nameWidth,
        inline ? 3 : 23,
        volumeX,
        volumeWidth,
        actions,
        queue ? -1 : actions + button + gap,
        actions + (button + gap) * (queue ? 3 : 2),
        actions + (button + gap) * (queue ? 4 : 3),
        queue ? actions + button + gap : -1,
        queue ? actions + (button + gap) * 2 : -1,
        inline ? 24 : 44,
        button);
  }
}
