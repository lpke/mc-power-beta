package local.luke.power.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.*;
import local.luke.power.PowerBeta;
import local.luke.power.config.*;
import local.luke.power.input.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.*;
import org.lwjgl.opengl.GL11;

/** BTA-style independent page/content scrolling, collapsible groups and shared typed controls. */
public final class PowerOptionsScreen extends UiScreen {
  public static final List<String> PAGES =
      List.of(
          "General",
          "Video",
          "Audio",
          "Controls",
          "Interface",
          "Map",
          "Camera",
          "Movement",
          "Inventory",
          "Building",
          "Creative",
          "World editing",
          "Game rules",
          "Gameplay",
          "Crafting",
          "Fixes",
          "Advanced", "Defaults");
  private List<String> pages = PAGES;
  private final DefaultsPanel defaultsPanel = new DefaultsPanel(this);
  private local.luke.power.rules.CreationRules creationRules;
  private String savedWorldName;
  private boolean cheatPreset, ruleReviewChecked;
  private DefaultBases.Base reviewedRulesBase;
  public List<String> visiblePages() { return pages; }

  private static final int[] ICONS = {
    58, 20, 2256, 280, 323, 358, 345, 301, 54, 45, 2, 271, 339, 260, 61, 265, 331, 340
  };

  private record Row(String group, Setting setting, int y, int height, boolean separator) {
    Row(String group, Setting setting, int y, int height) { this(group, setting, y, height, false); }
    boolean heading() { return setting == null && !separator; }
  }

  private record View(String page, double scroll, double sideScroll, String query,
      boolean changedOnly, List<String> conflicts, String highlighted, String mapView, Set<String> collapsed,
      boolean showDisabled, int selected, boolean libraryOpen, MusicLibraryScreen.State library, boolean musicEditorReturn, boolean allSounds) {}
  private WorldMapScreen mapHost;
  private PowerOptionsScreen mapOwner;
  private Map<String, JsonElement> mapEntry;
  private int panelWidth;
  private String mapView = "Map";
  private boolean mapAppearanceDestination;
  private Runnable embeddedClose;
  static PowerOptionsScreen mapPanel(WorldMapScreen host) {
    PowerOptionsScreen panel = new PowerOptionsScreen(host);
    panel.mapHost = host; panel.mapView = "Map"; panel.page = "Map";
    panel.scroll = panel.sideScroll = 0; panel.search.setText(""); panel.history.clear();
    panel.highlightedId = ""; panel.conflictIds = List.of(); panel.changedOnly = panel.libraryOpen = false;
    return panel;
  }
  static PowerOptionsScreen mapPanel(WorldMapScreen host, PowerOptionsScreen owner) {
    PowerOptionsScreen panel = mapPanel(host);
    panel.mapOwner = owner; panel.session = owner.session;
    panel.refreshMapEntry();
    return panel;
  }
  void panelEntry(boolean minimap, String setting) {
    pushView();
    if (minimap) {
      mapView = "Minimap"; collapsed.clear(); highlightedId = setting;
      focusAnchorId = setting; focusAnchorScroll = scroll; focusAnchorSideScroll = sideScroll;
    }
  }
  static PowerOptionsScreen minimapPanel(WorldMapScreen host, PowerOptionsScreen owner) {
    PowerOptionsScreen panel = mapPanel(host, owner);
    panel.pushView(); panel.mapView = "Minimap"; panel.collapsed.clear();
    return panel;
  }
  private void refreshMapEntry() {
    if (mapOwner != null) mapEntry = new SettingsDraft(session.settings(), MapSettingsScope::contains).snapshot();
  }
  private void cancelBorrowedMap() {
    if (!MenuPreferences.current().autoApply) {
      for (Setting s : session.settings()) if (mapEntry.containsKey(s.id)) s.value = mapEntry.get(s.id).deepCopy();
      try { session.preview(); } catch (Exception e) { error = e.getMessage(); return; }
    }
    closeOptions();
  }
  boolean embedded() { return mapHost != null; }
  Screen returnScreen() { return embedded() ? mapHost : this; }
  private boolean inMapScope(Setting s) { return !embedded() || MapSettingsScope.contains(s); }
  void panelSize(int span) { panelWidth = span; if (minecraft != null && textRenderer != null) layout(); }
  void zoomMinimap(int wheel) {
    Setting setting = session.settings().stream().filter(s -> s.id.equals("maps.zoom")).findFirst().orElseThrow();
    double next = local.luke.power.map.MinimapZoom.scroll(setting.value.getAsDouble(), wheel);
    if (next == setting.value.getAsDouble()) return;
    if (MenuPreferences.current().autoApply) {
      // Wheel bursts use the same immutable save queue as the gameplay minimap.
      // A normal Options save also backs up files and must not run on each notch.
      var preview = local.luke.power.map.MapConfig.copy();
      preview.zoom = next;
      local.luke.power.map.MapConfig.queued(preview);
      setting.value = new JsonPrimitive(next);
      setting.accept();
      error = "";
      refreshMapEntry();
    } else {
      setting.value = new JsonPrimitive(next);
      changed(setting, true);
    }
  }
  void resetMinimapZoom() {
    Setting zoom = session.settings().stream().filter(s -> s.id.equals("maps.zoom")).findFirst().orElseThrow();
    Setting steps = session.settings().stream().filter(s -> s.id.equals("maps.zoomSteps")).findFirst().orElseThrow();
    Setting reset = session.settings().stream().filter(s -> s.id.equals("maps.zoomDefault")).findFirst().orElseThrow();
    double[] levels = Catalog.JSON.fromJson(steps.value, double[].class);
    zoom.value = new JsonPrimitive(local.luke.power.map.MinimapZoomSteps.reset(levels, reset.value.getAsDouble()));
    changed(zoom);
  }
  void toggleMapSetting(String id) {
    Setting setting = session.settings().stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow();
    setting.value = new JsonPrimitive(!setting.value.getAsBoolean()); changed(setting);
  }
  void toggleMapQuickSetting(local.luke.power.map.MapToggle toggle) {
    var changes = toggle.changes(local.luke.power.map.MapConfig.current());
    var edited = session.settings().stream().filter(s -> changes.containsKey(s.id.startsWith("maps.") ? s.id.substring(5) : "")).toList();
    for (Setting setting : edited) setting.value = Catalog.JSON.toJsonTree(changes.get(setting.id.substring(5)));
    changed(edited, false);
  }
  boolean panelModal() { return capture != null || confirmClose || confirmReset || choices.active(); }
  boolean minimapWheelAllowed() { return !panelModal() && !valueEditor.active() && dragging == null; }
  boolean radarIconsAllowed() { return !panelModal() && !valueEditor.active() && !search.focused; }
  void panelCancelGesture() {
    capture = null; captureModifier = 0; dragging = null; contentBar.release(); sidebarBar.release();
    valueEditor.cancel(); choices.close(); hoverDelay.clear(); TextDrag.cancel();
    finishContinuousChange();
  }
  void panelWheel(int wheel) {
    if (choices.wheel(wheel)) return;
    if (wheel == 0 || panelModal()) return;
    valueEditor.cancel(); scrollViewport(wheel, false);
  }
  private void scrollViewport(int wheel, boolean sidebar) {
    if (sidebar) sideScroll = Math.max(0, Math.min(sidebarTrack().maximum(), sideScroll - Math.signum(wheel) * 44));
    else scroll = Math.max(0, Math.min(contentTrack().maximum(), scroll - Math.signum(wheel) * 48));
    // A viewport move releases a related-setting reveal without rebuilding the setting rows.
    if (Double.compare(scroll, focusAnchorScroll) != 0 || Double.compare(sideScroll, focusAnchorSideScroll) != 0)
      focusAnchorId = "";
    hoverDelay.clear();
  }
  void panelRelease() {
    dragging = null; contentBar.release(); sidebarBar.release();
    finishContinuousChange();
  }
  void requestPanelClose(Runnable close) {
    embeddedClose = close;
    panelRelease(); valueEditor.cancel();
    if (mapOwner != null) closeOptions();
    else if (session != null && session.changes() > 0) confirmClose = true;
    else closeOptions();
  }
  void minimapAppearance(String id) {
    pushView(); mapView = "Minimap"; page = "Map"; search.setText(""); changedOnly = false;
    conflictIds = List.of(); highlightedId = id; scroll = 0; collapsed.clear();
    focusAnchorId = id; focusAnchorScroll = scroll; focusAnchorSideScroll = sideScroll; layout();
  }
  private boolean localMapView(Setting s) {
    return switch (mapView) {
      case "Minimap" -> HudEditorSettings.mapAppearance(s.id);
      case "Controls" -> s.kind == Setting.Kind.KEY;
      default -> s.page.equals("Map") && !HudEditorSettings.mapAppearance(s.id);
    };
  }
  private final Deque<View> history = new ArrayDeque<>();
  private String highlightedId = "";
  private String focusAnchorId = "";
  private double focusAnchorScroll, focusAnchorSideScroll;
  private record EditorReturn(Screen screen, View view, List<View> history) {}
  private EditorReturn editorReturn;

  private static View rememberedView;
  private static List<View> rememberedHistory = List.of();
  private boolean hidden, autoSavePending, libraryOpen, musicEditorReturn, allSounds;
  private MusicLibraryScreen library;
  private final MusicEdits musicEdits = new MusicEdits(this);
  final InlineValueEditor valueEditor = new InlineValueEditor();
  private final MusicStatusBar musicStatus = new MusicStatusBar();
  private MusicLibraryScreen.State libraryState;
  private final ScrollBar contentBar = new ScrollBar(), sidebarBar = new ScrollBar();
  int contentLeft() { return left(); }
  int contentRight() { return right() - 14; }
  int contentBottom() { return bottom(); }
  int audioContentTop() {
    return musicLayout().bottom() + 3;
  }
  int musicEditorTop() {
    return library == null ? audioContentTop() + 18 + MusicFilters.CONTENT_GAP : library.panelTop();
  }
  private boolean musicStatusVisible() {
    return !libraryVisible() || library.showMusicStatus() && library.trackStatusVisible();
  }
  private boolean libraryVisible() { return libraryOpen && audio(); }
  private boolean musicControlsVisible() { return audio(); }
  private boolean settingRowsVisible() { return !libraryVisible(); }
  void refreshMusicEditor() {
    if (session == null || minecraft == null) return;
    scroll = 0;
    selected = -1;
    valueEditor.cancel();
    layout();
  }
  private void openMusicEditor() {
    if (libraryVisible()) return;
    pushView();
    musicEditorReturn = true;
    page = "Audio";
    allSounds = false;
    search.setText(""); search.focused = false;
    changedOnly = false; conflictIds = List.of(); highlightedId = ""; focusAnchorId = "";
    scroll = 0;
    libraryOpen = true;
    library.showTracks();
    library.clearHistory();
    layout();
  }
  void editMusic() {
    openMusicEditor(); library.openPresets(); layout();
  }
  void openPlaylist(String id) {
    openMusicEditor(); library.showPlaylist(id); layout();
  }
  void changeWorldPlaylist(Runnable action) {
    if (libraryVisible() && library.activePlaylistTab()) library.guard(action);
    else action.run();
  }
  private int musicStatusWidth() { return contentRight() - left() - (libraryVisible() && library.conversionVisible() ? 92 : 0); }
  private int contentHeight() { return rows.isEmpty() ? 0 : rows.get(rows.size() - 1).y + rows.get(rows.size() - 1).height + 6; }
  private ScrollBar.Track contentTrack() { return new ScrollBar.Track(right() - 8, top(), bottom() - top(), contentHeight(), scroll); }
  private ScrollBar.Track sidebarTrack() { return new ScrollBar.Track(origin() + sidebar() + 3, 24, bottom() - 24, pages.size() * 22, sideScroll); }

  private final boolean directPause;
  private static String rememberedPage = "General";
  private static final Map<String, Double> positions = new HashMap<>();
  private static final Set<String> rememberedCollapsed = new HashSet<>(SettingsLayout.initiallyCollapsed());
  private static double rememberedSideScroll;
  private static boolean showDisabled;
  private List<String> conflictIds = List.of();
  private final Screen parent;
  private Screen exitTarget;
  private ConfigSession session;
  private final TextInput search = new TextInput("", 128);
  private final Set<String> collapsed = new HashSet<>();
  private final ItemRenderer icons = new ItemRenderer();
  private String page = "General", error = "";
  private double scroll, sideScroll;
  private Setting capture;
  private int captureModifier;
  private boolean confirmClose, confirmReset, savedRestart, changedOnly;
  private Setting dragging;
  private double dragLeft, dragSpan;

  private static int sidebarWidth(int width) { return Math.max(90, Math.min(155, width / 4)); }
  private int uiWidth() {
    if (embedded()) return panelWidth;
    int available = Math.min(width, height * 16 / 9);
    return isolatedRules() ? Math.min(668, Math.max(200, (available - sidebarWidth(available) - 2) * 88 / 100)) : available;
  }
  private int origin() { return embedded() ? 0 : (width - uiWidth()) / 2; }
  private int right() { return origin() + uiWidth(); }
  private boolean pauseActions() { return directPause && editorReturn == null; }
  private int footerY() {
    if (isolatedFooter()) return height - 28;
    int actions = controls() || video() ? disabledX() : actionX(0);
    return height - (resetX() + 92 <= actions ? 28 : 51);
  }
  private int resetX() { return origin() + 8 + (pauseActions() ? 50 : 0); }
  private int changesX() { return resetX() + 94; }
  private int changesEnd() { return footerY() < height - 28 ? right() - 8 : (controls() || video() ? disabledX() : actionX(0)) - 4; }
  private OptionsFooterLayout footerLayout() { return OptionsFooterLayout.at(uiWidth(), right(), textRenderer.getWidth("Auto apply")); }
  private int actionWidth(int i) { return footerLayout().width(i); }
  private int actionX(int i) { return footerLayout().x(i); }

