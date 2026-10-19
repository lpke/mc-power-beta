package local.luke.power.ui;

import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.opengl.GL11;

public abstract class UiScreen extends Screen {
  protected void click() { minecraft.soundManager.method_2009("random.click", 1, 1); }
  protected void titleBar(String title) {
    fill(0, 0, width, 22, 0x70000000);
    drawCenteredTextWithShadow(textRenderer, title, width / 2, 7, 0xffffff);
  }
  private final java.nio.IntBuffer inputClip = org.lwjgl.BufferUtils.createIntBuffer(16);
  void rectangle(int left,int top,int right,int bottom,int colour) { fill(left,top,right,bottom,colour); }
  protected boolean inside(int x, int y, int left, int top, int width, int height) {
    return x >= left && x < left + width && y >= top && y < top + height;
  }

  protected String fit(String text, int span) {
    if (textRenderer.getWidth(text) <= span) return text;
    while (!text.isEmpty() && textRenderer.getWidth(text + "...") > span)
      text = text.substring(0, text.length() - 1);
    return text + "...";
  }

  protected void text(String s, int x, int y, int color) {
    drawStringWithShadow(textRenderer, s, x, y, color);
  }

  void centeredText(String text, int y, int colour) {
    drawCenteredTextWithShadow(textRenderer, text, width / 2, y, colour);
  }

  protected void button(
      String label, int left, int top, int span, int h, int mx, int my, boolean enabled) {
    button(label, left, top, span, h, mx, my, enabled, -1);
  }

