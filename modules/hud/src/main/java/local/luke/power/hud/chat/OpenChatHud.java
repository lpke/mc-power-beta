package local.luke.power.hud.chat;

import local.luke.power.chat.*;
import local.luke.power.ui.TextSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;

/** Open-chat rows and their selection share one reflowed layout; closed chat stays native. */
public final class OpenChatHud extends DrawContext {
  public interface Scrolling { void power$chatScroll(int height); }
  private final ChatLayout layout=new ChatLayout();
  private int scroll,visible,autoTicks;
  public int render(Minecraft mc,int width,int height,int requested,boolean scrolling) {
    if(!(mc.currentScreen instanceof ChatScreen))return scroll=0;
    var source=ChatTranscript.messages();
    var old=layout.rows();var anchor=scroll>0&&scroll<old.size()?old.get(scroll):null;
    boolean reflow=layout.update(source,mc.textRenderer,Math.min(320,Math.max(1,width-6)),mc.textRenderer::getWidth);
    if(requested!=scroll)scroll=requested;
    else if(reflow&&anchor!=null)scroll=layout.anchor(anchor,scroll);
    visible=Math.max(1,Math.min(20,(height-56)/9));
    if(scrolling)scroll+=Math.round(Mouse.getDWheel()/120f);else scroll=0;
    scroll=Math.max(0,Math.min(Math.max(0,layout.rows().size()-visible),scroll));
    var selection=ChatSelection.history;selection.begin(Display.isActive()?mc.currentScreen:null);selection.messages(source);
    selection.viewport(2,2+Math.min(320,width-6));
    int bottom=height-48,count=Math.min(visible,layout.rows().size()-scroll);
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_COLOR_BUFFER_BIT|GL11.GL_CURRENT_BIT);
    try {
      GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_ALPHA_TEST);
      for(int i=0;i<count;i++)fill(2,bottom-i*9-1,2+Math.min(320,width-6),bottom-i*9+8,0x80000000);
      for(int i=0;i<count;i++) {
        var row=layout.rows().get(scroll+i);
        selection.line(row.message(),row.from(),row.to(),row.text(),mc.textRenderer,2,bottom-i*9,mc.textRenderer::getWidth);
      }
      selection.end();
      if(selection.dragging())selection.move(mc.currentScreen,Mouse.getX()*width/mc.displayWidth,height-Mouse.getY()*height/mc.displayHeight-1,false);
      for(int i=0;i<count;i++) {
        int from=selection.start(i),to=selection.end(i);
        if(from>=0&&to>from)TextSelection.background(selection.x(i,from),bottom-i*9-1,selection.x(i,to),bottom-i*9+8);
      }
      GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
      for(int i=0;i<count;i++)mc.textRenderer.drawWithShadow(layout.rows().get(scroll+i).text(),2,bottom-i*9,0xffffffff);
    } finally {GL11.glPopAttrib();}
    return scroll;
  }
  public int tick(Minecraft mc,int height,int requested,boolean scrolling) {
    if(!scrolling||!(mc.currentScreen instanceof ChatScreen)||!Display.isActive()||!ChatSelection.history.dragging()){autoTicks=0;return requested;}
    int y=height-Mouse.getY()*height/mc.displayHeight-1,direction=ChatSelection.history.edge(y);
    if(direction==0){autoTicks=0;return requested;}
    if(++autoTicks<2)return requested;autoTicks=0;
    return scroll=Math.max(0,Math.min(Math.max(0,layout.rows().size()-visible),requested+direction));
  }
}
