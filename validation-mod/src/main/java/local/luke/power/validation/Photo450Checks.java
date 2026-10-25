package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.JsonObject;
import local.luke.power.input.*;
import local.luke.power.map.*;
import local.luke.power.render.RenderControls;
import local.luke.power.mixin.CullingChunkCacheAccessor;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import local.luke.power.ui.*;
import local.luke.power.world.WorldCycles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Disposable native checks, plus stable controls for physical input and owner captures. */
final class Photo450Checks {
  private static Screen opened;
  private static Integer savedBrightKey;
  private static Map<String,Integer> savedModifiers;
  private static int[] stagedPointer;
  private static int[] surfacePatch;
  private Photo450Checks() {}
  static void run(Minecraft mc,String action) throws Exception {
    check(mc.world!=null&&mc.player!=null&&!mc.world.isRemote&&Display.isActive(),"focus a disposable singleplayer world");
    check(mc.options.musicVolume==0&&mc.options.soundVolume==0,"mute the disposable client");
    PhotoModeTools.tick(mc);
    if(action.equals("open")){opened=create(mc.currentScreen);mc.setScreen(opened);return;}
    if(action.equals("close")){checkPhoto(mc);mc.setScreen((Screen)field(mc.currentScreen,"previousScreen"));restoreBinding(mc);return;}
    if(action.equals("surface")){checkPhoto(mc);surface(mc,mc.currentScreen);return;}
    if(action.equals("stage")){checkPhoto(mc);stagedPointer=stage(mc,mc.currentScreen,true);state(mc);return;}
    if(action.equals("stage-modal")){checkPhoto(mc);check(stagedPointer!=null,"run stage first");
      PhotoModeHooks.tools().mouse(mc.currentScreen,stagedPointer[0],stagedPointer[1],2,true,0);
      Method actionMethod=PhotoModeTools.class.getDeclaredMethod("action",int.class);actionMethod.setAccessible(true);actionMethod.invoke(tools(),0);return;}
    if(action.equals("state")){state(mc);return;}
    if(action.equals("capture")){checkPhoto(mc);((PhotoModeHooks.Camera)mc.currentScreen).powerPhoto$screenshot();return;}
    if(action.equals("toggles")){checkPhoto(mc);field(tools(),"toggles",true);return;}
    if(action.startsWith("bind ")){
      String[] values=action.split(" ");check(values.length==4&&values[1].equals("fullbright"),"bind fullbright <keycode> <modifier-mask>");
      if(savedBrightKey==null){savedBrightKey=RenderControls.FULL_BRIGHT.code;savedModifiers=Bindings.modifiers();}
      RenderControls.FULL_BRIGHT.code=Integer.parseInt(values[2]);var mods=new HashMap<>(Bindings.modifiers());
      mods.put(RenderControls.FULL_BRIGHT.translationKey,Integer.parseInt(values[3]));Bindings.configure(mods);Bindings.register(mc.options.allKeys);return;
    }
    if(action.startsWith("toggle ")){checkPhoto(mc);toggle(MapToggle.valueOf(action.substring(7).toUpperCase(Locale.ROOT)));return;}
    if(action.startsWith("middle ")){checkPhoto(mc);String[] v=action.split(" ");
      PhotoModeHooks.tools().mouse(mc.currentScreen,Integer.parseInt(v[1]),Integer.parseInt(v[2]),2,true,0);return;}
    if(!action.equals("check"))throw new IllegalArgumentException("photo-450 open|check|state|capture|close|toggles|toggle ITEMS/MOBS/WAYPOINTS/CHUNKS/LIGHT|middle x y|bind fullbright code mask");
    failures=0;
    var previous=mc.currentScreen;var before=MapConfig.copy();boolean grabbed=Mouse.isGrabbed();
    var cycles=(WorldCycles)mc.world.method_262();long daylight=cycles.power$daylightTime();
    int brightCode=RenderControls.FULL_BRIGHT.code;Map<String,Integer> modifiers=Bindings.modifiers();boolean bright=RenderControls.fullBright();
    Screen photo=create(previous);Object tools=tools();
    try {
      mc.setScreen(photo);Mouse.setGrabbed(false);
      test("PHOTO450 visit defaults and independent photo waypoint preference",()->{
        check((boolean)field(tools,"items")&&(boolean)field(tools,"mobs"),"entity defaults are off");
        check(!(boolean)field(tools,"chunks")&&!(boolean)field(tools,"spawnable"),"expensive photo overlays started enabled");
        boolean world=MapConfig.current().worldWaypoints,map=MapConfig.current().waypoints;
        toggle(MapToggle.WAYPOINTS);check(MapConfig.current().photoWaypoints!=before.photoWaypoints,"photo preference did not change");
        check(MapConfig.current().worldWaypoints==world&&MapConfig.current().waypoints==map,"photo toggle changed another view");
        toggle(MapToggle.WAYPOINTS);
      });
      test("PHOTO450 photo tools keep their native host and fit small layouts",()->{
        for(int[] size:new int[][]{{320,240},{359,240},{360,240},{427,240},{640,360}}){
          photo.init(mc,size[0],size[1]);field(tools,"toggles",true);
          var layout=(MapTogglePanel.Layout)call(tools,"panel");
          check(layout.toggles().size()==5,"wrong photo toggle list");
          check(layout.bounds().x()>=0&&layout.bounds().y()>=0&&layout.bounds().x()+layout.bounds().width()<=size[0],"photo panel clipped");
          for(int i=layout.first();i<layout.first()+layout.visible();i++){
            var row=layout.row(i);String name=layout.toggle(i).name();
            boolean consumed=PhotoModeHooks.tools().mouse(photo,row.x()+21,row.y()+10,0,true,0);
            check(consumed&&mc.currentScreen==photo,"checkbox leaked to native screen "+name);
            check((int)field(photo,"dragButton")==-1,"checkbox started camera drag");
            PhotoModeHooks.tools().mouse(photo,row.x()+21,row.y()+10,0,false,0);
            PhotoModeHooks.tools().mouse(photo,row.x()+21,row.y()+10,0,true,0);
            PhotoModeHooks.tools().mouse(photo,row.x()+21,row.y()+10,0,false,0);
          }
        }
        field(tools,"toggles",false);
      });
      test("PHOTO450 full bright dispatch and camera-key priority",()->{
        RenderControls.FULL_BRIGHT.code=Keyboard.KEY_F6;
        var plain=new HashMap<>(modifiers);plain.remove(RenderControls.FULL_BRIGHT.translationKey);Bindings.configure(plain);Bindings.register(mc.options.allKeys);
        PhotoModeHooks.tools().key(photo,'\0',Keyboard.KEY_F6);check(RenderControls.fullBright()!=bright,"full bright did not work in photo");
        PhotoModeHooks.tools().key(photo,'\0',Keyboard.KEY_F6);check(RenderControls.fullBright()==bright,"full bright did not restore");
        RenderControls.FULL_BRIGHT.code=Keyboard.KEY_Q;Bindings.register(mc.options.allKeys);
        check(!PhotoModeHooks.tools().key(photo,'q',Keyboard.KEY_Q),"camera rotation key was intercepted");
        check(RenderControls.fullBright()==bright,"camera priority leaked fullbright");
      });
      test("PHOTO450 photo actions remain available with both maps disabled",()->{
        var style=MapConfig.copy();style.minimap=style.worldMap=false;MapConfig.preview(style);
        try {
          for(var binding:List.of(MapKeys.MAP_WAYPOINTS,MapKeys.WORLD_WAYPOINTS,MapKeys.RADAR,
              MapKeys.GRID,MapKeys.CLEAR_DESTINATION))
            check(MapKeys.available(binding.translationKey),"photo binding unavailable: "+binding.translationKey);
        } finally {MapConfig.preview(before);}
      });
      test("PHOTO450 native drags keep ownership when crossing photo tools",()->{
        field(tools,"toggles",true);var layout=(MapTogglePanel.Layout)call(tools,"panel");
        int x=layout.bounds().x()+24,y=layout.bounds().y()+10;
        field(photo,"dragButton",0);
        check(!PhotoModeHooks.tools().mouse(photo,x,y,-1,false,0),"tool panel stole camera drag");
        check((int)field(photo,"dragButton")==0,"tool panel cleared camera ownership");
        field(photo,"dragButton",-1);
        Object slider=field(photo,"timeSlider");sliderField(slider,"dragging",true);
        check(!PhotoModeHooks.tools().mouse(photo,x,y,-1,false,0),"tool panel stole slider drag");
        check((boolean)sliderField(slider,"dragging"),"tool panel cleared slider ownership");sliderField(slider,"dragging",false);field(tools,"toggles",false);
      });
      test("PHOTO450 local entity filters are render-only and do not hide the player",()->{
        var item=new net.minecraft.class_142(mc.world,mc.player.x,mc.player.y,mc.player.z,new net.minecraft.item.ItemStack(1,1,0));
        var mob=net.minecraft.class_206.method_732("Pig",mc.world);check(mob!=null,"native Pig missing");
        field(tools,"items",false);field(tools,"mobs",false);
        check(PhotoModeTools.hidden(item)&&PhotoModeTools.hidden(mob),"local entity filters did not hide items/mobs");
        check(!PhotoModeTools.hidden(mc.player),"mob filter hid the player");
        field(tools,"items",true);field(tools,"mobs",true);
        check(!PhotoModeTools.hidden(item)&&!PhotoModeTools.hidden(mob),"local filters did not restore rendering");
        check(!item.dead&&!mob.dead,"render visibility changed entity lifetime");
      });
      test("PHOTO450 screenshot key requests one capture while context owns all feature keys",()->{
        int code=0;for(var binding:mc.options.allKeys)if(binding.translationKey.equals("key.power_controls.take_screenshot"))code=binding.code;
        check(code!=0,"native screenshot binding missing");
        PhotoModeHooks.tools().key(photo,'\0',code);check((boolean)field(photo,"shouldScreenshot"),"photo screenshot key ignored");
        field(photo,"shouldScreenshot",false);field(tools,"context",true);
        PhotoModeHooks.tools().key(photo,'\0',code);check(!(boolean)field(photo,"shouldScreenshot"),"context leaked screenshot key");
        PhotoModeHooks.tools().key(photo,'\0',Keyboard.KEY_ESCAPE);check(!(boolean)field(tools,"context"),"Escape did not close context");
      });
      test("PHOTO450 modal waypoint draft survives resize and owns input",()->{
        Method edit=PhotoModeTools.class.getDeclaredMethod("edit",Waypoint.class);edit.setAccessible(true);edit.invoke(tools,MapKeys.newWaypoint());
        Object draft=field(tools,"layer");check(draft instanceof WaypointEditorScreen&&mc.currentScreen==photo,"waypoint replaced photo screen");
        float pan=(float)field(photo,"cameraX");
        check(PhotoModeHooks.tools().key(photo,'q',Keyboard.KEY_Q),"modal did not own camera key");
        check(PhotoModeHooks.tools().blocked(photo),"modal did not block held movement");
        check(PhotoModeHooks.tools().controlsHidden(photo),"photo controls remain behind the editor");
        photo.init(mc,320,240);check(field(tools,"layer")==draft,"resize replaced editor draft");
        check((float)field(photo,"cameraX")==pan,"modal resize moved camera");
        PhotoModeHooks.tools().key(photo,'\0',Keyboard.KEY_ESCAPE);
        check(field(tools,"layer")==null&&mc.currentScreen==photo,"cancel did not return to photo host");
        check(!PhotoModeHooks.tools().controlsHidden(photo),"closing the editor did not restore photo controls");
      });
      test("PHOTO450 Create waypoint always owns a fresh ID and preserves its source",()->{
        var source=MapKeys.newWaypoint();source.id="temporary-destination";source.name="Existing location";
        String beforeSource=local.luke.power.config.Catalog.JSON.toJson(source),lastId="";
        Method createAction=PhotoModeTools.class.getDeclaredMethod("action",int.class);createAction.setAccessible(true);
        for(int attempt=0;attempt<2;attempt++){
          field(tools,"target",source);createAction.invoke(tools,0);
          var draft=(Waypoint)field(field(tools,"layer"),"initial");
          check(!draft.id.equals(source.id)&&!draft.id.equals(lastId),"Create reused the source or previous creation ID");
          check(draft.x==source.x&&draft.y==source.y&&draft.z==source.z&&draft.dimension==source.dimension,"Create changed the selected position");
          lastId=draft.id;
          check(beforeSource.equals(local.luke.power.config.Catalog.JSON.toJson(source)),"Create mutated the source waypoint");
          PhotoModeHooks.tools().key(photo,'\0',Keyboard.KEY_ESCAPE);
          check(field(tools,"layer")==null,"fresh waypoint draft did not cancel cleanly");
        }
      });
      test("PHOTO450 disabled surface survey performs zero checks",()->{
        var loaded=loaded(mc);
        field(tools,"chunks",false);field(tools,"spawnable",false);
        PhotoModeTools.tick(mc);var survey=(PhotoTerrainSurvey)field(tools,"survey");
        check(survey.checksLastTick==0,"disabled survey read terrain");
        field(tools,"spawnable",true);PhotoModeTools.tick(mc);
        check(survey.checksLastTick<=256,"surface survey exceeded tick budget");
        field(tools,"spawnable",false);PhotoModeTools.tick(mc);
        check(survey.checksLastTick==0,"disabling overlay did not stop work immediately");
        field(tools,"chunks",true);for(int tick=0;tick<12;tick++)PhotoModeTools.tick(mc);field(tools,"chunks",false);PhotoModeTools.tick(mc);
        check(loaded.equals(loaded(mc)),"surface/chunk surveys changed the loaded chunk set");
      });
      test("PHOTO450 unloaded and cave rays are bounded",()->{
        int[] reads={0};check(PhotoRay.trace(.5,20.5,.5,64.5,20.5,.5,8192,(x,y,z)->{reads[0]++;return x==16?-1:0;})==null,"ray crossed unloaded chunk");
        check(reads[0]==17,"ray read past unloaded boundary");
        check(PhotoRay.trace(.5,20.5,.5,64.5,20.5,.5,8192,(x,y,z)->x==12?1:0).equals(new PhotoRay.Hit(12,20,0)),"visible cave hit lost");
      });
      test("PHOTO450 actual native photo matrices pick the loaded surface without loading chunks",()->{
        Map<String,Object> camera=new HashMap<>();
        for(String name:List.of("rotation","rotationGoal","tilt","tiltGoal","zoom","zoomGoal","cameraX","cameraY"))camera.put(name,field(photo,name));
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
        try {
          for(float rotation:new float[]{-.5F,0F,.5F}){
            field(photo,"rotation",rotation);field(photo,"rotationGoal",rotation);
            int[] pick=stage(mc,photo,false);var loaded=loaded(mc);
            var hit=PhotoPicking.pick(mc,pick[0],pick[1],8192);
            check(hit!=null&&hit.x()==pick[2]&&hit.z()==pick[4]&&hit.y()==pick[3],"native ray missed projected loaded surface at rotation "+rotation+": "+hit);
            check(PhotoPicking.pick(mc,-photo.width*32,-photo.height*32,8192)==null,"offscreen ray escaped renderer bounds");
            check(loaded.equals(loaded(mc)),"photo native ray loaded or unloaded terrain");
          }
        }finally{
          for(var entry:camera.entrySet())field(photo,entry.getKey(),entry.getValue());
          GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopAttrib();
        }
      });
      test("PHOTO450 context does not teleport to an unknown or obstructed destination",()->{
        Waypoint point=MapKeys.newWaypoint();point.x=29_999_000;point.z=29_999_000;
        field(tools,"target",point);check(!(boolean)call(tools,"safeTeleport"),"unloaded teleport accepted");
        point.x=(int)Math.floor(mc.player.x);point.z=(int)Math.floor(mc.player.z);point.y=0;
        check(!(boolean)call(tools,"safeTeleport"),"invalid-height teleport accepted");
        check(mc.currentScreen==photo,"teleport probe changed screen");
      });
      test("PHOTO450 lighting is restored synchronously when photo closes",()->{
        long original=(long)field(photo,"originalTOD");
        Object slider=field(photo,"timeSlider");
        sliderField(slider,"value",.75F);sliderField(slider,"dragging",true);call(photo,"updateSliders");sliderField(slider,"dragging",false);
        check(cycles.power$daylightTime()!=original,"fixture did not change daylight preview");
        photo.init(mc,320,240);check((long)field(photo,"originalTOD")==original,"resize replaced entry daylight");
        mc.setScreen(previous);
        check(cycles.power$daylightTime()==original,"preview daylight leaked into pause screen");
        Field darkness=net.minecraft.world.World.class.getDeclaredField("field_202");darkness.setAccessible(true);
        check(darkness.getInt(mc.world)==mc.world.method_151(1F),"cached daylight darkness remained stale after exit");
        check(!PhotoModeTools.active(),"photo tools remained active after exit");
        check(Class.forName("local.luke.power.controls.util.ModOptions").getField("photoModeFogMultiplier").getFloat(null)==1,"photo fog leaked");
      });
    } finally {
      mc.setScreen(previous);MapConfig.preview(before);MapConfig.queued(before);cycles.power$daylightTime(daylight);mc.world.method_237();
      if(RenderControls.fullBright()!=bright){
        RenderControls.FULL_BRIGHT.code=Keyboard.KEY_F6;Bindings.configure(Map.of());Bindings.register(mc.options.allKeys);
        RenderControls.photoPress(mc,Keyboard.KEY_F6);
      }
      RenderControls.FULL_BRIGHT.code=brightCode;Bindings.configure(modifiers);Bindings.register(mc.options.allKeys);
      Mouse.setGrabbed(grabbed);
    }
    log("PHOTO450 FAILURES "+failures);check(failures==0,"photo-450 failures="+failures);
  }
  private static void restoreBinding(Minecraft mc){if(savedBrightKey!=null){RenderControls.FULL_BRIGHT.code=savedBrightKey;Bindings.configure(savedModifiers);Bindings.register(mc.options.allKeys);savedBrightKey=null;savedModifiers=null;}}
  private static Screen create(Screen previous)throws Exception{return (Screen)Class.forName("local.luke.power.controls.tweaks.photomode.PhotoModeScreen").getConstructor(Screen.class).newInstance(previous);}
  private static Object tools(){return PhotoModeHooks.tools();}
  private static void checkPhoto(Minecraft mc){check(mc.currentScreen instanceof PhotoProjection.CameraOwner,"photo mode must be open");}
  private static void toggle(MapToggle toggle)throws Exception{Method method=PhotoModeTools.class.getDeclaredMethod("toggle",MapToggle.class);method.setAccessible(true);method.invoke(tools(),toggle);}
  private static void state(Minecraft mc)throws Exception{
    JsonObject value=new JsonObject();value.addProperty("active",PhotoModeTools.active());
    value.addProperty("fullBright",RenderControls.fullBright());value.addProperty("photoWaypoints",MapConfig.current().photoWaypoints);
    value.addProperty("daylight",((WorldCycles)mc.world.method_262()).power$daylightTime());
    value.addProperty("screen",mc.currentScreen==null?"none":mc.currentScreen.getClass().getName());
    for(String name:List.of("items","mobs","chunks","spawnable","toggles","context","contextSafe"))value.addProperty(name,(boolean)field(tools(),name));
    Object layer=field(tools(),"layer");value.addProperty("layer",layer==null?"none":layer.getClass().getName());
    value.addProperty("surveyChecks",((PhotoTerrainSurvey)field(tools(),"survey")).checksLastTick);
    if(mc.currentScreen instanceof PhotoProjection.CameraOwner){value.addProperty("width",mc.currentScreen.width);value.addProperty("height",mc.currentScreen.height);
      for(String name:List.of("cameraX","cameraY","zoomGoal","rotationGoal","tiltGoal"))value.addProperty(name,(Number)field(mc.currentScreen,name));}
    if(stagedPointer!=null){value.addProperty("pickGuiX",stagedPointer[0]);value.addProperty("pickGuiY",stagedPointer[1]);
      Waypoint hovered=PhotoModeTools.active()?WaypointMarkers.hovered(mc,stagedPointer[0],stagedPointer[1]):null;
      value.addProperty("hoveredWaypoint",hovered==null?"none":hovered.id);
      value.addProperty("pickBlockX",stagedPointer[2]);value.addProperty("pickBlockY",stagedPointer[3]);value.addProperty("pickBlockZ",stagedPointer[4]);}
    if(surfacePatch!=null){value.addProperty("surfaceX",surfacePatch[0]);value.addProperty("surfaceY",surfacePatch[1]);value.addProperty("surfaceZ",surfacePatch[2]);}
    Path report=Path.of("power-beta-data/reports/photo-450-state.json");Files.createDirectories(report.getParent());Files.writeString(report,value.toString());log("PHOTO450 state "+value);
  }
  /** Deliberately edits only the disposable scene: a dark grass platform with one harmless decoration. */
  private static void surface(Minecraft mc,Screen photo)throws Exception{
    check(!PhotoModeHooks.tools().controlsHidden(photo),"close the waypoint modal before staging terrain");
    var bounds=(RenderedTerrainBounds)mc.worldRenderer;
    int x=(int)Math.floor((bounds.powerTerrain$minX()+bounds.powerTerrain$maxX())*.5),
        z=(int)Math.floor((bounds.powerTerrain$minZ()+bounds.powerTerrain$maxZ())*.5),y=96;
    var loaded=loaded(mc);
    // Native mutation/light callbacks may inspect neighbors. Admit the complete surrounding chunks first.
    for(int dx=-18;dx<=18;dx+=6)for(int dz=-18;dz<=18;dz+=6)
      check(MapSampler.loaded(mc.world,(x+dx)>>4,(z+dz)>>4)!=null,"surface fixture requires loaded neighboring chunks");
    for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
      int bx=x+dx,bz=z+dz;
      for(int by=127;by>y;by--)if(mc.world.getBlockId(bx,by,bz)!=0)mc.world.method_154(bx,by,bz,0,0);
      mc.world.method_154(bx,y-2,bz,1,0);mc.world.method_154(bx,y-1,bz,3,0);mc.world.method_154(bx,y,bz,2,0);
    }
    mc.world.method_154(x,y+1,z,37,0);
    check(mc.world.getBlockId(x,y+1,z)==37,"surface fixture flower was rejected");
    check(mc.world.method_164(net.minecraft.class_56.BLOCK,x,y+1,z)<8
        &&mc.world.method_164(net.minecraft.class_56.BLOCK,x+1,y+1,z)<8,"surface fixture has emitted light");
    int snowX=x-Math.floorMod(x,16);
    if(x-snowX>8)snowX+=16;
    int[] snowMetadata={0,2,3,4,7};
    for(int i=0;i<snowMetadata.length;i++){
      int bz=z+2+i;
      for(int by=127;by>y;by--)if(mc.world.getBlockId(snowX,by,bz)!=0)mc.world.method_154(snowX,by,bz,0,0);
      mc.world.method_154(snowX,y-2,bz,1,0);mc.world.method_154(snowX,y-1,bz,3,0);mc.world.method_154(snowX,y,bz,2,0);
      mc.world.method_154(snowX,y+1,bz,78,snowMetadata[i]);
      check(mc.world.method_1778(snowX,y+1,bz)==snowMetadata[i],"snow fixture metadata changed");
      boolean colliding=net.minecraft.block.Block.BLOCKS[78].method_1624(mc.world,snowX,y+1,bz)!=null;
      check(colliding==(snowMetadata[i]>=3),"native snow collision cutoff changed at "+snowMetadata[i]);
      check(mc.world.method_164(net.minecraft.class_56.BLOCK,snowX,y+1,bz)<8,"snow fixture has emitted light");
    }
    var style=MapConfig.copy();style.lightRadius=12;MapConfig.preview(style);
    PhotoTerrainSurvey survey=new PhotoTerrainSurvey();
    boolean plain=false,flower=false;int cells=0,maxChecks=0,ticks=0;long start=System.nanoTime(),maxTickNanos=0;
    for(;ticks<128&&!(plain&&flower&&(boolean)field(survey,"completed"));ticks++){
      long tickStart=System.nanoTime();survey.tick(mc,false,true);maxTickNanos=Math.max(maxTickNanos,System.nanoTime()-tickStart);maxChecks=Math.max(maxChecks,survey.checksLastTick);
      List<?> snapshot=(List<?>)field(survey,"cells");cells=snapshot.size();
      for(Object cell:snapshot){
        if(!(boolean)recordValue(cell,"spawn"))continue;
        int bx=((Number)recordValue(cell,"x")).intValue(),bz=((Number)recordValue(cell,"z")).intValue();
        if(bx==x&&bz==z){flower=true;check(((Number)recordValue(cell,"y")).doubleValue()==y+1,"flower changed surface overlay elevation");}
        if(bx==x+1&&bz==z)plain=true;
      }
    }
    long surveyNanos=System.nanoTime()-start;
    check(plain&&flower,"surface survey missed dark grass or grass beneath non-colliding flower");
    check((boolean)field(survey,"completed"),"positive surface survey did not finish its bounded fixture footprint");
    for(int i=0;i<snowMetadata.length;i++){
      Object cell=surfaceCell(survey,snowX,z+2+i);
      if(snowMetadata[i]<3){
        check(cell!=null&&(boolean)recordValue(cell,"spawn"),"ordinary snow excluded spawn support at metadata "+snowMetadata[i]);
        check(((Number)recordValue(cell,"y")).doubleValue()==y+1+(1+snowMetadata[i])/8d,"ordinary snow overlay used wrong visual height");
      }else check(cell==null||!(boolean)recordValue(cell,"spawn"),"colliding snow accepted as spawnable at metadata "+snowMetadata[i]);
    }
    PhotoTerrainSurvey borders=new PhotoTerrainSurvey();boolean allBorders=false;int borderTicks=0;Object lastBorderSnapshot=null;
    for(;borderTicks<2048&&!allBorders;borderTicks++){
      borders.tick(mc,true,true);check(borders.checksLastTick<=256,"snow borders exceeded tick work limit");
      Object borderSnapshot=field(borders,"cells");if(borderSnapshot==lastBorderSnapshot)continue;lastBorderSnapshot=borderSnapshot;allBorders=true;
      for(int i=0;i<snowMetadata.length;i++){
        Object cell=surfaceCell(borders,snowX,z+2+i);
        if(cell==null){allBorders=false;continue;}
        check((boolean)recordValue(cell,"border"),"snow fixture was not a native chunk boundary");
        check(((Number)recordValue(cell,"y")).doubleValue()==y+1+(1+snowMetadata[i])/8d,"snow border used collision height instead of visual height at metadata "+snowMetadata[i]);
        check((boolean)recordValue(cell,"spawn")== (snowMetadata[i]<3),"snow border/spawn state disagreed at metadata "+snowMetadata[i]);
      }
    }
    check(allBorders,"bounded snow-border survey did not reach fixture columns");
    var originalRules=AccessRules.copy(mc);Object originalTarget=field(tools(),"target");
    try{
      var allowed=originalRules.copy();allowed.set(local.luke.power.rules.GameRule.ALLOW_TELEPORTATION,true);AccessRules.restore(mc,allowed);
      assertSafeAt(mc,x+1,y+1,z,true,"plain grass");assertSafeAt(mc,x,y+1,z,true,"harmless flower");
      for(int i=0;i<snowMetadata.length;i++)assertSafeAt(mc,snowX,y+1,z+2+i,snowMetadata[i]<3,"snow metadata "+snowMetadata[i]);
      try{
        mc.world.method_154(x+1,y+2,z,1,0);assertSafeAt(mc,x+1,y+1,z,false,"solid head obstruction");mc.world.method_154(x+1,y+2,z,0,0);
        mc.world.method_154(x+1,y+1,z,9,0);assertSafeAt(mc,x+1,y+1,z,false,"water at feet");
        mc.world.method_154(x+1,y+1,z,11,0);assertSafeAt(mc,x+1,y+1,z,false,"lava at feet");
      }finally{mc.world.method_154(x+1,y+1,z,0,0);mc.world.method_154(x+1,y+2,z,0,0);}
    }finally{AccessRules.restore(mc,originalRules);field(tools(),"target",originalTarget);}

