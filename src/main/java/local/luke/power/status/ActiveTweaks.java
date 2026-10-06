package local.luke.power.status;

import java.util.ArrayList;
import java.util.List;
import local.luke.power.input.TweakIndicators;
import net.minecraft.class_564;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public final class ActiveTweaks {
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
  public static void render(Minecraft mc) {
    if (mc.world == null || mc.player == null || mc.player.dead || mc.player.health <= 0
        || mc.options.hideHud || mc.currentScreen != null) return;
    StatusSettings settings = StatusConfig.current();
    List<String> lines = lines(settings);
    if (lines.isEmpty()) return;
    var size = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
    int width = size.method_1857(), height = size.method_1858(), textWidth = 0;
    for (int i = 0; i < lines.size(); i++) {
      String label = lines.get(i);
      while (mc.textRenderer.getWidth(label) > width - 16 && !label.isEmpty()) label = label.substring(0, label.length() - 1);
      lines.set(i, label);
      textWidth = Math.max(textWidth, mc.textRenderer.getWidth(label));
    }
    StatusLayout layout = StatusLayout.at(width, height, textWidth, lines.size(), settings);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glAlphaFunc(GL11.GL_GREATER, 0);
      for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i);
        mc.textRenderer.drawWithShadow(line, layout.lineX(textWidth, mc.textRenderer.getWidth(line)),
            layout.y() + i * 10, settings.argb());
      }
    } finally { GL11.glPopAttrib(); }
  }
}
