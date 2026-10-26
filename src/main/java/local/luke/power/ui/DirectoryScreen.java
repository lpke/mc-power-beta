package local.luke.power.ui;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import local.luke.power.audio.MusicLibrary;
import local.luke.power.audio.MusicFolders;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.*;

/** Shared folder picker. Selections may span directories; import is one validated batch. */
public final class DirectoryScreen extends UiScreen {
  private final Screen parent;
  private final Consumer<List<String>> chosen;
  private final List<String> existing;
  private final Set<Path> selected = new LinkedHashSet<>();
  private Path directory = Path.of(System.getProperty("user.home"));
  private List<Path> children = List.of();
  private String error = "";
  private int scroll;
  private boolean hidden;
  private final TextInput path = new TextInput(directory.toString(), 4096);
  private final ScrollBar scrollbar = new ScrollBar();

  public DirectoryScreen(Screen parent, Consumer<List<String>> chosen, List<String> existing) {
    this.parent = parent; this.chosen = chosen; this.existing = List.copyOf(existing);
    try {
      String saved = MenuPreferences.current().musicBrowserDirectory;
      if (!saved.isEmpty()) {
        Path last = Path.of(saved);
        while (last != null && !Files.isDirectory(last)) last = last.getParent();
        if (last != null) directory = last;
      }
    } catch (RuntimeException ignored) { /* Home remains available after an invalid saved location. */ }
    read();
  }
  private int panelWidth() { return Math.min(width, height * 16 / 9); }
  private int panelLeft() { return (width - panelWidth()) / 2; }
  private int panelRight() { return panelLeft() + panelWidth(); }
  private boolean selectionWraps() { return panelWidth() < 432; }
  private int selectX() { return selectionWraps() ? panelLeft() + 14 : panelRight() - 164; }
  private int selectY() { return selectionWraps() ? 86 : 62; }
  private int listTop() { return selectionWraps() ? 110 : 86; }
  private int listBottom() { return height - 52; }
  private ScrollBar.Track track() { return new ScrollBar.Track(panelRight() - 10, listTop(), Math.max(0, listBottom() - listTop()), children.size() * 20, scroll); }
  public void init() { Keyboard.enableRepeatEvents(true); }
  public void removed() { scrollbar.release(); path.focused = false; Keyboard.enableRepeatEvents(false); }

