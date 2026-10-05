package local.luke.power.ui;

import local.luke.power.audio.AudioSettings.MenuControlsPosition;

/** Screen-relative anchors in scaled GUI pixels, clamped after offsets. */
public record MenuMusicLayout(int x, int y, int width, int height) {
  public static MenuMusicLayout at(
      int screenWidth,
      int screenHeight,
      int menuTop,
      int menuBottom,
      MenuControlsPosition position,
      int offsetX,
      int offsetY) {
    return at(screenWidth, screenHeight, menuTop, menuBottom, position, offsetX, offsetY, false);
  }

  public static MenuMusicLayout at(int screenWidth, int screenHeight, int menuTop, int menuBottom,
      MenuControlsPosition position, int offsetX, int offsetY, boolean scrub) {
    int width = Math.max(180, Math.min(200, screenWidth - 16));
    boolean side =
        position == MenuControlsPosition.MIDDLE_LEFT
            || position == MenuControlsPosition.MIDDLE_RIGHT;
    if (side) width = Math.max(180, Math.min(width, screenWidth / 2 - 112));
    int height = scrub ? 48 : 32;
    int x =
        switch (position) {
          case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> 8;
          case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> screenWidth - width - 8;
          default -> (screenWidth - width) / 2;
        };
    int y =
        switch (position) {
          case MENU_TOP -> menuTop - height - 8;
          case MENU_BOTTOM -> menuBottom + 8;
          case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 8;
          case MIDDLE_LEFT, MIDDLE_RIGHT -> (screenHeight - height) / 2;
          default -> screenHeight - height - 16;
        };
    if (side && screenWidth / 2 - 112 < width) y = menuTop - height - 8;
    return new MenuMusicLayout(
        Math.max(4, Math.min(screenWidth - width - 4, x + offsetX)),
        Math.max(4, Math.min(screenHeight - height - 12, y + offsetY)),
        width,
        height);
  }
}
