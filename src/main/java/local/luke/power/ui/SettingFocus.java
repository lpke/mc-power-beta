package local.luke.power.ui;

/** Place a linked setting below its heading with neighboring rows still visible. */
public final class SettingFocus {
  private SettingFocus() {}

  public static double scroll(int rowY, int rowHeight, int viewport, int content, int headingHeight) {
    int context = Math.max(headingHeight, Math.min(rowHeight * 2, (viewport - rowHeight) / 2));
    return Math.max(0, Math.min(rowY - context, Math.max(0, content - viewport)));
  }
}
