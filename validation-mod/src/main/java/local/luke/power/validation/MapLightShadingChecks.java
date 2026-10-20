package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.map.*;
import local.luke.power.ui.WorldMapScreen;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.*;

final class MapLightShadingChecks {
  private static Field field(String name) throws Exception {
    Field f = Maps.class.getDeclaredField(name); f.setAccessible(true); return f;
  }
  @SuppressWarnings("unchecked")
  static void run(Minecraft mc) throws Exception {
    failures = 0; check(mc.world != null && Maps.cache() != null, "load a disposable mapped world first");
    var settings = MapConfig.copy(); var previous = mc.currentScreen;
    var saved = new LinkedHashMap<Field,Object>();
    for (String name : List.of("cache", "layer", "dimension", "x", "y", "z", "radar")) { var f = field(name); saved.put(f, f.get(null)); }
    int cx = ((int)Math.floor(mc.player.x) >> 4) * 16 + 8, cz = ((int)Math.floor(mc.player.z) >> 4) * 16 + 8;
    int y = 110; var blocks = new ArrayList<int[]>();
    var memory = new MapCache(Path.of("power-beta-validation-memory-only-spawnlight"), 64, text -> { throw new AssertionError(text); });
    var tilesField = MapCache.class.getDeclaredField("tiles"); tilesField.setAccessible(true);
    var tiles = (Map<MapMath.Key,MapTile>)tilesField.get(memory);
    try {
      for (int z = cz - 8; z <= cz + 8; z++) for (int x = cx - 8; x <= cx + 8; x++) {
        check(MapSampler.loaded(mc.world, x >> 4, z >> 4) != null, "fixture floor crosses unloaded terrain");
        for (int py = y; py <= y + 2; py++) {
          blocks.add(new int[]{x,py,z,mc.world.getBlockId(x,py,z),mc.world.method_1778(x,py,z)});
          mc.world.method_154(x,py,z,py == y ? 1 : 0,0);
        }
        var key = MapMath.key(mc.world.dimension.id,-1,0,x,z);
        var tile = tiles.computeIfAbsent(key, MapTile::new); tile.put(MapMath.index(x,z),0x809060,0x809060,y,0,1,0);
      }
      field("cache").set(null,memory);field("layer").setInt(null,-1);field("dimension").setInt(null,mc.world.dimension.id);
      field("x").setDouble(null,cx+.5);field("y").setDouble(null,y+1);field("z").setDouble(null,cz+.5);
      var draft = MapConfig.copy();draft.lightOverlay=true;draft.lightRadius=8;draft.minimap=false;draft.worldMap=true;
      MapConfig.preview(draft);mc.setScreen(new WorldMapScreen());MapLightShading.clear();long submitted=MapIO.submitted();
      test("world-map spawn shading surveys loaded floors within its own radius and bounded tick work", () -> {
        for(int tick=0;tick<20;tick++) { MapLightShading.tick(mc,draft);check(MapLightShading.checksLastTick<=512,"survey exceeded its column budget"); }
        var f=MapLightShading.class.getDeclaredField("cells");f.setAccessible(true);
        var spans=(List<MapLightShading.Cell>)f.get(null);check(!spans.isEmpty(),"dark spawnable floor has no red overlay");
        check(spans.stream().anyMatch(c->c.z()==cz&&c.x()<=cx&&c.x()+c.length()>cx),"survey missed center floor");
        check(spans.stream().mapToInt(MapLightShading.Cell::length).sum()<=197,"survey escaped its radius");
        check(MapIO.submitted()==submitted,"light survey admitted terrain IO");
      });
      test("spawn-light overlay clips both map shapes and restores native GL state", () -> {
        int framebuffer=GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT), matrix=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        int texture=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
        var canvas=new Release300MapRefinement.Canvas();
        try {
          canvas.bind();GL11.glDisable(GL11.GL_FOG);GL11.glDisable(GL11.GL_ALPHA_TEST);GL11.glClearColor(.08f,.08f,.08f,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
          var view=new MapProjection(cx+.5,cz+.5,6,.5,.5,128,128,0);
          MapLightShading.draw(mc,view,Maps.dimension(),false,false,draft);
          int rgb=canvas.rgb(64,64);check((rgb>>16&255)>(rgb&255)+40,"survey did not render red at the center");
          MapLightShading.draw(mc,view,Maps.dimension(),true,false,draft);
          check(GL11.glGetError()==GL11.GL_NO_ERROR,"overlay leaked an OpenGL error");
          canvas.bind();draft.lightOverlay=false;draft.waypoints=draft.footsteps=draft.entityNames=false;
          draft.radar=true;draft.entityHeights=true;draft.playerOpacity=0;MapRenderer.beginFrame(1);
          double cameraY=Maps.framePose(mc,1).y();
          field("radar").set(null,List.of(
              new Maps.Radar(cx-4+.5,cameraY,cz+.5,"Near",0xffffff,false,cx-4+.5,cameraY,cz+.5,0),
              new Maps.Radar(cx+4+.5,cameraY+24,cz+.5,"Vehicle",0xffffff,false,cx+4+.5,cameraY+24,cz+.5,0),
              new Maps.Radar(cx+.5,cameraY-24,cz-4+.5,"Item",0xff5555,true,cx+.5,cameraY-24,cz-4+.5,0)));
          MapRenderer.markers(mc,view,Maps.dimension(),false,false,draft);
          int near=canvas.rgb(40,64),far=canvas.rgb(88,64),item=canvas.rgb(64,40);
          check((near&255)>(far&255)+80,"height dimming did not affect native vehicle radar dots: " + Integer.toHexString(near) + "/" + Integer.toHexString(far));
          check((item>>16&255)>0&&(item>>16&255)<120,"item radar dot did not retain its dimmed red color");
          check(GL11.glGetError()==GL11.GL_NO_ERROR,"radar chevrons leaked an OpenGL error");draft.lightOverlay=true;
        } finally {
          EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,framebuffer);canvas.close();
          GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();
          GL11.glPopClientAttrib();GL11.glPopAttrib();GL13.glActiveTexture(texture);GL11.glMatrixMode(matrix);
        }
      });
      test("closed disabled views and fully disabled maps perform no light survey work", () -> {
        draft.lightOverlay=true;mc.setScreen(null);MapLightShading.tick(mc,draft);check(MapLightShading.checksLastTick==0,"closed world-map-only view scanned light");
        draft.minimap=true;MapLightShading.tick(mc,draft);check(MapLightShading.checksLastTick>0,"visible minimap did not enable its overlay survey");
        draft.minimap=draft.worldMap=false;
        for(int i=0;i<40;i++){MapLightShading.tick(mc,draft);check(MapLightShading.checksLastTick==0,"disabled map survey still running");}
      });
    } finally {
      MapLightShading.clear();for(var e:saved.entrySet())e.getKey().set(null,e.getValue());
      for(var b:blocks)mc.world.method_154(b[0],b[1],b[2],b[3],b[4]);
      MapConfig.preview(settings);mc.setScreen(previous);MapRenderer.clear();MinimapBuffer.clear();
    }
    log("RELEASE300 MAP LIGHT failures="+failures);
  }
}
