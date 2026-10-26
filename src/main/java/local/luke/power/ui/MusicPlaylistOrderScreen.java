package local.luke.power.ui;

import java.util.*;
import local.luke.power.audio.AudioController;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Reorders an isolated copy; Done returns it to the existing playlist editor. */
public final class MusicPlaylistOrderScreen extends UiScreen {
  private static final int TOP = 54, ROW = 26;
  private final PowerOptionsScreen parent;
  private final MusicPresetDraft editor;
  private final List<String> tracks;
  private final ScrollBar scrollbar = new ScrollBar();
  private int scroll, dragIndex = -1, pressX, pressY;
  private boolean dragging;
  private long lastScroll;

  public MusicPlaylistOrderScreen(PowerOptionsScreen parent, MusicPresetDraft editor) {
    this.parent = parent; this.editor = editor; tracks = new ArrayList<>(editor.tracks());
  }
  private int span() { return Math.min(540, width - 24); }
  private int left() { return (width - span()) / 2; }
  private int right() { return left() + span(); }
  private int bottom() { return height - 42; }
  private int rowY(int index) { return TOP + index * ROW - scroll; }
  private int dropIndex(int y) { return ListReorder.slot(y, TOP, scroll, ROW, tracks.size()); }
  private boolean listContains(int x, int y) { return inside(x, y, left(), TOP, span(), bottom() - TOP); }
  private ScrollBar.Track track() {
    return new ScrollBar.Track(right() + 3, TOP, Math.max(0, bottom() - TOP), tracks.size() * ROW, scroll);
  }
  private void cancelDrag() { dragIndex = -1; dragging = false; }
  public void removed() { cancelDrag(); scrollbar.release(); }
  protected void keyPressed(char c, int key) {
    if (key == Keyboard.KEY_ESCAPE) {
      if (dragIndex >= 0) cancelDrag(); else minecraft.setScreen(parent);
    }
  }
  public void onMouseEvent() {
    super.onMouseEvent();
    int wheel = Mouse.getEventDWheel();
    if (wheel != 0) scroll = (int) Math.max(0, Math.min(track().maximum(), scroll - Math.signum(wheel) * ROW));
  }
  protected void mouseClicked(int x, int y, int button) {
    if (button != 0) return;
    cancelDrag();
    if (scrollbar.press(track(), x, y)) { scroll = (int) scrollbar.drag(track(), y, true); return; }
    if (inside(x, y, width / 2 - 102, height - 28, 100, 20)) {
      click(); editor.order(tracks); minecraft.setScreen(parent); return;
    }
    if (inside(x, y, width / 2 + 2, height - 28, 100, 20)) { click(); minecraft.setScreen(parent); return; }
    if (!listContains(x, y)) return;
    int index = (y - TOP + scroll) / ROW;
    if (index < 0 || index >= tracks.size()) return;
    int rowY = rowY(index);
    int offset = inside(x, y, right() - 44, rowY, 20, 18) ? -1
        : inside(x, y, right() - 22, rowY, 20, 18) ? 1 : 0;
    if (offset != 0) {
      if (index + offset >= 0 && index + offset < tracks.size()) {
        click(); Collections.swap(tracks, index, index + offset);
      }
    } else if (x < right() - 48) {
      dragIndex = index; pressX = x; pressY = y; lastScroll = 0;
    }
  }
  protected void mouseReleased(int x, int y, int button) {
    super.mouseReleased(x, y, button);
    if (button != 0) return;
    if (dragIndex >= 0) {
      dragging |= Math.abs(x - pressX) + Math.abs(y - pressY) >= 3;
      if (dragging && Display.isActive() && listContains(x, y)) ListReorder.move(tracks, dragIndex, dropIndex(y));
      cancelDrag();
    }
    scroll = (int) scrollbar.drag(track(), y, false);
    scrollbar.release();
  }
  private void updateDrag(int x, int y) {
    if (!Display.isActive()) { cancelDrag(); scrollbar.release(); return; }
    if (dragIndex < 0) return;
    dragging |= Math.abs(x - pressX) + Math.abs(y - pressY) >= 3;
    long now = System.nanoTime();
    if (dragging && listContains(x, y) && now - lastScroll >= 100_000_000L) {
      if (y >= bottom() - 14) scroll = (int) Math.min(track().maximum(), scroll + ROW);
      else if (y < TOP + 14) scroll = Math.max(0, scroll - ROW);
      lastScroll = now;
    }
  }
  public void render(int mx, int my, float delta) {
    renderBackground(); titleBar("Playlist order");
    text(fit("Drag to reorder. Shuffle off plays this order.", span()), left(), 33, 0xaaaaaa);
    updateDrag(mx, my);
    scroll = (int) Math.max(0, Math.min(track().maximum(), scrollbar.drag(track(), my, Mouse.isButtonDown(0))));
    clip(left(), TOP, span(), Math.max(0, bottom() - TOP));
    for (int i = 0; i < tracks.size(); i++) {
      int y = rowY(i);
      if (y + 24 < TOP || y >= bottom()) continue;
      if (i == dragIndex) fill(left(), y, right(), y + 22, 0x604c6480);
      else if (inside(mx, my, left(), y, span() - 48, 22)) fill(left(), y, right() - 48, y + 22, 0x50000000);
      for (int bar = 0; bar < 3; bar++) fill(left() + 2, y + 5 + bar * 3, left() + 8, y + 6 + bar * 3, 0xff888888);
      text(fit((i + 1) + ". " + AudioController.musicLabel(tracks.get(i)), span() - 66), left() + 14, y + 5, 0xdddddd);
      iconButton("up", right() - 44, y, 20, mx, my, i > 0 && dragIndex < 0);
      iconButton("down", right() - 22, y, 20, mx, my, i + 1 < tracks.size() && dragIndex < 0);
    }
    if (dragging && listContains(mx, my)) {
      int line = Math.max(TOP, Math.min(bottom() - 2, rowY(dropIndex(my)) - 2));
      fill(left(), line, right(), line + 2, 0xffffdd88);
    }
    unclip(); scrollbar.render(this, track());
    button("Done", width / 2 - 102, height - 28, 100, 20, mx, my, true);
    button("Cancel", width / 2 + 2, height - 28, 100, 20, mx, my, true);
  }
}
