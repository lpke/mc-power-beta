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

  private final Screen parent;
  private ConfigSession session;
  private final TextInput search = new TextInput("", 128);
  private final Set<String> collapsed = new HashSet<>();
  private final ItemRenderer icons = new ItemRenderer();
  private String page = "General", error = "";
  private double scroll, sideScroll;
  private Setting capture;
  private int captureModifier;
  private boolean confirmClose, confirmReset, savedRestart;
  private int selected = -1, hoverTicks;
  private String lastHover = "";
  private List<Row> rows = List.of();

  public PowerOptionsScreen(Screen parent) {
    this.parent = parent;
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
    Keyboard.enableRepeatEvents(false);
  }

  public ConfigSession session() {
    return session;
  }

  private int sidebar() {
    return Math.max(90, Math.min(155, width / 4));
  }

  private int left() {
    return sidebar() + 16;
  }

  private int span() {
    return width - left() - 14;
  }

  private boolean audio() {
    return page.equals("Audio") && search.text.isBlank();
  }

  private boolean video() {
    return page.equals("Video") && search.text.isBlank();
  }

  private int top() {
    return audio() ? 88 : video() ? 78 : 52;
  }

  private int bottom() {
    return height - 44;
  }

  private void layout() {
    if (session == null) return;
    String query = search.text.strip().toLowerCase(Locale.ROOT);
    Map<String, List<Setting>> groups = new LinkedHashMap<>();
    for (Setting s : session.settings())
      if (query.isEmpty()
          ? s.page.equals(page)
          : (s.label + " " + s.description + " " + s.group + " " + s.page)
              .toLowerCase(Locale.ROOT)
              .contains(query))
        groups
            .computeIfAbsent(
                query.isEmpty() ? s.group : s.page + " / " + s.group, k -> new ArrayList<>())
            .add(s);
    List<Row> next = new ArrayList<>();
    int y = 0, rowHeight = span() < 340 ? 40 : 24;
    for (var group : groups.entrySet()) {
      next.add(new Row(group.getKey(), null, y, 24));
      y += 24;
      if (query.isEmpty() && collapsed.contains(page + "/" + group.getKey())) continue;
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
  }

  public void onMouseEvent() {
    super.onMouseEvent();
    int wheel = Mouse.getEventDWheel();
    if (wheel == 0 || capture != null || confirmClose || confirmReset) return;
    int x = Mouse.getEventX() * width / minecraft.displayWidth;
    if (x < sidebar() + 8) sideScroll -= Math.signum(wheel) * 44;
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
    if (inside(x, y, left(), 28, span() - 22, 18)) {
      search.focused = true;
      if (button == 1) {
        search.text = "";
        search.selectAll();
        scroll = 0;
        layout();
      }
      return;
    }
    if (inside(x, y, width - 32, 28, 18, 18)) {
      search.text = "";
      search.selectAll();
      scroll = 0;
      layout();
      return;
    }
    search.focused = false;
    if (button == 0 && y >= 24 && y < bottom() && x < sidebar()) {
      int index = (int) ((y - 24 + sideScroll) / 22);
      if (index >= 0 && index < PAGES.size()) {
        page = PAGES.get(index);
        search.text = "";
        search.selectAll();
        scroll = 0;
        selected = -1;
        layout();
      }
      return;
    }
    if (audio() && button == 0 && y >= 52 && y < 72) {
      int index = (x - left()) / Math.max(1, span() / 3);
      if (x >= left() && index == 0) local.luke.power.audio.AudioController.togglePause();
      else if (index == 1) local.luke.power.audio.AudioController.next();
      else if (index == 2) local.luke.power.audio.AudioController.reload();
      return;
    }
    if (video() && button == 0 && inside(x, y, left(), 52, span(), 20)) {
      minecraft.setScreen(new net.minecraft.client.gui.screen.pack.PackScreen(this));
      return;
    }
    if (y >= top() && y < bottom() && x >= left() && x < width - 10) {
      for (int i = 0; i < rows.size(); i++) {
        Row r = rows.get(i);
        int ry = top() + r.y - (int) scroll;
        if (y < ry || y >= ry + r.height) continue;
        if (r.setting == null) {
          if (button == 0) {
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
        if (x >= width - 34) {
          s.reset();
          session.link(s);
          return;
        }
        if ((s.kind == Setting.Kind.INTEGER || s.kind == Setting.Kind.DECIMAL) && x >= width - 60) {
          minecraft.setScreen(new ValueScreen(this, s));
          return;
        }
        activate(s, button == 1 ? -1 : 1);
        return;
      }
    }
    if (button != 0) return;
    if (inside(x, y, 8, height - 28, 88, 20)) {
      confirmReset = true;
      return;
    }
    if (inside(x, y, width - 154, height - 28, 70, 20)) {
      if (session.changes() > 0) confirmClose = true;
      else minecraft.setScreen(parent);
      return;
    }
    if (inside(x, y, width - 80, height - 28, 72, 20)) saveAndClose();
  }

  private void activate(Setting s, int direction) {
    error = "";
    switch (s.kind) {
      case KEY -> { capture = s; captureModifier = 0; }
      case TEXT, LIST -> {
        if (s.id.equals("audio.musicDirectories") || s.id.equals("audio.menuDirectories"))
          minecraft.setScreen(new FolderScreen(this, s));
        else minecraft.setScreen(new ValueScreen(this, s));
      }
      default -> {
        s.cycle(direction);
        session.link(s);
      }
    }
  }

  private void saveAndClose() {
    try {
      savedRestart |= session.restartRequired();
      session.save(FabricLoader.getInstance().getGameDir());
      if (savedRestart) {
        savedRestart = false;
        error = "Saved. Restart the game for marked changes.";
        confirmClose = false;
      } else minecraft.setScreen(parent);
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
        for (Setting s : session.settings()) if (s.page.equals(page)) s.reset();
        confirmReset = false;
      } else saveAndClose();
    } else if (inside(x, y, width / 2 + 2, cy, 100, 20)) {
      if (confirmReset) confirmReset = false;
      else {
        session.discard();
        minecraft.setScreen(parent);
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
      String before = search.text;
      search.key(c, key);
      if (!before.equals(search.text)) {
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

  private boolean conflict(Setting s) {
    return s.kind == Setting.Kind.KEY
        && s.value.getAsInt() != 0
        && session.settings().stream()
            .anyMatch(v -> v != s && v.kind == Setting.Kind.KEY && Chord.decode(v.value.getAsInt()).key() == Chord.decode(s.value.getAsInt()).key());
  }

  public void render(int mx, int my, float delta) {
    // This wait belongs only to binding capture, never gameplay input.
    if (capture != null && captureModifier != 0 && !Bindings.physical(captureModifier)) {
      capture.value = new JsonPrimitive(captureModifier);
      capture = null;
      captureModifier = 0;
    }
    renderBackground();
    fill(0, 0, width, 22, 0x70000000);
    fill(0, height - 38, width, height, 0x90000000);
    drawCenteredTextWithShadow(textRenderer, "Options", width / 2, 7, 0xffffff);
    if (session == null) {
      text(error, 10, 45, 0xff8888);
      button("Back", width / 2 - 50, height - 28, 100, 20, mx, my, true);
      return;
    }
    fill(sidebar() + 4, 24, sidebar() + 7, bottom(), 0x90404040);
    fill(sidebar() + 7, 24, sidebar() + 8, bottom(), 0x90000000);
    clip(0, 24, sidebar() + 2, bottom() - 24);
    for (int i = 0; i < PAGES.size(); i++) {
      int y = 24 + i * 22 - (int) sideScroll;
      boolean hover = inside(mx, my, 0, y, sidebar(), 22);
      if (PAGES.get(i).equals(page) && search.text.isBlank())
        fill(0, y, sidebar(), y + 21, 0x50333333);
      GL11.glPushMatrix();
      GL11.glColor4f(1, 1, 1, 1);
      icons.method_1487(
          textRenderer, minecraft.textureManager, new ItemStack(ICONS[i], 1, 0), 7, y + 2);
      GL11.glPopMatrix();
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      text(
          fit(PAGES.get(i), sidebar() - 31),
          28,
          y + 7,
          hover ? 0xffffa0 : PAGES.get(i).equals(page) ? 0xffffff : 0xaaaaaa);
    }
    unclip();
    if (sideScroll > 0) text("^", sidebar() - 10, 24, 0xcccccc);
    if (sideScroll < PAGES.size() * 22 - (bottom() - 24))
      text("v", sidebar() - 10, bottom() - 10, 0xcccccc);
    input(search, left(), 28, span() - 22, mx, my, "Search all settings...");
    button("x", width - 32, 28, 18, 18, mx, my, true);
    if (audio()) {
      int w = span() / 3;
      button("Play / pause", left(), 52, w - 3, 18, mx, my, true);
      button("Next track", left() + w, 52, w - 3, 18, mx, my, true);
      button("Reload folders", left() + 2 * w, 52, w - 3, 18, mx, my, true);
      text(fit(local.luke.power.audio.AudioController.status(), span()), left(), 75, 0xaaaaaa);
    }
    if (video()) button("Texture packs...", left(), 52, span(), 20, mx, my, true);
    String tip = "", hoverId = "";
    clip(left() - 2, top(), span() + 8, bottom() - top());
    for (int index = 0; index < rows.size(); index++) {
      Row row = rows.get(index);
      int y = top() + row.y - (int) scroll;
      if (y + row.height < top() || y >= bottom()) continue;
      if (row.setting == null) {
        text(
            (collapsed.contains(page + "/" + row.group) && search.text.isBlank() ? "> " : "v ")
                + row.group,
            left(),
            y + 8,
            0xffffff);
        fill(left(), y + 21, width - 14, y + 22, 0x50555555);
        continue;
      }
      Setting s = row.setting;
      boolean hover = inside(mx, my, left(), y, span(), row.height) && my >= top() && my < bottom();
      if (hover || selected == index) fill(left() - 2, y, width - 12, y + row.height, 0x30333333);
      boolean narrow = row.height == 40;
      int cy = y + (narrow ? 16 : 2),
          controlLeft = narrow ? left() + 4 : Math.max(left() + 100, width - 164);
      int textWidth = narrow ? span() - 4 : controlLeft - left() - 5;
      String label = s.label + (s.restart ? " *" : "");
      text(
          fit(label, textWidth),
          left() + 3,
          y + (narrow ? 3 : 8),
          s.changed() ? 0xffdd88 : 0xdddddd);
      boolean numeric = s.kind == Setting.Kind.INTEGER || s.kind == Setting.Kind.DECIMAL;
      int valueEnd = width - (numeric ? 62 : 36);
      button(
          fit(value(s), Math.max(5, valueEnd - controlLeft - 8)),
          controlLeft,
          cy,
          valueEnd - controlLeft,
          18,
          mx,
          my,
          true);
      if (numeric) button("...", width - 60, cy, 22, 18, mx, my, true);
      button("R", width - 34, cy, 20, 18, mx, my, !s.value.equals(s.defaultValue));
      if (conflict(s)) text("!", controlLeft - 6, cy + 5, 0xff8855);
      if (hover) {
        hoverId = s.id;
        tip =
            mx >= width - 34
                ? "Reset to " + s.display(s.defaultValue)
                : s.label
                    + "\n"
                    + s.description
                    + (s.restart ? "\nRestart required." : "")
                    + (conflict(s) ? "\nShared key: the matching binding with more modifiers takes priority." : "")
                    + "\nLeft click increases; right click decreases.";
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
        fill(width - 8, top(), width - 5, bottom(), 0xff222222);
        fill(width - 8, y, width - 5, y + thumb, 0xff777777);
      }
    }
    text(
        fit(
            error.isEmpty()
                ? (session.changes() == 0 ? "" : session.changes() + " unsaved changes")
                : error,
            width - 16),
        8,
        height - 40,
        error.isEmpty() ? 0xaaaaaa : 0xffbb88);
    button("Reset page...", 8, height - 28, 88, 20, mx, my, true);
    button("Cancel", width - 154, height - 28, 70, 20, mx, my, true);
    button("Done", width - 80, height - 28, 72, 20, mx, my, true);
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
              : confirmReset ? "Reset " + page + " to Power Beta defaults?" : "Save your changes?";
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
