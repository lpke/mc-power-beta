package local.luke.power.ui;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import local.luke.power.audio.MusicLibrary;
import local.luke.power.audio.MusicFolders;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.*;

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

  public DirectoryScreen(Screen parent, Consumer<List<String>> chosen, List<String> existing) {
    this.parent = parent;
    this.chosen = chosen;
    this.existing = List.copyOf(existing);
    try {
      String saved = MenuPreferences.current().musicBrowserDirectory;
      if (!saved.isEmpty()) {
        Path last = Path.of(saved);
        while (last != null && !Files.isDirectory(last)) last = last.getParent();
        if (last != null) directory = last;
      }
    } catch (RuntimeException ignored) { /* Fall back to Home if a stored path is invalid. */ }
    read();
  }

  private final ScrollBar scrollbar = new ScrollBar();
  private ScrollBar.Track track() { return new ScrollBar.Track(panelRight() - 10, 90, Math.max(0, height - 142), children.size() * 20, scroll); }

  private int panelWidth() { return Math.min(width, height * 16 / 9); }
  private int panelLeft() { return (width - panelWidth()) / 2; }
  private int panelRight() { return panelLeft() + panelWidth(); }

  public void init() {
    Keyboard.enableRepeatEvents(true);
  }

  public void removed() {
    scrollbar.release();
    Keyboard.enableRepeatEvents(false);
  }

  private void read() {
    try (var stream = Files.list(directory)) {
      children =
          stream
              .filter(
                  p ->
                      Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)
                          && (hidden || !p.getFileName().toString().startsWith(".")))
              .limit(2048)
              .sorted(
                  Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
              .toList();
      path.setText(directory.toString());
      path.selectAll();
      scroll = 0;
      error = "";
    } catch (Exception e) {
      children = List.of();
      error = "Could not read this folder.";
    }
  }

  private void go(Path next) {
    if (Files.isDirectory(next)) {
      directory = next.toAbsolutePath().normalize();
      read();
    } else error = "Folder unavailable.";
  }

  private void close() {
    try { MenuPreferences.update(v -> v.musicBrowserDirectory = directory.toString()); }
    catch (java.io.IOException e) { local.luke.power.PowerBeta.LOG.warn("Could not remember music browser location", e); }
    minecraft.setScreen(parent);
  }

  private void choose(boolean childrenOnly) {
    List<String> roots = selected.isEmpty() ? List.of(directory.toString()) : selected.stream().map(Path::toString).toList();
    try {
      var added = MusicFolders.additions(FabricLoader.getInstance().getGameDir(), existing, roots, childrenOnly);
      if (added.isEmpty()) {
        error = childrenOnly ? "No new child folders containing audio." : "Those folders are already listed.";
        return;
      }
      chosen.accept(added);
      close();
    } catch (java.io.IOException | RuntimeException e) {
      error = Objects.toString(e.getMessage(), "Could not add these folders.");
    }
  }

  public void onMouseEvent() {
    super.onMouseEvent();
    int d = Mouse.getEventDWheel();
    if (d != 0)
      scroll =
          Math.max(
              0,
              Math.min(
                  Math.max(0, children.size() * 20 - (height - 142)),
                  scroll - (int) Math.signum(d) * 40));
  }

  protected void keyPressed(char c, int key) {
    if (key == Keyboard.KEY_ESCAPE) {
      close();
      return;
    }
    if (key == Keyboard.KEY_RETURN && path.focused) {
      try {
        go(MusicLibrary.resolve(FabricLoader.getInstance().getGameDir(), path.text()));
      } catch (RuntimeException e) {
        error = "Invalid folder path.";
      }
      return;
    }
    path.key(c, key);
  }

  protected void mouseClicked(int x, int y, int b) {
    if (b != 0) return;
    if (scrollbar.press(track(), x, y)) { scroll = (int) scrollbar.drag(track(), y, true); return; }
    if (inside(x, y, panelLeft() + 14, 38, panelWidth() - 76, 18)) {
      path.focused = true;
      return;
    }
    path.focused = false;
    if (inside(x, y, panelRight() - 54, 37, 40, 20)) {
      try {
        go(MusicLibrary.resolve(FabricLoader.getInstance().getGameDir(), path.text()));
      } catch (RuntimeException e) {
        error = "Invalid folder path.";
      }
      return;
    }
    if (inside(x, y, panelLeft() + 14, 62, 48, 20)) {
      if (directory.getParent() != null) go(directory.getParent());
      return;
    }
    if (inside(x, y, panelLeft() + 66, 62, 52, 20)) {
      go(Path.of(System.getProperty("user.home")));
      return;
    }
    if (inside(x, y, panelLeft() + 122, 62, 76, 20)) {
      go(FabricLoader.getInstance().getGameDir());
      return;
    }
    if (inside(x, y, panelLeft() + 202, 62, 80, 20)) {
      hidden = !hidden;
      read();
      return;
    }
    if (inside(x, y, panelLeft() + 14, 90, panelWidth() - 34, Math.max(0, height - 142))) {
      int i = (y - 90 + scroll) / 20;
      if (i >= 0 && i < children.size()) {
        Path child = children.get(i);
        if (x < panelLeft() + 34) {
          if (!selected.remove(child)) selected.add(child);
          minecraft.soundManager.method_2009("random.click", 1, 1);
        } else go(child);
      }
      return;
    }
    if (inside(x, y, width / 2 - 148, height - 28, 112, 20)) choose(false);
    else if (inside(x, y, width / 2 - 32, height - 28, 112, 20)) choose(true);
    else if (inside(x, y, width / 2 + 84, height - 28, 64, 20)) close();
  }

  public void render(int x, int y, float delta) {
    scroll = (int) scrollbar.drag(track(), y, Mouse.isButtonDown(0));
    scroll = (int) Math.max(0, Math.min(scroll, track().maximum()));
    renderBackground();
    drawCenteredTextWithShadow(textRenderer, "Choose music folders", width / 2, 14, 0xffffff);
    input(path, panelLeft() + 14, 38, panelWidth() - 76, x, y, "");
    button("Go", panelRight() - 54, 37, 40, 20, x, y, true);
    button("Up", panelLeft() + 14, 62, 48, 20, x, y, directory.getParent() != null);
    button("Home", panelLeft() + 66, 62, 52, 20, x, y, true);
    button("Game folder", panelLeft() + 122, 62, 76, 20, x, y, true);
    button("Hidden: " + (hidden ? "On" : "Off"), panelLeft() + 202, 62, 80, 20, x, y, true);
    clip(panelLeft() + 14, 90, panelWidth() - 28, Math.max(0, height - 142));
    for (int i = 0; i < children.size(); i++) {
      int yy = 90 + i * 20 - scroll;
      boolean hover = inside(x, y, panelLeft() + 14, yy, panelWidth() - 28, 20);
      if (hover) fill(panelLeft() + 14, yy, panelRight() - 14, yy + 20, 0x50555555);
      button(selected.contains(children.get(i)) ? "x" : "", panelLeft() + 16, yy + 2, 16, 16, x, y, true);
      text(
          fit("> " + children.get(i).getFileName(), panelWidth() - 66),
          panelLeft() + 38,
          yy + 6,
          hover ? 0xffffa0 : 0xdddddd);
    }
    if (children.isEmpty()) text(error.isEmpty() ? "No subfolders." : error, panelLeft() + 20, 96, 0xaaaaaa);
    unclip();
    scrollbar.render(this, track());
    text(fit(error.isEmpty() ? selected.size() + " selected. Child folders scans one level only." : error, panelWidth() - 28),
        panelLeft() + 14, height - 44, error.isEmpty() ? 0xaaaaaa : 0xff8888);
    button(selected.isEmpty() ? "Use this folder" : "Use selected (" + selected.size() + ")", width / 2 - 148, height - 28, 112, 20, x, y, true);
    button("Use child folders", width / 2 - 32, height - 28, 112, 20, x, y, true);
    button("Cancel", width / 2 + 84, height - 28, 64, 20, x, y, true);
  }
}
