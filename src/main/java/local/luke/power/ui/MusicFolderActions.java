package local.luke.power.ui;

import com.google.gson.JsonElement;
import java.util.*;
import local.luke.power.audio.MusicFolders;
import local.luke.power.config.Setting;
import org.lwjgl.input.Keyboard;

/** Source actions use the surrounding Options transaction, without another folder draft. */
final class MusicFolderActions extends UiScreen {
  private final PowerOptionsScreen parent;
  private final MusicEdits edits;
  private final Runnable changed;
  private String removing = "", removalLabel = "", error = "";
  private JsonElement removalValue;

  MusicFolderActions(PowerOptionsScreen parent, Runnable changed) {
    this.parent = parent;
    this.edits = new MusicEdits(parent);
    this.changed = changed;
  }

  private Setting setting() { return edits.setting("musicDirectories"); }
  String error() { return error; }
  boolean modal() { return !removing.isEmpty(); }
  void cancel() { removing = removalLabel = ""; removalValue = null; error = ""; }

  void add() {
    error = "";
    JsonElement expected = setting().value.deepCopy();
    var sources = MusicFolders.decode(expected);
    minecraft.setScreen(new DirectoryScreen(parent, additions -> {
      if (!expected.equals(setting().value))
        throw new IllegalArgumentException("Folders changed. Reopen Add folder.");
      List<String> paths = new ArrayList<>(sources);
      paths.addAll(additions);
      if (paths.size() > MusicFolders.MAX_FOLDERS || paths.size() != new HashSet<>(paths).size())
        throw new IllegalArgumentException("Use at most 32 unique folders. Nothing was added.");
      apply(MusicFolders.encode(paths));
    }, sources));
  }

  void remove(String path, String label) {
    if (!MusicFolders.decode(setting().value).contains(path)) {
      error = "Folder changed. Select it again.";
      return;
    }
    removing = path;
    removalLabel = label;
    removalValue = setting().value.deepCopy();
    error = "";
  }

  private void apply(JsonElement value) {
    Setting source = setting();
    source.validate(value);
    var draft = new SettingsDraft(List.of(source), ignored -> true);
    draft.get(source.id).value = value;
    if (!parent.acceptEditor(draft))
      throw new IllegalArgumentException(parent.editorError());
    error = "";
    changed.run();
  }

  void key(int key) { if (key == Keyboard.KEY_ESCAPE) cancel(); }

  private int dialogWidth() { return Math.min(312, width - 20); }
  private int dialogLeft() { return (width - dialogWidth()) / 2; }
  private int buttonWidth() { return (dialogWidth() - 4) / 2; }
  private int buttonY() { return height / 2 + 13; }

  void dialogClick(int x, int y, int button) {
    if (!modal() || button != 0) return;
    if (inside(x, y, dialogLeft() + buttonWidth() + 4, buttonY(), buttonWidth(), 20)) {
      click(); cancel(); return;
    }
    if (!inside(x, y, dialogLeft(), buttonY(), buttonWidth(), 20)) return;
    click();
    try {
      if (!removalValue.equals(setting().value))
        throw new IllegalArgumentException("Folders changed. Cancel and select Remove again.");
      var sources = MusicFolders.decode(setting().value);
      List<String> paths = new ArrayList<>(sources);
      paths.remove(removing);
      apply(MusicFolders.encode(paths));
      cancel();
    } catch (RuntimeException failure) { error = Objects.toString(failure.getMessage(), "Could not remove this folder."); }
  }

  void renderDialog(int mx, int my) {
    if (!modal()) return;
    fill(0, 0, width, height, 0xc0000000);
    int x = dialogLeft(), w = dialogWidth();
    WarningText.draw(this, w, buttonY() - 12, "Remove \"" + removalLabel + "\"?",
        error.isEmpty() ? "Files and playlist entries stay unchanged.\n" + removing : error,
        error.isEmpty() ? 0xaaaaaa : 0xff8888);
    button("Remove", x, buttonY(), buttonWidth(), 20, mx, my, true);
    button("Cancel", x + buttonWidth() + 4, buttonY(), buttonWidth(), 20, mx, my, true);
  }
}
