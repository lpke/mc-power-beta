package local.luke.power.ui;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.MusicLibrary;
import local.luke.power.config.Setting;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.input.*;

public final class FolderScreen extends UiScreen {
  private final PowerOptionsScreen parent;
  private final Setting setting;
  private final List<String> folders = new ArrayList<>();
  private int selected = -1, scroll;
  private String error = "";
  private final TextInput path = new TextInput("", 4096);

  public FolderScreen(PowerOptionsScreen parent, Setting setting) {
    this.parent = parent;
    this.setting = setting;
    for (var v : setting.value.getAsJsonArray()) folders.add(v.getAsString());
  }

  private int panelWidth() { return Math.min(width, height * 21 / 9); }
  private int panelLeft() { return (width - panelWidth()) / 2; }
  private int panelRight() { return panelLeft() + panelWidth(); }

  public void init() {
    Keyboard.enableRepeatEvents(true);
  }

  public void removed() {
    Keyboard.enableRepeatEvents(false);
  }

  protected void keyPressed(char c, int key) {
    if (key == Keyboard.KEY_ESCAPE) {
      minecraft.setScreen(parent);
      return;
    }
    if (key == Keyboard.KEY_RETURN && path.focused) {
      add(path.text);
      return;
    }
    path.key(c, key);
  }

  private void add(String folder) {
    if (folder.isBlank()) {
      error = "Enter a folder path.";
      return;
    }
    try {
      Path resolved = MusicLibrary.resolve(FabricLoader.getInstance().getGameDir(), folder);
      if (!Files.isDirectory(resolved)) {
        error = "That folder does not exist.";
        return;
      }
      if (folders.size() >= 32) {
        error = "Use at most 32 folders.";
        return;
      }
      String value = resolved.toAbsolutePath().normalize().toString();
      if (!folders.contains(value)) folders.add(value);
      selected = folders.indexOf(value);
      path.text = "";
      path.selectAll();
      error = "";
    } catch (RuntimeException e) {
      error = "Invalid folder path.";
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
                  Math.max(0, folders.size() * 24 - (height - 143)),
                  scroll - (int) Math.signum(d) * 24));
  }

  protected void mouseClicked(int x, int y, int b) {
    if (b != 0) return;
    if (inside(x, y, panelLeft() + 16, 42, panelWidth() - 128, 18)) {
      path.focused = true;
      return;
    }
    path.focused = false;
    if (inside(x, y, panelRight() - 104, 41, 40, 20)) {
      add(path.text);
      return;
    }
    if (inside(x, y, panelRight() - 60, 41, 44, 20)) {
      minecraft.setScreen(new DirectoryScreen(this, folder -> add(folder)));
      return;
    }
    if (y >= 74 && y < height - 66) {
      int i = (y - 74 + scroll) / 24;
      if (i >= 0 && i < folders.size()) selected = i;
      return;
    }
    if (inside(x, y, panelLeft() + 16, height - 54, 70, 20) && selected >= 0 && selected < folders.size()) {
      folders.remove(selected);
      selected = Math.min(selected, folders.size() - 1);
      return;
    }
    if (inside(x, y, panelRight() - 160, height - 30, 70, 20)) {
      JsonArray a = new JsonArray();
      folders.forEach(a::add);
      setting.value = a;
      parent.changed(setting);
      minecraft.setScreen(parent);
    }
    if (inside(x, y, panelRight() - 84, height - 30, 70, 20)) minecraft.setScreen(parent);
  }

  public void render(int x, int y, float delta) {
    renderBackground();
    drawCenteredTextWithShadow(textRenderer, setting.label, width / 2, 15, 0xffffff);
    input(path, panelLeft() + 16, 42, panelWidth() - 128, x, y, "Add folder path...");
    button("Add", panelRight() - 104, 41, 40, 20, x, y, true);
    button("Browse", panelRight() - 60, 41, 44, 20, x, y, true);
    clip(panelLeft() + 14, 74, panelWidth() - 28, Math.max(0, height - 140));
    for (int i = 0; i < folders.size(); i++) {
      int yy = 74 + i * 24 - scroll;
      if (i == selected) fill(panelLeft() + 14, yy, panelRight() - 14, yy + 22, 0x90555555);
      text(fit(folders.get(i), panelWidth() - 40), panelLeft() + 20, yy + 7, 0xffffff);
    }
    unclip();
    if (folders.isEmpty()) text("No folders selected.", panelLeft() + 20, 82, 0xaaaaaa);
    button("Remove", panelLeft() + 16, height - 54, 70, 20, x, y, selected >= 0);
    text(
        fit(error.isEmpty() ? "OGG, WAV and MUS. MP3 is not supported." : error, panelWidth() - 116),
        panelLeft() + 96,
        height - 47,
        error.isEmpty() ? 0xaaaaaa : 0xff8888);
    button("Done", panelRight() - 160, height - 30, 70, 20, x, y, true);
    button("Cancel", panelRight() - 84, height - 30, 70, 20, x, y, true);
  }
}
