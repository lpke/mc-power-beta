package local.luke.power.ui;

import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntConsumer;
import local.luke.power.config.DefaultBases;
import local.luke.power.config.Setting;
import local.luke.power.config.Tooltips;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Modal choice lists and context actions share row geometry, styling and ownership. */
final class SettingsDropdown {
  private final ScrollBar bar = new ScrollBar();
  private final HoverDelay hover = new HoverDelay();
  private List<String> labels = List.of(), help = List.of();
  private List<Boolean> enabled = List.of();
  private IntConsumer accepted;
  private Object owner;
  private int selected, focus, defaultIndex, x, y, width, rows, rowHeight = 18, viewportHeight;
  private int anchorLeft, anchorTop, anchorWidth, screenWidth, screenHeight;
  private boolean context, keyboardFocus;
  private double scroll;
  private PopupLayout.Rows layout;

  static boolean accepts(Setting setting) {
    return setting.kind == Setting.Kind.CHOICE || setting.kind == Setting.Kind.BOOLEAN && setting.choices.equals(List.of("Hold", "Toggle"));
  }
  private static int index(Setting setting, com.google.gson.JsonElement value) {
    return setting.kind == Setting.Kind.BOOLEAN ? value.getAsBoolean() ? 1 : 0 : value.getAsInt();
  }

  boolean active() { return accepted != null; }
  boolean editing(Object candidate) { return active() && Objects.equals(owner, candidate); }
  boolean editing(Setting setting) { return editing((Object)setting); }

  void open(Setting setting, int left, int top, int span, int screenWidth, int screenHeight,
      Runnable changed) {
    open(setting, left, top, span, screenWidth, screenHeight, () -> {}, changed);
  }
  void open(Setting setting, int left, int top, int span, int screenWidth, int screenHeight,
      Runnable beforeChange, Runnable changed) {
    List<String> hints = java.util.stream.IntStream.range(0, setting.choices.size())
        .mapToObj(i -> Tooltips.choice(setting, i)).toList();
    open(setting, setting.choices, hints, index(setting, setting.value),
        index(setting, DefaultBases.value(setting.id, setting.powerBetaDefault)),
        left, top, span, 18, screenWidth, screenHeight, chosen -> {
          if (chosen == index(setting, setting.value)) return;
          beforeChange.run();
          setting.value = setting.kind == Setting.Kind.BOOLEAN ? new JsonPrimitive(chosen == 1) : new JsonPrimitive(chosen);
          changed.run();
        });
  }

  void open(List<String> options, List<String> hints, int current, int reset,
      int left, int top, int span, int screenWidth, int screenHeight, IntConsumer changed) {
    open(null, options, hints, current, reset, left, top, span, 18, screenWidth, screenHeight, changed);
  }

  void open(Object owner, List<String> options, List<String> hints, int current, int reset,
      int left, int top, int span, int rowHeight, int screenWidth, int screenHeight, IntConsumer changed) {
    close();
    if (options.isEmpty()) return;
    labels = List.copyOf(options); help = List.copyOf(hints);
    enabled = java.util.Collections.nCopies(options.size(), true);
    this.owner = owner;
    selected = focus = Math.max(-1, Math.min(options.size() - 1, current)); defaultIndex = reset;
    this.rowHeight = rowHeight;
    anchorLeft = left; anchorTop = top; anchorWidth = span;
    this.screenWidth = screenWidth; this.screenHeight = screenHeight;
    layout = new PopupLayout.Rows(labels.size(), rowHeight, Set.of());
    accepted = Objects.requireNonNull(changed);
    position(); reveal();
  }

  void openActions(UiScreen screen, List<String> options, List<Boolean> available,
      int left, int top, int screenWidth, int screenHeight, IntConsumer action) {
    openActions(null, screen, options, available, left, top, screenWidth, screenHeight, action);
  }

  void openActions(Object owner, UiScreen screen, List<String> options, List<Boolean> available,
      int left, int top, int screenWidth, int screenHeight, IntConsumer action) {
    if (available.size() != options.size()) throw new IllegalArgumentException("Every action needs availability");
    int span = options.stream().mapToInt(screen::textWidth).max().orElse(120) + 2 * PopupStyle.PADDING;
    open(owner, options, List.of(), -1, -1, left, top, span, 18, screenWidth, screenHeight, action);
    if (!active()) return;
    enabled = List.copyOf(available); context = true;
    position();
  }

  /** Add visual dividers before option indices without changing callback indices. */
  void separatorsBefore(int... indices) {
    if (!active()) return;
    Set<Integer> separators = java.util.Arrays.stream(indices).boxed().collect(java.util.stream.Collectors.toSet());
    layout = new PopupLayout.Rows(labels.size(), rowHeight, separators);
    position(); reveal();
  }

  /** Current and Default markers also apply to menus with a leading editor action. */
  void markers(int current, int reset) {
    if (!active()) return;
    selected = current >= 0 && current < labels.size() ? current : -1;
    defaultIndex = reset >= 0 && reset < labels.size() ? reset : -1;
    if (!keyboardFocus && !context) { focus = selected; reveal(); }
  }

