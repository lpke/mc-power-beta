package local.luke.power.ui;

import java.nio.file.Path;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.Setting;
import local.luke.power.config.backend.AudioBackend;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.input.Mouse;

/** Embedded Audio panel. Edits belong to the surrounding Options session. */
public final class MusicLibraryScreen extends UiScreen {
  public record State(
      boolean queue, String folder, String query, int trackScroll, int queueScroll, String lastFolder, Set<String> collapsed, String presetFilter, Set<String> initialized) {
    public State(boolean queue, String folder, String query, int trackScroll, int queueScroll, String lastFolder, Set<String> collapsed, String presetFilter) {
      this(queue,folder,query,trackScroll,queueScroll,lastFolder,collapsed,presetFilter,Set.of());
    }
    public State(boolean queue, String folder, String query, int trackScroll, int queueScroll, String lastFolder, Set<String> collapsed) {
      this(queue,folder,query,trackScroll,queueScroll,lastFolder,collapsed,"");
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
  private boolean presetsOpen;
  private MusicPresetDraft previousDraft;
  private String selectedPreset = "";
  private final Set<String> initializedFilters = new HashSet<>();
  private final TextInput search = new TextInput("", 128);
  private final Map<String, Setting> settings = new HashMap<>();
  private final Map<String, List<Setting>> groupSettings = new HashMap<>();
  private List<String> tracks = List.of(), folders = List.of();
  private record Row(String group, String label, String id, int index, int y, int height) {}
  private final Set<String> collapsed = new HashSet<>();
  private List<Row> rows = List.of();
  private int contentHeight;

  private final Map<String, MusicLibrary.Track> custom = new HashMap<>();
  private final ScrollBar scrollbar = new ScrollBar();
  private int conversionRevision, conversionTicks, trackScroll, queueScroll;
  private boolean queue, conversionNeeded;
  private String folder = "active", lastFolder = "", error = "", hoverHelp = "";
  private Set<String> activeTracks = Set.of();

  public boolean trackStatusVisible() { return error.isEmpty() && !Mp3Converter.busy() && conversionTicks == 0; }

  public boolean conversionVisible() {
    return !queue && !presetsOpen && (conversionNeeded || Mp3Converter.busy());
  }

  public String hoverHelp() {
    return hoverHelp;
  }

  private int revision = -1;
  private Setting dragging;
  private List<String> renderedQueue = List.of();

  public MusicLibraryScreen(PowerOptionsScreen parent) {
    this.parent = parent;
    edits = new MusicEdits(parent); presets = new MusicPresetsPanel(parent);
  }

  public State state() {
    return new State(queue, folder, search.text(), trackScroll, queueScroll, lastFolder, Set.copyOf(collapsed), selectedPreset, Set.copyOf(initializedFilters));
  }

  public void restore(State state) {
    if (state == null) return;
    closePresets();
    collapsed.clear(); collapsed.addAll(state.collapsed);
    initializedFilters.clear(); initializedFilters.addAll(state.initialized);
    queue = state.queue;
    folder = state.folder;
    lastFolder = state.lastFolder;
    selectedPreset = state.presetFilter;
    search.setText(state.query);
    trackScroll = state.trackScroll;
    queueScroll = state.queueScroll;
    search.focused = false;
  }

  private int left() {
    return parent.contentLeft();
  }

  private int right() {
    return parent.contentRight();
  }

  private int span() {
    return right() - left();
  }

  private int bottom() {
    return parent.contentBottom();
  }

  private int listTop() {
    return presetsOpen ? statusTop() + 4 + presets.headerHeight() : controlsTop()
        + (queue ? 18 : Math.max(filters().height(), bulkTop() - controlsTop() + 18)) + 6;
  }

  private int statusTop() { return parent.audioContentTop(); }
  private int controlsTop() { return statusTop() + 18; }
  private int bulkTop() {
    return controlsTop() + (span() < 380 ? 22 : span() < 504 ? filters().height() + 4 : 0);
  }
  private int includeX() { return right() - 148; }
  private int excludeX() { return right() - 72; }

  private boolean narrow() {
    return span() < 350;
  }

  private int rowHeight() {
    return rowLayout().height();
  }

  private MusicRowLayout rowLayout() { return MusicRowLayout.at(left(), span(), queue); }

  private int filterX() {
    return queue ? right() - 82 : left();
  }

  private int filterWidth() { return queue ? 82 : Math.min(240, span()); }
  public boolean queueVisible() { return queue && !presetsOpen; }
  public boolean presetVisible() { return presetsOpen; }
  public boolean modal() { return presetsOpen && presets.modal(); }
  public boolean dirtyPreset() { return presetsOpen && presets.dirty(); }
  public void guard(Runnable action) { if (presetsOpen) presets.guard(action); else action.run(); }
  public void dialogClick(int x,int y,int b) { presets.headerClick(x,y,b); }
  public void renderDialog(int x,int y) { if (presetsOpen) presets.renderDialog(x,y); }
  public void openPresets(boolean choosing) {
    presetsOpen = true; queue = false; presets.open(choosing); search.setText(""); search.focused=false; rebuild();
  }
  public void back(Runnable exit) {
    if (presetsOpen && presets.editing()) presets.guard(() -> { presets.open(false); refresh(); });
    else exit.run();
  }


  private void closePresets() {
    presetsOpen = false;
    presets.open(false);
    previousDraft = null;
  }

  public void close() {
    removed();
    closePresets();
  }

  private void show(boolean queue) {
    dragging = null;
    scrollbar.release();
    parent.finishContinuousChange();
    search.focused = false;
    this.queue = queue;
    closePresets();
    error = "";
    rebuild();
  }

  public void showQueue() { show(true); }
  public void showTracks() { show(false); }

  public void showTrack(String id) {
    show(false);
    folder = "";
    search.setText("");
    rebuild();
    collapsed.remove(group(id));
    rebuildRows();
    rows.stream().filter(row -> id.equals(row.id)).findFirst().ifPresent(row -> scroll(row.y));
  }

  private boolean folderSelected() {
    return !Set.of("active", "", "custom", "favourites", "presets").contains(folder);
  }

  private MusicFilters.Layout filters() {
    if (span() < 380) {
      String label = folderSelected() ? "Folders" : switch (folder) {
        case "active" -> "Active"; case "custom" -> "All folders"; case "favourites" -> "Favourites";
        case "presets" -> "Presets"; default -> "Everything";
      };
      boolean selector = folderSelected() || folder.equals("presets");
      return new MusicFilters.Layout(List.of(new MusicFilters.Tab("cycle",label,left(),controlsTop(),span())),
          left(),controlsTop()+44,span(),selector?62:40);
    }
    return MusicFilters.layout(left(), controlsTop(), span() - (span() < 504 ? 0 : 152), folderSelected() || folder.equals("presets"));
  }

  private void selectFilter(String id) {
    folder = id.equals("folder")
        ? folders.contains(lastFolder) ? lastFolder : folders.isEmpty() ? "folder" : folders.get(0)
        : id;
    if (folderSelected()) lastFolder = folder;
    if (folder.equals("presets") && edits.presets().stream().noneMatch(p -> p.id().equals(selectedPreset)))
      selectedPreset = edits.presets().isEmpty() ? "" : edits.presets().get(0).id();
    scroll(0);
    rebuild();
  }

  private int scroll() {
    return queue ? queueScroll : trackScroll;
  }

  private void scroll(int value) {
    int bounded =
        Math.max(
            0, Math.min(value, Math.max(0, contentHeight - (bottom() - listTop()))));
    if (queue) queueScroll = bounded;
    else trackScroll = bounded;
  }

  public void top() {
    scroll(0);
  }

  private int volumeX() { return rowLayout().volumeX(); }
  private int volumeEnd() { return volumeX() + rowLayout().volumeWidth(); }

  private ScrollBar.Track track() {
    return new ScrollBar.Track(
        right() + 5, listTop(), bottom() - listTop(), contentHeight, scroll());
  }

  public boolean hasQuery() {
    return !queue && (!presetsOpen || presets.editing()) && !search.text().isEmpty();
  }

  public boolean focused() {
    return search.focused;
  }

  public void clearQuery() {
    search.setText("");
    scroll(0);
    rebuild();
  }

  public void unfocus() {
    search.focused = false;
  }

  public void focus() {
    if (queue) showTracks();
    presets.unfocus();
    search.focused = true;
    search.selectAll();
  }

  public void init() {
    presets.init(minecraft,width,height);
    refresh();
  }

  public void removed() {
    dragging = null;
    scrollbar.release();
    parent.finishContinuousChange();
  }

  private void click() {
    minecraft.soundManager.method_2009("random.click", 1, 1);
  }

  private void refresh() {
    AudioBackend.discoverMusic(parent.session(), minecraft);
    custom.clear();
    AudioController.customTracks().forEach(t -> custom.put(t.id(), t));
    conversionNeeded = Mp3Converter.needed(custom.values());
    TreeSet<String> paths = new TreeSet<>(Comparator.comparing((String path) -> path.toLowerCase(Locale.ROOT)).thenComparing(Comparator.naturalOrder()));
    for (List<String> roots : List.of(AudioConfig.current().musicDirectories, AudioConfig.current().menuDirectories)) {
      for (String root : roots) {
        Path resolved = MusicLibrary.resolve(FabricLoader.getInstance().getGameDir(), root).toAbsolutePath().normalize();
        try { resolved = resolved.toRealPath(); } catch (java.io.IOException ignored) { /* Keep unavailable folders selectable. */ }
        paths.add(resolved.toString());
      }
    }
    for (var t : custom.values())
      if (t.path().getParent() != null) paths.add(t.path().getParent().toString());
    folders = List.copyOf(paths);
    settings.clear();
    parent.session().settings().forEach(s -> settings.put(s.id, s));
    groupSettings.clear();
    for (String id : AudioController.music(minecraft)) {
      Setting enabled = settings.get("audio.trackEnabled." + id);
      if (enabled != null) groupSettings.computeIfAbsent(group(id), key -> new ArrayList<>()).add(enabled);
    }
    revision = AudioController.libraryRevision();
    rebuild();
  }

  private void rebuild() {
    activeTracks = AudioController.activeMusic(minecraft);
    List<String> source =
        queue ? MusicRequests.tracks() : new ArrayList<>(AudioController.music(minecraft));
    Map<String, Integer> scores = new HashMap<>();
    if (!queue)
      for (String id : source)
        scores.put(id, FuzzySearch.score(search.text(), AudioController.musicLabel(id), path(id)));
    tracks =
        queue
            ? source
            : source.stream()
                .filter(this::inFolder)
                .filter(id -> scores.get(id) < FuzzySearch.NONE)
                .sorted(
                    Comparator.comparingInt((String id) -> scores.get(id))
                        .thenComparing(AudioController::musicLabel, String.CASE_INSENSITIVE_ORDER))
                .toList();
    rebuildRows();
    scroll(scroll());
  }

  private String group(String id) {
    var track = custom.get(id);
    if (track != null) return "folder:" + Objects.toString(track.path().getParent(), "Custom tracks");
    var builtin = BuiltinMusic.find(id);
    return builtin == null ? "Other tracks" : builtin.group();
  }

  private int groupOrder(String group) {
    if (group.startsWith("folder:")) return -1;
    int index = BuiltinMusic.GROUPS.indexOf(group);
    return index < 0 ? BuiltinMusic.GROUPS.size() : index;
  }

  private List<Setting> members(String group) {
    return groupSettings.getOrDefault(group, List.of());
  }

  private int includedCount(String group) {
    return (int) members(group).stream().filter(s -> included(s.id.substring("audio.trackEnabled.".length()))).count();
  }

  private void toggleGroup(String group) {
    List<Setting> members = members(group);
    if (members.isEmpty()) return;
    boolean include = includedCount(group) != members.size();
    if (presetsOpen && presets.editing()) {
      for (Setting setting : members) presets.draft().include(setting.id.substring("audio.trackEnabled.".length()), include);
      return;
    }
    for (Setting setting : members) setting.value = new com.google.gson.JsonPrimitive(include);
    // Operate on the whole group even when search hides some rows. One transaction
    // preserves Cancel/Apply behavior, volumes, queue order and all other groups.
    parent.changed(members, false);
  }

  private void rebuildRows() {
    List<Row> result = new ArrayList<>();
    int y = 0;
    if (queue) {
      for (int i = 0; i < tracks.size(); i++, y += rowHeight())
        result.add(new Row("", "", tracks.get(i), i, y, rowHeight()));
    } else {
      Map<String, List<String>> groups = new TreeMap<>(Comparator.comparingInt(this::groupOrder)
          .thenComparing(String.CASE_INSENSITIVE_ORDER).thenComparing(Comparator.naturalOrder()));
      for (String id : tracks) groups.computeIfAbsent(group(id), k -> new ArrayList<>()).add(id);
      if (presetsOpen && presets.editing() && previousDraft != presets.draft()) {
        collapsed.clear(); collapsed.addAll(groups.keySet()); previousDraft = presets.draft();
        trackScroll = 0;
      } else if (!presetsOpen && !groups.isEmpty()
          && (folder.isEmpty() || folder.equals("active") && groups.size() > 1) && initializedFilters.add(folder)) collapsed.addAll(groups.keySet());
      for (var entry : groups.entrySet()) {
        String key = entry.getKey();
        String label = key.startsWith("folder:") ? Objects.toString(Path.of(key.substring(7)).getFileName(), key.substring(7)) : key;
        result.add(new Row(key, label, null, -1, y, 24)); y += 24;
        if (collapsed.contains(key) && search.text().isBlank()) continue;
        for (String id : entry.getValue()) {
          result.add(new Row(key, label, id, -1, y, rowHeight())); y += rowHeight();
        }
      }
    }
    rows = List.copyOf(result);
    contentHeight = y;
  }

  private boolean included(String id) {
    if (presetsOpen && presets.editing()) return presets.draft().included(id);
    Setting s = settings.get("audio.trackEnabled." + id);
    return s != null && s.value.getAsBoolean();
  }
  private void toggleIncluded(String id) {
    if (presetsOpen && presets.editing()) { presets.draft().include(id, !included(id)); return; }
    Setting s = settings.get("audio.trackEnabled." + id);
    if (s != null) { s.cycle(1); parent.changed(s); }
  }

  private boolean inFolder(String id) {
    if (presetsOpen) return true;
    if (folder.equals("favourites")) return edits.favourites().contains(id);
    if (folder.equals("presets")) return edits.presets().stream().filter(p -> p.id().equals(selectedPreset))
        .anyMatch(p -> !p.excluded().contains(id));
    if (folder.equals("active")) return activeTracks.contains(id);
    if (folder.isEmpty()) return true;
    MusicLibrary.Track t = custom.get(id);
    if (folder.equals("custom")) return t != null;
    return !folder.equals("folder") && t != null
        && t.path().getParent() != null
        && t.path().startsWith(Path.of(folder));
  }

  private String path(String id) {
    MusicLibrary.Track t = custom.get(id);
    return t == null ? "Built-in soundtrack" : t.path().toString();
  }

  private boolean playable(String id) {
    MusicLibrary.Track t = custom.get(id);
    return t == null ? !id.startsWith("music:custom/") : t.playable();
  }

  private String folderLabel() {
    return folders.isEmpty() ? "No folders" : Objects.toString(Path.of(folder).getFileName(), folder);
  }

  private void cycleFolder(int direction) {
    if (folders.isEmpty()) return;
    folder = folders.get(Math.floorMod(folders.indexOf(folder) + direction, folders.size()));
    lastFolder = folder;
    scroll(0);
    rebuild();
  }

  public void tick() {
    if (conversionRevision != Mp3Converter.revision()) {
      conversionRevision = Mp3Converter.revision();
      conversionTicks = 200;
    } else if (conversionTicks > 0) conversionTicks--;
    if (revision != AudioController.libraryRevision()) refresh();
    else if (queue && !tracks.equals(MusicRequests.tracks())) rebuild();
    else if (!queue && folder.equals("active") && activeTracks != AudioController.activeMusic(minecraft)) rebuild();
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
    // Compare the list the user actually saw, not a newer tick's list. Duplicates are intentional.
    if (queue && !renderedQueue.equals(MusicRequests.tracks())) {
      rebuild();
      error = "Queue changed. Select the track again.";
      return false;
    }
    try {
      MusicRequests.edit(change);
      error = "";
      rebuild();
      return true;
    } catch (Exception e) {
      error = Objects.toString(e.getMessage(), "Could not save queue");
      return false;
    }
  }

  public void searchClick(int x, int y, int button) {
    if (queue || presetsOpen && !presets.editing()) return;
    presets.unfocus();
    search.focused = true;
    if (button == 1) clearQuery();
  }

  protected void keyPressed(char c, int key) {
    if (presetsOpen && presets.modal()) { presets.key(c,key); return; }
    if (presetsOpen && !search.focused) presets.key(c,key);
    if (!queue && search.focused) {
      search.key(c, key);
      scroll(0);
      rebuild();
    }
  }

  protected void mouseClicked(int x, int y, int b) {
    if (b != 0 && b != 1) return;
    if (presetsOpen && (presets.modal() || !presets.editing() || y < listTop())) {
      search.focused=false;
      presets.headerClick(x,y,b); rebuild(); return;
    }
    if (b == 0 && scrollbar.press(track(), x, y)) {
      scroll((int) scrollbar.drag(track(), y, true));
      return;
    }
    search.focused = false; presets.unfocus();
    if (queue && inside(x, y, filterX(), controlsTop(), filterWidth(), 18)) {
      if (!tracks.isEmpty()) { click(); queueEdit(q -> q.tracks.clear()); }
      return;
    }
    if (!queue && !presetsOpen) {
      if (inside(x,y,includeX(),bulkTop(),72,18)) { click(); edits.includeAll(); return; }
      if (inside(x,y,excludeX(),bulkTop(),72,18)) {
        click();
        try { edits.excludeAll(AudioController.music(minecraft)); error = ""; }
        catch (IllegalArgumentException e) { error = e.getMessage(); }
        return;
      }
      var tabs = filters();
      for (var tab : tabs.tabs()) if (inside(x, y, tab.x(), tab.y(), tab.width(), 18)) {
        click();
        if (tab.id().equals("cycle")) {
          var choices=List.of("active","","custom","folder","favourites","presets");
          String current=folderSelected()?"folder":folder;
          selectFilter(choices.get(Math.floorMod(choices.indexOf(current)+(b==0?1:-1),choices.size())));
        } else selectFilter(tab.id());
        return;
      }
      if ((folderSelected() || folder.equals("presets")) && inside(x, y, tabs.folderX(), tabs.folderY(), tabs.folderWidth(), 18)) {
        if (folder.equals("presets")) {
          var choices = edits.presets();
          if (!choices.isEmpty()) {
            int i = -1; for (int n=0;n<choices.size();n++) if (choices.get(n).id().equals(selectedPreset)) i=n;
            selectedPreset=choices.get(Math.floorMod(i+(b==0?1:-1),choices.size())).id(); rebuild();
          }
        } else if (!folders.isEmpty()) { click(); cycleFolder(b == 0 ? 1 : -1); }
        return;
      }
    }
    if (conversionVisible() && inside(x, y, right() - 88, statusTop(), 88, 14)) {
      click();
      if (Mp3Converter.busy()) Mp3Converter.cancel();
      else
        Mp3Converter.start(FabricLoader.getInstance().getGameDir(), AudioController.customTracks());
      return;
    }
    if (y < listTop() || y >= bottom() || x < left() || x >= right()) return;
    Row clicked = rows.stream().filter(row -> y - listTop() + scroll() >= row.y
        && y - listTop() + scroll() < row.y + row.height).findFirst().orElse(null);
    if (clicked == null) return;
    if (clicked.id == null) {
      int rowY = listTop() + clicked.y - scroll();
      if (inside(x, y, right() - 20, rowY + 3, 20, 18)) {
        click(); toggleGroup(clicked.group); return;
      }
      if (!collapsed.remove(clicked.group)) collapsed.add(clicked.group);
      rebuildRows(); scroll(scroll()); return;
    }
    int index = clicked.index;
    if (queue && !renderedQueue.equals(MusicRequests.tracks())) {
      rebuild();
      error = "Queue changed. Select the track again.";
      return;
    }
    String id = clicked.id;
    int rowY = listTop() + clicked.y - scroll();
    var layout = rowLayout(); int cy = rowY + layout.controlsY(); int bw = layout.buttonWidth();
    if (!queue && inside(x,y,left(),rowY+3,20,18)) { click(); toggleIncluded(id); return; }
    Setting volume = settings.get("audio.sound." + id);
    if (!presetsOpen && volume != null && inside(x,y,volumeX(),cy,layout.volumeWidth(),18) && b==0) {
      dragging=volume;slide(x);return;
    }
    if (!presetsOpen && volume != null && inside(x,y,layout.exactX(),cy,20,18)) {
      click();minecraft.setScreen(new ValueScreen(parent,volume));return;
    }
    if (!presetsOpen && inside(x,y,layout.favouriteX(),cy,bw,18)) { click();edits.favourite(id);rebuild();return; }
    if (queue) {
      if (inside(x,y,layout.queueX(),cy,bw,18)) { click();queueEdit(q -> q.tracks.remove(index)); }
      else if (inside(x,y,layout.playX(),cy,bw,18) && playable(id)) AudioController.toggleTrack(id);
      else if (inside(x,y,layout.upX(),cy,bw,18) && index>0) { click();queueEdit(q -> q.move(index,-1)); }
      else if (inside(x,y,layout.downX(),cy,bw,18) && index+1<tracks.size()) { click();queueEdit(q -> q.move(index,1)); }
    } else {
      if (inside(x,y,layout.previewX(),cy,bw,18) && playable(id)) AudioController.previewSound(id);
      else if (inside(x,y,layout.playX(),cy,bw,18) && playable(id)) AudioController.toggleTrack(id);
      else if (!presetsOpen && inside(x,y,layout.queueX(),cy,bw,18)) { click();queueEdit(q -> q.add(id)); }
    }
  }

  public void renderSearch(int x, int y, int w, int mx, int my) {
    if (presetsOpen && !presets.editing()) text("Music presets",x+2,y+5,0xdddddd);
    else if (queue) {
      text("Music queue", x + 2, y + 5, 0xdddddd);
    } else input(search, x, y, w, mx, my, "Search tracks or folders...");
  }

  public void render(int mx, int my, float delta) {
    scroll((int) scrollbar.drag(track(), my, Mouse.isButtonDown(0)));
    if (dragging != null) {
      if (Mouse.isButtonDown(0)) slide(mx);
      else {
        dragging = null;
        parent.finishContinuousChange();
      }
    }
    String tip = "";
    if (presetsOpen) {
      presets.renderHeader(mx,my);
      if (!presets.editing()) { hoverHelp = ""; return; }
    }
    if (queue) {
      text("Up next", left() + 2, controlsTop() + 5, 0xdddddd);
      button("Clear queue", filterX(), controlsTop(), filterWidth(), 18, mx, my, !tracks.isEmpty());
      if (inside(mx, my, filterX(), controlsTop(), filterWidth(), 18))
        tip = "Remove queued requests. Music files and rotation stay unchanged.";
    } else if (!presetsOpen) {
      button("Include all",includeX(),bulkTop(),72,18,mx,my,true);
      button("Exclude all",excludeX(),bulkTop(),72,18,mx,my,true);
      if (inside(mx,my,includeX(),bulkTop(),148,18))
        tip = "Change every built-in and custom track, including tracks outside this filter. Volumes and queue stay unchanged.";
      var tabs = filters();
      for (var tab : tabs.tabs()) {
        boolean selected = tab.id().equals("cycle") || (tab.id().equals("folder") ? folderSelected() : tab.id().equals(folder));
        fill(tab.x(), tab.y(), tab.x() + tab.width(), tab.y() + 18, selected ? 0xb0000000 : 0x40000000);
        text(tab.label(), tab.x() + 4, tab.y() + 5, selected ? 0xffffff : 0xaaaaaa);
        if (selected) fill(tab.x(), tab.y() + 17, tab.x() + tab.width(), tab.y() + 18, 0xffaaaaaa);
        if (inside(mx, my, tab.x(), tab.y(), tab.width(), 18))
          tip = switch (tab.id()) {
            case "active" -> "The pool allowed by your soundtrack, folders and world/menu rules. Excluded tracks remain visible.";
            case "" -> "All available built-in and custom music.";
            case "custom" -> "All tracks found in your music folders.";
            case "favourites" -> "Tracks you have starred. Favourites do not change automatic rotation.";
            case "presets" -> "Browse a saved preset without loading or changing it.";
            case "cycle" -> "Choose Active, Everything, All folders, Folders, Favourites or Presets.";
            default -> "Tracks in the selected folder and its subfolders.";
          };
      }
      if (folderSelected() || folder.equals("presets")) {
        boolean presetFilter = folder.equals("presets");
        button(fit(presetFilter ? edits.name(selectedPreset) : folderLabel(), tabs.folderWidth() - 8), tabs.folderX(), tabs.folderY(), tabs.folderWidth(), 18, mx, my, presetFilter ? !edits.presets().isEmpty() : !folders.isEmpty());
        if (inside(mx, my, tabs.folderX(), tabs.folderY(), tabs.folderWidth(), 18))
          tip = presetFilter ? "Browse another saved preset without loading it." : folders.isEmpty() ? "Add music folders in Audio settings." : folder;
      }
    }
    if (queue) renderedQueue = List.copyOf(tracks);
    clip(left() - 2, listTop(), span() + 4, Math.max(0, bottom() - listTop()));
    for (Row row : rows) {
      int y = listTop() + row.y - scroll();
      if (y + row.height <= listTop() || y >= bottom()) continue;
      if (row.id == null) {
        boolean closed = collapsed.contains(row.group) && search.text().isBlank();
        int total = members(row.group).size(), included = includedCount(row.group);
        String count = included + "/" + total;
        int countWidth = minecraft.textRenderer.getWidth(count);
        text((closed ? "> " : "v ") + fit(row.label, span() - countWidth - 44), left() + 2, y + 8,
            inside(mx,my,right()-20,y+3,20,18) && my>=listTop() && my<bottom() ? 0xffff55 : 0xffffff);
        text(count, right() - 26 - countWidth, y + 8, included > 0 && included < total ? 0xffdd88 : 0xaaaaaa);
        musicToggle(included > 0, right() - 20, y + 3, mx, my, total > 0);
        fill(left(), y + 21, right(), y + 22, 0x50555555);
        if (inside(mx, my, left(), y, span(), row.height) && my >= listTop() && my < bottom())
          tip = inside(mx, my, right() - 20, y + 3, 20, 18)
              ? (included == total ? "Exclude" : "Include") + " every track in this group. Includes hidden search results; volumes and queue stay unchanged."
              : (row.group.startsWith("folder:") ? row.group.substring(7) : row.label + " soundtrack")
                  + "\n" + included + " of " + total + " tracks included in automatic rotation.";
        continue;
      }
      String id=row.id; int i=row.index;
      var layout=rowLayout(); int cy=y+layout.controlsY(),bw=layout.buttonWidth();
      boolean hover=inside(mx,my,left(),y,span(),row.height)&&my>=listTop()&&my<bottom();
      if(hover)fill(left()-2,y,right()+2,y+row.height-1,0x60000000);
      text(fit((queue?(i+1)+". ":"")+AudioController.musicLabel(id),layout.nameWidth()),layout.nameX(),y+8,playable(id)?0xdddddd:0xffbb77);
      if(hover)tip=path(id)+(playable(id)?"":"\nConvert MP3, or reload if the file has moved.");
      if(!queue)musicToggle(included(id),left(),y+3,mx,my,true);
      Setting volume=settings.get("audio.sound."+id);
      if(!presetsOpen){
        if(volume!=null)slider(volume.display()+"%",volumeX(),cy,layout.volumeWidth(),mx,my,volume.value.getAsDouble()/100);
        button("...",layout.exactX(),cy,20,18,mx,my,volume!=null);
        iconButton("star",layout.favouriteX(),cy,bw,mx,my,true,edits.favourites().contains(id));
      }
      if(queue){
        iconButton("up",layout.upX(),cy,bw,mx,my,i>0);
        iconButton("down",layout.downX(),cy,bw,mx,my,i+1<tracks.size());
        iconButton("remove",layout.queueX(),cy,bw,mx,my,true);
      }else{
        iconButton("speaker",layout.previewX(),cy,bw,mx,my,playable(id),AudioController.previewing(id));
        if(!presetsOpen)button("+",layout.queueX(),cy,bw,18,mx,my,true);
      }
      iconButton(AudioController.playingTrack(id)?"pause":"play",layout.playX(),cy,bw,mx,my,playable(id));
      if(hover){
        if(!queue&&inside(mx,my,left(),y+3,20,18))tip="Include in automatic rotation. Preview and queue work even when excluded.";
        else if(!presetsOpen&&inside(mx,my,volumeX(),cy,layout.volumeWidth(),18))tip="Track volume; music and master volumes also apply.";
        else if(!presetsOpen&&inside(mx,my,layout.exactX(),cy,20,18))tip="Enter an exact volume";
        else if(!presetsOpen&&inside(mx,my,layout.favouriteX(),cy,bw,18))tip=edits.favourites().contains(id)?"Remove from favourites":"Add to favourites";
        else if(inside(mx,my,layout.playX(),cy,bw,18))tip="Play or pause this track";
        else if(!queue&&inside(mx,my,layout.previewX(),cy,bw,18))tip="Preview without changing the current track; click again to stop.";
        else if(!presetsOpen&&inside(mx,my,layout.queueX(),cy,bw,18))tip=queue?"Remove this request":"Add to the end of the queue";
        else if(queue&&inside(mx,my,layout.upX(),cy,bw,18))tip="Move up";
        else if(queue&&inside(mx,my,layout.downX(),cy,bw,18))tip="Move down";
      }
    }

    if (tracks.isEmpty())
      text(queue ? "Queue is empty" : "No matching tracks.", left() + 4, listTop() + 12, 0xaaaaaa);
    unclip();
    scrollbar.render(this, track());
    if (conversionVisible()) {
      button(Mp3Converter.busy() ? "Cancel MP3" : "Convert MP3", right() - 88, statusTop(), 88, 14, mx, my, true);
      if (inside(mx, my, right() - 88, statusTop(), 88, 14))
        tip = "Convert MP3 with FFmpeg to a WAV cache. Original files stay untouched.";
    }
    String status =
        !error.isEmpty()
            ? error
            : Mp3Converter.busy() || conversionTicks > 0
                ? Mp3Converter.status()
                : AudioController.status();
    if (!trackStatusVisible()) text(fit(status, span() - (conversionVisible() ? 92 : 0)), left(), statusTop() + 4, error.isEmpty() ? 0xaaaaaa : 0xff8888);
    hoverHelp = tip;
  }
}
