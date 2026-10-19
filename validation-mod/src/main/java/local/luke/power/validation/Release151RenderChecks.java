package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.*;
import local.luke.power.render.RenderControls;
import local.luke.power.status.*;
import local.luke.power.ui.*;
import local.luke.power.visual.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

final class Release151RenderChecks {
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("text")) { text(mc); return; }
    (AccessRules.of(mc.world.method_262())).allAllowed(true);
    var s = VisualConfig.copy(); s.spectatorFullBright = true; VisualConfig.preview(s);
    CreativeVehicleChecks.mode(mc, "SPECTATOR"); RenderControls.tick(mc);
    mc.world.method_229(3, 105, 3, Block.STONE.id);
    float before = mc.world.method_1784(3, 105, 3, 0);
    int light = mc.world.method_255(3, 105, 3);
    RenderControls.beginRender();
    try {
      check(mc.world.method_1784(3, 105, 3, 0) == .2f, "opaque AO sample lost");
      check(mc.world.method_1784(3, 106, 3, 0) == 1, "air not fully lit");
      check(mc.world.method_1784(3, 105, 3, 15) == 1, "emission was shaded");
      check(mc.world.method_255(3, 105, 3) == light, "simulation light changed");
    } finally { RenderControls.endRender(); }
    check(mc.world.method_1784(3, 105, 3, 0) == before, "render brightness escaped scope");
    CreativeVehicleChecks.mode(mc, "CREATIVE"); RenderControls.tick(mc);
    log("PASS full bright keeps opaque AO, emission and simulation light separate");
  }
  private static void text(Minecraft mc) throws Exception {
    var old = StatusConfig.copy();
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glLoadIdentity();
    GL11.glOrtho(0, mc.displayWidth, mc.displayHeight, 0, -1, 1);
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix(); GL11.glLoadIdentity();
    try {
      GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_FOG);
      GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glDisable(GL11.GL_ALPHA_TEST);
      MenuPreferences.syncPresentation();
      var settings = StatusConfig.copy();
      check(MenuPreferences.debugTextColor().equals("#FFFFFF") && settings.textColor.equals("#FFFFFF") && settings.opacity == 100, "white settings were not loaded");
      for (int color : new int[]{InterfaceState.debugTextColor, settings.argb(), -1}) {
        GL11.glClearColor(0, 0, 0, 1); GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        mc.textRenderer.drawWithShadow("WHITE HUD CHAT", 10, 10, color);
        ByteBuffer pixels = BufferUtils.createByteBuffer(160 * 20 * 4);
        GL11.glReadPixels(0, mc.displayHeight - 30, 160, 20, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        int white = 0;
        for (int i = 0; i < pixels.capacity(); i += 4)
          if ((pixels.get(i)&255)==255 && (pixels.get(i+1)&255)==255 && (pixels.get(i+2)&255)==255) white++;
        check(white > 50, "opaque white text is dull: " + color + " white pixels=" + white);
      }
      log("PASS F3, active tweaks and chat colors render opaque white pixels");
    } finally {
      StatusConfig.preview(old); MenuPreferences.previewDebugColor(MenuPreferences.current().debugTextColor);
      GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopAttrib();
    }
  }
}
