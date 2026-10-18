package local.luke.power.ui;

import local.luke.power.config.DefaultBases;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** One row per reset base; only a complete load needs confirmation. */
final class DefaultsPanel extends UiScreen {
  private final PowerOptionsScreen owner;
  private DefaultBases.Base pending;
  private String error = "";
  private final HoverDelay hoverDelay = new HoverDelay();
  private DefaultBases.Base hovered;
  DefaultsPanel(PowerOptionsScreen owner) { this.owner = owner; }
  void tick(boolean active) { if (active) hoverDelay.tick(); else hoverDelay.clear(); }
  boolean modal() { return pending != null; }
  boolean key(int key) {
    if (!modal()) return false;
    if (key == Keyboard.KEY_ESCAPE) pending = null;
    return true;
  }
  private boolean stacked(int span) { return span < 290; }
  private int buttonWidth(int span) { return stacked(span) ? 78 : 90; }
  private int rowTop(int span, int index) { return (stacked(span) ? 64 : 84) + index * (stacked(span) ? 36 : 34); }
  private int loadX(int left, int span) { return left + span - buttonWidth(span) * (stacked(span) ? 1 : 2) - (stacked(span) ? 0 : 4); }
  private int baseY(int span, int top) { return top + (stacked(span) ? 18 : 0); }
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
    int w = buttonWidth(span);
    for (var base : DefaultBases.Base.values()) {
      int top = rowTop(span, base.ordinal());
      if (inside(x, y, loadX(left, span), top, w, stacked(span) ? 18 : 20)) { click(); pending = base; return true; }
      if (inside(x, y, left + span - w, baseY(span, top), w, stacked(span) ? 18 : 20)) {
        if (base != DefaultBases.current()) {
          click();
          try { DefaultBases.use(base, FabricLoader.getInstance().getGameDir()); owner.session().settings().forEach(s -> s.refreshDefault()); }
          catch (Exception failure) { error = failure.getMessage(); }
        }
        return true;
      }
    }
    return false;
  }
  void draw(int x, int y, int left, int span) {
    hovered = null;
    text(fit("Choose what Reset restores, or load a complete setup.", span), left, 37, 0xdddddd);
    text(fit(error.isEmpty() ? "Changing the base keeps your current settings." : error, span), left, 54, error.isEmpty() ? 0xaaaaaa : 0xff8888);
    int w = buttonWidth(span);
    for (var base : DefaultBases.Base.values()) {
      int top = rowTop(span, base.ordinal());
      if (!modal() && inside(x, y, left, top, span, stacked(span) ? 36 : 24)) {
        rectangle(left - 2, top, left + span, top + (stacked(span) ? 36 : 24), 0x60000000);
        hovered = base;
      }
      boolean selected = base == DefaultBases.current();
      if (selected) rectangle(left + 1, top + 5, left + 3, top + 15, 0xffffdd66);
      text(fit(base.label, span - w * (stacked(span) ? 1 : 2) - 16), left + 8, top + (stacked(span) ? 14 : 6), selected ? 0xffffdd66 : 0xdddddd);
      button("Load", loadX(left, span), top, w, stacked(span) ? 18 : 20, x, y, true);
      button("Use as base", left + span - w, baseY(span, top), w, stacked(span) ? 18 : 20, x, y, !selected);
    }
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
