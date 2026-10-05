package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import java.lang.reflect.Method;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.Setting;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

final class InlineValueChecks {
  private static void press(Screen s, int x, int y, int button) {
    ((ScreenInput)(Object)s).power$click(x,y,button);
  }
  private static void key(Screen s, int key) {
    ((ScreenInput)(Object)s).power$key('\0',key);
  }
  private static void text(Object editor, String value) throws Exception {
    ((TextInput)field(editor,"input")).setText(value);
  }
  private static void commit(Screen screen, Object editor) throws Exception {
    press(screen,(int)field(editor,"x")+(int)field(editor,"width")-9,(int)field(editor,"y")+8,0);
  }
  private static Setting start(PowerOptionsScreen s, String id) throws Exception {
    field(s,"libraryOpen",false); field(s,"relatedIds",List.of(id)); search(s,"");
    field(s,"scroll",0d); s.render(-1,-1,0);
    Object row=((List<?>)field(s,"rows")).get(0);
    Method x=s.getClass().getDeclaredMethod("mainControlLeft",row.getClass());x.setAccessible(true);
    int y=(int)call(s,"top")+((int)call(row,"height")==40?16:2);
    press(s,(int)x.invoke(s,row)+10,y+8,1);
    check((boolean)call(field(s,"valueEditor"),"active"),"right-click missed slider");
    return (Setting)call(row,"setting");
  }

