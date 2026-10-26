package local.luke.power.ui;

import java.util.*;
import local.luke.power.audio.AudioController;
import local.luke.power.audio.MusicPlaylist;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Playlist list and editor header, embedded in the same Audio content area. */
final class MusicPresetsPanel extends UiScreen {
  private final PowerOptionsScreen parent;
  private final MusicEdits edits;
  private final ScrollBar scrollbar = new ScrollBar();
  private final TextInput name = new TextInput("", 80);
  private MusicPresetDraft draft;
  private boolean hideExcluded;

  boolean hideExcluded() {
    return hideExcluded;
  }

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

  MusicPresetDraft draft() {
    return draft;
  }

  void unfocus() {
    name.focused = false;
  }

  boolean editing() {
    return draft != null;
  }

  boolean modal() {
    return pending != null || !deleting.isEmpty();
  }

  boolean dirty() {
    return draft != null && draft.changed(name.text());
  }

  void open() {
    pending = null;
    deleting = "";
    name.focused = false;
    scrollbar.release();
    draft = null;
    scroll = 0;
    error = "";
  }

  void edit(MusicPlaylist preset) {
    draft = new MusicPresetDraft(preset, AudioController.music(minecraft));
    hideExcluded = !draft.creating();
    name.setText(draft.name());
    name.focused = false;
    error = "";
  }

