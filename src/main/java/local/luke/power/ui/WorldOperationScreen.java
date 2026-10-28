package local.luke.power.ui;

import com.google.gson.JsonPrimitive;
import java.util.*;
import local.luke.power.config.Setting;
import local.luke.power.world.WorldSnapshots;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

/** The screen owns its drafts; the worker owns all streaming and verification. */
public final class WorldOperationScreen extends UiScreen {
  private final Screen parent;
  private final String folder;
  private final WorldSnapshots.Action action;
  private final Setting name;
  private final String worldName;
  private WorldSnapshots.BackupOptions options = WorldSnapshots.BackupOptions.ALL;
  private final InlineValueEditor inline = new InlineValueEditor();
  private WorldSnapshots.Task task;
  private String error = "";
  @Override public void init() { Keyboard.enableRepeatEvents(true); }

  public WorldOperationScreen(Screen parent, String folder, String displayName, WorldSnapshots.Action action) {
    this.parent = parent; this.folder = folder; this.action = action;
    String initial = displayName == null || displayName.isBlank() ? folder : displayName;
    worldName = initial;
    if (action == WorldSnapshots.Action.DUPLICATE) initial += " (Copy)";
    name = new Setting("worldOperation.name", "", "", "", "World name", "", Setting.Kind.TEXT,
        new JsonPrimitive(initial), new JsonPrimitive(initial), 0, 0, 1, List.of(), false);
    name.validator(value -> { try { WorldSnapshots.validName(value.getAsString()); }
      catch (java.io.IOException invalid) { throw new IllegalArgumentException(invalid.getMessage()); } });
  }
  private int left() { return Math.max(8, width / 2 - 150); }
  private int span() { return Math.min(300, width - 16); }
  private int footerSpan() { return task == null ? (span() - 6) / 2 : span(); }
  private int backLeft() { return task == null ? left() + span() - footerSpan() : left(); }
  private boolean running() { return task != null && !task.finished(); }
  private void leave() {
    inline.cancel();
    if (running()) task.cancel(); else minecraft.setScreen(parent);
  }
  private void start() {
    try {
      task = WorldSnapshots.start(FabricLoader.getInstance().getGameDir(), folder, action,
          name.value.getAsString(), options, () -> minecraft.world == null);
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
    if (inside(x, y, backLeft(), height - 30, footerSpan(), 20)) { click(); leave(); return; }
    if (task != null) return;
    if (!inline.active() && inside(x, y, left(), height - 30, footerSpan(), 20)) { click(); start(); return; }
    if (action == WorldSnapshots.Action.DUPLICATE && inside(x, y, left(), 62, span(), 18)) {
      click(); inline.begin(name, name, () -> error = "", left(), 62, span(), 28, height - 38); return;
    }
    if (action == WorldSnapshots.Action.BACKUP) {
      if (inside(x, y, left(), 70, span(), 18)) {
        click(); options = new WorldSnapshots.BackupOptions(!options.maps(), options.powerConfig(), options.metadataBackups());
      } else if (inside(x, y, left(), 92, span(), 18)) {
        click(); options = new WorldSnapshots.BackupOptions(options.maps(), !options.powerConfig(), options.metadataBackups());
      } else if (inside(x, y, left(), 114, span(), 18)) {
        click(); options = new WorldSnapshots.BackupOptions(options.maps(), options.powerConfig(), !options.metadataBackups());
      }
    }
  }
  @Override public void removed() {
    Keyboard.enableRepeatEvents(false); inline.cancel(); TextDrag.cancel();
    if (running()) task.cancel();
  }
  @Override public void render(int mx, int my, float delta) {
    renderBackground(); titleBar(fit((action == WorldSnapshots.Action.BACKUP ? "Back up world: " : "Duplicate world: ") + worldName, width - 16));
    inline.beginFrame();
    if (task == null) {
      if (action == WorldSnapshots.Action.BACKUP) {
        text("Includes terrain, player data, world rules", left(), 39, 0xcccccc);
        text("and recovery records. Also include:", left(), 50, 0xcccccc);
        button("Maps and waypoints: " + on(options.maps()), left(), 70, span(), 18, mx, my, true);
        button("Power Beta config: " + on(options.powerConfig()), left(), 92, span(), 18, mx, my, true);
        button("Previous metadata backups: " + on(options.metadataBackups()), left(), 114, span(), 18, mx, my, true);
      } else {
        text("New world name", left(), 44, 0xcccccc);
        if (inline.active()) inline.render(this, left(), 62, span(), mx, my, 28, height - 38);
        else button(fit(name.value.getAsString(), span() - 12), left(), 62, span(), 18, mx, my, true);
      }
      button(action == WorldSnapshots.Action.BACKUP ? "Create backup" : "Duplicate", left(), height - 30, footerSpan(), 20, mx, my, !inline.active());
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
    if (!error.isEmpty()) lines(error, action == WorldSnapshots.Action.BACKUP ? 141 : 94, 0xff8888);
    button(running() ? task.cancelling() ? "Canceling..." : "Cancel" : "Back", backLeft(), height - 30, footerSpan(), 20, mx, my, task == null || !task.cancelling());
    inline.endFrame(); inline.renderError(this);
    if (!running()) backTooltip("Edit world", backLeft(), height - 30, footerSpan(), 20, mx, my);
    if (task == null && action == WorldSnapshots.Action.BACKUP) {
      if (inside(mx, my, left(), 70, span(), 18)) foregroundTooltip("Map tiles and saved waypoints for this world.", mx, my);
      else if (inside(mx, my, left(), 92, span(), 18)) foregroundTooltip("A snapshot of config/power-beta.json, including global preferences and creation presets. Restoring the world does not apply these settings automatically.", mx, my);
      else if (inside(mx, my, left(), 114, span(), 18)) foregroundTooltip("Completed backups of this world's metadata. Interrupted saves and recovery evidence are always included.", mx, my);
    }
  }
  private static String on(boolean enabled) { return enabled ? "On" : "Off"; }
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
