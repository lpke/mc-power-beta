package local.luke.power.ui;

import local.luke.power.audio.AudioConfig;
import local.luke.power.audio.AudioController;

/** Compact transport for the pause menu, using the same playback state as Audio settings. */
public final class PauseMenuMusic extends UiScreen {
  private MenuMusicLayout bounds;
  private final MusicStatusBar scrub = new MusicStatusBar();
  public void init() { scrub.cancel(); }
  private static final int BUTTON_WIDTH = 20, BUTTON_STEP = 24;

  public void layout(int menuTop, int menuBottom) {
    var s = AudioConfig.current();
    bounds = MenuMusicLayout.at(width, height, menuTop, menuBottom,
        s.menuControlsPosition, s.menuControlsOffsetX, s.menuControlsOffsetY, s.menuControlsScrub);
  }

  private int controlsX() {
    return bounds.x() + (bounds.width() - (BUTTON_STEP * 3 + BUTTON_WIDTH)) / 2;
  }

  public boolean press(int x, int y, int button) {
    if (!AudioConfig.current().menuControls || bounds == null || button != 0) return false;
    if (AudioConfig.current().menuControlsScrub && scrub.press(x, y, button)) return true;
    for (int i = 0; i < 4; i++) {
      if (!inside(x, y, controlsX() + i * BUTTON_STEP, bounds.y(), BUTTON_WIDTH, 18)) continue;
      minecraft.soundManager.method_2009("random.click", 1, 1);
      switch (i) {
        case 0 -> AudioController.togglePause();
        case 1 -> AudioController.previous();
        case 2 -> AudioController.next();
        case 3 -> AudioController.quiet();
      }
      return true;
    }
    if (inside(x, y, bounds.x(), bounds.y() + 21, bounds.width(), 12)) {
      minecraft.soundManager.method_2009("random.click", 1, 1);
      scrub.cancel();
      PowerOptionsScreen.openMusic(minecraft, minecraft.currentScreen, false, AudioController.currentTrackId());
      return true;
    }
    return false;
  }

  public void render(int mx, int my, float delta) {
    if (!AudioConfig.current().menuControls || bounds == null) return;
    int x = controlsX(), y = bounds.y();
    iconButton(AudioController.musicPlaying() ? "pause" : "play", x, y, BUTTON_WIDTH, mx, my, true);
    trackButton("", true, x + BUTTON_STEP, y, BUTTON_WIDTH, mx, my);
    trackButton("", false, x + BUTTON_STEP * 2, y, BUTTON_WIDTH, mx, my);
    button("Q", x + BUTTON_STEP * 3, y, BUTTON_WIDTH, 18, mx, my, true);
    String status = fit(AudioController.status(), bounds.width());
    text(status, bounds.x() + (bounds.width() - textRenderer.getWidth(status)) / 2, y + 24, inside(mx, my, bounds.x(), y + 21, bounds.width(), 12) ? 0xffffa0 : 0xaaaaaa);
    if (AudioConfig.current().menuControlsScrub) scrub.renderScrubber(this, bounds.x(), y + 34, bounds.width(), mx, my);
  }
}
