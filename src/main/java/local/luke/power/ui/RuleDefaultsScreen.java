package local.luke.power.ui;

import local.luke.power.config.RuleDefaults;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Private review draft; no template or world changes until an explicitly accepted durable save. */
public final class RuleDefaultsScreen extends UiScreen {
  private final Screen parent;
  private final RuleDefaults.Review review;
  private final Runnable accepted, cancelled;
  private final String backDestination;
  private final ScrollBar bar = new ScrollBar();
  private boolean expanded;
  private double scroll;
  private String error = "";
  private RuleDefaultsScreen(Screen parent, RuleDefaults.Review review, Runnable accepted, Runnable cancelled, String backDestination) {
    this.parent = parent; this.review = review; this.accepted = accepted; this.cancelled = cancelled;
    this.backDestination = backDestination;
  }
  public static boolean openIfNeeded(Minecraft mc, Screen parent, Runnable accepted, Runnable cancelled, String backDestination) {
    var review = RuleDefaults.review();
    if (!review.required()) {
      if (review.needsAcknowledgement()) {
        try { review.commit(false); }
        catch (Exception failure) {
          var screen = new RuleDefaultsScreen(parent, review, accepted, cancelled, backDestination);
          screen.error = failure.getMessage(); mc.setScreen(screen); return true;
        }
      }
      return false;
    }
    mc.setScreen(new RuleDefaultsScreen(parent, review, accepted, cancelled, backDestination)); return true;
  }
  private int left() { return (width - span()) / 2; }
  private int span() { return Math.min(680, width - 24); }
  private ScrollBar.Track track() { return new ScrollBar.Track(left() + span() - 4, 118,
      Math.max(0, height - 188), review.differences().size() * 38, scroll); }
  public void removed() { bar.release(); }
  private void commit(boolean reset) {
    try {
      review.commit(reset); accepted.run(); minecraft.setScreen(parent);
    } catch (Exception failure) { error = failure.getMessage(); }
  }
  protected void keyPressed(char c, int key) { if (key == Keyboard.KEY_ESCAPE) cancelled.run(); }
  protected void mouseClicked(int x, int y, int button) {
    if (button != 0) return;
    if (inside(x, y, left(), 90, span(), 20)) { click(); expanded = !expanded; return; }
    if (expanded && bar.press(track(), x, y)) return;
    if (expanded && y >= 118 && y < height - 70) {
      int index = (int)((y - 118 + scroll) / 38);
      var changes = review.differences();
      if (index >= 0 && index < changes.size() && changes.get(index).removable()
          && inside(x, y, left() + span() - 68, 118 + index * 38 - (int)scroll + 8, 56, 18)) {
        click(); review.remove(changes.get(index)); return;
      }
    }
    int w = Math.min(108, (span() - 8) / 3), start = width / 2 - (3 * w + 8) / 2;
    if (inside(x, y, start, height - 28, w, 20)) { click(); commit(true); }
    else if (review.compatible() && inside(x, y, start + w + 4, height - 28, w, 20)) { click(); commit(false); }
    else if (inside(x, y, start + 2 * (w + 4), height - 28, w, 20)) { click(); cancelled.run(); }
  }
  public void onMouseEvent() {
    super.onMouseEvent();
    if (expanded) scroll = Math.max(0, Math.min(track().maximum(), scroll - Math.signum(Mouse.getEventDWheel()) * 38));
  }
  public void render(int x, int y, float delta) {
    renderBackground(); fill(0, 0, width, height, 0xb0000000); titleBar("Updated new-world defaults");
    text(fit("Review " + review.base.label + " rules introduced in Power Beta " + review.releases() + ".", span()), left(), 36, 0xffffff);
    text(fit("Reset updates Survival and Cheats templates. Existing worlds stay unchanged.", span()), left(), 51, 0xcccccc);
    text(fit(review.compatible() ? "Keep current retains your rules and accepts this defaults version."
        : "These templates are incompatible. Reset, or go back and use a compatible release.", span()), left(), 66, review.compatible() ? 0xcccccc : 0xffbb88);
    tabButton((expanded ? "v Hide" : "> Show") + " changes (" + review.differences().size() + ")", left(), 90, span(), 20, x, y, true, expanded);
    if (expanded) {
      scroll = bar.drag(track(), y, Mouse.isButtonDown(0));
      clip(left(), 118, span(), Math.max(0, height - 188));
      var changes = review.differences();
      for (int i = 0; i < changes.size(); i++) {
        var change = changes.get(i); int top = 118 + i * 38 - (int)scroll;
        if (top + 38 <= 118 || top >= height - 70) continue;
        fill(left(), top, left() + span() - 10, top + 37, 0x70000000);
        int available = span() - (change.removable() ? 86 : 20);
        text(fit((change.template().equals("cheats") ? "Cheats / " : "Survival / ") + change.label(), available), left() + 6, top + 5, 0xffffff);
        text(fit(change.current() + "  ->  " + change.latest(), available), left() + 6, top + 20, 0xcccccc);
        if (change.removable()) button("Delete", left() + span() - 68, top + 8, 56, 18, x, y, true);
      }
      unclip(); bar.render(this, track());
    }
    String notice = error.isEmpty() ? review.compatible() ? "A verified settings backup is saved before changes." : review.problem() : error;
    text(fit(notice, span()), left(), height - 51, error.isEmpty() ? 0xaaaaaa : 0xff8888);
    int w = Math.min(108, (span() - 8) / 3), start = width / 2 - (3 * w + 8) / 2;
    button("Reset", start, height - 28, w, 20, x, y, true);
    if (review.compatible()) button("Keep current", start + w + 4, height - 28, w, 20, x, y, true);
    button("Back", start + 2 * (w + 4), height - 28, w, 20, x, y, true);
    backTooltip(backDestination, start + 2 * (w + 4), height - 28, w, 20, x, y);
  }
}
