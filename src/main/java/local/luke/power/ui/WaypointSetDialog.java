package local.luke.power.ui;

import java.util.function.Consumer;
import local.luke.power.map.Waypoint;
import org.lwjgl.input.Keyboard;

/** The same set-name prompt serves store actions and isolated waypoint editor drafts. */
final class WaypointSetDialog {
  private final TextInput input = new TextInput("", 48);
  private Consumer<String> create;
  private String error = "";
  boolean active() { return create != null; }
  void open(Consumer<String> accepted) {
    create = accepted; error = ""; input.setText(""); input.focused = true; TextDrag.cancel();
  }
  private void close() { create = null; input.focused = false; TextDrag.cancel(); }
  private void accept() {
    try { String name = input.text().strip(); Waypoint.text(name, 48); create.accept(name); close(); }
    catch (IllegalArgumentException failure) { error = failure.getMessage(); }
  }
  boolean key(UiScreen screen, char c, int key) {
    if (!active()) return false;
    if (key == Keyboard.KEY_ESCAPE) close();
    else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { screen.click(); accept(); }
    else { input.key(c, key); error = ""; }
    return true;
  }
  boolean press(UiScreen screen, int x, int y, int button) {
    if (!active()) return false;
    if (button != 0) return true;
    int width = Math.min(300, screen.width - 24), left = (screen.width - width) / 2, top = screen.height / 2;
    input.focused = screen.inside(x, y, left + 10, top - 10, width - 20, 18); input.click(x, button);
    if (screen.inside(x, y, left + 10, top + 34, (width - 24) / 2, 20)) { screen.click(); accept(); }
    else if (screen.inside(x, y, screen.width / 2 + 2, top + 34, (width - 24) / 2, 20)) { screen.click(); close(); }
    return true;
  }
  void render(UiScreen screen, int mx, int my) {
    if (!active()) return;
    int width = Math.min(300, screen.width - 24), left = (screen.width - width) / 2, top = screen.height / 2;
    screen.rectangle(0, 0, screen.width, screen.height, 0xc0000000);
    screen.rectangle(left, top - 36, left + width, top + 64, 0xff252525);
    screen.centeredText("New waypoint set", top - 28, 0xffffff);
    screen.input(input, left + 10, top - 10, width - 20, mx, my, "Set name");
    if (!error.isEmpty()) screen.text(screen.fit(error, width - 20), left + 10, top + 14, 0xff8888);
    screen.button("Create", left + 10, top + 34, (width - 24) / 2, 20, mx, my, true);
    screen.button("Cancel", screen.width / 2 + 2, top + 34, (width - 24) / 2, 20, mx, my, true);
  }
}
