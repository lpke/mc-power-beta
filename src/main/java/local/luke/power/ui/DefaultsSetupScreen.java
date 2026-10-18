package local.luke.power.ui;

import local.luke.power.config.DefaultBases;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

/** Shown before the title only for installations that have not chosen a setup. */
public final class DefaultsSetupScreen extends UiScreen {
  private final Screen title;
  private String error = "";
  private boolean restart;
  private final HoverDelay hoverDelay = new HoverDelay();
  public DefaultsSetupScreen(Screen title) { this.title = title; }
  private int span() { return Math.min(380, width - 32); }
  private int rowTop(DefaultBases.Base base) { return 70 + base.ordinal() * 28; }
  public void tick() { hoverDelay.tick(); }
  public void render(int x, int y, float delta) {
    renderBackground(); titleBar("Welcome to Power Beta");
    centeredText("Choose your starting defaults.", 44, 0xffffff);
    centeredText(fit("You can change these later in Options > Defaults.", width - 24), 180, 0xaaaaaa);
    if (restart) {
      centeredText("Saved. Restart to apply every change.", 100, 0xffffff);
      button("Quit", (width - span()) / 2, 126, span(), 20, x, y, true);
      return;
    }
    DefaultBases.Base hovered = null;
    for (var base : DefaultBases.Base.values()) {
      int top = rowTop(base);
      button(base.label, (width - span()) / 2, top, span(), 20, x, y, true);
      if (inside(x, y, (width - span()) / 2, top, span(), 20)) hovered = base;
    }
    if (!error.isEmpty()) centeredText(fit(error, width - 24), 198, 0xff8888);
    if (hoverDelay.ready(hovered == null ? "" : hovered.id)) foregroundTooltip(hovered.description, x, y);
  }
  protected void mouseClicked(int x, int y, int button) {
    if (button != 0) return;
    if (restart) { if (inside(x, y, (width - span()) / 2, 126, span(), 20)) { click(); minecraft.scheduleStop(); } return; }
    for (var base : DefaultBases.Base.values()) if (inside(x, y, (width - span()) / 2, rowTop(base), span(), 20)) {
      click();
      try {
        restart = DefaultBases.load(base, minecraft, FabricLoader.getInstance().getGameDir());
        if (!restart) minecraft.setScreen(title);
      } catch (Exception failure) { error = failure.getMessage(); }
      return;
    }
  }
  protected void keyPressed(char c, int key) {}
}
