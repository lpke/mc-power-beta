package local.luke.power.ui;

import local.luke.power.audio.*;
import net.minecraft.client.gui.screen.Screen;

/** Reuses the Audio page transport and scrubber; no second playback state. */
public final class MainMenuMusic extends UiScreen {
  private final MusicStatusBar status = new MusicStatusBar();
  private MenuMusicLayout bounds;

  public void layout(int menuTop, int menuBottom) {
    AudioSettings s = AudioConfig.current();
    bounds =
        MenuMusicLayout.at(
            width,
            height,
            menuTop,
            menuBottom,
            s.menuControlsPosition,
            s.menuControlsOffsetX,
            s.menuControlsOffsetY);
  }

  public void removed() {
    status.cancel();
  }

  private AudioToolbar.Layout toolbar() {
    return AudioToolbar.layout(
        bounds.x(),
        bounds.width(),
        MusicRequests.tracks().size(),
        AudioController.musicPlaying(),
        false);
  }

  private int statusY() {
    return bounds.y() + toolbar().bottom() - 52 + 3;
  }

  public boolean press(Screen parent, int x, int y, int button) {
    if (!AudioConfig.current().menuControls || bounds == null) return false;
    if (status.press(x, y, button)) return true;
    if (button != 0) return false;
    for (var item : toolbar().buttons()) {
      if (!item.contains(x, y - bounds.y() + 52)) continue;
      if (!item.enabled()) return true;
      minecraft.soundManager.method_2009("random.click", 1, 1);
      switch (item.action()) {
        case PLAY -> AudioController.togglePause();
        case PREVIOUS -> AudioController.previous();
        case NEXT -> AudioController.next();
        case QUIET -> AudioController.quiet();
        case RELOAD -> AudioController.reload();
        case LIBRARY -> PowerOptionsScreen.openMusic(minecraft, parent, false, "");
        case QUEUE -> PowerOptionsScreen.openMusic(minecraft, parent, true, "");
      }
      return true;
    }
    if (inside(x, y, bounds.x(), statusY(), bounds.width(), 16)) {
      String id = AudioController.currentTrackId();
      if (!id.isEmpty()) PowerOptionsScreen.openMusic(minecraft, parent, false, id);
      return true;
    }
    return false;
  }

  public void render(int mx, int my, float delta) {
    if (!AudioConfig.current().menuControls || bounds == null) {
      status.cancel();
      return;
    }
    fill(
        bounds.x() - 3,
        bounds.y() - 3,
        bounds.x() + bounds.width() + 3,
        bounds.y() + bounds.height() + 3,
        0x50000000);
    for (var item : toolbar().buttons()) {
      int y = bounds.y() + item.y() - 52;
      if (item.action() == AudioToolbar.Action.PLAY)
        iconButton(
            AudioController.musicPlaying() ? "pause" : "play",
            item.x(),
            y,
            item.width(),
            mx,
            my,
            true);
      else if (item.action() == AudioToolbar.Action.PREVIOUS
          || item.action() == AudioToolbar.Action.NEXT)
        trackButton(
            item.label(),
            item.action() == AudioToolbar.Action.PREVIOUS,
            item.x(),
            y,
            item.width(),
            mx,
            my);
      else button(item.label(), item.x(), y, item.width(), 18, mx, my, item.enabled());
    }
    status.render(this, bounds.x(), statusY(), bounds.width(), mx, my);
  }
}
