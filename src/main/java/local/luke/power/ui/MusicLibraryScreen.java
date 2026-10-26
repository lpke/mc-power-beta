package local.luke.power.ui;

import java.nio.file.Path;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.Setting;
import local.luke.power.config.backend.AudioBackend;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Music catalog and saved playlists within the Options transaction. */
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
  private final MusicFolderActions folders;
  private final TextInput search = new TextInput("", 128);
  private final ScrollBar scrollbar = new ScrollBar();
  private final MusicListDrag reorder = new MusicListDrag();
  private final Map<String, Setting> settings = new HashMap<>();
  private final Map<String, MusicLibrary.Track> custom = new HashMap<>();
  private final Map<String, List<String>> groupTracks = new LinkedHashMap<>();
  private final Map<String, Integer> libraryGroupCounts = new HashMap<>();
  private final Map<String, MusicLibrarySources.Folder> sourceFolders = new LinkedHashMap<>();
  private MusicLibrarySources.Snapshot sourceLibrary;
  private com.google.gson.JsonElement sourceValue, orderValue;
  private final Set<String> collapsed = new HashSet<>(), initializedFilters = new HashSet<>();
  private Set<String> available = Set.of();
  private List<String> tracks = List.of(), renderedQueue = List.of(), sectionKeys = List.of(), groupOrder = List.of();
  private record Row(String group, String label, String id, int index, int y, int height) {}
  private List<Row> rows = List.of();
  private String folder = "library", error = "", hoverHelp = "", hoverKey = "";
  private boolean queue, presetsOpen, conversionNeeded, previousAdding, previousGrouped;
  private int revision = -1, conversionRevision, conversionTicks, trackScroll, queueScroll, contentHeight, playlistScroll;
  private String playlistQuery = "", catalogQuery = "";
  private MusicPresetDraft previousDraft;
  private List<String> previousTracks = List.of();
  private Setting dragging;
  private String draggingGroup;
  private MusicPresetDraft draggingGroupDraft;

  public MusicLibraryScreen(PowerOptionsScreen parent) {
    this.parent = parent; edits = new MusicEdits(parent);
    presets = new MusicPresetsPanel(parent); folders = new MusicFolderActions(parent, this::refresh);
  }
  private boolean playlistView() { return presetsOpen && presets.editing() && !presets.adding(); }
  private boolean groupedView() { return !queue && (!playlistView() || presets.grouped()); }
  private boolean libraryTab() { return !queue && !presetsOpen && folder.equals("library"); }
  private boolean managedSource(String group) { return libraryTab() && sourceFolders.containsKey(group); }
  private boolean canOrderTracks() { return search.text().isBlank() && (queue || playlistView() && presets.editable() && !presets.grouped()); }
  boolean activePlaylistTab() { return !queue && folder.equals("active"); }
  private String activeTabLabel() {
    return activePlaylistTab() && presets.dirty() && !presets.draft().id().equals(edits.selected())
        ? presets.draft().name() : edits.currentName();
  }
  public boolean playlistEditor() { return presetsOpen && presets.editing(); }
  public boolean playlistEditable() { return playlistEditor() && presets.editable(); }
  public boolean playlistDirty() { return playlistEditor() && presets.dirty(); }
  public boolean readOnly() { return playlistEditor() && presets.readOnly(); }
  String footerError() { return showMusicStatus() ? "" : failure(); }
  private String failure() {
    return !error.isEmpty() ? error : !folders.error().isEmpty() ? folders.error() : presetsOpen ? presets.error() : "";
  }
  public boolean showMusicStatus() {
    return height >= 300;
  }
  public boolean savePlaylist() {
    finishDrag();
    boolean saved = presetsOpen && presets.savePlaylist();
    syncPanel();
    return saved;
  }
  public void cancelPlaylist() { finishDrag(); presets.cancelPlaylist(); syncPanel(); }
  public boolean draggingSlider() { return dragging != null || draggingGroup != null || reorder.active(); }
  public boolean trackStatusVisible() {
    return error.isEmpty() && folders.error().isEmpty() && (!presetsOpen || presets.error().isEmpty())
        && (queue || presetsOpen || !Mp3Converter.busy() && conversionTicks == 0);
  }
  public boolean conversionVisible() { return !queue && !presetsOpen && Mp3Converter.available() && (conversionNeeded || Mp3Converter.busy()); }
  public String hoverHelp() { return hoverHelp; }
  public String hoverKey() { return hoverKey; }
  public boolean queueVisible() { return queue; }
  public boolean presetVisible() { return presetsOpen; }
  public String query() { return search.text(); }
  public boolean modal() { return folders.modal() || presetsOpen && presets.modal(); }
  public boolean dirtyPreset() { return playlistDirty(); }
  public boolean hasSections() { return groupedView() && (!presetsOpen || presets.editing()) && !sectionKeys.isEmpty(); }
  public boolean sectionsCollapsed() { return SectionCollapse.allCollapsed(sectionKeys, collapsed); }
  public void toggleSections() {
    if (!hasSections()) return;
    finishDrag(); SectionCollapse.toggleAll(sectionKeys, collapsed); rebuildRows(); scroll(scroll());
  }
  private String collapseKey(String group) {
    return (playlistView() ? "playlist/" : "") + (search.text().isBlank() ? "" : "filtered/") + group;
  }
  public State state() {
    return new State(queue, folder, search.text(), trackScroll, queueScroll, "", Set.copyOf(collapsed), "", Set.copyOf(initializedFilters), false);
  }
  public void restore(State state) {
    if (state == null) return;
    closePresets(); collapsed.clear(); collapsed.addAll(state.collapsed);
    initializedFilters.clear(); initializedFilters.addAll(state.initialized);
    queue = state.queue;
    folder = Set.of("active", "library", "playlists", "favorites").contains(state.folder) ? state.folder : "library";
    presetsOpen = !queue && !folder.equals("library");
    search.setText(state.query); trackScroll = state.trackScroll; queueScroll = state.queueScroll; search.focused = false;
  }
  private int left() { return parent.contentLeft(); }
  private int right() { return parent.contentRight(); }
  private int span() { return right() - left(); }
  private int bottom() { return parent.contentBottom(); }
  private int statusTop() { return parent.audioContentTop(); }
  private int controlsTop() { return statusTop() + (showMusicStatus() ? 18 : 0); }
  private boolean catalog() { return presetsOpen && !presets.editing(); }
  private String catalogNewLabel() { return "New playlist"; }
  private int catalogNewWidth() { return textWidth(catalogNewLabel()) + 16; }
  private int catalogNewX() { return right() - catalogNewWidth(); }
  private int catalogNewY() { return controlsTop(); }
  int panelTop() { return controlsTop() + 18 + MusicFilters.CONTENT_GAP; }
  private int actionTop() { return presetsOpen ? parent.musicEditorTop() : filters().actionsY(); }
  private int listTop() {
    return queue ? controlsTop() + 24 : presetsOpen ? actionTop() + presets.headerHeight() + 4
        : controlsTop() + filters().height() + (actionTop() == controlsTop() ? 10 : 6);
  }
  private int groupHeight(String group) { return 20; }
  private int sourceRemoveX() { return right() - 18; }
  private MusicRowLayout rowLayout() { return MusicRowLayout.at(left(), span(), queue); }
  private int rowHeight() { return rowLayout().height(); }
  private int volumeX() { return rowLayout().volumeX(); }
  private int volumeEnd() { return volumeX() + rowLayout().volumeWidth(); }
  private int groupVolumeWidth() { return rowLayout().volumeWidth(); }
  private int groupVolumeX() { return rowLayout().volumeX(); }
  private boolean groupHasVolume(String group) { return playlistView() && presets.editable() && presets.grouped() && !group.equals("Unavailable tracks"); }
  private int reloadX() { return filters().reloadX(); }
  private int addFolderX() { return filters().addFolderX(); }
  private int addFolderWidth() { return filters().overflow() ? 20 : 68; }
  private int reloadWidth() { return filters().overflow() ? 20 : 48; }
  private boolean libraryActionsMenu() {
    return filters().overflow() || !showMusicStatus() && conversionVisible();
  }
  private MusicFilters.Layout filters() {
    return MusicFilters.layout(left(), controlsTop(), span(), folder, activeTabLabel(), this::textWidth);
  }
  private void navigationActions() {
    var layout = filters();
    List<String> labels = new ArrayList<>();
    List<Runnable> actions = new ArrayList<>();
    for (var tab : layout.hiddenTabs()) {
      labels.add(tab.label()); actions.add(() -> selectFilter(tab.id()));
    }
    labels.add("New playlist"); actions.add(this::newPlaylist);
    if (libraryTab()) {
      labels.add("Add folder"); actions.add(folders::add);
      labels.add("Reload"); actions.add(AudioController::reload);
      if (conversionVisible()) { labels.add(Mp3Converter.busy() ? "Cancel MP3" : "Convert MP3"); actions.add(this::convertMp3); }
    }
    parent.musicActions("music-navigation", labels, Collections.nCopies(labels.size(), true),
        reloadX(), controlsTop() + 18, index -> actions.get(index).run());
  }
  private void newPlaylist() {
    guard(() -> {
      finishDrag(); closePresets(); queue = false; folder = "playlists"; presetsOpen = true;
      search.setText(""); search.focused = false; error = "";
      presets.newPlaylist(); syncPanel(); rebuild(); parent.refreshMusicEditor();
    });
  }
  private int scroll() { return queue ? queueScroll : trackScroll; }
  private void scroll(int value) {
    int bounded = Math.max(0, Math.min(value, Math.max(0, contentHeight - (bottom() - listTop()))));
    if (queue) queueScroll = bounded; else trackScroll = bounded;
  }
  private ScrollBar.Track track() { return new ScrollBar.Track(right() + 5, listTop(), Math.max(0, bottom() - listTop()), contentHeight, scroll()); }
  private boolean inList(int x, int y) { return inside(x, y, left(), listTop(), span(), bottom() - listTop()); }

  public void guard(Runnable action) { finishDrag(); if (presetsOpen) presets.guard(action); else action.run(); }
  public void dialogClick(int x, int y, int b) { if (folders.modal()) folders.dialogClick(x, y, b); else { presets.headerClick(x, y, b); syncPanel(); } }
  public void renderDialog(int x, int y) { if (folders.modal()) folders.renderDialog(x, y); else if (presetsOpen) presets.renderDialog(x, y); }
  public void openPresets() { selectFilter("playlists"); }
  public void showTracks() { selectFilter("library"); }
  public void showQueue() {
    finishDrag(); closePresets(); queue = true; search.focused = false; search.setText("");
    error = ""; refresh(); parent.refreshMusicEditor();
  }
  private void selectFilter(String id) {
    if (dirtyPreset()) { guard(() -> selectFilter(id)); return; }
    finishDrag(); closePresets(); queue = false;
    folder = Set.of("active", "library", "playlists", "favorites").contains(id) ? id : "library";
    presetsOpen = !folder.equals("library");
    if (folder.equals("favorites")) presets.showFavorites();
    else if (folder.equals("active")) presets.showSelected();
    search.setText(""); search.focused = false; error = ""; trackScroll = 0;
    refresh(); parent.refreshMusicEditor();
  }
  public void back(Runnable exit) {
    if (reorder.active()) { reorder.cancel(); return; }
    finishDrag();
    if (presets.backFromAdd()) { syncPanel(); return; }
    if (presetsOpen && presets.editing() && folder.equals("playlists")) presets.guard(() -> { presets.open(); refresh(); });
    else if (dirtyPreset()) presets.guard(exit); else exit.run();
  }
  private void closePresets() { presetsOpen = false; presets.open(); previousDraft = null; previousTracks = List.of(); }
  public void close() { removed(); folders.cancel(); closePresets(); }
  public void showTrack(String id) { guard(() -> { selectFilter("library"); revealTrack(id); }); }
  public void showActiveTrack(String id) {
    guard(() -> {
      boolean active = !AudioController.previewing(id)
          && MusicPlaylist.contains(AudioConfig.current(), edits.selected(), id);
      selectFilter(active ? "active" : "library");
      revealTrack(id);
    });
  }
  private void revealTrack(String id) {
    collapsed.remove(collapseKey(group(id))); rebuildRows();
    rows.stream().filter(row -> id.equals(row.id)).findFirst()
        .ifPresent(row -> scroll(Math.max(0, row.y - groupHeight(row.group))));
  }
  public boolean hasQuery() { return !queue && !search.text().isEmpty(); }
  public boolean focused() { return search.focused; }
  public void clearQuery() { finishDrag(); search.setText(""); scroll(0); rebuild(); }
  public void unfocus() { search.focused = false; }
  public void focus() { if (queue) showTracks(); finishDrag(); presets.unfocus(); search.focused = true; search.selectAll(); }
  public void init() {
    finishDrag(); presets.init(minecraft, width, height); folders.init(minecraft, width, height);
    if (folder.equals("favorites") && presetsOpen && !presets.editing()) presets.showFavorites();
    else if (folder.equals("active") && presetsOpen) presets.syncSelected();
    refresh();
  }
  public void removed() { finishDrag(); scrollbar.release(); }
  private void finishDrag() {
    dragging = null; draggingGroup = null; draggingGroupDraft = null; reorder.cancel();
    parent.finishContinuousChange();
  }
  private void refresh() {
    finishDrag(); AudioBackend.discoverMusic(parent.session(), minecraft);
    sourceLibrary = AudioController.sourceLibrary(); sourceFolders.clear();
    sourceLibrary.folders().forEach(source -> sourceFolders.put(source.id(), source));
    sourceValue = edits.setting("musicDirectories").value; orderValue = edits.setting("libraryOrder").value;
    custom.clear(); sourceLibrary.tracks().forEach(t -> custom.put(t.id(), t));
    available = Set.copyOf(AudioController.music(minecraft));
    groupOrder = MusicLibrarySources.order(AudioConfig.current());
    conversionNeeded = Mp3Converter.needed(custom.values()); settings.clear();
    parent.session().settings().forEach(s -> settings.put(s.id, s));
    revision = AudioController.libraryRevision(); syncPanel(); rebuild();
  }
  private void syncPanel() {
    if (presetsOpen && folder.equals("active")) presets.syncSelected();
    if (presetsOpen && folder.equals("favorites") && !presets.editing() && !presets.modal()) presets.showFavorites();
    MusicPresetDraft draft = presets.draft();
    boolean changed = previousDraft != draft || previousAdding != presets.adding() || previousGrouped != presets.grouped()
        || draft != null && !previousTracks.equals(draft.tracks());
    if (!changed) return;
    reorder.cancel();
    if (previousDraft != draft) {
      if (folder.equals("playlists")) {
        if (previousDraft == null && draft != null) catalogQuery = search.text();
        search.setText(draft == null ? catalogQuery : "");
      } else search.setText("");
      trackScroll = 0; playlistScroll = 0; playlistQuery = "";
    }
    else if (!previousAdding && presets.adding()) {
      playlistScroll = trackScroll; playlistQuery = search.text(); search.setText(""); trackScroll = 0;
    } else if (previousAdding && !presets.adding()) { search.setText(playlistQuery); trackScroll = playlistScroll; }
    else if (previousGrouped != presets.grouped()) trackScroll = 0;
    previousDraft = draft; previousAdding = presets.adding(); previousGrouped = presets.grouped();
    previousTracks = draft == null ? List.of() : draft.tracks();
    rebuild();
  }
  private void rebuild() {
    if (presetsOpen && !presets.editing()) {
      presets.filter(search.text());
      tracks = List.of(); rows = List.of(); sectionKeys = List.of(); groupTracks.clear(); contentHeight = 0;
      return;
    }
    List<String> source = queue ? MusicRequests.tracks() : playlistView() ? presets.draft().tracks() : new ArrayList<>(available);
    groupTracks.clear(); source.forEach(id -> groupTracks.computeIfAbsent(group(id), key -> new ArrayList<>()).add(id));
    libraryGroupCounts.clear();
    if (groupedView() && (playlistView() || presets.adding()))
      available.forEach(id -> libraryGroupCounts.merge(group(id), 1, Integer::sum));
    if (queue) tracks = List.copyOf(source);
    else if (playlistView()) tracks = source.stream().filter(this::matches).toList();
    else tracks = source.stream().sorted(Comparator.comparing(AudioController::musicLabel, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(Comparator.naturalOrder())).filter(this::matches).toList();
    rebuildRows(); scroll(scroll());
  }
  private boolean matches(String id) { return matchesAlias(search.text(), AudioController.musicLabel(id)); }
  static boolean matchesAlias(String query, String alias) {
    return alias.toLowerCase(Locale.ROOT).contains(query.strip().toLowerCase(Locale.ROOT));
  }
  private String group(String id) {
    if (!available.contains(id)) return "Unavailable tracks";
    String source = playlistView() ? null : sourceLibrary.trackSources().get(id);
    return source == null ? MusicGroups.id(id, custom.get(id)) : source;
  }
  private String groupLabel(String group) {
    var source = sourceFolders.get(group);
    if (source != null) return source.label();
    return group.startsWith("folder:") ? Objects.toString(Path.of(group.substring(7)).getFileName(), group.substring(7)) : group;
  }
  private List<String> members(String group) { return groupTracks.getOrDefault(group, List.of()); }
  private int includedCount(String group) { return presets.editing() ? (int)members(group).stream().filter(presets.draft()::included).count() : 0; }
  private void addGroup(String group) {
    if (!presets.editable() || !presets.adding()) return;
    presets.draft().appendTracks(members(group).stream().sorted(Comparator.comparing(AudioController::musicLabel, String.CASE_INSENSITIVE_ORDER).thenComparing(Comparator.naturalOrder())).toList()); syncPanel();
  }
  private void rebuildRows() {
    List<Row> result = new ArrayList<>(); int y = 0;
    Map<String, Integer> positions = new HashMap<>();
    if (playlistView()) { var ids = presets.draft().tracks(); for (int i = 0; i < ids.size(); i++) positions.put(ids.get(i), i); }
    if (!groupedView()) {
      for (int i = 0; i < tracks.size(); i++, y += rowHeight())
        result.add(new Row("", "", tracks.get(i), queue ? i : positions.get(tracks.get(i)), y, rowHeight()));
    } else {
      Map<String, List<String>> groups = new LinkedHashMap<>();
      tracks.forEach(id -> groups.computeIfAbsent(group(id), key -> new ArrayList<>()).add(id));
      if ((libraryTab() || presets.adding()) && search.text().isBlank()) for (var source : sourceFolders.values())
        groups.computeIfAbsent(source.id(), key -> new ArrayList<>());
      List<String> order = playlistView() ? new ArrayList<>(groups.keySet()) : new ArrayList<>(groupOrder.stream().filter(groups::containsKey).toList());
      for (String key : groups.keySet()) if (!order.contains(key)) order.add(key);
      for (String key : order) {
        String collapse = collapseKey(key);
        if (initializedFilters.add(collapse) && !playlistView() && search.text().isBlank()) collapsed.add(collapse);
        int heading = groupHeight(key); result.add(new Row(key, groupLabel(key), null, -1, y, heading)); y += heading;
        if (collapsed.contains(collapse)) continue;
        for (String id : groups.get(key)) { result.add(new Row(key, groupLabel(key), id, positions.getOrDefault(id, -1), y, rowHeight())); y += rowHeight(); }
      }
    }
    rows = List.copyOf(result); sectionKeys = rows.stream().filter(row -> row.id == null).map(row -> collapseKey(row.group)).toList(); contentHeight = y;
  }
  private String path(String id) {
    MusicLibrary.Track t = custom.get(id);
    return t != null ? t.path().toString() : available.contains(id) ? "Included Minecraft music" : id + "\nTrack unavailable. Its playlist entry is preserved.";
  }
  private boolean playable(String id) { MusicLibrary.Track t = custom.get(id); return available.contains(id) && (t == null || t.playable()); }
  public void tick() {
    presets.tickChoices();
    if (conversionRevision != Mp3Converter.revision()) { conversionRevision = Mp3Converter.revision(); conversionTicks = 200; }
    else if (conversionTicks > 0) conversionTicks--;
    if (revision != AudioController.libraryRevision() || sourceLibrary != AudioController.sourceLibrary()
        || sourceValue != edits.setting("musicDirectories").value || orderValue != edits.setting("libraryOrder").value) refresh();
    else { syncPanel(); if (queue && !tracks.equals(MusicRequests.tracks())) { reorder.cancel(); rebuild(); } }
  }
  public void wheel(int amount) {
    if (folders.modal()) return;
    if (presetsOpen && (presets.modal() || !presets.editing())) { presets.wheel(amount); return; }
    if (amount != 0) scroll(scroll() - (int)Math.signum(amount) * rowHeight());
  }
  private void slide(int x) {
    if (dragging == null) return;
    var before = dragging.value; dragging.slide((x - volumeX() - 4d) / Math.max(1, volumeEnd() - volumeX() - 8));
    if (!before.equals(dragging.value)) parent.changed(dragging, true);
  }
  private void slideGroup(int x) {
    if (!presets.editable() || draggingGroup == null || draggingGroupDraft != presets.draft()) { finishDrag(); return; }
    double fraction = (x - groupVolumeX() - 4d) / Math.max(1, groupVolumeWidth() - 8);
    draggingGroupDraft.volume(draggingGroup, (int)Math.round(100 * Math.max(0, Math.min(1, fraction))));
  }
  private boolean queueEdit(java.util.function.Consumer<MusicQueue> change) {
    try { MusicRequests.edit(change); error = ""; rebuild(); return true; }
    catch (Exception e) { error = Objects.toString(e.getMessage(), "Could not save queue"); return false; }
  }
  public void searchClick(int x, int y, int button) {
    if (queue) return;
    finishDrag(); presets.unfocus(); search.focused = true; String previous = search.text(); search.click(x, button);
    if (!previous.equals(search.text())) clearQuery();
  }
  protected void keyPressed(char c, int key) {
    if (folders.modal()) { folders.key(key); return; }
    if (presetsOpen && presets.modal()) { presets.key(c, key); return; }
    if (reorder.active() && key == org.lwjgl.input.Keyboard.KEY_ESCAPE) { reorder.cancel(); return; }
    if (presetsOpen && !search.focused) presets.key(c, key);
    if (!queue && search.focused) { reorder.cancel(); search.key(c, key); scroll(0); rebuild(); }
  }
  private StickyHeader stickyHeader() { return StickyHeader.at(rows, row -> row.id == null, Row::y, Row::height, scroll(), listTop(), bottom()); }
  private Row rowAt(int y) {
    StickyHeader sticky = stickyHeader();
    if (sticky != null && sticky.contains(y, listTop())) return rows.get(sticky.index());
    return rows.stream().filter(row -> y - listTop() + scroll() >= row.y && y - listTop() + scroll() < row.y + row.height).findFirst().orElse(null);
  }
  private int rowY(Row row) {
    var sticky = stickyHeader(); return sticky != null && rows.get(sticky.index()) == row ? sticky.y() : listTop() + row.y - scroll();
  }
  protected void mouseClicked(int x, int y, int b) {
    if (b != 0 && b != 1) return;
    finishDrag();
    if (modal()) { dialogClick(x, y, b); return; }
    if (!queue) for (var tab : filters().tabs()) if (b == 0 && inside(x, y, tab.x(), tab.y(), tab.width(), 18)) { click(); selectFilter(tab.id()); return; }
    if (!queue && filters().overflow() && b == 0 && inside(x, y, reloadX(), controlsTop(), 20, 18)) {
      click(); navigationActions(); return;
    }
    if (catalog() && !filters().overflow() && b == 0 && inside(x, y, catalogNewX(), catalogNewY(), catalogNewWidth(), 18)) {
      click(); newPlaylist(); return;
    }
    if (presetsOpen && (!presets.editing() || y < listTop())) { search.focused = false; presets.headerClick(x, y, b); syncPanel(); rebuild(); return; }
    if (b == 0 && scrollbar.press(track(), x, y)) { scroll((int)scrollbar.drag(track(), y, true)); return; }
    search.focused = false; presets.unfocus();
    if (queue && b == 0 && inside(x, y, right() - 82, controlsTop(), 82, 18)) { if (!tracks.isEmpty()) { click(); queueEdit(q -> q.tracks.clear()); } return; }
    if (libraryTab() && b == 0 && inside(x, y, addFolderX(), actionTop(), addFolderWidth(), 18)) {
      click();
      if (libraryActionsMenu()) {
        List<String> actions = new ArrayList<>(List.of("Add folder", "Reload"));
        if (conversionVisible()) actions.add(Mp3Converter.busy() ? "Cancel MP3" : "Convert MP3");
        parent.musicActions("music-library-actions", actions, Collections.nCopies(actions.size(), true),
            addFolderX(), actionTop() + 18, action -> {
              if (action == 0) folders.add();
              else if (action == 1) AudioController.reload();
              else convertMp3();
            });
      } else folders.add();
      return;
    }
    if (libraryTab() && !libraryActionsMenu() && b == 0 && inside(x, y, reloadX(), actionTop(), reloadWidth(), 18)) { click(); AudioController.reload(); return; }
    if (conversionVisible() && showMusicStatus() && b == 0 && inside(x, y, right() - 88, statusTop(), 88, 14)) { click(); convertMp3(); return; }
    if (!inList(x, y)) return;
    Row row = rowAt(y); if (row == null) return;
    if (row.id == null) { headingClick(row, x, y, rowY(row) + 1, b); return; }
    var layout = rowLayout(); Setting volume = settings.get("audio.sound." + row.id);
    int cy = rowY(row) + layout.controlsY();
    if (presets.adding() && inside(x, y, layout.nameX(), cy, 18, 18)) {
      if (b == 0) { click(); presets.draft().include(row.id, !presets.draft().included(row.id)); syncPanel(); rebuild(); }
      return;
    }
    if (inside(x, y, volumeX(), cy, layout.volumeWidth(), 18)) {
      if (volume != null) {
        if (b == 0) { dragging = volume; slide(x); }
        else parent.valueEditor.begin(row, volume, () -> parent.changed(volume), volumeX(), cy, layout.volumeWidth(), listTop(), bottom());
      }
      return;
    }
    if (inside(x, y, layout.favouriteX(), cy, layout.buttonWidth(), 18)) {
      if (b == 0) { click(); toggleFavorite(row.id); }
      return;
    }
    if (inside(x, y, layout.playX(), cy, layout.buttonWidth(), 18)) {
      if (b == 0 && playable(row.id)) {
        click();
        try {
          if (playlistView() && !presets.draft().creating() && !edits.selected().equals(presets.draft().id()))
            edits.load(presets.draft().id());
          AudioController.toggleTrack(row.id);
        } catch (RuntimeException failure) { error = Objects.toString(failure.getMessage(), "Could not play this song."); }
      }
      return;
    }
    if (b == 1) { context(row, x, y); return; }
    if (canOrderTracks()) {
      List<String> ids = queue ? MusicRequests.tracks() : presets.draft().tracks();
      if (queue && !renderedQueue.equals(ids)) { rebuild(); error = "Queue changed. Select the track again."; return; }
      var slots = rows.stream().filter(r -> r.id != null).map(r -> new MusicListDrag.Slot(r.id, r.y, r.height)).toList();
      reorder.begin(queue ? MusicListDrag.Kind.QUEUE : MusicListDrag.Kind.PLAYLIST, queue ? null : presets.draft(), ids, slots, row.index, x, y);
    }
  }
  private void convertMp3() {
    if (Mp3Converter.busy()) Mp3Converter.cancel();
    else Mp3Converter.start(FabricLoader.getInstance().getGameDir(), sourceLibrary.tracks());
  }
  private void headingClick(Row row, int x, int y, int cy, int b) {
    if (managedSource(row.group) && b == 0 && inside(x, y, sourceRemoveX(), cy, 18, 18)) { click(); var source = sourceFolders.get(row.group); folders.remove(source.path(), source.label()); return; }
    if (presets.editable() && presets.adding() && inside(x, y, right() - 40, cy, 40, 18)) { if (b == 0 && includedCount(row.group) < members(row.group).size()) { click(); addGroup(row.group); } return; }
    if (groupHasVolume(row.group) && inside(x, y, groupVolumeX(), cy, groupVolumeWidth(), 18)) {
      if (b == 0) { draggingGroup = row.group; draggingGroupDraft = presets.draft(); slideGroup(x); }
      else {
        MusicPresetDraft draft = presets.draft(); Setting value = new Setting("music.draftGroupVolume", "audio", "Audio", "Music", "Group volume", "Volume for this playlist", Setting.Kind.INTEGER,
            new com.google.gson.JsonPrimitive(draft.volume(row.group)), new com.google.gson.JsonPrimitive(100), 0, 100, 1, List.of(), false);
        parent.valueEditor.begin("musicGroup/" + row.group, value, () -> { if (presets.editable() && presets.draft() == draft) draft.volume(row.group, value.value.getAsInt()); }, groupVolumeX(), cy, groupVolumeWidth(), listTop(), bottom());
      }
      return;
    }
    if (b != 0) return;
    if (libraryTab() && search.text().isBlank() && x >= left() + 14) {
      List<Row> headings = rows.stream().filter(r -> r.id == null).toList(); List<String> ids = headings.stream().map(Row::group).toList();
      reorder.begin(MusicListDrag.Kind.GROUPS, sourceLibrary, ids,
          headings.stream().map(r -> new MusicListDrag.Slot(r.group, r.y, r.height)).toList(), ids.indexOf(row.group), x, y);
    } else toggleHeading(row.group);
  }
  private boolean favorite(String id) {
    return presetsOpen && presets.favorites() ? presets.draft().included(id) : edits.favourites().contains(id);
  }
  private void toggleFavorite(String id) {
    if (presetsOpen && presets.favorites()) {
      if (presets.editable()) presets.draft().include(id, !presets.draft().included(id));
    } else edits.favourite(id);
    syncPanel(); rebuild();
  }
  private void toggleHeading(String group) { if (!collapsed.remove(collapseKey(group))) collapsed.add(collapseKey(group)); rebuildRows(); scroll(scroll()); }
  private void context(Row row, int x, int y) {
    String id = row.id; MusicPresetDraft owner = presets.editing() ? presets.draft() : null;
    List<String> queueSnapshot = queue ? List.copyOf(renderedQueue) : List.of();
    boolean favorite = owner != null && owner.favorites() ? owner.included(id) : edits.favourites().contains(id);
    boolean stopPreview = AudioController.previewing(id);
    List<String> labels = new ArrayList<>(List.of("Add to queue", favorite ? "Remove favourite" : "Favourite", stopPreview ? "Stop preview" : "Preview"));
    List<Boolean> enabled = new ArrayList<>(List.of(true, true, stopPreview || playable(id)));
    if (presets.editable() && presets.adding()) { labels.add("Add to playlist"); enabled.add(!owner.included(id)); }
    else if (playlistView() && presets.editable() && !owner.favorites()) { labels.add("Remove from playlist"); enabled.add(true); }
    else if (queue) { labels.add("Remove from queue"); enabled.add(true); }
    parent.musicActions(row, labels, enabled, x, y, action -> {
      if (owner != null && (presets.draft() != owner || !presetsOpen)) { error = "Playlist changed. Select the song again."; return; }
      if (queue && !queueSnapshot.equals(MusicRequests.tracks())) { error = "Queue changed. Select the song again."; rebuild(); return; }
      try {
        switch (action) {
          case 0 -> queueEdit(q -> q.add(id));
          case 1 -> {
            if (owner != null && owner.favorites() && presets.editable()) owner.include(id, !favorite);
            else if (edits.favourites().contains(id) == favorite) edits.favourite(id);
          }
          case 2 -> { if (stopPreview ? AudioController.previewing(id) : playable(id)) AudioController.previewSound(id); }
          default -> {
            if (presets.editable() && presets.adding()) owner.appendTracks(List.of(id));
            else if (playlistView() && presets.editable()) owner.include(id, false);
            else if (queue) queueEdit(q -> q.tracks.remove(row.index));
          }
        }
        syncPanel(); rebuild();
      } catch (RuntimeException failure) { error = Objects.toString(failure.getMessage(), "Could not change this song."); }
    });
  }
  protected void mouseReleased(int x, int y, int b) {
    presets.release(y, b);
    if (b != 0) return;
    if (dragging != null || draggingGroup != null) {
      if (dragging != null) slide(x); else slideGroup(x);
      dragging = null; draggingGroup = null; draggingGroupDraft = null; parent.finishContinuousChange();
    }
    scroll((int)scrollbar.drag(track(), y, true)); scrollbar.release();
    if (!reorder.active()) return;
    reorder.update(x, y);
    if (!reorder.active()) return;
    try {
      if (!inList(x, y) || !Display.isActive()) return;
      if (!reorder.moved()) { if (reorder.kind() == MusicListDrag.Kind.GROUPS) toggleHeading(reorder.original().get(reorder.index())); return; }
      List<String> next = new ArrayList<>(reorder.original()); int slot = reorder.slot(y - listTop() + scroll());
      if (reorder.kind() == MusicListDrag.Kind.GROUPS) {
        var current = rows.stream().filter(r -> r.id == null).map(Row::group).toList();
        if (reorder.owner() != sourceLibrary || !current.equals(reorder.original())) throw new IllegalArgumentException("Library changed. Drag the group again.");
        if (ListReorder.move(next, reorder.index(), slot)) edits.libraryOrder(next);
      } else if (reorder.kind() == MusicListDrag.Kind.PLAYLIST) {
        if (!canOrderTracks() || presets.draft() != reorder.owner() || !presets.draft().tracks().equals(reorder.original())) throw new IllegalArgumentException("Playlist changed. Drag the song again.");
        if (ListReorder.move(next, reorder.index(), slot)) presets.draft().order(next);
      } else {
        if (!MusicRequests.tracks().equals(reorder.original())) throw new IllegalArgumentException("Queue changed. Drag the song again.");
        if (ListReorder.move(next, reorder.index(), slot)) queueEdit(q -> { q.tracks.clear(); q.tracks.addAll(next); });
      }
      error = "";
    } catch (RuntimeException failure) { error = Objects.toString(failure.getMessage(), "Could not reorder this list."); }
    finally { reorder.cancel(); groupOrder = MusicLibrarySources.order(AudioConfig.current()); syncPanel(); rebuild(); }
  }
  public void renderSearch(int x, int y, int w, int mx, int my) {
    if (queue) text("Music queue", x + 2, y + 5, 0xdddddd);
    else input(search, x, y, w, mx, my, catalog() ? "Search playlists..."
        : presets.favorites() ? "Search favorites..."
        : presets.editing() && !presets.adding() ? "Search playlist..." : "Search library...");
  }
  private record GroupHover(String tip, String key) {}
  private GroupHover groupHeading(Row row, int y, int mx, int my) {
    boolean hover = inside(mx, my, left(), y, span(), row.height) && my >= listTop() && my < bottom();
    boolean closed = collapsed.contains(collapseKey(row.group)); int total = members(row.group).size();
    var source = sourceFolders.get(row.group); boolean managed = managedSource(row.group);
    String count = presets.adding() || playlistView()
        ? includedCount(row.group) + "/" + libraryGroupCounts.getOrDefault(row.group, 0) : Integer.toString(total);
    int countRight = managed ? sourceRemoveX() - 6 : presets.adding() ? right() - 46 : right() - 4;
    if (managed) iconButton("remove", sourceRemoveX(), y + 1, 18, mx, my, true);
    if (presets.editable() && presets.adding()) button("Add", right() - 40, y + 1, 40, 18, mx, my, includedCount(row.group) < total);
    if (groupHasVolume(row.group)) {
      int volume = presets.draft().volume(row.group);
      if (parent.valueEditor.editing("musicGroup/" + row.group)) parent.valueEditor.render(this, groupVolumeX(), y + 1, groupVolumeWidth(), mx, my, listTop(), bottom());
      else slider(volume + "%", groupVolumeX(), y + 1, groupVolumeWidth(), mx, my, volume / 100d);
    }
    int labelRight = groupHasVolume(row.group) ? groupVolumeX() - 6 : countRight - textWidth(count) - 6;
    text((closed ? "> " : "v ") + fit(row.label, Math.max(12, labelRight - left() - 12)), left() + 2, y + 6, 0xffffff);
    text(count, countRight - textWidth(count), y + 6, 0xaaaaaa); fill(left(), y + row.height - 1, right(), y + row.height, 0x50555555);
    if (!hover) return new GroupHover("", "");
    String tip = source != null ? source.path() : row.group.startsWith("folder:") ? row.group.substring(7) : row.label;
    return new GroupHover(total + (total == 1 ? " song in " : " songs in ") + tip, "group:" + row.group);
  }
  public void render(int mx, int my, float delta) {
    syncPanel(); String tip = "", key = "";
    if (!queue) {
      var layout = filters();
      MusicTabs.render(this, layout, folder,
          layout.overflow() ? reloadX() - 4 : libraryTab() ? addFolderX() - 4
              : catalog() ? catalogNewX() - 4 : right(), mx, my);
      if (layout.overflow()) button("...", reloadX(), controlsTop(), 20, 18, mx, my, true,
          parent.musicActionsOpen("music-navigation") ? 0xffffa0 : -1);
    }
    if (catalog() && !filters().overflow()) button(catalogNewLabel(), catalogNewX(), catalogNewY(), catalogNewWidth(), 18, mx, my, true);
    if (presetsOpen) {
      presets.renderHeader(mx, my);
      if (!presets.editing()) { hoverHelp = hoverKey = ""; return; }
    }
    if (!modal()) scroll((int)scrollbar.drag(track(), my, Mouse.isButtonDown(0)));
    if (dragging != null) { if (Mouse.isButtonDown(0)) slide(mx); else { dragging = null; parent.finishContinuousChange(); } }
    if (draggingGroup != null) { if (Mouse.isButtonDown(0)) slideGroup(mx); else { draggingGroup = null; draggingGroupDraft = null; } }
    reorder.update(mx, my);
    if (reorder.active() && reorder.moved() && inList(mx, my) && reorder.scrollDue()) {
      if (my >= bottom() - 14) scroll(scroll() + rowHeight()); else if (my < listTop() + 14) scroll(scroll() - rowHeight());
    }
    if (queue) { text("Up next", left() + 2, controlsTop() + 5, 0xdddddd); button("Clear queue", right() - 82, controlsTop(), 82, 18, mx, my, !tracks.isEmpty()); renderedQueue = List.copyOf(tracks); }
    else if (libraryTab() && !filters().overflow()) {
      if (libraryActionsMenu()) {
        button("...", addFolderX(), actionTop(), addFolderWidth(), 18, mx, my, true,
            parent.musicActionsOpen("music-library-actions") ? 0xffffa0 : -1);
      } else {
        button("Add folder", addFolderX(), actionTop(), addFolderWidth(), 18, mx, my, true);
        button("Reload", reloadX(), actionTop(), reloadWidth(), 18, mx, my, true);
      }
      if (inside(mx, my, addFolderX(), actionTop(), addFolderWidth(), 18)) tip = libraryActionsMenu() ? "Library actions" : "Add folder";
      else if (!libraryActionsMenu() && inside(mx, my, reloadX(), actionTop(), reloadWidth(), 18)) tip = "Reload";
    }
    String playingId = AudioController.musicPlaying() ? AudioController.currentTrackId() : "";
    boolean playingPreview = !playingId.isEmpty() && AudioController.previewing(playingId);
    StickyHeader sticky = stickyHeader(); int rowsTop = sticky == null ? listTop() : Math.max(listTop(), sticky.y() + sticky.height());
    clip(left() - 2, rowsTop, span() + 4, Math.max(0, bottom() - rowsTop)); int listMouseY = sticky != null && sticky.contains(my, listTop()) ? -1 : my;
    for (Row row : rows) {
      int y = listTop() + row.y - scroll(); if (y + row.height <= listTop() || y >= bottom()) continue;
      if (row.id == null) { if (sticky == null || row != rows.get(sticky.index())) { var h = groupHeading(row, y, mx, listMouseY); if (!h.tip.isEmpty()) { tip = h.tip; key = h.key; } } continue; }
      String id = row.id; var layout = rowLayout(); int cy = y + layout.controlsY();
      boolean hover = inside(mx, listMouseY, left(), y, span(), row.height) && listMouseY >= rowsTop && listMouseY < bottom();
      if (hover) { tip = path(id); key = "track:" + id; }
      if (hover && !reorder.active()) fill(left() - 2, y, right(), y + row.height, 0x60000000);
      int nameX = layout.nameX();
      if (presets.adding()) {
        boolean included = presets.draft().included(id);
        button(included ? "x" : "+", nameX, cy, 18, 18, mx, listMouseY, true, included ? 0xffffdd66 : -1);
        nameX += 22;
        if (hover && inside(mx, listMouseY, layout.nameX(), cy, 18, 18))
          tip = included ? "Remove from playlist" : "Add to playlist";
      }
      String label = AudioController.musicLabel(id);
      text(fit(label, volumeX() - nameX - 6), nameX, y + 7, id.equals(playingId) ? 0xffffdd66 : playable(id) ? 0xdddddd : 0xffbb77);
      Setting volume = settings.get("audio.sound." + id);
      if (volume != null) {
        if (parent.valueEditor.editing(row)) parent.valueEditor.render(this, volumeX(), cy, layout.volumeWidth(), mx, listMouseY, rowsTop, bottom());
        else slider(volume.display(), volumeX(), cy, layout.volumeWidth(), mx, listMouseY, volume.value.getAsDouble() / 100);
      }
      iconButton("star", layout.favouriteX(), cy, layout.buttonWidth(), mx, listMouseY, true, favorite(id));
      iconButton(!playingPreview && id.equals(playingId) ? "pause" : "play", layout.playX(), cy, layout.buttonWidth(), mx, listMouseY, playable(id));
      if (hover && inside(mx, listMouseY, volumeX(), cy, layout.volumeWidth(), 18)) tip = parent.valueEditor.editing(row) ? parent.valueEditor.help() : "Track volume";
      else if (hover && inside(mx, listMouseY, layout.favouriteX(), cy, layout.buttonWidth(), 18)) tip = favorite(id) ? "Remove favourite" : "Favourite";
      else if (hover && inside(mx, listMouseY, layout.playX(), cy, layout.buttonWidth(), 18)) tip = "";
    }
    if (sticky != null) { clip(left() - 2, listTop(), span() + 4, Math.max(0, bottom() - listTop())); var h = groupHeading(rows.get(sticky.index()), sticky.y(), mx, my); if (!h.tip.isEmpty()) { tip = h.tip; key = h.key; } }
    if (reorder.active() && reorder.moved() && inList(mx, my)) {
      int slot = reorder.slot(my - listTop() + scroll()); int natural = slot == reorder.original().size() ? contentHeight : reorder.lineY(slot);
      int line = Math.max(listTop(), Math.min(bottom() - 2, listTop() + natural - scroll())); fill(left(), line, right(), line + 2, 0xffffdd88);
    }
    if (rows.isEmpty()) text(fit(queue ? "Queue is empty" : playlistView() && search.text().isBlank() ? "No songs. Use Add songs." : "No matching songs", span() - 8), left() + 4, listTop() + 12, 0xaaaaaa);
    unclip(); scrollbar.render(this, track());
    if (conversionVisible() && showMusicStatus()) { button(Mp3Converter.busy() ? "Cancel MP3" : "Convert MP3", right() - 88, statusTop(), 88, 14, mx, my, true); if (inside(mx, my, right() - 88, statusTop(), 88, 14)) tip = "Convert MP3 to a playback cache. Original files stay unchanged."; }
    String failure = failure();
    String status = !failure.isEmpty() ? failure : Mp3Converter.busy() || conversionTicks > 0 ? Mp3Converter.status() : AudioController.status();
    if (showMusicStatus() && !trackStatusVisible()) text(fit(status, span() - (conversionVisible() ? 92 : 0)), left(), statusTop() + 4, failure.isEmpty() ? 0xaaaaaa : 0xff8888);
    if (modal() || reorder.active()) tip = key = "";
    hoverHelp = tip; hoverKey = key.isEmpty() ? tip : key;
  }
}
