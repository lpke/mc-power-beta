package local.luke.power.offhand;

import local.luke.power.ui.GuiItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.item.ItemRenderer;
import org.lwjgl.opengl.GL11;

/** Shared live/editor rendering, with the slot's upper-left supplied by the HUD layout. */
public final class OffhandHud extends DrawableHelper {
  private static final OffhandHud INSTANCE = new OffhandHud();
  private final ItemRenderer items = new ItemRenderer();
  private OffhandHud() {}
  public static boolean visible(Minecraft mc) { return mc.player != null && Offhand.visible(mc.player) && Offhand.get(mc.player.inventory) != null
      &&!(mc.player instanceof local.luke.power.input.PlayerMode mode&&mode.power$spectator()); }
  public static void render(Minecraft mc) {
    if (!visible(mc) || mc.options.hideHud) return;
    var scaled = new net.minecraft.class_564(mc.options, mc.displayWidth, mc.displayHeight);
    int x = scaled.method_1857() / 2 - 120, y = scaled.method_1858() - 23;
    if (mc.inGameHud instanceof local.luke.power.ui.StatusHudLayout layout) {
      if (!layout.power$offhandVisible()) return;
      x = layout.power$offhandX(); y = layout.power$offhandY();
    }
    draw(mc, x, y);
  }
  public static void draw(Minecraft mc,int x,int y) { INSTANCE.render(mc,x,y); }
  public static void frame(Minecraft mc, int x, int y, boolean right) {
    GL11.glColor4f(1, 1, 1, 1);
    mc.textureManager.bindTexture(mc.textureManager.getTextureId("/assets/powerbeta/textures/hud/offhand-" + (right ? "right" : "left") + ".png"));
    GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    var vertices = net.minecraft.client.render.Tessellator.INSTANCE;
    vertices.startQuads();
    vertices.vertex(x, y + 24, 0, 0, 1); vertices.vertex(x + 29, y + 24, 0, 1, 1);
    vertices.vertex(x + 29, y, 0, 1, 0); vertices.vertex(x, y, 0, 0, 0); vertices.draw();
  }
  private void render(Minecraft mc,int x,int y) {
    if(!visible(mc))return;
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_CURRENT_BIT|GL11.GL_LIGHTING_BIT|GL11.GL_TEXTURE_BIT|GL11.GL_COLOR_BUFFER_BIT);
    GL11.glPushMatrix();
    try {
      boolean right = x >= new net.minecraft.class_564(mc.options, mc.displayWidth, mc.displayHeight).method_1857() / 2;
      frame(mc, x, y, right);

      var item=Offhand.get(mc.player.inventory);
      if(item!=null) {
        GuiItems.draw(items,mc,item,x+(right?10:3),y+4,true);
      }
    } finally { GL11.glPopMatrix();GL11.glPopAttrib(); }
  }
}
