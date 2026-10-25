package local.luke.power.ui;

import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.function.IntConsumer;
import local.luke.power.config.DefaultBases;
import local.luke.power.config.Setting;
import local.luke.power.config.Tooltips;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Modal choice list shared by settings and editor selectors. */
final class SettingsDropdown {
  private final ScrollBar bar = new ScrollBar();
  private final HoverDelay hover = new HoverDelay();
  private List<String> labels = List.of(), help = List.of();
  private IntConsumer accepted;
  private int selected, focus, defaultIndex, x, y, width, rows;
  private double scroll;

  static boolean accepts(Setting setting) {
    return setting.kind == Setting.Kind.CHOICE || setting.kind == Setting.Kind.BOOLEAN && setting.choices.equals(List.of("Hold", "Toggle"));
  }
  private static int index(Setting setting, com.google.gson.JsonElement value) {
    return setting.kind == Setting.Kind.BOOLEAN ? value.getAsBoolean() ? 1 : 0 : value.getAsInt();
  }

  boolean active() { return accepted != null; }
  boolean editing(Setting setting) { return active() && owner == setting; }
  private Setting owner;

  void open(Setting setting, int left, int top, int span, int screenWidth, int screenHeight,
      Runnable changed) {
    open(setting, left, top, span, screenWidth, screenHeight, () -> {}, changed);
  }
  void open(Setting setting, int left, int top, int span, int screenWidth, int screenHeight,
      Runnable beforeChange, Runnable changed) {
    List<String> hints = java.util.stream.IntStream.range(0, setting.choices.size())
        .mapToObj(i -> Tooltips.choice(setting, i)).toList();
    open(setting.choices, hints, index(setting, setting.value),
        index(setting, DefaultBases.value(setting.id, setting.powerBetaDefault)),
        left, top, span, screenWidth, screenHeight, index -> {
          if (index == index(setting, setting.value)) return;
          beforeChange.run();
          setting.value = setting.kind == Setting.Kind.BOOLEAN ? new JsonPrimitive(index == 1) : new JsonPrimitive(index); changed.run();
        });
    owner = setting;
  }

  void open(List<String> options, List<String> hints, int current, int reset,
      int left, int top, int span, int screenWidth, int screenHeight, IntConsumer changed) {
    close();
    if (options.isEmpty()) return;
    labels = List.copyOf(options); help = List.copyOf(hints);
    selected = focus = Math.max(0, Math.min(options.size() - 1, current)); defaultIndex = reset;
    width = Math.max(40, Math.min(Math.max(span, 150), screenWidth - 12));
    x = Math.max(6, Math.min(left, screenWidth - width - 6));
    int below = Math.max(0, (screenHeight - top - 25) / 20), above = Math.max(0, (top - 6) / 20);
    rows = Math.max(1, Math.min(options.size(), Math.max(below, above)));
    y = below >= rows ? top + 21 : Math.max(4, top - rows * 20 - 3);
    accepted = changed;
    reveal();
  }

  void close() { accepted = null; owner = null; bar.release(); hover.clear(); }
  void tick() { if (active()) hover.tick(); }
  private ScrollBar.Track track() { return new ScrollBar.Track(x + width - 5, y, rows * 20, labels.size() * 20, scroll); }
  private void reveal() {
    if (focus * 20 < scroll) scroll = focus * 20;
    else if ((focus + 1) * 20 > scroll + rows * 20) scroll = (focus + 1 - rows) * 20;
    scroll = Math.max(0, Math.min(track().maximum(), scroll));
  }
  boolean wheel(int amount) {
    if (!active()) return false;
    if (amount != 0) { scroll = Math.max(0, Math.min(track().maximum(), scroll - Math.signum(amount) * 40)); hover.clear(); }
    return true;
  }
  boolean key(UiScreen screen, int key) {
    if (!active()) return false;
    switch (key) {
      case Keyboard.KEY_ESCAPE -> close();
      case Keyboard.KEY_UP, Keyboard.KEY_DOWN -> {
        focus = Math.floorMod(focus + (key == Keyboard.KEY_UP ? -1 : 1), labels.size()); reveal(); hover.clear();
      }
      case Keyboard.KEY_HOME -> { focus = 0; reveal(); }
      case Keyboard.KEY_END -> { focus = labels.size() - 1; reveal(); }
      case Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER, Keyboard.KEY_SPACE -> select(screen, focus);
      default -> {}
    }
    return true;
  }
  private void select(UiScreen screen, int index) {
    IntConsumer action = accepted; close(); screen.click(); action.accept(index);
  }
  boolean press(UiScreen screen, int mx, int my, int button) {
    if (!active()) return false;
    if (button != 0 && button != 1) return true;
    if (button == 0 && bar.press(track(), mx, my)) { scroll = bar.drag(track(), my, true); return true; }
    if (screen.inside(mx, my, x, y, width, rows * 20)) {
      int index = ((int)scroll + my - y) / 20;
      if (index >= 0 && index < labels.size()) select(screen, index);
    } else close();
    return true;
  }
  void release(int my, int button) {
    if (active() && button == 0) { scroll = bar.drag(track(), my, true); bar.release(); }
  }
  void render(UiScreen screen, int mx, int my) {
    if (!active()) return;
    if (!Display.isActive()) { close(); return; }
    scroll = bar.drag(track(), my, true);
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    try {
      GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING); GL11.glDepthMask(false);
      screen.rectangle(x - 2, y - 2, x + width + 2, y + rows * 20 + 2, 0xff777777);
      screen.rectangle(x - 1, y - 1, x + width + 1, y + rows * 20 + 1, 0xff181818);
      int rowWidth = width - (track().visible() ? 10 : 0), hovered = -1;
      screen.clip(x, y, width, rows * 20);
      for (int i = (int)scroll / 20; i < labels.size(); i++) {
        int top = y + i * 20 - (int)scroll;
        if (top >= y + rows * 20) break;
        boolean over = screen.inside(mx, my, x, Math.max(y, top), rowWidth, Math.min(y + rows * 20, top + 20) - Math.max(y, top));
        if (over) hovered = i;
        int reserve = i == defaultIndex ? screen.textWidth("Default") + 12 : 0;
        WaypointSetDropdown.row(screen, labels.get(i), x, top,
            rowWidth, mx, my, i == selected, i == focus, reserve);
        if (i == defaultIndex) screen.text("Default", x + rowWidth - screen.textWidth("Default") - 7, top + 6, 0x888888);
      }
      screen.unclip(); bar.render(screen, track());
      String tip = hovered >= 0 && hovered < help.size() ? help.get(hovered) : "";
      if (!tip.isBlank() && hovered == defaultIndex) tip = "Default - " + tip;
      if (hover.ready(hovered < 0 || tip.isBlank() ? "" : Integer.toString(hovered))) screen.tooltip(tip, mx, my);
    } finally { GL11.glPopAttrib(); }
  }
}