  private int selected = -1, musicRevision = -1;
  private final HoverDelay hoverDelay = new HoverDelay();
  private final SettingsDropdown choices = new SettingsDropdown();
  private List<Row> rows = List.of();
  private List<String> sectionKeys = List.of();

  public PowerOptionsScreen(Screen parent) { this(parent, false); }

  public static void openMap(net.minecraft.client.Minecraft mc, Screen parent) {
    PowerOptionsScreen screen = new PowerOptionsScreen(parent);
    screen.page = "Map"; screen.search.setText(""); screen.highlightedId = "";
    screen.conflictIds = List.of(); screen.changedOnly = false; screen.scroll = 0;
    screen.history.clear(); screen.libraryOpen = false;
    mc.setScreen(screen);
  }

  public static void openDebug(net.minecraft.client.Minecraft mc) {
    var screen = new PowerOptionsScreen(null);
    screen.page = "Controls"; screen.search.setText("Debug"); screen.history.clear();
    screen.highlightedId = ""; screen.conflictIds = List.of(); screen.changedOnly = false;
    screen.collapsed.remove("Controls/Debug"); screen.scroll = 0;
    mc.setScreen(screen);
  }

  public PowerOptionsScreen(Screen parent, boolean directPause) {
    this.parent = parent;
    this.exitTarget = parent;
    this.directPause = directPause;
    page = rememberedPage;
    scroll = positions.getOrDefault(page, 0d);
    sideScroll = rememberedSideScroll;
    collapsed.addAll(rememberedCollapsed);
    if (rememberedView != null) restore(rememberedView);
    history.addAll(rememberedHistory);
  }

  public static void openCreationRules(net.minecraft.client.Minecraft mc, Screen parent, local.luke.power.rules.CreationRules draft) {
    PowerOptionsScreen screen = new PowerOptionsScreen(parent);
    screen.creationRules = draft;
    screen.session = local.luke.power.config.backend.CreationRulesBackend.open(draft);
    screen.page = "Game rules"; screen.search.setText(""); screen.changedOnly = false;
    screen.conflictIds = List.of(); screen.highlightedId = ""; screen.history.clear();
    screen.collapsed.clear(); screen.scroll = 0; screen.sideScroll = 0; screen.libraryOpen = false;
    mc.setScreen(screen);
  }

  public static void openSavedWorldRules(net.minecraft.client.Minecraft mc, Screen parent, String folder) throws Exception {
    var world = new local.luke.power.world.SavedWorldRules(FabricLoader.getInstance().getGameDir(), folder, () -> mc.world == null);
    PowerOptionsScreen screen = new PowerOptionsScreen(parent);
    screen.savedWorldName = world.name();
    screen.session = local.luke.power.config.backend.SavedWorldBackend.open(world);
    screen.page = "Game rules"; screen.search.setText(""); screen.changedOnly = false;
    screen.conflictIds = List.of(); screen.highlightedId = ""; screen.history.clear();
    screen.collapsed.clear(); screen.scroll = 0; screen.sideScroll = 0; screen.libraryOpen = false;
    mc.setScreen(screen);
  }

  private boolean isolatedRules() { return creationRules != null || savedWorldName != null; }
  private boolean autoApply() { return !isolatedRules() && MenuPreferences.current().autoApply; }
  private boolean playlistEditor() { return libraryVisible() && library.playlistEditor(); }
  private boolean isolatedFooter() { return isolatedRules() || playlistEditor(); }

  private record DraftFooter(String label, int x, int width, boolean enabled, Runnable action) {}
  private List<DraftFooter> draftFooter() {
    int cancelWidth = 76, end = right() - 8;
    if (playlistEditor() && library.readOnly())
      return List.of(new DraftFooter("Back", end - cancelWidth, cancelWidth, true, () -> { library.back(this::closeLibrary); layout(); }));
    String label = isolatedRules() ? "Save rules" : "Save playlist";
    int saveWidth = Math.min(Math.max(76, textWidth(label) + 16), Math.max(36, (uiWidth() - 24) / 2));
    Runnable cancel = isolatedRules() ? () -> {
      if (session.changes() > 0) confirmClose = true; else closeOptions();
    } : () -> { library.cancelPlaylist(); layout(); };
    Runnable save = isolatedRules() ? () -> save(true, false) : () -> { library.savePlaylist(); layout(); };
    boolean dirty = isolatedRules() ? session.changes() > 0 : library.playlistEditable() && library.playlistDirty();
    if (playlistEditor() && !dirty)
      return List.of(new DraftFooter(label, end - saveWidth, saveWidth, false, save));
    return List.of(new DraftFooter("Cancel", end - saveWidth - 4 - cancelWidth, cancelWidth, true, cancel),
        new DraftFooter(label, end - saveWidth, saveWidth, dirty, save));
  }

  private boolean selectedPreset(Setting setting) {
    return !setting.id.startsWith("rules.survival.") && !setting.id.startsWith("rules.cheats.")
        || setting.id.startsWith(cheatPreset ? "rules.cheats." : "rules.survival.");
  }
  private boolean rulesToolbar() {
    return page.equals("Game rules") && session != null
        && (creationRules != null || session.settings().stream().anyMatch(s -> s.backend.equals("rulePresets")));
  }
  private int ruleButtonWidth() { return (span() - 4) / 2; }
  private void chooseRulePreset(boolean cheat) {
    if (creationRules == null) { cheatPreset = cheat; scroll = 0; layout(); return; }
    var template = creationRules.template(cheat);
    List<Setting> entries = session.settings().stream().filter(s -> s.id.startsWith("rules.creation.")).toList();
    for (Setting setting : entries) setting.value = template.get(local.luke.power.rules.GameRule.find(setting.id.substring("rules.creation.".length())));
    changed(entries, false); layout();
  }

  /** Open music from another screen without inheriting a stale settings filter. */
  public static void openMusicDestination(net.minecraft.client.Minecraft mc, Screen parent) {
    boolean queue = !local.luke.power.audio.MusicRequests.tracks().isEmpty();
    openMusic(mc, parent, queue, null);
    if (!queue && mc.currentScreen instanceof PowerOptionsScreen screen) screen.audioTop();
  }

  private void audioTop() {
    libraryOpen = false;
    allSounds = false;
    scroll = 0;
    positions.put("Audio", 0d);
    collapsed.remove("Audio/Volume");
    collapsed.remove("Audio/Music playback");
    layout();
  }

  private static boolean individualSound(Setting setting) {
    return setting.group.equals("Game sounds / All") || setting.id.startsWith("audio.sound.") && !setting.id.startsWith("audio.sound.music:");
  }
  private void openAllSounds() {
    pushView();
    page = "Audio"; allSounds = true; libraryOpen = false;
    search.setText(""); search.focused = false; changedOnly = false; conflictIds = List.of();
    highlightedId = focusAnchorId = ""; scroll = 0;
    collapsed.remove("Audio/All");
    layout();
  }

  public static void openMusic(net.minecraft.client.Minecraft mc, Screen parent, boolean queue, String track) {
    PowerOptionsScreen screen = new PowerOptionsScreen(parent);
    screen.page = "Audio";
    screen.search.setText(""); screen.changedOnly = false;
    screen.conflictIds = List.of(); screen.highlightedId = ""; screen.history.clear();
    screen.libraryState = null; screen.libraryOpen = true; screen.musicEditorReturn = false;
    screen.allSounds = false;
    mc.setScreen(screen);
    if (screen.library == null) return;
    if (queue) screen.library.showQueue();
    else if (track != null && !track.isEmpty()) screen.library.showActiveTrack(track);
    else screen.library.showTracks();
    screen.layout();
  }

  public void init() {
    choices.close(); valueEditor.cancel();
    Keyboard.enableRepeatEvents(true);
    if (session == null)
      try {
        session = embedded() ? SettingsRegistry.openMap(minecraft) : SettingsRegistry.open(minecraft);
      } catch (Exception e) {
        PowerBeta.LOG.error("Could not open settings", e);
        error = "Settings could not be loaded. See the game log.";
      }
    if (session != null && !isolatedRules() && !embedded()) {
      if (library == null) { library = new MusicLibraryScreen(this); library.restore(libraryState); }
      library.init(minecraft, width, height);
    }
    layout();
  }

  public void removed() {
    choices.close(); valueEditor.cancel();
    rememberPosition();
    musicStatus.cancel();
    contentBar.release(); sidebarBar.release();
    if (library != null) library.removed();
    Keyboard.enableRepeatEvents(false);
  }

  private View view() {
    return new View(page, scroll, sideScroll, search.text(), changedOnly, conflictIds,
        highlightedId, mapView, Set.copyOf(collapsed), showDisabled, selected, libraryOpen, library == null ? libraryState : library.state(), musicEditorReturn, allSounds);
  }

  private void rememberPosition() {
    if (isolatedRules() || embedded()) return;
    rememberedView = view();
    rememberedHistory = List.copyOf(history);
    if (!changedOnly && conflictIds.isEmpty() && search.text().isBlank()) {
      rememberedPage = page;
      positions.put(page, scroll);
      rememberedSideScroll = sideScroll;
      rememberedCollapsed.clear();
      rememberedCollapsed.addAll(collapsed);
    }
  }

  void closeAudioPanel() { closeLibrary(); }
  private void closeLibrary() {
    if (library != null && library.dirtyPreset()) { library.guard(this::closeLibrary); return; }
    libraryOpen = false;
    if (musicEditorReturn && !history.isEmpty()) restore(history.pop());
    history.removeIf(View::libraryOpen);
    musicEditorReturn = false;
    if (!page.equals("Audio") || allSounds || libraryOpen) {
      page = "Audio"; allSounds = false; libraryOpen = false;
      search.setText(""); search.focused = false;
      changedOnly = false; conflictIds = List.of(); highlightedId = ""; focusAnchorId = "";
      scroll = positions.getOrDefault("Audio", 0d);
    }
    libraryState = null;
    if (library != null) library.close();
    layout();
  }

