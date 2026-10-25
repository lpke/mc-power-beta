package local.luke.power.input;

import java.util.function.ToIntFunction;

/** Footer geometry shared by the native photo camera and its map tools. */
public record PhotoControlsLayout(int width, int height, int toggleWidth, int centerWidth,
    int closeWidth, int screenshotWidth) {
  public static PhotoControlsLayout at(int width, int height, ToIntFunction<String> measure) {
    int toggles = measured(measure, "Hide Toggles", "Show Toggles");
    int center = measured(measure, "Center", "Full Reset");
    int close = measured(measure, "Close", "Close");
    center = Math.min(center, Math.max(1, width - 16 - toggles - close - 8));
    return new PhotoControlsLayout(width, height, toggles, center, close,
        measured(measure, "Take Screenshot", "Take Screenshot"));
  }
  private static int measured(ToIntFunction<String> measure, String first, String second) {
    return Math.max(42, Math.max(measure.applyAsInt(first), measure.applyAsInt(second)) + 8);
  }
  public int toggleX() { return 8; }
  public int buttonHeight() { return 18; }
  public int bottomY() { return height - 24; }
  public int centerX() { return toggleX() + toggleWidth() + 4; }
  public int closeX() { return width - 8 - closeWidth; }
  public int rotationLeftX() { return (width - screenshotWidth - 48) / 2; }
  public int screenshotX() { return rotationLeftX() + 24; }
  public int rotationRightX() { return screenshotX() + screenshotWidth + 4; }
  public int rotationY() {
    boolean fits = centerX() + centerWidth + 4 <= rotationLeftX()
        && rotationRightX() + 24 <= closeX();
    return bottomY() - (fits ? 0 : 22);
  }
  public int panelBottom() { return rotationY() - 4; }
}
