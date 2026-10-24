package local.luke.power.ui;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.item.ItemRenderer;
import org.lwjgl.opengl.GL11;

final class ItemIcons {
  private final ItemRenderer renderer=new ItemRenderer();
  void draw(UiScreen screen,Minecraft mc,ItemCatalogue.Entry entry,int x,int y) {
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glPushMatrix();
    try {
      GL11.glColor4f(1,1,1,1);
      if(entry.icon()!=null) GuiItems.draw(renderer,mc,entry.icon(),x,y,false);
      else {
        // Non-inventory states such as fire still have a native terrain texture.
        GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
        int texture=Block.BLOCKS[entry.id()].method_1627(1,Math.max(0,entry.damage()));
        GL11.glBindTexture(GL11.GL_TEXTURE_2D,mc.textureManager.getTextureId("/terrain.png"));
        screen.drawTexture(x,y,(texture%16)*16,(texture/16)*16,16,16);
      }
    } finally {GL11.glPopMatrix();GL11.glPopAttrib();}
  }
}
