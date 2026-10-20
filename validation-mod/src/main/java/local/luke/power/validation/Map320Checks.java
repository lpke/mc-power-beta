package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;

import java.nio.file.*;
import java.util.*;
import local.luke.power.map.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

/** Map control geometry and modal input stay on the live world-map host. Disposable clients only. */
final class Map320Checks {
  private static final Path REPORT=Path.of("power-beta-data/reports/release-320/map-ui");
  private static Object invoke(Object target,String name,Class<?>[] types,Object... args)throws Exception {
    return Editor230Checks.invoke(target,name,types,args);
  }
  private static Object control(WorldMapScreen map,String action)throws Exception {
    for(Object button:(List<?>)call(call(map,"controls"),"buttons"))if(call(button,"action").toString().equals(action))return button;
    throw new AssertionError("Missing map control "+action);
  }
  private static void press(WorldMapScreen map,int x,int y,int button){((ScreenInput)(Object)map).power$click(x,y,button);}
  private static void click(WorldMapScreen map,String action)throws Exception {
    Object bounds=call(control(map,action),"bounds");press(map,(int)call(bounds,"x")+3,(int)call(bounds,"y")+5,0);
  }
  private static void key(WorldMapScreen map,int key){((ScreenInput)(Object)map).power$key('\0',key);}
  private static boolean active(WorldMapScreen map)throws Exception{return (boolean)call(field(map,"coordinates"),"active");}
  private static TextInput coordinate(WorldMapScreen map,String axis)throws Exception{return (TextInput)field(field(map,"coordinates"),axis);}
  private static void render(Minecraft mc,WorldMapScreen map,String name,int w,int h)throws Exception {
    SettingsSnapshot.render(mc,map,w,h,REPORT.resolve(name+".png"));
  }
  private static boolean intersects(Object a,Object b)throws Exception {
    int x=(int)call(a,"x"),y=(int)call(a,"y"),w=(int)call(a,"width"),h=(int)call(a,"height");
    return x<(int)call(b,"x")+(int)call(b,"width")&&x+w>(int)call(b,"x")
        &&y<(int)call(b,"y")+(int)call(b,"height")&&y+h>(int)call(b,"y");
  }
  private static void checkControls(Minecraft mc,WorldMapScreen map)throws Exception {
    Object layout=call(map,"controls");List<?> buttons=(List<?>)call(layout,"buttons");
    check(buttons.size()==10,"map controls lost an action");
    int left=(int)call(map,"mapLeft");
    for(Object button:buttons){
      Object b=call(button,"bounds");String label=(String)call(button,"label");
      check((int)call(b,"x")>=left+8&&(int)call(b,"x")+(int)call(b,"width")<=map.width-8,"map control escaped pane: "+label);
      check((int)call(b,"width")>=mc.textRenderer.getWidth(label)+12,"label padding lost: "+label);
      check(!label.equals("Screenshot"),"Screenshot button remains");
      check(!intersects(b,call(layout,"scale")),"scale overlaps "+label);
      for(Object other:buttons)if(other!=button)check(!intersects(b,call(other,"bounds")),"buttons overlap at "+map.width+": "+label);
    }
    Object waypoint=call(control(map,"WAYPOINTS"),"bounds"),light=call(control(map,"LIGHT"),"bounds"),close=call(control(map,"CLOSE"),"bounds");
    check((int)call(waypoint,"x")==left+8&&(int)call(light,"x")==left+8,"Waypoints and overlay row lost left alignment");
    check((int)call(waypoint,"y")-(int)call(light,"y")==22,"left map block is not two rows");
    check((int)call(close,"y")==map.height-24,"Close left the bottom right");
  }

