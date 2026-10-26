package local.luke.power.ui;

import java.util.*;
import local.luke.power.audio.AudioController;
import local.luke.power.audio.MusicPlaylist;
import local.luke.power.audio.MusicLibrarySources;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Saved playlists and their isolated membership, order and group-volume drafts. */
final class MusicPresetsPanel extends UiScreen {
  private final PowerOptionsScreen parent;
  private final MusicEdits edits;
  private final ScrollBar scrollbar = new ScrollBar();
  private final TextInput name = new TextInput("", 80);
  private final SettingsDropdown choices = new SettingsDropdown();
  private MusicPresetDraft draft;
  private boolean adding, grouped;
  private String error = "", deleting = "";
  private int scroll;
  private Runnable pending;
  private List<MusicPlaylist> countedPresets;
  private int countedRevision = -1;
  private MusicLibrarySources.Snapshot countedLibrary;
  private Map<String, Integer> includedCounts = Map.of();

  MusicPresetsPanel(PowerOptionsScreen parent) {
    this.parent = parent;
    edits = new MusicEdits(parent);
  }

  MusicPresetDraft draft() { return draft; }
  boolean adding() { return editing() && adding; }
  boolean grouped() { return editing() && grouped; }
  boolean favorites() { return editing() && draft.favorites(); }
  boolean hideExcluded() { return editing() && !adding(); }
  void unfocus() { name.focused = false; }
  boolean editing() { return draft != null; }
  boolean modal() { return choices.active() || pending != null || !deleting.isEmpty(); }
  boolean dirty() { return draft != null && draft.changed(name.text()); }
  String error() { return error; }

  void open() {
    pending = null;
    deleting = "";
    name.focused = false;
    choices.close();
    scrollbar.release();
    draft = null;
    adding = grouped = false;
    scroll = 0;
    error = "";
  }

  void edit(MusicPlaylist playlist) { edit(playlist, false); }

  void showFavorites() {
    edit(edits.presets().stream().filter(p -> MusicPlaylist.FAVORITES.equals(p.id()))
        .findFirst().orElseThrow(() -> new IllegalArgumentException("Favorites are unavailable")));
  }

  boolean backFromAdd() {
    if (!adding()) return false;
    adding = false;
    choices.close();
    return true;
  }

  void appendPlaylist(String id) {
    if (!editing()) return;
    try {
      MusicPlaylist source = edits.presets().stream().filter(p -> p.id().equals(id)).findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Playlist no longer exists"));
      draft.appendTracks(source.tracks());
      error = "";
    } catch (IllegalArgumentException e) { error = e.getMessage(); }
  }

  private void edit(MusicPlaylist playlist, boolean copy) {
    if (playlist != null && MusicPlaylist.BETA.equals(playlist.id()) && !copy) return;
    draft = copy
        ? new MusicPresetDraft(edits.copy(playlist), AudioController.music(minecraft), true)
        : new MusicPresetDraft(playlist, AudioController.music(minecraft));
    name.setText(draft.name());
    name.focused = false;
    choices.close();
    adding = grouped = false;
    error = "";
  }

  void guard(Runnable action) {
    choices.close();
    name.focused = false;
    if (dirty()) pending = action;
    else { draft = null; action.run(); }
  }

  private boolean save() {
    try {
      edits.save(draft.snapshot(name.text()), draft.creating() ? null : draft.original());
      draft = null;
      adding = grouped = false;
      error = "";
      return true;
    } catch (IllegalArgumentException e) {
      error = e.getMessage();
      return false;
    }
  }

