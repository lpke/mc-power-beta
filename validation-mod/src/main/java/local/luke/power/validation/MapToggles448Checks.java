package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import local.luke.power.config.Catalog;
import local.luke.power.map.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Native UI checks and owner-review images. Run only in a muted disposable world. */
public final class MapToggles448Checks {
  private static final Path REPORT=Path.of("power-beta-data/reports/map-toggles-448");
  private static WheelState wheelState;
  private MapToggles448Checks() {}
  public static void run(Minecraft mc,String action) throws Exception {
    action=action.trim();
    if(action.equals("stop")) {if(wheelState!=null){wheelState.close(mc);wheelState=null;}return;}
    if(action.startsWith("prepare-wheel ")) {
      check(wheelState==null,"stop the previous wheel check first");
      wheelState=new WheelState(mc,action.substring(14).trim());return;
    }
    if(action.equals("capture-current")) {
      check(wheelState!=null,"prepare-wheel map|gear first");
      check(mc.options.soundVolume==0&&mc.options.musicVolume==0,"mute disposable client");
      Files.createDirectories(REPORT);
      capture(mc,mc.currentScreen,mc.currentScreen.width,mc.currentScreen.height,"settled-"+wheelState.kind);
      return;
    }
    if(action.equals("check-wheel")) {check(wheelState!=null,"prepare-wheel map|gear first");wheelState.checkState(mc);return;}
    check(wheelState==null,"stop the physical wheel check first");
    boolean capture=action.equals("capture");
    check(capture||action.isBlank()||action.trim().equals("run"),"use map-toggles-448 run|capture");
    check(mc.world!=null&&mc.player!=null&&!mc.world.isRemote&&Display.isActive(),"focus a disposable singleplayer world");
    check(mc.options.soundVolume==0&&mc.options.musicVolume==0,"mute the disposable client");
    PowerConfig.flushPending();
    var before=MapConfig.copy();var screen=mc.currentScreen;
    boolean grabbed=Mouse.isGrabbed(),auto=MenuPreferences.current().autoApply,hidden=mc.options.hideHud,debug=mc.options.debugHud;
    boolean toggleVisibility=field(WorldMapScreen.class,"rememberedTogglesVisible").getBoolean(null);
    field(WorldMapScreen.class,"rememberedTogglesVisible").setBoolean(null,true);
    failures=0;
    try {
      var style=MapConfig.copy();style.minimap=style.worldMap=style.showInInventory=style.radar=true;
      style.hostile=style.animals=true;
      style.size=64;style.corner=1;style.offsetX=style.offsetY=6;
      MapConfig.preview(style);Maps.tick(mc);Mouse.setGrabbed(false);
      mc.options.hideHud=mc.options.debugHud=false;
      if(capture) {
        Files.createDirectories(REPORT);
        for(int[] size:new int[][]{{640,360},{320,240}}) {
          var map=open(mc,size[0],size[1]);
          capture(mc,map,size[0],size[1],"world-map-"+size[0]);
          // Use measured native navigation bounds rather than guessing which footer row wrapped.
          pressAction(map,"TOGGLES");capture(mc,map,size[0],size[1],"world-map-hidden-"+size[0]);
          pressAction(map,"TOGGLES");
          field(WorldMapScreen.class,"toggleScroll").setInt(map,9);
          capture(mc,map,size[0],size[1],"world-map-scrolled-"+size[0]);
        }
        var map=open(mc,800,360);call(map,"toggleSettings");
        capture(mc,map,800,360,"world-map-settings");
        field(WorldMapScreen.class,"toggleScroll").setInt(map,9);
        capture(mc,map,800,360,"world-map-settings-scrolled");
        movePreviewOverToggles(map);
        capture(mc,map,800,360,"world-map-settings-overlap");
        var pause=new net.minecraft.class_525();mc.setScreen(pause);pause.init(mc,640,360);Mouse.setGrabbed(false);
        openGear(mc);capture(mc,pause,640,360,"minimap-gear");
        return;
      }
      test("MAPTOGGLES448 every checkbox changes its retained setting and cannot start a map drag",()->{
        var map=open(mc,320,240);
        for(int index=0;index<MapToggle.WORLD_MAP.size();index++) {
          field(WorldMapScreen.class,"toggleScroll").setInt(map,index);
          var panel=(MapTogglePanel.Layout<MapToggle>)call(map,"togglePanel");
          check(index>=panel.first()&&index<panel.first()+panel.visible(),"row unavailable after scroll");
          var toggle=panel.toggle(index);boolean enabled=toggle.enabled(MapConfig.current(),false);
          var row=panel.row(index);map.layerPress(row.x()+20,row.y()+10,0);
          check(toggle.enabled(MapConfig.current(),false)!=enabled,"checkbox did not change "+toggle);
          check(!field(WorldMapScreen.class,"dragging").getBoolean(map),"checkbox started map drag");
          map.layerRelease(row.x()+20,row.y()+10,0);
          map.layerPress(row.x()+20,row.y()+10,0);map.layerRelease(row.x()+20,row.y()+10,0);
          check(toggle.enabled(MapConfig.current(),false)==enabled,"second click did not restore "+toggle);
        }
        pressAction(map,"TOGGLES");check(!field(WorldMapScreen.class,"togglesVisible").getBoolean(map),"hide failed");
        check(!field(WorldMapScreen.class,"togglesVisible").getBoolean(open(mc,320,240)),"new visit lost hidden toggle preference");
        field(WorldMapScreen.class,"rememberedTogglesVisible").setBoolean(null,true);
      });
      test("MAPTOGGLES448 middle-click IDs 0 through 9 retain their actions and head state stays visit-local",()->{
        var middle=Minimap.class.getDeclaredMethod("middleClick",Minecraft.class);middle.setAccessible(true);
        var reset=MapKeys.class.getDeclaredMethod("resetIcons");reset.setAccessible(true);reset.invoke(null);
        for(int index=0;index<10;index++) {
          var chosen=MapConfig.copy();chosen.middleClickAction=index;MapConfig.preview(chosen);
          var toggle=MapToggle.ALL.get(index);
          boolean enabled=toggle.enabled(MapConfig.current(),index==5);
          String original=Catalog.JSON.toJson(MapConfig.current());
          middle.invoke(null,mc);
          check(toggle.enabled(MapConfig.current(),index==5)!=enabled,"wrong middle action "+index);
          if(index==5)check(original.equals(Catalog.JSON.toJson(MapConfig.current())),"heads wrote preferences");
          middle.invoke(null,mc);
          check(toggle.enabled(MapConfig.current(),index==5)==enabled,"middle action did not toggle off "+index);
        }
        reset.invoke(null);
      });
      test("MAPTOGGLES448 sidebar checkboxes use a single manual draft without saving",()->{
        PowerConfig.flushPending();String document=PowerConfig.document().toString();
        MenuPreferences.current().autoApply=false;
        var map=open(mc,800,360);call(map,"toggleSettings");
        var panel=field(WorldMapScreen.class,"settingsPanel").get(map);
        check(panel!=null,"settings sidebar missing");
        var toggle=WorldMapScreen.class.getDeclaredMethod("toggleOverlay",MapToggle.class);toggle.setAccessible(true);
        var prior=MapConfig.copy();toggle.invoke(map,MapToggle.MOBS);
        check(MapConfig.current().hostile==MapConfig.current().animals,"mob filter was only partially changed");
        check(MapConfig.current().hostile!= (prior.hostile&&prior.animals),"mob filter did not preview");
        check(document.equals(PowerConfig.document().toString()),"manual map draft saved before Apply");
        toggle.invoke(map,MapToggle.WAYPOINTS);
        check(MapConfig.current().waypoints!=prior.waypoints,"waypoint visibility did not preview");
        check(document.equals(PowerConfig.document().toString()),"manual waypoint draft saved before Apply");
        call(map,"toggleSettings");
        check(field(PowerOptionsScreen.class,"confirmClose").getBoolean(panel),"dirty sidebar did not confirm exit");
        map.layerPress(800/2+12,360/2+20,0);map.layerRelease(800/2+12,360/2+20,0);
        check(field(WorldMapScreen.class,"settingsPanel").get(map)==null,"Discard did not close the sidebar");
        check(MapConfig.current().hostile==prior.hostile&&MapConfig.current().animals==prior.animals,"Discard did not restore both mob filters");
        check(MapConfig.current().waypoints==prior.waypoints,"Discard did not restore waypoint visibility");
        check(document.equals(PowerConfig.document().toString()),"Discard wrote preferences");
      });
      test("MAPTOGGLES448 front checkbox owns both buttons over minimap preview and context remains modal",()->{
        MenuPreferences.current().autoApply=false;
        var map=open(mc,800,360);call(map,"toggleSettings");movePreviewOverToggles(map);
        var panel=(MapTogglePanel.Layout<MapToggle>)call(map,"togglePanel");
        int index=panel.first()+panel.visible()-1;var row=panel.row(index);
        int x=row.x()+20,y=row.y()+10;
        // Rendering places the left-bottom preview behind these rows. Publish its hit area
        // directly here so callback checks do not require a capture-size framebuffer.
        field(WorldMapScreen.class,"minimapHit").set(map,new MinimapGeometry.Box(row.x(),row.y(),row.width(),row.height()));
        var options=field(WorldMapScreen.class,"settingsPanel").get(map);
        String mapView=(String)field(PowerOptionsScreen.class,"mapView").get(options);
        var toggle=panel.toggle(index);boolean enabled=toggle.enabled(MapConfig.current(),false);
        double zoom=MapConfig.current().zoom;
        map.layerPress(x,y,0);map.layerRelease(x,y,0);
        check(toggle.enabled(MapConfig.current(),false)!=enabled,"front checkbox lost left click to preview");
        check(mapView.equals(field(PowerOptionsScreen.class,"mapView").get(options)),"checkbox opened minimap appearance");
        map.layerPress(x,y,1);map.layerRelease(x,y,1);
        check(MapConfig.current().zoom==zoom,"front checkbox leaked right click into preview zoom reset");
        check(!field(WorldMapScreen.class,"context").getBoolean(map),"checkbox right click opened map context");
        check(!field(WorldMapScreen.class,"dragging").getBoolean(map),"checkbox overlap started map drag");
        boolean after=toggle.enabled(MapConfig.current(),false);
        map.layerPress(760,80,1);map.layerRelease(760,80,1);
        check(field(WorldMapScreen.class,"context").getBoolean(map),"terrain context did not open");
        map.layerPress(x,y,0);map.layerRelease(x,y,0);
        check(!field(WorldMapScreen.class,"context").getBoolean(map),"context outside click did not dismiss");
        check(toggle.enabled(MapConfig.current(),false)==after,"context click changed checkbox behind it");
        check(mapView.equals(field(PowerOptionsScreen.class,"mapView").get(options)),"context click reached preview");
      });
      test("MAPTOGGLES448 disabled spawn overlay stops native survey work on the next tick",()->{
        var disabled=MapConfig.copy();disabled.lightOverlay=false;MapConfig.preview(disabled);
        MapLightShading.tick(mc,disabled);check(MapLightShading.checksLastTick==0,"disabled light survey kept working");
      });
    } finally {
      field(WorldMapScreen.class,"rememberedTogglesVisible").setBoolean(null,toggleVisibility);
      MenuPreferences.current().autoApply=auto;
      MapConfig.queued(before);PowerConfig.flushPending();
      mc.options.hideHud=hidden;mc.options.debugHud=debug;
      mc.setScreen(screen);Mouse.setGrabbed(grabbed);
    }
    log("MAPTOGGLES448 failures="+failures);check(failures==0,"map-toggles-448 failures="+failures);
  }
  private static void movePreviewOverToggles(WorldMapScreen map) throws Exception {
    var moved=MapConfig.copy();moved.corner=2;moved.offsetX=moved.offsetY=0;moved.size=108;
    MapConfig.preview(moved);
    field(WorldMapScreen.class,"minimapStyleSource").set(map,null);
  }
  private static final class WheelState {
    final MapSettings before=MapConfig.copy();
    final Screen previous;
    final boolean grabbed;
    final String kind,document;
    final int first;
    final double zoom;
    WheelState(Minecraft mc,String kind) throws Exception {
      check(kind.equals("map")||kind.equals("gear"),"use prepare-wheel map|gear");
      check(mc.world!=null&&mc.player!=null&&!mc.world.isRemote&&Display.isActive(),"focus disposable singleplayer");
      check(mc.options.soundVolume==0&&mc.options.musicVolume==0,"mute disposable client");
      this.kind=kind;previous=mc.currentScreen;grabbed=Mouse.isGrabbed();
      PowerConfig.flushPending();document=PowerConfig.document().toString();
      var style=MapConfig.copy();style.minimap=style.worldMap=style.showInInventory=true;
      style.size=64;style.corner=1;style.offsetX=style.offsetY=6;MapConfig.preview(style);Maps.tick(mc);
      MinimapQuickMenuLayout.Rect box;
      if(kind.equals("map")) {
        var map=open(mc,320,240);box=((MapTogglePanel.Layout<MapToggle>)call(map,"togglePanel")).bounds();
        first=field(WorldMapScreen.class,"toggleScroll").getInt(map);zoom=((MapMath.View)field(WorldMapScreen.class,"view").get(map)).scale;
      } else {
        var pause=new net.minecraft.class_525();mc.setScreen(pause);pause.init(mc,320,180);Mouse.setGrabbed(false);openGear(mc);
        var layout=(MinimapQuickMenuLayout.Layout)field(MinimapQuickMenu.class,"cachedLayout").get(null);box=layout.menu();
        first=field(MinimapQuickMenu.class,"firstRow").getInt(null);zoom=MapConfig.current().zoom;
      }
      int x=box.x()+box.width()/2,y=box.y()+box.height()/2;
      log("MAPTOGGLES448 WHEEL READY "+kind+"; move window pointer to "+(x*mc.displayWidth/mc.currentScreen.width)+","+(y*mc.displayHeight/mc.currentScreen.height)+" and send one wheel-down; then map-toggles-448 check-wheel; map-toggles-448 stop");
    }
    void checkState(Minecraft mc) throws Exception {
      int after;
      if(kind.equals("map")) {
        check(mc.currentScreen instanceof WorldMapScreen,"map screen changed");
        after=field(WorldMapScreen.class,"toggleScroll").getInt(mc.currentScreen);
        check(((MapMath.View)field(WorldMapScreen.class,"view").get(mc.currentScreen)).scale==zoom,"toggle wheel zoomed terrain");
        check(!field(WorldMapScreen.class,"dragging").getBoolean(mc.currentScreen),"wheel began map drag");
      } else {
        check(MinimapQuickMenu.active(mc),"gear closed during wheel input");
        after=field(MinimapQuickMenu.class,"firstRow").getInt(null);
        check(MapConfig.current().zoom==zoom,"gear wheel zoomed minimap");
      }
      check(after>first,"native wheel did not scroll quick settings");
      check(document.equals(PowerConfig.document().toString()),"wheel wrote settings");
      log("MAPTOGGLES448 WHEEL PASS "+kind+" first="+first+" after="+after);
    }
    void close(Minecraft mc) throws Exception {
      Method close=MinimapQuickMenu.class.getDeclaredMethod("close");close.setAccessible(true);close.invoke(null);
      mc.setScreen(previous);MapConfig.preview(before);Mouse.setGrabbed(grabbed);
    }
  }
  private static WorldMapScreen open(Minecraft mc,int width,int height) {
    var map=new WorldMapScreen();mc.setScreen(map);map.init(mc,width,height);Mouse.setGrabbed(false);return map;
  }
  private static void openGear(Minecraft mc) {
    var settings=MapConfig.current();var screen=mc.currentScreen;
    var layout=MinimapQuickMenuLayout.at(Minimap.layout(settings,screen.width,screen.height,false),settings.circular,settings.corner,screen.width,screen.height,MapTogglePanel.labelWidth(mc));
    var gear=layout.gear();check(MinimapQuickMenu.mouse(mc,gear.x()+8,gear.y()+8,0,true),"gear click unavailable");
    MinimapQuickMenu.mouse(mc,gear.x()+8,gear.y()+8,0,false);
    check(MinimapQuickMenu.active(mc),"gear did not open");
  }
  private static void pressAction(WorldMapScreen screen,String action) throws Exception {
    Object controls=call(screen,"controls");
    Method buttons=controls.getClass().getDeclaredMethod("buttons");buttons.setAccessible(true);
    for(Object button:(java.util.List<?>)buttons.invoke(controls)) {
      Method kind=button.getClass().getDeclaredMethod("action");kind.setAccessible(true);
      if(!kind.invoke(button).toString().equals(action))continue;
      Method bounds=button.getClass().getDeclaredMethod("bounds");bounds.setAccessible(true);Object box=bounds.invoke(button);
      Method x=box.getClass().getDeclaredMethod("x"),y=box.getClass().getDeclaredMethod("y");x.setAccessible(true);y.setAccessible(true);
      int px=(int)x.invoke(box)+3,py=(int)y.invoke(box)+3;screen.layerPress(px,py,0);screen.layerRelease(px,py,0);return;
    }
    throw new AssertionError("missing navigation action "+action);
  }
  private static Object call(Object owner,String name) throws Exception {
    Method method=owner.getClass().getDeclaredMethod(name);method.setAccessible(true);return method.invoke(owner);
  }
  private static Field field(Class<?> type,String name) throws Exception {Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
  private static void capture(Minecraft mc,Screen screen,int width,int height,String name) throws Exception {
    int nativeScale=mc.options.guiScale;
    Class<?> options=Class.forName("local.luke.power.controls.util.ModOptions");
    var scaleField=options.getField("guiScale");var realScaleField=options.getField("realGuiScale");
    float scale=scaleField.getFloat(null),realScale=realScaleField.getFloat(null);
    mc.options.guiScale=2;scaleField.setFloat(null,2/8f);realScaleField.setFloat(null,2/8f);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
    try {
      Screen plate=screen;
      if(MinimapQuickMenu.active(mc)) {
        Screen host=screen;
        plate=new Screen() { @Override public void render(int x,int y,float delta) {
          host.render(x,y,delta);MinimapQuickMenu.renderMenu(mc,x,y);
        }};
        plate.init(mc,width,height);
      }
      SettingsSnapshot.render(mc,plate,width,height,REPORT.resolve(name+".png"));
    }
    finally {GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glPopAttrib();mc.options.guiScale=nativeScale;scaleField.setFloat(null,scale);realScaleField.setFloat(null,realScale);}
  }
}
