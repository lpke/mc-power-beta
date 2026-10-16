package local.luke.power.ui;

import com.google.gson.JsonPrimitive;
import java.util.*;
import local.luke.power.config.Setting;
import local.luke.power.input.PanoramaCatalog;
import local.luke.power.input.PanoramaRenderer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Panorama choices use the parent settings session's apply behaviour. */
public final class PanoramaScreen extends UiScreen {
  private final PowerOptionsScreen parent;
  private final Setting setting;
  private final Setting blur;
  private final ScrollBar bar = new ScrollBar();
  private double scroll;
  private int ticks;

  public PanoramaScreen(PowerOptionsScreen parent, Setting setting) {
    this.parent = parent;
    this.setting = setting;
    blur = parent.session().settings().stream()
        .filter(s -> s.id.equals("power_controls:userinterface.panoramaConfig.blurBackground"))
        .findFirst().orElse(null);
  }
  public void tick() { ticks++; }
  private int left() { return (width - span()) / 2; }
  private int span() { return Math.min(220, width - 24); }
  private List<PanoramaCatalog.Entry> entries() {
    return PanoramaCatalog.entries();
  }
  private int listTop() { return 44; }
  private int listBottom() { return height - 62; }
  private ScrollBar.Track track() { return new ScrollBar.Track(left() + span() + 3, listTop(), listBottom() - listTop(), entries().size() * 24, scroll); }
  private void select(String id) {
    setting.value = new JsonPrimitive(id);
    parent.changed(setting);
  }
  protected void mouseClicked(int x, int y, int button) {
    if (button != 0) return;
    if (bar.press(track(), x, y)) return;
    if (inside(x, y, left(), listTop(), span(), listBottom() - listTop())) {
      int row = (int)((y - listTop() + scroll) / 24);
      List<PanoramaCatalog.Entry> choices = entries();
      int rowY = listTop() + row * 24 - (int)scroll;
      if (row >= 0 && row < choices.size() && rowY >= listTop() && rowY + 20 <= listBottom() && y < rowY + 20)
        select(choices.get(row).id());
    }
    if (blur != null && inside(x, y, left(), height - 52, span(), 20)) {
      blur.value = new JsonPrimitive(!blur.value.getAsBoolean());
      parent.changed(blur);
    }
    if (inside(x, y, width / 2 - 50, height - 28, 100, 20)) minecraft.setScreen(parent);
  }
  protected void keyPressed(char c, int key) {
    if (key == Keyboard.KEY_ESCAPE) { minecraft.setScreen(parent); return; }
  }
  public void render(int x, int y, float delta) {
    if (PanoramaCatalog.folder(setting.value.getAsString()).isEmpty()) renderBackgroundTexture(0);
    else {
      PanoramaRenderer.render(minecraft.textureManager::getTextureId, setting.value.getAsString(), blur != null && blur.value.getAsBoolean(),
          ticks + delta, minecraft.displayWidth, minecraft.displayHeight);
      fillGradient(0, 0, width, height, -2130706433, 16777215);
      fillGradient(0, 0, width, height, 0, Integer.MIN_VALUE);
    }
    drawCenteredTextWithShadow(textRenderer, "Title panorama", width / 2, 18, 0xffffff);
    int wheel = Mouse.getDWheel();
    scroll = Math.max(0, Math.min(track().maximum(), scroll - Integer.signum(wheel) * 48));
    scroll = bar.drag(track(), y, Mouse.isButtonDown(0));
    List<PanoramaCatalog.Entry> choices = entries();
    for (int i = 0; i < choices.size(); i++) {
      int rowY = listTop() + i * 24 - (int)scroll;
      if (rowY < listTop() || rowY + 20 > listBottom()) continue;
      var entry = choices.get(i);
      button((PanoramaCatalog.scene(setting.value.getAsString()).equals(entry.scene()) ? "> " : "")
          + entry.label(), left(), rowY, span(), 20, x, y, true);
    }
    bar.render(this, track());
    button("Blur panorama: " + (blur != null && blur.value.getAsBoolean() ? "On" : "Off"),
        left(), height - 52, span(), 20, x, y, blur != null);
    button("Back", width / 2 - 50, height - 28, 100, 20, x, y, true);
  }
}
