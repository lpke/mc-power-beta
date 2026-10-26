package local.luke.power.ui;

import java.util.*;
import local.luke.power.audio.AudioController;
import local.luke.power.audio.MusicPlaylist;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Saved playlists and their isolated membership, order and group-volume drafts. */
final class MusicPresetsPanel extends UiScreen {
  private final PowerOptionsScreen parent;
  private final MusicEdits edits;
  private final ScrollBar scrollbar = new ScrollBar();
  private final TextInput name = new TextInput("", 80);
  private MusicPresetDraft draft;
  private String error = "", deleting = "";
  private int scroll;
  private Runnable pending;
  private List<MusicPlaylist> countedPresets;
  private int countedRevision = -1;
  private Map<String, Integer> includedCounts = Map.of();

  MusicPresetsPanel(PowerOptionsScreen parent) {
    this.parent = parent;
    edits = new MusicEdits(parent);
  }

  MusicPresetDraft draft() { return draft; }
  boolean hideExcluded() { return false; }
  void unfocus() { name.focused = false; }
  boolean editing() { return draft != null; }
  boolean modal() { return pending != null || !deleting.isEmpty(); }
  boolean dirty() { return draft != null && draft.changed(name.text()); }
  String error() { return error; }

  void open() {
    pending = null;
    deleting = "";
    name.focused = false;
    scrollbar.release();
    draft = null;
    scroll = 0;
    error = "";
  }

  void edit(MusicPlaylist playlist) { edit(playlist, false); }

  private void edit(MusicPlaylist playlist, boolean copy) {
    if (playlist != null && MusicPlaylist.BETA.equals(playlist.id()) && !copy) return;
    draft = copy
        ? new MusicPresetDraft(edits.copy(playlist), AudioController.music(minecraft), true)
        : new MusicPresetDraft(playlist, AudioController.music(minecraft));
    name.setText(draft.name());
    name.focused = false;
    error = "";
  }

  void guard(Runnable action) {
    if (dirty()) pending = action;
    else { draft = null; action.run(); }
  }

  private boolean save() {
    try {
      edits.save(draft.snapshot(name.text()));
      draft = null;
      error = "";
      return true;
    } catch (IllegalArgumentException e) {
      error = e.getMessage();
      return false;
    }
  }

  private int left() { return parent.contentLeft(); }
  private int right() { return parent.contentRight(); }
  private int top() { return parent.audioContentTop() + 40; }
  private int bottom() { return parent.contentBottom(); }
  private int span() { return right() - left(); }
  int headerHeight() { return 22; }
  private int nameX() { return left(); }
  private int nameWidth() { return span() - 96; }
  private int orderX() { return right() - 92; }
  private int createWidth() { return Math.min(94, span() - shuffleWidth() - 4); }
  private int newX() { return left(); }
  private int shuffleWidth() { return 96; }
  private int shuffleX() { return right() - shuffleWidth(); }
  private int saveX() { return right() - 40; }
  private int saveWidth() { return 40; }
  private int editButtonY() { return top(); }
  private int rowTop() { return top() + headerHeight() + 4; }
  private int rowHeight() { return span() < 400 ? 60 : 40; }
  private int actionsY(int row) { return row + (span() < 400 ? 34 : 7); }
  private int textWidth() { return span() - (span() < 400 ? 4 : 208); }
  private int actionWidth() { return span() < 260 ? 40 : 48; }
  private int editX() { return right() - 4 * (actionWidth() + 2) + 2; }
  private int copyX() { return right() - 3 * (actionWidth() + 2) + 2; }
  private int deleteX() { return right() - 2 * (actionWidth() + 2) + 2; }
  private int playX() { return right() - actionWidth(); }
  private int editX(int index) { return editX(); }
  private int actionY(int index) { return actionsY(rowTop() + index * rowHeight() - scroll); }

  String help(int x, int y) {
    if (!editing() && inside(x, y, shuffleX(), top(), shuffleWidth(), 18))
      return "Shuffle automatic playback for all playlists. Off follows each playlist's saved order.";
    if (editing() && inside(x, y, orderX(), top(), 48, 18))
      return "Drag tracks or use the arrows to change their order. Used when Shuffle is Off.";
    return "";
  }

  private ScrollBar.Track track() {
    return new ScrollBar.Track(right() + 5, rowTop(), Math.max(0, bottom() - rowTop()),
        edits.presets().size() * rowHeight(), scroll);
  }

  void wheel(int delta) {
    scroll = (int) Math.max(0, Math.min(track().maximum(), scroll - Math.signum(delta) * rowHeight()));
  }

  void key(char c, int key) {
    if (modal()) {
      if (key == Keyboard.KEY_ESCAPE) { pending = null; deleting = ""; }
      return;
    }
    if (editing()) name.key(c, key);
  }

  boolean headerClick(int x, int y, int b) {
    if (modal()) { dialogClick(x, y, b); return true; }
    if (!editing()) return listClick(x, y, b);
    name.focused = inside(x, y, nameX(), top(), nameWidth(), 18);
    if (name.focused) name.click(x, b);
    if (b == 0 && !draft.tracks().isEmpty() && inside(x, y, orderX(), top(), 48, 18)) {
      click(); minecraft.setScreen(new MusicPlaylistOrderScreen(parent, draft)); return true;
    }
    if (b == 0 && inside(x, y, saveX(), editButtonY(), saveWidth(), 18)) {
      click(); save(); return true;
    }
    return y < top() + headerHeight();
  }