  private boolean controls() { return !embedded() && page.equals("Controls"); }
  private int disabledX() { return actionX(0) - 94; }
  private String previewId(Setting s) {
    if (s.id.equals("power_controls:features.moreSoundsChests") && s.value.getAsInt() == 2) return "random.door_open";
    if (s.id.startsWith("audio.sound.")) return s.id.substring("audio.sound.".length());
    return switch (s.id) {
      case "power_environment:config.MUSIC_CONFIG.volumeNetherPortalAmbient" -> "portal.portal";
      case "power_environment:config.MUSIC_CONFIG.volumeRainAmbient" -> "ambient.weather.rain";
      case "power_environment:config.MUSIC_CONFIG.volumeGhastAmbient" -> "mob.ghast.moan";
      case "power_environment:config.MUSIC_CONFIG.volumeCaveAmbient" -> "ambient.cave.cave";
      default -> local.luke.power.audio.ExtraSounds.previewId(s.id);
    };
  }
  private boolean soundPreview(Setting s) { return previewId(s) != null; }
  private boolean hasLink(Setting setting) {
    return setting.kind == Setting.Kind.KEY ? ControlLinks.related(session, setting) != null
        : !ControlLinks.controls(session, setting).isEmpty();
  }
  private int mainControlLeft(Row row) {
    if (VolumeControls.accepts(row.setting)) {
      int end = controlEnd(row.setting);
      return end - VolumeControls.width(end - left() - 4)
          + (page.equals("Audio") && !allSounds && search.text().isBlank() && !filtered()
              && row.setting.id.startsWith("audio.category.") ? VolumeControls.CHILD_INSET : 0);
    }
    return controlLeft(row) + (audioVolumeAction(row.setting) ? 0 : customise(row.setting) ? 66 : soundPreview(row.setting) || hasLink(row.setting) ? 22 : 0);
  }
  private boolean volumeAuxiliary(Setting s) { return VolumeControls.accepts(s) && (soundPreview(s) || hasLink(s)); }
  private int volumeAuxiliaryX(Row row) { return row.height == 40 ? right() - 34 : mainControlLeft(row) - 24; }
  private int volumeAuxiliaryY(Row row, int y) { return y + (row.height == 40 ? 0 : SettingRows.buttonOffset(span())); }
  private int volumeAuxiliaryHeight(Row row) { return row.height == 40 ? 14 : 18; }
  private boolean audioVolumeAction(Setting s) { return s.id.equals("native.music") || s.id.equals("native.sound"); }
  private int audioActionX(Row row) { return row.height == 40 ? right() - 58 : mainControlLeft(row) - 48; }
  private int audioActionY(Row row, int y) { return y + (row.height == 40 ? 0 : SettingRows.buttonOffset(span())); }
  private int audioActionHeight(Row row) { return row.height == 40 ? 14 : 18; }
  private boolean auxiliaryControl(Setting s) {
    return s.id.equals("native.music") || s.id.equals("native.sound") || customise(s) || soundPreview(s) || hasLink(s);
  }
  private boolean customise(Setting s) { return s.id.equals("logo.style") && s.value.getAsInt() == 0; }
  private boolean canClear() { return libraryVisible() ? library.hasQuery() : !search.text().isBlank() || filtered(); }
  private boolean searchScopeVisible() {
    return !embedded() && !isolatedRules() && editorReturn == null && !libraryVisible()
        && !allSounds && !filtered() && !page.equals("Defaults") && !controls();
  }
  private int sectionButtonX() { return right() - 34; }
  private int searchScopeX() { return sectionButtonX() - 24; }
  private int clearSearchX() { return (searchScopeVisible() ? searchScopeX() : sectionButtonX()) - 24; }
  private int searchWidth() { return (canClear() ? clearSearchX() : searchScopeVisible() ? searchScopeX() : sectionButtonX()) - 4 - searchLeft(); }
  private boolean mapAppearanceAvailable() {
    boolean enabled = session.settings().stream().filter(s -> s.id.equals("maps.worldMap"))
        .anyMatch(s -> s.value.getAsBoolean());
    return !embedded() && editorReturn == null && WorldMapScreen.canOpenSettings(minecraft, this, enabled);
  }
  private boolean openMapAppearance(String id) {
    return mapAppearanceAvailable() && WorldMapScreen.openSettings(minecraft, this, true, true, id);
  }
  private String searchPage(Setting s) {
    return OptionsSearchScope.page(s, embedded() || mapAppearanceDestination);
  }
  private String searchGroup(Setting s, int hudMode) {
    if (allSounds) return "All";
    if (controls()) return s.group;
    return OptionsSearchScope.group(s, hudMode, embedded() || mapAppearanceDestination);
  }
  private String sectionKey(String group) {
    return !search.text().isBlank() || changedOnly ? "filtered/" + group.replace(" / ", "/") : page + "/" + group;
  }
  private List<String> sections() {
    return sectionKeys;
  }
  private boolean hasSections() { return libraryVisible() ? library.hasSections() : !sections().isEmpty(); }
  private void toggleSections() {
    if (libraryVisible()) { library.toggleSections(); return; }
    SectionCollapse.toggleAll(sections(), collapsed);
    selected = -1;
    layout();
  }
  public String navigationDestination() {
    return libraryVisible() ? library.navigationDestination()
        : destination(page, mapView, allSounds, !conflictIds.isEmpty(), changedOnly);
  }
  private String destination(View view) {
    if (view.libraryOpen && view.page.equals("Audio")) return MusicLibraryScreen.destination(view.library);
    return destination(view.page, view.mapView, view.allSounds, !view.conflicts.isEmpty(), view.changedOnly);
  }
  private String destination(String page, String mapView, boolean allSounds, boolean conflicts, boolean changedOnly) {
    if (allSounds) return "All game sounds";
    if (conflicts) return "Conflicting bindings";
    if (changedOnly) return "Changed settings";
    if (embedded()) return mapView.equals("Minimap") ? "Minimap appearance"
        : mapView.equals("Controls") ? "Map controls" : "Map settings";
    if (savedWorldName != null) return "Saved world game rules";
    if (creationRules != null) return "New world game rules";
    return page + " settings";
  }
  private String editorDestination() {
    return editorReturn.screen instanceof DraftEditorScreen editor ? editor.title() : "HUD editor";
  }
  private String exitDestination() {
    if (exitTarget instanceof PowerOptionsScreen options) return options.navigationDestination();
    if (exitTarget instanceof WorldMapScreen) return "World map";
    if (exitTarget instanceof net.minecraft.class_180) return "Create world";
    if (exitTarget instanceof net.modificationstation.stationapi.api.client.gui.screen.EditWorldScreen) return "Edit world";
    if (exitTarget instanceof net.minecraft.client.gui.screen.TitleScreen || minecraft.world == null) return "Title screen";
    return exitTarget == null ? "game" : "Game menu";
  }
  private String backDestination() {
    if (libraryVisible()) return "Audio settings";
    if (mapOwner != null && history.size() == 1) return mapOwner.navigationDestination();
    if (editorReturn != null && history.size() <= 1) return editorDestination();
    return history.isEmpty() ? exitDestination() : destination(history.peek());
  }

  private int searchLeft() { return left() + (history.isEmpty() && !libraryVisible() ? 0 : 48); }
  private void pushView() { pushView(view()); }
  private void pushView(View previous) {
    if (history.size() == 32) history.removeLast();
    history.push(previous);
    search.focused = false;
    selected = -1;
  }
  private void back() {
    if (mapOwner != null && history.size() == 1) { closeOptions(); return; }
    if (editorReturn != null && history.size() <= 1) { returnToEditor(); return; }
    if (history.isEmpty()) return;
    restore(history.pop());
    layout();
  }
  private void restore(View view) {
    focusAnchorId = "";
    page = view.page; scroll = view.scroll; sideScroll = view.sideScroll;
    search.setText(view.query); search.selectAll(); search.focused = false;
    changedOnly = view.changedOnly; conflictIds = view.conflicts; highlightedId = view.highlighted; mapView = view.mapView;
    collapsed.clear(); collapsed.addAll(view.collapsed);
    showDisabled = view.showDisabled; selected = view.selected;
    libraryOpen = view.libraryOpen; libraryState = view.library; musicEditorReturn = view.musicEditorReturn;
    allSounds = view.allSounds;
    if (library != null) library.restore(libraryState);
  }
  /** Visit Controls without publishing or replacing the isolated editor draft. */
  void openRelatedControl(Screen editor, Setting setting) {
    Setting target = ControlLinks.controls(session, setting).stream().findFirst().orElse(null);
    if (target == null) return;
    editorReturn = new EditorReturn(editor, view(), List.copyOf(history));
    history.clear();
    openSetting(target);
    minecraft.setScreen(returnScreen());
  }
  private void returnToEditor() {
    EditorReturn destination = editorReturn;
    if (destination == null) return;
    editorReturn = null;
    restore(destination.view);
    history.clear(); history.addAll(destination.history);
    layout();
    minecraft.setScreen(destination.screen);
  }
  private void closeOptions() {
    if (embedded()) {
      confirmClose = false;
      Runnable close = embeddedClose; embeddedClose = null;
      if (close != null) close.run(); else mapHost.closeSettings();
    } else if (editorReturn != null) returnToEditor();
    else minecraft.setScreen(exitTarget);
  }

  private void linkedControls(Setting setting) {
    ControlLinks.controls(session, setting).stream().findFirst().ifPresent(this::openSetting);
  }
  private void related(Setting key) {
    Setting target = ControlLinks.related(session, key);
    if (target != null) openSetting(target);
  }
  private HudEditorScreen openHudSetting(String id) {
    if (editorReturn != null && editorReturn.screen instanceof HudEditorScreen editor) {
      if (!id.equals(HudEditorSettings.ENTRY)) editor.focusSetting(id);
      returnToEditor();
      return editor;
    }
    HudEditorScreen editor = new HudEditorScreen(this, id);
    minecraft.setScreen(editor);
    return editor;
  }
  private void openSetting(Setting target) {
    if (!inMapScope(target)) return;
    if (embedded() && HudEditorSettings.mapAppearance(target.id)) { minimapAppearance(target.id); return; }
    if (HudEditorSettings.mapAppearance(target.id) && openMapAppearance(target.id)) return;
    if (HudEditorSettings.contains(target) && !target.id.equals(HudEditorSettings.ENTRY)) {
      openHudSetting(target.id);
      return;
    }
    rememberPosition();
    pushView();
    page = embedded() ? "Map" : target.page;
    allSounds = individualSound(target);
    if (embedded()) mapView = target.kind == Setting.Kind.KEY ? "Controls" : "Map";
    libraryOpen = false;
    conflictIds = List.of(); changedOnly = false; search.setText("");
    collapsed.remove(page + "/" + (allSounds ? "All" : target.group));
    highlightedId = target.id;
    if (target.kind == Setting.Kind.KEY && !ControlLinks.enabled(session, target)) showDisabled = true;
    scroll = 0;
    focusAnchorId = target.id;
    focusAnchorScroll = scroll;
    focusAnchorSideScroll = sideScroll;
    layout();
  }

  private void openGroup(Setting setting) {
    if (individualSound(setting)) { openAllSounds(); return; }
    if (embedded()) { openSetting(setting); return; }
    if (HudEditorSettings.mapAppearance(setting.id) && openMapAppearance("")) return;
    if (HudEditorSettings.contains(setting) && !setting.id.equals(HudEditorSettings.ENTRY)) { openHudSetting(setting.id); return; }
    rememberPosition();
    pushView();
    page = setting.page;
    allSounds = false;
    libraryOpen = false;
    search.setText("");
    changedOnly = false;
    conflictIds = List.of();
    highlightedId = "";
    collapsed.remove(page + "/" + setting.group);
    scroll = 0;
    layout();
    rows.stream().filter(r -> r.group.equals(setting.group)).findFirst()
        .ifPresent(r -> scroll = r.y);
    layout();
  }

  private List<Setting> rowSettings(Setting setting) {
    String paired = setting.id.equals("inventoryTools.preferredItems") ? "inventoryTools.protectedSlots"
        : setting.id.equals("maps.zoomSteps") ? "maps.zoomDefault" : "";
    return paired.isEmpty() ? ColorSettings.linked(setting, session.settings())
        : session.settings().stream().filter(value -> value == setting || value.id.equals(paired)).toList();
  }
  private boolean settingChanged(Setting setting) {
    return rowSettings(setting).stream().anyMatch(Setting::changed);
  }

  private boolean filtered() {
    return changedOnly || !conflictIds.isEmpty();
  }

  private List<Setting> resetTargets() {
    List<String> listed = conflictIds;
    return session.settings().stream()
        .filter(this::inMapScope)
        .filter(s -> !allSounds || individualSound(s))
        .filter(s -> !s.group.equals("Music data") && !s.id.equals("audio.musicDirectories"))
        .filter(s -> changedOnly ? settingChanged(s) : listed.isEmpty() ? embedded() ? localMapView(s) : s.page.equals(page) : listed.contains(s.id))
        .filter(this::selectedPreset)
        .filter(s -> SettingAccess.visible(session, s) && SettingAccess.reason(session, s).isEmpty())
        .flatMap(s -> rowSettings(s).stream()).distinct()
        .toList();
  }

  void loadDefaults(DefaultBases.Base base) throws Exception {
    session.discard();
    boolean restart;
    try { restart = DefaultBases.load(base, minecraft, FabricLoader.getInstance().getGameDir()); }
    finally { session = SettingsRegistry.open(minecraft); layout(); }
    error = restart ? "Loaded. Restart the game for marked changes." : "Loaded " + base.label + ".";
    layout();
  }

  public ConfigSession session() {
    return session;
  }

  private int sidebar() {
    return isolatedRules() || embedded() ? 0 : sidebarWidth(uiWidth());
  }

  private int left() {
    return origin() + (isolatedRules() || embedded() ? 14 : sidebar() + 16);
  }

  private int span() {
    return right() - left() - 14;
  }

  private boolean audio() {
    return page.equals("Audio") && !allSounds && search.text().isBlank() && !changedOnly && conflictIds.isEmpty();
  }

  private boolean video() {
    return page.equals("Video") && search.text().isBlank() && !changedOnly;
  }

  private int top() {
    return audio() ? audioContentTop() + 20 : rulesToolbar() ? 78 : 52;
  }

  private int bottom() {
    return footerY() - 16;
  }

  private StickyHeader stickyHeader() {
    return StickyHeader.at(rows, Row::heading, row -> row.setting != null, Row::y, Row::height,
        (int) scroll, top(), bottom());
  }

  private void groupHeading(Row row, int y) {
    SettingsGroup.draw(this, allSounds && row.group.equals("All") ? "All game sounds" : row.group,
        collapsed.contains(sectionKey(row.group)), left(), right() - 14, y);
  }

  private int groupChevronWidth() { return Math.max(textRenderer.getWidth("> "), textRenderer.getWidth("v ")); }

