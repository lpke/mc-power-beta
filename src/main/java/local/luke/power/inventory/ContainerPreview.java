package local.luke.power.inventory;

import java.util.*;
import local.luke.power.input.*;
import local.luke.power.visual.VisualConfig;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

/** Read-only target snapshot. It never opens a container or sends inventory actions. */
public final class ContainerPreview extends DrawableHelper {
  private static final ContainerPreview INSTANCE = new ContainerPreview();
  private final ItemRenderer renderer = new ItemRenderer();
  public static void render(Minecraft mc) {
    if (!VisualConfig.current().containerPreview || mc.currentScreen != null || mc.world == null || mc.world.isRemote
        || mc.player == null || !Bindings.down(GameplayKeys.PREVIEW) || mc.field_2823 == null) return;
    var hit = mc.field_2823;
    List<Inventory> sources = new ArrayList<>();
    if (hit.field_1983 == class_212.TILE) {
      int x = hit.field_1984, y = hit.field_1985, z = hit.field_1986;
      if (!mc.world.method_239(x,y,z)) return;
      if (mc.world.method_1777(x,y,z) instanceof Inventory inventory) sources.add(inventory);
      if (mc.world.getBlockId(x,y,z) == 54) {
        for (int[] offset : new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
          int nx = x + offset[0], nz = z + offset[1];
          if (mc.world.method_239(nx,y,nz) && mc.world.getBlockId(nx,y,nz) == 54
              && mc.world.method_1777(nx,y,nz) instanceof Inventory second) {
            if (offset[0] < 0 || offset[1] < 0) sources.add(0,second); else sources.add(second);
            break;
          }
        }
      }
    } else if (hit.field_1989 instanceof Inventory inventory) sources.add(inventory);
    if (sources.isEmpty()) return;
    List<ItemStack> stacks = new ArrayList<>();
    for (Inventory source : sources) {
      if (source.size() < 1 || source.size() > 54) return;
      for (int i=0; i<source.size();i++) { ItemStack stack=source.getStack(i); stacks.add(stack == null ? null : stack.clone()); }
    }
    if (stacks.size() > 54) return;
    INSTANCE.draw(mc,stacks);
  }
  private void draw(Minecraft mc, List<ItemStack> stacks) {
    var size = new class_564(mc.options,mc.displayWidth,mc.displayHeight);
    int columns = Math.min(9,stacks.size()), rows=(stacks.size()+columns-1)/columns;
    int x=(size.method_1857()-columns*18)/2, y=Math.max(14,size.method_1858()/2-rows*18-18);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
    try {
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      fill(x-4,y-4,x+columns*18+4,y+rows*18+4,0xee222222);
      GL11.glColor4f(1,1,1,1);
      GL11.glBindTexture(GL11.GL_TEXTURE_2D,mc.textureManager.getTextureId("/gui/inventory.png"));
      for (int i=0;i<stacks.size();i++) drawTexture(x+(i%columns)*18,y+(i/columns)*18,7,83,18,18);
      GL11.glPushMatrix(); GL11.glRotatef(120,1,0,0); class_583.method_1930(); GL11.glPopMatrix(); GL11.glEnable(32826);
      for (int i=0;i<stacks.size();i++) if (stacks.get(i)!=null) {
        int px=x+(i%columns)*18+1, py=y+(i/columns)*18+1;
        renderer.method_1487(mc.textRenderer,mc.textureManager,stacks.get(i),px,py);
        renderer.method_1488(mc.textRenderer,mc.textureManager,stacks.get(i),px,py);
      }
    } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
  }
}
