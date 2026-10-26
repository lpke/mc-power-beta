package local.luke.power.ui;

import java.nio.file.Path;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.Setting;
import local.luke.power.config.backend.AudioBackend;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.input.Mouse;

/** Music library, saved playlists and playback settings within the Options transaction. */
public final class MusicLibraryScreen extends UiScreen {
  public record State(boolean queue, String folder, String query, int trackScroll, int queueScroll,
      String lastFolder, Set<String> collapsed, String presetFilter, Set<String> initialized, boolean hideExcluded) {
    public State(boolean queue, String folder, String query, int trackScroll, int queueScroll,
        String lastFolder, Set<String> collapsed, String presetFilter, Set<String> initialized) {
      this(queue, folder, query, trackScroll, queueScroll, lastFolder, collapsed, presetFilter, initialized, false);
    }
    public State(boolean queue, String folder, String query, int trackScroll, int queueScroll,
        String lastFolder, Set<String> collapsed, String presetFilter) {
      this(queue, folder, query, trackScroll, queueScroll, lastFolder, collapsed, presetFilter, Set.of());
    }
    public State(boolean queue, String folder, String query, int trackScroll, int queueScroll,
        String lastFolder, Set<String> collapsed) {
      this(queue, folder, query, trackScroll, queueScroll, lastFolder, collapsed, "");
    }
    public State(boolean queue, String folder, String query, int trackScroll, int queueScroll, String lastFolder) {
      this(queue, folder, query, trackScroll, queueScroll, lastFolder, Set.of());
    }
    public State(boolean queue, String folder, String query, int trackScroll, int queueScroll) {
      this(queue, folder, query, trackScroll, queueScroll, "");
    }
  }

  private final PowerOptionsScreen parent;
  private final MusicEdits edits;
  private final MusicPresetsPanel presets;
  private final TextInput search = new TextInput("", 128);
  private final ScrollBar scrollbar = new ScrollBar();
  private final Map<String, Setting> settings = new HashMap<>();
  private final Map<String, MusicLibrary.Track> custom = new HashMap<>();
  private final Map<String, List<String>> groupTracks = new HashMap<>();
  private final Set<String> collapsed = new HashSet<>(), initializedFilters = new HashSet<>();
  private Set<String> available = Set.of();
  private List<String> tracks = List.of(), renderedQueue = List.of(), sectionKeys = List.of();
  private record Row(String group, String label, String id, int index, int y, int height) {}
  private List<Row> rows = List.of();
  private String folder = "library", error = "", hoverHelp = "", hoverKey = "";
  private boolean queue, presetsOpen, favouritesOnly, conversionNeeded;
  private int revision = -1, conversionRevision, conversionTicks, trackScroll, queueScroll, contentHeight;
  private MusicPresetDraft previousDraft;
  private Setting dragging;
  private String draggingGroup;
  private MusicPresetDraft draggingGroupDraft;

  public MusicLibraryScreen(PowerOptionsScreen parent) {
    this.parent = parent;
    edits = new MusicEdits(parent);
    presets = new MusicPresetsPanel(parent);
  }

  public boolean draggingSlider() { return dragging != null || draggingGroup != null; }
  public boolean trackStatusVisible() {
    return error.isEmpty() && (!presetsOpen || presets.error().isEmpty())
        && (queue || !folder.equals("library") || !Mp3Converter.busy() && conversionTicks == 0);
  }
  public boolean conversionVisible() {
    return !queue && folder.equals("library") && Mp3Converter.available() && (conversionNeeded || Mp3Converter.busy());
  }
  public String hoverHelp() { return hoverHelp; }
  public String hoverKey() { return hoverKey; }
  public boolean queueVisible() { return queue; }
  public boolean presetVisible() { return presetsOpen; }
  public boolean settingsVisible() { return !queue && folder.equals("settings"); }
  public String query() { return search.text(); }
  public int settingsTop() { return controlsTop() + 24; }
  public boolean modal() { return presetsOpen && presets.modal(); }
  public boolean dirtyPreset() { return presetsOpen && presets.dirty(); }
  public boolean hasSections() { return !queue && !settingsVisible() && (!presetsOpen || presets.editing()) && !sectionKeys.isEmpty(); }
  public boolean sectionsCollapsed() { return SectionCollapse.allCollapsed(sectionKeys, collapsed); }

