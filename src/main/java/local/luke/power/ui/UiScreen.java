package local.luke.power.ui;

import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.opengl.GL11;

public abstract class UiScreen extends Screen {
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
    button("", x, y, w, 18, mx, my, enabled);
    int color = !enabled ? 0xff777777 : active ? icon.equals("star") ? 0xffffcc33 : 0xffffff55 : 0xffdddddd;
    int cx = x + w / 2, cy = y + 9;
    String[] pixels = switch (icon) {
      case "star" -> new String[]{"    #    ", "   ###   ", "#########", " ####### ", "  #####  ", " ##   ## ", " #     # "};
      case "edit" -> new String[]{"      ## ", "     #++#", "    #++# ", "   #++#  ", "  #++#   ", " #++#    ", " #o#     ", " ##      ", " #       "};
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
    for (int row = 0; row < pixels.length; row++)
      for (int col = 0; col < pixels[row].length(); col++) {
        char pixel = pixels[row].charAt(col);
        if (pixel == ' ') continue;
        int shade = enabled && pixel == '+' ? 0xffffcc55 : enabled && pixel == 'o' ? 0xffbbaaaa : color;
        fill(cx - 4 + col, cy - 4 + row, cx - 3 + col, cy - 3 + row, shade);
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
    button("", x, y, w, 18, mx, my, true);
    String label = fit(hex, Math.max(5, w - 26));
    int start = x + Math.max(3, (w - textRenderer.getWidth(label) - 16) / 2);
    fill(start, y + 3, start + 12, y + 15, 0xff000000);
    fill(start + 1, y + 4, start + 11, y + 14, 0xff000000 | Integer.parseInt(hex.replace("#", ""), 16));
    text(label, start + 16, y + 5, inside(mx, my, x, y, w, 18) ? 0xffffa0 : 0xe0e0e0);
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
    clip(left + 3, top, span - 6, 18);
    int offset =
        Math.max(0, textRenderer.getWidth(input.text().substring(0, input.cursor())) - (span - 12));
    int x = left + 4 - offset;
    if (input.focused && input.start() != input.end())
      fill(
          x + textRenderer.getWidth(input.text().substring(0, input.start())),
          top + 3,
          x + textRenderer.getWidth(input.text().substring(0, input.end())),
          top + 14,
          0xff335577);
    text(
        input.text().isEmpty() && !input.focused ? placeholder : input.text(),
        x,
        top + 5,
        input.text().isEmpty() && !input.focused ? 0x888888 : 0xffffff);
    if (input.focused && System.currentTimeMillis() / 500 % 2 == 0)
      fill(
          x + textRenderer.getWidth(input.text().substring(0, input.cursor())),
          top + 3,
          x + textRenderer.getWidth(input.text().substring(0, input.cursor())) + 1,
          top + 14,
          0xffffffff);
    unclip();
  }

  protected void tooltip(String message, int mx, int my) {
    int max = Math.min(width - 20, 280);
    java.util.List<String> lines = new java.util.ArrayList<>();
    for (String paragraph : message.split("\n")) {
      String line = "";
      for (String word : paragraph.split(" ")) {
        if (!line.isEmpty() && textRenderer.getWidth(line + " " + word) > max) {
          lines.add(line);
          line = "";
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
