package local.luke.power.ui;

/** Shared bounds for the song name, exact volume editor favourite and play controls. */
public record MusicRowLayout(int nameX, int nameWidth, int controlsY, int volumeX, int volumeWidth,
    int favouriteX, int previewX, int playX, int queueX, int upX, int downX, int height, int buttonWidth) {
  public static int hoverLeft(int left) { return left - 2; }
  public static int hoverRight(int right) { return right + 4; }
  public static int hoverWidth(int left, int right) { return hoverRight(right) - hoverLeft(left); }
  public static int sliderWidth(int width) { return VolumeControls.musicWidth(width); }
  public int groupVolumeWidth() { return volumeWidth + VolumeControls.CHILD_INSET; }
  public int groupVolumeX() { return playX + buttonWidth - groupVolumeWidth(); }
  public int groupRemoveX() { return groupVolumeX() - buttonWidth - 4; }
  public static MusicRowLayout playlist(int left, int width) { return at(left, width, false); }
  public static MusicRowLayout at(int left, int width, boolean queue) {
    int button = width < 260 ? 18 : 20, play = left + width - button;
    int favorite = play - button - 2;
    int volume = favorite - 4 - sliderWidth(width);
    return new MusicRowLayout(left + 2, Math.max(12, volume - left - 8), 3,
        volume, sliderWidth(width), favorite, -1, play, -1, -1, -1, 24, button);
  }
}