  public void toggleSections() {
    if (!hasSections()) return;
    finishDrag();
    SectionCollapse.toggleAll(sectionKeys, collapsed);
    rebuildRows();
    scroll(scroll());
  }

  private String collapseKey(String group) { return search.text().isBlank() ? group : "filtered/" + group; }

  public State state() {
    return new State(queue, folder, search.text(), trackScroll, queueScroll, "", Set.copyOf(collapsed),
        "", Set.copyOf(initializedFilters), favouritesOnly);
  }

  public void restore(State state) {
    if (state == null) return;
    closePresets();
    collapsed.clear(); collapsed.addAll(state.collapsed);
    initializedFilters.clear(); initializedFilters.addAll(state.initialized);
    queue = state.queue;
    folder = Set.of("library", "playlists", "settings").contains(state.folder) ? state.folder : "library";
    presetsOpen = !queue && folder.equals("playlists");
    favouritesOnly = state.hideExcluded;
    search.setText(state.query);
    trackScroll = state.trackScroll;
    queueScroll = state.queueScroll;
    search.focused = false;
  }

  private int left() { return parent.contentLeft(); }
  private int right() { return parent.contentRight(); }
  private int span() { return right() - left(); }
  private int bottom() { return parent.contentBottom(); }
  private int statusTop() { return parent.audioContentTop(); }
  private int controlsTop() { return statusTop() + 18; }
  private int actionTop() { return controlsTop() + 22; }
  private int listTop() {
    return queue ? controlsTop() + 24 : presetsOpen ? actionTop() + presets.headerHeight() + 4
        : actionTop() + 24;
  }
  private int groupHeight() { return presetsOpen ? 24 : 20; }
  private int rowHeight() { return rowLayout().height(); }
  private MusicRowLayout rowLayout() {
    return presetsOpen ? MusicRowLayout.playlist(left(), span()) : MusicRowLayout.at(left(), span(), queue);
  }
  private int volumeX() { return rowLayout().volumeX(); }
  private int volumeEnd() { return volumeX() + rowLayout().volumeWidth(); }
  private int groupVolumeWidth() { return Math.min(100, Math.max(46, span() / 4)); }
  private int groupVolumeX() { return right() - 48 - groupVolumeWidth(); }
  private int groupResetX() { return right() - 44; }
  private boolean groupHasVolume(String group) { return presetsOpen && !group.equals("Unavailable tracks"); }
  private int reloadX() { return right() - 48; }
  private int foldersX() { return right() - 104; }
  private int filterX() { return queue ? right() - 82 : left(); }
  private int filterWidth() { return queue ? 82 : 64; }
  private MusicFilters.Layout filters() { return MusicFilters.layout(left(), controlsTop(), span(), ""); }
  private int scroll() { return queue ? queueScroll : trackScroll; }
  private void scroll(int value) {
    int bounded = Math.max(0, Math.min(value, Math.max(0, contentHeight - (bottom() - listTop()))));
    if (queue) queueScroll = bounded; else trackScroll = bounded;
  }
  private ScrollBar.Track track() {
    return new ScrollBar.Track(right() + 5, listTop(), Math.max(0, bottom() - listTop()), contentHeight, scroll());
  }

  public void guard(Runnable action) {
    finishDrag();
    if (presetsOpen) presets.guard(action); else action.run();
  }
  public void dialogClick(int x, int y, int b) { presets.headerClick(x, y, b); }
  public void renderDialog(int x, int y) { if (presetsOpen) presets.renderDialog(x, y); }
  public void openPresets() { selectFilter("playlists"); }
  public void showTracks() { selectFilter("library"); }
  public void showSettings() { selectFilter("settings"); }
  public void showQueue() {
    finishDrag();
    closePresets();
    queue = true;
    search.focused = false;
    scrollbar.release();
    error = "";
    if (revision != AudioController.libraryRevision()) refresh(); else rebuild();
    parent.refreshMusicEditor();
  }

  private void selectFilter(String id) {
    if (dirtyPreset()) { guard(() -> selectFilter(id)); return; }
    finishDrag();
    closePresets();
    queue = false;
    folder = id.equals("presets") ? "playlists" : Set.of("library", "playlists", "settings").contains(id) ? id : "library";
    presetsOpen = folder.equals("playlists");
    search.setText(""); search.focused = false;
    error = "";
    trackScroll = 0;
    if (revision != AudioController.libraryRevision()) refresh(); else rebuild();
    parent.refreshMusicEditor();
  }

