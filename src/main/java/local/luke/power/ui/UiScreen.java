package local.luke.power.ui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.lwjgl.opengl.GL11;
public abstract class UiScreen extends Screen {
  protected boolean inside(int x,int y,int left,int top,int width,int height){return x>=left&&x<left+width&&y>=top&&y<top+height;}
  protected String fit(String text,int span){if(textRenderer.getWidth(text)<=span)return text;while(!text.isEmpty()&&textRenderer.getWidth(text+"...")>span)text=text.substring(0,text.length()-1);return text+"...";}
  protected void text(String s,int x,int y,int color){drawStringWithShadow(textRenderer,s,x,y,color);}
  protected void button(String label,int left,int top,int span,int h,int mx,int my,boolean enabled){ButtonWidget b=new ButtonWidget(0,left,top,Math.max(10,span),h,label);b.active=enabled;b.render(minecraft,mx,my);}
  protected void clip(int left,int top,int span,int h){GL11.glEnable(GL11.GL_SCISSOR_TEST);double sx=(double)minecraft.displayWidth/width,sy=(double)minecraft.displayHeight/height;GL11.glScissor((int)Math.ceil(left*sx),(int)Math.ceil((height-top-h)*sy),Math.max(0,(int)Math.floor(span*sx)),Math.max(0,(int)Math.floor(h*sy)));}
  protected void unclip(){GL11.glDisable(GL11.GL_SCISSOR_TEST);}
  protected void input(TextInput input,int left,int top,int span,int mx,int my,String placeholder){
    fill(left-1,top-1,left+span+1,top+19,input.focused?0xffaaaaaa:0xff555555);fill(left,top,left+span,top+18,0xff101010);
    clip(left+3,top,span-6,18);
    int offset=Math.max(0,textRenderer.getWidth(input.text.substring(0,input.cursor()))-(span-12));
    int x=left+4-offset;
    if(input.focused&&input.start()!=input.end())fill(x+textRenderer.getWidth(input.text.substring(0,input.start())),top+3,x+textRenderer.getWidth(input.text.substring(0,input.end())),top+14,0xff335577);
    text(input.text.isEmpty()&&!input.focused?placeholder:input.text,x,top+5,input.text.isEmpty()&&!input.focused?0x888888:0xffffff);
    if(input.focused&&System.currentTimeMillis()/500%2==0)fill(x+textRenderer.getWidth(input.text.substring(0,input.cursor())),top+3,x+textRenderer.getWidth(input.text.substring(0,input.cursor()))+1,top+14,0xffffffff);
    unclip();
  }
  protected void tooltip(String message,int mx,int my){
    int max=Math.min(width-20,280);java.util.List<String> lines=new java.util.ArrayList<>();
    for(String paragraph:message.split("\n")){String line="";for(String word:paragraph.split(" ")){if(!line.isEmpty()&&textRenderer.getWidth(line+" "+word)>max){lines.add(line);line="";}line+=(line.isEmpty()?"":" ")+word;}lines.add(line);}
    int w=lines.stream().mapToInt(textRenderer::getWidth).max().orElse(0),h=lines.size()*11+8;
    int x=Math.max(5,Math.min(mx+10,width-w-10)),y=Math.max(5,Math.min(my+12,height-h-5));
    fill(x-4,y-4,x+w+4,y+h-4,0xf0101010);int lineY=y;for(String line:lines){text(line,x,lineY,0xeeeeee);lineY+=11;}
  }
}
