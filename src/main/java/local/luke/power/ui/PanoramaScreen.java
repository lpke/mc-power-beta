package local.luke.power.ui;

import com.google.gson.JsonPrimitive;
import java.util.*;
import local.luke.power.config.Setting;
import local.luke.power.input.PanoramaCatalog;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Searchable version picker; selecting a row remains a local draft until Done. */
public final class PanoramaScreen extends UiScreen {
  private final PowerOptionsScreen parent;
  private final Setting setting;
  private final TextInput search = new TextInput("", 64);
  private final ScrollBar bar = new ScrollBar();
  private String selected;
  private double scroll;

  public PanoramaScreen(PowerOptionsScreen parent, Setting setting) {
    this.parent = parent;
    this.setting = setting;
    selected = setting.value.getAsString();
  }
  public void init() { Keyboard.enableRepeatEvents(true); }
  public void removed() { Keyboard.enableRepeatEvents(false); }
  private int left() { return Math.max(12, (width - 460) / 2); }
  private int span() { return width - 2 * left(); }
  private List<PanoramaCatalog.Entry> entries() {
    String query = search.text().strip().toLowerCase(Locale.ROOT);
    return PanoramaCatalog.entries().stream().filter(e -> e.label().toLowerCase(Locale.ROOT).contains(query)).toList();
  }
  private ScrollBar.Track track() { return new ScrollBar.Track(width - left() - 6, 72, height - 110, entries().size() * 24, scroll); }
  private void done() {
    setting.value = new JsonPrimitive(selected);
    parent.changed(setting);
    minecraft.setScreen(parent);
  }
  protected void mouseClicked(int x, int y, int button) {
    if (button != 0) return;
    search.focused = inside(x, y, left(), 44, span(), 18);
    if (search.focused) { search.click(x, button); return; }
    if (bar.press(track(), x, y)) return;
    if (inside(x, y, left(), 72, span() - 12, height - 110)) {
      int row = (int)((y - 72 + scroll) / 24);
      List<PanoramaCatalog.Entry> choices = entries();
      int rowY = 72 + row * 24 - (int)scroll;
      if (row >= 0 && row < choices.size() && rowY >= 72 && rowY + 20 <= height - 38 && y < rowY + 20)
        selected = choices.get(row).id();
    }
    if (inside(x, y, width / 2 - 102, height - 28, 100, 20)) done();
    if (inside(x, y, width / 2 + 2, height - 28, 100, 20)) minecraft.setScreen(parent);
  }
  protected void keyPressed(char c, int key) {
    if (key == Keyboard.KEY_ESCAPE) { minecraft.setScreen(parent); return; }
    if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { done(); return; }
    String before = search.text(); search.key(c, key);
    if (!before.equals(search.text())) scroll = 0;
  }
  public void render(int x, int y, float delta) {
    renderBackground();
    drawCenteredTextWithShadow(textRenderer, "Title panorama", width / 2, 18, 0xffffff);
    input(search, left(), 44, span(), x, y, "Search versions");
    int wheel = Mouse.getDWheel();
    scroll = Math.max(0, Math.min(track().maximum(), scroll - Integer.signum(wheel) * 48));
    scroll = bar.drag(track(), y, Mouse.isButtonDown(0));
    List<PanoramaCatalog.Entry> choices = entries();
    for (int i = 0; i < choices.size(); i++) {
      int rowY = 72 + i * 24 - (int)scroll;
      if (rowY < 72 || rowY + 20 > height - 38) continue;
      var entry = choices.get(i);
      button((entry.id().equals(selected) ? "> " : "") + entry.label(), left(), rowY, span() - 12, 20, x, y, true);
    }
    bar.render(this, track());
    button("Done", width / 2 - 102, height - 28, 100, 20, x, y, true);
    button("Cancel", width / 2 + 2, height - 28, 100, 20, x, y, true);
  }
}