  protected void button(String label, int left, int top, int span, int h,
      int mx, int my, boolean enabled, int labelColour) {
    int w = Math.max(10, span);
    boolean hovered = inside(mx, my, left, top, w, h);
    int v = 46 + (enabled ? hovered ? 2 : 1 : 0) * 20;
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.textureManager.getTextureId("/gui/gui.png"));
    GL11.glColor4f(1, 1, 1, 1);
    // Vanilla crops the bottom of buttons shorter than 20 pixels and loses a
    // column on odd widths. Tile the interior while retaining all four edges.
    for (int dy = 0; dy < h; ) {
      int sh = dy == 0 || dy == h - 2 ? 2 : Math.min(16, h - 2 - dy);
      int sy = dy == 0 ? 0 : dy == h - 2 ? 18 : 2;
      for (int dx = 0; dx < w; ) {
        int sw = dx == 0 || dx == w - 2 ? 2 : Math.min(196, w - 2 - dx);
        int sx = dx == 0 ? 0 : dx == w - 2 ? 198 : 2;
        drawTexture(left + dx, top + dy, sx, v + sy, sw, sh);
        dx += sw;
      }
      dy += sh;
    }
    drawCenteredTextWithShadow(textRenderer, label, left + w / 2, top + (h - 8) / 2,
        !enabled ? 0xa0a0a0 : labelColour >= 0 ? labelColour : hovered ? 0xffffa0 : 0xe0e0e0);
  }

  protected void tabButton(String label, int x, int y, int w, int mx, int my, boolean selected) {
    tabButton(label, x, y, w, 18, mx, my, true, selected);
  }
  protected void tabButton(String label, int x, int y, int w, int h, int mx, int my, boolean enabled, boolean selected) {
    boolean hover = inside(mx, my, x, y, w, h);
    button(label, x, y, w, h, mx, my, enabled, hover ? 0xffffa0 : selected ? 0xffffff : 0xb8b8b8);
    fill(x + 2, y + h - 3, x + w - 2, y + h - 1, selected ? 0xffdddddd : hover ? 0xffaaaaaa : 0xff909090);
  }

  protected void chevron(int x, int y, boolean down, int colour) {
    for (int i = 0; i < 3; i++) {
      if (down) { fill(x + i, y + i, x + i + 1, y + i + 1, colour); fill(x + 4 - i, y + i, x + 5 - i, y + i + 1, colour); }
      else { fill(x + i, y + i, x + i + 1, y + i + 1, colour); fill(x + i, y + 4 - i, x + i + 1, y + 5 - i, colour); }
    }
  }

  protected void trackButton(String label, boolean previous, int x, int y, int w, int mx, int my) {
    button("", x, y, w, 18, mx, my, true);
    int colour = inside(mx, my, x, y, w, 18) ? 0xffffa0 : 0xe0e0e0;
    int textWidth = textRenderer.getWidth(label);
    int start = x + (w - (label.isEmpty() ? 7 : textWidth + 11)) / 2;
    int iconX = previous || label.isEmpty() ? start : start + textWidth + 4;
    if (!label.isEmpty()) text(label, previous ? start + 11 : start, y + 5, colour);
    // Pixel-drawn skip symbol remains legible with any font or texture pack.
    for (int row = 0; row < 7; row++) {
      int length = 4 - Math.abs(3 - row);
      int edge = previous ? iconX + 6 : iconX;
      fill(previous ? edge - length : edge, y + 5 + row,
          previous ? edge : edge + length, y + 6 + row, 0xff000000 | colour);
    }
    int bar = previous ? iconX : iconX + 6;
    fill(bar, y + 5, bar + 1, y + 12, 0xff000000 | colour);
  }

  protected void iconButton(String icon, int x, int y, int w, int mx, int my, boolean enabled) {
    iconButton(icon, x, y, w, mx, my, enabled, false);
  }

  protected void iconButton(String icon, int x, int y, int w, int mx, int my, boolean enabled, boolean active) {
    iconButton(icon, x, y, w, 18, mx, my, enabled, active);
  }

  protected void iconButton(String icon, int x, int y, int w, int h, int mx, int my, boolean enabled, boolean active) {
    button("", x, y, w, h, mx, my, enabled);
    int color = !enabled ? 0xff777777 : icon.startsWith("eye") ? 0xffffffff : active ? icon.equals("star") ? 0xffffcc33 : 0xffffff55 : 0xffdddddd;
    int cx = x + w / 2;
    String[] pixels = switch (icon) {
      case "eye" -> new String[]{"   ###   ", " ##   ## ", "#   #   #", "#  ###  #", "#   #   #", " ##   ## ", "   ###   "};
      case "eyeClosed" -> new String[]{"         ", "         ", "#       #", " #     # ", "  #####  ", " #  #  # ", "         "};
      case "collapse" -> new String[]{"  #   #  ", "   # #   ", "    #    ", "         ", "         ", "    #    ", "   # #   ", "  #   #  "};
      case "expand" -> new String[]{"    #    ", "   # #   ", "  #   #  ", "         ", "  #   #  ", "   # #   ", "    #    "};
      case "check" -> new String[]{"        #", "       ##", "      ## ", "#    ##  ", "##  ##   ", " ####    ", "  ##     "};
      case "star" -> new String[]{"    #    ", "   ###   ", "#########", " ####### ", "  #####  ", " ##   ## ", " #     # "};
      case "music" -> new String[]{"   ######", "   #    #", "   #    #", "   #    #", " ###  ###", "#### ####", " ##   ## "};
      case "remove" -> new String[]{" #     # ", "  #   #  ", "   # #   ", "    #    ", "   # #   ", "  #   #  ", " #     # "};
      case "play" -> new String[]{"  #      ", "  ###    ", "  #####  ", "  #######", "  #####  ", "  ###    ", "  #      "};
      case "pause" -> new String[]{" ##  ##  ", " ##  ##  ", " ##  ##  ", " ##  ##  ", " ##  ##  ", " ##  ##  ", " ##  ##  "};
      case "reload" -> new String[]{"  ####  #", " #    # #", "#      ##", "#    ####", "#        ", " #    #  ", "  ####   "};
      case "speaker" -> new String[]{"   #   # ", "  ## #  #", "####  # #", "####  # #", "####  # #", "  ## #  #", "   #   # "};
      case "up" -> new String[]{"    #    ", "   ###   ", "  #####  ", "    #    ", "    #    ", "    #    ", "         "};
      case "down" -> new String[]{"    #    ", "    #    ", "    #    ", "  #####  ", "   ###   ", "    #    ", "         "};
      case "controls" -> new String[]{"#########", "# # # # #", "#########", "# # # # #", "#########", "# ##### #", "#########"};
      default -> new String[]{" #   #  ", "########", " #   #  ", "   #   #", "########", "   #   #", "        "};
    };
    int top = y + (h - pixels.length) / 2;
    for (int row = 0; row < pixels.length; row++)
      for (int col = 0; col < pixels[row].length(); col++) {
        char pixel = pixels[row].charAt(col);
        if (pixel == ' ') continue;
        int shade = enabled && pixel == '+' ? 0xffffcc55 : enabled && pixel == 'o' ? 0xffbbaaaa : color;
        fill(cx - 4 + col, top + row, cx - 3 + col, top + 1 + row, shade);
      }
  }

  protected void musicToggle(boolean included, int x, int y, int mx, int my, boolean enabled) {
    iconButton("music", x, y, 20, mx, my, enabled);
    if (!included) for (int d = 0; d < 10; d++)
      fill(x + 5 + d, y + 13 - d, x + 7 + d, y + 14 - d, 0xffff5555);
  }

  void scrollbar(int x, int top, int height, int y, int thumb, boolean active) {
    fill(x, top, x + 4, top + height, 0xff222222);
    fill(x, y, x + 4, y + thumb, active ? 0xffaaaaaa : 0xff777777);
  }

  protected void colourButton(String hex, int x, int y, int w, int mx, int my) {
    colourButton(hex, 100, x, y, w, mx, my);
  }

  protected void colourButton(String hex, double opacity, int x, int y, int w, int mx, int my) {
    button("", x, y, w, 18, mx, my, true);
    String label = w >= 44 ? fit(local.luke.power.config.ColorSettings.label(hex, opacity), w - 26) : "";
    int total = label.isEmpty() ? 12 : textRenderer.getWidth(label) + 16;
    int start = x + Math.max(3, (w - total) / 2);
    fill(start, y + 3, start + 12, y + 15, 0xff000000);
    int rgb;
    try { rgb = local.luke.power.light.LightSettings.rgb(hex); }
    catch (IllegalArgumentException invalidDraft) { rgb = 0x777777; }
    colorSwatch(rgb, opacity, start + 1, y + 4, 10, 10);
    if (!label.isEmpty()) text(label, start + 16, y + 5, inside(mx, my, x, y, w, 18) ? 0xffffa0 : 0xe0e0e0);
  }

  protected void colorSwatch(int rgb, double opacity, int x, int y, int w, int h) {
    double alpha = Math.max(0, Math.min(1, opacity / 100));
    if (alpha == 1) { fill(x, y, x + w, y + h, 0xff000000 | rgb); return; }
    for (int dy = 0; dy < h; dy += 4) for (int dx = 0; dx < w; dx += 4) {
      int background = ((dx / 4 + dy / 4) & 1) == 0 ? 0x888888 : 0xcccccc;
      int color = 0xff000000;
      for (int shift = 0; shift <= 16; shift += 8)
        color |= (int)Math.round((rgb >> shift & 255) * alpha + (background >> shift & 255) * (1 - alpha)) << shift;
      fill(x + dx, y + dy, x + Math.min(w, dx + 4), y + Math.min(h, dy + 4), color);
    }
  }

  protected void slider(String label, int x, int y, int w, int mx, int my, double fraction) {
    button("", x, y, w, 18, mx, my, false);
    int thumb = x + (int) Math.round(Math.max(0, Math.min(1, fraction)) * Math.max(0, w - 8));
    button("", thumb, y, 8, 18, mx, my, true);
    drawCenteredTextWithShadow(textRenderer, fit(label, w - 8), x + w / 2, y + 5,
        inside(mx, my, x, y, w, 18) ? 0xffffa0 : 0xffffff);
  }

  protected void clip(int left, int top, int span, int h) {
    GL11.glEnable(GL11.GL_SCISSOR_TEST);
    double sx = (double) minecraft.displayWidth / width,
        sy = (double) minecraft.displayHeight / height;
    GL11.glScissor(
        (int) Math.ceil(left * sx),
        (int) Math.ceil((height - top - h) * sy),
        Math.max(0, (int) Math.floor(span * sx)),
        Math.max(0, (int) Math.floor(h * sy)));
  }

  protected void unclip() {
    GL11.glDisable(GL11.GL_SCISSOR_TEST);
  }

  protected void input(
      TextInput input, int left, int top, int span, int mx, int my, String placeholder) {
    fill(left - 1, top - 1, left + span + 1, top + 19, input.focused ? 0xffaaaaaa : 0xff555555);
    fill(left, top, left + span, top + 18, 0xff101010);
    // Inputs also render inside clipped scrolling lists. Preserve their outer clip.
    GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
    boolean clipped = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
    int clipX = 0, clipY = 0, clipRight = 0, clipTop = 0;
    if (clipped) {
      GL11.glGetInteger(GL11.GL_SCISSOR_BOX, inputClip);
      clipX = inputClip.get(0); clipY = inputClip.get(1);
      clipRight = clipX + inputClip.get(2); clipTop = clipY + inputClip.get(3);
    }
    clip(left + 3, top, span - 6, 18);
    if (clipped) {
      GL11.glGetInteger(GL11.GL_SCISSOR_BOX, inputClip);
      int x1 = Math.max(clipX, inputClip.get(0)), y1 = Math.max(clipY, inputClip.get(1));
      int x2 = Math.min(clipRight, inputClip.get(0) + inputClip.get(2));
      int y2 = Math.min(clipTop, inputClip.get(1) + inputClip.get(3));
      GL11.glScissor(x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1));
    }
    var view = input.layout(minecraft.currentScreen, left + 4, span - 8, textRenderer::getWidth);
    String value = input.text();
    int x = left + 4;
    int from = Math.max(view.start(), input.start()), to = Math.min(view.end(), input.end());
    if (input.focused && from < to)
      fill(x + textRenderer.getWidth(value.substring(view.start(), from)), top + 3,
          x + textRenderer.getWidth(value.substring(view.start(), to)), top + 14, 0xff335577);
    text(value.isEmpty() && !input.focused ? placeholder : value.substring(view.start(), view.end()),
        x, top + 5, value.isEmpty() && !input.focused ? 0x888888 : 0xffffff);
    if (input.focused && InterfaceState.uiCursorVisible()) {
      int caret = x + textRenderer.getWidth(value.substring(view.start(), input.cursor()));
      fill(caret, top + 3, caret + 1, top + 14, 0xffffffff);
    }
    GL11.glPopAttrib();
  }

  protected void foregroundTooltip(String message, int mx, int my) {
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
    try {
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_LIGHTING);
      tooltip(message, mx, my);
    } finally { GL11.glPopAttrib(); }
  }

  protected void tooltip(String message, int mx, int my) {
    int max = Math.max(1, Math.min(width - 20, 280));
    java.util.List<String> lines = new java.util.ArrayList<>();
    for (String paragraph : message.split("\n")) {
      String line = "";
      for (String word : paragraph.split(" ")) {
        if (!line.isEmpty() && textRenderer.getWidth(line + " " + word) > max) {
          lines.add(line);
          line = "";
        }
        while (word.length() > 1 && textRenderer.getWidth(word) > max) {
          int end = 1;
          while (end < word.length() && textRenderer.getWidth(word.substring(0, end + 1)) <= max) end++;
          lines.add(word.substring(0, end));
          word = word.substring(end);
        }
        line += (line.isEmpty() ? "" : " ") + word;
      }
      lines.add(line);
    }
    int w = lines.stream().mapToInt(textRenderer::getWidth).max().orElse(0),
        h = lines.size() * 11 + 8;
    int x = Math.max(5, Math.min(mx + 10, width - w - 10)),
        y = Math.max(5, Math.min(my + 12, height - h - 5));
    fill(x - 4, y - 4, x + w + 4, y + h - 4, 0xf0101010);
    int lineY = y;
    for (String line : lines) {
      text(line, x, lineY, 0xeeeeee);
      lineY += 11;
    }
  }
}