  static void run(Minecraft mc) throws Exception {
    failures=0;
    boolean auto=MenuPreferences.current().autoApply;
    var audio=AudioConfig.copy();
    MenuPreferences.update(v->v.autoApply=false);
    PowerOptionsScreen options=MenuUpdateChecks.open(mc,"General");
    try {
      options.init(mc,640,420);
      Object editor=field(options,"valueEditor");
      test("numeric drafts validate before changing settings, then commit inline",()->{
        Setting s=start(options,"native.fpsLimit");
        var before=s.value.deepCopy();
        for(String invalid:List.of("", "NaN", "2.5", "999999999999999999999", "-1")) {
          text(editor,invalid);commit(options,editor);
          check(s.value.equals(before),"invalid input changed setting: "+invalid);
          check((boolean)call(editor,"active"),"invalid input closed editor");
        }
        text(editor,"850");commit(options,editor);
        check(s.value.getAsInt()==850,"check did not commit");
        check(!(boolean)call(editor,"active")&&mc.currentScreen==options,"commit navigated");
      });
      test("Escape and outside clicks cancel; keyboard input stays out of search",()->{
        Setting s=start(options,"native.sensitivity");var before=s.value.deepCopy();
        ((ScreenInput)(Object)options).power$key('7',Keyboard.KEY_7);
        check(((TextInput)field(editor,"input")).text().equals("7"),"text did not replace selection");
        check(((TextInput)field(options,"search")).text().isEmpty(),"typing reached search");
        key(options,Keyboard.KEY_ESCAPE);
        check(s.value.equals(before)&&mc.currentScreen==options,"Escape changed value or closed menu");
        start(options,"native.sensitivity");text(editor,"12");press(options,1,1,0);
        check(s.value.equals(before)&&!(boolean)call(editor,"active"),"outside click committed draft");
        start(options,"native.sensitivity");text(editor,"12");
        field(options,"relatedIds",List.of("native.invert"));call(options,"layout");options.render(-1,-1,0);
        check(s.value.equals(before)&&!(boolean)call(editor,"active"),"hidden editor retained a stale target");
      });
      test("Enter commits through the same live GUI scale path",()->{
        Setting s=start(options,"native.guiScale");int scale=s.value.getAsInt()==1?2:1;
        text(editor,Integer.toString(scale));key(options,Keyboard.KEY_RETURN);
        check(s.value.getAsInt()==scale&&mc.currentScreen==options,"GUI scale lost screen");
        check(options.width==(mc.displayWidth+scale-1)/scale,"GUI scale was not immediate");
        check(!(boolean)call(editor,"active"),"resizing left editor active");
        options.init(mc,640,420);
      });
      test("inline inputs preserve outer clipping at four GUI sizes",()->{
        for(int[] size:new int[][]{{320,240},{427,240},{640,420},{854,480}}) {
          options.init(mc,size[0],size[1]);start(options,"native.sensitivity");
          options.render(-1,-1,0);
          check(GL11.glGetError()==0,"input GL error");
          check(!GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),"input leaked scissor state");
          key(options,Keyboard.KEY_ESCAPE);
        }
        options.init(mc,640,420);
      });
      test("library volumes edit in place without changing neighbours or row width",()->{
        field(options,"relatedIds",List.of());field(options,"page","Audio");search(options,"");
        field(options,"libraryOpen",true);
        var library=(MusicLibraryScreen)field(options,"library");library.showTrack("music:calm1.ogg");
        ((TextInput)field(library,"search")).setText("calm1");call(library,"rebuild");
        field(library,"trackScroll",0);
        var row=((List<?>)field(library,"rows")).stream().filter(r->{try{return "music:calm1.ogg".equals(call(r,"id"));}catch(Exception e){throw new RuntimeException(e);}}).findFirst().orElseThrow();
        var layout=(MusicRowLayout)call(library,"rowLayout");
        int y=(int)call(library,"listTop")+(int)call(row,"y")+layout.controlsY();
        press(options,layout.volumeX()+8,y+8,1);
        check((boolean)call(editor,"active"),"track did not edit inline");
        check((int)field(editor,"width")==layout.volumeWidth(),"editor width differs from slider");
        options.render(-1,-1,0);text(editor,"37");commit(options,editor);
        check(find(options.session(),"audio.sound.music:calm1.ogg").value.getAsInt()==37,"track volume not committed");
        check(mc.currentScreen==options,"track edit navigated");
      });
      test("fog cycle drafts support exact inline input and cancellation",()->{
        Setting fog=find(options.session(),"video.fogCycle");
        var screen=new IntegerArrayScreen(options,fog,new IntegerArrayDraft.Rules(2,32,16,true),"Distance","chunks");
        mc.setScreen(screen);screen.init(mc,640,420);
        int x=(int)call(screen,"sliderX");press(screen,x+8,78,1);
        Object entry=field(screen,"valueEditor");text(entry,"31");screen.render(-1,-1,0);commit(screen,entry);
        check(((IntegerArrayDraft)field(screen,"draft")).get(0)==31,"fog draft not committed");
        press(screen,x+8,78,1);text(entry,"99");commit(screen,entry);
        check(((IntegerArrayDraft)field(screen,"draft")).get(0)==31,"invalid fog value changed draft");
        key(screen,Keyboard.KEY_ESCAPE);check(mc.currentScreen==screen,"Escape exited instead of cancelling draft");
        key(screen,Keyboard.KEY_ESCAPE);check(mc.currentScreen==options,"second Escape did not return");
      });
      options.session().discard();
      test("pause-menu status opens the current or paused track and handles silence",()->{
        var config=AudioConfig.copy();config.menuControls=true;AudioConfig.preview(config);
        for(String mode:List.of("playing","paused","silent")) {
          if(mode.equals("silent"))AudioController.quiet();
          else {AudioController.playNow("music:calm1.ogg");if(mode.equals("paused"))AudioController.pause();}
          if(mode.equals("paused"))check(AudioController.status().equals("Paused: "+AudioController.musicLabel("music:calm1.ogg")),"missing paused track title");
          var pause=new net.minecraft.class_525();mc.setScreen(pause);pause.render(-1,-1,0);
          var panel=(PauseMenuMusic)field(pause,"power$music");var bounds=(MenuMusicLayout)field(panel,"bounds");
          press(pause,bounds.x()+bounds.width()/2,bounds.y()+26,0);
          check(mc.currentScreen instanceof PowerOptionsScreen,"status did not open options: "+mode);
          var opened=(PowerOptionsScreen)mc.currentScreen;
          check((boolean)call(opened,"libraryVisible"),"status did not open library: "+mode);
          if(!mode.equals("silent"))check(((MusicLibraryScreen)field(opened,"library")).state().folder().isEmpty(),"track not shown in Everything");
          opened.session().discard();
        }
      });
    } finally {
      options.session().discard();AudioController.pause();AudioConfig.preview(audio);
      MenuPreferences.update(v->v.autoApply=auto);mc.setScreen(null);
    }
    log("INLINE VALUE FAILURES "+failures);
  }
}