    check(maxChecks<=256,"positive surface survey exceeded per-tick column limit");
    check(loaded.equals(loaded(mc)),"surface fixture/survey loaded terrain");
    surfacePatch=new int[]{x,y,z};
    field(tools(),"context",false);field(tools(),"chunks",false);field(tools(),"spawnable",true);field(tools(),"toggles",true);
    ((PhotoTerrainSurvey)field(tools(),"survey")).clear();
    stagedPointer=stage(mc,photo,false);
    for(int tick=0;tick<16;tick++)PhotoModeTools.tick(mc);
    log("PASS PHOTO450 dark grass/flower, snow metadata0/2 spawn and3/4/7 collision exclusion, exact snow border heights; cells="+cells+" ticks="+ticks+" borderTicks="+borderTicks
        +" maxChecks="+maxChecks+" maxTickMs="+(maxTickNanos/1_000_000.0)+" totalSurveyMs="+(surveyNanos/1_000_000.0));
    state(mc);
  }
  private static void assertSafeAt(Minecraft mc,int x,int y,int z,boolean expected,String label)throws Exception{
    Waypoint target=MapKeys.newWaypoint();target.x=x;target.y=y;target.z=z;field(tools(),"target",target);
    check((boolean)call(tools(),"safeTeleport")==expected,"safeTeleport geometry disagreed for "+label);
  }
  private static Object surfaceCell(PhotoTerrainSurvey survey,int x,int z)throws Exception{
    List<?> cells=(List<?>)field(survey,"cells");if(cells.isEmpty())return null;
    Method getX=cells.get(0).getClass().getDeclaredMethod("x"),getZ=cells.get(0).getClass().getDeclaredMethod("z");getX.setAccessible(true);getZ.setAccessible(true);
    for(Object cell:cells)if(((Number)getX.invoke(cell)).intValue()==x&&((Number)getZ.invoke(cell)).intValue()==z)return cell;
    return null;
  }
  private static Object recordValue(Object record,String name)throws Exception{
    Method method=record.getClass().getDeclaredMethod(name);method.setAccessible(true);return method.invoke(record);
  }
  private static Object sliderField(Object slider,String name)throws Exception{
    Field field=sliderField(slider.getClass(),name);return field.get(slider);
  }
  private static void sliderField(Object slider,String name,Object value)throws Exception{sliderField(slider.getClass(),name).set(slider,value);}
  private static Field sliderField(Class<?> type,String name)throws Exception{
    // Controls are compiled with Biny; the validation module uses Barn. Find retained runtime fields by superclass.
    String[] names=name.equals("value")?new String[]{"value","field_2591"}:new String[]{"dragging","field_2592"};
    for(Class<?> current=type;current!=null;current=current.getSuperclass())for(String candidate:names)
      try{Field field=current.getDeclaredField(candidate);field.setAccessible(true);return field;}catch(NoSuchFieldException ignored){}
    throw new NoSuchFieldException(name);
  }
  private static Set<Integer> loaded(Minecraft mc){return new HashSet<>(((CullingChunkCacheAccessor)mc.world.method_259()).power$loadedChunks().keySet());}
  /** Stage a real projected surface. The returned GUI point is suitable for a physical middle click. */
  private static int[] stage(Minecraft mc,Screen photo,boolean marker)throws Exception{
    field(photo,"tilt",90F);field(photo,"tiltGoal",90F);field(photo,"zoom",4F);field(photo,"zoomGoal",4F);
    field(photo,"cameraX",0F);field(photo,"cameraY",0F);
    var bounds=(RenderedTerrainBounds)mc.worldRenderer;
    int x=(int)Math.floor((bounds.powerTerrain$minX()+bounds.powerTerrain$maxX())*.5),
        z=(int)Math.floor((bounds.powerTerrain$minZ()+bounds.powerTerrain$maxZ())*.5),y=127;
    check(MapSampler.loaded(mc.world,x>>4,z>>4)!=null,"renderer-center fixture chunk is not loaded");
    for(;y>=0;y--){
      int id=mc.world.getBlockId(x,y,z);if(id==0)continue;
      var block=net.minecraft.block.Block.BLOCKS[id];if(block==null)continue;
      var material=block.field_1900;
      if(material==net.minecraft.block.Material.WATER||material==net.minecraft.block.Material.LAVA
          ||material==net.minecraft.block.Material.LEAVES||block.method_1624(mc.world,x,y,z)!=null)break;
    }
    check(y>=0,"fixture has no loaded surface");
    Method camera=net.minecraft.class_555.class.getDeclaredMethod("method_1840",float.class,int.class);camera.setAccessible(true);
    camera.invoke(mc.field_2818,1F,0);PhotoPicking.capture(mc,1F);
    var model=BufferUtils.createFloatBuffer(16);var projection=BufferUtils.createFloatBuffer(16);var viewport=BufferUtils.createIntBuffer(16);var point=BufferUtils.createFloatBuffer(3);
    GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,model);GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX,projection);GL11.glGetInteger(GL11.GL_VIEWPORT,viewport);
    var origin=mc.field_2807==null?mc.player:mc.field_2807;
    check(GLU.gluProject((float)(x+.5-origin.x),(float)(y+.5-origin.y),(float)(z+.5-origin.z),model,projection,viewport,point),"native photo projection failed");
    int px=Math.round(point.get(0)*photo.width/mc.displayWidth),py=Math.round((mc.displayHeight-point.get(1))*photo.height/mc.displayHeight);
    check(px>=24&&px<photo.width-24&&py>=48&&py<photo.height-72,"fixture surface outside uncluttered photo viewport: "+px+","+py);
    if(marker){
      var style=MapConfig.copy();style.photoWaypoints=true;MapConfig.preview(style);
      Waypoint destination=MapKeys.newWaypoint();destination.x=x;destination.y=Math.min(127,y+1);destination.z=z;Maps.destination(destination);Maps.tick(mc);WaypointMarkers.project(mc,1F);
      Waypoint hovered=WaypointMarkers.hovered(mc,px,py);
      check(hovered!=null&&hovered.x==x&&hovered.z==z,"staged marker is not hoverable through shared native projection");
      log("PHOTO450 staged pointer "+px+","+py+" -> terrain "+x+","+y+","+z+"; destination marker shares this X/Z. Use middle x y, then stage-modal for modal capture.");
    }
    return new int[]{px,py,x,y,z};
  }

}
