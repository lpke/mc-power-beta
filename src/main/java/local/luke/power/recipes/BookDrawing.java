package local.luke.power.recipes;

import java.util.*;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

/** Native item/font rendering with pinned recipe-book textures. */
public final class BookDrawing extends DrawableHelper {
  private static final String ROOT="/assets/powerbeta/textures/recipes/";
  private static final java.nio.FloatBuffer ITALIC=org.lwjgl.BufferUtils.createFloatBuffer(16)
      .put(new float[]{1,0,0,0,-.25f,1,0,0,0,0,1,0,1,0,0,1}).flip();
  private final ItemRenderer renderer=new ItemRenderer();
  public void image(Minecraft mc,String name,int x,int y,int width,int height) {
    image(mc,name,x,y,width,height,0,0,width,height,width,height);
  }
  public void image(Minecraft mc,String name,int x,int y,int width,int height,int u,int v,int sw,int sh,int tw,int th) {
    GL11.glColor4f(1,1,1,1);GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_DEPTH_TEST);
    mc.textureManager.bindTexture(mc.textureManager.getTextureId(ROOT+name+".png"));
    Tessellator t=Tessellator.INSTANCE;t.startQuads();
    t.vertex(x,y+height,zOffset,u/(double)tw,(v+sh)/(double)th);t.vertex(x+width,y+height,zOffset,(u+sw)/(double)tw,(v+sh)/(double)th);
    t.vertex(x+width,y,zOffset,(u+sw)/(double)tw,v/(double)th);t.vertex(x,y,zOffset,u/(double)tw,v/(double)th);t.draw();
  }
  public void panel(Minecraft mc,int x,int y,int width,int height) {
    int[] a={0,4,width-4,width},b={0,4,height-4,height},s={0,4,28,32};
    for(int row=0;row<3;row++)for(int col=0;col<3;col++)
      image(mc,"overlay_recipe",x+a[col],y+b[row],a[col+1]-a[col],b[row+1]-b[row],s[col],s[row],s[col+1]-s[col],s[row+1]-s[row],32,32);
  }
  public void item(Minecraft mc,ItemStack item,int x,int y,float scale,boolean count) {
    if(item==null)return;
    GL11.glPushMatrix();GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_CURRENT_BIT|GL11.GL_LIGHTING_BIT);
    try {
      GL11.glTranslatef(x,y,0);GL11.glScalef(scale,scale,1);GL11.glColor4f(1,1,1,1);
      GL11.glPushMatrix();GL11.glRotatef(120,1,0,0);net.minecraft.class_583.method_1930();GL11.glPopMatrix();
      GL11.glEnable(org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL);GL11.glEnable(GL11.GL_DEPTH_TEST);
      renderer.method_1487(mc.textRenderer,mc.textureManager,item,0,0);
      if(count)renderer.method_1488(mc.textRenderer,mc.textureManager,item,0,0);
    }finally{GL11.glPopAttrib();GL11.glPopMatrix();}
  }
  public void rectangle(int x,int y,int width,int height,int color){fill(x,y,x+width,y+height,color);}
  public void decorations(Minecraft mc,ItemStack item,int x,int y) {
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try{renderer.method_1488(mc.textRenderer,mc.textureManager,item,x,y);}finally{GL11.glPopAttrib();}
  }
  public void search(Minecraft mc,Object owner,TextInput input,RecipeBookLayout layout) {
    var bounds=layout.search();int left=bounds.x()+4,top=bounds.y()+3;
    rectangle(bounds.x(),bounds.y(),bounds.width(),bounds.height(),input.focused?0xffffffff:0xffa0a0a0);
    rectangle(bounds.x()+1,bounds.y()+1,bounds.width()-2,bounds.height()-2,0xff000000);
    var view=input.layout(owner,left,bounds.width()-8,mc.textRenderer::getWidth);String value=input.text();
    int from=Math.max(view.start(),input.start()),to=Math.min(view.end(),input.end());
    if(input.focused&&from<to)TextSelection.background(left+mc.textRenderer.getWidth(value.substring(view.start(),from)),top-1,
        left+mc.textRenderer.getWidth(value.substring(view.start(),to)),top+9);
    if(value.isEmpty()&&!input.focused) {
      GL11.glPushMatrix();
      try{GL11.glTranslatef(left,top,0);GL11.glMultMatrix(ITALIC);mc.textRenderer.drawWithShadow("Search...",0,0,0xaaaaaa);}
      finally{GL11.glPopMatrix();}
    }else mc.textRenderer.drawWithShadow(value.substring(view.start(),view.end()),left,top,0xffffff);
    if(input.focused&&InterfaceState.uiCursorVisible()) {
      int caret=left+mc.textRenderer.getWidth(value.substring(view.start(),input.cursor()));
      if(input.cursor()<value.length()||value.length()>=50)rectangle(caret,top-1,1,10,0xffd0d0d0);
      else mc.textRenderer.drawWithShadow("_",caret,top,0xffffff);
    }
  }
  public void tooltip(Minecraft mc,String message,int mx,int my,int screenWidth,int screenHeight) {
    List<String> lines=Arrays.asList(message.split("\n"));int width=lines.stream().mapToInt(mc.textRenderer::getWidth).max().orElse(0);
    int x=mx+12,y=my-12,height=lines.size()*10-2;
    if(x+width+6>screenWidth)x=mx-width-16;if(y+height+6>screenHeight)y=screenHeight-height-6;
    x=Math.max(4,x);y=Math.max(4,y);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_LIGHTING);
    fill(x-3,y-4,x+width+3,y+height+4,0xf0100010);fill(x-4,y-3,x+width+4,y+height+3,0xf0100010);
    fill(x-3,y-3,x+width+3,y-2,0xff5000ff);fill(x-3,y+height+2,x+width+3,y+height+3,0xff28007f);
    for(int i=0;i<lines.size();i++)mc.textRenderer.drawWithShadow(lines.get(i),x,y+i*10,i==0?0xffffff:0xaaaaaa);
  }
}
