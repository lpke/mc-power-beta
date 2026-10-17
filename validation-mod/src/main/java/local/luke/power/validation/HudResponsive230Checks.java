package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import java.nio.file.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import net.minecraft.client.Minecraft;

/** Forest asset capture and HUD editor regression checks, disposable instance only. */
final class HudResponsive230Checks {
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("forest")) { forest(mc); return; }
    if (action.startsWith("view ")) {
      String[] pose = action.substring(5).split(" ");
      mc.player.yaw = mc.player.prevYaw = Float.parseFloat(pose[0]);
      mc.player.pitch = mc.player.prevPitch = Float.parseFloat(pose[1]);
      return;
    }
    checks(mc,action);
  }
  private static final Path REPORT=Path.of("power-beta-data/reports/hud-responsive-230");
  private static Object get(Object o,String name)throws Exception{return Editor230Checks.get(o,name);}
  private static Object call(Object o,String name)throws Exception{return Editor230Checks.invoke(o,name,new Class<?>[0]);}
  private static Object invoke(Object o,String name,Class<?>[] types,Object... args)throws Exception{return Editor230Checks.invoke(o,name,types,args);}
  private static int n(Object o,String name)throws Exception{return ((Number)call(o,name)).intValue();}
  private static double d(Object o,String name)throws Exception{return ((Number)call(o,name)).doubleValue();}
  private static void set(Object o,String name,Object value)throws Exception{
    for(Class<?> type=o.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);f.set(o,value);return;}catch(NoSuchFieldException ignored){}
    throw new NoSuchFieldException(name);
  }
  private static void click(Screen s,int x,int y,int button){((ScreenInput)(Object)s).power$click(x,y,button);}
  private static void key(Screen s,int key){((ScreenInput)(Object)s).power$key((char)0,key);}
  private static void open(Minecraft mc,Screen s,int w,int h){mc.setScreen(s);s.init(mc,w,h);}
  private static SettingsDraft draft(Object o)throws Exception{return (SettingsDraft)get(o,"draft");}
  private static void select(Object o,String id)throws Exception{invoke(o,"select",new Class<?>[]{String.class},id);}
  private static void refresh(Object o)throws Exception{call(o,"afterEdit");}
  private static void shot(Minecraft mc,HudEditorScreen hud,String name,int w,int h)throws Exception{
    open(mc,hud,w,h);SettingsSnapshot.render(mc,hud,w,h,REPORT.resolve(name+".png"));
  }
  private static List<?> elements(Object hud)throws Exception{return (List<?>)call(get(hud,"preview"),"elements");}
  private static Object element(Object hud,String id)throws Exception{
    for(Object e:elements(hud))if(call(e,"id").equals(id))return e;throw new AssertionError("Missing preview element "+id);
  }
  private static Map<String,com.google.gson.JsonElement> live(Minecraft mc)throws Exception{
    Map<String,com.google.gson.JsonElement> result=new LinkedHashMap<>();
    for(var s:SettingsRegistry.open(mc).settings())if(HudEditorSettings.contains(s))result.put(s.id,s.value.deepCopy());
    return result;
  }
  private static void restore(Minecraft mc,Map<String,com.google.gson.JsonElement> values)throws Exception{
    var session=SettingsRegistry.open(mc);
    for(var s:session.settings())if(values.containsKey(s.id))s.value=values.get(s.id).deepCopy();
    session.save(Path.of(".").toAbsolutePath());
  }
  private static void footer(HudEditorScreen hud,String label)throws Exception{
    for(Object b:(List<?>)call(hud,"footer"))if(call(b,"label").equals(label)){
      click(hud,n(b,"x")+n(b,"width")/2,hud.height-19,0);return;
    }
    throw new AssertionError("Missing footer "+label);
  }
  private static int expose(Minecraft mc,HudEditorScreen hud,Setting s)throws Exception{
    ((Set<?>)get(hud,"collapsed")).clear();set(hud,"scroll",0d);
    int y=(int)invoke(hud,"controlY",new Class<?>[]{Setting.class},s);
    double scroll=Math.max(0,y-n(hud,"controlsTop")-24);
    double maximum=d(call(hud,"track"),"maximum");set(hud,"scroll",Math.min(scroll,maximum));
    return (int)invoke(hud,"controlY",new Class<?>[]{Setting.class},s);
  }
  private static void checks(Minecraft mc,String action)throws Exception{
    Files.createDirectories(REPORT);failures=0;var parent=mc.currentScreen;var original=PowerConfig.document();
    var originalLive=live(mc);boolean auto=MenuPreferences.current().autoApply;
    try{
      if(action.equals("layout"))layout(mc);
      else if(action.equals("ghosts"))ghosts(mc);
      else if(action.equals("drafts"))drafts(mc);
      else throw new IllegalArgumentException(action);
    }finally{
      restore(mc,originalLive);PowerConfig.write(original);
      MenuPreferences.update(v->v.autoApply=auto);mc.setScreen(parent);
    }
    log("HUD RESPONSIVE "+action+" FAILURES "+failures);
  }
  private static void layout(Minecraft mc)throws Exception{
    test("responsive layout, both divider orientations, Normal bubbles, general controls and background switching",()->{
      var options=new PowerOptionsScreen(null);open(mc,options,640,420);
      var hud=new HudEditorScreen(options,HudEditorSettings.ENTRY);var values=draft(hud);
      byte[] bytes=Files.readAllBytes(PowerConfig.path());var before=live(mc);
      for(int[] size:new int[][]{{320,240},{640,420},{960,540},{1720,720},{540,960}}){
        shot(mc,hud,"layout-"+size[0]+"-"+size[1],size[0],size[1]);
        check((boolean)get(hud,"background"),"background should start enabled");
        var ids=(List<String>)call(hud,"selectionIds");check(!ids.contains("crosshair")&&!ids.contains("prompts")&&!ids.contains("toast"),"general controls remain in the selector");
        select(hud,"layout");var controls=(List<Setting>)call(hud,"controls");
        for(String id:List.of("power_hud:config.disableCrosshair","power_hud:config.drawXboxXAndYButtons","power_controls:userinterface.hideAchievementToast"))check(controls.stream().anyMatch(s->s.id.equals(id)),"General missed "+id);
        int sw=n(hud,"selectorWidth"),left=n(hud,"left");
        for(int i=0;i<4;i++){click(hud,left+sw+12,38,0);int scenario=(int)get(hud,"scenario");check(scenario==0||scenario==2,"underwater remains a separate scenario");}
        values.get(HudEditorSettings.ENTRY).value=new com.google.gson.JsonPrimitive(2);refresh(hud);hud.render(-1,-1,0);
        element(hud,"native.3");check((int)get(hud,"scenario")==0,"Normal not selected");
        var snapshot=values.snapshot();click(hud,left+sw+150,38,0);check(!(boolean)get(hud,"background"),"Bg button missed");
        SettingsSnapshot.render(mc,hud,size[0],size[1],REPORT.resolve("solid-"+size[0]+".png"));
        check(values.snapshot().equals(snapshot),"preview background changed a saved setting");click(hud,left+sw+150,38,0);
        boolean horizontal=(boolean)call(call(hud,"geometry"),"horizontal");int x=horizontal?n(hud,"controlsRight")+5:left+15,y=horizontal?100:n(hud,"controlsBottom")+5;
        int old=horizontal?n(hud,"controlsSpan"):n(hud,"controlsBottom");click(hud,x,y,0);
        check((boolean)call(get(hud,"divider"),"dragging"),"divider did not capture");
        ((ScreenInput)(Object)hud).power$release(x+(old>280?-25:25),y+(old>n(hud,"controlsTop")+47?-15:15),0);
        check((horizontal?n(hud,"controlsSpan"):n(hud,"controlsBottom"))!=old,"divider did not use release coordinates");
        check(n(hud,"previewBottom")-n(hud,"previewTop")>=48&&n(hud,"previewSpan")>=280,"resize hid the preview");
      }
      check(live(mc).equals(before)&&Arrays.equals(bytes,Files.readAllBytes(PowerConfig.path())),"layout previews changed live settings or disk");
    });
    test("hover tooltips wait thirteen ticks and reset between values and R buttons",()->{
      var options=new PowerOptionsScreen(null);open(mc,options,640,420);var hud=new HudEditorScreen(options,HudEditorSettings.ENTRY);open(mc,hud,640,420);
      var s=draft(hud).get("power_hud:config.disableCrosshair");int y=expose(mc,hud,s)+7,x=n(hud,"controlsLeft")+10;
      SettingsSnapshot.render(mc,hud,640,420,REPORT.resolve("tooltip-before.png"),x,y);
      Object delay=get(hud,"hover");check((int)get(delay,"ticks")==0,"tooltip did not start delayed");
      for(int i=0;i<12;i++)hud.tick();hud.render(x,y,0);check((int)get(delay,"ticks")==12,"render advanced tooltip time");
      hud.tick();SettingsSnapshot.render(mc,hud,640,420,REPORT.resolve("tooltip-after.png"),x,y);
      var a=javax.imageio.ImageIO.read(REPORT.resolve("tooltip-before.png").toFile());var b=javax.imageio.ImageIO.read(REPORT.resolve("tooltip-after.png").toFile());int difference=0;
      for(int py=0;py<a.getHeight();py++)for(int px=0;px<a.getWidth();px++)if(a.getRGB(px,py)!=b.getRGB(px,py))difference++;
      check(difference>100,"delayed tooltip never rendered");hud.render(n(hud,"resetX")+8,y,0);check((int)get(delay,"ticks")==0,"R reused a different target's delay");
    });
  }
  private static void ghosts(Minecraft mc)throws Exception{
    test("disabled, transparent, empty and F3-hidden clusters retain outlines and drag without enabling",()->{
      var options=new PowerOptionsScreen(null);open(mc,options,640,420);var hud=new HudEditorScreen(options,HudEditorSettings.ENTRY);open(mc,hud,640,420);var values=draft(hud);
      values.get("infoHud.enabled").value=new com.google.gson.JsonPrimitive(false);
      values.get("infoHud.clusters.1.enabled").value=new com.google.gson.JsonPrimitive(false);values.get("infoHud.clusters.1.position").value=new com.google.gson.JsonPrimitive(4);
      values.get("infoHud.clusters.1.coordinates").value=new com.google.gson.JsonPrimitive(true);values.get("power_hud:config.disableCrosshair").value=new com.google.gson.JsonPrimitive(true);
      refresh(hud);set(hud,"snapping",false);shot(mc,hud,"hidden-clusters",640,420);
      int count=0;for(Object e:elements(hud))if(((String)call(e,"id")).startsWith("cluster.")){check((boolean)call(e,"hidden"),"disabled HUD rendered a cluster");count++;}check(count==4,"missing hidden cluster outlines");
      Object e=element(hud,"cluster.1");double scale=d(hud,"scale"),cx=d(hud,"canvasX"),cy=d(hud,"canvasY");int x=(int)Math.round(cx+(d(e,"x")+d(e,"width")/2)*scale),y=(int)Math.round(cy+(d(e,"y")+d(e,"height")/2)*scale);
      int old=values.get("infoHud.clusters.1.offsetX").value.getAsInt();click(hud,x,y,0);((ScreenInput)(Object)hud).power$release(x+15,y+5,0);
      check(get(hud,"selected").equals("cluster.1")&&values.get("infoHud.clusters.1.offsetX").value.getAsInt()==old,"first selection dragged a hidden cluster");
      click(hud,x,y,0);((ScreenInput)(Object)hud).power$release(x+15,y+5,0);
      check(values.get("infoHud.clusters.1.offsetX").value.getAsInt()>old,"hidden cluster did not drag");
      check(!values.get("infoHud.enabled").value.getAsBoolean()&&!values.get("infoHud.clusters.1.enabled").value.getAsBoolean(),"drag enabled a hidden cluster");
      values.get("infoHud.enabled").value=new com.google.gson.JsonPrimitive(true);values.get("infoHud.clusters.1.enabled").value=new com.google.gson.JsonPrimitive(true);refresh(hud);hud.render(-1,-1,0);
      check(!(boolean)call(element(hud,"cluster.1"),"hidden"),"enabled cluster stayed hidden");
      values.get("infoHud.clusters.1.opacity").value=new com.google.gson.JsonPrimitive(0);refresh(hud);hud.render(-1,-1,0);check((boolean)call(element(hud,"cluster.1"),"hidden"),"zero-opacity outline missing");
      values.get("infoHud.clusters.1.opacity").reset();values.get("infoHud.clusters.1.hideWithDebug").value=new com.google.gson.JsonPrimitive(true);set(hud,"scenario",2);refresh(hud);hud.render(-1,-1,0);check((boolean)call(element(hud,"cluster.1"),"hidden"),"F3-hidden outline missing");
      for(String field:List.of("coordinates","direction","biome","timeOfDay","day","chunk","fps"))values.get("infoHud.clusters.1."+field).value=new com.google.gson.JsonPrimitive(false);
      set(hud,"scenario",0);refresh(hud);hud.render(-1,-1,0);check((boolean)call(element(hud,"cluster.1"),"hidden")&&d(element(hud,"cluster.1"),"width")>0,"empty cluster has no selectable outline");
      check(options.session().changes()==0,"ghost preview leaked to the parent");
    });
  }
  private static void drafts(Minecraft mc)throws Exception{
    test("every visible HUD setting has an individual undoable default reset",()->{
      var options=new PowerOptionsScreen(null);open(mc,options,640,420);var hud=new HudEditorScreen(options,HudEditorSettings.ENTRY);open(mc,hud,640,420);var values=draft(hud);var before=live(mc);byte[] bytes=Files.readAllBytes(PowerConfig.path());Set<String> checked=new HashSet<>();
      for(int mode=0;mode<3;mode++){
        values.get(HudEditorSettings.ENTRY).value=new com.google.gson.JsonPrimitive(mode);refresh(hud);
        for(String id:(List<String>)call(hud,"selectionIds")){
          select(hud,id);
          for(var s:(List<Setting>)call(hud,"controls")){
            check((boolean)invoke(hud,"resettable",new Class<?>[]{Setting.class},s),"R missing for "+s.id);
            var original=s.value.deepCopy();
            // Keep already-modified conditional rows visible, especially Vanilla's mode selector.
            if(s.value.equals(s.defaultValue)) {
              if(s.kind==Setting.Kind.TEXT)s.value=new com.google.gson.JsonPrimitive(ColourScreen.accepts(s)?"#224466":"Custom preview");else s.cycle(1);
            }
            check(!s.value.equals(s.defaultValue),"unchanged reset fixture "+s.id);var snapshot=values.snapshot();
            int y=expose(mc,hud,s);click(hud,n(hud,"resetX")+8,y+7,0);check(s.value.equals(s.defaultValue),"R failed for "+s.id);
            footer(hud,"Undo");check(values.snapshot().equals(snapshot),"Undo failed for individual R "+s.id);s.value=original;refresh(hud);checked.add(s.id);
          }
        }
      }
      check(checked.size()>100,"HUD setting audit incomplete");check(live(mc).equals(before)&&Arrays.equals(bytes,Files.readAllBytes(PowerConfig.path())),"individual resets changed live settings");
    });
    test("auto/manual apply, nested colours, Done, Escape Save and Discard keep drafts isolated",()->{
      for(boolean automatic:new boolean[]{false,true})for(boolean commit:new boolean[]{false,true}){
        MenuPreferences.update(v->v.autoApply=automatic);
        var options=new PowerOptionsScreen(null);open(mc,options,640,420);var hud=new HudEditorScreen(options,HudEditorSettings.ENTRY);open(mc,hud,640,420);var values=draft(hud);var before=live(mc);byte[] bytes=Files.readAllBytes(PowerConfig.path());
        var toggle=values.get("power_hud:config.disableCrosshair");int y=expose(mc,hud,toggle);click(hud,n(hud,"controlX")+8,y+7,0);
        var pending=values.snapshot();select(hud,"debug");var colour=values.get("interface.debugTextColor");
        for(boolean accept:new boolean[]{false,true}){
          y=expose(mc,hud,colour);click(hud,n(hud,"controlX")+8,y+7,0);check(mc.currentScreen instanceof ColourScreen,"colour picker did not open");var picker=(ColourScreen)mc.currentScreen;
          ((TextInput)get(picker,"hex")).setText("#224466");click(picker,n(picker,"left")+8,n(picker,"top")+122,0);
          check(live(mc).equals(before)&&Arrays.equals(bytes,Files.readAllBytes(PowerConfig.path())),"saving a swatch wrote live state");
          if(accept)click(picker,picker.width/2-52,picker.height-18,0);else key(picker,Keyboard.KEY_ESCAPE);
          check(mc.currentScreen==hud,"colour picker did not return to HUD");
          if(!accept)check(values.get("interface.colours").value.equals(pending.get("interface.colours")),"colour Cancel retained swatches");
        }
        check(values.get("interface.colours").value.getAsJsonArray().toString().contains("#224466"),"colour Done lost the local swatch");
        check(live(mc).equals(before)&&Arrays.equals(bytes,Files.readAllBytes(PowerConfig.path()))&&options.session().changes()==0,"HUD draft changed live or parent settings");
        if(automatic&&commit)footer(hud,"Done");else{
          key(hud,Keyboard.KEY_ESCAPE);check((boolean)call(get(hud,"exit"),"active"),"Escape skipped its warning");click(hud,hud.width/2+(commit?-80:0),hud.height/2+18,0);
        }
        check(mc.currentScreen==options,"exit did not return to Options");
        if(!commit)check(live(mc).equals(before)&&options.session().changes()==0&&Arrays.equals(bytes,Files.readAllBytes(PowerConfig.path())),"Discard changed live settings");
        else{
          if(!automatic)check(Arrays.equals(bytes,Files.readAllBytes(PowerConfig.path())),"manual parent wrote before Apply");
          options.session().save(Path.of(".").toAbsolutePath());check(MenuPreferences.current().colours.contains("#224466"),"committed swatch did not persist");
        }
        restore(mc,before);
      }
    });
  }
  private static void forest(Minecraft mc) throws Exception {
    if(mc.world==null){
      mc.interactionManager=new net.minecraft.SingleplayerInteractionManager(mc);
      mc.method_2120("Power Beta HUD capture","Power Beta HUD capture",17320261003L);
      net.minecraft.client.render.entity.EntityRenderDispatcher.field_2489.method_1917(mc.world,mc.textureManager,mc.textRenderer,mc.player,mc.options,0F);
    }
    if(mc.player.dead||mc.player.health<=0)mc.method_2122(true,0);
    AccessRules.of(mc.world.method_262()).allAllowed(true);CreativeVehicleChecks.mode(mc,"CREATIVE");
    WebsiteGallery.setting(mc,"native.renderDistance","2");
    WebsiteGallery.setting(mc,"power_controls:userinterface.hideAchievementToast","true");
    int fx=0,fz=0;boolean found=false;
    outer:for(int radius=16;radius<=512;radius+=32)for(int x=-radius;x<=radius;x+=16)for(int z=-radius;z<=radius;z+=16)
      if(mc.world.method_1781().method_1787(x,z).field_888.equals("Forest")){fx=x;fz=z;found=true;break outer;}
    check(found,"Forest biome unavailable");
    WebsiteGallery.x=fx;WebsiteGallery.y=104;WebsiteGallery.z=fz;
    for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)mc.world.method_214((fx>>4)+dx,(fz>>4)+dz);
    for(int dx=-20;dx<=20;dx++)for(int dz=-18;dz<=30;dz++){
      for(int dy=-3;dy<0;dy++)WebsiteGallery.block(mc,dx,dy,dz,3,0);
      WebsiteGallery.block(mc,dx,0,dz,2,0);
      for(int dy=1;dy<24;dy++)if(mc.world.getBlockId(fx+dx,104+dy,fz+dz)!=0)WebsiteGallery.block(mc,dx,dy,dz,0,0);
    }
    Random random=new Random(230);List<int[]> trees=new ArrayList<>();
    for(int attempt=0;attempt<300&&trees.size()<40;attempt++){
      int x=random.nextInt(35)-17,z=random.nextInt(43)-14;
      if(x*x+z*z<25||trees.stream().anyMatch(p->(p[0]-x)*(p[0]-x)+(p[1]-z)*(p[1]-z)<12))continue;
      trees.add(new int[]{x,z});int h=5+random.nextInt(3),variant=trees.size()%5==0?2:0;
      for(int dy=h-3;dy<=h;dy++)for(int ax=-2;ax<=2;ax++)for(int az=-2;az<=2;az++){
        int radius=dy==h?1:2;if(Math.abs(ax)>radius||Math.abs(az)>radius||Math.abs(ax)==radius&&Math.abs(az)==radius&&random.nextBoolean())continue;
        WebsiteGallery.block(mc,x+ax,dy,z+az,18,variant|4);
      }
      for(int dy=1;dy<h;dy++)WebsiteGallery.block(mc,x,dy,z,17,variant);
    }
    ChatChecks.submit(mc,"/time set noon");ChatChecks.submit(mc,"/weather clear");
    mc.options.thirdPerson=false;mc.options.hideHud=true;WebsiteGallery.camera(mc,.5,2.62,.5,-11,3);mc.inGameHud.clearChat();
    check(mc.world.method_1781().method_1787((int)Math.floor(mc.player.x),(int)Math.floor(mc.player.z)).field_888.equals("Forest"),"Camera left the forest biome");
    log("FOREST CAPTURE Forest biome, native renderer, first person, HUD hidden");
  }
}
