package local.luke.power.ui;

/** Music's row action preference wraps above footer actions when space is tight. */
record MusicFooterLayout(int labelX, int buttonX, int y, int buttonWidth) {
  static final String LABEL = "Double-click to:";

  static MusicFooterLayout at(int left, int actionsX, int bottomY, int labelWidth, int buttonWidth) {
    int y = left + labelWidth + 6 + buttonWidth + 6 <= actionsX ? bottomY : bottomY - 23;
    return new MusicFooterLayout(left, left + labelWidth + 6, y, buttonWidth);
  }
}