  private void read() {
    try (var stream = Files.list(directory)) {
      children = stream.filter(p -> Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)
          && (hidden || !p.getFileName().toString().startsWith(".")))
          .limit(2048).sorted(Comparator.comparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER)).toList();
      path.setText(directory.toString()); path.selectAll(); scroll = 0; error = "";
    } catch (Exception e) { children = List.of(); error = "Could not read this folder."; }
  }
  private void go(Path next) {
    if (Files.isDirectory(next, LinkOption.NOFOLLOW_LINKS) && Files.isReadable(next)) {
      directory = next.toAbsolutePath().normalize(); read();
    } else error = "Folder unavailable.";
  }
  private void enterPath() {
    if (path.text().isBlank()) { error = "Enter a folder path."; return; }
    try { go(MusicLibrary.resolve(FabricLoader.getInstance().getGameDir(), path.text())); }
    catch (RuntimeException e) { error = "Invalid folder path."; }
  }
  private void close() {
    try { MenuPreferences.update(v -> v.musicBrowserDirectory = directory.toString()); }
    catch (java.io.IOException e) { local.luke.power.PowerBeta.LOG.warn("Could not remember music browser location", e); }
    minecraft.setScreen(parent);
  }
  private void choose() {
    try {
      if (selected.isEmpty() && !path.text().equals(directory.toString())) {
        enterPath(); if (!error.isEmpty()) return;
      }
      List<String> roots = selected.isEmpty() ? List.of(directory.toString()) : selected.stream().map(Path::toString).toList();
      var added = MusicFolders.additions(FabricLoader.getInstance().getGameDir(), existing, roots, false);
      if (added.isEmpty()) { error = "Those folders are already imported."; return; }
      chosen.accept(added); close();
    } catch (java.io.IOException | RuntimeException e) { error = Objects.toString(e.getMessage(), "Could not add these folders."); }
  }
  public void onMouseEvent() {
    super.onMouseEvent(); int d = Mouse.getEventDWheel();
    if (d != 0) scroll = (int)Math.max(0, Math.min(track().maximum(), scroll - Math.signum(d) * 40));
  }
  protected void mouseReleased(int x, int y, int button) {
    super.mouseReleased(x, y, button);
    if (button == 0) { scroll = (int)scrollbar.drag(track(), y, true); scrollbar.release(); }
  }
  protected void keyPressed(char c, int key) {
    if (key == Keyboard.KEY_ESCAPE) { close(); return; }
    if (key == Keyboard.KEY_RETURN && path.focused) { enterPath(); return; }
    path.key(c, key);
  }
  protected void mouseClicked(int x, int y, int b) {
    if (b != 0 && b != 1) return;
    if (b == 0 && scrollbar.press(track(), x, y)) { scroll = (int)scrollbar.drag(track(), y, true); return; }
    if (inside(x, y, panelLeft() + 14, 38, panelWidth() - 76, 18)) { path.focused = true; path.click(x, b); return; }
    if (b != 0) return;
    path.focused = false;
    if (inside(x, y, panelRight() - 54, 37, 40, 20)) { click(); enterPath(); return; }
    if (inside(x, y, panelLeft() + 14, 62, 40, 20)) { if (directory.getParent() != null) { click(); go(directory.getParent()); } return; }
    if (inside(x, y, panelLeft() + 58, 62, 44, 20)) { click(); go(Path.of(System.getProperty("user.home"))); return; }
    if (inside(x, y, panelLeft() + 106, 62, 76, 20)) { click(); go(FabricLoader.getInstance().getGameDir()); return; }
    if (inside(x, y, panelLeft() + 186, 62, 72, 20)) { click(); hidden = !hidden; read(); return; }
    if (inside(x, y, selectX(), selectY(), 66, 20)) { if (!selected.containsAll(children)) { click(); selected.addAll(children); } return; }
    if (inside(x, y, selectX() + 70, selectY(), 80, 20)) { if (children.stream().anyMatch(selected::contains)) { click(); selected.removeAll(children); } return; }
    if (inside(x, y, panelLeft() + 14, listTop(), panelWidth() - 34, listBottom() - listTop())) {
      int i = (y - listTop() + scroll) / 20;
      if (i >= 0 && i < children.size()) {
        Path child = children.get(i); click();
        if (x < panelLeft() + 34) { if (!selected.remove(child)) selected.add(child); }
        else go(child);
      }
      return;
    }
    if (inside(x, y, width / 2 - 102, height - 28, 130, 20)) { click(); choose(); }
    else if (inside(x, y, width / 2 + 32, height - 28, 70, 20)) { click(); close(); }
  }
  public void render(int x, int y, float delta) {
    scroll = (int)Math.max(0, Math.min(track().maximum(), scrollbar.drag(track(), y, Mouse.isButtonDown(0))));
    renderBackground(); drawCenteredTextWithShadow(textRenderer, "Choose music folders", width / 2, 14, 0xffffff);
    input(path, panelLeft() + 14, 38, panelWidth() - 76, x, y, "Folder path");
    button("Go", panelRight() - 54, 37, 40, 20, x, y, true);
    button("Up", panelLeft() + 14, 62, 40, 20, x, y, directory.getParent() != null);
    button("Home", panelLeft() + 58, 62, 44, 20, x, y, true);
    button("Game folder", panelLeft() + 106, 62, 76, 20, x, y, true);
    button("Hidden: " + (hidden ? "On" : "Off"), panelLeft() + 186, 62, 72, 20, x, y, true);
    button("Select all", selectX(), selectY(), 66, 20, x, y, !children.isEmpty() && !selected.containsAll(children));
    button("Deselect all", selectX() + 70, selectY(), 80, 20, x, y, children.stream().anyMatch(selected::contains));
    clip(panelLeft() + 14, listTop(), panelWidth() - 28, Math.max(0, listBottom() - listTop()));
    for (int i = 0; i < children.size(); i++) {
      int yy = listTop() + i * 20 - scroll;
      if (yy + 20 <= listTop() || yy >= listBottom()) continue;
      boolean hover = inside(x, y, panelLeft() + 14, Math.max(yy, listTop()), panelWidth() - 28, Math.min(yy + 20, listBottom()) - Math.max(yy, listTop()));
      if (hover) fill(panelLeft() + 14, yy, panelRight() - 14, yy + 20, 0x50555555);
      button(selected.contains(children.get(i)) ? "x" : "", panelLeft() + 16, yy + 2, 16, 16, x, y, true);
      text(fit("> " + children.get(i).getFileName(), panelWidth() - 66), panelLeft() + 38, yy + 6, hover ? 0xffffa0 : 0xdddddd);
    }
    if (children.isEmpty()) text(error.isEmpty() ? "No subfolders." : error, panelLeft() + 20, listTop() + 6, 0xaaaaaa);
    unclip(); scrollbar.render(this, track());
    String status = !error.isEmpty() ? error : selected.isEmpty() ? "" : selected.size() + " selected";
    if (!status.isEmpty()) text(fit(status, panelWidth() - 28), panelLeft() + 14, height - 44, error.isEmpty() ? 0xaaaaaa : 0xff8888);
    button(selected.isEmpty() ? "Use this folder" : "Use selected (" + selected.size() + ")", width / 2 - 102, height - 28, 130, 20, x, y, true);
    button("Cancel", width / 2 + 32, height - 28, 70, 20, x, y, true);
  }
}