  private void layout() {
    if (session == null) return;
    if (!page.equals("Audio")) allSounds = false;
    boolean revealTarget = !focusAnchorId.isEmpty() && focusAnchorId.equals(highlightedId)
        && search.text().isBlank() && !filtered()
        && Double.compare(scroll, focusAnchorScroll) == 0
        && Double.compare(sideScroll, focusAnchorSideScroll) == 0;
    if (!revealTarget) focusAnchorId = "";
    pages = embedded() ? List.of("Map") : PAGES.stream().filter(p -> SettingAccess.pageVisible(session, p)).filter(p -> p.equals("Defaults") && !isolatedRules() || session.settings().stream()
        .anyMatch(s -> s.page.equals(p) && SettingAccess.visible(session, s))).toList();
    if (!pages.contains(page) && !pages.isEmpty()) {
      positions.put(page, scroll);
      page = pages.get(0);
      scroll = positions.getOrDefault(page, 0d);
    }
    if (capture != null && !SettingAccess.visible(session, capture)) { capture = null; captureModifier = 0; }
    if (dragging != null && !SettingAccess.visible(session, dragging)) dragging = null;
    mapAppearanceDestination = mapAppearanceAvailable();
    String query = search.text().strip().toLowerCase(Locale.ROOT);
    int cluster = session.settings().stream().filter(s -> s.id.equals("infoHud.selectedCluster"))
        .mapToInt(s -> s.value.getAsInt()).findFirst().orElse(0);
    int hudMode = session.settings().stream().filter(s -> s.id.equals(HudEditorSettings.ENTRY))
        .mapToInt(s -> s.value.getAsInt()).findFirst().orElse(1);
    Map<String, List<Setting>> groups = new LinkedHashMap<>();
    List<String> listed = conflictIds;
    List<Setting> candidates = listed.isEmpty() ? SettingsLayout.ordered(session.settings()) : listed.stream()
        .map(id -> session.settings().stream().filter(s -> s.id.equals(id)).findFirst().orElse(null))
        .filter(Objects::nonNull).toList();
    candidates = candidates.stream().filter(this::inMapScope)
        .filter(s -> !allSounds || individualSound(s))
        .filter(s -> SettingAccess.pageVisible(session, s.page))
        .filter(s -> !s.id.equals("maps.worldMapEditor") || mapAppearanceDestination)
        .filter(s -> !controls() || s.kind == Setting.Kind.KEY)
        .filter(s -> query.isEmpty() || !controls() && (!searchScopeVisible() || !OptionsSearchScope.thisTab(page)) || searchPage(s).equals(page)).toList();
    if (embedded() && mapView.equals("Minimap") && query.isEmpty() && listed.isEmpty() && !changedOnly)
      candidates = HudEditorSections.order(candidates);
    Map<String, Integer> scores = new HashMap<>();
    if (!query.isEmpty()) candidates = candidates.stream().filter(s ->
        !s.group.equals("Individual music tracks") && !s.group.equals("Track rotation")
            && !s.id.startsWith("audio.sound.music:")).toList();
    if (!query.isEmpty() && listed.isEmpty()) {
      for (Setting s : candidates) scores.put(s.id, FuzzySearch.score(query, s.label, s.description + " " + s.group + " " + s.page
          + (ColorSettings.opacity(s, session.settings()) != null ? " color opacity transparency" : "")
          + (HudEditorSettings.contains(s) ? " HUD editor " + HudEditorSettings.tabLabel(HudEditorSettings.tab(s, hudMode)) : "")));
      candidates = candidates.stream().sorted(Comparator.comparingInt(s -> scores.get(s.id))).toList();
    }
    for (Setting s : candidates) {
      if (s.backend.equals("defaults") || HudEditorSettings.internal(s) || ColorSettings.hiddenOpacity(s, session.settings())) continue;
      if (query.isEmpty() && !changedOnly && listed.isEmpty()
          && (!embedded() && HudEditorSettings.contains(s) && !s.id.equals(HudEditorSettings.ENTRY)
              || HudEditorSettings.lettering(s))) continue;
      if (!SettingAccess.visible(session, s) || !selectedPreset(s)) continue;
      if (query.isEmpty() && !changedOnly && listed.isEmpty()
          && !local.luke.power.config.backend.InfoHudBackend.selected(s, cluster)) continue;
      if (s.group.equals("Music data")) continue;
      if (!allSounds && query.isEmpty() && !changedOnly && listed.isEmpty() && individualSound(s)) continue;
      if (query.isEmpty() && !changedOnly && listed.isEmpty() && (s.group.equals("Track rotation") || s.id.startsWith("audio.sound.music:"))) continue;
      if (listed.isEmpty() && s.kind == Setting.Kind.KEY && !showDisabled && !ControlLinks.enabled(session, s)) continue;
      if ((!changedOnly || settingChanged(s)) && (!listed.isEmpty() || (query.isEmpty()
          ? changedOnly || (embedded() ? localMapView(s) : s.page.equals(page))
          : scores.getOrDefault(s.id, FuzzySearch.NONE) < FuzzySearch.NONE)))
        groups.computeIfAbsent(query.isEmpty() && !changedOnly ? allSounds ? "All" : embedded() && mapView.equals("Minimap") ? HudEditorSections.group(s) : s.group
            : searchGroup(s, hudMode), k -> new ArrayList<>()).add(s);
    }
    List<Row> next = new ArrayList<>();
    int y = 0, rowHeight = SettingRows.height(span());
    for (var group : groups.entrySet()) {
      boolean flat = !listed.isEmpty() || group.getKey().isEmpty();
      if (!flat) { next.add(new Row(group.getKey(), null, y, SettingsGroup.HEIGHT)); y += SettingsGroup.HEIGHT; }
      if (!flat && collapsed.contains(sectionKey(group.getKey()))) continue;
      for (Setting s : group.getValue()) {
        next.add(new Row(group.getKey(), s, y, rowHeight));
        y += rowHeight;
        if (listed.isEmpty() && query.isEmpty() && !changedOnly
            && s != group.getValue().get(group.getValue().size() - 1)
            && Catalog.metadata(s.id).has("separatorAfter") && Catalog.metadata(s.id).get("separatorAfter").getAsBoolean()) {
          int spacing = (int)Catalog.number(Catalog.metadata(s.id), "separatorSpacing", 13);
          next.add(new Row(group.getKey(), null, y, spacing, true)); y += spacing;
        }
      }
      y += 6;
    }
    rows = next;
    sectionKeys = rows.stream().filter(Row::heading).map(r -> sectionKey(r.group)).toList();
    if (revealTarget) {
      boolean found = false;
      for (int i = 0; i < rows.size(); i++) {
        Row row = rows.get(i);
        if (row.setting == null || !row.setting.id.equals(focusAnchorId)) continue;
        selected = i;
        int viewport = Math.max(0, bottom() - top());
        scroll = SettingFocus.scroll(row.y, row.height, viewport, contentHeight(), SettingsGroup.HEIGHT);
        // Tiny layouts may fit a complete setting without also fitting its heading.
        scroll = Math.max(scroll, row.y + row.height - viewport);
        found = true;
        break;
      }
      if (found) {
        int tabY = pages.indexOf(page) * 22;
        if (tabY < sideScroll) sideScroll = tabY;
        else if (tabY + 22 > sideScroll + bottom() - 24) sideScroll = tabY + 22 - (bottom() - 24);
      } else focusAnchorId = "";
    }
    scroll = Math.max(0, Math.min(scroll, Math.max(0, y - (bottom() - top()))));
    sideScroll =
        Math.max(0, Math.min(sideScroll, Math.max(0, pages.size() * 22 - (bottom() - 24))));
    if (!focusAnchorId.isEmpty()) {
      focusAnchorScroll = scroll;
      focusAnchorSideScroll = sideScroll;
    }
  }

  void refreshRuleTemplates() {
    if (session.settings().stream().noneMatch(s -> s.backend.equals("rulePresets")) || RuleDefaults.review().required()) return;
    Map<String, JsonElement> drafts = new HashMap<>();
    session.settings().stream().filter(s -> s.backend.equals("rulePresets") && s.changed())
        .forEach(s -> drafts.put(s.id, s.value.deepCopy()));
    session.removeBackend("rulePresets"); local.luke.power.config.backend.RulePresetBackend.register(session);
    session.settings().forEach(s -> { if (drafts.containsKey(s.id)) s.value = drafts.get(s.id); });
    layout();
  }
  private boolean reviewRules() {
    ruleReviewChecked = true; reviewedRulesBase = DefaultBases.current();
    if (embedded() || isolatedRules() || minecraft.world != null) return false;
    return RuleDefaultsScreen.openIfNeeded(minecraft, this, () -> {
      session.removeBackend("rulePresets"); local.luke.power.config.backend.RulePresetBackend.register(session); layout();
    }, () -> { page = "General"; search.setText(""); layout(); minecraft.setScreen(this); }, "General settings");
  }
  public void tick() {
    if (!page.equals("Game rules")) ruleReviewChecked = false;
    if (page.equals("Game rules") && minecraft.currentScreen == this
        && (!ruleReviewChecked || reviewedRulesBase != DefaultBases.current()) && reviewRules()) return;
    choices.tick(); hoverDelay.tick();
    defaultsPanel.tick(page.equals("Defaults"));
    if (libraryVisible()) library.tick();
    int revision = local.luke.power.audio.AudioController.libraryRevision();
    if (session != null && !isolatedRules() && !embedded() && revision != musicRevision) {
      musicRevision = revision;
      if (local.luke.power.config.backend.AudioBackend.discoverMusic(session, minecraft)) layout();
    }
  }

  net.minecraft.client.Minecraft client() { return minecraft; }

  public void changed(Setting setting) { changed(setting, false); }

  void musicActions(List<String> labels, List<Boolean> enabled, int x, int y, java.util.function.IntConsumer action) {
    musicActions(null, labels, enabled, x, y, action);
  }
  void musicActions(Object owner, List<String> labels, List<Boolean> enabled, int x, int y, java.util.function.IntConsumer action) {
    valueEditor.cancel();
    choices.openActions(owner, this, labels, enabled, x, y, width, height, action);
  }
  boolean musicActionsOpen(Object owner) { return choices.editing(owner); }

  boolean acceptEditor(SettingsDraft draft) {
    return acceptEditor(draft, false);
  }
  boolean acceptEditor(SettingsDraft draft, boolean continuous) {
    Map<Setting, com.google.gson.JsonElement> before = new LinkedHashMap<>();
    for (Setting copy : draft.settings()) {
      Setting target = session.settings().stream().filter(s -> s.id.equals(copy.id)).findFirst().orElseThrow();
      before.put(target, target.value.deepCopy());
      target.value = copy.value.deepCopy();
    }
    try { session.validate(); }
    catch (Exception e) {
      before.forEach((s, v) -> s.value = v);
      error = Objects.toString(e.getMessage(), "Invalid editor settings");
      return false;
    }
    changed(before.keySet(), continuous);
    if (error.isEmpty()) return true;
    before.forEach((s, v) -> s.value = v);
    try { session.preview(autoApply()); }
    catch (Exception e) { PowerBeta.LOG.error("Could not restore editor preview", e); }
    return false;
  }

  String editorError() { return error; }

  public void finishContinuousChange() {
    if (mapOwner != null) { mapOwner.finishContinuousChange(); if (MenuPreferences.current().autoApply && session.changes() == 0) refreshMapEntry(); }
    else if (autoSavePending) save(false, false);
  }

  public void changed(Setting setting, boolean continuous) {
    changed(List.of(setting), continuous);
  }

  /** Preview/save a group edit once, after all draft values have been updated. */
  public void changed(Collection<Setting> settings, boolean continuous) {
    if (mapOwner != null) {
      mapOwner.changed(settings, continuous || dragging != null); error = mapOwner.error;
      if (MenuPreferences.current().autoApply && session.changes() == 0) refreshMapEntry();
      return;
    }
    settings.forEach(session::link);
    error = "";
    try { session.preview(autoApply()); }
    catch (Exception e) {
      error = "Could not preview this value. " + Objects.toString(e.getMessage(), "See the game log.");
      PowerBeta.LOG.error("Settings preview failed", e);
    }
    if (autoApply() && error.isEmpty()) {
      if (dragging == null && !continuous) save(false, false);
      else autoSavePending = true;
    }
    if (changedOnly || settings.stream().anyMatch(s -> s.id.startsWith("rules.world.") || s.id.equals("infoHud.selectedCluster"))) layout();
  }

  private int controlLeft(Row row) {
    return row.height == 40 ? left() + (row.setting.kind == Setting.Kind.KEY ? 16 : 4) : Math.max(left() + 100, right() - 188);
  }

  private void drag(int pixelX) {
    if (dragging == null) return;
    var before = dragging.value.deepCopy();
    dragging.slide((pixelX - dragLeft) / dragSpan);
    if (!before.equals(dragging.value)) changed(dragging);
  }


  public void onMouseEvent() {
    super.onMouseEvent();
    int wheel = Mouse.getEventDWheel();
    if (choices.wheel(wheel)) return;
    if (hidden || wheel == 0 || capture != null || confirmClose || confirmReset) return;
    if (libraryVisible() && library.modal()) { library.wheel(wheel); return; }
    valueEditor.cancel();
    int x = Mouse.getEventX() * width / minecraft.displayWidth;
    if (!isolatedRules() && !embedded() && x < origin() + sidebar() + 8) scrollViewport(wheel, true);
    else if (page.equals("Defaults")) { defaultsPanel.wheel(wheel, left(), span()); return; }
    else if (libraryVisible()) library.wheel(wheel);
    else scrollViewport(wheel, false);
  }

  @Override
  protected void mouseReleased(int x, int y, int button) {
    choices.release(y, button);
    if (libraryVisible()) library.mouseReleased(x, y, button);
    super.mouseReleased(x, y, button);
  }

