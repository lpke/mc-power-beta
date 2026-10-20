package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import local.luke.power.config.SettingsRegistry;
import local.luke.power.input.Bindings;
import local.luke.power.map.*;
import local.luke.power.render.*;
import local.luke.power.rules.RuleSet;
import local.luke.power.visual.VisualConfig;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.*;

final class MapFullBrightChecks {
  private static Field field(Class<?> type,String name)throws Exception {
    Field result=type.getDeclaredField(name);result.setAccessible(true);return result;
  }
  private static void bright(boolean expected) {
    if(RenderControls.fullBright()!=expected)RenderControls.press(114);
    check(RenderControls.fullBright()==expected,"full bright did not toggle; focus the disposable client");
  }
  private static void color(int actual,int expected,String label) {
    for(int shift:new int[]{0,8,16})check(Math.abs((actual>>shift&255)-(expected>>shift&255))<=2,
        label+" actual="+Integer.toHexString(actual)+" expected="+Integer.toHexString(expected));
  }
  private static void frame(Minecraft mc,Release300MapRefinement.Canvas canvas,WorldMapBuffer buffer,
      MapSettings settings,int expected)throws Exception {
    var view=new MapProjection(32,32,1,0,0,128,128,0);
    canvas.bind();MapRenderer.beginFrame();field(MapRenderer.class,"pixelsPerUnit").setDouble(null,2);
    // Minimap.drawFrame owns this GUI state around its buffer; the direct call must supply it too.
    GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glDisable(GL11.GL_CULL_FACE);
    GL11.glDisable(GL11.GL_ALPHA_TEST);GL11.glDisable(GL11.GL_FOG);
    GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
    MapRenderer.uploadBudget(64);
    check(MinimapBuffer.draw(mc,view,settings),"minimap buffer did not draw");
    color(canvas.rgb(64,64),expected,"retained minimap brightness");
    boolean complete=false;
    for(int i=0;i<32;i++) {
      canvas.bind();MapRenderer.beginFrame();field(MapRenderer.class,"pixelsPerUnit").setDouble(null,2);
      MapRenderer.uploadBudget(64);
      check(buffer.draw(mc,view,0,Maps.layer(),settings,canvas.guiWidth,canvas.guiHeight),"world map buffer did not draw");
      if(field(WorldMapBuffer.class,"active").get(buffer)!=null&&field(WorldMapBuffer.class,"pending").get(buffer)==null) {
        complete=true;break;
      }
    }
    check(complete,"world map restyle exceeded bounded render calls");
    color(canvas.rgb(64,64),expected,"retained world-map brightness");
    canvas.bind();var preview=Minimap.frame(mc,settings,128,128,false,true);
    Minimap.drawFrame(mc,settings,preview,true);
    color(canvas.rgb(preview.layout().x()+20,preview.layout().y()+20),expected,"HUD minimap preview brightness");
  }