  public void back(Runnable exit) {
    finishDrag();
    if (presetsOpen && presets.editing()) presets.guard(() -> { presets.open(); refresh(); });
    else exit.run();
  }
  private void closePresets() { presetsOpen = false; presets.open(); previousDraft = null; }
  public void close() { removed(); closePresets(); }

  public void showTrack(String id) {
    showTracks();
    favouritesOnly = false;
    rebuild();
    collapsed.remove(collapseKey(group(id)));
    rebuildRows();
    rows.stream().filter(row -> id.equals(row.id)).findFirst().ifPresent(row -> scroll(Math.max(0, row.y - groupHeight())));
  }

  public boolean hasQuery() { return !queue && (!presetsOpen || presets.editing()) && !search.text().isEmpty(); }
  public boolean focused() { return search.focused; }
  public void clearQuery() {
    search.setText(""); scroll(0); rebuild();
    if (settingsVisible()) parent.refreshMusicEditor();
  }
  public void unfocus() { search.focused = false; }
  public void focus() {
    if (queue) showTracks();
    presets.unfocus(); search.focused = true; search.selectAll();
  }
  public void init() {
    finishDrag();
    presets.init(minecraft, width, height);
    refresh();
  }
  public void removed() { finishDrag(); scrollbar.release(); }
  private void finishDrag() {
    dragging = null; draggingGroup = null; draggingGroupDraft = null;
    parent.finishContinuousChange();
  }

  private void refresh() {
    AudioBackend.discoverMusic(parent.session(), minecraft);
    custom.clear();
    AudioController.customTracks().forEach(t -> custom.put(t.id(), t));
    available = Set.copyOf(AudioController.music(minecraft));
    conversionNeeded = Mp3Converter.needed(custom.values());
    settings.clear();
    parent.session().settings().forEach(s -> settings.put(s.id, s));
    revision = AudioController.libraryRevision();
    rebuild();
  }

  private void rebuild() {
    Collection<String> source;
    if (queue) source = MusicRequests.tracks();
    else {
      Set<String> all = new LinkedHashSet<>(available);
      if (presetsOpen && presets.editing()) all.addAll(presets.draft().tracks());
      source = all;
    }
    groupTracks.clear();
    source.forEach(id -> groupTracks.computeIfAbsent(group(id), k -> new ArrayList<>()).add(id));
    if (queue) tracks = List.copyOf(source);
    else {
      Map<String, Integer> scores = new HashMap<>();
      source.forEach(id -> scores.put(id, FuzzySearch.score(search.text(), AudioController.musicLabel(id), path(id))));
      tracks = source.stream()
          .filter(id -> presetsOpen || !favouritesOnly || edits.favourites().contains(id))
          .filter(id -> scores.get(id) < FuzzySearch.NONE)
          .sorted(Comparator.comparingInt((String id) -> scores.get(id))
              .thenComparing(AudioController::musicLabel, String.CASE_INSENSITIVE_ORDER)).toList();
    }
    rebuildRows();
    scroll(scroll());
  }

  private String group(String id) { return available.contains(id) ? MusicGroups.id(id, custom.get(id)) : "Unavailable tracks"; }
  private int groupOrder(String group) {
    if (group.startsWith("folder:")) return -1;
    int index = BuiltinMusic.GROUPS.indexOf(group);
    return index < 0 ? BuiltinMusic.GROUPS.size() : index;
  }
  private List<String> members(String group) { return groupTracks.getOrDefault(group, List.of()); }
  private boolean included(String id) { return presetsOpen && presets.editing() && presets.draft().included(id); }
  private int includedCount(String group) { return (int) members(group).stream().filter(this::included).count(); }
  private void toggleIncluded(String id) {
    if (presetsOpen && presets.editing()) presets.draft().include(id, !included(id));
  }
  private void toggleGroup(String group) {
    if (!presetsOpen || !presets.editing()) return;
    boolean include = includedCount(group) != members(group).size();
    members(group).forEach(id -> presets.draft().include(id, include));
  }
  private void slideGroup(int x) {
    if (draggingGroup == null || draggingGroupDraft != presets.draft()) { finishDrag(); return; }
    double fraction = (x - groupVolumeX() - 4d) / Math.max(1, groupVolumeWidth() - 8);
    draggingGroupDraft.volume(draggingGroup, (int) Math.round(100 * Math.max(0, Math.min(1, fraction))));
  }

