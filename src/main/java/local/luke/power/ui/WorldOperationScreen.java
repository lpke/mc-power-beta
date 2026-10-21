package local.luke.power.ui;

import com.google.gson.JsonPrimitive;
import java.util.*;
import local.luke.power.config.Setting;
import local.luke.power.world.WorldSnapshots;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

/** The screen owns only its name draft; the worker owns all streaming and verification. */
public final class WorldOperationScreen extends UiScreen {
  private final Screen parent;
  private final String folder;
  private final WorldSnapshots.Action action;
  private final Setting name;
  private final InlineValueEditor inline = new InlineValueEditor();
  private WorldSnapshots.Task task;
  private String error = "";
  @Override public void init() { Keyboard.enableRepeatEvents(true); }

  public WorldOperationScreen(Screen parent, String folder, String displayName, WorldSnapshots.Action action) {
    this.parent = parent; this.folder = folder; this.action = action;
    String initial = displayName == null || displayName.isBlank() ? folder : displayName;
    if (action == WorldSnapshots.Action.DUPLICATE) initial += " (Copy)";
    name = new Setting("worldOperation.name", "", "", "", "World name", "", Setting.Kind.TEXT,
        new JsonPrimitive(initial), new JsonPrimitive(initial), 0, 0, 1, List.of(), false);
    name.validator(value -> { try { WorldSnapshots.validName(value.getAsString()); }
      catch (java.io.IOException invalid) { throw new IllegalArgumentException(invalid.getMessage()); } });
  }
  private int left() { return Math.max(8, width / 2 - 150); }
  private int span() { return Math.min(300, width - 16); }
  private boolean running() { return task != null && !task.finished(); }
  private void leave() {
    inline.cancel();
    if (running()) task.cancel(); else minecraft.setScreen(parent);
  }
  private void start() {
    try {
      task = WorldSnapshots.start(FabricLoader.getInstance().getGameDir(), folder, action,
          name.value.getAsString(), () -> minecraft.world == null);
      error = "";
    } catch (Exception failure) { error = Objects.toString(failure.getMessage(), "World files could not be read."); }
  }
  @Override protected void keyPressed(char c, int key) {
    if (inline.key(c, key)) return;
    if (key == Keyboard.KEY_ESCAPE) leave();
  }
  @Override protected void mouseClicked(int x, int y, int button) {
    if (inline.press(x, y, button)) return;
    if (button != 0) return;
    if (inside(x, y, left(), height - 30, span(), 20)) { click(); leave(); return; }
    if (task != null) return;
    if (action == WorldSnapshots.Action.DUPLICATE && inside(x, y, left(), 62, span(), 18)) {
      click(); inline.begin(name, name, () -> error = "", left(), 62, span(), 28, height - 38); return;
    }
    if (inside(x, y, left(), 116, span(), 20)) { click(); start(); }
  }
  @Override public void removed() {
    Keyboard.enableRepeatEvents(false); inline.cancel(); TextDrag.cancel();
    if (running()) task.cancel();
  }
  @Override public void render(int mx, int my, float delta) {
    renderBackground(); titleBar(action == WorldSnapshots.Action.BACKUP ? "Back up world" : "Duplicate world");
    inline.beginFrame();
    if (task == null) {
      text(action == WorldSnapshots.Action.DUPLICATE ? "New world name" : "World", left(), 44, 0xcccccc);
      if (inline.active()) inline.render(this, left(), 62, span(), mx, my, 28, height - 38);
      else if (action == WorldSnapshots.Action.DUPLICATE) button(fit(name.value.getAsString(), span() - 12), left(), 62, span(), 18, mx, my, true);
      else text(fit(name.value.getAsString(), span()), left(), 65, 0xffffff);
      text(action == WorldSnapshots.Action.BACKUP ? "Includes map and recovery files." : "Recovery history stays with the original.", left(), 91, 0xaaaaaa);
      button(action == WorldSnapshots.Action.BACKUP ? "Create backup" : "Duplicate", left(), 116, span(), 20, mx, my, !inline.active());
    } else {
      text(fit(task.cancelling() ? "Canceling and cleaning up" : task.phase(), span()), left(), 47, 0xffffff);
      if (running()) {
        fill(left(), 67, left() + span(), 73, 0xff333333);
        fill(left(), 67, left() + (int)(span() * task.progress()), 73, 0xffaaaaaa);
      }
      if (task.result() != null) {
        text(fit(task.result().path().getFileName().toString(), span()), left(), 72, 0x99ff99);
        int y = 92;
        for (String warning : task.result().warnings()) { y = lines(warning, y, 0xffcc88); if (y > height - 50) break; }
      }
      if (!task.error().isEmpty()) lines(task.error(), 94, 0xff8888);
    }
    if (!error.isEmpty()) lines(error, 147, 0xff8888);
    button(running() ? task.cancelling() ? "Canceling..." : "Cancel" : "Back", left(), height - 30, span(), 20, mx, my, task == null || !task.cancelling());
    inline.endFrame(); inline.renderError(this);
  }
  private int lines(String message, int y, int color) {
    String line = "";
    for (String word : message.split(" ")) {
      if (!line.isEmpty() && textRenderer.getWidth(line + " " + word) > span()) {
        text(fit(line, span()), left(), y, color); y += 11; line = "";
        if (y > height - 45) return y;
      }
      line += (line.isEmpty() ? "" : " ") + word;
    }
    if (y <= height - 45) text(fit(line, span()), left(), y, color);
    return y + 11;
  }
}
