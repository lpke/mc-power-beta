package local.luke.power.ui;

/** Ordinary Options rows and Defaults rows share spacing and control heights. */
final class SettingRows {
  static final int BUTTON_HEIGHT = 18;
  static boolean narrow(int span) { return span < 340; }
  static int height(int span) { return narrow(span) ? 40 : 24; }
  static int buttonOffset(int span) { return narrow(span) ? 16 : 3; }
  static int labelOffset(int span) { return narrow(span) ? 3 : 8; }
  private SettingRows() {}
}
