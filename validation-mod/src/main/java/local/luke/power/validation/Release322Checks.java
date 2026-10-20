package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.JsonPrimitive;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.map.*;
import local.luke.power.rules.GameRule;
import local.luke.power.sprint.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

/** Version-specific disposable-client checks; hold phases use physical input. */
final class Release322Checks {
  private static final Path REPORT=Path.of("power-beta-data/reports/release-322");
  private static WorldMapScreen heldMap;
  private static PowerOptionsScreen heldPanel;
  private static MapSettings beforeHeld;
  private static Object selectedBefore,scrollBefore;
  private static Object get(Object target,String name)throws Exception{return Editor230Checks.get(target,name);}
  private static Object call(Object target,String name,Class<?>[] types,Object...args)throws Exception{return Editor230Checks.invoke(target,name,types,args);}
  private static Object call(Object target,String name)throws Exception{return call(target,name,new Class<?>[0]);}
  private static void set(Object target,String name,Object value)throws Exception{
    for(Class<?> c=target.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);f.set(target,value);return;}catch(NoSuchFieldException ignored){}
    throw new NoSuchFieldException(name);
  }
  private static void key(Screen screen,int key){((ScreenInput)(Object)screen).power$key('\0',key);}
  private static void press(Screen screen,int x,int y){((ScreenInput)(Object)screen).power$click(x,y,0);}
  private static void open(Minecraft mc,Screen screen,int w,int h){mc.setScreen(screen);screen.init(mc,w,h);}
  private static void activate(PowerOptionsScreen owner,String id)throws Exception{call(owner,"activate",new Class<?>[]{Setting.class,int.class},find(owner.session(),id),1);}

  static void run(Minecraft mc,String phase)throws Exception{
    Files.createDirectories(REPORT);failures=0;
    switch(phase){
      case "ui" -> ui(mc);
      case "lock-read" -> {
        check(MapConfig.current().worldMapRadarIcons,"Mobs preference did not survive restart");
        var map=new WorldMapScreen();open(mc,map,960,540);
        check(MapKeys.iconsShown(mc,false,MapConfig.current()),"restored world lock inactive");
        check(!MapKeys.iconsShown(mc,true,MapConfig.current()),"restored lock leaked to minimap");
        SettingsSnapshot.render(mc,map,960,540,REPORT.resolve("mobs-lock-after-restart.png"));
        log("PASS world-only Mobs preference survives a real restart");
      }
      case "sidebar-prepare" -> {
        beforeHeld=MapConfig.copy();var settings=MapConfig.copy();settings.worldMap=settings.minimap=settings.radar=true;settings.worldMapRadarIcons=false;MapConfig.preview(settings);
        heldMap=new WorldMapScreen();open(mc,heldMap,960,540);call(heldMap,"toggleSettings");heldPanel=(PowerOptionsScreen)get(heldMap,"settingsPanel");
        selectedBefore=get(heldPanel,"selected");scrollBefore=get(heldPanel,"scroll");
        check(!MapKeys.iconsHeld(mc),"release Tab before prepare");log("SIDEBAR READY hold physical Tab");
      }
      case "sidebar-held" -> {
        check(mc.currentScreen==heldMap&&MapKeys.iconsHeld(mc),"sidebar blocked physical Tab heads");
        check(get(heldPanel,"selected").equals(selectedBefore)&&get(heldPanel,"scroll").equals(scrollBefore),"Tab navigated settings");
        check(MapKeys.iconsShown(mc,false,MapConfig.current())&&MapKeys.iconsShown(mc,true,MapConfig.current()),"held key did not reach both views");
        var search=(TextInput)get(heldPanel,"search");search.focused=true;check(!MapKeys.iconsHeld(mc),"search lost keyboard ownership");search.focused=false;
        set(heldPanel,"capture",find(heldPanel.session(),"maps.radarIconSize"));check(!MapKeys.iconsHeld(mc),"binding capture lost ownership");set(heldPanel,"capture",null);
        SettingsSnapshot.render(mc,heldMap,960,540,REPORT.resolve("sidebar-tab-heads.png"));log("PASS real Tab with sidebar; no row navigation; text/capture retain ownership");
      }
      case "sidebar-released" -> {
        check(!MapKeys.iconsHeld(mc),"release physical Tab");check(!MapKeys.iconsShown(mc,false,MapConfig.current()),"hold did not release");
        call(heldMap,"toggleOverlay",new Class<?>[]{Class.forName("local.luke.power.ui.WorldMapControls$Action")},action("MOBS"));
        check(MapKeys.iconsShown(mc,false,MapConfig.current()),"Mobs On did not lock heads");check(!MapKeys.iconsShown(mc,true,MapConfig.current()),"lock leaked to minimap");
        var settings=MapConfig.copy();settings.radar=false;MapConfig.preview(settings);check(!MapKeys.iconsShown(mc,false,settings),"disabled radar shows heads");
        heldPanel.session().discard();MapConfig.preview(beforeHeld);mc.setScreen(null);heldMap=null;heldPanel=null;
        log("PASS world-only Mobs lock respects disabled radar");
      }
      case "flight-creative","flight-survival" -> prepareFlight(mc,phase.endsWith("creative")?"CREATIVE":"SURVIVAL");
      case "land" -> {
        check(Bindings.down(mc.options.forwardKey),"hold physical W");check(SprintView.active(SprintView.Kind.CREATIVE),"flight not sprinting");
        call(mc.player,"creative_setFlying",new Class<?>[]{boolean.class},false);SprintController.movement(mc.player);
        check(SprintController.active(mc.player),"flight sprint did not transfer");log("PASS flight sprint transfers with physical forward held; wait for landing");
      }
      case "land-check" -> {
        check(!(boolean)call(mc.player,"creative_isFlying"),"flight still enabled");check(mc.player.field_1623,"not landed yet");check(SprintController.active(mc.player),"sprint stopped on landing");log("PASS sprint remains active after native landing");
      }
      case "stopped" -> {check(!Bindings.down(mc.options.forwardKey),"release W");check(!SprintController.active(mc.player),"released forward retained sprint");log("PASS releasing forward cancels inherited sprint");}
      default -> throw new IllegalArgumentException("release-322 ui|sidebar-prepare|sidebar-held|sidebar-released|flight-creative|flight-survival|land|land-check|stopped");
    }
    log("RELEASE322 "+phase+" FAILURES "+failures);
  }
  @SuppressWarnings({"unchecked","rawtypes"}) private static Object action(String name)throws Exception{return Enum.valueOf((Class)Class.forName("local.luke.power.ui.WorldMapControls$Action"),name);}

  private static void ui(Minecraft mc)throws Exception{
    var previous=mc.currentScreen;var before=MapConfig.copy();boolean auto=MenuPreferences.current().autoApply;PowerConfig.flushPending();var document=PowerConfig.document();byte[] bytes=Files.readAllBytes(PowerConfig.path());
    try{
      MenuPreferences.current().autoApply=false;
      test("Minimap Edit Escape/Back returns directly to Options and retains manual drafts",()->{
        for(boolean escape:new boolean[]{true,false}){
          var owner=new PowerOptionsScreen(null);open(mc,owner,960,540);set(owner,"page","Map");
          var unrelated=find(owner.session(),"gameplay.boatPlacementFacing");unrelated.cycle(1);owner.changed(unrelated);var draft=unrelated.value.deepCopy();
          activate(owner,"maps.hudEditor");var map=(WorldMapScreen)mc.currentScreen;map.init(mc,960,540);var panel=(PowerOptionsScreen)get(map,"settingsPanel");
          var size=find(owner.session(),"maps.size");size.value=new JsonPrimitive(97);panel.changed(size);
          if(escape)key(map,Keyboard.KEY_ESCAPE);else press(panel,(int)call(panel,"left")+10,32);
          check(mc.currentScreen==owner,"did not return directly to original Options");check(unrelated.value.equals(draft)&&size.value.getAsInt()==97,"return lost drafts");
          check(Arrays.equals(bytes,Files.readAllBytes(PowerConfig.path())),"Back saved manual draft");owner.session().discard();
        }
      });
      test("nested sidebar views return to Minimap before Options",()->{
        var owner=new PowerOptionsScreen(null);open(mc,owner,960,540);activate(owner,"maps.hudEditor");var map=(WorldMapScreen)mc.currentScreen;map.init(mc,960,540);var panel=(PowerOptionsScreen)get(map,"settingsPanel");
        call(panel,"minimapAppearance",new Class<?>[]{String.class},"maps.size");key(map,Keyboard.KEY_ESCAPE);check(mc.currentScreen==map,"nested Back skipped Minimap");key(map,Keyboard.KEY_ESCAPE);check(mc.currentScreen==owner,"root Back lost owner");owner.session().discard();
      });
      test("zoom footer clicks work at narrow/wide sizes with isolated drafts",()->{
        for(int width:new int[]{320,640,960}){
          var owner=new PowerOptionsScreen(null);open(mc,owner,width,360);var steps=find(owner.session(),"maps.zoomSteps");var original=steps.value.deepCopy();activate(owner,"maps.zoomSteps");var editor=mc.currentScreen;editor.init(mc,width,240);
          SettingsSnapshot.render(mc,editor,width,240,REPORT.resolve("zoom-steps-"+width+".png"));Object footer=call(editor,"footer");int[] xs=(int[])call(footer,"x"),spans=(int[])call(footer,"width");
          press(editor,xs[0]+spans[0]/2,212);check(((List<?>)get(editor,"values")).size()==original.getAsJsonArray().size()+1,"Add missed");
          press(editor,xs[2]+spans[2]/2,212);check(((List<?>)get(editor,"values")).size()==original.getAsJsonArray().size(),"Undo missed");
          press(editor,xs[1]+spans[1]/2,212);press(editor,xs[3]+spans[3]/2,212);check(mc.currentScreen==owner,"Done missed");owner.session().discard();
        }
      });
    }finally{PowerConfig.write(document);MapConfig.preview(before);MenuPreferences.current().autoApply=auto;mc.setScreen(previous);}
  }
  private static void prepareFlight(Minecraft mc,String mode)throws Exception{
    mc.setScreen(null);AccessRules.set(mc,GameRule.ALLOW_SPRINTING,true);AccessRules.set(mc,GameRule.ALLOW_FLIGHT,true);AccessRules.set(mc,GameRule.ALLOW_CREATIVE_MODE,true);
    CreativeVehicleChecks.mode(mc,mode);call(mc.player,"power_flightOverride",new Class<?>[]{int.class},1);call(mc.player,"creative_setFlying",new Class<?>[]{boolean.class},true);
    int x=(int)Math.floor(mc.player.x),z=(int)Math.floor(mc.player.z),floor=(int)Math.floor(mc.player.boundingBox.minY)-1;
    for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=30;dz++){
      check(mc.world.method_239(x+dx,floor,z+dz),"platform left loaded terrain");mc.world.method_154(x+dx,floor,z+dz,1,0);
      for(int dy=1;dy<=8;dy++)mc.world.method_154(x+dx,floor+dy,z+dz,0,0);
    }
    mc.player.method_1340(x+.5,floor+6,z+.5);mc.player.yaw=mc.player.pitch=0;mc.player.field_1623=false;mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;
    var config=Class.forName("local.luke.power.creative.config.Config");var settings=call(config.getMethod("current").invoke(null),"copy");set(settings,"landStopsFlight",false);config.getMethod("preview",settings.getClass()).invoke(null,settings);
    log("FLIGHT READY "+mode+": hold W and tap left Ctrl");
  }
}
