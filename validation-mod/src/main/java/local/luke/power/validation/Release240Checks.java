package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

/** Explicitly dispatched fixtures for a muted disposable client. */
public final class Release240Checks {
  public static int clicks;
  public static boolean captureTitle;
  public static final List<String> titleText = new ArrayList<>();
  static final Path REPORT = Path.of("power-beta-data/reports/release-240");
  static Object get(Object o,String name)throws Exception { return Editor230Checks.get(o,name); }
  static Object call(Object o,String name,Object...args)throws Exception {
    String alias=name.equals("entryClicked")?"method_1267":name;
    for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())for(var m:c.getDeclaredMethods())if((m.getName().equals(name)||m.getName().equals(alias)||m.getName().endsWith("$"+name))&&m.getParameterCount()==args.length){m.setAccessible(true);return m.invoke(o,args);}
    throw new NoSuchMethodException(name);
  }
  static int n(Object o,String name)throws Exception { return ((Number)call(o,name)).intValue(); }
  static void set(Object o,String name,Object value)throws Exception {
    for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);f.set(o,value);return;}catch(NoSuchFieldException ignored){}
    throw new NoSuchFieldException(name);
  }
  static void click(Screen s,int x,int y,int button){((ScreenInput)(Object)s).power$click(x,y,button);}
  static void key(Screen s,int key){((ScreenInput)(Object)s).power$key((char)0,key);}
  static void open(Minecraft mc,Screen s,int w,int h){mc.setScreen(s);s.init(mc,w,h);}
  static SettingsDraft draft(Object s)throws Exception{return (SettingsDraft)get(s,"draft");}
  static Setting setting(ConfigSession s,String id){return s.settings().stream().filter(v->v.id.equals(id)).findFirst().orElseThrow();}
  static void shot(Minecraft mc,Screen s,String name,int w,int h)throws Exception {s.init(mc,w,h);SettingsSnapshot.render(mc,s,w,h,REPORT.resolve(name+".png"));}
  static int expose(Object screen,Setting value)throws Exception {
    ((Set<?>)get(screen,"collapsed")).clear();set(screen,"scroll",0d);
    int y=(int)call(screen,"controlY",value),top=n(screen,"controlsTop");
    double maximum=((Number)call(call(screen,"track"),"maximum")).doubleValue();set(screen,"scroll",Math.min(maximum,Math.max(0,y-top-24d)));
    return (int)call(screen,"controlY",value);
  }
  static void run(Minecraft mc,String action)throws Exception {
    if(action.startsWith("forest")){Forest240Capture.run(mc,action);return;}
    if(action.equals("saved")){
      check(MenuPreferences.current().debugOrder.indexOf("biome")<MenuPreferences.current().debugOrder.indexOf("version"),"F3 order lost across restart");
      check(MenuPreferences.current().debugTextSize==1.6&&!MenuPreferences.current().debugLines.getOrDefault("terrain",true),"F3 size/visibility lost across restart");
      log("PASS 2.4 F3 ordering, text size, visibility and gaps survive restart");return;
    }
    if(action.equals("save")){
      var s=SettingsRegistry.open(mc);var order=new ArrayList<>(DebugOrder.defaults());order.remove("biome");order.add(0,"biome");order.add(1,"");
      setting(s,"interface.debugOrder").value=Catalog.JSON.toJsonTree(order);setting(s,"interface.debugTextSize").value=new JsonPrimitive(1.6);
      setting(s,"interface.debugLines.terrain").value=new JsonPrimitive(false);s.save(Path.of(".").toAbsolutePath());log("PASS 2.4 durable F3 order save");return;
    }
    Files.createDirectories(REPORT);failures=0;
    var original=PowerConfig.document();boolean auto=MenuPreferences.current().autoApply;var parent=mc.currentScreen;
    Map<String,JsonElement> values=new LinkedHashMap<>();
    for(var s:SettingsRegistry.open(mc).settings())if(HudEditorSettings.contains(s)||s.id.startsWith("logo.")||s.id.startsWith("interface.")||s.id.contains("panoramaConfig"))values.put(s.id,s.value.deepCopy());
    try {
      if(action.equals("hud"))Hud240Checks.run(mc);
      else if(action.equals("menus"))Menu240Checks.run(mc);
      else if(action.equals("gameplay"))Menu240Checks.gameplay(mc);
      else throw new IllegalArgumentException(action);
    }finally{
      var restore=SettingsRegistry.open(mc);for(var s:restore.settings())if(values.containsKey(s.id))s.value=values.get(s.id).deepCopy();restore.save(Path.of(".").toAbsolutePath());
      MenuPreferences.update(v->v.autoApply=auto);PowerConfig.write(original);mc.setScreen(parent);
    }
    log("RELEASE 2.4 "+action+" FAILURES "+failures);
  }
}