  private int left() { return parent.contentLeft(); }
  private int right() { return parent.contentRight(); }
  private int top() { return parent.audioContentTop() + 36 + MusicFilters.CONTENT_GAP; }
  private int bottom() { return parent.contentBottom(); }
  private int span() { return right() - left(); }
  private MusicEditorHeader header() { return MusicEditorHeader.at(left(), top(), span()); }
  int headerHeight() { return editing() ? header().height() : 22; }
  private int nameX() { return header().nameX(); }
  private int nameWidth() { return header().nameWidth(); }
  private int actionWidthHeader() { return header().actionWidth(); }
  private int addX() { return header().addX(); }
  private int groupedX() { return header().viewX(); }
  private int actionY() { return header().actionsY(); }
  private int createWidth() { return Math.min(94, span() - shuffleWidth() - 4); }
  private int newX() { return left(); }
  private int shuffleWidth() { return 96; }
  private int shuffleX() { return right() - shuffleWidth(); }
  private int saveX() { return header().saveX(); }
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
    if (editing() && inside(x, y, groupedX(), actionY(), actionWidthHeader(), 18))
      return adding() ? "Append tracks in another saved playlist's order. Existing tracks keep their position."
          : grouped() ? "Show the saved playback order. Drag tracks in List view to reorder them."
          : "Group tracks without changing their saved playback order.";
    return "";
  }

  private ScrollBar.Track track() {
    return new ScrollBar.Track(right() + 5, rowTop(), Math.max(0, bottom() - rowTop()),
        edits.presets().size() * rowHeight(), scroll);
  }

  void wheel(int delta) {
    if (choices.wheel(delta)) return;
    if (modal()) return;
    scroll = (int) Math.max(0, Math.min(track().maximum(), scroll - Math.signum(delta) * rowHeight()));
  }

  void tickChoices() { choices.tick(); }
  void release(int y, int button) { choices.release(y, button); }

  void key(char c, int key) {
    if (choices.key(this, key)) return;
    if (modal()) {
      if (key == Keyboard.KEY_ESCAPE) { pending = null; deleting = ""; }
      return;
    }
    if (editing() && !favorites()) name.key(c, key);
  }

  boolean headerClick(int x, int y, int b) {
    if (choices.press(this, x, y, b)) return true;
    if (modal()) { dialogClick(x, y, b); return true; }
    if (!editing()) return listClick(x, y, b);
    name.focused = !favorites() && inside(x, y, nameX(), top(), nameWidth(), 18);
    if (name.focused) name.click(x, b);
    if (b == 0 && inside(x, y, addX(), actionY(), actionWidthHeader(), 18)) {
      click();
      if (adding()) backFromAdd();
      else choices.open(List.of("From Library", "From playlist"), List.of(), 0, -1,
          addX(), actionY(), actionWidthHeader(), width, height, index -> {
            if (index == 0) adding = true;
            else choosePlaylist();
          });
      return true;
    }
    if (b == 0 && inside(x, y, groupedX(), actionY(), actionWidthHeader(), 18)) {
      click();
      if (adding()) choosePlaylist(); else grouped = !grouped;
      return true;
    }
    if (b == 0 && inside(x, y, saveX(), editButtonY(), saveWidth(), 18)) {
      click(); save(); return true;
    }
    return y < top() + headerHeight();
  }

  private void choosePlaylist() {
    MusicPresetDraft target = draft;
    List<MusicPlaylist> sources = edits.presets().stream()
        .filter(p -> !p.id().equals(target.id())).toList();
    choices.open(sources.stream().map(MusicPlaylist::name).toList(), List.of(), 0, -1,
        groupedX(), actionY(), Math.max(actionWidthHeader(), 150), width, height, index -> {
          if (draft == target) appendPlaylist(sources.get(index).id());
        });
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
    boolean reserved = MusicPlaylist.reserved(p.id());
    if (inside(x, y, playX(), cy, actionWidth(), 18)) {
      if (!p.id().equals(edits.selected())) { click(); edits.load(p.id()); }
    } else if (!builtin && inside(x, y, editX(), cy, actionWidth(), 18)) {
      click(); edit(p);
    } else if (inside(x, y, copyX(), cy, actionWidth(), 18)) {
      click(); edit(p, true);
    } else if (!reserved && inside(x, y, deleteX(), cy, actionWidth(), 18)) {
      click(); deleting = p.id();
    }
    return true;
  }

  private Map<String, Integer> includedCounts(List<MusicPlaylist> values) {
    int revision = AudioController.libraryRevision();
    var library = AudioController.sourceLibrary();
    if (values != countedPresets || revision != countedRevision || library != countedLibrary) {
      Set<String> available = AudioController.music(minecraft);
      Map<String, Integer> counts = new HashMap<>();
      for (MusicPlaylist p : values)
        counts.put(p.id(), (int) p.tracks().stream().filter(available::contains).count());
      includedCounts = Map.copyOf(counts);
      countedPresets = values;
      countedRevision = revision; countedLibrary = library;
    }
    return includedCounts;
  }

  void renderHeader(int mx, int my) {
    if (editing()) {
      if (favorites()) text("Favorites", nameX() + 2, top() + 5, 0xdddddd);
      else input(name, nameX(), top(), nameWidth(), mx, my, "Playlist name");
      button("Save", saveX(), editButtonY(), saveWidth(), 18, mx, my, true);
      button(adding() ? "Back to playlist" : "Add songs", addX(), actionY(), actionWidthHeader(), 18, mx, my, true);
      button(adding() ? "From playlist" : grouped() ? "List" : "Grouped",
          groupedX(), actionY(), actionWidthHeader(), 18, mx, my, true);
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
        boolean reserved = MusicPlaylist.reserved(p.id());
        boolean selected = p.id().equals(edits.selected());
        int unavailable = p.tracks().size() - counts.get(p.id());
        text(fit(p.name(), textWidth()), left() + 2, y + 5, selected ? 0xffdd88 : 0xdddddd);
        text(fit(p.tracks().size() + (p.tracks().size() == 1 ? " track" : " tracks") + (builtin ? " / Built-in" : "")
            + (unavailable > 0 ? " / " + unavailable + " unavailable" : ""), textWidth()), left() + 2, y + 19, 0xaaaaaa);
        int cy = actionsY(y);
        if (!builtin) {
          button("Edit", editX(), cy, actionWidth(), 18, mx, my, true);
        }
        if (!reserved) button("Delete", deleteX(), cy, actionWidth(), 18, mx, my, true);
        button("Copy", copyX(), cy, actionWidth(), 18, mx, my, true);
        button(selected ? "Active" : "Use", playX(), cy, actionWidth(), 18, mx, my, !selected);
        fill(left(), y + rowHeight() - 4, right(), y + rowHeight() - 3, 0x40555555);
      }
      unclip();
      scrollbar.render(this, track());
    }
  }

  void renderDialog(int mx, int my) {
    if (choices.active()) { choices.render(this, mx, my); return; }
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
