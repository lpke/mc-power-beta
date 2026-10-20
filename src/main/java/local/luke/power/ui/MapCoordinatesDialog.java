package local.luke.power.ui;

import java.util.function.BiConsumer;
import local.luke.power.map.MapMath;
import org.lwjgl.input.Keyboard;

/** A visit-local coordinate draft leaves the world map active behind its modal input. */
final class MapCoordinatesDialog {
  private final TextInput x = new TextInput("", 10), z = new TextInput("", 10);
  private BiConsumer<Double, Double> accept;
  private String error = "";
  boolean active() { return accept != null; }
  void open(double px, double pz, BiConsumer<Double, Double> accepted) {
    x.setText(Long.toString((long)Math.floor(px))); z.setText(Long.toString((long)Math.floor(pz)));
    x.focused = true; z.focused = false; error = ""; accept = accepted; TextDrag.cancel();
  }
  private void close() { accept = null; x.focused = z.focused = false; TextDrag.cancel(); }
  private void done() {
    try {
      int px = Integer.parseInt(x.text().trim()), pz = Integer.parseInt(z.text().trim());
      if (Math.abs((long)px) > MapMath.BORDER || Math.abs((long)pz) > MapMath.BORDER) throw new NumberFormatException();
      accept.accept((double)px, (double)pz); close();
    } catch (NumberFormatException invalid) { error = "Enter whole coordinates within 32,000,000 blocks."; }
  }
  boolean key(UiScreen screen, char c, int key) {
    if (!active()) return false;
    if (key == Keyboard.KEY_ESCAPE) close();
    else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { screen.click(); done(); }
    else if (key == Keyboard.KEY_TAB) { TextDrag.cancel(); x.focused = !x.focused; z.focused = !x.focused; }
    else { x.key(c,key); z.key(c,key); error = ""; }
    return true;
  }
  private record Bounds(int left, int top, int width) {
    int fieldX() { return left + 28; }
    int fieldWidth() { return width - 40; }
    int buttonWidth() { return (width - 28) / 2; }
    int buttonX(int index) { return left + 12 + index * (buttonWidth() + 4); }
  }
  private Bounds bounds(UiScreen screen) {
    int width = Math.min(340, screen.width - 24);
    return new Bounds((screen.width - width) / 2, Math.max(4, (screen.height - 150) / 2), width);
  }
  boolean press(UiScreen screen, int mx, int my, int button) {
    if (!active()) return false;
    Bounds b = bounds(screen);
    if (button == 0 || button == 1) {
      x.focused = screen.inside(mx,my,b.fieldX(),b.top+32,b.fieldWidth(),18);
      z.focused = screen.inside(mx,my,b.fieldX(),b.top+58,b.fieldWidth(),18);
      TextInput field = x.focused ? x : z.focused ? z : null;
      if (field != null) {
        field.click(mx,button);
        if (button == 1 && Boolean.TRUE.equals(local.luke.power.controls.ControlFeatures.USER_INTERFACE_CONFIG.clearTextFieldWithRightClick)) {
          TextDrag.cancel(); field.setText(""); field.selectAll(); error = "";
        }
      }
      if (button == 0 && screen.inside(mx,my,b.buttonX(0),b.top+118,b.buttonWidth(),20)) { screen.click(); done(); }
      else if (button == 0 && screen.inside(mx,my,b.buttonX(1),b.top+118,b.buttonWidth(),20)) { screen.click(); close(); }
    }
    return true;
  }
  void render(UiScreen screen,int mx,int my) {
    if (!active()) return;
    Bounds b = bounds(screen);
    screen.rectangle(0,0,screen.width,screen.height,0xc0000000);
    screen.rectangle(b.left,b.top,b.left+b.width,b.top+150,0xff252525);
    screen.centeredText("Go to map coordinates",b.top+10,0xffffff);
    screen.text("X",b.left+12,b.top+37,0xcccccc);
    screen.input(x,b.fieldX(),b.top+32,b.fieldWidth(),mx,my,"");
    screen.text("Z",b.left+12,b.top+63,0xcccccc);
    screen.input(z,b.fieldX(),b.top+58,b.fieldWidth(),mx,my,"");
    if (!error.isEmpty()) screen.text(screen.fit(error,b.width-24),b.left+12,b.top+88,0xff8888);
    screen.button("Go to",b.buttonX(0),b.top+118,b.buttonWidth(),20,mx,my,true);
    screen.button("Cancel",b.buttonX(1),b.top+118,b.buttonWidth(),20,mx,my,true);
  }
}
