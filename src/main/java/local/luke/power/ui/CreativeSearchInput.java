package local.luke.power.ui;

import local.luke.power.input.CreativePaletteBridge;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Compact catalog search with the shared cursor, selection, clipboard and pointer editor. */
public final class CreativeSearchInput extends UiScreen implements CreativePaletteBridge.Search {
  static { CreativePaletteBridge.register(client -> new CreativeSearchInput((Minecraft) client)); }
  private final TextInput search = new TextInput("", 128);
  private int left, top, span;
  private CreativeSearchInput(Minecraft client) { minecraft = client; textRenderer = client.textRenderer; }
  public String text() { return search.text(); }
  public boolean focused() { return search.focused; }
  public void render(int width, int height, int left, int top, int span, int mx, int my) {
    this.width = width; this.height = height; this.left = left; this.top = top; this.span = span;
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_SCISSOR_BIT | GL11.GL_COLOR_BUFFER_BIT);
    try {
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      fill(left - 1, top - 1, left + span + 1, top + 11, search.focused ? 0xffaaaaaa : 0xff777777);
      fill(left, top, left + span, top + 10, 0xff101010);
      clip(left + 2, top, span - 4, 10);
      var view = search.layout(minecraft.currentScreen, left + 3, span - 6, textRenderer::getWidth);
      String value = search.text();
      int from = Math.max(view.start(), search.start()), to = Math.min(view.end(), search.end());
      if (search.focused && from < to) fill(left + 3 + textRenderer.getWidth(value.substring(view.start(), from)), top + 1,
          left + 3 + textRenderer.getWidth(value.substring(view.start(), to)), top + 9, 0xff335577);
      text(value.isEmpty() && !search.focused ? "Search..." : value.substring(view.start(), view.end()), left + 3, top + 1,
          value.isEmpty() && !search.focused ? 0x888888 : 0xffffff);
      if (search.focused && InterfaceState.uiCursorVisible()) {
        int caret = left + 3 + textRenderer.getWidth(value.substring(view.start(), search.cursor()));
        fill(caret, top + 1, caret + 1, top + 9, 0xffffffff);
      }
    } finally { GL11.glPopAttrib(); }
  }
  public boolean press(int x, int y, int button) {
    search.focused = inside(x, y, left, top, span, 10);
    local.luke.power.input.InventoryTextInput.focus(minecraft.currentScreen, search.focused);
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
    local.luke.power.input.InventoryTextInput.focus(minecraft.currentScreen, false);
  }
}
