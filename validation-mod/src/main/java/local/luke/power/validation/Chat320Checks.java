package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.chat.*;
import local.luke.power.config.Catalog;
import local.luke.power.ui.TextSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.class_564;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** History endpoints are established only by the external physical mouse driver. */
final class Chat320Checks {
  private static final Path REPORT=Path.of("power-beta-data/reports/release-320");
  private static final String RAW="§bOriginal message start. Power Beta keeps one original message while its display rows wrap. "
      +"§6Drag from the first character through every colored row to the final character. "
      +"§aIncoming messages and a narrower window must retain the same selected source text. "
      +"§dCopy must preserve spaces, remove color codes and never invent line breaks between wrapped rows. "
      +"§fPhysical selection reaches this final marker: END OF ORIGINAL MESSAGE.";
  private static final String EXPECTED=new ChatTranscript.Message(RAW).plain;
  private static ChatScreen chat;
  private static int incoming;

  static void run(Minecraft mc,String mode)throws Exception {
    Files.createDirectories(REPORT);
    switch(mode) {
      case "chat-prepare" -> prepare(mc);
      case "chat-state" -> state(mc);
      case "chat-check" -> selected(mc);
      case "chat-copy-arm" -> {
        requireChat(mc);
        String selection=ChatSelection.history.text();boolean dragging=ChatSelection.history.dragging();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection("PB320 copy sentinel"),null);
        check(selection.equals(ChatSelection.history.text())&&dragging==ChatSelection.history.dragging(),"arming clipboard changed history endpoints");
        log("320 CHAT COPY ARMED with sentinel; send physical Ctrl+C");
      }
      case "chat-copy" -> {
        selected(mc);
        check(EXPECTED.equals(Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor)),"physical Ctrl+C clipboard differs from original message");
        log("PASS 320 physical chat clipboard contains original text without wrap newlines");
      }
      case "chat-incoming" -> {requireChat(mc);mc.inGameHud.addChatMessage("Incoming message "+(++incoming)+" while original selection remains.");log("320 chat incoming added");}
      case "chat-scroll-add" -> {
        requireChat(mc);for(int i=0;i<60;i++)mc.inGameHud.addChatMessage("Scroll fixture "+i+" retains the earlier selected original message.");
        check(ChatTranscript.messages().stream().anyMatch(message->message.plain.equals(EXPECTED)),"chat retention removed fixture original");
        log("320 chat scroll messages added");
      }
      case "chat-focus-check" -> {
        requireChat(mc);check(!ChatSelection.history.selected()&&!ChatSelection.history.dragging(),"focus loss retained history selection or drag");
        log("PASS 320 physical focus loss canceled chat selection and drag");
      }
      case "chat-close-check" -> closeCheck(mc);
      case "chat-pixels" -> pixels(mc);
      default -> throw new IllegalArgumentException("Unknown chat fixture "+mode);
    }
  }
  private static Object get(Object target,String name)throws Exception{return Editor230Checks.get(target,name);}
  private static Object invoke(Object target,String name,Class<?>[] types,Object...args)throws Exception{return Editor230Checks.invoke(target,name,types,args);}
  private static void requireChat(Minecraft mc){check(chat!=null&&mc.currentScreen==chat,"run chat-prepare and keep that chat screen open");}
  private static Field offset(Minecraft mc)throws Exception {
    for(Class<?> type=mc.inGameHud.getClass();type!=null;type=type.getSuperclass())for(Field f:type.getDeclaredFields())
      if(f.getName().equals("chatOffset")||f.getName().endsWith("$chatOffset")){f.setAccessible(true);return f;}
    throw new NoSuchFieldException("chatOffset");
  }
  private static void prepare(Minecraft mc)throws Exception {
    check(mc.world!=null&&mc.player!=null,"open disposable world first");
    check(org.lwjgl.opengl.Display.isActive(),"focus the disposable client before chat-prepare");
    mc.inGameHud.clearChat();offset(mc).setInt(mc.inGameHud,0);incoming=0;
    mc.inGameHud.addChatMessage(RAW);chat=new ChatScreen();mc.setScreen(chat);
    check(!ChatSelection.history.selected()&&!ChatSelection.history.dragging(),"chat preparation preselected history");
    state(mc);
    check(rows().stream().filter(row->(boolean)row.get("original")).count()>=4,"original fixture did not wrap into four display rows");
    log("320 CHAT PREPARED without selection; use chat-state coordinates for physical drag");
  }
  private static void selected(Minecraft mc)throws Exception {
    requireChat(mc);
    check(ChatSelection.history.selected(),"physical pointer did not select chat history");
    check(!ChatSelection.history.dragging(),"release the physical mouse before checking selection");
    check(EXPECTED.equals(ChatSelection.history.text()),"selected original differs: "+ChatSelection.history.text());
    check(ChatSelection.history.text().indexOf('\n')<0,"copy introduced artificial wrapping newlines");
    log("PASS 320 physical history selection matches original "+EXPECTED.length()+" characters");
  }
  private static List<Map<String,Object>> rows()throws Exception {
    var result=new ArrayList<Map<String,Object>>();var selection=ChatSelection.history;
    int count=(int)get(selection,"count");Object[] rows=(Object[])get(selection,"rows");
    for(int i=0;i<count;i++){
      Object row=rows[i];var message=(ChatTranscript.Message)get(row,"message");int[] positions=(int[])get(row,"positions");
      var data=new LinkedHashMap<String,Object>();data.put("index",i);data.put("original",message.plain.equals(EXPECTED));
      for(String name:List.of("from","to","x","y"))data.put(name,get(row,name));
      data.put("width",positions[positions.length-1]);data.put("text",get(row,"raw"));data.put("plain",message.plain);result.add(data);
    }
    return result;
  }
  private static void state(Minecraft mc)throws Exception {
    requireChat(mc);draw(mc,null);
    var scale=new class_564(mc.options,mc.displayWidth,mc.displayHeight);int width=scale.method_1857(),height=scale.method_1858();
    var rows=rows();var original=rows.stream().filter(row->(boolean)row.get("original")).toList();
    var data=new LinkedHashMap<String,Object>();data.put("expectedPlain",EXPECTED);data.put("raw",RAW);
    data.put("displayWidth",mc.displayWidth);data.put("displayHeight",mc.displayHeight);data.put("scaledWidth",width);data.put("scaledHeight",height);
    data.put("screenWidth",chat.width);data.put("screenHeight",chat.height);data.put("scaleX",mc.displayWidth/(double)width);data.put("scaleY",mc.displayHeight/(double)height);
    data.put("rowCount",rows.size());data.put("originalRowCount",original.size());data.put("rows",rows);
    data.put("selected",ChatSelection.history.text());data.put("dragging",ChatSelection.history.dragging());data.put("chatOffset",offset(mc).getInt(mc.inGameHud));
    data.put("sourceMessages",ChatTranscript.messages().size());
    int mouseX=Mouse.getX()*width/mc.displayWidth,mouseY=height-Mouse.getY()*height/mc.displayHeight-1;
    Object config=Class.forName("local.luke.power.hud.Config").getField("config").get(null);
    Object renderer=get(mc.inGameHud,"power$chat");
    data.put("paused",mc.paused);data.put("focused",Display.isActive());
    data.put("mouseX",mouseX);data.put("mouseY",mouseY);data.put("nativeMouseX",Mouse.getX());data.put("nativeMouseY",Mouse.getY());
    data.put("scrollingEnabled",config.getClass().getField("enableChatScroll").get(config));
    data.put("openChatScroll",get(renderer,"scroll"));data.put("openChatVisible",get(renderer,"visible"));data.put("openChatAutoTicks",get(renderer,"autoTicks"));
    data.put("selectionEdge",ChatSelection.history.edge(mouseY));
    if(!original.isEmpty()){
      var first=original.stream().min(Comparator.comparingInt(row->(int)row.get("from"))).orElseThrow();
      var last=original.stream().max(Comparator.comparingInt(row->(int)row.get("to"))).orElseThrow();
      data.put("dragStart",point(mc,width,height,(int)first.get("x"),(int)first.get("y")+3));
      data.put("dragEnd",point(mc,width,height,(int)last.get("x")+(int)last.get("width"),(int)last.get("y")+3));
      data.put("entireOriginalVisible",(int)first.get("from")==0&&(int)last.get("to")==EXPECTED.length());
    }
    Files.writeString(REPORT.resolve("chat-state.json"),Catalog.JSON.toJson(data));
    log("320 CHAT STATE rows="+rows.size()+" originalRows="+original.size()+" selected="+ChatSelection.history.text().length());
  }
  private static Map<String,Object> point(Minecraft mc,int width,int height,int x,int y){
    return Map.of("guiX",x,"guiY",y,"x",(int)Math.round(x*mc.displayWidth/(double)width),"y",(int)Math.round((y+.25)*mc.displayHeight/(double)height));
  }
  private static void closeCheck(Minecraft mc)throws Exception {
    requireChat(mc);mc.setScreen(null);check(!ChatSelection.history.selected()&&!ChatSelection.history.dragging(),"closing chat retained selection");
    history(mc);
    chat=new ChatScreen();mc.setScreen(chat);draw(mc,REPORT.resolve("chat-reopened.png"));
    mc.setScreen(null);check(!ChatSelection.history.selected(),"reopened chat retained selection");
    history(mc);
    chat=new ChatScreen();mc.setScreen(chat);draw(mc,null);log("PASS 320 repeated chat close/reopen rendered without stale offset failure");
  }
  private static void history(Minecraft mc)throws Exception {
    var scale=new class_564(mc.options,mc.displayWidth,mc.displayHeight);var offset=offset(mc);
    int value=(int)invoke(get(mc.inGameHud,"power$chat"),"render",new Class<?>[]{Minecraft.class,int.class,int.class,int.class,boolean.class},
        mc,scale.method_1857(),scale.method_1858(),offset.getInt(mc.inGameHud),true);
    offset.setInt(mc.inGameHud,value);
  }
  private static final int CANVAS=0x6080a0;
  private static BufferedImage draw(Minecraft mc,Path file)throws Exception {
    int mode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    var scale=new class_564(mc.options,mc.displayWidth,mc.displayHeight);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
    try {
      GL11.glViewport(0,0,mc.displayWidth,mc.displayHeight);GL11.glDisable(GL11.GL_SCISSOR_TEST);
      GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_ALPHA_TEST);
      GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glClearColor((CANVAS>>16&255)/255f,(CANVAS>>8&255)/255f,(CANVAS&255)/255f,1);
      GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
      GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(0,scale.method_1857(),scale.method_1858(),0,1000,3000);
      GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();GL11.glTranslatef(0,0,-2000);GL11.glColor4f(1,1,1,1);
      history(mc);chat.render(-1,-1,0);GL11.glFinish();
      if(file==null)return null;
      ByteBuffer pixels=BufferUtils.createByteBuffer(mc.displayWidth*mc.displayHeight*4);
      GL11.glReadPixels(0,0,mc.displayWidth,mc.displayHeight,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
      var image=new BufferedImage(mc.displayWidth,mc.displayHeight,BufferedImage.TYPE_INT_RGB);
      for(int y=0;y<mc.displayHeight;y++)for(int x=0;x<mc.displayWidth;x++){
        int at=((mc.displayHeight-y-1)*mc.displayWidth+x)*4;
        image.setRGB(x,y,(pixels.get(at)&255)<<16|(pixels.get(at+1)&255)<<8|pixels.get(at+2)&255);
      }
      ImageIO.write(image,"png",file.toFile());
      check(GL11.glGetError()==GL11.GL_NO_ERROR,"chat canvas left an OpenGL error");return image;
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();
      GL11.glMatrixMode(mode);GL11.glPopAttrib();
    }
  }
  private static ChatBuffer editor()throws Exception {
    return (ChatBuffer)Class.forName("local.luke.power.commands.integration.chat.ChatWidgetAccess").getMethod("editor").invoke(null);
  }
  private static void input(String text)throws Exception {
    Class.forName("local.luke.power.commands.integration.chat.ChatWidgetAccess").getMethod("setText",String.class).invoke(null,text);
  }
  private static int rgb(BufferedImage image,int width,int height,int x,int y) {
    int px=Math.min(image.getWidth()-1,(int)Math.floor((x+.5)*image.getWidth()/width));
    int py=Math.min(image.getHeight()-1,(int)Math.floor((y+.5)*image.getHeight()/height));
    return image.getRGB(px,py)&0xffffff;
  }
  private static int blended(int foreground,int background,int alpha) {
    int value=0;for(int shift:new int[]{0,8,16})value|=Math.round(((foreground>>shift&255)*alpha+(background>>shift&255)*(255-alpha))/255f)<<shift;
    return value;
  }
  private static boolean near(int actual,int expected,int tolerance) {
    for(int shift:new int[]{0,8,16})if(Math.abs((actual>>shift&255)-(expected>>shift&255))>tolerance)return false;return true;
  }
  private static Map<String,Object> glyph(BufferedImage image,int width,int height,int x,int y,int advance,int background) {
    int empty=0,ink=0;
    for(int dy=0;dy<8;dy++)for(int dx=0;dx<advance;dx++){
      if(near(rgb(image,width,height,x+dx,y+dy),background,1))empty++;else ink++;
    }
    return Map.of("transparentPixels",empty,"inkPixels",ink,"totalPixels",advance*8);
  }
  private static float[] color() {
    FloatBuffer values=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_CURRENT_COLOR,values);
    return new float[]{values.get(0),values.get(1),values.get(2),values.get(3)};
  }
  private static void selectionState() {
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_BLEND);GL11.glEnable(GL11.GL_ALPHA_TEST);
      GL11.glBlendFunc(GL11.GL_ONE,GL11.GL_ZERO);GL11.glColor4f(.17f,.31f,.52f,.73f);
      float[] before=color();int source=GL11.glGetInteger(GL11.GL_BLEND_SRC),dest=GL11.glGetInteger(GL11.GL_BLEND_DST);
      TextSelection.background(-20,-20,-10,-10);
      check(GL11.glIsEnabled(GL11.GL_TEXTURE_2D)&&GL11.glIsEnabled(GL11.GL_BLEND)&&GL11.glIsEnabled(GL11.GL_ALPHA_TEST),"selection leaked texture/blend/alpha enable state");
      check(GL11.glGetInteger(GL11.GL_BLEND_SRC)==source&&GL11.glGetInteger(GL11.GL_BLEND_DST)==dest,"selection changed caller blend factors");
      float[] after=color();for(int i=0;i<4;i++)check(Math.abs(before[i]-after[i])<.00001,"selection changed caller current color");
    } finally {GL11.glPopAttrib();}
  }
  private static void pixels(Minecraft mc)throws Exception {
    requireChat(mc);selected(mc);
    String originalInput=editor().text();int cursor=editor().cursor(),anchor=(int)get(editor(),"anchor");
    String selected=ChatSelection.history.text();
    try {
      draw(mc,REPORT.resolve("chat-selected-history.png"));
      input("OOOOOO typing");editor().all();
      BufferedImage image=draw(mc,REPORT.resolve("chat-selected-history-and-input.png"));
      var scale=new class_564(mc.options,mc.displayWidth,mc.displayHeight);int width=scale.method_1857(),height=scale.method_1858();
      var first=rows().stream().filter(row->(boolean)row.get("original")&&(int)row.get("from")==0).findFirst().orElseThrow(()->new AssertionError("scroll original first row into view before chat-pixels"));
      int hx=(int)first.get("x"),hy=(int)first.get("y"),tx=4+mc.textRenderer.getWidth("> ");
      int history=rgb(image,width,height,hx+1,hy-1),typing=rgb(image,width,height,tx+1,height-13);
      int expected=blended(TextSelection.COLOR&0xffffff,blended(0,CANVAS,128),128);
      check(history==typing,"history and typing selection top strips differ: "+Integer.toHexString(history)+" vs "+Integer.toHexString(typing));
      check(near(history,expected,2),"selection is not the expected translucent blue over half-black chat: "+Integer.toHexString(history)+" expected "+Integer.toHexString(expected));
      check(!near(history,TextSelection.COLOR&0xffffff,2),"selection became opaque blue");
      int advance=mc.textRenderer.getWidth("O");
      var historyGlyph=glyph(image,width,height,hx,hy,advance,history);
      var typingGlyph=glyph(image,width,height,tx,height-12,advance,typing);
      for(var evidence:List.of(historyGlyph,typingGlyph)){
        check((int)evidence.get("transparentPixels")>=6,"font transparent pixels became a solid glyph rectangle: "+evidence);
        check((int)evidence.get("inkPixels")>=6,"font glyph disappeared into highlight: "+evidence);
      }
      selectionState();check(ChatSelection.history.text().equals(selected),"pixel comparison changed physical history selection");
      var evidence=new LinkedHashMap<String,Object>();evidence.put("historyRGB",String.format("#%06X",history));evidence.put("typingRGB",String.format("#%06X",typing));
      evidence.put("expectedRGB",String.format("#%06X",expected));evidence.put("canvasRGB",String.format("#%06X",CANVAS));evidence.put("alpha",128);
      evidence.put("historyGlyph",historyGlyph);evidence.put("typingGlyph",typingGlyph);evidence.put("historyGui",List.of(hx,hy));evidence.put("typingGui",List.of(tx,height-12));
      Files.writeString(REPORT.resolve("chat-pixels.json"),Catalog.JSON.toJson(evidence));
      log("PASS 320 chat history/input selection pixels match translucent blue; font alpha and caller GL state retained");
    } finally {
      input(originalInput);editor().position(anchor,false);editor().position(cursor,true);
    }
  }
}
