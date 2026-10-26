package local.luke.power.ui;

/** Shared bounds for playlist names and the Add songs and current-view controls. */
record MusicEditorHeader(int nameX, int nameWidth, int addX, int addWidth, int viewX,
    int viewWidth, int actionsY, int height, boolean inline) {
  static MusicEditorHeader at(int left, int top, int width, int addWidth, int viewWidth) {
    return at(left, top, width, addWidth, viewWidth, addWidth > 0 ? 100 : 40);
  }
  static MusicEditorHeader at(int left, int top, int width, int addWidth, int viewWidth, int minimumNameWidth) {
    int view = Math.min(Math.max(18, viewWidth), width - (addWidth > 0 ? 22 : 0));
    int add = Math.min(Math.max(0, addWidth), Math.max(0, width - view - 4));
    int actions = add + (add > 0 ? 4 : 0) + view;
    boolean inline = width >= minimumNameWidth + 4 + actions;
    int name = inline ? width - actions - 4 : width;
    int actionX = inline ? left + name + 4 : left;
    return new MusicEditorHeader(left, name, actionX, add, actionX + (add > 0 ? add + 4 : 0),
        view, inline ? top : top + 22, inline ? 22 : 44, inline);
  }
}
