package local.luke.power.ui;

import local.luke.power.input.CreativePaletteBridge;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Compact catalog search with the shared cursor, selection, clipboard and pointer editor. */
public final class CreativeSearchInput extends UiScreen implements CreativePaletteBridge.Search {
  static { CreativePaletteBridge.register(client -> new CreativeSearchInput((Minecraft) client)); }
  private final TextInput search = new TextInput("", 128);
  private final Object host;
  private static final int HEIGHT = 9;
  private int left, top, span;
  private CreativeSearchInput(Minecraft client) { minecraft = client; textRenderer = client.textRenderer; host = client.currentScreen; }
  public String text() { return search.text(); }
  public boolean focused() { return search.focused; }
  public void focus() {
    search.focused = true;
    local.luke.power.input.InventoryTextInput.focus(host, this, true);
    Keyboard.enableRepeatEvents(true);
  }
  public void render(int width, int height, int left, int top, int span, int mx, int my) {
    this.width = width; this.height = height; this.left = left; this.top = top; this.span = span;
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_SCISSOR_BIT | GL11.GL_COLOR_BUFFER_BIT);
    try {
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      fill(left - 1, top - 1, left + span + 1, top + HEIGHT + 1, 0xff373737);
      fill(left, top, left + span, top + HEIGHT, 0xff8b8b8b);
      fill(left, top + HEIGHT, left + span + 1, top + HEIGHT + 1, 0xffffffff);
      fill(left + span, top, left + span + 1, top + HEIGHT, 0xffffffff);
      clip(left + 2, top, span - 4, HEIGHT);
      var view = search.layout(minecraft.currentScreen, left + 3, span - 6, textRenderer::getWidth);
      String value = search.text();
      int from = Math.max(view.start(), search.start()), to = Math.min(view.end(), search.end());
      if (search.focused && from < to) fill(left + 3 + textRenderer.getWidth(value.substring(view.start(), from)), top,
          left + 3 + textRenderer.getWidth(value.substring(view.start(), to)), top + 8, 0xff335577);
      text(value.substring(view.start(), view.end()), left + 3, top, 0xffffff);
      if (search.focused && InterfaceState.uiCursorVisible()) {
        int caret = left + 3 + textRenderer.getWidth(value.substring(view.start(), search.cursor()));
        fill(caret, top, caret + 1, top + 8, 0xffffffff);
      }
    } finally { GL11.glPopAttrib(); }
  }
  public boolean press(int x, int y, int button) {
    if (button != 0 && button != 1) return false;
    search.focused = inside(x, y, left, top, span, HEIGHT);
    local.luke.power.input.InventoryTextInput.focus(host, this, search.focused);
    Keyboard.enableRepeatEvents(search.focused);
    if (search.focused) search.click(x, button);
    return search.focused;
  }
  public boolean key(char character, int key) {
    if (!search.focused) return false;
    if (key == Keyboard.KEY_ESCAPE) { close(); return true; }
    search.key(character, key); return true;
  }
  public void close() {
    search.focused = false; TextDrag.cancel(); Keyboard.enableRepeatEvents(false);
    local.luke.power.input.InventoryTextInput.focus(host, this, false);
  }
}
