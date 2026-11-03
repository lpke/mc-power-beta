package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.hud.HudCoordinates;
import local.luke.power.input.*;
import local.luke.power.map.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Visit-local cave controls, native cave sampling and independent cache/light contexts. */
final class CaveMap473Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-473/cave-map");
  private static final int[][] SIZES = {{854,480},{320,240}};
  private CaveMap473Checks() {}

  static void run(Minecraft mc, String action) throws Exception {
    check(Set.of("run","capture").contains(action), "use cave-map-473 run|capture");
    check(mc.world != null && !mc.world.isRemote && mc.player != null, "open a disposable singleplayer world");
    check(Display.isActive() && !Mouse.isButtonDown(0) && mc.options.soundVolume == 0 && mc.options.musicVolume == 0,
        "focus and mute the disposable client, then release mouse buttons");
    check(Display.getWidth() >= 1708 && Display.getHeight() >= 960, "resize disposable display to at least 1708x960");
    check(!DetachedCamera.isActive(), "leave freecam before starting this fixture");
    Files.createDirectories(REPORT); failures = 0;
    var original = MapConfig.copy(); var previous = mc.currentScreen;
    Object oldCache = getStatic(Maps.class,"cache"), oldSampler = getStatic(Maps.class,"sampler");
    var memory = new MapCache(Files.createTempDirectory("power-beta-cave-map473-"),64,message->{throw new AssertionError(message);});
    var savedBlocks = new ArrayList<int[]>();
    int x = ((int)Math.floor(mc.player.x)>>4)*16, z = ((int)Math.floor(mc.player.z)>>4)*16;
    var chunk = MapSampler.loaded(mc.world,x>>4,z>>4);
    check(chunk != null, "fixture chunk must already be loaded");
    Object camera = Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    Method active = camera.getClass().getMethod("setActive",boolean.class);
    Method position = camera.getClass().getMethod("setCameraPosition",double.class,double.class,double.class);
    double playerX=mc.player.x, playerY=mc.player.y, playerZ=mc.player.z;
    try (var uiState = new Release462Checks.State(mc)) {
      for(int dx=0;dx<16;dx++) for(int dz=0;dz<16;dz++) for(int y=87;y<=101;y++) {
        check(mc.world.method_1777(x+dx,y,z+dz)==null,"fixture would replace block-entity data");
        savedBlocks.add(new int[]{x+dx,y,z+dz,mc.world.getBlockId(x+dx,y,z+dz),mc.world.method_1778(x+dx,y,z+dz)});
      }
      for(int[] block:savedBlocks) mc.world.method_154(block[0],block[1],block[2],block[1]==88?1:block[1]==90?5:block[1]==100?1:0,0);
      var settings = MapConfig.copy(); settings.worldMap = true; settings.minimap = true;
      settings.caveMode = 0; settings.caveAbove = 0; settings.caveBelow = 1; settings.roofSize = 1;
      settings.radar = settings.footsteps = settings.waypoints = settings.worldWaypoints = settings.deathpoints = false;
      settings.lightOverlay = true; settings.lightRadius = 8;
      settings.mapOpenColumns = 1024; settings.mapOpenBudget = 8; settings.updateRate = 20;
      settings.heightShading = settings.slopeShading = false;
      MapConfig.preview(settings); setStatic(Maps.class,"cache",memory); setStatic(Maps.class,"sampler",null);
      Maps.tick(mc);
      for(int[] size:SIZES) {
        var screen = new WorldMapScreen(previous); mc.setScreen(screen); screen.init(mc,size[0],size[1]);
        var view = (MapMath.View)get(screen,"view"); view.x=x+8; view.z=z+8; view.scale=12;
        test("CAVE MAP473 mode, drag, wheel, exact entry and reset " + size[0],()->{
          check(label(screen).equals("Surface"),"button does not name its current Surface state");
          check(call(controls(screen),"caveSlider")==null,"surface exposes cave controls");
          mode(screen); check(label(screen).equals("Caves"),"manual cave button does not name current state");
          Object slider = call(controls(screen),"caveSlider"), reset = call(controls(screen),"caveReset");
          int sx=n(slider,"x"), sy=n(slider,"y"), sw=n(slider,"width");
          check(sx > n(call(modeButton(screen),"bounds"),"x"),"slider is not right of Caves");
          check(n(reset,"x")==sx+sw+4 && n(reset,"y")==sy,"reset does not follow slider");
          click(screen,sx+4,sy+8,0);
          check(screen.caveWindow().reference()==0,"slider minimum endpoint lost");
          release(screen,sx+sw-4,sy+8,-1);
          check((boolean)get(screen,"caveDragging"),"Beta motion event released slider ownership");
          release(screen,sx+sw-4,sy+8,0);
          check(screen.caveWindow().reference()==127 && !(boolean)get(screen,"dragging"),"release did not apply maximum or started panning");
          exact(screen,"89",true); check(screen.caveWindow().reference()==89,"exact entry did not commit");
          exact(screen,"91",false); check(screen.caveWindow().reference()==89,"exact entry Escape changed Y");
          check((boolean)call(screen,"caveWheel",new Class<?>[]{int.class,int.class,int.class},sx+10,sy+8,120),"slider wheel was not consumed");
          check(screen.caveWindow().reference()==90,"wheel did not advance one block");
          double scale=view.scale;
          call(screen,"caveWheel",new Class<?>[]{int.class,int.class,int.class},sx+10,sy+8,-120);
          check(screen.caveWindow().reference()==89 && view.scale==scale,"slider wheel changed map zoom");
          click(screen,n(reset,"x")+8,n(reset,"y")+8,0);
          check(screen.caveWindow().reference()==(int)Math.max(0,Math.min(127,Math.floor(Maps.y()))),"reset did not use current player Y");
          mode(screen); check(label(screen).equals("Surface"),"returning Surface label wrong");
          check(call(controls(screen),"caveSlider")==null && call(controls(screen),"caveReset")==null,"surface retained cave controls");
          check(!(boolean)call(screen,"cavePress",new Class<?>[]{int.class,int.class,int.class},sx+10,sy+8,0),"hidden slider consumed input");
          shot(mc,screen,size,"surface");
          mode(screen); exact(screen,"89",true);
        });
        test("CAVE MAP473 selected height drives real floors, buffer and light context " + size[0],()->{
          var sample = MapSampler.class.getDeclaredMethod("sample",net.minecraft.world.World.class,net.minecraft.class_43.class,
              MapTile.class,int.class,int.class,CaveWindow.class); sample.setAccessible(true);
          var nativeSampler = new MapSampler(new MapPalette());
          String firstContext = screen.caveContext(); int firstLayer = screen.lightLayer(Maps.dimension());
          for(int selected:new int[]{89,91,89}) {
            exact(screen,Integer.toString(selected),true);
            var window=screen.caveWindow();
            var key=MapMath.key(Maps.dimension(),window.layer(),0,x,z,window.context());
            memory.sample(key); MapIO.awaitQueued(); memory.publish(4,Long.MAX_VALUE);
            MapTile tile=memory.peek(key); check(tile!=null,"fixture tile was not admitted");
            for(int dx=0;dx<16;dx++)for(int dz=0;dz<16;dz++)sample.invoke(nativeSampler,mc.world,chunk,tile,x+dx,z+dz,window);
            int index=MapMath.index(x+4,z+4);
            check(tile.height(index)==selected-1 && tile.block(index)==(selected==89?1:5),"selected Y reused the wrong native floor");
            nativeSampler.tick(mc,memory,Maps.layer(),Maps.x(),Maps.y(),Maps.z());
            check((int)get(nativeSampler,"layer")==window.layer() && get(nativeSampler,"context").equals(window.context()),
                "tick sampler ignored the open map's cave window");
            check(memory.viewContext().equals(MapSampler.window(settings,Maps.y()).context()),"world map changed minimap cache context");
            MapLightShading.tick(mc,settings);
            check(getStatic(MapLightShading.class,"context").equals(window.context()),"spawn light shading kept a different cave context");
            for(int frame=0;frame<3;frame++)shot(mc,screen,size,"caves-y"+selected);
            Object buffer=get(screen,"buffer");
            check(get(buffer,"context").equals(window.context()),"world-map buffer retained the wrong height");
            if(selected==91)check(firstLayer==window.layer() && !firstContext.equals(window.context()),"fixture does not distinguish same-band cache keys");
          }
          exact(screen,"91",true);
          Object slider=call(controls(screen),"caveSlider"); click(screen,n(slider,"x")+8,n(slider,"y")+8,1);
          shot(mc,screen,size,"cave-exact"); key(screen,'\0',Keyboard.KEY_ESCAPE);
        });
      }
      test("CAVE MAP473 automatic caves and reset follow real freecam reporting",()->{
        settings.caveMode=1; MapConfig.preview(settings);
        position.invoke(camera,x+8.5,89+HeadGeometry.EYE_ABOVE_BASE,z+8.5); active.invoke(camera,true);
        Maps.tick(mc);
        check(DetachedCamera.isActive() && Maps.layer()>=0,"automatic cave fixture did not use freecam under its roof");
        var screen=new WorldMapScreen(previous); mc.setScreen(screen); screen.init(mc,320,240);
        check(label(screen).equals("Caves") && screen.caveWindow().reference()==89,"automatic caves did not expose current freecam Y");
        exact(screen,"42",true);
        position.invoke(camera,x+8.5,91+HeadGeometry.EYE_ABOVE_BASE,z+8.5); Maps.tick(mc);
        check(screen.caveWindow().reference()==42,"camera movement replaced fixed slider Y");
        Object reset=call(controls(screen),"caveReset"); click(screen,n(reset,"x")+8,n(reset,"y")+8,0);
        check(screen.caveWindow().reference()==91,"reset used physical player instead of freecam");
        position.invoke(camera,x+8.5,89+HeadGeometry.EYE_ABOVE_BASE,z+8.5); Maps.tick(mc);
        check(screen.caveWindow().reference()==89,"reset did not resume camera following");
        active.invoke(camera,false); Maps.tick(mc);
        var live=ReportedPosition.current(mc.player,mc.player.x,HudCoordinates.y(mc.player),mc.player.z,mc.player.yaw,mc.player.pitch);
        check(screen.caveWindow().reference()==(int)Math.max(0,Math.min(127,Math.floor(live.y()))),"leaving freecam retained detached Y");
        check(mc.player.x==playerX && mc.player.y==playerY && mc.player.z==playerZ,"map view moved the player");
      });
    } finally {
      active.invoke(camera,false); mc.setScreen(previous); MapLightShading.clear();
      setStatic(Maps.class,"cache",oldCache); setStatic(Maps.class,"sampler",oldSampler);
      MapConfig.preview(original);
      for(int[] block:savedBlocks)mc.world.method_154(block[0],block[1],block[2],block[3],block[4]);
      memory.finish(); Maps.tick(mc);
    }
    Files.writeString(REPORT.resolve(action+".json"),"{\"failures\":"+failures+"}");
    log("CAVE MAP473 "+action+" failures="+failures); check(failures==0,"cave-map-473 failures="+failures);
  }

  private static void shot(Minecraft mc,WorldMapScreen screen,int[] size,String name)throws Exception {
    SettingsSnapshot.render(mc,screen,size[0],size[1],REPORT.resolve(name+"-"+size[0]+".png"));
  }
  private static Object controls(WorldMapScreen screen)throws Exception {return call(screen,"controls");}
  private static Object modeButton(WorldMapScreen screen)throws Exception {
    for(Object button:(List<?>)call(controls(screen),"buttons")) if(call(button,"action").toString().equals("LAYER")) return button;
    throw new AssertionError("missing cave mode button");
  }
  private static String label(WorldMapScreen screen)throws Exception {return (String)call(modeButton(screen),"label");}
  private static void mode(WorldMapScreen screen)throws Exception {Object box=call(modeButton(screen),"bounds");click(screen,n(box,"x")+8,n(box,"y")+8,0);}
  private static void exact(WorldMapScreen screen,String text,boolean commit)throws Exception {
    Object box=call(controls(screen),"caveSlider");click(screen,n(box,"x")+8,n(box,"y")+8,1);
    for(char c:text.toCharArray())key(screen,c,Keyboard.KEY_NONE);
    key(screen,'\0',commit?Keyboard.KEY_RETURN:Keyboard.KEY_ESCAPE);
  }
  private static void click(WorldMapScreen screen,int x,int y,int button){((ScreenInput)(Object)screen).power$click(x,y,button);}
  private static void release(WorldMapScreen screen,int x,int y,int button){((ScreenInput)(Object)screen).power$release(x,y,button);}
  private static void key(WorldMapScreen screen,char c,int code){((ScreenInput)(Object)screen).power$key(c,code);}
  private static Field field(Class<?> owner,String name)throws Exception {Field f=owner.getDeclaredField(name);f.setAccessible(true);return f;}
  private static Object get(Object owner,String name)throws Exception {return field(owner.getClass(),name).get(owner);}
  private static Object getStatic(Class<?> owner,String name)throws Exception {return field(owner,name).get(null);}
  private static void setStatic(Class<?> owner,String name,Object value)throws Exception {field(owner,name).set(null,value);}
  private static Object call(Object owner,String name)throws Exception {return call(owner,name,new Class<?>[0]);}
  private static Object call(Object owner,String name,Class<?>[] types,Object... args)throws Exception {Method m=owner.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(owner,args);}
  private static int n(Object owner,String name)throws Exception {return ((Number)call(owner,name)).intValue();}
}