  protected void mouseClicked(int x, int y, int button) {
    if (choices.press(this, x, y, button)) return;
    if (page.equals("Defaults") && !confirmClose && !confirmReset && defaultsPanel.press(x, y, button, left(), span())) return;
    if (valueEditor.press(x, y, button)) return;
    if (hidden) {
      if (button == 0 && inside(x, y, disabledX(), height - 28, 90, 20)) { hidden = false; click(); }
      return;
    }
    if (session == null) {
      if (y > height - 30) minecraft.setScreen(parent);
      return;
    }
    if (capture != null) {
      if (button == 0 && inside(x, y, width / 2 - 102, height / 2 + 36, 100, 20)) {
        click(); capture = null; captureModifier = 0; return;
      }
      if (button == 0 && inside(x, y, width / 2 + 2, height / 2 + 36, 100, 20)) {
        click(); clearBinding(); return;
      }
      capture.value = new JsonPrimitive(new Chord(button - 100, Bindings.heldModifiers()).encoded());
      Setting bound = capture; capture = null; changed(bound);
      return;
    }
    if (confirmClose || confirmReset) {
      if (button == 0) dialogClick(x, y);
      return;
    }
    if (libraryVisible() && library.modal()) { library.dialogClick(x,y,button); return; }
    if (isolatedFooter() && y >= height - 30) {
      if (button == 0) for (DraftFooter item : draftFooter())
        if (inside(x, y, item.x, height - 28, item.width, 20)) {
          if (item.enabled) { click(); item.action.run(); } break;
        }
      return;
    }
    if (libraryVisible() && library.dirtyPreset()
        && (y >= bottom() || x < MusicRowLayout.hoverLeft(left()) && y >= 24 && y < bottom())) {
      library.guard(() -> mouseClicked(x,y,button)); return;
    }
    if (button != 0 && button != 1) return;
    if (button == 0 && rulesToolbar()) {
      for (int i = 0; i < 2; i++) if (inside(x, y, left() + i * (ruleButtonWidth() + 4), 53, ruleButtonWidth(), 20)) {
        click(); chooseRulePreset(i == 1); return;
      }
    }
    if (!isolatedRules() && !embedded() && button == 0 && sidebarBar.press(sidebarTrack(), x, y)) { sideScroll = sidebarBar.drag(sidebarTrack(), y, true); return; }
    if (settingRowsVisible() && button == 0 && contentBar.press(contentTrack(), x, y)) { scroll = contentBar.drag(contentTrack(), y, true); return; }
    if (button == 0 && (libraryVisible() || !history.isEmpty()) && inside(x, y, left(), 27, 44, 20)) {
      click(); if (libraryVisible()) closeLibrary(); else back(); return;
    }
    if (inside(x, y, searchLeft(), 27, searchWidth(), 20)) {
      if (libraryVisible()) { library.searchClick(x, y, button); return; }
      if (page.equals("Defaults")) return;
      search.focused = true;
      String previous = search.text();
      search.click(x, button);
      if (!previous.equals(search.text())) {
        scroll = 0;
        layout();
      }
      return;
    }
    if (button == 0 && searchScopeVisible() && inside(x, y, searchScopeX(), 27, 20, 20)) {
      click(); OptionsSearchScope.toggle(page); scroll = 0; selected = -1; layout(); return;
    }
    if (button == 0 && hasSections() && inside(x, y, sectionButtonX(), 27, 20, 20)) {
      click(); toggleSections(); return;
    }
    if (canClear() && inside(x, y, clearSearchX(), 27, 20, 20)) {
      click();
      if (libraryVisible()) { library.clearQuery(); return; }
      if (!history.isEmpty() && !conflictIds.isEmpty()) { back(); return; }
      conflictIds = List.of();
      highlightedId = "";
      search.setText("");
      search.selectAll();
      scroll = 0;
      layout();
      return;
    }
    search.focused = false;
    if (library != null) library.unfocus();
    if (!isolatedRules() && !embedded() && button == 0 && y >= 24 && y < bottom() && x >= origin() && x < origin() + sidebar()) {
      int index = (int) ((y - 24 + sideScroll) / 22);
      if (index >= 0 && index < pages.size()) {
        boolean same = pages.get(index).equals(page);
        boolean acceptLocation = same && !history.isEmpty() && !filtered() && search.text().isBlank();
        rememberPosition();
        click();
        history.clear();
        musicEditorReturn = false;
        highlightedId = "";
        focusAnchorId = "";
        page = pages.get(index);
        allSounds = false;
        conflictIds = List.of();
        changedOnly = false;
        search.setText("");
        search.selectAll();
        scroll = acceptLocation ? scroll : same ? 0 : positions.getOrDefault(page, 0d);
        if (page.equals("Audio")) closeLibrary();
        selected = -1;
        layout();
        if (editorReturn != null) editorReturn = new EditorReturn(editorReturn.screen, view(), List.of());
      }
      return;
    }
    if (page.equals("Defaults")) {
      if (button == 0 && inside(x, y, actionX(2), height - 28, actionWidth(2), 20)) save(true);
      return;
    }
    if (musicControlsVisible() && button == 1) {
      var item = musicButton(x, y);
      if (item != null && item.action() == AudioToolbar.Action.PLAYLIST) {
        click(); openPlaylist(musicEdits.selected()); return;
      }
    }
    if (musicControlsVisible() && button == 0 && y >= 52 && y < audioContentTop() - 3 && x >= left() && x < contentRight()) {
      var item = musicButton(x, y);
      if (item == null) return;
      if (!item.enabled() && !(item.action() == AudioToolbar.Action.QUEUE
          && libraryVisible() && library.queueVisible())) return;
      AudioToolbar.Action action = item.action();
      click();
      switch (action) {
        case QUIET -> local.luke.power.audio.AudioController.quiet();
        case PLAY -> local.luke.power.audio.AudioController.togglePause();
        case PREVIOUS -> local.luke.power.audio.AudioController.previous();
        case NEXT -> local.luke.power.audio.AudioController.next();
        case SHUFFLE -> {
          Setting shuffle = musicEdits.setting("shuffle");
          shuffle.value = new JsonPrimitive(!shuffle.value.getAsBoolean()); changed(shuffle);
        }
        case QUEUE -> {
          if (libraryVisible() && library.queueVisible()) closeLibrary();
          else { libraryOpen = true; library.showQueue(); layout(); } }
        case PLAYLIST -> chooseWorldPlaylist(MusicPlaylistPicker.TOOLBAR, item.x(), item.y(), item.width());
      }
      return;
    }
    if (musicControlsVisible() && musicStatusVisible() && musicStatus.press(x,y,button)) return;
    if (musicControlsVisible() && (button == 0 || button == 1) && musicStatusVisible()
        && (button == 1 ? musicStatus.overLabel(x, y) : musicStatus.overTrack(x, y))) {
      if (button == 1) {
        click();
        if (local.luke.power.audio.MusicRequests.tracks().isEmpty()) library.guard(this::audioTop);
        else { libraryOpen = true; library.showQueue(); layout(); }
        return;
      }
      String id = local.luke.power.audio.AudioController.navigationTrackId();
      if (!id.isEmpty()) {
        click();
        libraryOpen = true; library.showActiveTrack(id); layout();
      }
      return;
    }
    if (libraryVisible() && x >= MusicRowLayout.hoverLeft(left()) && y >= audioContentTop() && y < bottom()) {
      library.mouseClicked(x, y, button); return;
    }
    if (settingRowsVisible() && y >= top() && y < bottom() && x >= left() && x < right() - 10) {
      StickyHeader sticky = stickyHeader();
      int pinned = sticky != null && sticky.contains(y, top()) ? sticky.index() : -1;
      for (int i = 0; i < rows.size(); i++) {
        if (pinned >= 0 && i != pinned) continue;
        Row r = rows.get(i);
        int ry = pinned == i ? sticky.y() : top() + r.y - (int) scroll;
        if (y < ry || y >= ry + r.height) continue;
        if (r.separator) return;
        if (r.heading()) {
          if (button == 0) {
            String id = sectionKey(r.group);
            boolean jump = (!search.text().isBlank() || changedOnly)
                && x >= left() + groupChevronWidth() && !collapsed.contains(id);
            if (jump) {
              if (i + 1 < rows.size() && rows.get(i + 1).setting != null) {
                click(); openGroup(rows.get(i + 1).setting);
              }
            } else {
              SettingsGroup.toggle(collapsed, id);
              if (pinned >= 0) scroll = r.y;
              layout();
            }
          }
          return;
        }
        selected = i;
        highlightedId = "";
        Setting s = r.setting;
        if (!SettingAccess.reason(session, s).isEmpty()) return;
        if (audioVolumeAction(s) && inside(x, y, audioActionX(r), audioActionY(r, ry), 44, audioActionHeight(r))) {
          click();
          if (s.id.equals("native.music")) openMusicEditor(); else openAllSounds();
          return;
        }
        if (volumeAuxiliary(s) && inside(x, y, volumeAuxiliaryX(r), volumeAuxiliaryY(r, ry), 20, volumeAuxiliaryHeight(r))) {
          if (soundPreview(s)) local.luke.power.audio.AudioController.previewSound(previewId(s));
          else { click(); linkedControls(s); }
          return;
        }
        int controlY = ry + SettingRows.buttonOffset(span());
        if (y < controlY) return;
        if (!s.id.equals(HudEditorSettings.ENTRY) && x >= right() - 34) {
          click();
          rowSettings(s).forEach(Setting::reset);
          if (s.id.equals("interface.debugOrder")) session.settings().stream().filter(value -> value.id.equals("interface.debugRightOrder")).forEach(Setting::reset);
          changed(rowSettings(s), false);
          return;
        }
        if (s.kind == Setting.Kind.KEY && !conflicts(s).isEmpty() && x >= controlLeft(r) - 12 && x < controlLeft(r)) {
          click();
          pushView();
          List<String> fixed = new ArrayList<>(); fixed.add(s.id);
          conflicts(s).forEach(c -> fixed.add(c.id));
          conflictIds = List.copyOf(fixed);
          search.setText(""); changedOnly = false; scroll = 0; layout(); return;
        }
        if (!VolumeControls.accepts(s) && x >= controlLeft(r) && x < mainControlLeft(r)) {
          if (customise(s)) { click(); minecraft.setScreen(new TitleLetteringScreen(this)); }
          else if (soundPreview(s)) local.luke.power.audio.AudioController.previewSound(previewId(s));
          else if (hasLink(s)) { click(); if (s.kind == Setting.Kind.KEY) related(s); else linkedControls(s); }
          return;
        }
        if (x < mainControlLeft(r) || x >= controlEnd(s)) return;
        if (button == 1 && playlistSetting(s)) {
          click(); String id = s.value.getAsString();
          openPlaylist(local.luke.power.audio.MusicPlaylist.SAME_AS_WORLD.equals(id) ? musicEdits.selected() : id); return;
        }
        if (InheritedColors.accepts(s) && InheritedColorControl.at(mainControlLeft(r), controlEnd(s) - mainControlLeft(r), InheritedColors.custom(s)).colorAt(x)) {
          click(); openColor(s);
        } else if (InheritedColors.accepts(s) || s.id.equals(HudEditorSettings.ENTRY) || s.id.equals("maps.hudEditor") || s.id.equals("maps.worldMapEditor") || HudEditorSettings.lettering(s) || ItemEditors.accepts(s) || ColorSettings.accepts(s)) activate(s, button == 1 ? -1 : 1);
        else if (s.kind == Setting.Kind.INTEGER || s.kind == Setting.Kind.DECIMAL) {
          if (button == 0 && s.slider()) {
            dragging = s;
            double pixels = (double) minecraft.displayWidth / width;
            dragLeft = (mainControlLeft(r) + 4) * pixels;
            dragSpan = Math.max(1, right() - 38 - mainControlLeft(r) - 8) * pixels;
            drag((int) (x * pixels));
          } else {
            search.focused = false; dragging = null;
            valueEditor.begin(s, s, () -> changed(s), mainControlLeft(r), controlY,
                right() - 38 - mainControlLeft(r), top(), bottom());
          }
        } else activate(s, button == 1 ? -1 : 1);
        return;
      }
    }
    if (button == 1 && inside(x, y, actionX(1), height - 28, actionWidth(1), 20)) {
      click();
      try {
        MenuPreferences.update(v -> v.autoApply = !v.autoApply);
        if (MenuPreferences.current().autoApply) save(false, false);
      } catch (Exception e) { error = "Could not save auto-apply preference"; PowerBeta.LOG.error(error, e); }
      return;
    }
    if (button != 0) return;
    if (video() && minecraft.world != null && inside(x, y, disabledX(), height - 28, 90, 20)) { click(); hidden = true; return; }
    if (pauseActions() && inside(x, y, origin() + 8, footerY(), 46, 20)) {
      if (MenuPreferences.current().autoApply && session.changes() > 0) save(false, false);
      click(); exitTarget = new net.minecraft.class_525();
      if (session.changes() > 0) confirmClose = true; else minecraft.setScreen(exitTarget);
      return;
    }
    if (controls() && inside(x, y, disabledX(), height - 28, 90, 20)) {
      click(); showDisabled = !showDisabled; layout(); return;
    }
    if (inside(x, y, resetX(), footerY(), 88, 20)) {
      click();
      confirmReset = true;
      return;
    }
    int changesEnd = changesEnd();
    if ((session.changes() > 0 || changedOnly) && changesEnd - changesX() >= 18
        && inside(x, y, changesX(), footerY(), changesEnd - changesX(), 20)) {
      click();
      conflictIds = List.of();
      highlightedId = "";
      changedOnly = !changedOnly;
      search.setText("");
      scroll = 0;
      layout();
      return;
    }
    if (inside(x, y, actionX(0), height - 28, actionWidth(0), 20)) {
      click();
      if (mapOwner != null) { cancelBorrowedMap(); return; }
      if (editorReturn != null) { returnToEditor(); return; }
      if (session.changes() > 0) confirmClose = true;
      else closeOptions();
      return;
    }
    if (inside(x, y, actionX(1), height - 28, actionWidth(1), 20)) save(false);
    if (inside(x, y, actionX(2), height - 28, actionWidth(2), 20)) save(true);
  }

