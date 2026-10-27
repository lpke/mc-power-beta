package local.luke.power.ui;

import java.util.*;
import local.luke.power.audio.AudioController;
import local.luke.power.audio.MusicPlaylist;
import local.luke.power.audio.MusicLibrarySources;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Saved playlists and their isolated membership and order drafts. */
final class MusicPresetsPanel extends UiScreen {
  private final PowerOptionsScreen parent;
  private final MusicEdits edits;
  private final ScrollBar scrollbar = new ScrollBar();
  private final TextInput name = new TextInput("", 80);
  private final TextInput rename = new TextInput("", 80);
  private final MusicListDrag reorder = new MusicListDrag();
  private MusicPlaylist renaming, pressedPlaylist;
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
  void unfocus() { name.focused = false; cancelRename(); }
  boolean dragging() { return reorder.active(); }
  boolean inlineRename() { return renaming != null; }
  void cancelPointer() { reorder.cancel(); pressedPlaylist = null; scrollbar.release(); }
  boolean editing() { return draft != null; }
  boolean editable() { return editing() && !draft.readOnly(); }
  boolean readOnly() { return editing() && draft.readOnly(); }
  boolean modal() { return choices.active() || pending != null || !deleting.isEmpty() || renaming != null; }
  boolean dirty() { return editable() && draft.changed(name.text()); }
  String error() { return error; }