  @SuppressWarnings("unchecked")
  static void run(Minecraft mc)throws Exception {
    test("full bright redraws both map buffers and HUD preview while retaining cave settings and tile bytes",()->{
      check(GLContext.getCapabilities().GL_EXT_framebuffer_object,"map brightness fixture requires framebuffers");
      var savedMap=MapConfig.copy();var visual=VisualConfig.copy();var rules=AccessRules.copy(mc);
      var previous=mc.currentScreen;boolean paused=mc.paused;int binding=RenderControls.FULL_BRIGHT.code;
      var modifiers=Bindings.modifiers();String mode=mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
      var state=(FullBrightState)field(RenderControls.class,"brightness").get(null);
      var lightState=new LinkedHashMap<Field,Object>();
      for(var f:FullBrightState.class.getDeclaredFields()){f.setAccessible(true);lightState.put(f,f.get(state));}
      boolean active=RenderControls.fullBright();
      var mapState=new LinkedHashMap<Field,Object>();
      for(String name:List.of("cache","world","dimension","layer","x","y","z","radarPreview")) {
        var f=field(Maps.class,name);mapState.put(f,f.get(null));
      }
      int matrix=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
      int framebuffer=GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
      int activeTexture=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
      boolean blit=GLContext.getCapabilities().GL_EXT_framebuffer_blit;
      int read=blit?GL11.glGetInteger(EXTFramebufferBlit.GL_READ_FRAMEBUFFER_BINDING_EXT):framebuffer;
      int draw=blit?GL11.glGetInteger(EXTFramebufferBlit.GL_DRAW_FRAMEBUFFER_BINDING_EXT):framebuffer;
      var cache=new MapCache(Path.of("power-beta-validation-memory-only-brightness"),256,message->{throw new AssertionError(message);});
      var tiles=(Map<MapMath.Key,MapTile>)field(MapCache.class,"tiles").get(cache);
      var loading=(Map<MapMath.Key,CompletableFuture<MapTile>>)field(MapCache.class,"loading").get(cache);
      var cycles=(local.luke.power.world.WorldCycles)mc.world.method_262();long daylight=cycles.power$daylightTime();
      var buffer=new WorldMapBuffer();Release300MapRefinement.Canvas canvas=null;
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
      GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
      try {
        cycles.power$daylightTime(6000);
        mc.setScreen(null);mc.paused=false;AccessRules.restore(mc,RuleSet.cheats());CreativeVehicleChecks.mode(mc,"SURVIVAL");
        var lighting=VisualConfig.copy();lighting.spectatorFullBright=false;VisualConfig.preview(lighting);
        state.update(null,false,false,false);RenderControls.tick(mc);
        RenderControls.FULL_BRIGHT.code=114;Bindings.configure(Map.of());
        cache.viewContext(CaveWindow.at(64,10,10).context());
        for(int z=-3;z<=3;z++)for(int x=-3;x<=3;x++) {
          var tile=new MapTile(new MapMath.Key(0,8,0,x,z,cache.viewContext()));
          for(int i=0;i<MapMath.PIXELS;i++)tile.put(i,0x809060,0x809060,63,0,1,0);
          tile=MapCodec.decode(MapCodec.encode(tile),tile.key);tile.readOnly=true;tiles.put(tile.key,tile);
        }
        for(int i=0;i<16;i++)loading.put(new MapMath.Key(0,8,0,1000+i,1000,cache.viewContext()),new CompletableFuture<>());
        MinimapBuffer.clear();MapRenderer.clear();field(Maps.class,"cache").set(null,cache);field(Maps.class,"world").set(null,null);
        field(Maps.class,"dimension").setInt(null,0);field(Maps.class,"layer").setInt(null,8);
        field(Maps.class,"x").setDouble(null,32);field(Maps.class,"y").setDouble(null,64);field(Maps.class,"z").setDouble(null,32);
        var settings=MapConfig.copy();settings.size=64;settings.opacity=1;settings.rotate=settings.circular=false;
        settings.coordinates=settings.biome=settings.clock=settings.compass=false;
        settings.waypoints=settings.radar=settings.chunkGrid=settings.slimeChunks=settings.lightOverlay=false;
        settings.footsteps=false;settings.playerOpacity=0;settings.mapSmoothing=false;
        settings.updateRate=1;settings.uploadsPerFrame=16;settings.mapOpenBudget=8;settings.caveLighting=true;
        MapConfig.preview(settings);settings=MapConfig.current();canvas=new Release300MapRefinement.Canvas();
        var reference=tiles.values().iterator().next();byte[] bytes=MapCodec.encode(reference);
        int dark=reference.color(0,settings,false),bright=reference.color(0,settings,true);
        check(dark!=bright,"fixture has no cave shading contrast");long submitted=MapIO.submitted();
        frame(mc,canvas,buffer,settings,dark);int normalStyle=MapRenderer.style(settings);
        bright(true);check(MapRenderer.style(settings)!=normalStyle,"full bright retained texture style");
        frame(mc,canvas,buffer,settings,bright);
        check(MapConfig.current().caveLighting,"full bright changed saved cave-lighting preference");
        bright(false);frame(mc,canvas,buffer,settings,dark);
        var session=SettingsRegistry.open(mc);
        find(session,"maps.caveLighting").parse("false");session.preview(true);
        frame(mc,canvas,buffer,MapConfig.current(),bright);session.discard();
        frame(mc,canvas,buffer,MapConfig.current(),dark);
        bright(true);var cancelBright=SettingsRegistry.open(mc);
        find(cancelBright,"maps.caveLighting").parse("false");cancelBright.preview(true);cancelBright.discard();
        check(MapConfig.current().caveLighting,"Cancel lost enabled cave lighting");
        frame(mc,canvas,buffer,MapConfig.current(),bright);bright(false);
        settings=MapConfig.copy();settings.caveLighting=false;MapConfig.preview(settings);settings=MapConfig.current();
        int offStyle=MapRenderer.style(settings);frame(mc,canvas,buffer,settings,bright);
        bright(true);check(MapRenderer.style(settings)==offStyle,"disabled cave lighting caused needless restyle");
        frame(mc,canvas,buffer,settings,bright);bright(false);
        settings=MapConfig.copy();settings.caveLighting=true;MapConfig.preview(settings);settings=MapConfig.current();
        lighting.spectatorFullBright=true;VisualConfig.preview(lighting);CreativeVehicleChecks.mode(mc,"SPECTATOR");RenderControls.tick(mc);
        frame(mc,canvas,buffer,settings,bright);
        bright(false);frame(mc,canvas,buffer,settings,dark);
        CreativeVehicleChecks.mode(mc,"SURVIVAL");RenderControls.tick(mc);frame(mc,canvas,buffer,settings,dark);
        bright(true);CreativeVehicleChecks.mode(mc,"SPECTATOR");RenderControls.tick(mc);bright(false);
        CreativeVehicleChecks.mode(mc,"SURVIVAL");RenderControls.tick(mc);
        check(RenderControls.fullBright(),"spectator visit lost manual full bright");frame(mc,canvas,buffer,settings,bright);
        bright(false);
        tiles.clear();
        for(int z=-3;z<=3;z++)for(int x=-3;x<=3;x++) {
          var surface=new MapTile(new MapMath.Key(0,-1,0,x,z));
          for(int i=0;i<MapMath.PIXELS;i++)surface.put(i,0x809060,0x809060,63,0,1,0);
          surface=MapCodec.decode(MapCodec.encode(surface),surface.key);surface.readOnly=true;tiles.put(surface.key,surface);
        }
        field(Maps.class,"layer").setInt(null,-1);buffer.clear();MinimapBuffer.clear();MapRenderer.clear();
        settings=MapConfig.copy();settings.caveLighting=true;MapConfig.preview(settings);settings=MapConfig.current();
        var surface=tiles.values().iterator().next();byte[] surfaceBytes=MapCodec.encode(surface);
        cycles.power$daylightTime(6000);frame(mc,canvas,buffer,settings,surface.color(0,settings,false,0));
        cycles.power$daylightTime(18000);int night=surface.color(0,settings,false,mc.world.method_151(1));
        check(night!=surface.color(0,settings,true),"nighttime surface fixture lacks contrast");
        frame(mc,canvas,buffer,settings,night);bright(true);frame(mc,canvas,buffer,settings,surface.color(0,settings,true));
        bright(false);frame(mc,canvas,buffer,settings,night);cycles.power$daylightTime(6000);
        frame(mc,canvas,buffer,settings,surface.color(0,settings,false,0));
        check(Arrays.equals(surfaceBytes,MapCodec.encode(surface)),"night/full-bright rendering rewrote surface terrain");
        check(MapIO.submitted()==submitted,"brightness redraw submitted map storage work");
        check(Arrays.equals(bytes,MapCodec.encode(reference)),"brightness redraw mutated persisted terrain");
        check(MapConfig.current().caveLighting,"brightness visits changed cave preference");
        check(GL11.glGetError()==GL11.GL_NO_ERROR,"brightness fixture left an OpenGL error");
      } finally {
        cycles.power$daylightTime(daylight);
        buffer.clear();MinimapBuffer.clear();MapRenderer.clear();loading.clear();cache.retire();cache.releaseIfIdle();
        for(var e:mapState.entrySet())e.getKey().set(null,e.getValue());
        AccessRules.restore(mc,RuleSet.cheats());CreativeVehicleChecks.mode(mc,mode);
        VisualConfig.preview(visual);MapConfig.preview(savedMap);AccessRules.restore(mc,rules);
        for(var e:lightState.entrySet())e.getKey().set(state,e.getValue());field(RenderControls.class,"active").setBoolean(null,active);
        RenderControls.FULL_BRIGHT.code=binding;Bindings.configure(modifiers);mc.paused=paused;mc.setScreen(previous);
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,framebuffer);
        if(canvas!=null)canvas.close();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glPopClientAttrib();GL11.glPopAttrib();
        GL13.glActiveTexture(activeTexture);GL11.glMatrixMode(matrix);
        if(blit){EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferBlit.GL_READ_FRAMEBUFFER_EXT,read);
          EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferBlit.GL_DRAW_FRAMEBUFFER_EXT,draw);}
        else EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,framebuffer);
      }
    });
  }
}
