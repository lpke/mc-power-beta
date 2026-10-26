package local.luke.power.ui;

/** One geometry for the editable name, Save and playlist presentation actions. */
record MusicEditorHeader(int nameX, int nameWidth, int saveX, int addX, int viewX,
    int actionsY, int actionWidth, int height, boolean inline) {
  static MusicEditorHeader at(int left, int top, int width) {
    int action = Math.min(104, (width - 4) / 2);
    boolean inline = width >= 120 + 40 + 104 + 104 + 12;
    int name = inline ? width - 40 - 2 * action - 12 : width - 44;
    int save = inline ? left + name + 4 : left + width - 40;
    int add = inline ? save + 44 : left;
    return new MusicEditorHeader(left, name, save, add, add + action + 4,
        inline ? top : top + 22, action, inline ? 22 : 44, inline);
  }
}