  void choose(List<String> labels, List<String> help, int selected, int x, int y, int span,
      java.util.function.IntConsumer accepted) {
    valueEditor.cancel(); search.focused = false;
    choices.open(labels, help, selected, -1, x, y, span, width, height, accepted);
  }

  private void activate(Setting s, int direction) {
    if (!SettingAccess.reason(session, s).isEmpty()) return;
    error = "";
    click();
    if (s.id.equals(local.luke.power.creativeui.CreativeTabsBackend.SETTING)) {
      minecraft.setScreen(new CreativeTabsScreen(this, s, () -> changed(s))); return;
    }
    if (s.id.equals("inventoryTools.hotbarEditor")) {
      if (!local.luke.power.inventory.tools.SavedCreativeHotbars.editable(minecraft)) {
        error = "Editing saved hotbars requires Creative access in a singleplayer world."; return;
      }
      minecraft.setScreen(new SavedHotbarsScreen(this)); return;
    }
    if (s.id.equals("inventoryTools.preferredItems") || s.id.equals("inventoryTools.protectedSlots")) {
      minecraft.setScreen(new SortingPreferencesScreen(this, session, () -> changed(
          session.settings().stream().filter(value -> value.id.equals("inventoryTools.preferredItems")
              || value.id.equals("inventoryTools.protectedSlots")).toList(), false)));
      return;
    }
    if (s.id.equals("inventoryTools.toolWhitelist")) {
      minecraft.setScreen(ToolWhitelistEditor.open(this, s, () -> changed(s))); return;
    }
    if (s.id.equals("rules.review")) { reviewRules(); return; }
    if (s.id.equals("maps.zoomSteps")) { minecraft.setScreen(new ZoomStepsScreen(this, s)); return; }
    if (s.id.equals("interface.debugOrder")) {
      openHudSetting(s.id).beginReorder(); return;
    }
    if (s.id.equals("maps.worldMapEditor")) {
      WorldMapScreen.openSettings(minecraft, this, true, false, ""); return;
    }
    if (s.id.equals(HudEditorSettings.ENTRY) || s.id.equals("maps.hudEditor")) {
      if (embedded()) { minimapAppearance(""); return; }
      if (s.id.equals("maps.hudEditor")) {
        Setting enabled = session.settings().stream().filter(v -> v.id.equals("maps.worldMap")).findFirst().orElseThrow();
        if (!WorldMapScreen.openMinimapSettings(minecraft, this, enabled.value.getAsBoolean())) openHudSetting("maps.size").showMinimap();
      }
      else openHudSetting(s.id);
      return;
    }
    if (HudEditorSettings.lettering(s)) {
      minecraft.setScreen(new TitleLetteringScreen(this));
      return;
    }
    if (s.id.equals("audio.menuPlaylist")) {
      Row row = rows.stream().filter(r -> r.setting == s).findFirst().orElseThrow();
      valueEditor.cancel(); search.focused = false;
      MusicPlaylistPicker.open(this, choices, s, musicEdits, s, true,
          mainControlLeft(row), top() + row.y - (int)scroll + SettingRows.buttonOffset(span()),
          controlEnd(s) - mainControlLeft(row), 18);
      return;
    }
    if (s.id.equals("audio.playlist")) {
      Row row = rows.stream().filter(r -> r.setting == s).findFirst().orElseThrow();
      chooseWorldPlaylist(s, mainControlLeft(row), top() + row.y - (int) scroll + SettingRows.buttonOffset(span()),
          controlEnd(s) - mainControlLeft(row));
      return;
    }
    if (s.id.equals("power_controls:userinterface.panoramaConfig.panoramaFolder")) {
      minecraft.setScreen(new PanoramaScreen(this, s));
      return;
    }
    if (s.id.equals("native.texturePack")) {
      minecraft.setScreen(new TexturePackScreen(this, s));
      return;
    }
    if (ItemEditors.accepts(s)) {
      minecraft.setScreen(ItemEditors.screen(this, s));
      return;
    }
    if (InheritedColors.accepts(s)) { InheritedColors.toggle(s, session.settings()); changed(s); return; }
    if (ColorSettings.accepts(s)) {
      openColor(s); return;
    }
    if (s.numeric() && !s.slider() || s.kind == Setting.Kind.TEXT) {
      Row row = rows.stream().filter(r -> r.setting == s).findFirst().orElseThrow();
      int y = top() + row.y - (int) scroll + SettingRows.buttonOffset(span());
      valueEditor.begin(s, s, () -> changed(s), mainControlLeft(row), y,
          right() - 38 - mainControlLeft(row), top(), bottom());
      return;
    }
    if (SettingsDropdown.accepts(s)) {
      Row row = rows.stream().filter(r -> r.setting == s).findFirst().orElseThrow();
      search.focused = false; valueEditor.cancel();
      choices.open(s, mainControlLeft(row), top() + row.y - (int)scroll + SettingRows.buttonOffset(span()),
          controlEnd(s) - mainControlLeft(row), embedded() ? right() : width, height, () -> changed(s));
      return;
    }
    switch (s.kind) {
      case KEY -> { capture = s; captureModifier = 0; }
      case TEXT, LIST -> {
        if (s.id.equals("video.fogCycle"))
          minecraft.setScreen(new IntegerArrayScreen(this, s,
              new IntegerArrayDraft.Rules(2, 32, 16, true), "Distance", "chunks"));
        else if (s.id.equals("audio.musicDirectories"))
          openMusicEditor();
        else minecraft.setScreen(new ValueScreen(s, returnScreen(), () -> changed(s)));
      }
      default -> {
        s.cycle(direction);
        changed(s);
      }
    }
  }

  private void openColor(Setting setting) {
    Setting swatches = session.settings().stream().filter(value -> value.id.equals("interface.colours")).findFirst().orElse(null);
    minecraft.setScreen(new ColourScreen(returnScreen(), setting, ColorSettings.opacity(setting, session.settings()),
        () -> changed(ColorSettings.linked(setting, session.settings()), false), swatches));
  }

  private void save(boolean close) { save(close, true); }

  private void save(boolean close, boolean sound) {
    if (isolatedRules() && session.changes() == 0) return;
    if (mapOwner != null) {
      if (close) { if (sound) click(); closeOptions(); }
      else { mapOwner.save(false, sound); error = mapOwner.error; if (session.changes() == 0) refreshMapEntry(); }
      return;
    }
    if (sound) click();
    autoSavePending = false;
    try {
      savedRestart |= session.restartRequired();
      session.save(FabricLoader.getInstance().getGameDir());
      if (savedRestart) {
        savedRestart = false;
        error = "Saved. Restart the game for marked changes.";
        confirmClose = false;
      } else if (close) closeOptions();
      layout();
    } catch (Exception e) {
      PowerBeta.LOG.error("Settings save failed; restoring previous values", e);
      Throwable cause = e;
      while (cause.getCause() != null) cause = cause.getCause();
      error = cause.getMessage() == null ? "Could not save settings" : cause.getMessage();
      confirmClose = false;
    }
  }

  private void dialogClick(int x, int y) {
    int cy = height / 2 + 12;
    if (inside(x, y, width / 2 - 102, cy, 100, 20)) {
      if (confirmReset) {
        List<Setting> targets = resetTargets();
        for (Setting s : targets) { s.reset(); session.link(s); }
        try { session.preview(autoApply()); } catch (Exception e) { error = e.getMessage(); }
        if (autoApply() && error.isEmpty()) save(false, false);
        layout();
        confirmReset = false;
      } else save(true);
    } else if (inside(x, y, width / 2 + 2, cy, 100, 20)) {
      if (confirmReset) confirmReset = false;
      else {
        try {
          if (mapOwner != null) { cancelBorrowedMap(); return; }
          session.discard();
          closeOptions();
        } catch (Exception e) { error = "Could not restore settings: " + e.getMessage(); confirmClose = false; }
      }
    } else if (inside(x, y, width / 2 - 50, cy + 24, 100, 20)) {
      confirmClose = false;
      confirmReset = false;
      embeddedClose = null;
      exitTarget = parent;
    }
  }

  public boolean capturingBinding() { return capture != null; }
  private void clearBinding() {
    Setting bound = capture; bound.value = new JsonPrimitive(0);
    capture = null; captureModifier = 0; changed(bound);
  }

  protected void keyPressed(char c, int key) {
    if (choices.key(this, key)) return;
    if (page.equals("Defaults") && defaultsPanel.key(key)) return;
    if (valueEditor.key(c, key)) return;
    if (hidden) { if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_F1) hidden = false; return; }
    if (capture != null) {
      // Unmapped platform events must not silently clear an existing binding.
      if (key == Keyboard.KEY_NONE) return;
      if (key == Keyboard.KEY_ESCAPE) {
        capture = null; captureModifier = 0;
        return;
      }
      if (key == Keyboard.KEY_DELETE) { clearBinding(); return; }
      if (Chord.modifier(key) != 0) {
        captureModifier = key;
        return;
      }
      int modifiers = Bindings.heldModifiers();
      capture.value = new JsonPrimitive(key == Keyboard.KEY_DELETE && modifiers == 0 ? 0 : new Chord(key, modifiers).encoded());
      Setting bound = capture; capture = null; changed(bound);
      return;
    }
    if (confirmClose || confirmReset) {
      if (key == Keyboard.KEY_ESCAPE) {
        confirmClose = confirmReset = false; embeddedClose = null; exitTarget = parent;
      }
      return;
    }
    if (libraryVisible() && library.modal()) { library.keyPressed(c,key); return; }
    if (key == Keyboard.KEY_ESCAPE) {
      if (libraryVisible()) { library.back(this::closeLibrary); return; }
      if (!history.isEmpty()) { back(); return; }
      if (editorReturn != null) { returnToEditor(); return; }
      if (!conflictIds.isEmpty()) { conflictIds = List.of(); scroll = positions.getOrDefault(page, 0d); layout(); return; }
      if (search.focused) {
        search.focused = false;
        return;
      }
      if (mapOwner != null) closeOptions();
      else if (session != null && session.changes() > 0) confirmClose = true;
      else closeOptions();
      return;
    }
    if ((Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
        && key == Keyboard.KEY_F) {
      if (libraryVisible()) { library.focus(); return; }
      if (page.equals("Defaults")) return;
      search.focused = true;
      search.selectAll();
      return;
    }
    if (libraryVisible()) { library.keyPressed(c, key); return; }
    if (search.focused) {
      String before = search.text();
      View searchOrigin = embedded() && before.isBlank() ? view() : null;
      search.key(c, key);
      if (!before.equals(search.text())) {
        if (searchOrigin != null && !search.text().isBlank()) { pushView(searchOrigin); search.focused = true; }
        conflictIds = List.of();
        highlightedId = "";
        scroll = 0;
        selected = -1;
        layout();
      }
      return;
    }
    if (key == Keyboard.KEY_DOWN || key == Keyboard.KEY_UP || key == Keyboard.KEY_TAB) {
      highlightedId = "";
      int dir = key == Keyboard.KEY_UP ? -1 : 1;
      for (int attempt = 0; attempt < Math.max(1, rows.size()); attempt++) {
        selected = Math.floorMod(selected + dir, Math.max(1, rows.size()));
        if (rows.isEmpty() || !rows.get(selected).separator) break;
      }
      if (!rows.isEmpty()) {
        Row r = rows.get(selected);
        scroll = Math.max(0, r.y - 20);
        layout();
      }
      return;
    }
    if (selected >= 0 && selected < rows.size()) {
      Setting s = rows.get(selected).setting;
      if (s != null
          && (key == Keyboard.KEY_LEFT || key == Keyboard.KEY_RIGHT || key == Keyboard.KEY_RETURN))
        activate(s, key == Keyboard.KEY_LEFT ? -1 : 1);
    }
  }

  private String value(Setting s) {
    if (InheritedColors.accepts(s)) return InheritedColors.label(s);
    if (ColorSettings.accepts(s)) return ColorSettings.label(s, session.settings());
    if (s.id.equals(local.luke.power.creativeui.CreativeTabsBackend.SETTING)) return "Edit...";
    if (s.id.equals("inventoryTools.preferredItems")) return "Edit...";
    if (s.id.equals("inventoryTools.toolWhitelist")) {
      var tools = s.value.getAsJsonArray();
      return tools.size() == 1 && tools.get(0).getAsString().equals("*") ? "All tools" : tools.size() + " tools...";
    }
    if (s.id.equals("interface.debugOrder")) return "Reorder";
    if (s.id.equals("rules.review")) return "Review...";
    if (s.id.equals("maps.zoomSteps")) return s.value.getAsJsonArray().size() + " levels...";
    if (s.id.equals(HudEditorSettings.ENTRY) || s.id.equals("maps.hudEditor") || s.id.equals("maps.worldMapEditor") || s.id.equals("logo.logo.lines")) return "Edit...";
    if (!SettingAccess.reason(session, s).isEmpty()) return SettingAccess.lockedValue(s);
    if (s.id.equals("power_controls:userinterface.panoramaConfig.panoramaFolder")) return local.luke.power.input.PanoramaCatalog.label(s.value.getAsString());
    if (s.id.equals("audio.playlist")) return musicEdits.currentName();
    if (s.id.equals("audio.menuPlaylist")) return musicEdits.name(s.value.getAsString());
    if (capture == s) return modifierLabel(Bindings.heldModifiers()) + "Press key...";
    if (ItemEditors.accepts(s)) return ItemEditors.display(s);
    if (s.kind != Setting.Kind.KEY) return s.display();
    Chord chord = Chord.decode(s.value.getAsInt());
    int code = chord.key();
    return modifierLabel(chord.modifiers()) + (code == 0
        ? "Unbound"
        : code < 0
            ? "Mouse " + (code + 101)
            : Objects.toString(Keyboard.getKeyName(code), "Key " + code));
  }
  private boolean playlistSetting(Setting setting) {
    return setting.id.equals("audio.playlist") || setting.id.equals("audio.menuPlaylist");
  }
  private String settingLabel(Setting setting) {
    String label = SettingLabels.display(setting, !search.text().isBlank());
    boolean local = search.text().isBlank() || allSounds || searchScopeVisible() && OptionsSearchScope.thisTab(page);
    return local && setting.id.startsWith("audio.category.") && label.startsWith("Game sounds / ")
        ? label.substring("Game sounds / ".length()) : label;
  }
  private int controlEnd(Setting s) { return right() - (s.id.equals(HudEditorSettings.ENTRY) ? 14 : 38); }