  void guard(Runnable action) {
    if (dirty()) pending = action;
    else {
      draft = null;
      action.run();
    }
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

  private int left() {
    return parent.contentLeft();
  }

  private int right() {
    return parent.contentRight();
  }

  private int top() {
    return parent.audioContentTop() + 22;
  }

  private int bottom() {
    return parent.contentBottom();
  }

  private int span() {
    return right() - left();
  }

  int headerHeight() {
    return editing() ? (span() >= 640 ? 26 : 48) : 50;
  }

  private String actionLabel() {
    return draft.creating() ? "Creating" : "Editing";
  }

  private int nameX() {
    return left() + textRenderer.getWidth(actionLabel()) + 6;
  }

  private int nameWidth() {
    return right() - nameX() - (span() >= 640 ? 378 : 52);
  }

  private int orderX() { return span() >= 640 ? right() - 372 : right() - 48; }

  private int createWidth() { return Math.min(150, Math.max(60, span() - 114)); }

  private int saveX() {
    return span() >= 640 ? right() - 320 : left();
  }

  private int hideX() {
    return saveX() + saveWidth() + 4;
  }

  private int saveWidth() { return span() < 326 ? 40 : 62; }
  private int bulkWidth() { return span() < 326 ? 34 : 72; }
  private int hideWidth() { return span() < 326 ? span() - saveWidth() - 2 * bulkWidth() - 12 : 102; }

  private int editButtonY() {
    return top() + (span() >= 640 ? 0 : 22);
  }

  private int includeX() {
    return right() - 2 * bulkWidth() - 4;
  }

  private int excludeX() {
    return right() - bulkWidth();
  }

  private int bulkY() {
    return editButtonY();
  }

  private int rowTop() {
    return top() + headerHeight();
  }

  private int rowHeight() {
    return span() < 360 ? 60 : 40;
  }

  private int actionsY(int row) {
    return row + (span() < 360 ? 34 : 7);
  }

  private int textWidth() {
    return span() - (span() < 360 ? 4 : 178);
  }

  private ScrollBar.Track track() {
    return new ScrollBar.Track(
        right() + 5,
        rowTop(),
        Math.max(0, bottom() - rowTop()),
        (edits.presets().size() + 1) * rowHeight(),
        scroll);
  }

  void wheel(int delta) {
    scroll =
        (int) Math.max(0, Math.min(track().maximum(), scroll - Math.signum(delta) * rowHeight()));
  }

  void key(char c, int key) {
    if (modal()) {
      if (key == Keyboard.KEY_ESCAPE) {
        pending = null;
        deleting = "";
      }
      return;
    }
    if (editing()) name.key(c, key);
  }

  boolean headerClick(int x, int y, int b) {
    if (modal()) {
      dialogClick(x, y, b);
      return true;
    }
    if (!editing()) return listClick(x, y, b);
    name.focused = inside(x, y, nameX(), top(), nameWidth(), 18);
    if (name.focused) name.click(x, b);
    if (b == 0 && !draft.tracks().isEmpty() && inside(x,y,orderX(),top(),48,18)) {
      click(); minecraft.setScreen(new MusicPlaylistOrderScreen(parent,draft)); return true;
    }
    if (b == 0 && inside(x, y, saveX(), editButtonY(), saveWidth(), 18)) {
      save();
      return true;
    }
    if (inside(x, y, hideX(), editButtonY(), hideWidth(), 18)) {
      hideExcluded = !hideExcluded;
      return true;
    }
    if (inside(x, y, includeX(), bulkY(), bulkWidth(), 18)) {
      draft.includeAll();
      return true;
    }
    if (inside(x, y, excludeX(), bulkY(), bulkWidth(), 18)) {
      try {
        draft.excludeAll(AudioController.music(minecraft));
        error = "";
      } catch (IllegalArgumentException e) {
        error = e.getMessage();
      }
      return true;
    }
    return y < top() + headerHeight();
  }

  private boolean listClick(int x, int y, int b) {
    if (b != 0) return true;
    if (scrollbar.press(track(), x, y)) {
      scroll = (int) scrollbar.drag(track(), y, true);
      return true;
    }
    if (inside(x, y, left(), top(), createWidth(), 20)) {
      edit(null);
      return true;
    }
    if (inside(x,y,right()-108,top(),108,20)) {
      List<String> choices = edits.selected().isEmpty() ? List.of("Save as new playlist")
          : List.of("Update " + edits.name(edits.selected()), "Save as new playlist");
      parent.choose(choices,List.of(),-1,right()-108,top(),108,index ->
          edit(edits.current(edits.selected().isEmpty() || index==1)));
      return true;
    }
    if (y < rowTop() || y >= bottom()) return true;
    int index = (y - rowTop() + scroll) / rowHeight();
    int row = rowTop() + index * rowHeight() - scroll;
    int cy = actionsY(row);
    if (index == 0) {
      if (inside(x, y, right() - 54, cy, 54, 18)) {
        edits.load("");
        parent.closeAudioPanel();
      }
      return true;
    }
    var values = edits.presets();
    if (index > values.size()) return true;
    MusicPlaylist p = values.get(index - 1);
    if (inside(x, y, right() - 54, cy, 54, 18)) {
      edits.load(p.id());
      parent.closeAudioPanel();
    } else if (inside(x, y, right() - 170, cy, 54, 18)) edit(p);
    else if (inside(x, y, right() - 112, cy, 54, 18)) deleting = p.id();
    return true;
  }

  private Map<String, Integer> includedCounts(List<MusicPlaylist> values) {
    int revision = AudioController.libraryRevision();
    if (values != countedPresets || revision != countedRevision) {
      Set<String> available = AudioController.music(minecraft);
      Map<String, Integer> counts = new HashMap<>();
      for (MusicPlaylist p : values)
        counts.put(
            p.id(),
            (int) available.stream().filter(p::includes).count());
      includedCounts = Map.copyOf(counts);
      countedPresets = values;
      countedRevision = revision;
    }
    return includedCounts;
  }

  void renderHeader(int mx, int my) {
    if (editing()) {
      button("Order",orderX(),top(),48,18,mx,my,!draft.tracks().isEmpty());
      text(actionLabel(), left(), top() + 5, 0xffffff);
      input(name, nameX(), top(), nameWidth(), mx, my, "Playlist name");
      button("Save", saveX(), editButtonY(), saveWidth(), 18, mx, my, true);
      button(
          fit(hideExcluded ? "Show excluded" : "Hide excluded", hideWidth() - 4),
          hideX(),
          editButtonY(),
          hideWidth(),
          18,
          mx,
          my,
          true);
      button(span() < 326 ? "All" : "Include all", includeX(), bulkY(), bulkWidth(), 18, mx, my, true);
      button(span() < 326 ? "None" : "Exclude all", excludeX(), bulkY(), bulkWidth(), 18, mx, my, true);
    } else {
      button("New playlist", left(), top(), createWidth(), 20, mx, my, true);
      button("Save current",right()-108,top(),108,20,mx,my,true);
      text(
          fit("Load copies tracks and group volumes into the current selection.", span()),
          left(),
          top() + 28,
          0xaaaaaa);
      scroll =
          (int)
              Math.max(
                  0,
                  Math.min(track().maximum(), scrollbar.drag(track(), my, Mouse.isButtonDown(0))));
      clip(left(), rowTop(), span(), Math.max(0, bottom() - rowTop()));
      var presets = edits.presets();
      var counts = includedCounts(presets);
      for (int i = 0; i <= presets.size(); i++) {
        int y = rowTop() + i * rowHeight() - scroll;
        if (y + rowHeight() <= rowTop() || y >= bottom()) continue;
        MusicPlaylist p = i == 0 ? null : presets.get(i - 1);
        text(fit(p == null ? "Default rotation" : p.name(), textWidth()), left() + 2, y + 5, 0xdddddd);
        text(
            fit(
                p == null
                    ? "Use soundtrack settings and default exclusions"
                    : p.tracks().size() + " tracks" + (counts.get(p.id()) < p.tracks().size() ? ", " + (p.tracks().size()-counts.get(p.id())) + " unavailable" : ""),
                textWidth()),
            left() + 2,
            y + 19,
            0xaaaaaa);
        button("Load", right() - 54, actionsY(y), 54, 18, mx, my, true);
        if (p != null) {
          button("Edit", right() - 170, actionsY(y), 54, 18, mx, my, true);
          button("Delete", right() - 112, actionsY(y), 54, 18, mx, my, true);
        }
        fill(left(), y + rowHeight() - 4, right(), y + rowHeight() - 3, 0x40555555);
      }
      unclip();
      scrollbar.render(this, track());
    }
    if (!error.isEmpty()) text(fit(error, span()), left(), bottom() - 10, 0xff8888);
  }

  void renderDialog(int mx, int my) {
    if (!modal()) return;
    fill(0, 0, width, height, 0xd0000000);
    int w = Math.min(312, width - 20), x = (width - w) / 2, y = height / 2 - 35;
    text(pending != null ? "Save changes to this playlist?" : "Delete this playlist?", x, y, 0xffffff);
    if (!error.isEmpty()) text(fit(error, w), x, y + 15, 0xff8888);
    else if (!deleting.isEmpty())
      text(fit(deleting.equals(edits.selected()) ? "World selection will return to Default rotation."
          : "Music files and other playlists stay unchanged.",w), x, y + 15, 0xaaaaaa);
    for (int i = 0; i < (pending != null ? 3 : 2); i++)
      button(
          pending != null
              ? new String[] {"Save", "Discard", "Cancel"}[i]
              : new String[] {"Delete", "Cancel"}[i],
          x + i * (w / 3),
          y + 36,
          w / 3 - 4,
          20,
          mx,
          my,
          true);
  }

  private void dialogClick(int x, int y, int b) {
    if (b != 0) return;
    int w = Math.min(312, width - 20), left = (width - w) / 2, top = height / 2 + 1;
    for (int i = 0; i < (pending != null ? 3 : 2); i++)
      if (inside(x, y, left + i * (w / 3), top, w / 3 - 4, 20)) {
        if (pending != null) {
          Runnable next = pending;
          if (i == 0 && !save()) return;
          pending = null;
          if (i != 2) {
            draft = null;
            next.run();
          }
        } else {
          if (i == 0) edits.delete(deleting);
          deleting = "";
        }
        return;
      }
  }
}
