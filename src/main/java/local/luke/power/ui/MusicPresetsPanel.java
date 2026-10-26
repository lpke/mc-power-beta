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
  private MusicPlaylist sourcePlaylist;
  private boolean adding, grouped;
  private String error = "", deleting = "";
  private int scroll;
  private String query = "";
  private List<MusicPlaylist> filteredSource, filteredPresets = List.of();
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
  boolean editable() { return editing() && !draft.readOnly(); }
  boolean readOnly() { return editing() && draft.readOnly(); }
  boolean modal() { return choices.active() || pending != null || !deleting.isEmpty(); }
  boolean dirty() { return editable() && draft.changed(name.text()); }
  String error() { return error; }

  void open() {
    pending = null;
    deleting = "";
    name.focused = false;
    choices.close();
    scrollbar.release();
    draft = null; sourcePlaylist = null;
    adding = grouped = false;
    scroll = 0;
    error = "";
  }

  void edit(MusicPlaylist playlist) { edit(playlist, false); }

  void newPlaylist() { edit(null); }

  void filter(String value) {
    String next = value.strip().toLowerCase(Locale.ROOT);
    if (next.equals(query)) return;
    query = next;
    filteredSource = null;
    resetScroll();
  }

  void resetScroll() { scroll = 0; scrollbar.release(); }

  List<MusicPlaylist> filteredPresets() {
    List<MusicPlaylist> source = edits.presets();
    if (source != filteredSource) {
      filteredPresets = filterPresets(source, query);
      filteredSource = source;
    }
    return filteredPresets;
  }

  static List<MusicPlaylist> filterPresets(List<MusicPlaylist> source, String query) {
    String match = query.strip().toLowerCase(Locale.ROOT);
    return match.isEmpty() ? source : source.stream()
        .filter(p -> p.name().toLowerCase(Locale.ROOT).contains(match)).toList();
  }

  void showSelected() {
    edit(edits.presets().stream().filter(p -> p.id().equals(edits.selected())).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("World playlist is unavailable")));
  }

  /** Refresh saved selection changes only after this editor has finished its own draft. */
  void syncSelected() {
    if (dirty() || modal()) return;
    MusicPlaylist selected = edits.presets().stream().filter(p -> p.id().equals(edits.selected()))
        .findFirst().orElse(null);
    if (selected == null || draft != null && selected == sourcePlaylist) return;
    if (draft == null || !samePlaylist(draft.original(), selected)) {
      boolean retainGrouping = draft != null && draft.id().equals(selected.id()) && grouped;
      edit(selected);
      grouped = retainGrouping;
    } else sourcePlaylist = selected;
  }

  private static boolean samePlaylist(MusicPlaylist first, MusicPlaylist second) {
    return first.id().equals(second.id()) && first.name().equals(second.name())
        && first.tracks().equals(second.tracks()) && first.groupVolumes().equals(second.groupVolumes());
  }

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
    if (!editable()) return;
    try {
      MusicPlaylist source = edits.presets().stream().filter(p -> p.id().equals(id)).findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Playlist no longer exists"));
      draft.appendTracks(source.tracks());
      error = "";
    } catch (IllegalArgumentException e) { error = e.getMessage(); }
  }

  private void edit(MusicPlaylist playlist, boolean copy) {
    sourcePlaylist = copy ? null : playlist;
    draft = copy
        ? new MusicPresetDraft(edits.copy(playlist), AudioController.music(minecraft), true)
        : playlist != null && MusicPlaylist.BETA.equals(playlist.id()) ? MusicPresetDraft.readOnly(playlist)
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

  boolean savePlaylist() {
    if (!dirty()) return false;
    try {
      edits.save(draft.snapshot(name.text()), draft.creating() ? null : draft.original());
      cancelPlaylist();
      return true;
    } catch (IllegalArgumentException e) {
      error = e.getMessage();
      return false;
    }
  }

  void cancelPlaylist() {
    draft = null;
    adding = grouped = false;
    name.focused = false;
    choices.close();
    pending = null;
    deleting = "";
    error = "";
  }

  private int left() { return parent.contentLeft(); }
  private int right() { return parent.contentRight(); }
  private int top() { return parent.musicEditorTop(); }
  private int bottom() { return parent.contentBottom(); }
  private int span() { return right() - left(); }
  private String addLabel() { return adding() ? height < 300 ? "Back" : "Back to playlist" : "Add songs"; }
  private String viewLabel() { return adding() ? "From playlist" : "View: " + (grouped() ? "Grouped" : "List"); }
  private MusicEditorHeader header() {
    return MusicEditorHeader.at(left(), top(), span(), editable() ? textWidth(addLabel()) + 16 : 0,
        textWidth(viewLabel()) + 16, height < 300 ? 32 : editable() ? 100 : 40);
  }
  int headerHeight() { return editing() ? header().height() : 0; }
  private int nameX() { return header().nameX(); }
  private int nameWidth() { return header().nameWidth(); }
  private int addWidth() { return header().addWidth(); }
  private int viewWidth() { return header().viewWidth(); }
  private int addX() { return header().addX(); }
  private int groupedX() { return header().viewX(); }
  private int actionY() { return header().actionsY(); }
  private int rowTop() { return top() + headerHeight() + 4; }
  private int actionsWidth() { return 4 * (actionWidth() + 2) - 2; }
  private boolean inlineRows() { return span() >= actionsWidth() + 140; }
  private int rowHeight() { return inlineRows() ? 28 : 48; }
  private int actionsY(int row) { return row + (inlineRows() ? 5 : 24); }
  private int textWidth() { return span() - (inlineRows() ? actionsWidth() + 8 : 4); }
  private int actionWidth() { return span() < 260 ? 40 : 48; }
  private int editX() { return right() - 4 * (actionWidth() + 2) + 2; }
  private int copyX() { return right() - 3 * (actionWidth() + 2) + 2; }
  private int deleteX() { return right() - 2 * (actionWidth() + 2) + 2; }
  private int playX() { return right() - actionWidth(); }

  private ScrollBar.Track track() {
    return new ScrollBar.Track(right() + 5, rowTop(), Math.max(0, bottom() - rowTop()),
        filteredPresets().size() * rowHeight(), scroll);
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
    if (editable() && !favorites()) name.key(c, key);
  }

  boolean headerClick(int x, int y, int b) {
    if (choices.press(this, x, y, b)) return true;
    if (modal()) { dialogClick(x, y, b); return true; }
    if (!editing()) return listClick(x, y, b);
    name.focused = editable() && !favorites() && inside(x, y, nameX(), top(), nameWidth(), 18);
    if (name.focused) name.click(x, b);
    if (b == 0 && editable() && inside(x, y, addX(), actionY(), addWidth(), 18)) {
      click();
      if (adding()) backFromAdd();
      else choices.open("playlist-add", List.of("From Library", "From playlist"), List.of(), -1, -1,
          addX(), actionY(), addWidth(), 18, width, height, index -> {
            if (index == 0) adding = true;
            else choosePlaylist();
          });
      return true;
    }
    if (b == 0 && inside(x, y, groupedX(), actionY(), viewWidth(), 18)) {
      click();
      if (adding()) choosePlaylist(); else grouped = !grouped;
      return true;
    }
    return y < top() + headerHeight();
  }

  private void choosePlaylist() {
    MusicPresetDraft target = draft;
    List<MusicPlaylist> sources = edits.presets().stream()
        .filter(p -> !p.id().equals(target.id())).toList();
    choices.open(adding() ? "playlist-source" : "playlist-add",
        sources.stream().map(MusicPlaylist::name).toList(), List.of(), -1, -1,
        adding() ? groupedX() : addX(), actionY(), Math.max(adding() ? viewWidth() : addWidth(), 150),
        18, width, height, index -> {
          if (draft == target) appendPlaylist(sources.get(index).id());
        });
  }

  private boolean listClick(int x, int y, int b) {
    if (b != 0) return true;
    if (scrollbar.press(track(), x, y)) {
      scroll = (int) scrollbar.drag(track(), y, true);
      return true;
    }
    if (y < rowTop() || y >= bottom()) return true;
    int index = (y - rowTop() + scroll) / rowHeight();
    var values = filteredPresets();
    if (index < 0 || index >= values.size()) return true;
    int row = rowTop() + index * rowHeight() - scroll, cy = actionsY(row);
    MusicPlaylist p = values.get(index);
    boolean reserved = MusicPlaylist.reserved(p.id());
    if (inside(x, y, playX(), cy, actionWidth(), 18)) {
      if (!p.id().equals(edits.selected())) { click(); edits.load(p.id()); }
    } else if (inside(x, y, editX(), cy, actionWidth(), 18)) {
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

  private String trackCount(MusicPlaylist playlist) {
    int count = playlist.tracks().size();
    return count + (count == 1 ? " track" : " tracks");
  }

  private void renderCatalogName(MusicPlaylist playlist, int missing, int y, boolean selected) {
    String count = trackCount(playlist);
    String detail = count + (missing > 0 ? " / " + missing + " unavailable" : "");
    if (textWidth(detail) + 72 <= textWidth()) count = detail;
    int nameSpan = Math.max(12, textWidth() - textWidth(count) - 14);
    String label = fit(playlist.name(), nameSpan);
    if (selected) rectangle(left() + 1, y - 1, left() + 3, y + 9, 0xffffdd66);
    text(label, left() + 8, y, selected ? 0xffffdd66 : 0xdddddd);
    text(count, left() + 8 + textWidth(label) + 8, y, 0xaaaaaa);
  }

  void renderHeader(int mx, int my) {
    if (editing()) {
      if (favorites() || readOnly()) text(fit(draft.name(), nameWidth()), nameX() + 2, top() + 5, 0xdddddd);
      else input(name, nameX(), top(), nameWidth(), mx, my, "Playlist name");
      if (editable()) button(fit(addLabel(), addWidth() - 8), addX(), actionY(), addWidth(), 18, mx, my, true,
          choices.editing("playlist-add") ? 0xffffa0 : -1);
      button(fit(viewLabel(), viewWidth() - 8), groupedX(), actionY(), viewWidth(), 18, mx, my, true,
          choices.editing("playlist-source") ? 0xffffa0 : -1);
    } else {
      scroll = (int) Math.max(0, Math.min(track().maximum(), scrollbar.drag(track(), my, Mouse.isButtonDown(0))));
      clip(left(), rowTop(), span(), Math.max(0, bottom() - rowTop()));
      var values = filteredPresets();
      var counts = includedCounts(values);
      for (int i = 0; i < values.size(); i++) {
        int y = rowTop() + i * rowHeight() - scroll;
        if (y + rowHeight() <= rowTop() || y >= bottom()) continue;
        MusicPlaylist p = values.get(i);
        boolean builtin = MusicPlaylist.BETA.equals(p.id());
        boolean reserved = MusicPlaylist.reserved(p.id());
        boolean selected = p.id().equals(edits.selected());
        int unavailable = p.tracks().size() - counts.get(p.id());
        renderCatalogName(p, unavailable, y + (inlineRows() ? 10 : 7), selected);
        int cy = actionsY(y);
        button(builtin ? "View" : "Edit", editX(), cy, actionWidth(), 18, mx, my, true);
        button("Delete", deleteX(), cy, actionWidth(), 18, mx, my, !reserved);
        button("Copy", copyX(), cy, actionWidth(), 18, mx, my, true);
        button(selected ? "Active" : "Use", playX(), cy, actionWidth(), 18, mx, my, !selected);
        fill(left(), y + rowHeight() - 4, right(), y + rowHeight() - 3, 0x40555555);
      }
      if (values.isEmpty()) text("No matching playlists", left() + 4, rowTop() + 12, 0xaaaaaa);
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
      text(fit(deleting.equals(edits.selected()) ? "The world playlist will return to Beta 1.7.3."
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
          if (i == 0 && !savePlaylist()) return;
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