  private static String modifierLabel(int mask) {
    return ((mask & Chord.CTRL) != 0 ? "Ctrl+" : "")
        + ((mask & Chord.SHIFT) != 0 ? "Shift+" : "")
        + ((mask & Chord.ALT) != 0 ? "Alt+" : "");
  }

  private List<Setting> conflicts(Setting s) {
    if (s.kind != Setting.Kind.KEY || Chord.decode(s.value.getAsInt()).key() == 0) return List.of();
    return session.settings().stream().filter(v -> v != s && v.kind == Setting.Kind.KEY && SettingAccess.visible(session, v)
        && local.luke.power.debug.DebugConflicts.overlap(s.id, v.id)
        && Chord.decode(v.value.getAsInt()).key() == Chord.decode(s.value.getAsInt()).key()).toList();
  }

  private String conflictTip(Setting s, List<Setting> conflicts) {
    StringBuilder text = new StringBuilder();
    for (Setting v : conflicts) {
      if (text.length() > 0) text.append('\n');
      text.append(v.label).append(": ").append(value(v));
    }
    text.append("\n\nThe matching binding with more modifiers takes priority.");
    return text.toString();
  }

  private AudioToolbar.Layout musicLayout() {
    return AudioToolbar.layout(left(), span(), local.luke.power.audio.MusicRequests.tracks().size(),
        local.luke.power.audio.AudioController.musicPlaying(), libraryVisible());
  }
  private void chooseWorldPlaylist(Object owner, int x, int y, int width) {
    valueEditor.cancel(); search.focused = false;
    MusicPlaylistPicker.open(this, choices, owner, musicEdits, musicEdits.setting("playlist"), false, x, y, width, 18);
  }
  private Row musicVolumeRow() {
    return rows.stream().filter(row -> row.setting != null && row.setting.id.equals("native.music")).findFirst().orElseThrow();
  }
  private int musicEditX() { return audioActionX(musicVolumeRow()); }
  private int musicEditY() { return audioActionY(musicVolumeRow(), top() + musicVolumeRow().y - (int) scroll); }
  private AudioToolbar.Button playlistButton() {
    return musicButtons().stream().filter(button -> button.action() == AudioToolbar.Action.PLAYLIST).findFirst().orElseThrow();
  }
  private int musicPlaylistX() { return playlistButton().x(); }
  private int musicPlaylistY() { return playlistButton().y(); }
  private int musicPlaylistWidth() { return playlistButton().width(); }
  private List<AudioToolbar.Button> musicButtons() { return musicLayout().buttons(); }
  private AudioToolbar.Button musicButton(int x, int y) {
    for (var button : musicButtons()) if (button.contains(x, y)) return button;
    return null;
  }

  private void renderSidebar(int mx, int my) {
    if (isolatedRules() || embedded()) return;
    fill(origin() + sidebar() + 4, 24, origin() + sidebar() + 7, bottom(), 0x90404040);
    fill(origin() + sidebar() + 7, 24, origin() + sidebar() + 8, bottom(), 0x90000000);
    clip(origin(), 24, sidebar() + 2, bottom() - 24);
    for (int i = 0; i < pages.size(); i++) {
      int y = 24 + i * 22 - (int) sideScroll;
      boolean hover = inside(mx, my, origin(), y, sidebar(), 22);
      if (pages.get(i).equals(page) && (search.text().isBlank() || OptionsSearchScope.thisTab(page)) && !changedOnly)
        fill(origin(), y, origin() + sidebar(), y + 21, 0xc0000000);
      GuiItems.draw(icons, minecraft, new ItemStack(ICONS[PAGES.indexOf(pages.get(i))], 1, 0), origin() + 7, y + 2, false);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      text(
          fit(pages.get(i), sidebar() - 31),
          origin() + 28,
          y + 6,
          hover ? 0xffffa0 : pages.get(i).equals(page) ? 0xffffff : 0xaaaaaa);
    }
    unclip();
    sidebarBar.render(this, sidebarTrack());
  }

