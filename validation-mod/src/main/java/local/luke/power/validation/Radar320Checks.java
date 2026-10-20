package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.map.*;
import local.luke.power.ui.WorldMapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Disposable-client proof. Primary hold tests use physical input and never force a held binding. */
public final class Radar320Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-320/radar");
  private static final String[] TYPES = {"Cow","Pig","Chicken","Sheep","Player","Wolf",
      "Creeper","Skeleton","Zombie","Spider","Slime","Giant","Ghast","PigZombie","Squid"};
  private static MapSettings before;
  private static Screen screen;
  private static Object originalRadar;
  private static java.util.List<Maps.Radar> grid;
  private static Set<Integer> previousIds = Set.of(), releasedIds = Set.of();
  private static int frames, heldFrames, generated, peakNew, peakSize;
  private static boolean armed, reloadPending, coldProbe;
  private static int frameFailures;
  private Radar320Checks() {}

  private static Field field(Class<?> type,String name) throws Exception {
    Field f=type.getDeclaredField(name);f.setAccessible(true);return f;
  }
  @SuppressWarnings("unchecked") private static Map<Object,Integer> textures() throws Exception {
    return (Map<Object,Integer>)field(RadarIcons.class,"TEXTURES").get(null);
  }
  private static RadarIconSource source(Object key) throws Exception {
    var method=key.getClass().getDeclaredMethod("source");method.setAccessible(true);
    return (RadarIconSource)method.invoke(key);
  }
  private static Map<String,Integer> byType() throws Exception {
    Map<String,Integer> result=new LinkedHashMap<>();
    for(var e:textures().entrySet()){
      var source=source(e.getKey());
      if(source.type().equals("Sheep")&&source.woolColor()!=0)continue;
      result.putIfAbsent(source.type(),e.getValue());
    }
    return result;
  }
  public static void observe(Minecraft mc) {
    if(!armed)return;
    try { field(Maps.class,"radar").set(null,grid); }
    catch(Throwable error){failFrame("tick snapshot publication",error);}
  }
  /** Call from a validation-only outer renderer TAIL injection after the production frame HEAD. */
  public static void frame(Minecraft mc) {
    if(!armed)return;
    try {
      frames++;
      if(MapKeys.iconsHeld(mc))heldFrames++;
      if(coldProbe&&MapKeys.iconsHeld(mc)&&textures().size()>=15
          &&field(RadarIcons.class,"remaining").getInt(null)>0
          &&field(RadarIcons.class,"resolutions").getInt(null)>0){
        State state=State.read();
        var source=new RadarIconSource("Sheep","/mob/sheep.png",null,14,true);
        check(RadarIcons.draw(mc,source,new MapProjection(0,0,1,0,0,128,128,0),false,-100,-100,0,MapConfig.current()),"cold native model probe did not draw");
        check(state.equals(State.read()),"cold icon generation leaked native graphics state");
        coldProbe=false;log("PASS cold model-head generation restores framebuffer/renderbuffer/matrices/viewport/blend/color/pixel-store state");
      }
      Set<Integer> ids=new HashSet<>(textures().values()),fresh=new HashSet<>(ids);fresh.removeAll(previousIds);
      if(reloadPending){
        check(!(boolean)field(RadarIcons.class,"invalidated").get(null),"native texture reload did not invalidate icons");
        check(ids.size()<=1,"reload prepared more than one icon in its first native frame");
        reloadPending=false;log("PASS native texture reload invalidates reused texture IDs");
      } else check(fresh.size()<=1,"more than one new icon in one native frame: "+fresh.size());
      check(ids.size()<=128,"head cache exceeded 128 icons");
      check(field(RadarIcons.class,"remaining").getInt(null)>=0,"icon budget became negative");
      check(field(RadarIcons.class,"resolutions").getInt(null)>=0,"native texture lookup budget became negative");
      peakNew=Math.max(peakNew,fresh.size());peakSize=Math.max(peakSize,ids.size());generated+=fresh.size();previousIds=ids;
    } catch(Throwable error){failFrame("native radar frame",error);}
  }
  private static void failFrame(String label,Throwable error){
    if(frameFailures++<3)try{log("FAIL "+label+" "+error);}catch(Exception ignored){}
  }

  public static void run(Minecraft mc,String raw) throws Exception {
    String mode=raw.replaceFirst("^radar-","");
    if(mode.equals("radar"))mode="prepare";
    if(mode.equals("prepare")){prepare(mc);return;}
    check(armed,"run radar-prepare in a disposable loaded world first");
    switch(mode){
      case "check-held","check","held" -> {
        check(MapKeys.iconsHeld(mc),"physical Tab is not held or an input owner blocked it");
        check(byType().keySet().containsAll(Arrays.asList(TYPES)),"waiting for native head cache: "+byType().keySet());
        check(frameFailures==0,"native frame observer failures="+frameFailures);
        check(!coldProbe,"wait another rendered frame for cold icon GL-state probe");
        check(heldFrames>=15,"hold for at least fifteen native rendered frames");
        contactSheet();graphics(mc);
        log("PASS physical held radar all 14 native mobs plus player; frames="+frames+" held="+heldFrames+" new/frame="+peakNew+" peakCache="+peakSize);
      }
      case "check-released","released" -> {
        check(!MapKeys.iconsHeld(mc),"release physical Tab first");releasedIds=new HashSet<>(textures().values());
        var row=grid.get(0);check(!RadarIcons.draw(mc,row.icon(),new MapProjection(0,0,1,0,0,128,128,0),false,64,64,0,MapConfig.current()),"released key still drew a head");
        log("PASS physical Tab release restores dot path without clearing cache");
      }
      case "check-reheld","reheld" -> {
        check(MapKeys.iconsHeld(mc),"hold physical Tab again");
        check(new HashSet<>(textures().values()).containsAll(releasedIds),"reholding discarded reusable icons");
        log("PASS physical rehold reuses cached textures");
      }
      case "reload" -> {
        check(MapKeys.iconsHeld(mc),"hold physical Tab during native texture reload");
        mc.textureManager.method_1096();
        check((boolean)field(RadarIcons.class,"invalidated").get(null),"native texture reload hook did not mark the cache");
        reloadPending=true;previousIds=Set.of();log("RADAR reload armed; wait fifteen rendered frames then radar-check-held");
      }
      case "map" -> {mc.setScreen(new WorldMapScreen());view(mc);log("RADAR world-map capture ready");}
      case "minimap" -> {mc.setScreen(null);log("RADAR minimap capture ready; physical mouse rotation verifies upright heads");}
      case "capture","rotate" -> {check(MapKeys.iconsHeld(mc),"hold physical Tab for captures");contactSheet();graphics(mc);log("RADAR native contact sheet and 0/90 degree projections captured");}
      case "finish" -> finish(mc);
      default -> throw new IllegalArgumentException("radar prepare|check-held|check-released|check-reheld|reload|map|minimap|capture|finish");
    }
  }

  private static void prepare(Minecraft mc) throws Exception {
    check(!armed&&mc.world!=null&&mc.player!=null,"load a disposable world and finish any prior radar fixture");
    Files.createDirectories(REPORT);before=MapConfig.copy();screen=mc.currentScreen;
    originalRadar=field(Maps.class,"radar").get(null);
    var settings=MapConfig.copy();settings.minimap=settings.worldMap=settings.radar=true;
    settings.players=settings.hostile=settings.animals=settings.items=settings.otherEntities=true;
    settings.entityNames=true;settings.entityHeights=false;settings.radarDimming=false;
    settings.waypoints=settings.footsteps=settings.lightOverlay=false;settings.playerOpacity=0;
    settings.size=200;settings.zoom=2;MapConfig.preview(settings);
    java.util.List<Maps.Radar> rows=new ArrayList<>();
    int tick=field(Maps.class,"ticks").getInt(null);
    double x=Maps.x(),z=Maps.z(),y=Maps.framePose(mc,1).y();
    for(int i=0;i<TYPES.length;i++){
      String type=TYPES[i];
      String texture=type.equals("Player")?mc.player.method_1314():"/mob/"+switch(type){
        case "Giant","Zombie" -> "zombie";case "PigZombie" -> "pigzombie";default -> type.toLowerCase(Locale.ROOT);
      }+".png";
      var icon=new RadarIconSource(type,texture,type.equals("Player")?mc.player.skinUrl:null,0,type.equals("Sheep"));
      double px=x+(i%5-2)*16,pz=z+(i/5-1)*18;
      RadarKind kind=Set.of("Cow","Pig","Chicken","Sheep","Wolf","Squid").contains(type)?RadarKind.ANIMALS:
          type.equals("Player")?RadarKind.PLAYERS:RadarKind.HOSTILE;
      rows.add(new Maps.Radar(px,y,pz,type,kind.color(settings),false,px,y,pz,tick,kind,type,icon));
    }
    grid=java.util.List.copyOf(rows);frames=heldFrames=generated=peakNew=peakSize=frameFailures=0;
    previousIds=Set.of();releasedIds=Set.of();RadarIcons.clear();armed=true;coldProbe=true;observe(mc);
    var capture=RadarIconSource.class.getDeclaredMethod("capture",net.minecraft.entity.Entity.class,String.class);capture.setAccessible(true);
    check(capture.invoke(null,new FixtureCreeper(mc.world),"Creeper")==null,"custom native-mob subclass received guessed head geometry");
    mc.setScreen(new WorldMapScreen());view(mc);
    log("RADAR PREPARED physical Tab required; 15 immutable snapshots, no native entities spawned or terrain changed");
  }
  private static final class FixtureCreeper extends net.minecraft.class_114 {
    FixtureCreeper(net.minecraft.world.World world){super(world);}
  }
  private static void view(Minecraft mc) throws Exception {
    var view=(MapMath.View)field(WorldMapScreen.class,"view").get(mc.currentScreen);view.x=Maps.x();view.z=Maps.z();view.scale=3;
  }

  private static void contactSheet() throws Exception {
    int old=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
    try{
      GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT,1);GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH,0);
      GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS,0);GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS,0);
      var image=new BufferedImage(5*144,3*154,BufferedImage.TYPE_INT_ARGB);var graphics=image.createGraphics();
      graphics.setColor(new Color(0x242a30));graphics.fillRect(0,0,image.getWidth(),image.getHeight());graphics.setFont(new Font("Dialog",Font.PLAIN,14));
      var known=byType();
      for(int i=0;i<TYPES.length;i++){
        Integer texture=known.get(TYPES[i]);check(texture!=null,"missing icon "+TYPES[i]);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);var pixels=BufferUtils.createByteBuffer(64*64*4);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
        var tile=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);int alphaCount=0,blackCount=0,minX=64,maxX=-1,minY=64,maxY=-1;
        for(int py=0;py<64;py++)for(int px=0;px<64;px++){
          int index=(py*64+px)*4,a=pixels.get(index+3)&255,r=pixels.get(index)&255,g=pixels.get(index+1)&255,b=pixels.get(index+2)&255;
          tile.setRGB(px,63-py,a<<24|r<<16|g<<8|b);if(a>0){alphaCount++;minX=Math.min(minX,px);maxX=Math.max(maxX,px);minY=Math.min(minY,py);maxY=Math.max(maxY,py);if(r+g+b==0)blackCount++;}
        }
        check(alphaCount>30&&alphaCount<2200,TYPES[i]+" has empty icon or opaque backing rectangle: "+alphaCount);
        check(blackCount>10,TYPES[i]+" lacks native silhouette outline");
        check((tile.getRGB(0,0)>>>24)==0,TYPES[i]+" tile corner is not transparent");
        if(TYPES[i].equals("Creeper"))check(maxX-minX+1==18&&maxY-minY+1==18,"standard head is not 16px plus one-pixel outline");
        if(TYPES[i].equals("Cow"))check(maxY-minY+1==22,"cow horns missing from native head silhouette");
        graphics.drawImage(tile,i%5*144+8,i/5*154,128,128,null);graphics.setColor(Color.WHITE);graphics.drawString(TYPES[i],i%5*144+12,i/5*154+146);
        ImageIO.write(tile,"PNG",REPORT.resolve(TYPES[i]+".png").toFile());
      }
      graphics.dispose();ImageIO.write(image,"PNG",REPORT.resolve("native-head-contact-sheet.png").toFile());
    } finally{GL11.glBindTexture(GL11.GL_TEXTURE_2D,old);GL11.glPopClientAttrib();}
  }

  private record State(java.util.List<Integer> integers,java.util.List<Float> floats) {
    static State read(){
      java.util.List<Integer> ints=new ArrayList<>();
      for(int code:new int[]{EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT,EXTFramebufferObject.GL_RENDERBUFFER_BINDING_EXT,
          GL11.GL_MATRIX_MODE,GL11.GL_TEXTURE_BINDING_2D,GL13.GL_ACTIVE_TEXTURE,GL11.GL_BLEND_SRC,GL11.GL_BLEND_DST,
          GL14.GL_BLEND_SRC_RGB,GL14.GL_BLEND_DST_RGB,GL14.GL_BLEND_SRC_ALPHA,GL14.GL_BLEND_DST_ALPHA,
          GL11.GL_DEPTH_FUNC,GL11.GL_DEPTH_WRITEMASK,GL11.GL_PACK_ALIGNMENT,GL11.GL_UNPACK_ALIGNMENT,
          GL11.GL_PACK_ROW_LENGTH,GL11.GL_UNPACK_ROW_LENGTH,GL11.GL_PACK_SKIP_ROWS,GL11.GL_UNPACK_SKIP_ROWS,
          GL11.GL_PACK_SKIP_PIXELS,GL11.GL_UNPACK_SKIP_PIXELS})ints.add(GL11.glGetInteger(code));
      for(int code:new int[]{GL11.GL_BLEND,GL11.GL_TEXTURE_2D,GL11.GL_DEPTH_TEST,GL11.GL_LIGHTING,GL11.GL_FOG,GL11.GL_CULL_FACE,GL11.GL_ALPHA_TEST,GL11.GL_SCISSOR_TEST,GL11.GL_STENCIL_TEST})ints.add(GL11.glIsEnabled(code)?1:0);
      var viewport=BufferUtils.createIntBuffer(16);GL11.glGetInteger(GL11.GL_VIEWPORT,viewport);for(int i=0;i<4;i++)ints.add(viewport.get(i));
      GL11.glGetInteger(GL11.GL_SCISSOR_BOX,viewport);for(int i=0;i<4;i++)ints.add(viewport.get(i));
      java.util.List<Float> floats=new ArrayList<>();
      for(int code:new int[]{GL11.GL_MODELVIEW_MATRIX,GL11.GL_PROJECTION_MATRIX,GL11.GL_CURRENT_COLOR}){
        var values=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(code,values);for(int i=0;i<(code==GL11.GL_CURRENT_COLOR?4:16);i++)floats.add(values.get(i));
      }
      return new State(ints,floats);
    }
  }

  private static void graphics(Minecraft mc) throws Exception {
    State outer=State.read();
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
    GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
    Release300MapRefinement.Canvas canvas=null;
    try{
      canvas=new Release300MapRefinement.Canvas(128,128,256,256);var settings=MapConfig.copy();
      for(boolean circle:new boolean[]{false,true})for(int rotation:new int[]{0,90}){
        bindCanvas(canvas);GL11.glClearColor(0,0,0,0);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        var view=new MapProjection(0,0,1,16,16,96,96,Math.toRadians(rotation));
        for(int i=0;i<grid.size();i++){
          double wx=(i%5-2)*18,wz=(i/5-1)*24;var p=view.screen(wx,wz);State beforeDraw=State.read();
          check(RadarIcons.draw(mc,grid.get(i).icon(),view,circle,p.x(),p.y(),0,settings),"cached physical-held head failed to draw");
          check(beforeDraw.equals(State.read()),"head draw leaked framebuffer/renderbuffer/matrix/viewport/blend/color/pixel-store state");
        }
        var pixels=BufferUtils.createByteBuffer(256*256*4);GL11.glReadPixels(0,0,256,256,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
        var image=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);int visiblePixels=0;
        for(int y=0;y<256;y++)for(int x=0;x<256;x++){
          int j=(y*256+x)*4,a=pixels.get(j+3)&255;if(a>0)visiblePixels++;double gx=(x+.5)/2,gy=128-(y+.5)/2;
          if(gx<16||gx>112||gy<16||gy>112||circle&&Math.hypot(gx-64,gy-64)>48.5)check(a==0,"head escaped map clip");
          image.setRGB(x,255-y,a<<24|(pixels.get(j)&255)<<16|(pixels.get(j+1)&255)<<8|pixels.get(j+2)&255);
        }
        check(visiblePixels>100,"native head projection is blank; check fixture GUI culling/stencil/alpha state");
        ImageIO.write(image,"PNG",REPORT.resolve((circle?"circle":"square")+"-"+rotation+".png").toFile());
      }
      batch(mc,canvas,settings);
      pinnedRetirement(canvas);
      iconSizes(mc,canvas,settings);
      byte[] upright=null;
      for(int rotation:new int[]{0,90}){
        bindCanvas(canvas);GL11.glClearColor(0,0,0,0);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        RadarIcons.draw(mc,grid.get(0).icon(),new MapProjection(0,0,1,0,0,128,128,Math.toRadians(rotation)),false,64,64,0,settings);
        var pixels=BufferUtils.createByteBuffer(256*256*4);GL11.glReadPixels(0,0,256,256,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
        byte[] bytes=new byte[pixels.capacity()];pixels.get(bytes);
        if(upright==null)upright=bytes;else check(Arrays.equals(upright,bytes),"map rotation rotated the cached model-head sprite");
      }
      settings.radarDimming=true;settings.radarDimDistance=24;
      int[] sums=new int[2];
      for(int pass=0;pass<2;pass++){
        bindCanvas(canvas);GL11.glClearColor(0,0,0,0);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        RadarIcons.draw(mc,grid.get(0).icon(),new MapProjection(0,0,1,0,0,128,128,0),false,64,64,pass*24,settings);
        var pixels=BufferUtils.createByteBuffer(256*256*4);GL11.glReadPixels(0,0,256,256,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
        for(int j=0;j<pixels.capacity();j+=4)sums[pass]+=(pixels.get(j)&255)+(pixels.get(j+1)&255)+(pixels.get(j+2)&255);
      }
      check(sums[0]>0&&sums[1]<sums[0]*.5&&sums[1]>sums[0]*.2,"height dimming did not shade native head pixels");
      check(GL11.glGetError()==GL11.GL_NO_ERROR,"head tests left an OpenGL error");
      log("PASS cached native head GL state, transparent outlines, world-map circle/square clipping, upright 0/90 projections and height dimming");
    } finally{
      EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,outer.integers().get(0));
      if(canvas!=null)canvas.close();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(outer.integers().get(2));
      GL11.glPopClientAttrib();GL11.glPopAttrib();
    }
    check(outer.equals(State.read()),"radar fixture leaked native graphics state");
  }

  private static int iconHeight(Minecraft mc, Release300MapRefinement.Canvas canvas, MapSettings settings, int index) {
    bindCanvas(canvas);GL11.glClearColor(0,0,0,0);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    check(RadarIcons.draw(mc,grid.get(index).icon(),new MapProjection(0,0,1,0,0,128,128,0),false,64,64,0,settings),"cached size sample missing");
    var pixels=readCanvas();int lo=256,hi=-1;
    for(int y=0;y<256;y++)for(int x=0;x<256;x++)if((pixels.get((y*256+x)*4+3)&255)>0){lo=Math.min(lo,y);hi=Math.max(hi,y);}
    return Math.max(0,hi-lo+1);
  }
  private static void iconSizes(Minecraft mc, Release300MapRefinement.Canvas canvas, MapSettings settings) throws Exception {
    var ids=new HashSet<>(textures().values());int original=settings.radarIconSize;
    try {
      int previous=0,half=0;
      for(int size:new int[]{25,50,100}) {
        settings.radarIconSize=size;int height=iconHeight(mc,canvas,settings,6);
        check(height>previous,"head size did not grow at "+size);
        if(size==50)half=height;
        if(size==100)check(Math.abs(height-half*2)<=4,"50% head is not half size");
        previous=height;
      }
      settings.radarIconSize=50;int squid=iconHeight(mc,canvas,settings,14);
      check(Math.abs(squid-half)<=2,"squid is oversized relative to standard heads");
      check(new HashSet<>(textures().values()).equals(ids),"icon size rebaked cached textures");
      log("PASS configurable native head sizes scale pixels without rebaking; squid height="+squid+" standard="+half);
    } finally {settings.radarIconSize=original;}
  }
  private static void bindCanvas(Release300MapRefinement.Canvas canvas){
    canvas.bind();
    // Match the production minimap/marker GUI setup rather than the prior world render.
    GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDisable(GL11.GL_FOG);
    GL11.glDisable(GL11.GL_STENCIL_TEST);GL11.glDisable(GL11.GL_ALPHA_TEST);
  }
  private static ByteBuffer readCanvas(){
    var pixels=BufferUtils.createByteBuffer(256*256*4);
    GL11.glReadPixels(0,0,256,256,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);return pixels;
  }
  private static void batch(Minecraft mc,Release300MapRefinement.Canvas canvas,MapSettings settings) throws Exception {
    bindCanvas(canvas);GL11.glClearColor(0,0,0,0);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    var view=new MapProjection(0,0,1,16,16,96,96,0);
    int pinnedBefore=((Map<?,?>)field(RadarIcons.class,"PINNED").get(null)).size();
    try(var empty=new RadarIcons.Batch(0)){
      check(!RadarIcons.draw(mc,grid.get(0).icon(),view,true,18,64,0,settings,empty),"zero-capacity batch claimed a head");
      check(((Map<?,?>)field(RadarIcons.class,"PINNED").get(null)).size()==pinnedBefore,"rejected sprite pinned its texture");
    }
    try(var heads=new RadarIcons.Batch(1)){
      check(RadarIcons.draw(mc,grid.get(0).icon(),view,true,18,64,0,settings,heads),"minimap batch did not enqueue its native cow head");
      check(!RadarIcons.draw(mc,grid.get(1).icon(),view,true,30,64,0,settings,heads),"batch exceeded its view-local marker bound");
      var before=readCanvas();for(int i=3;i<before.capacity();i+=4)check((before.get(i)&255)==0,"deferred head painted before the minimap frame");
      GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glColor4f(.5f,.5f,.5f,1);
      GL11.glBegin(GL11.GL_QUADS);GL11.glVertex2d(16,16);GL11.glVertex2d(18,16);GL11.glVertex2d(18,112);GL11.glVertex2d(16,112);GL11.glEnd();
      // The enclosing preview clips at GUI x=14; the minimap itself starts at x=16.
      GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(28,0,228,256);
      GL11.glEnable(GL11.GL_STENCIL_TEST);State saved=State.read();heads.paint();
      check(saved.equals(State.read()),"deferred batch changed inherited scissor/stencil or native GL state");
      var pixels=readCanvas();boolean overlaps=false,coversBorder=false;
      var image=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
      for(int y=0;y<256;y++)for(int x=0;x<256;x++){
        int i=(y*256+x)*4,a=pixels.get(i+3)&255;
        if(x<28)check(a==0,"minimap head escaped the enclosing HUD-preview scissor");
        if(x>=28&&x<32&&a>0)overlaps=true;
        if(x>=32&&x<36&&y>=116&&y<=140&&a>0){
          int r=pixels.get(i)&255,g=pixels.get(i+1)&255,b=pixels.get(i+2)&255;
          if(!(r==g&&g==b&&r>=125&&r<=130))coversBorder=true;
        }
        image.setRGB(x,255-y,a<<24|(pixels.get(i)&255)<<16|(pixels.get(i+1)&255)<<8|pixels.get(i+2)&255);
      }
      check(overlaps,"deferred minimap head remained clipped to the minimap edge");
      check(coversBorder,"minimap frame still covers the deferred native head");
      ImageIO.write(image,"PNG",REPORT.resolve("minimap-over-frame-preview-scissor.png").toFile());
    } finally{GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glDisable(GL11.GL_STENCIL_TEST);}
    check(((Map<?,?>)field(RadarIcons.class,"PINNED").get(null)).size()==pinnedBefore,"closed minimap batch retained texture pins");
    log("PASS minimap heads paint after the frame across its edge, keep preview scissor/stencil state and obey batch capacity");
  }

  private static void pinnedRetirement(Release300MapRefinement.Canvas canvas) throws Exception {
    int oldTexture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),texture=GL11.glGenTextures();
    var pixels=BufferUtils.createByteBuffer(64*64*4);
    GL11.glBindTexture(GL11.GL_TEXTURE_2D,byType().get("Cow"));
    GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
    GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
    GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,64,64,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
    GL11.glBindTexture(GL11.GL_TEXTURE_2D,oldTexture);
    var add=RadarIcons.Batch.class.getDeclaredMethod("add",int.class,double.class,double.class,double.class,double.class);add.setAccessible(true);
    var retire=RadarIcons.class.getDeclaredMethod("retire",int.class);retire.setAccessible(true);
    var first=new RadarIcons.Batch(1);var second=new RadarIcons.Batch(1);
    try{
      check((boolean)add.invoke(first,texture,64d,64d,1d,1d)&&(boolean)add.invoke(second,texture,64d,64d,1d,1d),"overlapping batches did not acquire pins");
      check(((Map<?,?>)field(RadarIcons.class,"PINNED").get(null)).get(texture).equals(2),"shared native texture was not pinned twice");
      retire.invoke(null,texture);
      check(GL11.glIsTexture(texture),"eviction deleted a queued native texture");
      check(((Set<?>)field(RadarIcons.class,"RETIRED").get(null)).contains(texture),"eviction failed to defer texture deletion");
      bindCanvas(canvas);GL11.glClearColor(0,0,0,0);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);first.paint();
      var pixelsAfter=readCanvas();boolean painted=false;for(int i=3;i<pixelsAfter.capacity();i+=4)painted|=(pixelsAfter.get(i)&255)>0;
      check(painted,"retired pinned texture could not paint its queued head");
      first.close();check(GL11.glIsTexture(texture),"first overlapping batch deleted the second batch's texture");
      second.paint();second.close();check(!GL11.glIsTexture(texture),"last batch did not delete the retired native texture");
      check(!((Map<?,?>)field(RadarIcons.class,"PINNED").get(null)).containsKey(texture),"last batch retained a pin");
      check(!((Set<?>)field(RadarIcons.class,"RETIRED").get(null)).contains(texture),"last batch retained a retired texture");
      log("PASS queued native textures survive eviction and overlapping batches until the final release");
    } finally{first.close();second.close();if(GL11.glIsTexture(texture))GL11.glDeleteTextures(texture);}
  }
  private static void finish(Minecraft mc) throws Exception {
    armed=false;field(Maps.class,"radar").set(null,originalRadar);MapConfig.preview(before);mc.setScreen(screen);
    check(frameFailures==0,"radar frame observer failures="+frameFailures);
    log("RADAR COMPLETE frames="+frames+" generated="+generated+" failures="+frameFailures);
  }
}