  private void rebuildRows() {
    List<Row> result = new ArrayList<>();
    int y = 0;
    if (queue) {
      for (int i = 0; i < tracks.size(); i++, y += rowHeight())
        result.add(new Row("", "", tracks.get(i), i, y, rowHeight()));
    } else if (!settingsVisible()) {
      Map<String, List<String>> groups = new TreeMap<>(Comparator.comparingInt(this::groupOrder)
          .thenComparing(String.CASE_INSENSITIVE_ORDER).thenComparing(Comparator.naturalOrder()));
      tracks.forEach(id -> groups.computeIfAbsent(group(id), k -> new ArrayList<>()).add(id));
      if (presetsOpen && presets.editing() && previousDraft != presets.draft()) {
        collapsed.addAll(groups.keySet()); previousDraft = presets.draft(); trackScroll = 0;
      }
      for (var entry : groups.entrySet()) {
        String key = entry.getKey();
        if (initializedFilters.add(key)) collapsed.add(key);
        String label = key.startsWith("folder:") ? Objects.toString(Path.of(key.substring(7)).getFileName(), key.substring(7)) : key;
        result.add(new Row(key, label, null, -1, y, groupHeight())); y += groupHeight();
        if (collapsed.contains(collapseKey(key))) continue;
        for (String id : entry.getValue()) {
          result.add(new Row(key, label, id, -1, y, rowHeight())); y += rowHeight();
        }
      }
    }
    rows = List.copyOf(result);
    sectionKeys = rows.stream().filter(row -> row.id == null).map(row -> collapseKey(row.group)).toList();
    contentHeight = y;
  }

  private String path(String id) {
    MusicLibrary.Track t = custom.get(id);
    return t != null ? t.path().toString() : available.contains(id) ? "Included Minecraft music" : id + "\nTrack unavailable. Its playlist entry is preserved.";
  }
  private boolean playable(String id) {
    MusicLibrary.Track t = custom.get(id);
    return available.contains(id) && (t == null || t.playable());
  }

  public void tick() {
    if (conversionRevision != Mp3Converter.revision()) { conversionRevision = Mp3Converter.revision(); conversionTicks = 200; }
    else if (conversionTicks > 0) conversionTicks--;
    if (revision != AudioController.libraryRevision()) refresh();
    else if (queue && !tracks.equals(MusicRequests.tracks())) rebuild();
  }
  public void wheel(int amount) {
    if (modal()) return;
    if (presetsOpen && !presets.editing()) { presets.wheel(amount); return; }
    if (amount != 0) scroll(scroll() - (int) Math.signum(amount) * rowHeight());
  }
  private void slide(int x) {
    if (dragging == null) return;
    var before = dragging.value;
    dragging.slide((x - volumeX() - 4d) / Math.max(1, volumeEnd() - volumeX() - 8));
    if (!before.equals(dragging.value)) parent.changed(dragging, true);
  }
  private boolean queueEdit(java.util.function.Consumer<MusicQueue> change) {
    if (queue && !renderedQueue.equals(MusicRequests.tracks())) {
      rebuild(); error = "Queue changed. Select the track again."; return false;
    }
    try { MusicRequests.edit(change); error = ""; rebuild(); return true; }
    catch (Exception e) { error = Objects.toString(e.getMessage(), "Could not save queue"); return false; }
  }

  public void searchClick(int x, int y, int button) {
    if (queue || presetsOpen && !presets.editing()) return;
    presets.unfocus(); search.focused = true;
    String previous = search.text(); search.click(x, button);
    if (!previous.equals(search.text())) clearQuery();
  }
  protected void keyPressed(char c, int key) {
    if (presetsOpen && presets.modal()) { presets.key(c, key); return; }
    if (presetsOpen && !search.focused) presets.key(c, key);
    if (!queue && search.focused) {
      search.key(c, key); scroll(0); rebuild();
      if (settingsVisible()) parent.refreshMusicEditor();
    }
  }