  public void render(int mx, int my, float delta) {
    int pointerX = mx, pointerY = my;
    boolean libraryModal = libraryVisible() && library.modal();
    if (choices.active() || libraryModal || confirmClose || confirmReset || capture != null
        || page.equals("Defaults") && defaultsPanel.modal()) { mx = -1000; my = -1000; }
    valueEditor.beginFrame();
    if (!hidden && !confirmClose && !confirmReset && capture == null) {
      if (!isolatedRules() && !embedded()) sideScroll = sidebarBar.drag(sidebarTrack(), my, Mouse.isButtonDown(0));
      if (settingRowsVisible()) scroll = contentBar.drag(contentTrack(), my, Mouse.isButtonDown(0));
    }
    // This wait belongs only to binding capture, never gameplay input.
    if (capture != null && captureModifier != 0 && !Bindings.physical(captureModifier)) {
      capture.value = new JsonPrimitive(captureModifier);
      Setting bound = capture; capture = null;
      captureModifier = 0; changed(bound);
    }
    if (dragging != null) {
      if (Mouse.isButtonDown(0)) drag(Mouse.getX());
      else { dragging = null; finishContinuousChange(); }
    }
    if (hidden) {
      button("Show options", disabledX(), height - 28, 90, 20, mx, my, true);
      return;
    }
    if (!embedded()) renderBackground();
    else fill(origin(), 0, right(), height, 0xc0141414);
    fill(embedded() ? origin() : 0, 0, embedded() ? right() : width, 22, 0x70000000);
    fill(embedded() ? origin() : 0, footerY() - 10, embedded() ? right() : width, height, 0x90000000);
    drawCenteredTextWithShadow(textRenderer, fit(savedWorldName != null ? "Game rules: " + savedWorldName
        : embedded() ? mapView.equals("Minimap") ? "Minimap appearance" : mapView.equals("Controls") ? "Map controls" : "Map settings"
        : creationRules != null ? "New world game rules" : rulesToolbar() ? "New world defaults" : libraryVisible() ? "Music" : allSounds ? "All game sounds" : "Options", uiWidth() - 24), origin() + uiWidth() / 2, 7, 0xffffff);
    if (session == null) {
      text(error, 10, 45, 0xff8888);
      button("Back", width / 2 - 50, height - 28, 100, 20, mx, my, true);
      backTooltip(exitDestination(), width / 2 - 50, height - 28, 100, 20, mx, my);
      return;
    }
    renderSidebar(mx, my);
    if (page.equals("Defaults")) {
      defaultsPanel.init(minecraft, width, height);
      defaultsPanel.draw(confirmClose ? -1000 : mx, confirmClose ? -1000 : my, left(), span());
      if (!error.isEmpty()) text(fit(error, span()), left(), height - 44, 0xffbb88);
      button("Done", actionX(2), height - 28, actionWidth(2), 20, mx, my, true);
      defaultsPanel.overlay(pointerX, pointerY);
      if (confirmClose) {
        fill(0, 0, width, height, 0xc0000000);
        WarningText.draw(this, 312, height / 2, "Save your changes?", "", 0xaaaaaa);
        button("Save", width / 2 - 102, height / 2 + 12, 100, 20, pointerX, pointerY, true);
        button("Discard", width / 2 + 2, height / 2 + 12, 100, 20, pointerX, pointerY, true);
        button("Keep editing", width / 2 - 50, height / 2 + 36, 100, 20, pointerX, pointerY, true);
      }
      return;
    }
    if (libraryVisible() || !history.isEmpty()) button("Back", left(), 27, 44, 20, mx, my, true);
    if (libraryVisible()) library.renderSearch(searchLeft(), 28, searchWidth(), mx, my);
    else input(search, searchLeft(), 28, searchWidth(), mx, my,
        !conflictIds.isEmpty() ? "Conflicting bindings"
        : allSounds ? "Search all game sounds..." : controls() ? "Search controls..." : changedOnly ? "Search changed settings..." : embedded() ? "Search maps and controls..."
        : searchScopeVisible() && OptionsSearchScope.thisTab(page) ? "Search settings in this tab..." : "Search all settings...");
    if (canClear()) iconButton("remove", clearSearchX(), 27, 20, 20, mx, my, true, false);
    if (searchScopeVisible()) button(OptionsSearchScope.thisTab(page) ? "T" : "A", searchScopeX(), 27, 20, 20, mx, my, true);
    boolean expandSections = libraryVisible() ? library.sectionsCollapsed() : SectionCollapse.allCollapsed(sections(), collapsed);
    iconButton(expandSections ? "expand" : "collapse", sectionButtonX(), 27, 20, 20, mx, my,
        hasSections(), false);
    if (rulesToolbar()) {
      if (creationRules == null) {
        ConnectedTabs.baseline(this, left(), left() + span(), 53, 20,
            left() + (cheatPreset ? ruleButtonWidth() + 4 : 0), ruleButtonWidth());
        ConnectedTabs.tab(this, "Survival defaults", left(), 53, ruleButtonWidth(), 20, mx, my, !cheatPreset);
        ConnectedTabs.tab(this, "Cheat defaults", left() + ruleButtonWidth() + 4, 53, ruleButtonWidth(), 20, mx, my, cheatPreset);
      } else {
        button(fit("Copy Survival defaults", ruleButtonWidth() - 8), left(), 53, ruleButtonWidth(), 20, mx, my, true);
        button(fit("Copy Cheat defaults", ruleButtonWidth() - 8), left() + ruleButtonWidth() + 4, 53, ruleButtonWidth(), 20, mx, my, true);
      }
    }
    if (musicControlsVisible()) {
      for (var item : musicButtons()) {
        if (item.action() == AudioToolbar.Action.PREVIOUS || item.action() == AudioToolbar.Action.NEXT)
          trackButton(item.label(), item.action() == AudioToolbar.Action.PREVIOUS,
              item.x(), item.y(), item.width(), mx, my);
        else if (item.action() == AudioToolbar.Action.PLAY)
          iconButton(local.luke.power.audio.AudioController.musicPlaying() ? "pause" : "play",
              item.x(), item.y(), item.width(), mx, my, true);
        else if (item.action() == AudioToolbar.Action.QUEUE)
          tabButton(fit(item.label(), item.width() - 4), item.x(), item.y(), item.width(), 18, mx, my, item.enabled(),
              libraryVisible() && library.queueVisible());
        else if (item.action() == AudioToolbar.Action.PLAYLIST)
          WaypointSetDropdown.button(this, musicEdits.currentName(), item.x(), item.y(), item.width(), mx, my, true,
              choices.editing(MusicPlaylistPicker.TOOLBAR));
        else if (item.action() == AudioToolbar.Action.SHUFFLE) {
          boolean shuffle = musicEdits.setting("shuffle").value.getAsBoolean();
          iconButton("shuffle", item.x(), item.y(), item.width(), mx, my, true);
          if (!shuffle) for (int d = 0; d < 10; d++)
            fill(item.x() + 5 + d, item.y() + 13 - d, item.x() + 7 + d, item.y() + 14 - d, 0xffff5555);
        }
        else button(fit(item.label(), item.width() - 4), item.x(), item.y(), item.width(), 18, mx, my, item.enabled());
      }
      if (musicStatusVisible()) musicStatus.render(this,left(),audioContentTop(),
          musicStatusWidth(),mx,my);
    }
    String tip = "", hoverId = "";
    if (settingRowsVisible()) {
    StickyHeader sticky = stickyHeader();
    int rowsTop = sticky == null ? top() : Math.max(top(), sticky.y() + sticky.height());
    clip(left() - 2, rowsTop, span() + 8, Math.max(0, bottom() - rowsTop));
    int listMouseY = sticky != null && sticky.contains(my, top()) ? -1 : my;
    for (int index = 0; index < rows.size(); index++) {
      Row row = rows.get(index);
      int y = top() + row.y - (int) scroll;
      if (y + row.height < top() || y >= bottom()) continue;
      if (row.separator) {
        fill(left(), y + row.height / 2, right() - 14, y + row.height / 2 + 1, 0x50555555);
        continue;
      }
      if (row.heading()) {
        if (sticky == null || index != sticky.index()) groupHeading(row, y);
        continue;
      }
      Setting s = row.setting;
      String lock = SettingAccess.reason(session, s);
      boolean editable = lock.isEmpty();
      boolean hover = inside(mx, listMouseY, left(), y, span(), row.height) && listMouseY >= top() && listMouseY < bottom();
      if (s.id.equals(highlightedId)) {
        fill(left() - 2, y, right() - 12, y + row.height, 0x403d4c18);
        fill(left() - 2, y, right() - 12, y + 1, 0xffffff66);
        fill(left() - 2, y + row.height - 1, right() - 12, y + row.height, 0xffffff66);
        fill(left() - 2, y, left() - 1, y + row.height, 0xffffff66);
        fill(right() - 13, y, right() - 12, y + row.height, 0xffffff66);
      }
      if (hover || choices.editing(s)) fill(left() - 2, y, right() - 12, y + row.height, 0x60000000);
      boolean narrow = row.height == 40;
      int cy = y + SettingRows.buttonOffset(span()),
          controlLeft = mainControlLeft(row);
      int textWidth = narrow ? span() - 4 : controlLeft(row) - left() - 13;
      if (audioVolumeAction(s)) textWidth = audioActionX(row) - left() - 13;
      else if (volumeAuxiliary(s)) textWidth = volumeAuxiliaryX(row) - left() - 13;
      int indent = search.text().isBlank() ? (int)Catalog.number(Catalog.metadata(s.id), "indent", 0) : 0;
      String label = fit(settingLabel(s), textWidth - indent - (s.restart ? 9 : 0));
      text(
          label,
          left() + 3 + indent,
          y + SettingRows.labelOffset(span()),
          settingChanged(s) ? 0xffdd88 : 0xc4c4c4);
      if (s.restart) text("*", left() + 6 + indent + textRenderer.getWidth(label),
          y + SettingRows.labelOffset(span()), 0xe8a0a0);
      List<Setting> conflicts = conflicts(s);
      int valueEnd = controlEnd(s);
      if (editable && valueEditor.editing(s))
        valueEditor.render(this, controlLeft, cy, valueEnd - controlLeft, mx, listMouseY, rowsTop, bottom());
      else if (editable && InheritedColors.accepts(s)) {
        var bounds = InheritedColorControl.at(controlLeft, valueEnd - controlLeft, InheritedColors.custom(s));
        button(fit(InheritedColors.label(s), bounds.modeWidth() - 8), controlLeft, cy, bounds.modeWidth(), 18, mx, listMouseY, true);
        if (bounds.colorWidth() > 0) colourButton(ColorSettings.hex(s), bounds.colorX(), cy, bounds.colorWidth(), mx, listMouseY);
      }
      else if (editable && ColorSettings.accepts(s)) colourButton(ColorSettings.hex(s), ColorSettings.opacityPercent(s, session.settings()),
          controlLeft, cy, valueEnd - controlLeft, mx, listMouseY);
      else if (s.slider() && editable) slider(value(s), controlLeft, cy, valueEnd - controlLeft, mx, listMouseY,
          (s.value.getAsDouble() - s.min) / Math.max(0.000001, s.max - s.min));
      else if (s.kind == Setting.Kind.KEY && editable) button(fit(value(s), Math.max(5, valueEnd - controlLeft - 8)),
          controlLeft, cy, valueEnd - controlLeft, 18, mx, listMouseY, true,
          local.luke.power.input.Chord.decode(s.value.getAsInt()).key() == 0 ? 0x999999 : -1);
      else if (SettingsDropdown.accepts(s) || playlistSetting(s)) WaypointSetDropdown.button(this, value(s), controlLeft, cy, valueEnd - controlLeft, mx, listMouseY, editable, choices.editing(s));
      else button(fit(value(s), Math.max(5, valueEnd - controlLeft - 8)),
          controlLeft, cy, valueEnd - controlLeft, 18, mx, listMouseY, editable);
      boolean defaultValue = rowSettings(s).stream().allMatch(value -> value == s || value.value.equals(value.defaultValue))
          && ColorSettings.atDefault(s, session.settings()) && (!s.id.equals("interface.debugOrder")
          || session.settings().stream().filter(value -> value.id.equals("interface.debugRightOrder")).allMatch(value -> value.value.equals(value.defaultValue)));
      if (!s.id.equals(HudEditorSettings.ENTRY)) button("R", right() - 34, cy, 20, 18, mx, listMouseY, editable && !defaultValue);
      if (audioVolumeAction(s)) button(s.id.equals("native.music") ? "Edit..." : "All...",
          audioActionX(row), audioActionY(row, y), 44, audioActionHeight(row), mx, listMouseY, editable);
      else if (customise(s)) button("Customize", controlLeft(row), cy, 62, 18, mx, listMouseY, editable);
      else if (soundPreview(s) || hasLink(s)) iconButton(soundPreview(s) ? "speaker" : s.kind == Setting.Kind.KEY ? "settings" : "controls",
          volumeAuxiliary(s) ? volumeAuxiliaryX(row) : controlLeft(row),
          volumeAuxiliary(s) ? volumeAuxiliaryY(row, y) : cy, 20,
          volumeAuxiliary(s) ? volumeAuxiliaryHeight(row) : 18, mx, listMouseY, editable,
          soundPreview(s) && local.luke.power.audio.AudioController.previewing(previewId(s)));
      if (!conflicts.isEmpty()) text("!", controlLeft(row) - 7, cy + 5, 0xff8855);
      if (hover) {
        hoverId = s.id;
        if (!editable) tip = Tooltips.setting(s, lock);
        else if (!s.id.equals(HudEditorSettings.ENTRY) && inside(mx, listMouseY, right() - 34, cy, 20, 18)) { tip = "Reset to " + DefaultBases.current().label + " default"; hoverId += ".reset"; }
        else if (!conflicts.isEmpty() && inside(mx, listMouseY, controlLeft(row) - 12, cy, 12, 18)) {
          tip = conflictTip(s, conflicts); hoverId += ".conflicts";
        } else if (audioVolumeAction(s) && inside(mx, listMouseY, audioActionX(row), audioActionY(row, y), 44, audioActionHeight(row))
            || volumeAuxiliary(s) && inside(mx, listMouseY, volumeAuxiliaryX(row), volumeAuxiliaryY(row, y), 20, volumeAuxiliaryHeight(row))
            || auxiliaryControl(s) && mx >= controlLeft(row) && mx < controlLeft) {
          tip = s.id.equals("native.music") ? "Music library and playlists" : s.id.equals("native.sound") ? "Individual game sounds" : customise(s) ? "Paint and customize the retro title lettering." : soundPreview(s) ? s.id.startsWith("audio.sound.music:") ? "Preview track; click again to stop" : "Preview sound; click again to stop" : s.kind == Setting.Kind.KEY ? "Related settings" : "Related controls"; hoverId += ".related";
        } else tip = Tooltips.setting(s, valueEditor.editing(s) ? valueEditor.help()
            : ItemEditors.accepts(s) ? "Choose from the item or block list." : "");
      }
    }
    if (sticky != null) {
      clip(left() - 2, top(), span() + 8, bottom() - top());
      groupHeading(rows.get(sticky.index()), sticky.y());
    }
    unclip();
    contentBar.render(this, contentTrack());
    }
    if (libraryVisible()) {
      library.render(libraryModal && !choices.active() ? pointerX : mx, libraryModal && !choices.active() ? pointerY : my, delta);
      tip = library.hoverHelp(); hoverId = "library:" + library.hoverKey();
    }
    valueEditor.endFrame();
    if (musicControlsVisible()) {
      var item = musicButton(mx, my);
      if (item != null) {
        var action = item.action();
        boolean endsPreview = action == AudioToolbar.Action.NEXT || action == AudioToolbar.Action.PREVIOUS
            && !local.luke.power.audio.AudioController.previousRestartsCurrent();
        tip = endsPreview && local.luke.power.audio.AudioController.previewing(local.luke.power.audio.AudioController.currentTrackId())
            ? "End preview" : "";
        hoverId = "music." + action;
      }
    }
    if (musicControlsVisible() && musicStatusVisible() && musicStatus.overTrack(mx, my)) {
      tip = "Show song"; hoverId = "music.current";
    }
    String footerError = !error.isEmpty() ? error : libraryVisible() ? library.footerError() : "";
    if (!footerError.isEmpty()) text(fit(footerError, uiWidth() - 16), origin() + 8, footerY() - 12, 0xffbb88);
    if (isolatedFooter()) {
      for (DraftFooter item : draftFooter()) button(item.label, item.x, height - 28, item.width, 20, mx, my, item.enabled);
    } else {
    button(filtered() ? "Reset listed..." : "Reset page...", resetX(), footerY(), 88, 20, mx, my, true);
    int changesEnd = changesEnd();
    if ((session.changes() > 0 || changedOnly) && changesEnd - changesX() >= 18) {
      boolean hover = inside(mx, my, changesX(), footerY(), changesEnd - changesX(), 20);
      text(fit(session.changes() + (session.changes() == 1 ? " unsaved change" : " unsaved changes"), changesEnd - changesX() - 2),
          changesX(), footerY() + 6, hover || changedOnly ? 0xffffa0 : 0xffdd88);
    }
    if (pauseActions()) button("Menu", origin() + 8, footerY(), 46, 20, mx, my, true);
    if (video()) button("Hide options", disabledX(), height - 28, 90, 20, mx, my, minecraft.world != null);
    if (controls()) button(showDisabled ? "Hide Disabled" : "Show Disabled", disabledX(), height - 28, 90, 20, mx, my, true);
    button(editorReturn == null ? "Cancel" : "Back", actionX(0), height - 28, actionWidth(0), 20, mx, my, true);
    button(MenuPreferences.current().autoApply ? "Auto apply" : "Apply", actionX(1), height - 28, actionWidth(1), 20, mx, my, true);
    if (inside(mx, my, actionX(1), height - 28, actionWidth(1), 20)) {
      tip = "Save changes without closing. Right-click to " + (MenuPreferences.current().autoApply ? "disable" : "enable") + " auto-apply: settings apply as you change them.";
      hoverId = "apply";
    }
    button("Done", actionX(2), height - 28, actionWidth(2), 20, mx, my, true);
    }
    if (hasSections() && inside(mx, my, sectionButtonX(), 27, 20, 20)) {
      tip = expandSections ? "Expand all sections" : "Collapse all sections";
      hoverId = "sections";
    }
    if (searchScopeVisible() && inside(mx, my, searchScopeX(), 27, 20, 20)) {
      tip = OptionsSearchScope.thisTab(page) ? "Searching only this tab. Click to search all settings."
          : "Searching all settings. Click to search only this tab.";
      hoverId = "searchScope";
    }
    if (canClear() && inside(mx, my, clearSearchX(), 27, 20, 20)) { tip = "Clear search"; hoverId = "clearSearch"; }
    if (hoverDelay.ready(!choices.active() && dragging == null && !(libraryVisible() && library.draggingSlider()) && !confirmClose && !confirmReset ? hoverId : "") && !tip.isEmpty()) tooltip(tip, mx, my);
    valueEditor.renderError(this);
    choices.render(this, pointerX, pointerY);
    if (libraryVisible() || !history.isEmpty()) backTooltip(backDestination(), left(), 27, 44, 20, mx, my);
    if (isolatedFooter()) {
      for (DraftFooter item : draftFooter()) if (item.label.equals("Back"))
        backTooltip(library.backDestination(), item.x, height - 28, item.width, 20, mx, my);
    } else if (editorReturn != null)
      backTooltip(editorDestination(), actionX(0), height - 28, actionWidth(0), 20, mx, my);
    if (libraryVisible() && !choices.active() && capture == null && !confirmClose && !confirmReset)
      library.renderDragGhost(pointerX, pointerY);
    if (capture != null || confirmClose || confirmReset) {
      fill(0, 0, width, height, 0xc0000000);
      String message =
          capture != null
              ? "Press a key or mouse button"
              : confirmReset ? "Reset " + (filtered() ? "listed settings" : page) + " to Defaults?" : "Save your changes?";
      if (capture != null) {
        drawCenteredTextWithShadow(textRenderer, message, width / 2, height / 2 - 18, 0xffffff);
        drawCenteredTextWithShadow(
            textRenderer, "Allowed modifiers: Ctrl / Shift / Alt", width / 2, height / 2, 0xaaaaaa);
        drawCenteredTextWithShadow(
            textRenderer, "Press Esc to cancel or Del to clear.", width / 2, height / 2 + 14, 0xaaaaaa);
        button("Cancel", width / 2 - 102, height / 2 + 36, 100, 20, pointerX, pointerY, true);
        button("Clear", width / 2 + 2, height / 2 + 36, 100, 20, pointerX, pointerY, true);
      }
      else {
        WarningText.draw(this, 312, height / 2, message, "", 0xaaaaaa);
        int cy = height / 2 + 12;
        button(confirmReset ? "Reset" : "Save", width / 2 - 102, cy, 100, 20, pointerX, pointerY, true);
        button(confirmReset ? "Cancel" : "Discard", width / 2 + 2, cy, 100, 20, pointerX, pointerY, true);
        if (confirmClose) button("Keep editing", width / 2 - 50, cy + 24, 100, 20, pointerX, pointerY, true);
      }
    }
    if (libraryVisible()) library.renderDialog(pointerX,pointerY);
  }
}
