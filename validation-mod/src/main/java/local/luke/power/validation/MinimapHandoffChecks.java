package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import local.luke.power.map.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.*;

/** Actual retained GPU pixels stay visible through an incomplete cave-layer replacement. */
final class MinimapHandoffChecks {
  @SuppressWarnings("unchecked") static void run(Minecraft mc) throws Exception {
    Object oldCache = staticValue(Maps.class,"cache"), oldLayer = staticValue(Maps.class,"layer");
    int oldBuffer = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
    int oldMatrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    Release300MapRefinement.Canvas canvas = null;
    try {
      MinimapBuffer.clear(); MapRenderer.clear();
      var cache = new MapCache(Path.of("power-beta-data/reports/release-310/inert-map"),256,text -> {throw new AssertionError(text);});
      var tiles = (Map<MapMath.Key,MapTile>)field(cache,"tiles");
      var loading = (Map<MapMath.Key,CompletableFuture<MapTile>>)field(cache,"loading");
      int dimension = Maps.dimension();
      for (int i=0;i<16;i++) loading.put(new MapMath.Key(dimension,0,0,10000+i,10000),new CompletableFuture<>());
      fill(tiles,dimension,0,0x55bb44);
      set(Maps.class,"cache",cache); set(Maps.class,"layer",0);
      var settings=MapConfig.copy();settings.circular=false;settings.opacity=1;
      settings.heightShading=settings.slopeShading=settings.caveLighting=settings.chunkGrid=settings.slimeChunks=settings.lightOverlay=false;
      canvas=new Release300MapRefinement.Canvas();
      var view=new MapProjection(32,32,1,0,0,128,128,0);
      frame(mc,canvas,view,settings);
      int green=canvas.rgb(64,64);check((green>>8&255)>(green>>16&255),"old cave layer not green");
      Object active=staticValue(MinimapBuffer.class,"active");set(Maps.class,"layer",1);
      frame(mc,canvas,view,settings);
      check(staticValue(MinimapBuffer.class,"active")==active,"incomplete layer replaced retained terrain");
      check(canvas.rgb(64,64)==green,"incomplete layer flashed black");
      fill(tiles,dimension,1,0xdd5533);field(cache,"generation",((Number)field(cache,"generation")).longValue()+1);
      Object pending=staticValue(MinimapBuffer.class,"preparing");field(pending,"refreshed",0L);
      frame(mc,canvas,view,settings);
      int red=canvas.rgb(64,64);
      check(staticValue(MinimapBuffer.class,"active")!=active,"ready cave layer never replaced old terrain");
      check((red>>16&255)>(red>>8&255),"prepared layer did not publish new pixels");
      check(staticValue(MinimapBuffer.class,"preparing")==null,"completed candidate retained duplicate resources");
    } finally {
      MinimapBuffer.clear();MapRenderer.clear();set(Maps.class,"cache",oldCache);set(Maps.class,"layer",oldLayer);
      if(canvas!=null)canvas.close();
      GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();
      GL11.glMatrixMode(oldMatrix);EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,oldBuffer);GL11.glPopAttrib();
    }
  }
  static void fill(Map<MapMath.Key,MapTile> tiles,int dimension,int layer,int color) {
    for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++) {
      var key=new MapMath.Key(dimension,layer,0,x,z);var tile=new MapTile(key);
      for(int i=0;i<MapMath.PIXELS;i++)tile.put(i,color,color,64,15,1,0);
      tile.savedRevision=tile.revision;tiles.put(key,tile);
    }
  }
  static void frame(Minecraft mc,Release300MapRefinement.Canvas canvas,MapProjection view,MapSettings settings) {
    canvas.bind();GL11.glDisable(GL11.GL_FOG);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDisable(GL11.GL_ALPHA_TEST);
    GL11.glClearColor(0,0,0,0);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    MapRenderer.beginFrame(1);MapRenderer.uploadBudget(512);
    check(MinimapBuffer.draw(mc,view,settings),"minimap native framebuffer failed");
  }
  static Object staticValue(Class<?> type,String name)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
  static void set(Class<?> type,String name,Object value)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
}
