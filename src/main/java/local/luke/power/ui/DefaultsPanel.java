package local.luke.power.ui;

import java.util.*;
import local.luke.power.config.DefaultBases;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** One row per reset base; only a complete load needs confirmation. */
final class DefaultsPanel extends UiScreen {
  private final PowerOptionsScreen owner;
  private DefaultBases.Base pending;
  private String error = "";
  private final HoverDelay hoverDelay = new HoverDelay();
  private DefaultBases.Base hovered;
  private final ScrollBar scrollbar = new ScrollBar();
  private double scroll;
  private static final String INTRO = "Choose what setting configuration is considered \"Default\" (used by the Reset button next to each setting).";
  private static final String REMINDER = "Changing the base keeps your preferences. Unedited new-world rules follow the new base.";
  DefaultsPanel(PowerOptionsScreen owner) { this.owner = owner; }
  void tick(boolean active) { if (active) hoverDelay.tick(); else hoverDelay.clear(); }
  boolean modal() { return pending != null; }
  boolean key(int key) {
    if (!modal()) return false;
    if (key == Keyboard.KEY_ESCAPE) pending = null;
    return true;
  }
  private int buttonWidth(int span) { return SettingRows.narrow(span) ? Math.min(90, (span - 8) / 2) : 90; }
  private int rowSpan(int span) { return Math.min(560, span); }
  private List<String> lines(String text, int span) {
    List<String> lines = new ArrayList<>(); String line = "";
    for (String word : text.split(" ")) {
      if (!line.isEmpty() && textRenderer.getWidth(line + " " + word) > span) { lines.add(line); line = ""; }
      line += (line.isEmpty() ? "" : " ") + word;
    }
    lines.add(line); return lines;
  }
  private int rowTop(int span, int index) {
    int introBottom = 37 + (lines(INTRO, span - 8).size() + lines(REMINDER, span - 8).size()) * 11 + 6;
    return Math.max(SettingRows.narrow(span) ? 64 : 84, introBottom + 14) + index * SettingRows.height(rowSpan(span)) - (int)scroll;
  }
  private ScrollBar.Track track(int left, int span) {
    int content = rowTop(span, DefaultBases.Base.values().length) + (int)scroll - 28;
    return new ScrollBar.Track(left + span + 3, 28, height - 72, content, scroll);
  }
  void wheel(int amount, int left, int span) {
    if (!modal()) scroll = Math.max(0, Math.min(track(left, span).maximum(), scroll - Math.signum(amount) * 36));
  }
  private int loadX(int left, int span) { return left + span - buttonWidth(span) * 2 - 4; }
  private int buttonY(int span, int top) { return top + SettingRows.buttonOffset(span); }
  boolean press(int x, int y, int button, int left, int span) {
    if (modal()) {
      if (button == 0 && inside(x, y, width / 2 - 102, height / 2 + 24, 100, 20)) {
        click();
        try { owner.loadDefaults(pending); pending = null; error = ""; }
        catch (Exception failure) { error = failure.getMessage(); pending = null; }
      } else if (button == 0 && inside(x, y, width / 2 + 2, height / 2 + 24, 100, 20)) { click(); pending = null; }
      return true;
    }
    if (button != 0) return false;
    if (scrollbar.press(track(left, span), x, y)) return true;
    if (y < 28 || y >= height - 44) return false;
    int rowSpan = rowSpan(span), w = buttonWidth(rowSpan);
    for (var base : DefaultBases.Base.values()) {
      int top = rowTop(span, base.ordinal());
      if (inside(x, y, loadX(left, rowSpan), buttonY(rowSpan, top), w, SettingRows.BUTTON_HEIGHT)) { click(); pending = base; return true; }
      if (inside(x, y, left + rowSpan - w, buttonY(rowSpan, top), w, SettingRows.BUTTON_HEIGHT)) {
        if (base != DefaultBases.current()) {
          click();
          try {
            DefaultBases.use(base, FabricLoader.getInstance().getGameDir());
            owner.session().settings().forEach(s -> s.refreshDefault());
            owner.refreshRuleTemplates();
          }
          catch (Exception failure) { error = failure.getMessage(); }
        }
        return true;
      }
    }
    return false;
  }
  void draw(int x, int y, int left, int span) {
    hovered = null;
    scroll = Math.max(0, Math.min(track(left, span).maximum(), scrollbar.drag(track(left, span), y, !modal() && Display.isActive() && Mouse.isButtonDown(0))));
    clip(left - 2, 28, span + 4, height - 72);
    int lineY = 37 - (int)scroll;
    for (String line : lines(INTRO, span - 8)) { text(line, left, lineY, 0xdddddd); lineY += 11; }
    lineY += 6;
    for (String line : lines(REMINDER, span - 8)) { text(line, left, lineY, 0xaaaaaa); lineY += 11; }
    int rowSpan = rowSpan(span), w = buttonWidth(rowSpan);
    int pointerY = y >= 28 && y < height - 44 ? y : -1000;
    for (var base : DefaultBases.Base.values()) {
      int top = rowTop(span, base.ordinal());
      if (!modal() && inside(x, pointerY, left, top, rowSpan, SettingRows.height(rowSpan))) {
        rectangle(left - 2, top, left + rowSpan + 2, top + SettingRows.height(rowSpan), 0x60000000);
        hovered = base;
      }
      boolean selected = base == DefaultBases.current();
      int labelY = top + SettingRows.labelOffset(rowSpan);
      if (selected) rectangle(left + 1, labelY - 1, left + 3, labelY + 9, 0xffffdd66);
      text(fit(base.label, SettingRows.narrow(rowSpan) ? rowSpan - 12 : rowSpan - w * 2 - 16), left + 8, labelY, selected ? 0xffffdd66 : 0xdddddd);
      button("Load", loadX(left, rowSpan), buttonY(rowSpan, top), w, SettingRows.BUTTON_HEIGHT, x, pointerY, true);
      button("Use as base", left + rowSpan - w, buttonY(rowSpan, top), w, SettingRows.BUTTON_HEIGHT, x, pointerY, !selected);
    }
    unclip(); scrollbar.render(this, track(left, span));
    if (!error.isEmpty()) text(fit(error, span), left, height - 44, 0xff8888);
  }
  void overlay(int x, int y) {
    if (hoverDelay.ready(hovered == null ? "" : hovered.id))
      foregroundTooltip(hovered.label + "\n" + hovered.description, x, y);
    if (modal()) {
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
      try {
        GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING);
        rectangle(0, 0, width, height, 0xc0000000);
        centeredText("Load " + pending.label + "?", height / 2 - 32, 0xffffff);
        centeredText(fit("All settings and new-world defaults will be reset.", width - 24), height / 2 - 13, 0xdddddd);
        centeredText(fit("Worlds, recovery records and saved presets are kept.", width - 24), height / 2 + 1, 0xaaaaaa);
        button("Load", width / 2 - 102, height / 2 + 24, 100, 20, x, y, true);
        button("Cancel", width / 2 + 2, height / 2 + 24, 100, 20, x, y, true);
      } finally { GL11.glPopAttrib(); }
    }
  }
}