  private void position() {
    PopupLayout.Bounds bounds = context
        ? PopupLayout.context(anchorLeft, anchorTop, anchorWidth, layout.contentHeight(), screenWidth, screenHeight)
        : PopupLayout.dropdown(anchorLeft, anchorTop, anchorWidth, rowHeight, layout.contentHeight(), screenWidth, screenHeight);
    x = bounds.x(); y = bounds.y(); width = bounds.width(); viewportHeight = bounds.height();
    rows = Math.max(1, (viewportHeight + rowHeight - 1) / rowHeight);
    scroll = Math.max(0, Math.min(track().maximum(), scroll));
  }

  void close() {
    accepted = null; owner = null; context = keyboardFocus = false; scroll = 0;
    bar.release(); hover.clear();
  }
  void tick() { if (active()) hover.tick(); }
  private ScrollBar.Track track() { return new ScrollBar.Track(x + width - 5, y, viewportHeight, layout.contentHeight(), scroll); }
  private int rowWidth() { return width - (track().visible() ? 10 : 0); }
  private void reveal() {
    if (focus >= 0) {
      if (layout.top(focus) < scroll) scroll = layout.top(focus);
      else if (layout.top(focus) + rowHeight > scroll + viewportHeight)
        scroll = layout.top(focus) + rowHeight - viewportHeight;
    }
    scroll = Math.max(0, Math.min(track().maximum(), scroll));
  }
  boolean wheel(int amount) {
    if (!active()) return false;
    if (amount != 0) {
      scroll = Math.max(0, Math.min(track().maximum(), scroll - Math.signum(amount) * rowHeight * 2));
      keyboardFocus = false; hover.clear();
    }
    return true;
  }
  private void moveFocus(int direction) {
    int next = focus < 0 ? direction > 0 ? 0 : labels.size() - 1 : Math.floorMod(focus + direction, labels.size());
    for (int i = 0; i < labels.size(); i++, next = Math.floorMod(next + direction, labels.size()))
      if (enabled.get(next)) { focus = next; break; }
    keyboardFocus = true; reveal(); hover.clear();
  }
  boolean key(UiScreen screen, int key) {
    if (!active()) return false;
    switch (key) {
      case Keyboard.KEY_ESCAPE -> close();
      case Keyboard.KEY_UP -> moveFocus(-1);
      case Keyboard.KEY_DOWN -> moveFocus(1);
      case Keyboard.KEY_HOME -> { focus = -1; moveFocus(1); }
      case Keyboard.KEY_END -> { focus = -1; moveFocus(-1); }
      case Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER, Keyboard.KEY_SPACE -> select(screen, focus);
      default -> {}
    }
    return true;
  }
  private void select(UiScreen screen, int index) {
    if (index < 0 || index >= labels.size() || !enabled.get(index)) return;
    IntConsumer action = accepted; close(); screen.click(); action.accept(index);
  }
  boolean press(UiScreen screen, int mx, int my, int button) {
    if (!active()) return false;
    if (button != 0 && button != 1) return true;
    if (button == 0 && bar.press(track(), mx, my)) { scroll = bar.drag(track(), my, true); return true; }
    if (screen.inside(mx, my, x, y, width, viewportHeight)) {
      if (button == 0 && mx < x + rowWidth()) select(screen, layout.indexAt((int)scroll + my - y));
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
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_SCISSOR_BIT);
    try {
      GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING); GL11.glDepthMask(false);
      PopupStyle.background(screen, x, y, width, viewportHeight, !context);
      int rowWidth = rowWidth(), hovered = -1;
      screen.clip(x, y, width, viewportHeight);
      for (int i = 0; i < labels.size(); i++) {
        int top = y + layout.top(i) - (int)scroll;
        if (top >= y + viewportHeight + PopupLayout.SEPARATOR_HEIGHT) break;
        if (layout.separators().contains(i)) PopupStyle.separator(screen, x, top - PopupLayout.SEPARATOR_HEIGHT, width);
        if (top + rowHeight <= y || top >= y + viewportHeight) continue;
        boolean over = screen.inside(mx, my, x, Math.max(y, top), rowWidth,
            Math.min(y + viewportHeight, top + rowHeight) - Math.max(y, top));
        if (over) hovered = i;
        int reserve = i == defaultIndex ? screen.textWidth("Default") + 3 : 0;
        PopupStyle.row(screen, labels.get(i), x, top, rowWidth, rowHeight, over ? mx : -1000, over ? my : -1000,
            enabled.get(i), i == selected, keyboardFocus && i == focus, reserve);
        if (i == defaultIndex) screen.text("Default", x + rowWidth - screen.textWidth("Default") - PopupStyle.PADDING,
            top + (rowHeight - 8) / 2, 0x888888);
      }
      screen.unclip(); bar.render(screen, track());
      String tip = hovered >= 0 && hovered < help.size() ? help.get(hovered) : "";
      if (!tip.isBlank() && hovered == defaultIndex) tip = "Default - " + tip;
      if (hover.ready(hovered < 0 || tip.isBlank() ? "" : Integer.toString(hovered))) screen.tooltip(tip, mx, my);
    } finally { GL11.glPopAttrib(); }
  }
}