  void open() {
    cancelPointer(); cancelRename();
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

  void newPlaylist() { edit(null); }

  void filter(String value) {
    String next = value.strip().toLowerCase(Locale.ROOT);
    if (next.equals(query)) return;
    cancelPointer(); cancelRename();
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
    return MusicSearch.filter(source, query, MusicPlaylist::name);
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
        && first.tracks().equals(second.tracks());
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

  void edit(MusicPlaylist playlist) {
    cancelPointer(); cancelRename();
    sourcePlaylist = playlist;
    draft = playlist != null && MusicPlaylist.BETA.equals(playlist.id()) ? MusicPresetDraft.readOnly(playlist)
        : new MusicPresetDraft(playlist, AudioController.music(minecraft));
    name.setText(draft.name());
    name.focused = false;
    choices.close();
    adding = grouped = false;
    error = "";
  }

  void guard(Runnable action) {
    choices.close(); cancelPointer(); cancelRename();
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
    cancelPointer(); cancelRename();
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
  private MusicCatalogLayout catalogLayout() {
    return MusicCatalogLayout.at(left(), span(), textWidth("Duplicate") + 16);
  }
  private int rowHeight() { return catalogLayout().rowHeight(); }

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
  boolean release(int x, int y, int button) {
    choices.release(y, button);
    if (button != 0 || editing()) return false;
    scroll = (int) scrollbar.drag(track(), y, true); scrollbar.release();
    if (pressedPlaylist == null) return false;
    MusicPlaylist pressed = pressedPlaylist;
    try {
      if (modal() || !org.lwjgl.opengl.Display.isActive()
          || !inside(x, y, left(), rowTop(), span(), bottom() - rowTop())) return true;
      if (reorder.active()) reorder.update(x, y);
      if (reorder.active() && reorder.moved()) {
        List<String> next = new ArrayList<>(reorder.original());
        int slot = reorder.slot(y - rowTop() + scroll);
        if (ListReorder.move(next, reorder.index(), slot)) edits.reorderPresets(reorder.original(), next);
      } else {
        var values = filteredPresets();
        int index = (y - rowTop() + scroll) / rowHeight();
        if (index >= 0 && index < values.size() && values.get(index) == pressed) { click(); edit(pressed); }
      }
    } catch (IllegalArgumentException failure) { error = failure.getMessage(); }
    finally { cancelPointer(); }
    return true;
  }

  void key(char c, int key) {
    if (choices.key(this, key)) return;
    if (renaming != null) {
      if (key == Keyboard.KEY_ESCAPE) cancelRename();
      else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) commitRename();
      else { rename.key(c, key); error = ""; }
      return;
    }
    if (reorder.active() && key == Keyboard.KEY_ESCAPE) { cancelPointer(); return; }
    if (modal()) {
      if (key == Keyboard.KEY_ESCAPE) { pending = null; deleting = ""; }
      return;
    }
    if (editable() && !favorites()) name.key(c, key);
  }

  boolean headerClick(int x, int y, int b) {
    if (choices.press(this, x, y, b)) return true;
    if (renaming != null) { renameClick(x, y, b); return true; }
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
    if (b != 0 && b != 1) return true;
    cancelPointer();
    if (b == 0 && scrollbar.press(track(), x, y)) {
      scroll = (int) scrollbar.drag(track(), y, true);
      return true;
    }
    if (!inside(x, y, left(), rowTop(), span(), bottom() - rowTop())) return true;
    int index = (y - rowTop() + scroll) / rowHeight();
    var values = filteredPresets();
    if (index < 0 || index >= values.size()) return true;
    int row = rowTop() + index * rowHeight() - scroll;
    MusicPlaylist playlist = values.get(index);
    if (b == 1) { context(playlist, x, y); return true; }
    var layout = catalogLayout();
    int cy = layout.controlsY(row);
    if (inside(x, y, layout.useX(), row + 5, layout.useWidth(), 18)) {
      if (!playlist.id().equals(edits.selected())) { click(); act(() -> edits.load(playlist.id())); }
    } else if (inside(x, y, layout.duplicateX(), cy, layout.duplicateWidth(), 18)) {
      click(); duplicate(playlist);
    } else if (inside(x, y, layout.deleteX(), cy, 20, 18)) {
      if (!MusicPlaylist.reserved(playlist.id())) { click(); deleting = playlist.id(); }
    } else {
      pressedPlaylist = playlist;
      if (query.isBlank()) {
        var ids = values.stream().map(MusicPlaylist::id).toList();
        var slots = java.util.stream.IntStream.range(0, values.size())
            .mapToObj(i -> new MusicListDrag.Slot(ids.get(i), i * rowHeight(), rowHeight())).toList();
        reorder.begin(MusicListDrag.Kind.CATALOG, playlist, ids, slots, index, x, y);
      }
    }
    return true;
  }

  private void context(MusicPlaylist playlist, int x, int y) {
    boolean editable = !MusicPlaylist.reserved(playlist.id());
    parent.musicActions(playlist, List.of("Play", "Rename", "Duplicate", "Delete"),
        List.of(!playlist.tracks().isEmpty(), editable, true, editable), x, y, action -> {
          if (editing()) return;
          switch (action) {
            case 0 -> act(() -> edits.play(playlist.id()));
            case 1 -> beginRename(playlist);
            case 2 -> duplicate(playlist);
            case 3 -> deleting = playlist.id();
          }
        });
  }

  private void act(Runnable action) {
    try { action.run(); error = ""; }
    catch (IllegalArgumentException failure) { error = failure.getMessage(); }
  }
  private void duplicate(MusicPlaylist playlist) { act(() -> edits.save(edits.copy(playlist))); }
  private void beginRename(MusicPlaylist playlist) {
    if (MusicPlaylist.reserved(playlist.id())) return;
    cancelPointer(); renaming = playlist; rename.setText(playlist.name());
    rename.focused = true; rename.selectAll(); error = "";
  }
  private void cancelRename() {
    if (renaming != null) error = "";
    renaming = null; rename.focused = false;
  }
  private void commitRename() {
    if (renaming == null) return;
    if (rename.text().strip().equals(renaming.name())) { cancelRename(); return; }
    try { edits.rename(renaming, rename.text()); click(); cancelRename(); error = ""; }
    catch (IllegalArgumentException failure) { error = failure.getMessage(); }
  }
  private void renameClick(int x, int y, int button) {
    var values = filteredPresets();
    int index = values.indexOf(renaming);
    if (index < 0) { cancelRename(); return; }
    int row = rowTop() + index * rowHeight() - scroll;
    var layout = catalogLayout();
    int inputWidth = layout.nameWidth() - 24;
    if (inside(x, y, layout.nameX(), row + 5, inputWidth, 18)) {
      rename.focused = true; rename.click(x, button);
    } else if (button == 0 && inside(x, y, layout.nameX() + inputWidth + 4, row + 5, 20, 18)) {
      if (!rename.text().strip().equals(renaming.name())) commitRename();
    } else cancelRename();
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
    var layout = catalogLayout();
    String count = trackCount(playlist);
    String detail = count + (missing > 0 ? " / " + missing + " unavailable" : "");
    if (textWidth(detail) + 72 <= layout.nameWidth()) count = detail;
    if (textWidth(count) + 48 > layout.nameWidth()) count = "";
    int nameSpan = Math.max(12, layout.nameWidth() - (count.isEmpty() ? 0 : textWidth(count) + 8));
    String label = fit(playlist.name(), nameSpan);
    text(label, layout.nameX(), y, selected ? 0xffffdd66 : 0xffffff);
    if (!count.isEmpty()) text(count, layout.nameX() + textWidth(label) + 8, y, 0xaaaaaa);
  }

  private void renderUse(boolean selected, int row, int mx, int my) {
    var layout = catalogLayout();
    int colour = selected ? 0xffffdd66 : inside(mx, my, layout.useX(), row + 5, layout.useWidth(), 18)
        ? 0xffffa0 : 0xdddddd;
    RadioButton.draw(layout.useX() + layout.useWidth() / 2d, row + 14, selected, colour);
  }

  void renderHeader(int mx, int my) {
    if (!org.lwjgl.opengl.Display.isActive()) cancelPointer();
    if (editing()) {
      if (favorites() || readOnly()) text(fit(draft.name(), nameWidth()), nameX() + 2, top() + 5, 0xdddddd);
      else input(name, nameX(), top(), nameWidth(), mx, my, "Playlist name");
      if (editable()) button(fit(addLabel(), addWidth() - 8), addX(), actionY(), addWidth(), 18, mx, my, true,
          choices.editing("playlist-add") ? 0xffffa0 : -1);
      button(fit(viewLabel(), viewWidth() - 8), groupedX(), actionY(), viewWidth(), 18, mx, my, true,
          choices.editing("playlist-source") ? 0xffffa0 : -1);
    } else {
      int hoverX = modal() ? -1000 : mx, hoverY = modal() ? -1000 : my;
      scroll = (int) Math.max(0, Math.min(track().maximum(), scrollbar.drag(track(), my, Mouse.isButtonDown(0))));
      reorder.update(mx, my);
      if (reorder.active() && reorder.moved() && inside(mx, my, left(), rowTop(), span(), bottom() - rowTop()) && reorder.scrollDue()) {
        if (my < rowTop() + 14) scroll = Math.max(0, scroll - rowHeight());
        else if (my >= bottom() - 14) scroll = (int) Math.min(track().maximum(), scroll + rowHeight());
      }
      clip(left(), rowTop(), span(), Math.max(0, bottom() - rowTop()));
      var values = filteredPresets();
      var counts = includedCounts(values);
      var layout = catalogLayout();
      for (int i = 0; i < values.size(); i++) {
        int y = rowTop() + i * rowHeight() - scroll;
        if (y + rowHeight() <= rowTop() || y >= bottom()) continue;
        MusicPlaylist playlist = values.get(i);
        boolean hovered = inside(hoverX, hoverY, left(), y, span(), rowHeight())
            && hoverY >= rowTop() && hoverY < bottom();
        if (hovered || parent.musicActionsOpen(playlist)) fill(left(), y, right(), y + rowHeight(), 0x60000000);
        boolean selected = playlist.id().equals(edits.selected());
        renderUse(selected, y, hoverX, hoverY);
        if (playlist == renaming) {
          int inputWidth = layout.nameWidth() - 24;
          input(rename, layout.nameX(), y + 5, inputWidth, mx, my, "Playlist name");
          iconButton("check", layout.nameX() + inputWidth + 4, y + 5, 20, mx, my,
              !rename.text().strip().equals(playlist.name()));
        } else renderCatalogName(playlist, playlist.tracks().size() - counts.get(playlist.id()), y + 10, selected);
        int cy = layout.controlsY(y);
        button("Duplicate", layout.duplicateX(), cy, layout.duplicateWidth(), 18, hoverX, hoverY, true);
        iconButton("remove", layout.deleteX(), cy, 20, hoverX, hoverY, !MusicPlaylist.reserved(playlist.id()));
        fill(left(), y + rowHeight() - 1, right(), y + rowHeight(), 0x40555555);
      }
      if (reorder.active() && reorder.moved() && inside(mx, my, left(), rowTop(), span(), bottom() - rowTop())) {
        int line = rowTop() + reorder.lineY(reorder.slot(my - rowTop() + scroll)) - scroll;
        fill(left(), Math.max(rowTop(), line - 1), right(), Math.min(bottom(), line + 1), 0xff268bff);
      }
      if (values.isEmpty()) text("No matching playlists", left() + 4, rowTop() + 12, 0xaaaaaa);
      unclip();
      scrollbar.render(this, track());
    }
  }

  void renderDialog(int mx, int my) {
    if (choices.active()) { choices.render(this, mx, my); return; }
    if (!modal() || renaming != null) return;
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
