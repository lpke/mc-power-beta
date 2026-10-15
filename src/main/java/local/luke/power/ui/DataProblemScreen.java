package local.luke.power.ui;

import java.util.*;

/** A readable error instead of replacing data or entering an unsafe world. */
public final class DataProblemScreen extends UiScreen {
  private final String problem;
  private final boolean configuration;
  public DataProblemScreen(String problem, boolean configuration) { this.problem = problem; this.configuration = configuration; }
  private final ScrollBar bar = new ScrollBar();
  private List<String> lines = List.of();
  private double scroll;
  private int panelWidth() { return Math.max(32, Math.min(520, width - 40)); }
  private int left() { return (width - panelWidth()) / 2; }
  private ScrollBar.Track track() { return new ScrollBar.Track(left() + panelWidth() + 6, 44, Math.max(0, height - 86), lines.size() * 12, scroll); }
  public void init() {
    lines = new ArrayList<>(wrap(problem, panelWidth()));
    lines.add("");
    lines.addAll(wrap("Original files are preserved.", panelWidth()));
    lines.add("");
    lines.addAll(wrap(configuration ? "Close the game, repair the file or restore a backup, then restart."
        : "Repair the world data or use a compatible release, then reopen it.", panelWidth()));
    scroll = Math.max(0, Math.min(track().maximum(), scroll));
  }
  public void render(int mx, int my, float delta) {
    renderBackground();
    centeredText(fit(configuration ? "Settings need attention" : "World could not be opened", width - 24), 20, 0xffffff);
    scroll = bar.drag(track(), my, org.lwjgl.input.Mouse.isButtonDown(0));
    clip(left(), 44, panelWidth(), track().height());
    int y = 44 - (int) scroll;
    for (String line : lines) { text(line, left(), y, 0xdddddd); y += 12; }
    unclip(); bar.render(this, track());
    button(configuration ? "Close game" : "Back", width / 2 - 60, height - 30, 120, 20, mx, my, true);
  }
  public void onMouseEvent() {
    super.onMouseEvent();
    scroll = Math.max(0, Math.min(track().maximum(), scroll - Math.signum(org.lwjgl.input.Mouse.getEventDWheel()) * 24));
  }
  public void removed() { bar.release(); }
  private List<String> wrap(String value, int max) {
    List<String> lines = new ArrayList<>(); String line = "";
    for (String word : value.split("\\s+")) {
      if (!line.isEmpty() && textRenderer.getWidth(line + " " + word) > max) { lines.add(line); line = ""; }
      while (textRenderer.getWidth(word) > max) {
        int length = 1; while (length < word.length() && textRenderer.getWidth(word.substring(0, length + 1)) <= max) length++;
        if (!line.isEmpty()) { lines.add(line); line = ""; }
        lines.add(word.substring(0, length)); word = word.substring(length);
      }
      line += (line.isEmpty() ? "" : " ") + word;
    }
    if (!line.isEmpty()) lines.add(line); return lines;
  }
  protected void mouseClicked(int x, int y, int button) {
    if (button == 0 && bar.press(track(), x, y)) { scroll = bar.drag(track(), y, true); return; }
    if (button == 0 && inside(x, y, width / 2 - 60, height - 30, 120, 20)) close();
  }
  protected void keyPressed(char c, int key) {
    if (key == org.lwjgl.input.Keyboard.KEY_ESCAPE && !configuration) { close(); return; }
    if (key == org.lwjgl.input.Keyboard.KEY_UP) scroll -= 12;
    if (key == org.lwjgl.input.Keyboard.KEY_DOWN) scroll += 12;
    if (key == org.lwjgl.input.Keyboard.KEY_PRIOR) scroll -= track().height();
    if (key == org.lwjgl.input.Keyboard.KEY_NEXT) scroll += track().height();
    scroll = Math.max(0, Math.min(track().maximum(), scroll));
  }
  private void close() { if (configuration) minecraft.scheduleStop(); else minecraft.setScreen(null); }
}
