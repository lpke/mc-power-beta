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
          "Gameplay",
          "Crafting",
          "Fixes",
          "Advanced");
  private static final int[] ICONS = {
    58, 20, 2256, 280, 323, 345, 301, 54, 45, 2, 271, 260, 61, 265, 331
  };

  private record Row(String group, Setting setting, int y, int height) {}

  private record View(String page, double scroll, double sideScroll, String query,
      boolean changedOnly, List<String> conflicts, List<String> related, Set<String> collapsed,
      boolean showDisabled, int selected) {}
  private final Deque<View> history = new ArrayDeque<>();
  private List<String> relatedIds = List.of();

  private static String rememberedPage = "General";
  private static final Map<String, Double> positions = new HashMap<>();
  private static final Set<String> rememberedCollapsed = new HashSet<>();
  private static double rememberedSideScroll;
  private static boolean showDisabled;
  private List<String> conflictIds = List.of();
  private final Screen parent;
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
  private int footerY() { return height - (uiWidth() < (controls() ? 630 : 470) ? 51 : 28); }
  private int actionWidth() { return uiWidth() < 470 ? 54 : 66; }
  private int actionX(int i) { return right() - 8 - (3 - i) * (actionWidth() + 4); }

  private int selected = -1, hoverTicks, musicRevision = -1;
  private String lastHover = "";
  private List<Row> rows = List.of();

  public PowerOptionsScreen(Screen parent) {
    this.parent = parent;
    page = rememberedPage;
    scroll = positions.getOrDefault(page, 0d);
    sideScroll = rememberedSideScroll;
    collapsed.addAll(rememberedCollapsed);
  }

  public void init() {
    Keyboard.enableRepeatEvents(true);
    if (session == null)
      try {
        session = SettingsRegistry.open(minecraft);
      } catch (Exception e) {
        PowerBeta.LOG.error("Could not open settings", e);
        error = "Settings could not be loaded. See the game log.";
      }
    layout();
  }

  public void removed() {
    rememberPosition();
    Keyboard.enableRepeatEvents(false);
  }

  private void rememberPosition() {
    if (!changedOnly && conflictIds.isEmpty() && relatedIds.isEmpty() && search.text().isBlank()) {
      rememberedPage = page;
      positions.put(page, scroll);
      rememberedSideScroll = sideScroll;
      rememberedCollapsed.clear();
      rememberedCollapsed.addAll(collapsed);
    }
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
  private int searchLeft() { return left() + (history.isEmpty() ? 0 : 48); }
  private void pushView() {
    if (history.size() == 32) history.removeLast();
    history.push(new View(page, scroll, sideScroll, search.text(), changedOnly, conflictIds,
        relatedIds, Set.copyOf(collapsed), showDisabled, selected));
    search.focused = false;
    selected = -1;
  }
  private void back() {
    if (history.isEmpty()) return;
    View view = history.pop();
    page = view.page; scroll = view.scroll; sideScroll = view.sideScroll;
    search.setText(view.query); search.selectAll(); search.focused = false;
    changedOnly = view.changedOnly; conflictIds = view.conflicts; relatedIds = view.related;
    collapsed.clear(); collapsed.addAll(view.collapsed);
    showDisabled = view.showDisabled; selected = view.selected;
    layout();
  }
  private void linkedControls(Setting setting) {
    List<Setting> bindings = ControlLinks.controls(session, setting);
    if (bindings.isEmpty()) return;
    rememberPosition(); pushView();
    page = "Controls";
    relatedIds = bindings.stream().map(s -> s.id).toList();
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
    return audio() ? 88 : 52;
  }

  private int bottom() {
    return footerY() - 16;
  }

  private void layout() {
    if (session == null) return;
    String query = search.text().strip().toLowerCase(Locale.ROOT);
    Map<String, List<Setting>> groups = new LinkedHashMap<>();
    List<String> listed = conflictIds.isEmpty() ? relatedIds : conflictIds;
    List<Setting> candidates = listed.isEmpty() ? session.settings() : listed.stream()
        .map(id -> session.settings().stream().filter(s -> s.id.equals(id)).findFirst().orElse(null))
        .filter(Objects::nonNull).toList();
    for (Setting s : candidates) {
      if (listed.isEmpty() && s.kind == Setting.Kind.KEY && !showDisabled && !ControlLinks.enabled(session, s)) continue;
      if ((!changedOnly || s.changed()) && (!listed.isEmpty() || (query.isEmpty()
          ? changedOnly || s.page.equals(page)
          : (s.label + " " + s.description + " " + s.group + " " + s.page).toLowerCase(Locale.ROOT).contains(query))))
        groups.computeIfAbsent(query.isEmpty() && !changedOnly ? s.group : s.page + " / " + s.group, k -> new ArrayList<>()).add(s);
    }
    if (audio()) {
      Map<String, List<Setting>> ordered = new LinkedHashMap<>();
      for (String group : List.of("Volume", "Music library", "Sound categories", "Extra sounds", "Music and ambience", "Individual sounds", "Individual music tracks"))
        if (groups.containsKey(group)) ordered.put(group, groups.get(group));
      groups.forEach(ordered::putIfAbsent);
      groups = ordered;
      if (groups.containsKey("Volume")) groups.get("Volume").sort(Comparator.comparingInt(s -> s.id.equals("audio.master") ? 0 : 1));
    }
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
    int revision = local.luke.power.audio.AudioController.libraryRevision();
    if (session != null && revision != musicRevision) {
      musicRevision = revision;
      if (local.luke.power.config.backend.AudioBackend.discoverMusic(session, minecraft)) layout();
    }
  }

  public void changed(Setting setting) {
    session.link(setting);
    error = "";
    try { session.preview(); }
    catch (Exception e) {
      error = "Could not preview this value. " + Objects.toString(e.getMessage(), "See the game log.");
      PowerBeta.LOG.error("Settings preview failed", e);
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
    if (wheel == 0 || capture != null || confirmClose || confirmReset) return;
    int x = Mouse.getEventX() * width / minecraft.displayWidth;
    if (x < origin() + sidebar() + 8) sideScroll -= Math.signum(wheel) * 44;
    else scroll -= Math.signum(wheel) * 48;
    layout();
  }

  protected void mouseClicked(int x, int y, int button) {
    if (session == null) {
      if (y > height - 30) minecraft.setScreen(parent);
      return;
    }
    if (capture != null) {
      capture.value = new JsonPrimitive(new Chord(button - 100, Bindings.heldModifiers()).encoded());
      capture = null;
      return;
    }
    if (confirmClose || confirmReset) {
      if (button == 0) dialogClick(x, y);
      return;
    }
    if (button != 0 && button != 1) return;
    if (button == 0 && !history.isEmpty() && inside(x, y, left(), 28, 44, 18)) {
      click(); back(); return;
    }
    if (inside(x, y, searchLeft(), 28, right() - 36 - searchLeft(), 18)) {
      search.focused = true;
      if (button == 1) {
        search.setText("");
        search.selectAll();
        scroll = 0;
        layout();
      }
      return;
    }
    if (inside(x, y, right() - 32, 28, 18, 18)) {
      click();
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
    if (button == 0 && y >= 24 && y < bottom() && x >= origin() && x < origin() + sidebar()) {
      int index = (int) ((y - 24 + sideScroll) / 22);
      if (index >= 0 && index < PAGES.size()) {
        rememberPosition();
        click();
        history.clear();
        relatedIds = List.of();
        page = PAGES.get(index);
        conflictIds = List.of();
        changedOnly = false;
        search.setText("");
        search.selectAll();
        scroll = positions.getOrDefault(page, 0d);
        selected = -1;
        layout();
      }
      return;
    }
    if (audio() && button == 0 && y >= 52 && y < 72) {
      int index = (x - left()) / Math.max(1, span() / 4);
      if (x < left() || x >= right() - 14) return;
      click();
      if (index == 0) local.luke.power.audio.AudioController.togglePause();
      else if (index == 1) local.luke.power.audio.AudioController.previous();
      else if (index == 2) local.luke.power.audio.AudioController.next();
      else if (index == 3) local.luke.power.audio.AudioController.reload();
      return;
    }
    if (y >= top() && y < bottom() && x >= left() && x < right() - 10) {
      for (int i = 0; i < rows.size(); i++) {
        Row r = rows.get(i);
        int ry = top() + r.y - (int) scroll;
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
            layout();
          }
          return;
        }
        selected = i;
        Setting s = r.setting;
        int controlY = r.height == 40 ? ry + 16 : ry + 2;
        if (y < controlY) return;
        if (x >= right() - 34) {
          click();
          s.reset();
          changed(s);
          return;
        }
        if ((s.kind == Setting.Kind.INTEGER || s.kind == Setting.Kind.DECIMAL) && x >= right() - 60) {
          click();
          minecraft.setScreen(new ValueScreen(this, s));
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
          click();
          if (soundPreview(s)) local.luke.power.audio.AudioController.previewSound(previewId(s));
          else if (s.kind == Setting.Kind.KEY) related(s);
          else linkedControls(s);
          return;
        }
        if (x < mainControlLeft(r)) return;
        if (s.kind == Setting.Kind.INTEGER || s.kind == Setting.Kind.DECIMAL) {
          if (button == 0) {
            dragging = s;
            double pixels = (double) minecraft.displayWidth / width;
            dragLeft = (mainControlLeft(r) + 4) * pixels;
            dragSpan = Math.max(1, right() - 62 - mainControlLeft(r) - 8) * pixels;
            drag((int) (x * pixels));
          } else activate(s, -1);
        } else activate(s, button == 1 ? -1 : 1);
        return;
      }
    }
    if (button != 0) return;
    if (controls() && inside(x, y, disabledX(), height - 28, 90, 20)) {
      click(); showDisabled = !showDisabled; layout(); return;
    }
    if (inside(x, y, origin() + 8, footerY(), 88, 20)) {
      click();
      confirmReset = true;
      return;
    }
    int changesEnd = footerY() < height - 28 ? right() - 8 : (controls() ? disabledX() : actionX(0)) - 4;
    if ((session.changes() > 0 || changedOnly) && inside(x, y, origin() + 102, footerY(), changesEnd - origin() - 102, 20)) {
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
    error = "";
    click();
    if (s.id.equals("native.texturePack")) {
      minecraft.setScreen(new TexturePackScreen(this, s));
      return;
    }
    switch (s.kind) {
      case KEY -> { capture = s; captureModifier = 0; }
      case TEXT, LIST -> {
        if (s.id.equals("audio.musicDirectories") || s.id.equals("audio.menuDirectories"))
          minecraft.setScreen(new FolderScreen(this, s));
        else minecraft.setScreen(new ValueScreen(this, s));
      }
      default -> {
        s.cycle(direction);
        changed(s);
      }
    }
  }

  private void save(boolean close) {
    click();
    try {
      savedRestart |= session.restartRequired();
      session.save(FabricLoader.getInstance().getGameDir());
      if (savedRestart) {
        savedRestart = false;
        error = "Saved. Restart the game for marked changes.";
        confirmClose = false;
      } else if (close) minecraft.setScreen(parent);
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
        try { session.preview(); } catch (Exception e) { error = e.getMessage(); }
        layout();
        confirmReset = false;
      } else save(true);
    } else if (inside(x, y, width / 2 + 2, cy, 100, 20)) {
      if (confirmReset) confirmReset = false;
      else {
        try {
          session.discard();
          minecraft.setScreen(parent);
        } catch (Exception e) { error = "Could not restore settings: " + e.getMessage(); confirmClose = false; }
      }
    } else if (inside(x, y, width / 2 - 50, cy + 24, 100, 20)) {
      confirmClose = false;
      confirmReset = false;
    }
  }

  protected void keyPressed(char c, int key) {
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
      capture = null;
      return;
    }
    if (confirmClose || confirmReset) {
      if (key == Keyboard.KEY_ESCAPE) {
        confirmClose = confirmReset = false;
      }
      return;
    }
    if (key == Keyboard.KEY_ESCAPE) {
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
      search.focused = true;
      search.selectAll();
      return;
    }
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

  public void render(int mx, int my, float delta) {
    // This wait belongs only to binding capture, never gameplay input.
    if (capture != null && captureModifier != 0 && !Bindings.physical(captureModifier)) {
      capture.value = new JsonPrimitive(captureModifier);
      capture = null;
      captureModifier = 0;
    }
    if (dragging != null) {
      if (Mouse.isButtonDown(0)) drag(Mouse.getX());
      else dragging = null;
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
    if (sideScroll > 0) text("^", origin() + sidebar() - 10, 24, 0xcccccc);
    if (sideScroll < PAGES.size() * 22 - (bottom() - 24))
      text("v", origin() + sidebar() - 10, bottom() - 10, 0xcccccc);
    if (!history.isEmpty()) button("Back", left(), 28, 44, 18, mx, my, true);
    input(search, searchLeft(), 28, right() - 36 - searchLeft(), mx, my,
        !conflictIds.isEmpty() ? "Conflicting bindings" : !relatedIds.isEmpty() ? controls() ? "Related controls" : "Related settings"
        : changedOnly ? "Search changed settings..." : "Search all settings...");
    button("x", right() - 32, 28, 18, 18, mx, my, true);
    if (audio()) {
      int w = span() / 4;
      button("Play / pause", left(), 52, w - 3, 18, mx, my, true);
      button(w < 88 ? "Previous" : "Previous track", left() + w, 52, w - 3, 18, mx, my, true);
      button(w < 88 ? "Next" : "Next track", left() + 2 * w, 52, w - 3, 18, mx, my, true);
      button(w < 88 ? "Reload" : "Reload folders", left() + 3 * w, 52, w - 3, 18, mx, my, true);
      text(fit(local.luke.power.audio.AudioController.status(), span()), left(), 75, 0xaaaaaa);
    }
    String tip = "", hoverId = "";
    clip(left() - 2, top(), span() + 8, bottom() - top());
    for (int index = 0; index < rows.size(); index++) {
      Row row = rows.get(index);
      int y = top() + row.y - (int) scroll;
      if (y + row.height < top() || y >= bottom()) continue;
      if (row.setting == null) {
        text(
            (collapsed.contains(page + "/" + row.group) && search.text().isBlank() ? "> " : "v ")
                + row.group,
            left(),
            y + 8,
            0xffffff);
        fill(left(), y + 21, right() - 14, y + 22, 0x50555555);
        continue;
      }
      Setting s = row.setting;
      boolean hover = inside(mx, my, left(), y, span(), row.height) && my >= top() && my < bottom();
      if (hover) fill(left() - 2, y, right() - 12, y + row.height, 0x60000000);
      boolean narrow = row.height == 40;
      int cy = y + (narrow ? 16 : 2),
          controlLeft = mainControlLeft(row);
      int textWidth = narrow ? span() - 4 : controlLeft(row) - left() - 13;
      String label = s.label + (s.restart ? " *" : "");
      text(
          fit(label, textWidth),
          left() + 3,
          y + (narrow ? 3 : 8),
          s.changed() ? 0xffdd88 : 0xdddddd);
      boolean numeric = s.kind == Setting.Kind.INTEGER || s.kind == Setting.Kind.DECIMAL;
      int valueEnd = right() - (numeric ? 62 : 36);
      if (numeric) slider(value(s), controlLeft, cy, valueEnd - controlLeft, mx, my,
          (s.value.getAsDouble() - s.min) / Math.max(0.000001, s.max - s.min));
      else button(fit(value(s), Math.max(5, valueEnd - controlLeft - 8)),
          controlLeft, cy, valueEnd - controlLeft, 18, mx, my, true);
      if (numeric) button("...", right() - 60, cy, 22, 18, mx, my, true);
      button("R", right() - 34, cy, 20, 18, mx, my, !s.value.equals(s.defaultValue));
      if (controlLeft > controlLeft(row)) iconButton(soundPreview(s) ? "speaker" : s.kind == Setting.Kind.KEY ? "settings" : "controls", controlLeft(row), cy, 20, mx, my, true);
      List<Setting> conflicts = conflicts(s);
      if (!conflicts.isEmpty()) text("!", controlLeft(row) - 7, cy + 5, 0xff8855);
      if (hover) {
        hoverId = s.id;
        if (mx >= right() - 34) { tip = "Reset to " + s.display(s.defaultValue); hoverId += ".reset"; }
        else if (!conflicts.isEmpty() && inside(mx, my, controlLeft(row) - 12, cy, 12, 18)) {
          tip = conflictTip(s, conflicts); hoverId += ".conflicts";
        } else if (mx >= controlLeft(row) && mx < controlLeft) {
          tip = soundPreview(s) ? s.id.startsWith("audio.sound.music:") ? "Preview track; click again to stop" : "Preview sound" : s.kind == Setting.Kind.KEY ? "Related settings" : "Related controls"; hoverId += ".related";
        } else tip = s.description + (s.restart ? "\nRestart required." : "");
      }
    }
    unclip();
    if (!rows.isEmpty()) {
      Row last = rows.get(rows.size() - 1);
      int total = last.y + last.height;
      int track = bottom() - top();
      if (total > track) {
        int thumb = Math.max(12, track * track / total);
        int y = top() + (int) (scroll / (total - track) * (track - thumb));
        fill(right() - 8, top(), right() - 5, bottom(), 0xff222222);
        fill(right() - 8, y, right() - 5, y + thumb, 0xff777777);
      }
    }
    if (!error.isEmpty()) text(fit(error, uiWidth() - 16), origin() + 8, footerY() - 12, 0xffbb88);
    button(filtered() ? "Reset listed..." : "Reset page...", origin() + 8, footerY(), 88, 20, mx, my, true);
    int changesEnd = footerY() < height - 28 ? right() - 8 : (controls() ? disabledX() : actionX(0)) - 4;
    if (session.changes() > 0 || changedOnly) {
      boolean hover = inside(mx, my, origin() + 102, footerY(), changesEnd - origin() - 102, 20);
      text(fit(session.changes() + (session.changes() == 1 ? " unsaved change" : " unsaved changes"), changesEnd - origin() - 104),
          origin() + 102, footerY() + 6, hover || changedOnly ? 0xffffa0 : 0xffdd88);
    }
    if (controls()) button(showDisabled ? "Hide Disabled" : "Show Disabled", disabledX(), height - 28, 90, 20, mx, my, true);
    button("Cancel", actionX(0), height - 28, actionWidth(), 20, mx, my, true);
    button("Apply", actionX(1), height - 28, actionWidth(), 20, mx, my, session.changes() > 0);
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
  }
}
