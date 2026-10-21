package local.luke.power.status;

import java.util.ArrayList;
import java.util.List;
import local.luke.power.input.TweakIndicators;
import net.minecraft.class_564;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public final class ActiveTweaks {
  private record Frame(List<String> lines, StatusLayout layout, int textWidth, int argb) {}
  private ActiveTweaks() {}
  public static List<String> lines(StatusSettings settings) {
    List<String> lines = new ArrayList<>();
    if (!settings.enabled || settings.opacity == 0) return lines;
    for (var tweak : TweakIndicators.Tweak.values()) if (settings.includes(tweak)) {
      String label = TweakIndicators.label(tweak);
      if (label != null) lines.add(label);
    }
    return lines;
  }
  private static boolean visible(Minecraft mc) {
    return mc.world != null && mc.player != null && !mc.player.dead && mc.player.health > 0
        && !mc.options.hideHud && (mc.currentScreen == null || mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen);
  }
  private static Frame frame(Minecraft mc, int width, int height, StatusSettings settings, List<String> lines) {
    int textWidth = 0;
    for (int i = 0; i < lines.size(); i++) {
      String label = lines.get(i);
      while (mc.textRenderer.getWidth(label) > width - 16 && !label.isEmpty()) label = label.substring(0, label.length() - 1);
      lines.set(i, label);
      textWidth = Math.max(textWidth, mc.textRenderer.getWidth(label));
    }
    StatusLayout layout = StatusLayout.at(width, height, textWidth, lines.size(), settings);
    return new Frame(lines, layout, textWidth, settings.argb());
  }
  public static StatusLayout.Bounds bounds(Minecraft mc, int width, int height) {
    if (!visible(mc)) return null;
    StatusSettings settings = StatusConfig.current();
    List<String> lines = lines(settings);
    if (lines.isEmpty()) return null;
    Frame frame = frame(mc, width, height, settings, lines);
    return frame.layout.bounds(frame.textWidth, frame.lines.size());
  }
  public static void render(Minecraft mc) {
    if (!visible(mc)) return;
    StatusSettings settings = StatusConfig.current();
    List<String> lines = lines(settings);
    if (lines.isEmpty()) return;
    var size = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
    Frame frame = frame(mc, size.method_1857(), size.method_1858(), settings, lines);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glAlphaFunc(GL11.GL_GREATER, 0);
      for (int i = 0; i < frame.lines.size(); i++) {
        String line = frame.lines.get(i);
        mc.textRenderer.drawWithShadow(line, frame.layout.lineX(frame.textWidth, mc.textRenderer.getWidth(line)),
            frame.layout.y() + i * 10, frame.argb);
      }
    } finally { GL11.glPopAttrib(); }
  }
}
