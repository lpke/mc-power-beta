package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.*;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Real native menu dispatch, pointer transitions and grouped HUD draft transactions. */
final class HudMenu230Checks {
  private static final Path REPORT=Path.of("power-beta-data/reports/hud-menu-230");
  private static Object get(Object o,String key)throws Exception{return Editor230Checks.get(o,key);}
  private static Object call(Object o,String key)throws Exception{return Editor230Checks.invoke(o,key,new Class<?>[0]);}
  private static int number(Object o,String key)throws Exception{return ((Number)call(o,key)).intValue();}
  private static void set(Object o,String key,Object value)throws Exception{
    for(Class<?> type=o.getClass();type!=null;type=type.getSuperclass())try{Field f=type.getDeclaredField(key);f.setAccessible(true);f.set(o,value);return;}catch(NoSuchFieldException ignored){}
    throw new NoSuchFieldException(key);
  }
  private static void click(Screen s,int x,int y,int button){((ScreenInput)(Object)s).power$click(x,y,button);}
  private static void open(Minecraft mc,Screen s,int w,int h){mc.setScreen(s);s.init(mc,w,h);}
  private static SettingsDraft draft(Object screen)throws Exception{return (SettingsDraft)get(screen,"draft");}
  private static void select(HudEditorScreen s,String id)throws Exception{Editor230Checks.invoke(s,"select",new Class<?>[]{String.class},id);}
  private static void refresh(HudEditorScreen s)throws Exception{call(s,"afterEdit");}
  private static void resetButton(HudEditorScreen s,String label)throws Exception{
    for(Object button:(List<?>)call(s,"resetButtons"))if(call(button,"label").equals(label)){
      click(s,number(button,"x")+number(button,"width")/2,number(button,"y")+10,0);return;
    }
    throw new AssertionError("Missing reset action "+label);
  }
  private static boolean position(Setting s)throws Exception{
    Method method=Class.forName("local.luke.power.ui.HudEditorSections").getDeclaredMethod("position",Setting.class);
    method.setAccessible(true);return (boolean)method.invoke(null,s);
  }
  private static void footer(HudEditorScreen s,String label)throws Exception{
    for(Object button:(List<?>)call(s,"footer"))if(call(button,"label").equals(label)){
      click(s,number(button,"x")+number(button,"width")/2,s.height-19,0);return;
    }
    throw new AssertionError("Missing footer "+label);
  }
  private static boolean paused()throws Exception{Field f=AudioController.class.getDeclaredField("paused");f.setAccessible(true);return f.getBoolean(null);}
  static void run(Minecraft mc)throws Exception{
    failures=0;Files.createDirectories(REPORT);var document=PowerConfig.document();var audio=AudioConfig.copy();
    var parent=mc.currentScreen;var world=mc.world;var helper=mc.field_2767;boolean fullscreen=Display.isFullscreen();
    boolean auto=MenuPreferences.current().autoApply;
    try{
      mc.world=null;MenuPreferences.update(v->v.autoApply=false);
      test("sound category and individual volume help uses Game sounds",()->{
        var session=SettingsRegistry.open(mc);int checked=0;
        for(Setting s:session.settings())if(s.id.startsWith("audio.category.")||s.id.startsWith("audio.sound.")&&!s.id.startsWith("audio.sound.music:")){
          check(s.description.contains("Game sounds")&&!s.description.contains("sound-effects"),"old volume label "+s.id);checked++;
        }
        check(checked>10,"volume fixture did not find sound controls");
      });
      test("native and raw mouse helpers preserve the actual pointer across menus and centre only on capture release",()->{
        for(boolean raw:new boolean[]{false,true}){
          mc.field_2767=raw?(net.minecraft.class_596)Class.forName("local.luke.power.controls.tweaks.rawinput.RawMouseHelper")
              .getConstructor(java.awt.Component.class).newInstance(mc.canvas):new net.minecraft.class_596(mc.canvas);
          for(boolean full:new boolean[]{false,true}){
            Display.setFullscreen(full);Mouse.setGrabbed(false);
            var title=new TitleScreen();open(mc,title,640,420);
            Mouse.setCursorPosition(117,91);
            for(int step=0;step<5;step++){Display.processMessages();Mouse.poll();while(Mouse.next()){}Thread.sleep(10);}
            var pointer=java.awt.MouseInfo.getPointerInfo().getLocation();
            int initialX=Mouse.getX(),initialY=Mouse.getY();
            for(Screen menu:new Screen[]{new PowerOptionsScreen(title),new net.minecraft.client.gui.screen.world.SelectWorldScreen(title),new net.minecraft.client.gui.screen.pack.PackScreen(title)}){
              mc.setScreen(menu);mc.setScreen(title);Mouse.poll();
              check(java.awt.MouseInfo.getPointerInfo().getLocation().equals(pointer),"menu return warped native pointer: "+raw+"/"+full);
              check(Mouse.getX()==initialX&&Mouse.getY()==initialY,"menu return changed cached cursor "+initialX+","+initialY+" -> "+Mouse.getX()+","+Mouse.getY()+": "+raw+"/"+full);
              mc.setScreen(menu);((ScreenInput)(Object)menu).power$key((char)0,Keyboard.KEY_ESCAPE);Mouse.poll();
              check(!Mouse.isGrabbed()&&java.awt.MouseInfo.getPointerInfo().getLocation().equals(pointer),"Escape recaptured or moved the menu pointer: "+raw+"/"+full);
            }
            mc.field_2767.method_1970();check(Mouse.isGrabbed(),"capture failed");
            mc.field_2767.method_1971();check(!Mouse.isGrabbed(),"capture was not released");
            Mouse.poll();check(Mouse.getX()==Display.getWidth()/2&&Mouse.getY()==Display.getHeight()/2,"gameplay capture release did not centre");
          }
        }
      });
      Display.setFullscreen(fullscreen);mc.field_2767=helper;
      test("native title clicks operate every music control and status link without cycling the logo",()->{
        var settings=AudioConfig.copy();settings.master=0;settings.menuControls=settings.menuControlsMainMenu=true;
        settings.menuControlsScrub=false;settings.menuControlsOffsetX=settings.menuControlsOffsetY=0;
        byte[] before=Files.readAllBytes(PowerConfig.path());
        for(var anchor:new AudioSettings.MenuControlsPosition[]{AudioSettings.MenuControlsPosition.MENU_TOP,AudioSettings.MenuControlsPosition.MENU_BOTTOM,AudioSettings.MenuControlsPosition.TOP_CENTER})
          for(int[] size:new int[][]{{320,240},{640,420}}){
            settings.menuControlsPosition=anchor;AudioConfig.preview(settings);
            var title=new TitleScreen();open(mc,title,size[0],size[1]);title.render(-1,-1,0);
            var panel=(PauseMenuMusic)get(title,"power$music");var bounds=(MenuMusicLayout)get(panel,"bounds");
            int x=number(panel,"controlsX"),y=bounds.y()+8;
            AudioController.playNow("music:calm1.ogg");String first=AudioController.currentTrackId();
            AudioController.playNow("music:calm2.ogg");String second=AudioController.currentTrackId();
            check(!first.isEmpty()&&!second.isEmpty()&&!first.equals(second),"start the sound engine before this fixture");
            AudioController.pause();click(title,x+8,y,0);check(!paused(),"Play missed at "+anchor+"/"+size[0]);
            click(title,x+8,y,0);check(paused(),"Pause missed");
            click(title,x+32,y,0);check(AudioController.currentTrackId().equals(first),"Previous missed");
            click(title,x+56,y,0);check(AudioController.currentTrackId().equals(second),"Next missed");
            click(title,x+80,y,0);check(AudioController.currentTrackId().isEmpty(),"Quiet missed");
            click(title,bounds.x()+20,bounds.y()+25,0);check(mc.currentScreen instanceof PowerOptionsScreen,"track link missed");
            open(mc,title,size[0],size[1]);title.render(-1,-1,0);
            click(title,bounds.x()+20,bounds.y()+25,1);check(mc.currentScreen instanceof PowerOptionsScreen,"audio/queue link missed");
          }
        check(Arrays.equals(before,Files.readAllBytes(PowerConfig.path())),"music clicks changed saved title/settings");
      });
      AudioController.quiet();AudioConfig.preview(audio);
      test("one top selector reaches every HUD setting with shared collapsible groups",()->{
        var options=new PowerOptionsScreen(null);open(mc,options,640,420);var hud=new HudEditorScreen(options,HudEditorSettings.ENTRY);
        open(mc,hud,640,420);var local=draft(hud);Set<String> seen=new HashSet<>();
        for(int mode=0;mode<3;mode++){
          local.get(HudEditorSettings.ENTRY).value=new JsonPrimitive(mode);refresh(hud);
          for(String id:(List<String>)call(hud,"selectionIds")){
            select(hud,id);List<Setting> controls=(List<Setting>)call(hud,"controls");controls.forEach(s->seen.add(s.id));
            check(controls.stream().noneMatch(s->s.id.equals("infoHud.selectedCluster")),"redundant cluster picker remains");
            if(id.startsWith("cluster."))check(((String)call(hud,"selectedLabel")).startsWith("Info cluster "),"old cluster label");
            for(int[] size:new int[][]{{320,240},{640,420}}){
              open(mc,hud,size[0],size[1]);
              Path image=REPORT.resolve("mode-"+mode+"-"+id+"-"+size[0]+".png");SettingsSnapshot.render(mc,hud,size[0],size[1],image);
              check(number(hud,"controlsTop")==55,"top tabs still reserve space");
              check(((String)call(hud,"hint")).isEmpty(),"bottom help remains");
              int beforeRows=((List<?>)call(hud,"controlRows")).size();var before=local.snapshot();
              int hx=number(hud,"left")+8,hy=number(hud,"controlsTop")+8;
              click(hud,hx,hy,0);
              check(((List<?>)call(hud,"controlRows")).size()<beforeRows,"heading did not collapse");
              check(local.snapshot().equals(before),"heading changed a setting");
              click(hud,hx,hy,0);
              check(((List<?>)call(hud,"controlRows")).size()==beforeRows,"heading did not expand");
            }
          }
        }
        for(Setting s:local.settings())if(!Set.of("infoHud.selectedCluster","interface.colours").contains(s.id))check(seen.contains(s.id),"HUD setting became unreachable: "+s.id);
        select(hud,"cluster.0");set(hud,"scroll",46d);
        SettingsSnapshot.render(mc,hud,640,420,REPORT.resolve("scrolled-cluster.png"));Object scroll=get(hud,"scroll");
        click(hud,number(hud,"left")+8,38,0);((ScreenInput)(Object)hud).power$key((char)0,Keyboard.KEY_RETURN);
        check(get(hud,"scroll").equals(scroll),"opening or confirming the current selector item reset scroll");
      });
      test("all four HUD reset scopes undo through the button and preserve parent/disk",()->{
        for(int[] size:new int[][]{{320,240},{640,420}}){
          var options=new PowerOptionsScreen(null);open(mc,options,size[0],size[1]);
          byte[] bytes=Files.readAllBytes(PowerConfig.path());
          for(String id:List.of("layout","combined","native.4","cluster.1","tweaks","version","debug","chat"))
            for(int choice=0;choice<4;choice++){
              var hud=new HudEditorScreen(options,HudEditorSettings.ENTRY);open(mc,hud,size[0],size[1]);var local=draft(hud);
              for(Setting s:local.settings()){
                if(Set.of("infoHud.selectedCluster","interface.colours").contains(s.id))continue;
                if(s.kind==Setting.Kind.TEXT)s.value=new JsonPrimitive(s.id.endsWith("Color")?"#224466":"Custom version");else s.cycle(1);
              }
              local.get(HudEditorSettings.ENTRY).value=new JsonPrimitive(id.startsWith("native.")?2:1);select(hud,id);refresh(hud);
              var before=local.snapshot();List<Setting> scope=(List<Setting>)call(hud,choice<2?"allResetSettings":"controls");
              boolean positions=choice%2==1;Set<String> targets=new HashSet<>();
              for(Setting setting:scope)if(!positions||position(setting))targets.add(setting.id);
              // Open the real footer, then consume a background click outside its buttons.
              footer(hud,"Reset...");check((boolean)get(hud,"resetPrompt"),"reset footer missed");
              if(choice==0&&Set.of("combined","cluster.1","debug").contains(id))
                SettingsSnapshot.render(mc,hud,size[0],size[1],REPORT.resolve("reset-"+id+"-"+size[0]+".png"));
              check(call(hud,"resetQuestion").equals("Reset to defaults?"),"reset question is not shared");
              click(hud,5,5,0);check(local.snapshot().equals(before),"reset overlay leaked input");
              List<?> buttons=(List<?>)call(hud,"resetButtons");check(buttons.size()==5,"missing reset choice");
              Object cancel=buttons.get(4);check(number(cancel,"x")+number(cancel,"width")/2==hud.width/2,"Cancel is not centered");
              check(number(cancel,"y")>number(buttons.get(2),"y"),"Cancel is not below the scope rows");
              String label=(String)call(buttons.get(choice),"label");resetButton(hud,label);
              if(targets.isEmpty()){
                check((boolean)get(hud,"resetPrompt"),"empty position reset should be disabled");resetButton(hud,"Cancel");
              }else{
                for(var entry:before.entrySet())check(local.get(entry.getKey()).value.equals(targets.contains(entry.getKey())?local.get(entry.getKey()).defaultValue:entry.getValue()),"reset changed wrong scope: "+entry.getKey());
                footer(hud,"Undo");check(local.snapshot().equals(before),"Undo button failed after "+label+" in "+id);
                check(get(hud,"selected").equals(id),"Undo lost the selected HUD element after "+label);
              }
              check(options.session().changes()==0&&Arrays.equals(bytes,Files.readAllBytes(PowerConfig.path())),"reset changed parent or disk before Done");
            }
        }
      });
      test("audited HUD previews follow their draft switches and remain independently selectable",()->{
        var options=new PowerOptionsScreen(null);open(mc,options,640,420);var hud=new HudEditorScreen(options,HudEditorSettings.ENTRY);
        open(mc,hud,640,420);var local=draft(hud);local.get(HudEditorSettings.ENTRY).value=new JsonPrimitive(1);
        String[] ids={"crosshair","prompts","toast","native.4"};
        String[] fields={"power_hud:config.disableCrosshair","power_hud:config.drawXboxXAndYButtons","power_controls:userinterface.hideAchievementToast","power_hud:config.enableHotbarItemSelectionTooltips"};
        for(int i=0;i<ids.length;i++)for(boolean visible:new boolean[]{true,false}){
          String id=ids[i];boolean negative=i==0||i==2;
          local.get(fields[i]).value=new JsonPrimitive(visible!=negative);select(hud,i==3?"combined":"layout");refresh(hud);
          SettingsSnapshot.render(mc,hud,640,420,REPORT.resolve("preview-"+id+"-"+visible+".png"));
          List<?> elements=(List<?>)call(get(hud,"preview"),"elements");
          if(i<3){boolean found=false;String name=new String[]{"Crosshair","Controller prompts","Achievement toast"}[i];for(Object e:elements)if(call(e,"label").equals(name))found=true;check(found==visible,"preview ignores "+fields[i]);}
          if(i==3){
            local.get(HudEditorSettings.ENTRY).value=new JsonPrimitive(2);select(hud,id);refresh(hud);hud.render(-1,-1,0);
            elements=(List<?>)call(get(hud,"preview"),"elements");
            boolean found=false;for(Object e:elements)if(call(e,"id").equals(id))found=true;
            check(found==visible,"item-name preview ignores its toggle");local.get(HudEditorSettings.ENTRY).value=new JsonPrimitive(1);
          }
        }
        select(hud,"chat");SettingsSnapshot.render(mc,hud,640,420,REPORT.resolve("preview-chat.png"));
      });
    }finally{
      AudioController.quiet();AudioConfig.preview(audio);Mouse.setGrabbed(false);Display.setFullscreen(fullscreen);
      mc.field_2767=helper;mc.world=world;mc.setScreen(parent);PowerConfig.write(document);MenuPreferences.update(v->v.autoApply=auto);
    }
    log("HUD/MENU 2.3 FAILURES "+failures);
  }
}
