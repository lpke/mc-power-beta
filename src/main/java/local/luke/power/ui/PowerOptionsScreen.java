package local.luke.power.ui;

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
          "Camera",
          "Movement",
          "Inventory",
          "Building",
          "Creative",
          "World editing",
          "Commands",
          "Gameplay",
          "Crafting",
          "Fixes",
          "Advanced");
  private static final int[] ICONS = {
    58, 20, 2256, 280, 323, 345, 301, 54, 45, 2, 271, 339, 260, 61, 265, 331
  };

  private record Row(String group, Setting setting, int y, int height) {}

  private record View(String page, double scroll, double sideScroll, String query,
      boolean changedOnly, List<String> conflicts, List<String> related, String relatedLabel, Set<String> collapsed,
      boolean showDisabled, int selected, boolean libraryOpen, MusicLibraryScreen.State library) {}
  private final Deque<View> history = new ArrayDeque<>();
  private List<String> relatedIds = List.of();
  private String relatedLabel = "";

  private static View rememberedView;
  private static List<View> rememberedHistory = List.of();
  private boolean hidden, autoSavePending, libraryOpen;
  private MusicLibraryScreen library;
  final InlineValueEditor valueEditor = new InlineValueEditor();
  private final MusicStatusBar musicStatus = new MusicStatusBar();
  private MusicLibraryScreen.State libraryState;
  private final ScrollBar contentBar = new ScrollBar(), sidebarBar = new ScrollBar();
  int contentLeft() { return left(); }
  int contentRight() { return right() - 14; }
  int contentBottom() { return bottom(); }
  int audioContentTop() { if (presetVisible()) return 52; return AudioToolbar.bottom(span(), libraryVisible()) + 3; }
  private boolean libraryVisible() { return libraryOpen && audio(); }
  private boolean presetVisible() { return libraryVisible() && library.presetVisible(); }
  private boolean musicControlsVisible() { return audio() && !presetVisible(); }
  private int musicStatusWidth() { return contentRight() - left() - (libraryVisible() && library.conversionVisible() ? 92 : 0); }
  private int contentHeight() { return rows.isEmpty() ? 0 : rows.get(rows.size() - 1).y + rows.get(rows.size() - 1).height + 6; }
  private ScrollBar.Track contentTrack() { return new ScrollBar.Track(right() - 8, top(), bottom() - top(), contentHeight(), scroll); }
  private ScrollBar.Track sidebarTrack() { return new ScrollBar.Track(origin() + sidebar() + 3, 24, bottom() - 24, PAGES.size() * 22, sideScroll); }

  private final boolean directPause;
  private static String rememberedPage = "General";
  private static final Map<String, Double> positions = new HashMap<>();
  private static final Set<String> rememberedCollapsed = new HashSet<>();
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

  private int uiWidth() { return Math.min(width, height * 16 / 9); }
  private int origin() { return (width - uiWidth()) / 2; }
  private int right() { return origin() + uiWidth(); }
  private int footerY() { return height - (uiWidth() < (controls() || video() || directPause ? 700 : 470) ? 51 : 28); }
  private int resetX() { return origin() + 8 + (directPause ? 50 : 0); }
  private int changesX() { return resetX() + 94; }
  private int changesEnd() { return footerY() < height - 28 ? right() - 8 : (controls() || video() ? disabledX() : actionX(0)) - 4; }
  private int actionWidth() { return uiWidth() < 470 ? 54 : 66; }
  private int actionX(int i) { return right() - 8 - (3 - i) * (actionWidth() + 4); }

  private int selected = -1, hoverTicks, musicRevision = -1;
  private String lastHover = "";
  private List<Row> rows = List.of();

  public PowerOptionsScreen(Screen parent) { this(parent, false); }

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

  /** Open music from another screen without inheriting a stale settings filter. */
  public static void openMusic(net.minecraft.client.Minecraft mc, Screen parent, boolean queue, String track) {
    PowerOptionsScreen screen = new PowerOptionsScreen(parent);
    screen.page = "Audio";
    screen.search.setText(""); screen.changedOnly = false;
    screen.conflictIds = List.of(); screen.relatedIds = List.of(); screen.history.clear();
    screen.libraryState = null; screen.libraryOpen = true;
    mc.setScreen(screen);
    if (screen.library == null) return;
    if (queue) screen.library.showQueue();
    else if (track != null && !track.isEmpty()) screen.library.showTrack(track);
    else screen.library.showTracks();
    screen.layout();
  }

  public void init() {
    valueEditor.cancel();
    Keyboard.enableRepeatEvents(true);
    if (session == null)
      try {
        session = SettingsRegistry.open(minecraft);
      } catch (Exception e) {
        PowerBeta.LOG.error("Could not open settings", e);
        error = "Settings could not be loaded. See the game log.";
      }
    if (session != null) {
      if (library == null) { library = new MusicLibraryScreen(this); library.restore(libraryState); }
      library.init(minecraft, width, height);
    }
    layout();
  }

  public void removed() {
    valueEditor.cancel();
    rememberPosition();
    musicStatus.cancel();
    contentBar.release(); sidebarBar.release();
    if (library != null) library.removed();
    Keyboard.enableRepeatEvents(false);
  }

  private View view() {
    return new View(page, scroll, sideScroll, search.text(), changedOnly, conflictIds,
        relatedIds, relatedLabel, Set.copyOf(collapsed), showDisabled, selected, libraryOpen, library == null ? libraryState : library.state());
  }

  private void rememberPosition() {
    rememberedView = view();
    rememberedHistory = List.copyOf(history);
    if (!changedOnly && conflictIds.isEmpty() && relatedIds.isEmpty() && search.text().isBlank()) {
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
    if (library != null) library.close();
    libraryOpen = false;
    layout();
  }

  private boolean controls() { return page.equals("Controls"); }
  private int disabledX() { return actionX(0) - 94; }
  private String previewId(Setting s) {
    if (s.id.startsWith("audio.sound.")) return s.id.substring("audio.sound.".length());
    return switch (s.id) {
      case "power_environment:config.MUSIC_CONFIG.volumeNetherPortalAmbient" -> "portal.portal";
      case "power_environment:config.MUSIC_CONFIG.volumeRainAmbient" -> "ambient.weather.rain";
      case "power_environment:config.MUSIC_CONFIG.volumeGhastAmbient" -> "mob.ghast.moan";
      case "power_environment:config.MUSIC_CONFIG.volumeCaveAmbient" -> "ambient.cave.cave";
      default -> null;
    };
  }
  private boolean soundPreview(Setting s) { return previewId(s) != null; }
  private boolean hasLink(Setting setting) {
    return setting.kind == Setting.Kind.KEY ? ControlLinks.related(session, setting) != null
        : ControlLinks.hasControls(setting);
  }
  private int mainControlLeft(Row row) {
    return controlLeft(row) + (soundPreview(row.setting) || hasLink(row.setting) ? 22 : 0);
  }
  private boolean canClear() { return libraryVisible() ? library.hasQuery() : !search.text().isBlank() || filtered(); }
  private int searchWidth() { return right() - (canClear() ? 36 : 14) - searchLeft(); }
  private int searchLeft() { return left() + (history.isEmpty() && !libraryVisible() ? 0 : 48); }
  private void pushView() {
    if (history.size() == 32) history.removeLast();
    history.push(view());
    search.focused = false;
    selected = -1;
  }
  private void back() {
    if (history.isEmpty()) return;
    restore(history.pop());
    layout();
  }
  private void restore(View view) {
    page = view.page; scroll = view.scroll; sideScroll = view.sideScroll;
    search.setText(view.query); search.selectAll(); search.focused = false;
    changedOnly = view.changedOnly; conflictIds = view.conflicts; relatedIds = view.related; relatedLabel = view.relatedLabel;
    collapsed.clear(); collapsed.addAll(view.collapsed);
    showDisabled = view.showDisabled; selected = view.selected;
    libraryOpen = view.libraryOpen; libraryState = view.library;
    if (library != null) library.restore(libraryState);
  }
  private void linkedControls(Setting setting) {
    List<Setting> bindings = ControlLinks.controls(session, setting);
    if (bindings.isEmpty()) return;
    rememberPosition(); pushView();
    page = "Controls";
    libraryOpen = false;
    relatedIds = bindings.stream().map(s -> s.id).toList();
    relatedLabel = setting.label;
    conflictIds = List.of(); changedOnly = false; search.setText(""); scroll = 0;
    layout();
  }
  private void click() { minecraft.soundManager.method_2009("random.click", 1, 1); }
  private void related(Setting key) {
    Setting target = ControlLinks.related(session, key);
    if (target == null) return;
    rememberPosition();
    pushView();
    relatedIds = ControlLinks.settings(session, key).stream().map(s -> s.id).toList();
    page = target.page;
    libraryOpen = false;
    conflictIds = List.of();
    changedOnly = false;
    search.setText("");
    scroll = 0;
    layout();
  }

  private void openGroup(Setting setting) {
    rememberPosition();
    pushView();
    page = setting.page;
    libraryOpen = false;
    search.setText("");
    changedOnly = false;
    conflictIds = relatedIds = List.of();
    collapsed.remove(page + "/" + setting.group);
    scroll = 0;
    layout();
    rows.stream().filter(r -> r.group.equals(setting.group)).findFirst()
        .ifPresent(r -> scroll = r.y);
    layout();
  }

  private boolean filtered() {
    return changedOnly || !relatedIds.isEmpty() || !conflictIds.isEmpty();
  }

  private List<Setting> resetTargets() {
    List<String> listed = conflictIds.isEmpty() ? relatedIds : conflictIds;
    return session.settings().stream()
        .filter(s -> changedOnly ? s.changed() : listed.isEmpty() ? s.page.equals(page) : listed.contains(s.id))
        .filter(s -> SettingAccess.reason(session, s).isEmpty())
        .toList();
  }

  public ConfigSession session() {
    return session;
  }

  private int sidebar() {
    return Math.max(90, Math.min(155, uiWidth() / 4));
  }

  private int left() {
    return origin() + sidebar() + 16;
  }

  private int span() {
    return right() - left() - 14;
  }

  private boolean audio() {
    return page.equals("Audio") && search.text().isBlank() && !changedOnly && conflictIds.isEmpty() && relatedIds.isEmpty();
  }

  private boolean video() {
    return page.equals("Video") && search.text().isBlank() && !changedOnly;
  }

  private int top() {
    return audio() ? audioContentTop() + 20 : 52;
  }

  private int bottom() {
    return footerY() - 16;
  }

  private StickyHeader stickyHeader() {
    return StickyHeader.at(rows, row -> row.setting == null, Row::y, Row::height,
        (int) scroll, top(), bottom());
  }

  private void groupHeading(Row row, int y) {
    text((collapsed.contains(page + "/" + row.group) && search.text().isBlank() ? "> " : "v ")
        + row.group, left(), y + 8, 0xffffff);
    fill(left(), y + 21, right() - 14, y + 22, 0x50555555);
  }

  private void layout() {
    if (session == null) return;
    String query = search.text().strip().toLowerCase(Locale.ROOT);
    Map<String, List<Setting>> groups = new LinkedHashMap<>();
    List<String> listed = conflictIds.isEmpty() ? relatedIds : conflictIds;
    List<Setting> candidates = listed.isEmpty() ? session.settings() : listed.stream()
        .map(id -> session.settings().stream().filter(s -> s.id.equals(id)).findFirst().orElse(null))
        .filter(Objects::nonNull).toList();
    Map<String, Integer> scores = new HashMap<>();
    if (!query.isEmpty() && listed.isEmpty()) {
      for (Setting s : candidates) scores.put(s.id, FuzzySearch.score(query, s.label, s.description + " " + s.group + " " + s.page));
      candidates = candidates.stream().sorted(Comparator.comparingInt(s -> scores.get(s.id))).toList();
    }
    for (Setting s : candidates) {
      if (s.group.equals("Music data")) continue;
      if (query.isEmpty() && !changedOnly && listed.isEmpty() && (s.group.equals("Track rotation") || s.id.startsWith("audio.sound.music:"))) continue;
      if (listed.isEmpty() && s.kind == Setting.Kind.KEY && !showDisabled && !ControlLinks.enabled(session, s)) continue;
      if ((!changedOnly || s.changed()) && (!listed.isEmpty() || (query.isEmpty()
          ? changedOnly || s.page.equals(page)
          : scores.getOrDefault(s.id, FuzzySearch.NONE) < FuzzySearch.NONE)))
        groups.computeIfAbsent(query.isEmpty() && !changedOnly ? s.group : s.page + " / " + s.group, k -> new ArrayList<>()).add(s);
    }
    if (audio()) {
      Map<String, List<Setting>> ordered = new LinkedHashMap<>();
      for (String group : List.of("Volume", "Music library", "Music gaps", "Pause menu music", "Sound categories", "Extra sounds", "Music and ambience", "Individual sounds", "Individual music tracks"))
        if (groups.containsKey(group)) ordered.put(group, groups.get(group));
      groups.forEach(ordered::putIfAbsent);
      groups = ordered;
      if (groups.containsKey("Volume")) groups.get("Volume").sort(Comparator.comparingInt(s -> s.id.equals("audio.master") ? 0 : 1));
    }
    if (page.equals("General") && query.isEmpty() && !filtered()) {
      Map<String, List<Setting>> ordered = new LinkedHashMap<>();
      for (String group : List.of("Game", "Interface", "Input"))
        if (groups.containsKey(group)) ordered.put(group, groups.get(group));
      groups.forEach(ordered::putIfAbsent);
      groups = ordered;
      List<String> order = List.of("native.difficulty", "world.cheats", "power_controls:general.autosaveInterval",
          "native.guiScale", "visual.slashChat", "power_controls:general.pauseOnLostFocus", "interface.pauseToOptions",
          "native.sensitivity", "native.invert", "power_controls:general.rawInput", "power_controls:general.disableControllerInit");
      groups.values().forEach(entries -> entries.sort(Comparator.comparingInt(s -> {
        int index = order.indexOf(s.id); return index < 0 ? Integer.MAX_VALUE : index;
      })));
    }
    if (video() && groups.containsKey("Rendering")) groups.get("Rendering").sort(Comparator.comparingInt(s -> s.id.equals("native.fpsLimit") ? 0 : 1));
    List<Row> next = new ArrayList<>();
    int y = 0, rowHeight = span() < 340 ? 40 : 24;
    for (var group : groups.entrySet()) {
      boolean flat = controls() && query.isEmpty() && !changedOnly || !listed.isEmpty();
      if (!flat) { next.add(new Row(group.getKey(), null, y, 24)); y += 24; }
      if (!flat && query.isEmpty() && !changedOnly && collapsed.contains(page + "/" + group.getKey())) continue;
      for (Setting s : group.getValue()) {
        next.add(new Row(group.getKey(), s, y, rowHeight));
        y += rowHeight;
      }
      y += 6;
    }
    rows = next;
    scroll = Math.max(0, Math.min(scroll, Math.max(0, y - (bottom() - top()))));
    sideScroll =
        Math.max(0, Math.min(sideScroll, Math.max(0, PAGES.size() * 22 - (bottom() - 24))));
  }

  public void tick() {
    hoverTicks++;
    if (library != null) library.tick();
    int revision = local.luke.power.audio.AudioController.libraryRevision();
    if (session != null && revision != musicRevision) {
      musicRevision = revision;
      if (local.luke.power.config.backend.AudioBackend.discoverMusic(session, minecraft)) layout();
    }
  }

  public void changed(Setting setting) { changed(setting, false); }

  public void finishContinuousChange() { if (autoSavePending) save(false, false); }

  public void changed(Setting setting, boolean continuous) {
    changed(List.of(setting), continuous);
  }

  /** Preview/save a group edit once, after all draft values have been updated. */
  public void changed(Collection<Setting> settings, boolean continuous) {
    settings.forEach(session::link);
    error = "";
    try { session.preview(MenuPreferences.current().autoApply); }
    catch (Exception e) {
      error = "Could not preview this value. " + Objects.toString(e.getMessage(), "See the game log.");
      PowerBeta.LOG.error("Settings preview failed", e);
    }
    if (MenuPreferences.current().autoApply && error.isEmpty()) {
      if (dragging == null && !continuous) save(false, false);
      else autoSavePending = true;
    }
    if (changedOnly) layout();
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
    if (hidden || wheel == 0 || capture != null || confirmClose || confirmReset) return;
    valueEditor.cancel();
    int x = Mouse.getEventX() * width / minecraft.displayWidth;
    if (x < origin() + sidebar() + 8) sideScroll -= Math.signum(wheel) * 44;
    else if (libraryVisible()) library.wheel(wheel);
    else scroll -= Math.signum(wheel) * 48;
    layout();
  }

  protected void mouseClicked(int x, int y, int button) {
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
      capture.value = new JsonPrimitive(new Chord(button - 100, Bindings.heldModifiers()).encoded());
      Setting bound = capture; capture = null; changed(bound);
      return;
    }
    if (confirmClose || confirmReset) {
      if (button == 0) dialogClick(x, y);
      return;
    }
    if (libraryVisible() && library.modal()) { library.dialogClick(x,y,button); return; }
    if (libraryVisible() && library.dirtyPreset()
        && (y >= bottom() || x < left() && y >= 24 && y < bottom())) {
      library.guard(() -> mouseClicked(x,y,button)); return;
    }
    if (button != 0 && button != 1) return;
    if (button == 0 && sidebarBar.press(sidebarTrack(), x, y)) { sideScroll = sidebarBar.drag(sidebarTrack(), y, true); return; }
    if (!libraryVisible() && button == 0 && contentBar.press(contentTrack(), x, y)) { scroll = contentBar.drag(contentTrack(), y, true); return; }
    if (button == 0 && (libraryVisible() || !history.isEmpty()) && inside(x, y, left(), 27, 44, 20)) {
      click(); if (libraryVisible()) library.back(this::closeLibrary); else back(); return;
    }
    if (inside(x, y, searchLeft(), 27, searchWidth(), 20)) {
      if (libraryVisible()) { library.searchClick(x, y, button); return; }
      search.focused = true;
      if (button == 1) {
        search.setText("");
        search.selectAll();
        scroll = 0;
        layout();
      }
      return;
    }
    if (canClear() && inside(x, y, right() - 32, 27, 18, 20)) {
      click();
      if (libraryVisible()) { library.clearQuery(); return; }
      if (!history.isEmpty() && (!conflictIds.isEmpty() || !relatedIds.isEmpty())) { back(); return; }
      conflictIds = List.of();
      relatedIds = List.of();
      search.setText("");
      search.selectAll();
      scroll = 0;
      layout();
      return;
    }
    search.focused = false;
    if (library != null) library.unfocus();
    if (button == 0 && y >= 24 && y < bottom() && x >= origin() && x < origin() + sidebar()) {
      int index = (int) ((y - 24 + sideScroll) / 22);
      if (index >= 0 && index < PAGES.size()) {
        boolean same = PAGES.get(index).equals(page);
        rememberPosition();
        click();
        history.clear();
        relatedIds = List.of();
        page = PAGES.get(index);
        conflictIds = List.of();
        changedOnly = false;
        search.setText("");
        search.selectAll();
        scroll = same ? 0 : positions.getOrDefault(page, 0d);
        if (page.equals("Audio")) closeLibrary();
        selected = -1;
        layout();
      }
      return;
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
        case QUEUE -> { if (library.dirtyPreset()) { library.guard(() -> { library.showQueue(); layout(); }); return; }
          if (libraryVisible() && library.queueVisible()) closeLibrary();
          else { libraryOpen = true; library.showQueue(); layout(); } }
        case RELOAD -> local.luke.power.audio.AudioController.reload();
        case LIBRARY -> { if (library.dirtyPreset()) { library.guard(() -> { library.showTracks(); layout(); }); return; }
          if (libraryVisible() && !library.queueVisible() && !library.presetVisible()) closeLibrary();
          else { libraryOpen = true; library.showTracks(); layout(); } }
      }
      return;
    }
    if (musicControlsVisible() && (!libraryVisible() || library.trackStatusVisible()) && musicStatus.press(x,y,button)) return;
    if (musicControlsVisible() && button == 0 && (!libraryVisible() || library.trackStatusVisible()) && musicStatus.overTrack(x, y)) {
      String id = local.luke.power.audio.AudioController.currentTrackId();
      if (!id.isEmpty()) { click(); libraryOpen = true; library.showTrack(id); layout(); }
      return;
    }
    if (libraryVisible() && x >= left() && y >= audioContentTop() && y < bottom()) {
      library.mouseClicked(x, y, button); return;
    }
    if (!libraryVisible() && y >= top() && y < bottom() && x >= left() && x < right() - 10) {
      StickyHeader sticky = stickyHeader();
      int pinned = sticky != null && sticky.contains(y, top()) ? sticky.index() : -1;
      for (int i = 0; i < rows.size(); i++) {
        if (pinned >= 0 && i != pinned) continue;
        Row r = rows.get(i);
        int ry = pinned == i ? sticky.y() : top() + r.y - (int) scroll;
        if (y < ry || y >= ry + r.height) continue;
        if (r.setting == null) {
          if (button == 0 && (!search.text().isBlank() || changedOnly)) {
            // Every search header is followed by at least one setting in its section.
            if (i + 1 < rows.size() && rows.get(i + 1).setting != null) {
              click();
              openGroup(rows.get(i + 1).setting);
            }
          } else if (button == 0) {
            String id = page + "/" + r.group;
            if (!collapsed.add(id)) collapsed.remove(id);
            if (pinned >= 0) scroll = r.y;
            layout();
          }
          return;
        }
        selected = i;
        Setting s = r.setting;
        if (!SettingAccess.reason(session, s).isEmpty()) return;
        int controlY = r.height == 40 ? ry + 16 : ry + 2;
        if (y < controlY) return;
        if (x >= right() - 34) {
          click();
          s.reset();
          changed(s);
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
        if (x >= controlLeft(r) && x < mainControlLeft(r)) {
          if (soundPreview(s)) local.luke.power.audio.AudioController.previewSound(previewId(s));
          else { click(); if (s.kind == Setting.Kind.KEY) related(s); else linkedControls(s); }
          return;
        }
        if (x < mainControlLeft(r) || x >= right() - 38) return;
        if (s.kind == Setting.Kind.INTEGER || s.kind == Setting.Kind.DECIMAL) {
          if (button == 0) {
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
    if (button == 1 && inside(x, y, actionX(1), height - 28, actionWidth(), 20)) {
      click();
      try {
        MenuPreferences.update(v -> v.autoApply = !v.autoApply);
        if (MenuPreferences.current().autoApply) save(false, false);
      } catch (Exception e) { error = "Could not save auto-apply preference"; PowerBeta.LOG.error(error, e); }
      return;
    }
    if (button != 0) return;
    if (video() && minecraft.world != null && inside(x, y, disabledX(), height - 28, 90, 20)) { click(); hidden = true; return; }
    if (directPause && inside(x, y, origin() + 8, footerY(), 46, 20)) {
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
    if ((session.changes() > 0 || changedOnly) && inside(x, y, changesX(), footerY(), changesEnd - changesX(), 20)) {
      click();
      conflictIds = List.of();
      relatedIds = List.of();
      changedOnly = !changedOnly;
      search.setText("");
      scroll = 0;
      layout();
      return;
    }
    if (inside(x, y, actionX(0), height - 28, actionWidth(), 20)) {
      click();
      if (session.changes() > 0) confirmClose = true;
      else minecraft.setScreen(parent);
      return;
    }
    if (inside(x, y, actionX(1), height - 28, actionWidth(), 20)) save(false);
    if (inside(x, y, actionX(2), height - 28, actionWidth(), 20)) save(true);
  }

  private void activate(Setting s, int direction) {
    if (!SettingAccess.reason(session, s).isEmpty()) return;
    error = "";
    click();
    if (s.id.equals("audio.preset")) {
      libraryOpen = true; library.openPresets(); layout(); return;
    }
    if (s.id.equals("native.texturePack")) {
      minecraft.setScreen(new TexturePackScreen(this, s));
      return;
    }
    switch (s.kind) {
      case KEY -> { capture = s; captureModifier = 0; }
      case TEXT, LIST -> {
        if (s.id.equals("video.fogCycle"))
          minecraft.setScreen(new IntegerArrayScreen(this, s,
              new IntegerArrayDraft.Rules(2, 32, 16, true), "Distance", "chunks"));
        else if (s.id.equals("audio.musicDirectories") || s.id.equals("audio.menuDirectories"))
          minecraft.setScreen(new FolderScreen(this, s));
        else if (ColourScreen.accepts(s)) minecraft.setScreen(new ColourScreen(this, s));
        else minecraft.setScreen(new ValueScreen(this, s));
      }
      default -> {
        s.cycle(direction);
        changed(s);
      }
    }
  }

  private void save(boolean close) { save(close, true); }

  private void save(boolean close, boolean sound) {
    if (sound) click();
    autoSavePending = false;
    try {
      savedRestart |= session.restartRequired();
      session.save(FabricLoader.getInstance().getGameDir());
      if (savedRestart) {
        savedRestart = false;
        error = "Saved. Restart the game for marked changes.";
        confirmClose = false;
      } else if (close) minecraft.setScreen(exitTarget);
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
        try { session.preview(MenuPreferences.current().autoApply); } catch (Exception e) { error = e.getMessage(); }
        if (MenuPreferences.current().autoApply && error.isEmpty()) save(false, false);
        layout();
        confirmReset = false;
      } else save(true);
    } else if (inside(x, y, width / 2 + 2, cy, 100, 20)) {
      if (confirmReset) confirmReset = false;
      else {
        try {
          session.discard();
          minecraft.setScreen(exitTarget);
        } catch (Exception e) { error = "Could not restore settings: " + e.getMessage(); confirmClose = false; }
      }
    } else if (inside(x, y, width / 2 - 50, cy + 24, 100, 20)) {
      confirmClose = false;
      confirmReset = false;
      exitTarget = parent;
    }
  }

  protected void keyPressed(char c, int key) {
    if (valueEditor.key(c, key)) return;
    if (hidden) { if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_F1) hidden = false; return; }
    if (capture != null) {
      if (key == Keyboard.KEY_ESCAPE) {
        capture = null;
        return;
      }
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
        confirmClose = confirmReset = false; exitTarget = parent;
      }
      return;
    }
    if (libraryVisible() && library.modal()) { library.keyPressed(c,key); return; }
    if (key == Keyboard.KEY_ESCAPE) {
      if (libraryVisible()) { library.back(this::closeLibrary); return; }
      if (!history.isEmpty()) { back(); return; }
      if (!conflictIds.isEmpty()) { conflictIds = List.of(); scroll = positions.getOrDefault(page, 0d); layout(); return; }
      if (search.focused) {
        search.focused = false;
        return;
      }
      if (session != null && session.changes() > 0) confirmClose = true;
      else minecraft.setScreen(parent);
      return;
    }
    if ((Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
        && key == Keyboard.KEY_F) {
      if (libraryVisible()) { library.focus(); return; }
      search.focused = true;
      search.selectAll();
      return;
    }
    if (libraryVisible()) { library.keyPressed(c, key); return; }
    if (search.focused) {
      String before = search.text();
      search.key(c, key);
      if (!before.equals(search.text())) {
        conflictIds = List.of();
        relatedIds = List.of();
        scroll = 0;
        selected = -1;
        layout();
      }
      return;
    }
    if (key == Keyboard.KEY_DOWN || key == Keyboard.KEY_UP || key == Keyboard.KEY_TAB) {
      int dir = key == Keyboard.KEY_UP ? -1 : 1;
      selected = Math.floorMod(selected + dir, Math.max(1, rows.size()));
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
    if (!SettingAccess.reason(session, s).isEmpty()) return SettingAccess.lockedValue(s);
    if (s.id.equals("audio.preset")) return new MusicEdits(this).name(s.value.getAsString());
    if (capture == s) return modifierLabel(Bindings.heldModifiers()) + "Press key...";
    if (s.kind != Setting.Kind.KEY) return s.display();
    Chord chord = Chord.decode(s.value.getAsInt());
    int code = chord.key();
    return modifierLabel(chord.modifiers()) + (code == 0
        ? "Unbound"
        : code < 0
            ? "Mouse " + (code + 101)
            : Objects.toString(Keyboard.getKeyName(code), "Key " + code));
  }

  private static String modifierLabel(int mask) {
    return ((mask & Chord.CTRL) != 0 ? "Ctrl+" : "")
        + ((mask & Chord.SHIFT) != 0 ? "Shift+" : "")
        + ((mask & Chord.ALT) != 0 ? "Alt+" : "");
  }

  private List<Setting> conflicts(Setting s) {
    if (s.kind != Setting.Kind.KEY || Chord.decode(s.value.getAsInt()).key() == 0) return List.of();
    return session.settings().stream().filter(v -> v != s && v.kind == Setting.Kind.KEY
        && Chord.decode(v.value.getAsInt()).key() == Chord.decode(s.value.getAsInt()).key()).toList();
  }

  private String conflictTip(Setting s, List<Setting> conflicts) {
    StringBuilder text = new StringBuilder();
    for (Setting v : conflicts) {
      if (text.length() > 0) text.append('\n');
      text.append(v.label).append(": ").append(value(v));
    }
    text.append("\nThe matching binding with more modifiers takes priority.");
    return text.toString();
  }

  private AudioToolbar.Layout musicLayout() {
    return AudioToolbar.layout(left(), span(), local.luke.power.audio.MusicRequests.tracks().size(),
        local.luke.power.audio.AudioController.musicPlaying(), libraryVisible());
  }
  private List<AudioToolbar.Button> musicButtons() { return musicLayout().buttons(); }
  private AudioToolbar.Button musicButton(int x, int y) {
    for (var button : musicButtons()) if (button.contains(x, y)) return button;
    return null;
  }

  public void render(int mx, int my, float delta) {
    valueEditor.beginFrame();
    if (!hidden && !confirmClose && !confirmReset && capture == null) {
      sideScroll = sidebarBar.drag(sidebarTrack(), my, Mouse.isButtonDown(0));
      if (!libraryVisible()) scroll = contentBar.drag(contentTrack(), my, Mouse.isButtonDown(0));
    }
    // This wait belongs only to binding capture, never gameplay input.
    if (capture != null && captureModifier != 0 && !Bindings.physical(captureModifier)) {
      capture.value = new JsonPrimitive(captureModifier);
      Setting bound = capture; capture = null;
      captureModifier = 0; changed(bound);
    }
    if (dragging != null) {
      if (Mouse.isButtonDown(0)) drag(Mouse.getX());
      else { dragging = null; if (autoSavePending) save(false, false); }
    }
    if (hidden) {
      button("Show options", disabledX(), height - 28, 90, 20, mx, my, true);
      return;
    }
    renderBackground();
    fill(0, 0, width, 22, 0x70000000);
    fill(origin(), footerY() - 10, right(), height, 0x90000000);
    drawCenteredTextWithShadow(textRenderer, "Options", width / 2, 7, 0xffffff);
    if (session == null) {
      text(error, 10, 45, 0xff8888);
      button("Back", width / 2 - 50, height - 28, 100, 20, mx, my, true);
      return;
    }
    fill(origin() + sidebar() + 4, 24, origin() + sidebar() + 7, bottom(), 0x90404040);
    fill(origin() + sidebar() + 7, 24, origin() + sidebar() + 8, bottom(), 0x90000000);
    clip(origin(), 24, sidebar() + 2, bottom() - 24);
    for (int i = 0; i < PAGES.size(); i++) {
      int y = 24 + i * 22 - (int) sideScroll;
      boolean hover = inside(mx, my, origin(), y, sidebar(), 22);
      if (PAGES.get(i).equals(page) && search.text().isBlank() && !changedOnly)
        fill(origin(), y, origin() + sidebar(), y + 21, 0xc0000000);
      GL11.glPushMatrix();
      GL11.glColor4f(1, 1, 1, 1);
      icons.method_1487(
          textRenderer, minecraft.textureManager, new ItemStack(ICONS[i], 1, 0), origin() + 7, y + 2);
      GL11.glPopMatrix();
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      text(
          fit(PAGES.get(i), sidebar() - 31),
          origin() + 28,
          y + 7,
          hover ? 0xffffa0 : PAGES.get(i).equals(page) ? 0xffffff : 0xaaaaaa);
    }
    unclip();
    sidebarBar.render(this, sidebarTrack());
    if (libraryVisible() || !history.isEmpty()) button("Back", left(), 27, 44, 20, mx, my, true);
    if (libraryVisible()) library.renderSearch(searchLeft(), 28, searchWidth(), mx, my);
    else input(search, searchLeft(), 28, searchWidth(), mx, my,
        !conflictIds.isEmpty() ? "Conflicting bindings" : !relatedIds.isEmpty() ? controls() ? "Related controls for \"" + relatedLabel + "\"" : "Related settings"
        : changedOnly ? "Search changed settings..." : "Search all settings...");
    if (canClear()) button("x", right() - 32, 27, 18, 20, mx, my, true);
    if (musicControlsVisible()) {
      for (var item : musicButtons()) {
        if (item.action() == AudioToolbar.Action.PREVIOUS || item.action() == AudioToolbar.Action.NEXT)
          trackButton(item.label(), item.action() == AudioToolbar.Action.PREVIOUS,
              item.x(), item.y(), item.width(), mx, my);
        else if (item.action() == AudioToolbar.Action.PLAY)
          iconButton(local.luke.power.audio.AudioController.musicPlaying() ? "pause" : "play",
              item.x(), item.y(), item.width(), mx, my, true);
        else button(fit(item.label(), item.width() - 4), item.x(), item.y(), item.width(), 18, mx, my, item.enabled());
        if (libraryVisible() && item.enabled()
            && (item.action() == AudioToolbar.Action.QUEUE && library.queueVisible()
                || item.action() == AudioToolbar.Action.LIBRARY && !library.queueVisible()))
          fill(item.x() + 2, item.y() + 15, item.x() + item.width() - 2, item.y() + 17, 0xffb0b0b0);
      }
      if (!libraryVisible() || library.trackStatusVisible()) musicStatus.render(this,left(),audioContentTop(),
          musicStatusWidth(),mx,my);
    }
    String tip = "", hoverId = "";
    if (!libraryVisible()) {
    StickyHeader sticky = stickyHeader();
    int rowsTop = sticky == null ? top() : Math.max(top(), sticky.y() + sticky.height());
    clip(left() - 2, rowsTop, span() + 8, Math.max(0, bottom() - rowsTop));
    int listMouseY = sticky != null && sticky.contains(my, top()) ? -1 : my;
    for (int index = 0; index < rows.size(); index++) {
      Row row = rows.get(index);
      int y = top() + row.y - (int) scroll;
      if (y + row.height < top() || y >= bottom()) continue;
      if (row.setting == null) {
        if (sticky == null || index != sticky.index()) groupHeading(row, y);
        continue;
      }
      Setting s = row.setting;
      String lock = SettingAccess.reason(session, s);
      boolean editable = lock.isEmpty();
      boolean hover = inside(mx, listMouseY, left(), y, span(), row.height) && listMouseY >= top() && listMouseY < bottom();
      if (hover) fill(left() - 2, y, right() - 12, y + row.height, 0x60000000);
      boolean narrow = row.height == 40;
      int cy = y + (narrow ? 16 : 2),
          controlLeft = mainControlLeft(row);
      int textWidth = narrow ? span() - 4 : controlLeft(row) - left() - 13;
      String label = fit(s.label, textWidth - (s.restart ? 9 : 0));
      text(
          label,
          left() + 3,
          y + (narrow ? 3 : 8),
          s.changed() ? 0xffdd88 : 0xc4c4c4);
      if (s.restart) text("*", left() + 6 + textRenderer.getWidth(label),
          y + (narrow ? 3 : 8), 0xe8a0a0);
      List<Setting> conflicts = conflicts(s);
      boolean numeric = s.kind == Setting.Kind.INTEGER || s.kind == Setting.Kind.DECIMAL;
      int valueEnd = right() - 38;
      if (numeric && editable && valueEditor.editing(s))
        valueEditor.render(this, controlLeft, cy, valueEnd - controlLeft, mx, listMouseY, rowsTop, bottom());
      else if (numeric && editable) slider(value(s), controlLeft, cy, valueEnd - controlLeft, mx, listMouseY,
          (s.value.getAsDouble() - s.min) / Math.max(0.000001, s.max - s.min));
      else if (editable && ColourScreen.accepts(s)) colourButton(value(s), controlLeft, cy, valueEnd - controlLeft, mx, listMouseY);
      else if (s.kind == Setting.Kind.KEY && editable) button(fit(value(s), Math.max(5, valueEnd - controlLeft - 8)),
          controlLeft, cy, valueEnd - controlLeft, 18, mx, listMouseY, true,
          local.luke.power.input.Chord.decode(s.value.getAsInt()).key() == 0 ? 0x999999 : -1);
      else button(fit(value(s), Math.max(5, valueEnd - controlLeft - 8)),
          controlLeft, cy, valueEnd - controlLeft, 18, mx, listMouseY, editable);
      button("R", right() - 34, cy, 20, 18, mx, listMouseY, editable && !s.value.equals(s.defaultValue));
      if (controlLeft > controlLeft(row)) iconButton(soundPreview(s) ? "speaker" : s.kind == Setting.Kind.KEY ? "settings" : "controls", controlLeft(row), cy, 20, mx, listMouseY, editable, soundPreview(s) && local.luke.power.audio.AudioController.previewing(previewId(s)));
      if (!conflicts.isEmpty()) text("!", controlLeft(row) - 7, cy + 5, 0xff8855);
      if (hover) {
        hoverId = s.id;
        if (!editable) tip = lock + (s.description.isEmpty() ? "" : "\n" + s.description);
        else if (mx >= right() - 34) { tip = "Reset to " + s.display(s.defaultValue); hoverId += ".reset"; }
        else if (!conflicts.isEmpty() && inside(mx, listMouseY, controlLeft(row) - 12, cy, 12, 18)) {
          tip = conflictTip(s, conflicts); hoverId += ".conflicts";
        } else if (mx >= controlLeft(row) && mx < controlLeft) {
          tip = soundPreview(s) ? s.id.startsWith("audio.sound.music:") ? "Preview track; click again to stop" : "Preview sound; click again to stop" : s.kind == Setting.Kind.KEY ? "Related settings" : "Related controls"; hoverId += ".related";
        } else tip = valueEditor.editing(s) ? valueEditor.help()
            : s.description + (numeric && mx >= controlLeft ? "\nRight-click to enter an exact value." : "")
                + (s.restart ? "\nRestart required." : "");
      }
    }
    if (sticky != null) {
      clip(left() - 2, top(), span() + 8, bottom() - top());
      groupHeading(rows.get(sticky.index()), sticky.y());
    }
    unclip();
    contentBar.render(this, contentTrack());
    } else { library.render(mx, my, delta); tip = library.hoverHelp(); hoverId = "library:" + library.hoverKey(); }
    valueEditor.endFrame();
    if (musicControlsVisible()) {
      var item = musicButton(mx, my);
      if (item != null) {
        var action = item.action();
        tip = switch (action) {
          case QUIET -> "End this track and wait for the next one. Automatic music stays on.";
          case PLAY -> "Play or pause music";
          case PREVIOUS -> "Previous track";
          case NEXT -> "Next track; queued tracks take priority";
          case QUEUE -> item.enabled() ? "Open the music queue" : "Queue is empty. Add tracks from the library.";
          case RELOAD -> "Rescan music folders";
          case LIBRARY -> "Browse music tracks and folders";
        };
        hoverId = "music." + action;
      }
    }
    if (musicControlsVisible() && (!libraryVisible() || library.trackStatusVisible()) && musicStatus.overTrack(mx, my)) {
      tip = "Show this track in Everything"; hoverId = "music.current";
    }
    if (!error.isEmpty()) text(fit(error, uiWidth() - 16), origin() + 8, footerY() - 12, 0xffbb88);
    button(filtered() ? "Reset listed..." : "Reset page...", resetX(), footerY(), 88, 20, mx, my, true);
    int changesEnd = changesEnd();
    if (session.changes() > 0 || changedOnly) {
      boolean hover = inside(mx, my, changesX(), footerY(), changesEnd - changesX(), 20);
      text(fit(session.changes() + (session.changes() == 1 ? " unsaved change" : " unsaved changes"), changesEnd - changesX() - 2),
          changesX(), footerY() + 6, hover || changedOnly ? 0xffffa0 : 0xffdd88);
    }
    if (directPause) button("Menu", origin() + 8, footerY(), 46, 20, mx, my, true);
    if (video()) button("Hide options", disabledX(), height - 28, 90, 20, mx, my, minecraft.world != null);
    if (controls()) button(showDisabled ? "Hide Disabled" : "Show Disabled", disabledX(), height - 28, 90, 20, mx, my, true);
    button("Cancel", actionX(0), height - 28, actionWidth(), 20, mx, my, true);
    button(MenuPreferences.current().autoApply ? "Auto apply" : "Apply", actionX(1), height - 28, actionWidth(), 20, mx, my, true);
    if (inside(mx, my, actionX(1), height - 28, actionWidth(), 20)) {
      tip = "Save changes without closing. Right-click to " + (MenuPreferences.current().autoApply ? "disable" : "enable") + " auto-apply: settings apply as you change them.";
      hoverId = "apply";
    }
    button("Done", actionX(2), height - 28, actionWidth(), 20, mx, my, true);
    if (!hoverId.equals(lastHover)) {
      lastHover = hoverId;
      hoverTicks = 0;
    }
    if (!tip.isEmpty() && hoverTicks > 12 && !confirmClose && !confirmReset) tooltip(tip, mx, my);
    if (capture != null || confirmClose || confirmReset) {
      fill(0, 0, width, height, 0xc0000000);
      String message =
          capture != null
              ? "Press a key or mouse button"
              : confirmReset ? "Reset " + (filtered() ? "listed settings" : page) + " to Defaults?" : "Save your changes?";
      drawCenteredTextWithShadow(textRenderer, message, width / 2, height / 2 - 18, 0xffffff);
      if (capture != null) {
        drawCenteredTextWithShadow(
            textRenderer, "Modifiers: Ctrl / Shift / Alt", width / 2, height / 2, 0xaaaaaa);
        drawCenteredTextWithShadow(
            textRenderer, "Escape cancels. Delete clears.", width / 2, height / 2 + 14, 0xaaaaaa);
      }
      else {
        int cy = height / 2 + 12;
        button(confirmReset ? "Reset" : "Save", width / 2 - 102, cy, 100, 20, mx, my, true);
        button(confirmReset ? "Cancel" : "Discard", width / 2 + 2, cy, 100, 20, mx, my, true);
        if (confirmClose) button("Keep editing", width / 2 - 50, cy + 24, 100, 20, mx, my, true);
      }
    }
    if (libraryVisible()) library.renderDialog(mx,my);
  }
}
