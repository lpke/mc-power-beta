package local.luke.power.ui;

import com.google.gson.JsonElement;
import java.io.IOException;
import java.util.*;
import local.luke.power.audio.MusicFolders;
import local.luke.power.config.Setting;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.input.*;

/** Folder edits stay in a draft until Done or Save, including additions from the browser. */
public final class FolderScreen extends UiScreen {
  private final PowerOptionsScreen parent;
  private final Setting setting;
  private final List<String> folders = new ArrayList<>();
  private final Set<String> disabled = new LinkedHashSet<>();
  private final JsonElement original;
  private final TextInput path = new TextInput("", 4096);
  private final ScrollBar scrollbar = new ScrollBar();
  private int scroll;
  private String error = "";
  private boolean confirm;

  public FolderScreen(PowerOptionsScreen parent, Setting setting) {
    this.parent = parent;
    this.setting = setting;
    var selection = MusicFolders.decode(setting.value);
    folders.addAll(selection.paths());
    disabled.addAll(selection.disabled());
    original = MusicFolders.encode(folders, disabled);
  }

  private int panelWidth() { return Math.min(width, height * 16 / 9); }
  private int panelLeft() { return (width - panelWidth()) / 2; }
  private int panelRight() { return panelLeft() + panelWidth(); }
  private ScrollBar.Track track() { return new ScrollBar.Track(panelRight() - 10, 74, Math.max(0, height - 140), folders.size() * 24, scroll); }
  public void init() { Keyboard.enableRepeatEvents(true); }
  public void removed() { scrollbar.release(); Keyboard.enableRepeatEvents(false); }

  private boolean changed() { return !original.equals(MusicFolders.encode(folders, disabled)); }
  private void leave() {
    scrollbar.release();
    if (changed()) confirm = true;
    else minecraft.setScreen(parent);
  }

  private void save() {
    JsonElement value = MusicFolders.encode(folders, disabled);
    setting.validate(value);
    setting.value = value;
    parent.changed(setting);
    minecraft.setScreen(parent);
  }

  protected void keyPressed(char c, int key) {
    if (key == Keyboard.KEY_ESCAPE) {
      if (confirm) confirm = false; else leave();
      return;
    }
    if (confirm) return;
    if (key == Keyboard.KEY_RETURN && path.focused) { add(List.of(path.text())); return; }
    path.key(c, key);
  }

  private void add(List<String> selected) {
    try {
      var added = MusicFolders.additions(FabricLoader.getInstance().getGameDir(), folders, selected, false);
      folders.addAll(added);
      error = added.isEmpty() ? "Those folders are already listed." : "";
      path.setText("");
    } catch (IOException | RuntimeException e) {
      error = Objects.toString(e.getMessage(), "Invalid folder path.");
    }
  }

  public void onMouseEvent() {
    super.onMouseEvent();
    if (!confirm && Mouse.getEventDWheel() != 0)
      scroll = (int) Math.max(0, Math.min(track().maximum(), scroll - Math.signum(Mouse.getEventDWheel()) * 24));
  }

  protected void mouseClicked(int x, int y, int button) {
    if (button != 0 && button != 1) return;
    if (confirm) {
      if (button != 0) return;
      for (int i = 0; i < 3; i++) if (inside(x, y, width / 2 - 118 + i * 80, height / 2 + 8, 76, 20)) {
        if (i == 0) save();
        else if (i == 1) minecraft.setScreen(parent);
        else confirm = false;
      }
      return;
    }
    if (button == 0 && scrollbar.press(track(), x, y)) { scroll = (int) scrollbar.drag(track(), y, true); return; }
    if (inside(x, y, panelLeft() + 16, 74, panelWidth() - 36, Math.max(0, height - 140))) {
      int index = (y - 74 + scroll) / 24;
      if (index >= folders.size() || (y - 74 + scroll) % 24 >= 18) return;
      String folder = folders.get(index);
      if (x < panelLeft() + 54) {
        if (!disabled.remove(folder)) disabled.add(folder);
      } else if (button == 0 && x >= panelRight() - 38) {
        folders.remove(index);
        disabled.remove(folder);
      } else return;
      minecraft.soundManager.method_2009("random.click", 1, 1);
      return;
    }
    if (button != 0) return;
    path.focused = inside(x, y, panelLeft() + 16, 42, panelWidth() - 128, 18);
    if (inside(x, y, panelRight() - 104, 41, 40, 20)) add(List.of(path.text()));
    else if (inside(x, y, panelRight() - 60, 41, 44, 20))
      minecraft.setScreen(new DirectoryScreen(this, this::add, folders));
    else if (inside(x, y, panelRight() - 160, height - 30, 70, 20)) save();
    else if (inside(x, y, panelRight() - 84, height - 30, 70, 20)) leave();
  }

  public void render(int x, int y, float delta) {
    scroll = (int) scrollbar.drag(track(), y, !confirm && Mouse.isButtonDown(0));
    scroll = (int) Math.max(0, Math.min(scroll, track().maximum()));
    renderBackground();
    drawCenteredTextWithShadow(textRenderer, setting.label, width / 2, 15, 0xffffff);
    int mx = confirm ? -1 : x, my = confirm ? -1 : y;
    input(path, panelLeft() + 16, 42, panelWidth() - 128, mx, my, "Add folder path...");
    button("Add", panelRight() - 104, 41, 40, 20, mx, my, true);
    button("Browse", panelRight() - 60, 41, 44, 20, mx, my, true);
    clip(panelLeft() + 14, 74, panelWidth() - 28, Math.max(0, height - 140));
    for (int i = 0; i < folders.size(); i++) {
      int yy = 74 + i * 24 - scroll;
      String folder = folders.get(i);
      if (inside(mx, my, panelLeft() + 14, yy, panelWidth() - 28, 22))
        fill(panelLeft() + 14, yy, panelRight() - 14, yy + 22, 0x60000000);
      button(disabled.contains(folder) ? "Off" : "On", panelLeft() + 16, yy, 38, 18, mx, my, true);
      text(fit(folder, panelWidth() - 106), panelLeft() + 60, yy + 5, disabled.contains(folder) ? 0x999999 : 0xdddddd);
      button("X", panelRight() - 38, yy, 18, 18, mx, my, true);
    }
    unclip();
    scrollbar.render(this, track());
    if (folders.isEmpty()) text("No folders selected.", panelLeft() + 20, 82, 0xaaaaaa);
    text(fit(error.isEmpty() ? "OGG, WAV, MUS and MP3. Changes are saved with Done." : error, panelWidth() - 32),
        panelLeft() + 16, height - 47, error.isEmpty() ? 0xaaaaaa : 0xff8888);
    button("Done", panelRight() - 160, height - 30, 70, 20, mx, my, true);
    button("Cancel", panelRight() - 84, height - 30, 70, 20, mx, my, true);
    if (confirm) {
      fill(0, 0, width, height, 0xc0000000);
      drawCenteredTextWithShadow(textRenderer, "Save changes to these folders?", width / 2, height / 2 - 16, 0xffffff);
      for (int i = 0; i < 3; i++) button(new String[]{"Save", "Discard", "Cancel"}[i],
          width / 2 - 118 + i * 80, height / 2 + 8, 76, 20, x, y, true);
    } else if (inside(x,y,panelRight()-38,74,18,Math.max(0,height-140)))
      tooltip("Remove folder from the list. Files stay on disk.",x,y);
  }
}
