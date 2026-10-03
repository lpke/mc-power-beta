package local.luke.tweaks.hotbar;

public final class HotbarSettings {
  public boolean swap = true, scroll = true, numberRowKeys = true, overlay = true;
  public boolean reverseScroll = false, rememberRow = true;
  public int selectedRow = 2, offsetX = 4, offsetY = 4;
  public Alignment alignment = Alignment.BOTTOM_RIGHT;

  public enum Alignment {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER
  }

  public HotbarSettings copy() {
    HotbarSettings s = new HotbarSettings();
    s.swap = swap;
    s.scroll = scroll;
    s.numberRowKeys = numberRowKeys;
    s.overlay = overlay;
    s.reverseScroll = reverseScroll;
    s.rememberRow = rememberRow;
    s.selectedRow = selectedRow;
    s.offsetX = offsetX;
    s.offsetY = offsetY;
    s.alignment = alignment;
    return s;
  }

  public void validate() {
    if (selectedRow < 0
        || selectedRow > 2
        || alignment == null
        || Math.abs((long) offsetX) > 4096
        || Math.abs((long) offsetY) > 4096)
      throw new IllegalArgumentException("Invalid hotbar row or overlay position");
  }
}