  static void run(Minecraft mc)throws Exception {
    failures=0;check(mc.world!=null&&mc.player!=null,"open disposable world first");Files.createDirectories(REPORT);
    var previous=mc.currentScreen;var settings=MapConfig.copy();boolean auto=MenuPreferences.current().autoApply;
    Object ui=Class.forName("local.luke.power.controls.ControlFeatures").getField("USER_INTERFACE_CONFIG").get(null);
    var clearField=ui.getClass().getField("clearTextFieldWithRightClick");
    Object rightClear=clearField.get(ui);
    PowerConfig.flushPending();byte[] original=Files.readAllBytes(PowerConfig.path());
    double px=mc.player.x,py=mc.player.y,pz=mc.player.z;
    WorldMapScreen map=new WorldMapScreen();
    try {
      MenuPreferences.current().autoApply=false;clearField.set(ui,true);
      var enabled=MapConfig.copy();enabled.worldMap=true;MapConfig.preview(enabled);Maps.tick(mc);
      mc.setScreen(map);map.init(mc,640,360);
      test("measured two-row map controls remain separate from scale and right actions",()->{
        for(int[] size:new int[][]{{320,240},{480,270},{640,360},{960,540}}){
          map.init(mc,size[0],size[1]);checkControls(mc,map);
          if(org.lwjgl.opengl.Display.getWidth()>=size[0]*2&&org.lwjgl.opengl.Display.getHeight()>=size[1]*2)
            render(mc,map,"controls-"+size[0],size[0],size[1]);
        }
      });
      test("Go to keeps world-map sampling active and owns all underlying input",()->{
        map.init(mc,640,360);var view=(MapMath.View)field(map,"view");double x=view.x,z=view.z,scale=view.scale;
        click(map,"GO_TO");check(mc.currentScreen==map&&active(map),"Go to replaced the active map screen");
        check(map.samplingView((int)field(map,"dimension"),(int)field(map,"layer"))!=null,"Go to removed the sampling interest");
        check(!map.radarIconsAllowed(),"Go to allowed held radar icons");
        press(map,2,2,0);press(map,2,2,1);key(map,Keyboard.KEY_LEFT);key(map,Keyboard.KEY_HOME);key(map,Keyboard.KEY_EQUALS);
        check(!(boolean)field(map,"dragging")&&!(boolean)field(map,"context"),"modal input started a map gesture");
        check(view.x==x&&view.z==z&&view.scale==scale,"modal input changed map view");
        coordinate(map,"x").setText("-31415");coordinate(map,"z").setText("27182");
        map.init(mc,320,240);
        check(active(map)&&coordinate(map,"x").text().equals("-31415")&&coordinate(map,"z").text().equals("27182"),"resize replaced coordinate draft");
        key(map,Keyboard.KEY_TAB);check(coordinate(map,"x").focused!=coordinate(map,"z").focused,"Tab did not focus one field");
        render(mc,map,"go-to-narrow",320,240);
        key(map,Keyboard.KEY_RETURN);
        check(mc.currentScreen==map&&!active(map)&&view.x==-31415&&view.z==27182,"Enter did not accept coordinates into view");
        check(map.radarIconsAllowed(),"accepted Go to retained icon suppression");
        click(map,"GO_TO");coordinate(map,"x").setText("999999999");key(map,Keyboard.KEY_RETURN);
        check(active(map)&&view.x==-31415,"invalid coordinates changed view or closed overlay");
        key(map,Keyboard.KEY_ESCAPE);check(!active(map)&&mc.currentScreen==map&&view.x==-31415,"Escape changed view or closed map");
      });
      test("coordinate right-click clear and canceled edits stay local",()->{
        map.init(mc,640,360);click(map,"GO_TO");render(mc,map,"go-to",640,360);
        Object dialog=field(map,"coordinates"),bounds=invoke(dialog,"bounds",new Class<?>[]{UiScreen.class},map);
        press(map,(int)call(bounds,"fieldX")+4,(int)call(bounds,"top")+36,1);
        check(coordinate(map,"x").focused&&coordinate(map,"x").text().isEmpty(),"right-click did not clear focused X input");
        key(map,Keyboard.KEY_ESCAPE);check(!active(map),"Escape did not close coordinate overlay");
      });
      test("sidecar toggles remain manual session drafts and Go to blocks panel input",()->{
        map.init(mc,960,540);click(map,"SETTINGS");var panel=(PowerOptionsScreen)field(map,"settingsPanel");
        check(panel!=null&&!map.radarIconsAllowed(),"settings did not suppress held radar icons");checkControls(mc,map);
        boolean light=MapConfig.current().lightOverlay,slime=MapConfig.current().slimeChunks,path=MapConfig.current().footsteps;
        click(map,"LIGHT");click(map,"SLIME");click(map,"PATH");
        check(MapConfig.current().lightOverlay!=light&&MapConfig.current().slimeChunks!=slime&&MapConfig.current().footsteps!=path,"quick toggles did not preview");
        check(panel.session().changes()>=3,"quick toggles bypassed the settings session");
        long changes=panel.session().changes();click(map,"GO_TO");press(map,14,150,0);key(map,Keyboard.KEY_HOME);
        check(active(map)&&mc.currentScreen==map&&panel.session().changes()==changes,"coordinate modal reached settings");
        render(mc,map,"go-to-sidecar",960,540);key(map,Keyboard.KEY_ESCAPE);
        panel.session().discard();check(MapConfig.current().lightOverlay==light&&MapConfig.current().slimeChunks==slime&&MapConfig.current().footsteps==path,"discard did not restore quick toggles");
        call(map,"closeSettings");check(map.radarIconsAllowed(),"closed sidecar retained icon suppression");
      });
      test("context and pointer ownership survive unchanged while screenshot uses its deferred flag",()->{
        map.init(mc,640,360);press(map,320,180,0);((ScreenInput)(Object)map).power$release(330,185,-1);
        check((boolean)field(map,"dragging"),"pointer motion ended map pan");((ScreenInput)(Object)map).power$release(320,180,0);
        check(!(boolean)field(map,"dragging"),"native release retained map pan");
        press(map,320,180,1);check((boolean)field(map,"context")&&!map.radarIconsAllowed(),"context allowed held radar icons");
        key(map,Keyboard.KEY_ESCAPE);check(map.radarIconsAllowed(),"context close retained icon suppression");
        map.requestScreenshot();check((boolean)field(map,"screenshot"),"screenshot request skipped deferred capture");field(map,"screenshot",false);
      });
      check(mc.player.x==px&&mc.player.y==py&&mc.player.z==pz,"map UI moved the player");
      check(Arrays.equals(original,Files.readAllBytes(PowerConfig.path())),"manual map UI checks wrote preferences");
    } finally {
      var panel=(PowerOptionsScreen)field(map,"settingsPanel");if(panel!=null){panel.session().discard();call(map,"closeSettings");}
      clearField.set(ui,rightClear);MenuPreferences.current().autoApply=auto;MapConfig.preview(settings);mc.setScreen(previous);
    }
    log("MAP320 COMPLETE failures="+failures);
  }
}
