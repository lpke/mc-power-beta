package local.luke.power.ui;

/** Shared bounds for the song name, exact volume editor and single play control. */
public record MusicRowLayout(int nameX, int nameWidth, int controlsY, int volumeX, int volumeWidth,
    int favouriteX, int previewX, int playX, int queueX, int upX, int downX, int height, int buttonWidth) {
  public static int sliderWidth(int width) { return Math.max(54, Math.min(120, width / 4)); }
  public static MusicRowLayout playlist(int left, int width) { return at(left, width, false); }
  public static MusicRowLayout at(int left, int width, boolean queue) {
    int button = width < 260 ? 18 : 20, play = left + width - button;
    int volume = play - 4 - sliderWidth(width);
    return new MusicRowLayout(left + 2, Math.max(12, volume - left - 8), 3,
        volume, sliderWidth(width), -1, -1, play, -1, -1, -1, 24, button);
  }
}
