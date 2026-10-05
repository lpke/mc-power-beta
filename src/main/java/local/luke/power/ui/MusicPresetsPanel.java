package local.luke.power.ui;

import java.util.*;
import local.luke.power.audio.MusicPreset;
import local.luke.power.audio.AudioController;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Preset list and editor header, embedded in the same Audio content area. */
final class MusicPresetsPanel extends UiScreen {
  private final PowerOptionsScreen parent;
  private final MusicEdits edits;
  private final ScrollBar scrollbar = new ScrollBar();
  private final TextInput name = new TextInput("", 80);
  private MusicPresetDraft draft;
  private boolean choosing;
  private String error = "", deleting = "";
  private int scroll;
  private Runnable pending;
  private List<MusicPreset> countedPresets;
  private int countedRevision = -1;
  private Map<String, Integer> includedCounts = Map.of();

  MusicPresetsPanel(PowerOptionsScreen parent) {
    this.parent = parent;
    edits = new MusicEdits(parent);
  }

  MusicPresetDraft draft() {
    return draft;
  }

  void unfocus() { name.focused = false; }

  boolean editing() {
    return draft != null;
  }

  boolean modal() {
    return pending != null || !deleting.isEmpty();
  }

  boolean dirty() {
    return draft != null && draft.changed(name.text());
  }

  void open(boolean choosing) {
    this.choosing = choosing;
    pending = null;
    deleting = "";
    name.focused = false;
    scrollbar.release();
    draft = null;
    scroll = 0;
    error = "";
  }

  void edit(MusicPreset preset) {
    draft = new MusicPresetDraft(preset);
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
    return parent.audioContentTop() + 4;
  }

  private int bottom() {
    return parent.contentBottom();
  }

  private int span() {
    return right() - left();
  }

  int headerHeight() {
    return editing() ? (span() >= 480 ? 26 : span() < 218 ? 70 : 48) : choosing ? 46 : 26;
  }

  private String actionLabel() { return draft.creating() ? "Creating" : "Editing"; }
  private int nameX() { return left() + textRenderer.getWidth(actionLabel()) + 6; }
  private int nameWidth() { return right() - nameX() - (span() >= 480 ? 220 : 0); }
  private int saveX() { return span() >= 480 ? right() - 214 : left(); }
  private int editButtonY() { return top() + (span() >= 480 ? 0 : 22); }
  private int includeX() { return right() - 148; }
  private int excludeX() { return right() - 72; }
  private int bulkY() { return span() < 218 ? top() + 44 : editButtonY(); }
  private int rowTop() { return top() + headerHeight(); }

  private ScrollBar.Track track() {
    return new ScrollBar.Track(
        right() + 5,
        rowTop(),
        Math.max(0, bottom() - rowTop()),
        (edits.presets().size() + (choosing ? 1 : 0)) * 40,
        scroll);
  }

  void wheel(int delta) {
    scroll = (int) Math.max(0, Math.min(track().maximum(), scroll - Math.signum(delta) * 40));
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
    if (b == 0 && inside(x, y, saveX(), editButtonY(), 62, 18)) {
      save();
      return true;
    }
    if (inside(x, y, includeX(), bulkY(), 72, 18)) {
      draft.includeAll();
      return true;
    }
    if (inside(x, y, excludeX(), bulkY(), 72, 18)) {
      try { draft.excludeAll(AudioController.music(minecraft)); error = ""; }
      catch (IllegalArgumentException e) { error = e.getMessage(); }
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
    if (!choosing && inside(x, y, left(), top(), Math.min(150, span()), 20)) {
      edit(null);
      return true;
    }
    if (y < rowTop() || y >= bottom()) return true;
    int index = (y - rowTop() + scroll) / 40;
    int row = rowTop() + index * 40 - scroll;
    if (y >= row + 34) return true;
    if (choosing && index == 0) {
      if (inside(x, y, right() - 54, row + 7, 54, 18)) {
        edits.load("");
        parent.closeAudioPanel();
      }
      return true;
    }
    var presets = edits.presets();
    index -= choosing ? 1 : 0;
    if (index < 0 || index >= presets.size()) return true;
    MusicPreset p = presets.get(index);
    if (choosing) {
      if (inside(x, y, right() - 54, row + 7, 54, 18)) {
        edits.load(p.id());
        parent.closeAudioPanel();
      }
    } else {
      if (inside(x, y, right() - 112, row + 7, 54, 18)) edit(p);
      else if (inside(x, y, right() - 54, row + 7, 54, 18)) deleting = p.id();
    }
    return true;
  }

  private Map<String, Integer> includedCounts(List<MusicPreset> values) {
    int revision = AudioController.libraryRevision();
    if (values != countedPresets || revision != countedRevision) {
      Set<String> available = AudioController.music(minecraft);
      Map<String, Integer> counts = new HashMap<>();
      for (MusicPreset p : values)
        counts.put(p.id(), available.size() - (int) p.excluded().stream().filter(available::contains).count());
      includedCounts = Map.copyOf(counts);
      countedPresets = values;
      countedRevision = revision;
    }
    return includedCounts;
  }

  void renderHeader(int mx, int my) {
    if (editing()) {
      text(actionLabel(), left(), top() + 5, 0xffffff);
      input(name, nameX(), top(), nameWidth(), mx, my, "Preset name");
      button("Save", saveX(), editButtonY(), 62, 18, mx, my, true);
      button("Include all", includeX(), bulkY(), 72, 18, mx, my, true);
      button("Exclude all", excludeX(), bulkY(), 72, 18, mx, my, true);
    } else {
      if (choosing)
        text(
            fit("Loading replaces current track includes and excludes.", span()),
            left(),
            top() + 5,
            0xcccccc);
      else button("Create new preset", left(), top(), Math.min(150, span()), 20, mx, my, true);
      if (choosing) text("Choose a soundtrack selection", left(), top() + 28, 0xaaaaaa);
      scroll =
          (int)
              Math.max(
                  0,
                  Math.min(track().maximum(), scrollbar.drag(track(), my, Mouse.isButtonDown(0))));
      clip(left(), rowTop(), span(), Math.max(0, bottom() - rowTop()));
      var presets = edits.presets();
      var counts = includedCounts(presets);
      for (int i = 0; i < presets.size() + (choosing ? 1 : 0); i++) {
        int y = rowTop() + i * 40 - scroll;
        if (y + 40 <= rowTop() || y >= bottom()) continue;
        MusicPreset p = choosing && i == 0 ? null : presets.get(i - (choosing ? 1 : 0));
        text(
            fit(p == null ? "None" : p.name(), span() - (choosing ? 64 : 122)),
            left() + 2,
            y + 5,
            0xdddddd);
        text(
            fit(
                p == null
                    ? "Use World music and custom music settings"
                    : counts.get(p.id()) + " included tracks",
                span() - (choosing ? 64 : 122)),
            left() + 2,
            y + 19,
            0xaaaaaa);
        if (!choosing) button("Edit", right() - 112, y + 7, 54, 18, mx, my, true);
        button(choosing ? "Load" : "Delete", right() - 54, y + 7, 54, 18, mx, my, true);
        fill(left(), y + 36, right(), y + 37, 0x40555555);
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
    text(pending != null ? "Save changes to this preset?" : "Delete this preset?", x, y, 0xffffff);
    if (!error.isEmpty()) text(fit(error, w), x, y + 15, 0xff8888);
    else if (!deleting.isEmpty())
      text("Current track exclusions will stay unchanged.", x, y + 15, 0xaaaaaa);
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