  protected void mouseClicked(int x, int y, int b) {
    if (b != 0 && b != 1) return;
    finishDrag();
    if (modal()) { presets.headerClick(x, y, b); return; }
    if (!queue) for (var tab : filters().tabs())
      if (b == 0 && inside(x, y, tab.x(), tab.y(), tab.width(), 18)) {
        click(); selectFilter(tab.id()); return;
      }
    if (settingsVisible()) return;
    if (presetsOpen && (!presets.editing() || y < listTop())) {
      search.focused = false; presets.headerClick(x, y, b); rebuild(); return;
    }
    if (b == 0 && scrollbar.press(track(), x, y)) { scroll((int) scrollbar.drag(track(), y, true)); return; }
    search.focused = false; presets.unfocus();
    if (queue && b == 0 && inside(x, y, filterX(), controlsTop(), filterWidth(), 18)) {
      if (!tracks.isEmpty()) { click(); queueEdit(q -> q.tracks.clear()); }
      return;
    }
    if (!queue && !presetsOpen && b == 0) {
      if (inside(x, y, left(), actionTop(), 64, 18)) {
        click(); favouritesOnly = !favouritesOnly; rebuild(); scroll(0); return;
      }
      if (inside(x, y, foldersX(), actionTop(), 52, 18)) {
        click(); minecraft.setScreen(new FolderScreen(parent, edits.setting("musicDirectories"))); return;
      }
      if (inside(x, y, reloadX(), actionTop(), 48, 18)) { click(); AudioController.reload(); return; }
    }
    if (conversionVisible() && b == 0 && inside(x, y, right() - 88, statusTop(), 88, 14)) {
      click();
      if (Mp3Converter.busy()) Mp3Converter.cancel();
      else Mp3Converter.start(FabricLoader.getInstance().getGameDir(), AudioController.customTracks());
      return;
    }
    if (y < listTop() || y >= bottom() || x < left() || x >= right()) return;
    StickyHeader sticky = stickyHeader();
    boolean pinned = sticky != null && sticky.contains(y, listTop());
    Row clicked = pinned ? rows.get(sticky.index()) : rows.stream()
        .filter(row -> y - listTop() + scroll() >= row.y && y - listTop() + scroll() < row.y + row.height).findFirst().orElse(null);
    if (clicked == null) return;
    int rowY = pinned ? sticky.y() : listTop() + clicked.y - scroll();
    if (clicked.id == null) {
      if (presetsOpen && b == 0 && inside(x, y, right() - 20, rowY + 3, 20, 18)) {
        click(); toggleGroup(clicked.group); rebuildRows(); return;
      }
      if (groupHasVolume(clicked.group) && b == 0 && inside(x, y, groupResetX(), rowY + 3, 20, 18)) {
        if (presets.draft().volume(clicked.group) != 100) { click(); presets.draft().volume(clicked.group, 100); }
        return;
      }
      if (groupHasVolume(clicked.group) && inside(x, y, groupVolumeX(), rowY + 3, groupVolumeWidth(), 18)) {
        if (b == 0) {
          draggingGroup = clicked.group; draggingGroupDraft = presets.draft(); slideGroup(x);
        } else {
          MusicPresetDraft draft = presets.draft();
          Setting value = new Setting("music.draftGroupVolume", "audio", "Audio", "Music", "Group volume",
              "Volume for this playlist", Setting.Kind.INTEGER,
              new com.google.gson.JsonPrimitive(draft.volume(clicked.group)), new com.google.gson.JsonPrimitive(100),
              0, 100, 1, List.of(), false);
          parent.valueEditor.begin("musicGroup/" + clicked.group, value, () -> {
            if (presets.draft() == draft) draft.volume(clicked.group, value.value.getAsInt());
          }, groupVolumeX(), rowY + 3, groupVolumeWidth(), listTop(), bottom());
        }
        return;
      }
      if (b != 0) return;
      if (!collapsed.remove(collapseKey(clicked.group))) collapsed.add(collapseKey(clicked.group));
      rebuildRows(); scroll(pinned ? clicked.y : scroll()); return;
    }
    if (queue && !renderedQueue.equals(MusicRequests.tracks())) { rebuild(); error = "Queue changed. Select the track again."; return; }
    String id = clicked.id;
    int index = clicked.index;
    var layout = rowLayout(); int cy = rowY + layout.controlsY(), bw = layout.buttonWidth();
    if (presetsOpen && b == 0 && inside(x, y, left(), rowY + 3, 20, 18)) {
      click(); toggleIncluded(id); rebuildRows(); return;
    }
    Setting volume = settings.get("audio.sound." + id);
    if (!presetsOpen && volume != null && inside(x, y, volumeX(), cy, layout.volumeWidth(), 18)) {
      if (b == 0) { dragging = volume; slide(x); }
      else parent.valueEditor.begin(clicked, volume, () -> parent.changed(volume), volumeX(), cy, layout.volumeWidth(), listTop(), bottom());
      return;
    }
    if (b != 0) return;
    if (!presetsOpen && inside(x, y, layout.favouriteX(), cy, bw, 18)) {
      click(); edits.favourite(id); if (favouritesOnly) rebuild(); return;
    }
    if (queue) {
      if (inside(x, y, layout.queueX(), cy, bw, 18)) { click(); queueEdit(q -> q.tracks.remove(index)); }
      else if (inside(x, y, layout.playX(), cy, bw, 18) && playable(id)) { click(); AudioController.toggleTrack(id); }
      else if (inside(x, y, layout.upX(), cy, bw, 18) && index > 0) { click(); queueEdit(q -> q.move(index, -1)); }
      else if (inside(x, y, layout.downX(), cy, bw, 18) && index + 1 < tracks.size()) { click(); queueEdit(q -> q.move(index, 1)); }
    } else {
      if (inside(x, y, layout.previewX(), cy, bw, 18) && playable(id)) { click(); AudioController.previewSound(id); }
      else if (inside(x, y, layout.playX(), cy, bw, 18) && playable(id)) { click(); AudioController.toggleTrack(id); }
      else if (!presetsOpen && inside(x, y, layout.queueX(), cy, bw, 18)) { click(); queueEdit(q -> q.add(id)); }
    }
  }

