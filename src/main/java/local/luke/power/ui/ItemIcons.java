package local.luke.power.ui;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.item.ItemRenderer;
import org.lwjgl.opengl.GL11;

final class ItemIcons {
  private final ItemRenderer renderer=new ItemRenderer();
  void draw(UiScreen screen,Minecraft mc,ItemCatalogue.Entry entry,int x,int y) {
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    GL11.glPushMatrix();
    try {
      GL11.glColor4f(1,1,1,1);
      if(entry.icon()!=null) renderer.method_1487(mc.textRenderer,mc.textureManager,entry.icon(),x,y);
      else {
        // Non-inventory states such as fire still have a native terrain texture.
        int texture=Block.BLOCKS[entry.id()].method_1627(1,Math.max(0,entry.damage()));
        GL11.glBindTexture(GL11.GL_TEXTURE_2D,mc.textureManager.getTextureId("/terrain.png"));
        screen.drawTexture(x,y,(texture%16)*16,(texture/16)*16,16,16);
      }
    } finally {GL11.glPopMatrix();GL11.glPopAttrib();}
  }
}