  private boolean listClick(int x, int y, int b) {
    if (b != 0) return true;
    if (scrollbar.press(track(), x, y)) {
      scroll = (int) scrollbar.drag(track(), y, true);
      return true;
    }
    if (inside(x, y, newX(), top(), createWidth(), 18)) {
      click(); edit(null); return true;
    }
    if (inside(x, y, shuffleX(), top(), shuffleWidth(), 18)) {
      click();
      var setting = edits.setting("shuffle");
      setting.cycle(1);
      parent.changed(setting);
      return true;
    }
    if (y < rowTop() || y >= bottom()) return true;
    int index = (y - rowTop() + scroll) / rowHeight();
    var values = edits.presets();
    if (index < 0 || index >= values.size()) return true;
    int row = rowTop() + index * rowHeight() - scroll, cy = actionsY(row);
    MusicPlaylist p = values.get(index);
    boolean builtin = MusicPlaylist.BETA.equals(p.id());
    if (inside(x, y, playX(), cy, actionWidth(), 18)) {
      if (!p.id().equals(edits.selected())) { click(); edits.load(p.id()); }
    } else if (!builtin && inside(x, y, editX(), cy, actionWidth(), 18)) {
      click(); edit(p);
    } else if (inside(x, y, copyX(), cy, actionWidth(), 18)) {
      click(); edit(p, true);
    } else if (!builtin && inside(x, y, deleteX(), cy, actionWidth(), 18)) {
      click(); deleting = p.id();
    }
    return true;
  }

  private Map<String, Integer> includedCounts(List<MusicPlaylist> values) {
    int revision = AudioController.libraryRevision();
    if (values != countedPresets || revision != countedRevision) {
      Set<String> available = AudioController.music(minecraft);
      Map<String, Integer> counts = new HashMap<>();
      for (MusicPlaylist p : values)
        counts.put(p.id(), (int) available.stream().filter(p::includes).count());
      includedCounts = Map.copyOf(counts);
      countedPresets = values;
      countedRevision = revision;
    }
    return includedCounts;
  }

  void renderHeader(int mx, int my) {
    if (editing()) {
      input(name, nameX(), top(), nameWidth(), mx, my, "Playlist name");
      button("Order", orderX(), top(), 48, 18, mx, my, !draft.tracks().isEmpty());
      button("Save", saveX(), editButtonY(), saveWidth(), 18, mx, my, true);
    } else {
      button("New playlist", newX(), top(), createWidth(), 18, mx, my, true);
      button("Shuffle: " + (edits.setting("shuffle").value.getAsBoolean() ? "On" : "Off"),
          shuffleX(), top(), shuffleWidth(), 18, mx, my, true);
      scroll = (int) Math.max(0, Math.min(track().maximum(), scrollbar.drag(track(), my, Mouse.isButtonDown(0))));
      clip(left(), rowTop(), span(), Math.max(0, bottom() - rowTop()));
      var values = edits.presets();
      var counts = includedCounts(values);
      for (int i = 0; i < values.size(); i++) {
        int y = rowTop() + i * rowHeight() - scroll;
        if (y + rowHeight() <= rowTop() || y >= bottom()) continue;
        MusicPlaylist p = values.get(i);
        boolean builtin = MusicPlaylist.BETA.equals(p.id());
        boolean selected = p.id().equals(edits.selected());
        int unavailable = p.tracks().size() - counts.get(p.id());
        text(fit(p.name(), textWidth()), left() + 2, y + 5, selected ? 0xffdd88 : 0xdddddd);
        text(fit(p.tracks().size() + " tracks" + (builtin ? " / Built-in" : "")
            + (unavailable > 0 ? " / " + unavailable + " unavailable" : ""), textWidth()), left() + 2, y + 19, 0xaaaaaa);
        int cy = actionsY(y);
        if (!builtin) {
          button("Edit", editX(), cy, actionWidth(), 18, mx, my, true);
          button("Delete", deleteX(), cy, actionWidth(), 18, mx, my, true);
        }
        button("Copy", copyX(), cy, actionWidth(), 18, mx, my, true);
        button(selected ? "Active" : "Use", playX(), cy, actionWidth(), 18, mx, my, !selected);
        fill(left(), y + rowHeight() - 4, right(), y + rowHeight() - 3, 0x40555555);
      }
      unclip();
      scrollbar.render(this, track());
    }
  }

  void renderDialog(int mx, int my) {
    if (!modal()) return;
    fill(0, 0, width, height, 0xd0000000);
    int w = Math.min(312, width - 20), x = (width - w) / 2, y = height / 2 - 35;
    text(pending != null ? "Save changes to this playlist?" : "Delete this playlist?", x, y, 0xffffff);
    if (!error.isEmpty()) text(fit(error, w), x, y + 15, 0xff8888);
    else if (!deleting.isEmpty())
      text(fit(deleting.equals(edits.selected()) ? "The world playlist will return to Beta."
          : "Music files and other playlists stay unchanged.", w), x, y + 15, 0xaaaaaa);
    for (int i = 0; i < (pending != null ? 3 : 2); i++)
      button(pending != null ? new String[]{"Save", "Discard", "Cancel"}[i]
          : new String[]{"Delete", "Cancel"}[i], x + i * (w / 3), y + 36, w / 3 - 4, 20, mx, my, true);
  }

  private void dialogClick(int x, int y, int b) {
    if (b != 0) return;
    int w = Math.min(312, width - 20), left = (width - w) / 2, top = height / 2 + 1;
    for (int i = 0; i < (pending != null ? 3 : 2); i++)
      if (inside(x, y, left + i * (w / 3), top, w / 3 - 4, 20)) {
        click();
        if (pending != null) {
          Runnable next = pending;
          if (i == 0 && !save()) return;
          pending = null;
          if (i != 2) { draft = null; next.run(); }
        } else {
          if (i == 0) edits.delete(deleting);
          deleting = "";
        }
        return;
      }
  }
}