  public void renderSearch(int x, int y, int w, int mx, int my) {
    if (presetsOpen && !presets.editing()) text("Music playlists", x + 2, y + 5, 0xdddddd);
    else if (queue) text("Music queue", x + 2, y + 5, 0xdddddd);
    else input(search, x, y, w, mx, my, settingsVisible() ? "Search music settings..." : "Search tracks or folders...");
  }
  private StickyHeader stickyHeader() {
    return StickyHeader.at(rows, row -> row.id == null, Row::y, Row::height, scroll(), listTop(), bottom());
  }
  private record GroupHover(String tip, String key) {}
  private GroupHover groupHeading(Row row, int y, int mx, int my) {
    boolean hover = inside(mx, my, left(), y, span(), row.height) && my >= listTop() && my < bottom();
    boolean closed = collapsed.contains(collapseKey(row.group));
    int total = members(row.group).size(), included = includedCount(row.group);
    String count = presetsOpen ? included + "/" + total : Integer.toString(total);
    int countWidth = textRenderer.getWidth(count);
    int countRight = groupHasVolume(row.group) ? groupVolumeX() - 6 : right() - (presetsOpen ? 26 : 4);
    if (groupHasVolume(row.group)) {
      int volume = presets.draft().volume(row.group);
      if (parent.valueEditor.editing("musicGroup/" + row.group))
        parent.valueEditor.render(this, groupVolumeX(), y + 3, groupVolumeWidth(), mx, my, listTop(), bottom());
      else slider(volume + "%", groupVolumeX(), y + 3, groupVolumeWidth(), mx, my, volume / 100d);
      button("R", groupResetX(), y + 3, 20, 18, mx, my, volume != 100);
    }
    if (presetsOpen) musicToggle(included > 0, right() - 20, y + 3, mx, my, total > 0);
    text((closed ? "> " : "v ") + fit(row.label, Math.max(12, countRight - left() - countWidth - 18)), left() + 2, y + 7, 0xffffff);
    text(count, countRight - countWidth, y + 7, presetsOpen && included > 0 && included < total ? 0xffdd88 : 0xaaaaaa);
    fill(left(), y + row.height - 2, right(), y + row.height - 1, 0x50555555);
    String tip = "", key = "";
    if (hover) {
      key = "group:" + row.group;
      tip = row.group.startsWith("folder:") ? row.group.substring(7) : row.label;
      if (presetsOpen && inside(mx, my, right() - 20, y + 3, 20, 18))
        tip = (included == total ? "Remove" : "Add") + " every track in this group "
            + (included == total ? "from" : "to") + " this playlist. Includes tracks hidden by search.";
      else if (groupHasVolume(row.group) && inside(mx, my, groupResetX(), y + 3, 20, 18))
        tip = "Reset this playlist's group volume to 100%.";
      else if (groupHasVolume(row.group) && inside(mx, my, groupVolumeX(), y + 3, groupVolumeWidth(), 18))
        tip = parent.valueEditor.editing("musicGroup/" + row.group) ? parent.valueEditor.help()
            : "Group volume for this playlist. Right-click to enter an exact value. Takes effect when you save.";
      else tip += "\n" + (presetsOpen ? included + " of " + total + " tracks in this playlist." : total + " tracks.");
    }
    return new GroupHover(tip, key);
  }

