package local.luke.power.validation;

import java.nio.file.*;
import java.util.*;
import com.google.gson.JsonPrimitive;
import local.luke.power.config.*;
import local.luke.power.map.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

/** Regression checks run only inside the muted disposable release instance. */
final class Release330Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-330");
  private static Screen physical;
  private static double physicalZoom, physicalScale;
  private static Object physicalRow;
  private static Object get(Object o,String n)throws Exception{return Editor230Checks.get(o,n);}
  private static Object call(Object o,String n,Class<?>[] types,Object...args)throws Exception{return Editor230Checks.invoke(o,n,types,args);}
  private static Object call(Object o,String n)throws Exception{return call(o,n,new Class<?>[0]);}
  private static void set(Object o,String n,Object v)throws Exception{
    for(Class<?> c=o instanceof Class<?> type?type:o.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(n);f.setAccessible(true);f.set(o instanceof Class<?>?null:o,v);return;}catch(NoSuchFieldException ignored){}
    throw new NoSuchFieldException(n);
  }
  private static void check(boolean condition,String message){Validation.check(condition,"3.3.0: "+message);}
  private static Setting find(PowerOptionsScreen o,String id){return o.session().settings().stream().filter(s->s.id.equals(id)).findFirst().orElseThrow();}
  private static void click(Screen s,int x,int y){((ScreenInput)(Object)s).power$click(x,y,0);}
  private static void key(Screen s,int code){((ScreenInput)(Object)s).power$key('\0',code);}
  private static void open(Minecraft mc,Screen s,int w,int h){mc.setScreen(s);s.init(mc,w,h);}
  private static void query(PowerOptionsScreen s,String q)throws Exception{((TextInput)get(s,"search")).setText(q);call(s,"layout");}
  private static List<?> rows(PowerOptionsScreen s)throws Exception{return (List<?>)get(s,"rows");}
  private static List<String> ids(PowerOptionsScreen s)throws Exception{var ids=new ArrayList<String>();for(Object r:rows(s)){Setting v=(Setting)call(r,"setting");if(v!=null)ids.add(v.id);}return ids;}
  private static String group(PowerOptionsScreen s,String id)throws Exception{for(Object r:rows(s)){Setting v=(Setting)call(r,"setting");if(v!=null&&v.id.equals(id))return (String)call(r,"group");}throw new IllegalStateException("missing result "+id);}
  static void run(Minecraft mc,String phase)throws Exception{
    Files.createDirectories(REPORT);
    switch(phase){case "ui"->ui(mc);case "inventories"->inventories(mc);case "radar"->radar(mc);case "input-prepare","input-state","input-check","scale-check","inventory-prepare","inventory-check","hud-prepare","hud-check"->input(mc,phase);default->throw new IllegalArgumentException("release-330 ui|inventories");}
    Validation.log("RELEASE330 "+phase+" COMPLETE");
  }
  private static void ui(Minecraft mc)throws Exception{
    var before=MapConfig.copy();boolean auto=MenuPreferences.current().autoApply;
    MenuPreferences.update(s->s.autoApply=false);
    try{
      var settings=MapConfig.copy();settings.worldMap=settings.minimap=settings.radar=true;MapConfig.preview(settings);
      set(PowerOptionsScreen.class,"searchThisTab",false);
      var owner=new PowerOptionsScreen(null);open(mc,owner,960,540);set(owner,"page","Map");set(owner,"history",new ArrayDeque<>());query(owner,"");
      List<String> world=ids(owner).stream().filter(id->id.equals("maps.worldMap")||id.equals("maps.worldMapEditor")||id.equals("maps.pauseMap")||id.equals("maps.lightRadius")).toList();
      check(world.size()>=2&&world.get(0).equals("maps.worldMap")&&world.get(1).equals("maps.worldMapEditor"),"World map appearance not second row");
      query(owner,"minimap size");check(group(owner,"maps.size").equals("Map / Minimap appearance"),"wide minimap search destination label");
      check((int)call(owner,"sectionButtonX")-(int)call(owner,"searchScopeX")==24,"scope button spacing");
      check((int)call(owner,"searchScopeX")-(int)call(owner,"clearSearchX")==24,"clear button spacing");
      SettingsSnapshot.render(mc,owner,960,540,REPORT.resolve("search-wide-map.png"));
      call(owner,"openGroup",new Class<?>[]{Setting.class},find(owner,"maps.size"));
      check(mc.currentScreen instanceof WorldMapScreen,"minimap group did not open World Map");
      var map=(WorldMapScreen)mc.currentScreen;map.init(mc,960,540);
      var panel=(PowerOptionsScreen)get(map,"settingsPanel");check(panel.session()==owner.session(),"map group replaced Options session");
      check(get(panel,"mapView").equals("Minimap")&&get(panel,"highlightedId").equals(""),"minimap group highlight or destination");
      check(!(boolean)call(panel,"searchScopeVisible"),"scope button leaked into map sidebar");
      call(panel,"zoomMinimap",new Class<?>[]{int.class},120);
      double zoom=find(owner,"maps.zoom").value.getAsDouble();check(zoom>settings.zoom&&zoom<2.5,"sidebar wheel target not precise");
      check(MapConfig.current().zoom==zoom,"sidebar zoom not live");
      SettingsSnapshot.render(mc,map,960,540,REPORT.resolve("minimap-sidecar.png"));
      key(map,Keyboard.KEY_ESCAPE);check(mc.currentScreen==owner,"minimap search Escape lost source Options");
      check(find(owner,"maps.zoom").value.getAsDouble()==zoom,"source draft lost after sidebar return");
      call(owner,"activate",new Class<?>[]{Setting.class,int.class},find(owner,"maps.worldMapEditor"),1);
      check(mc.currentScreen instanceof WorldMapScreen,"World map appearance Edit failed");map=(WorldMapScreen)mc.currentScreen;map.init(mc,960,540);panel=(PowerOptionsScreen)get(map,"settingsPanel");
      check(get(panel,"mapView").equals("Map"),"World map appearance opened wrong sidebar view");
      SettingsSnapshot.render(mc,map,960,540,REPORT.resolve("world-map-sidecar.png"));
      call(panel,"back");check(mc.currentScreen==owner,"World map appearance Back lost source");
      set(owner,"page","General");query(owner,"radar");click(owner,(int)call(owner,"searchScopeX")+10,37);
      check(ids(owner).stream().noneMatch(id->id.startsWith("maps.")),"tab-scoped General search includes Map results");
      var reopened=new PowerOptionsScreen(null);open(mc,reopened,960,540);check((boolean)getStatic(PowerOptionsScreen.class,"searchThisTab"),"scope not remembered in session");
      set(reopened,"page","Map");query(reopened,"minimap size");check(ids(reopened).contains("maps.size"),"tab-scoped Map search lost appearance");
      click(reopened,(int)call(reopened,"searchScopeX")+10,37);check(!(boolean)getStatic(PowerOptionsScreen.class,"searchThisTab"),"scope did not cycle back");
      open(mc,owner,640,360);query(owner,"minimap size");check(group(owner,"maps.size").equals("General / Interface / HUD editor / Minimap"),"narrow minimap route label");
      query(owner,"debug axes");check(group(owner,"interface.debugAxes").startsWith("General / Interface / HUD editor / "),"HUD search breadcrumb");
      query(owner,"");set(owner,"page","Map");call(owner,"layout");check(!ids(owner).contains("maps.worldMapEditor"),"unavailable World map appearance row visible");
      SettingsSnapshot.render(mc,owner,640,360,REPORT.resolve("options-narrow.png"));
      var hud=new HudEditorScreen(owner,"maps.zoom");open(mc,hud,960,540);SettingsSnapshot.render(mc,hud,960,540,REPORT.resolve("hud-preview-wheel.png"));
      check((boolean)call(hud,"minimapRadarActive"),"HUD minimap preview not active");
      var draft=(SettingsDraft)get(hud,"draft");double old=draft.get("maps.zoom").value.getAsDouble();draft.checkpoint();draft.get("maps.zoom").value=new JsonPrimitive(MinimapZoom.scroll(old,-120));call(hud,"afterEdit");
      check(find(owner,"maps.zoom").value.getAsDouble()==zoom,"HUD wheel draft leaked before Done");draft.undo();call(hud,"afterEdit");check(draft.get("maps.zoom").value.getAsDouble()==old,"HUD zoom Undo failed");
      owner.session().discard();Validation.log("RELEASE330 UI PASS search routes/scopes, sidebar return/session, precise zoom draft/Undo, narrow fallback");
    }finally{mc.setScreen(null);MapConfig.preview(before);set(PowerOptionsScreen.class,"searchThisTab",false);MenuPreferences.update(s->s.autoApply=auto);}
  }
  private static Object getStatic(Class<?> c,String n)throws Exception{var f=c.getDeclaredField(n);f.setAccessible(true);return f.get(null);}
  private static void inventories(Minecraft mc)throws Exception{
    var before=MapConfig.copy();boolean debug=mc.options.debugHud,hidden=mc.options.hideHud;int oldScale=mc.options.guiScale;mc.options.guiScale=2;
    String mode=Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player).toString();
    try{
      mc.setScreen(null);CreativeVehicleChecks.mode(mc,"SURVIVAL");
      var settings=MapConfig.copy();settings.minimap=settings.showInInventory=true;settings.hideWithDebug=false;settings.size=90;settings.corner=1;settings.offsetX=settings.offsetY=6;MapConfig.preview(settings);mc.options.debugHud=mc.options.hideHud=false;
      Screen[] screens={new class_585(mc.player),new class_264(mc.player.inventory,new ChestInventory()),new class_516(mc.player.inventory,mc.world,(int)mc.player.x,(int)mc.player.y,(int)mc.player.z),new class_235(mc.player.inventory,new class_137()),new class_524(mc.player.inventory,new class_138())};
      Inventory330Capture.valid=true; Inventory330Capture.frames=0;
      for(Screen screen:screens){
        open(mc,screen,640,360);check(Minimap.inventoryVisible(mc),"inventory minimap missing for "+screen.getClass());
        var geometry=MinimapGeometry.layout(settings,640,360,false);double old=MapConfig.current().zoom;
        int mx=geometry.x()+geometry.size()/2,my=geometry.y()+geometry.size()/2;
        check(Minimap.inventoryWheel(mc,mx,my,120),"inventory wheel not consumed screen="+screen.getClass().getName()
            +" mode="+Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player)
            +" grabbed="+org.lwjgl.input.Mouse.isGrabbed()+" left="+org.lwjgl.input.Mouse.isButtonDown(0)
            +" right="+org.lwjgl.input.Mouse.isButtonDown(1)+" active="+org.lwjgl.opengl.Display.isActive()
            +" gui="+((local.luke.power.input.InventoryGui)screen).power$containsGuiSpace(mx,my)+" point="+mx+","+my);
        check(MapConfig.current().zoom>old,"inventory wheel did not zoom");
        check(!Minimap.inventoryWheel(mc,320,180,120),"inventory panel wheel stolen");
        SettingsSnapshot.render(mc,screen,640,360,REPORT.resolve("inventory-"+screen.getClass().getSimpleName()+".png"));
        check(Inventory330Capture.frames>0&&Inventory330Capture.valid,"minimap draw order not between dimming and panels for "+screen.getClass());
      }
      CreativeVehicleChecks.mode(mc,"CREATIVE");
      var creative=new class_585(mc.player);open(mc,creative,640,360);
      SettingsSnapshot.render(mc,creative,640,360,REPORT.resolve("inventory-creative.png"));
      check(Inventory330Capture.valid,"Creative minimap layer order");
      settings=MapConfig.copy();settings.showInInventory=false;MapConfig.preview(settings);check(!Minimap.inventoryVisible(mc),"inventory setting ignored");
      double old=settings.zoom;check(!Minimap.inventoryWheel(mc,580,50,120)&&MapConfig.current().zoom==old,"disabled inventory minimap consumed wheel");
      Validation.log("RELEASE330 INVENTORY PASS player/chest/crafting/dispenser/furnace: dim < minimap < panels; wheel respects GUI; disabled inactive");
    }finally{
      mc.setScreen(null);
      try{CreativeVehicleChecks.mode(mc,mode);}
      finally{MapConfig.preview(before);mc.options.debugHud=debug;mc.options.hideHud=hidden;mc.options.guiScale=oldScale;}
    }
  }


  private static void input(Minecraft mc,String phase)throws Exception {
    switch(phase) {
      case "input-prepare" -> {
        var s=MapConfig.copy();s.worldMap=s.minimap=true;s.zoom=1.25;MapConfig.preview(s);var map=new WorldMapScreen();mc.setScreen(map);call(map,"toggleSettings");
        physical=map;physicalZoom=MapConfig.current().zoom;physicalScale=((MapMath.View)get(map,"view")).scale;
      }
      case "input-state" -> {
        var result=new LinkedHashMap<String,Object>();result.put("width",mc.currentScreen.width);result.put("height",mc.currentScreen.height);
        if(mc.currentScreen instanceof WorldMapScreen map){var mini=get(map,"minimapLayout");result.put("miniX",call(mini,"x"));result.put("miniY",call(mini,"y"));result.put("miniSize",call(mini,"size"));var scale=call(map,"scaleBounds");result.put("scaleX",call(scale,"x"));result.put("scaleY",call(scale,"y"));result.put("scaleWidth",call(scale,"width"));result.put("scaleHeight",call(scale,"height"));}
        else if(mc.currentScreen instanceof HudEditorScreen hud){var preview=get(hud,"preview");var mini=get(preview,"minimapLayout");result.put("miniX",call(mini,"x"));result.put("miniY",call(mini,"y"));result.put("miniSize",call(mini,"size"));result.put("canvasX",call(hud,"canvasX"));result.put("canvasY",call(hud,"canvasY"));result.put("previewScale",call(hud,"scale"));}
        else {var mini=MinimapGeometry.layout(MapConfig.current(),mc.currentScreen.width,mc.currentScreen.height,false);result.put("miniX",mini.x());result.put("miniY",mini.y());result.put("miniSize",mini.size());}
        Files.writeString(REPORT.resolve("input.json"),Catalog.JSON.toJson(result));Validation.log("RELEASE330 INPUT "+result);
      }
      case "input-check" -> {check(mc.currentScreen==physical,"sidebar screen changed");check(MapConfig.current().zoom>physicalZoom,"physical sidebar wheel not zoomed");check(((MapMath.View)get(physical,"view")).scale==physicalScale,"sidebar minimap wheel zoomed world map too");Validation.log("RELEASE330 PHYSICAL SIDEBAR WHEEL PASS");}
      case "scale-check" -> {check((boolean)get(physical,"scaleDragging"),"scale drag ended before wheel check");check(((MapMath.View)get(physical,"view")).scale>physicalScale,"physical wheel did not zoom map during scale drag");Validation.log("RELEASE330 PHYSICAL SCALE DRAG WHEEL PASS");}
      case "inventory-prepare" -> {
        AccessRules.set(mc,local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE,true);CreativeVehicleChecks.mode(mc,"CREATIVE");var s=MapConfig.copy();s.minimap=s.showInInventory=true;s.corner=1;s.offsetX=s.offsetY=6;s.size=90;s.zoom=1.25;MapConfig.preview(s);
        physical=new class_585(mc.player);mc.setScreen(physical);physicalZoom=s.zoom;
        var originals=(List<?>)get(physical,"creative_items");var many=new ArrayList<Object>();
        while(many.size()<192)many.addAll(originals);
        set(physical,"creative_items",many);int max=((many.size()-64+7)/8)*8;
        set(physical,"creative_maxIndex",max);set(physical,"creative_normalGUI",false);set(physical,"creative_drag",false);
        set(physical,"creative_rowIndex",8);set(physical,"creative_slider",8f/max);physicalRow=get(physical,"creative_rowIndex");
      }
      case "inventory-check" -> {check(mc.currentScreen==physical&&MapConfig.current().zoom>physicalZoom,"physical inventory wheel did not zoom");check(get(physical,"creative_rowIndex").equals(physicalRow),"minimap wheel also scrolled Creative catalog: before="+physicalRow+" after="+get(physical,"creative_rowIndex")+" max="+get(physical,"creative_maxIndex"));Validation.log("RELEASE330 PHYSICAL CREATIVE WHEEL OWNERSHIP PASS");}
      case "hud-prepare" -> {var s=MapConfig.copy();s.zoom=1.25;MapConfig.preview(s);var owner=new PowerOptionsScreen(null);mc.setScreen(owner);physical=new HudEditorScreen(owner,"maps.zoom");mc.setScreen(physical);physicalZoom=((SettingsDraft)get(physical,"draft")).get("maps.zoom").value.getAsDouble();}
      case "hud-check" -> {check(((SettingsDraft)get(physical,"draft")).get("maps.zoom").value.getAsDouble()>physicalZoom,"physical HUD preview wheel did not zoom draft");Validation.log("RELEASE330 PHYSICAL HUD PREVIEW WHEEL PASS");}
    }
  }
  private static void radar(Minecraft mc)throws Exception {
    var before=MapConfig.copy();var visualBefore=local.luke.power.visual.VisualConfig.copy();
    var spawned=new ArrayList<net.minecraft.entity.Entity>();
    try {
      mc.setScreen(null);var settings=MapConfig.copy();settings.minimap=settings.worldMap=settings.radar=true;
      settings.items=settings.animals=settings.otherEntities=true;settings.radarRange=2;settings.radarLimit=64;MapConfig.preview(settings);
      var visibility=local.luke.power.visual.VisualConfig.copy();visibility.hideEntities=visibility.hideItems=visibility.hideTnt=true;
      visibility.hideMobs=visibility.hidePlayers=visibility.hideVehicles=visibility.hideProjectiles=visibility.hideOther=false;
      local.luke.power.visual.VisualConfig.preview(visibility);
      double x=mc.player.x,y=mc.player.boundingBox.minY,z=mc.player.z;
      for(int i=0;i<4096;i++){
        var item=new class_142(mc.world,x+1+(i%8)*.02,y+.2,z+1+(i/8%8)*.02,new net.minecraft.item.ItemStack(4,1,0));
        item.velocityX=item.velocityY=item.velocityZ=0;mc.world.method_210(item);spawned.add(item);
      }
      var pig=class_206.method_732("Pig",mc.world);pig.method_1340(x+4,y,z+4);mc.world.method_210(pig);spawned.add(pig);
      var far=class_206.method_732("Pig",mc.world);far.method_1340(x+48,y,z);mc.world.method_210(far);spawned.add(far);
      var tracker=Class.forName("local.luke.power.map.Maps").getDeclaredField("RADAR");tracker.setAccessible(true);Object tracking=tracker.get(null);
      var tick=tracking.getClass().getDeclaredMethod("tick",Minecraft.class,MapSettings.class,boolean.class,boolean.class,int.class,double.class,double.class,double.class);tick.setAccessible(true);
      var times=new ArrayList<Double>();
      for(int i=0;i<40;i++){
        long start=System.nanoTime();tick.invoke(tracking,mc,settings,true,false,i,x,y,z);times.add((System.nanoTime()-start)/1e6);
        if(i%4==0){var item=new class_142(mc.world,x+1,y+.2,z+1,new net.minecraft.item.ItemStack(4,1,0));mc.world.method_210(item);spawned.add(item);}
      }
      check(Maps.radar(true).stream().noneMatch(Maps.Radar::item),"hidden items retained by minimap radar");
      check(Maps.radar(true).stream().anyMatch(e->e.nativeType().equals("Pig")),"hidden item storm starved visible mob discovery");
      check(Maps.radar(true).stream().noneMatch(e->e.x()>x+32),"horizontal range ignored");
      pig.prevX=pig.x;pig.x+=.5;tick.invoke(tracking,mc,settings,true,false,41,x,y,z);
      check(Maps.radar(true).stream().anyMatch(e->e.nativeType().equals("Pig")&&e.x()==pig.x),"tracked marker did not refresh immediately");
      var map=new WorldMapScreen();open(mc,map,960,540);
      for(int i=0;i<40;i++)Maps.tick(mc);
      check(Maps.radar(true).isEmpty(),"plain World Map computed an unused minimap radar");
      call(map,"toggleSettings");
      for(int i=0;i<40;i++)Maps.tick(mc);
      check(Maps.radar(false).stream().anyMatch(Maps.Radar::item),"open World Map obeyed gameplay hide toggle");
      check(Maps.radar(true).stream().noneMatch(Maps.Radar::item),"World Map preview leaked hidden entities to minimap");
      check(Maps.radar(true).stream().anyMatch(e->e.nativeType().equals("Pig")),"cold sidebar minimap starved by World Map item storm");
      mc.setScreen(null);Maps.tick(mc);check(Maps.radar(false).isEmpty(),"closed World Map retained radar work");
      settings=MapConfig.copy();settings.minimap=false;MapConfig.preview(settings);Maps.tick(mc);
      check(Maps.radar(true).isEmpty()&&Maps.radar(false).isEmpty(),"enabled closed World Map keeps radar active");
      double avg=times.stream().mapToDouble(Double::doubleValue).average().orElseThrow(),max=times.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
      Validation.log("RELEASE330 RADAR PASS 4096 hidden items plus count churn; visible mob discovery and per-tick movement; independent view filters; range; closed views idle; avgMs="+avg+" maxMs="+max);
    }finally{for(var entity:spawned)entity.dead=true;mc.setScreen(null);local.luke.power.visual.VisualConfig.preview(visualBefore);MapConfig.preview(before);}
  }
  private static final class ChestInventory implements net.minecraft.inventory.Inventory {
    private final net.minecraft.item.ItemStack[] stacks = new net.minecraft.item.ItemStack[54];
    public int size(){return stacks.length;}
    public net.minecraft.item.ItemStack getStack(int slot){return stacks[slot];}
    public net.minecraft.item.ItemStack removeStack(int slot,int count){var stack=stacks[slot];if(stack==null)return null;if(count>=stack.count){stacks[slot]=null;return stack;}return stack.split(count);}
    public void setStack(int slot,net.minecraft.item.ItemStack stack){stacks[slot]=stack;}
    public String getName(){return "Chest";}
    public int getMaxCountPerStack(){return 64;}
    public void markDirty(){}
    public boolean canPlayerUse(net.minecraft.entity.player.PlayerEntity player){return true;}
  }

}
