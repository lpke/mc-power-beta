package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import local.luke.power.config.Catalog;
import local.luke.power.input.*;
import local.luke.power.map.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Native menu ownership and real pointer probes, restricted to a muted disposable client. */
final class Map449Checks {
  private static Session physical;
  private static String action;
  private Map449Checks() {}
  static void run(Minecraft mc,String command) throws Exception {
    if(command.equals("stop")) { if(physical!=null)physical.close();physical=null;return; }
    if(command.startsWith("prepare ")) {
      check(physical==null,"stop the previous physical case");
      action=command.substring(8);physical=new Session(mc);
      physical.host(action.startsWith("pause-"));
      int[] point=physical.point();
      physical.zoom=MapConfig.current().zoom;physical.waypoints=MapConfig.current().waypoints;
      log("MAP449 READY "+action+" pointer "+point[0]*mc.displayWidth/mc.currentScreen.width+","+point[1]*mc.displayHeight/mc.currentScreen.height);
      return;
    }
    if(command.equals("check")) {
      check(physical!=null,"prepare a physical case first");
      if(action.endsWith("wheel")) check(MapConfig.current().zoom!=physical.zoom&&MinimapQuickMenu.active(mc),"wheel did not zoom with menu open");
      else if(action.endsWith("right")) check(MapConfig.current().zoom==MapConfig.current().zoomDefault&&MinimapQuickMenu.active(mc),"right click did not reset with menu open");
      else if(action.endsWith("middle")) check(MapConfig.current().waypoints!=physical.waypoints&&MinimapQuickMenu.active(mc),"middle click did not toggle with menu open");
      else if(action.endsWith("left")) check(mc.currentScreen instanceof WorldMapScreen,"left click did not open World Map");
      else throw new IllegalArgumentException(action);
      check(physical.items.equals(physical.inventory.snapshot()),"physical map input changed native item data");
      log("PASS MAP449 physical "+action);return;
    }
    check(command.equals("run"),"use map-449 run|prepare inventory/pause-wheel/right/middle/left|check|stop");
    check(physical==null,"stop physical case first");failures=0;
    try(var state=new Session(mc)) {
      for(boolean pause:new boolean[]{false,true})test("MAP449 "+(pause?"Pause":"inventory")+" admits map actions under the open quick menu",()->{
        state.host(pause);int[] point=state.point();int x=point[0],y=point[1];
        double zoom=MapConfig.current().zoom;
        check(!MinimapQuickMenu.mouse(mc,x,y,-1,false),"quick menu claimed map motion/wheel");
        check(Minimap.inventoryWheel(mc,x,y,120),"map wheel was rejected");
        check(MapConfig.current().zoom!=zoom&&MinimapQuickMenu.active(mc),"wheel closed menu or failed to zoom");
        check(!MinimapQuickMenu.mouse(mc,x,y,1,true)&&Minimap.inventoryReset(mc,x,y),"map reset was rejected");
        check(MapConfig.current().zoom==MapConfig.current().zoomDefault&&MinimapQuickMenu.active(mc),"reset closed menu or wrong zoom");
        boolean visible=MapConfig.current().waypoints;
        check(!MinimapQuickMenu.mouse(mc,x,y,2,true)&&Minimap.inventoryMiddle(mc,x,y),"map middle click was rejected");
        check(MapConfig.current().waypoints!=visible&&MinimapQuickMenu.active(mc),"middle action lost its menu");
        Object modal=new Object();InventoryPanels.modal(mc.currentScreen,modal,true);
        try {check(!Minimap.inventoryWheel(mc,x,y,120)&&!Minimap.inventoryReset(mc,x,y)&&!Minimap.inventoryMiddle(mc,x,y)&&!MinimapMapVisit.open(mc,x,y),"map bypassed another modal");}
        finally {InventoryPanels.modal(mc.currentScreen,modal,false);}
        // A foreign modal dismisses the quick menu. Reopen before testing left click.
        state.openGear();point=state.point();x=point[0];y=point[1];
        check(!MinimapQuickMenu.mouse(mc,x,y,0,true)&&MinimapMapVisit.open(mc,x,y),"map left click was rejected");
        check(mc.currentScreen instanceof WorldMapScreen&&mc.currentScreen.shouldPause()==pause,"map lost origin pause ownership");
        ((UiScreen)mc.currentScreen).layerKey('\0',Keyboard.KEY_ESCAPE);
        check(mc.currentScreen==state.host,"map did not return to native host");
        check(state.items.equals(state.inventory.snapshot()),"map actions changed native inventory");
      });
      test("MAP449 quick-menu rows retain exclusive ownership",()->{
        state.host(false);var layout=state.layout();var row=layout.panel(0).row(0);
        boolean before=MapConfig.current().items;double zoom=MapConfig.current().zoom;
        check(MinimapQuickMenu.mouse(mc,row.x()+20,row.y()+10,0,true),"checkbox press leaked");
        check(MinimapQuickMenu.mouse(mc,row.x()+20,row.y()+10,0,false),"checkbox release leaked");
        check(MapConfig.current().items!=before&&MapConfig.current().zoom==zoom,"checkbox altered wrong setting");
        check(!Minimap.inventoryWheel(mc,row.x()+20,row.y()+10,120),"checkbox wheel reached terrain");
        check(InventoryPanels.modal(state.host),"inventory modal protection was removed");
      });
      test("MAP449 Pause alone hides world waypoint labels",()->{
        state.host(false);check(Minimap.waypointHost(mc.currentScreen),"inventory labels hidden");
        state.host(true);check(!Minimap.waypointHost(mc.currentScreen),"Pause labels visible");
        check(Minimap.waypointHost(null)&&Minimap.waypointHost(new net.minecraft.client.gui.screen.ChatScreen()),"game/chat labels hidden");
        check(!Minimap.waypointHost(new PowerOptionsScreen(null)),"Options admitted labels");
      });
      test("MAP449 World Map omits cave row and remembers visibility through visits and resize",()->{
        closeMenu(mc);field(WorldMapScreen.class,"rememberedTogglesVisible").setBoolean(null,true);
        var first=new WorldMapScreen();mc.setScreen(first);first.init(mc,640,360);
        var panel=(MapTogglePanel.Layout)call(first,"togglePanel");
        check(panel.toggles().equals(MapToggle.WORLD_MAP)&&!panel.toggles().contains(MapToggle.CAVES),"world-map row set includes cave mode");
        check(MinimapQuickMenuLayout.ROWS==MapToggle.ALL.size()&&MapToggle.ALL.get(4)==MapToggle.CAVES,"minimap cave action moved");
        String settings=Catalog.JSON.toJson(MapConfig.current());
        pressToggle(first);check(!field(WorldMapScreen.class,"togglesVisible").getBoolean(first),"Hide failed");
        first.init(mc,320,240);check(!field(WorldMapScreen.class,"togglesVisible").getBoolean(first),"resize forgot Hide");
        var second=new WorldMapScreen();mc.setScreen(second);check(!field(WorldMapScreen.class,"togglesVisible").getBoolean(second),"new visit forgot Hide");
        pressToggle(second);var third=new WorldMapScreen();mc.setScreen(third);
        check(field(WorldMapScreen.class,"togglesVisible").getBoolean(third),"new visit forgot Show");
        check(settings.equals(Catalog.JSON.toJson(MapConfig.current())),"panel visibility wrote preferences");
      });
    }
    log("MAP449 failures="+failures);check(failures==0,"map-449 failures="+failures);
  }
  private static final class Session implements AutoCloseable {
    final Minecraft mc;final MapSettings settings=MapConfig.copy();final Screen previous;
    final boolean remembered,hidden,debug;final Recipe402Checks.State inventory;Screen host;String items;
    double zoom;boolean waypoints;
    Session(Minecraft mc)throws Exception {
      check(mc.world!=null&&mc.player!=null&&!mc.world.isRemote&&Display.isActive(),"focus a disposable singleplayer world");
      check(mc.options.soundVolume==0&&mc.options.musicVolume==0,"mute disposable client");
      this.mc=mc;previous=mc.currentScreen;hidden=mc.options.hideHud;debug=mc.options.debugHud;
      remembered=field(WorldMapScreen.class,"rememberedTogglesVisible").getBoolean(null);inventory=new Recipe402Checks.State(mc);
    }
    void host(boolean pause)throws Exception {
      closeMenu(mc);inventory.reset(640);mc.options.hideHud=mc.options.debugHud=false;
      if(pause) {mc.setScreen(new net.minecraft.class_525());mc.currentScreen.init(mc,640,360);}
      host=mc.currentScreen;var next=MapConfig.copy();next.minimap=next.worldMap=next.showInInventory=true;
      next.hideWithDebug=false;next.size=80;next.corner=0;next.offsetX=next.offsetY=6;
      next.zoom=2.5;next.zoomDefault=1.25;next.pauseMap=false;next.middleClickAction=2;MapConfig.preview(next);Maps.tick(mc);
      Mouse.setGrabbed(false);host.render(-1,-1,1);openGear();items=inventory.snapshot();
    }
    MinimapQuickMenuLayout.Layout layout() {var s=MapConfig.current();return MinimapQuickMenuLayout.at(Minimap.layout(s,host.width,host.height,false),s.circular,s.corner,host.width,host.height,MapTogglePanel.labelWidth(mc));}
    void openGear() {if(MinimapQuickMenu.active(mc))return;MinimapQuickMenu.beginFrame();var b=layout().gear();check(MinimapQuickMenu.mouse(mc,b.x()+8,b.y()+8,0,true),"gear did not open");MinimapQuickMenu.mouse(mc,b.x()+8,b.y()+8,0,false);}
    int[] point() {var gui=(InventoryGui)host;var l=layout();var map=Minimap.layout(MapConfig.current(),host.width,host.height,false);int cx=map.x()+map.size()/2,cy=map.y()+map.size()/2;if(!l.menu().contains(cx,cy)&&!l.gear().containsCircle(cx,cy)&&!gui.power$containsGuiSpace(cx,cy))return new int[]{cx,cy};for(int y=map.y()+8;y<map.y()+map.size()-8;y+=2)for(int x=map.x()+8;x<map.x()+map.size()-8;x+=2)
      if(Minimap.inventoryContains(mc,x,y)&&!l.menu().contains(x,y)&&!l.gear().containsCircle(x,y)&&!gui.power$containsGuiSpace(x,y))return new int[]{x,y};throw new AssertionError("no exposed map pixel");}
    public void close()throws Exception {closeMenu(mc);if(mc.currentScreen instanceof WorldMapScreen map&&field(WorldMapScreen.class,"parent").get(map)==host)map.layerKey('\0',Keyboard.KEY_ESCAPE);inventory.close();field(WorldMapScreen.class,"rememberedTogglesVisible").setBoolean(null,remembered);MapConfig.queued(settings);PowerConfig.flushPending();mc.options.hideHud=hidden;mc.options.debugHud=debug;mc.setScreen(previous);}
  }
  private static void closeMenu(Minecraft mc)throws Exception {if(MinimapQuickMenu.active(mc)) {Method close=MinimapQuickMenu.class.getDeclaredMethod("close");close.setAccessible(true);close.invoke(null);}}
  private static void pressToggle(WorldMapScreen screen)throws Exception {
    Object controls=call(screen,"controls");Method buttons=controls.getClass().getDeclaredMethod("buttons");buttons.setAccessible(true);
    for(Object b:(java.util.List<?>)buttons.invoke(controls)) {Object action=call(b,"action");if(!action.toString().equals("TOGGLES"))continue;Object box=call(b,"bounds");int x=(int)call(box,"x")+3,y=(int)call(box,"y")+3;screen.layerPress(x,y,0);screen.layerRelease(x,y,0);return;}
    throw new AssertionError("missing toggle button");
  }
  private static Object call(Object object,String name)throws Exception {Method m=object.getClass().getDeclaredMethod(name);m.setAccessible(true);return m.invoke(object);}
  private static Field field(Class<?> type,String name)throws Exception {Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
}