  public void render(int mx, int my, float delta) {
    String tip = "", key = "";
    if (!queue) for (var tab : filters().tabs()) {
      boolean selected = folder.equals(tab.id());
      fill(tab.x(), tab.y(), tab.x() + tab.width(), tab.y() + 18, selected ? 0xb0000000 : 0x40000000);
      text(tab.label(), tab.x() + 4, tab.y() + 5, selected ? 0xffffff : 0xaaaaaa);
      if (selected) fill(tab.x(), tab.y() + 17, tab.x() + tab.width(), tab.y() + 18, 0xffaaaaaa);
    }
    if (settingsVisible()) { hoverHelp = hoverKey = ""; return; }
    if (presetsOpen) {
      presets.renderHeader(mx, my);
      tip = presets.help(mx, my);
      if (!presets.editing()) { hoverHelp = hoverKey = tip; return; }
    }
    scroll((int) scrollbar.drag(track(), my, Mouse.isButtonDown(0)));
    if (dragging != null) {
      if (Mouse.isButtonDown(0)) slide(mx); else { dragging = null; parent.finishContinuousChange(); }
    }
    if (draggingGroup != null) { if (Mouse.isButtonDown(0)) slideGroup(mx); else finishDrag(); }
    if (queue) {
      text("Up next", left() + 2, controlsTop() + 5, 0xdddddd);
      button("Clear queue", filterX(), controlsTop(), filterWidth(), 18, mx, my, !tracks.isEmpty());
      if (inside(mx, my, filterX(), controlsTop(), filterWidth(), 18)) tip = "Remove all queued requests.";
    } else if (!presetsOpen) {
      tabButton("Favorites", left(), actionTop(), 64, 18, mx, my, true, favouritesOnly);
      button("Folders", foldersX(), actionTop(), 52, 18, mx, my, true);
      button("Reload", reloadX(), actionTop(), 48, 18, mx, my, true);
      if (inside(mx, my, left(), actionTop(), 64, 18)) tip = "Show only starred tracks. Playlists stay unchanged.";
      else if (inside(mx, my, foldersX(), actionTop(), 52, 18)) tip = "Add or manage your music folders.";
      else if (inside(mx, my, reloadX(), actionTop(), 48, 18)) tip = "Rescan your music folders. Playlist membership stays unchanged.";
    }
    if (queue) renderedQueue = List.copyOf(tracks);
    StickyHeader sticky = stickyHeader();
    int rowsTop = sticky == null ? listTop() : Math.max(listTop(), sticky.y() + sticky.height());
    clip(left() - 2, rowsTop, span() + 4, Math.max(0, bottom() - rowsTop));
    int listMouseY = sticky != null && sticky.contains(my, listTop()) ? -1 : my;
    for (Row row : rows) {
      int y = listTop() + row.y - scroll();
      if (y + row.height <= listTop() || y >= bottom()) continue;
      if (row.id == null) {
        if (sticky == null || row != rows.get(sticky.index())) {
          var group = groupHeading(row, y, mx, listMouseY);
          if (!group.tip.isEmpty()) { tip = group.tip; key = group.key; }
        }
        continue;
      }
      String id = row.id; int index = row.index;
      var layout = rowLayout(); int cy = y + layout.controlsY(), bw = layout.buttonWidth();
      boolean hover = inside(mx, listMouseY, left(), y, span(), row.height) && listMouseY >= listTop() && listMouseY < bottom();
      if (hover) { fill(left() - 2, y, right() + 2, y + row.height - 1, 0x60000000); tip = path(id); key = "track:" + id; }
      int nameX = presetsOpen || queue ? layout.nameX() : left() + 2;
      int nameWidth = layout.nameWidth() + (!presetsOpen && !queue ? 22 : 0);
      text(fit((queue ? (index + 1) + ". " : "") + AudioController.musicLabel(id), nameWidth), nameX, y + 7, playable(id) ? 0xdddddd : 0xffbb77);
      if (presetsOpen) musicToggle(included(id), left(), y + 3, mx, listMouseY, true);
      Setting volume = settings.get("audio.sound." + id);
      if (!presetsOpen) {
        if (volume != null && parent.valueEditor.editing(row))
          parent.valueEditor.render(this, volumeX(), cy, layout.volumeWidth(), mx, listMouseY, rowsTop, bottom());
        else if (volume != null) slider(volume.display(), volumeX(), cy, layout.volumeWidth(), mx, listMouseY, volume.value.getAsDouble() / 100);
        iconButton("star", layout.favouriteX(), cy, bw, mx, listMouseY, true, edits.favourites().contains(id));
      }
      if (queue) {
        iconButton("up", layout.upX(), cy, bw, mx, listMouseY, index > 0);
        iconButton("down", layout.downX(), cy, bw, mx, listMouseY, index + 1 < tracks.size());
        iconButton("remove", layout.queueX(), cy, bw, mx, listMouseY, true);
      } else {
        iconButton("speaker", layout.previewX(), cy, bw, mx, listMouseY, playable(id), AudioController.previewing(id));
        if (!presetsOpen) button("+", layout.queueX(), cy, bw, 18, mx, listMouseY, true);
      }
      iconButton(AudioController.playingTrack(id) ? "pause" : "play", layout.playX(), cy, bw, mx, listMouseY, playable(id));
      if (hover) {
        if (presetsOpen && inside(mx, listMouseY, left(), y + 3, 20, 18)) tip = included(id) ? "Remove from this playlist" : "Add to this playlist";
        else if (!presetsOpen && inside(mx, listMouseY, volumeX(), cy, layout.volumeWidth(), 18)) tip = parent.valueEditor.editing(row) ? parent.valueEditor.help() : "Track volume, shared by all playlists. Right-click to enter an exact value.";
        else if (!presetsOpen && inside(mx, listMouseY, layout.favouriteX(), cy, bw, 18)) tip = edits.favourites().contains(id) ? "Remove from favorites" : "Add to favorites";
        else if (inside(mx, listMouseY, layout.playX(), cy, bw, 18)) tip = "Play or pause this track";
        else if (!queue && inside(mx, listMouseY, layout.previewX(), cy, bw, 18)) tip = "Preview without changing the current track; click again to stop.";
        else if (!presetsOpen && inside(mx, listMouseY, layout.queueX(), cy, bw, 18)) tip = queue ? "Remove this request" : "Add to the end of the queue";
        else if (queue && inside(mx, listMouseY, layout.upX(), cy, bw, 18)) tip = "Move up";
        else if (queue && inside(mx, listMouseY, layout.downX(), cy, bw, 18)) tip = "Move down";
      }
    }
    if (sticky != null) {
      clip(left() - 2, listTop(), span() + 4, Math.max(0, bottom() - listTop()));
      var group = groupHeading(rows.get(sticky.index()), sticky.y(), mx, my);
      if (!group.tip.isEmpty()) { tip = group.tip; key = group.key; }
    }
    if (rows.isEmpty()) text(queue ? "Queue is empty" : "No matching tracks", left() + 4, listTop() + 12, 0xaaaaaa);
    unclip(); scrollbar.render(this, track());
    if (conversionVisible()) {
      button(Mp3Converter.busy() ? "Cancel MP3" : "Convert MP3", right() - 88, statusTop(), 88, 14, mx, my, true);
      if (inside(mx, my, right() - 88, statusTop(), 88, 14)) tip = "Convert MP3 to a playback cache. Original files stay unchanged.";
    }
    String failure = !error.isEmpty() ? error : presetsOpen ? presets.error() : "";
    String status = !failure.isEmpty() ? failure : Mp3Converter.busy() || conversionTicks > 0 ? Mp3Converter.status() : AudioController.status();
    if (!trackStatusVisible()) text(fit(status, span() - (conversionVisible() ? 92 : 0)), left(), statusTop() + 4, failure.isEmpty() ? 0xaaaaaa : 0xff8888);
    hoverHelp = tip; hoverKey = key.isEmpty() ? tip : key;
  }
}
