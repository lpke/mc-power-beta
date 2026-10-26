package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Exercise actual screen dispatch so visibility and hit targets cannot drift apart. */
final class AudioRefinementChecks {
  private static void escape(PowerOptionsScreen s) {
    ((ScreenInput)(Object)s).power$key('\0',Keyboard.KEY_ESCAPE);
  }

  private static void sidebar(PowerOptionsScreen s, String page) throws Exception {
    int index=s.visiblePages().indexOf(page);check(index>=0,"missing tab "+page);
    field(s,"sideScroll",0d);
    click(s,(int)call(s,"origin")+18,24+index*22+10);
  }

  private static void back(PowerOptionsScreen s) throws Exception {
    click(s,(int)call(s,"left")+10,35);
  }

  private static void dialog(PowerOptionsScreen s,int choice) {
    int width=Math.min(312,s.width-20);
    click(s,(s.width-width)/2+choice*(width/3)+8,s.height/2+8);
  }

  private static MusicLibraryScreen open(PowerOptionsScreen s) throws Exception {
    field(s,"libraryOpen",true);
    var library=(MusicLibraryScreen)field(s,"library");
    library.openPresets();
    s.render(-1,-1,0);
    check((boolean)call(s,"musicControlsVisible"),"audio toolbar unavailable in presets");
    return library;
  }

  private static Object create(PowerOptionsScreen s, MusicLibraryScreen library) throws Exception {
    Object panel=field(library,"presets");
    click(s,(int)call(panel,"left")+10,(int)call(panel,"top")+8);
    check((boolean)call(panel,"editing"),"create missed");
    return panel;
  }

  private static void toolbar(PowerOptionsScreen s) throws Exception {
    check((boolean)call(s,"musicControlsVisible"),"toolbar stayed hidden");
    check(!(boolean)call(s,"libraryVisible"),"preset panel stayed open");
    s.render(-1,-1,0);
    AudioController.pause();
    musicClick(s,AudioToolbar.Action.PLAY);
    check(!AudioController.status().matches("Music paused|Paused: .+"),"visible Play button did not respond");
    AudioController.pause();
  }

  static void run(Minecraft mc) throws Exception {
    SilentAudioChecks.ready(mc);
    failures=0;
    AudioSettings before=AudioConfig.copy();
    var queue=MusicRequests.tracks();
    boolean auto=MenuPreferences.current().autoApply;
    MenuPreferences.update(v->v.autoApply=false);
    try {
      var config=new AudioSettings();
      MusicFixtureSupport.allBuiltin(config);
      config.playlists.add(new MusicPlaylist("test","Test",AudioController.music(mc).stream().filter(id -> !id.equals("music:calm1.ogg")).toList(), Map.of()));
      AudioConfig.preview(config);
      AudioController.pause();
      PowerOptionsScreen s=MenuUpdateChecks.open(mc,"Audio");
      for(String route:List.of("Back","Escape","Audio","Sidebar"))
        test("preset list exit via "+route+" restores toolbar",()->{
          open(s);
          switch(route) {
            case "Back" -> back(s);
            case "Escape" -> escape(s);
            case "Audio" -> sidebar(s,"Audio");
            case "Sidebar" -> { sidebar(s,"General");sidebar(s,"Audio"); }
          }
          toolbar(s);
        });
      for(int choice:new int[]{0,1,2}) {
        final int outcome=choice;
        test("new preset navigation handles Save/Discard/Cancel "+choice,()->{
          var library=open(s);
          var panel=create(s,library);
          ((TextInput)field(panel,"name")).setText("Exit test "+outcome);
          sidebar(s,"Audio");
          check(library.modal(),"dirty draft exited without prompt");
          dialog(s,outcome);
          if(outcome==2) {
            check(library.dirtyPreset()&&!library.modal(),"Cancel lost draft");
            check(library.presetVisible(),"Cancel closed preset editor");
            escape(s);dialog(s,1);escape(s);
          }
          toolbar(s);
        });
      }
      test("unchanged editor Back returns through the list with no stale panel state",()->{
        var library=open(s);var panel=field(library,"presets");
        MusicPlaylists460Checks.edit(s,library,"test");
        check((boolean)call(panel,"editing"),"Edit missed");
        back(s);check(!library.dirtyPreset()&&library.presetVisible(),"editor did not return to list");
        back(s);toolbar(s);
      });
      test("library tabs and playlist header fit supported sizes without bulk controls",()->{
        var library=open(s);var panel=create(s,library);
        var draft=(MusicPresetDraft)call(panel,"draft");
        var selected=List.copyOf(MusicFixtureSupport.tracks(AudioConfig.current()));
        for(int[] size:new int[][]{{320,240},{427,240},{640,420},{854,480}}) {
          s.init(mc,size[0],size[1]);s.render(-1,-1,0);
          int left=(int)call(panel,"left"),right=(int)call(panel,"right");
          int nx=(int)call(panel,"nameX"),nw=(int)call(panel,"nameWidth");
          check(nx>=left&&nw>=40&&nx+nw<=right,"name input clipped");
          check((int)call(panel,"groupedX")+(int)call(panel,"viewWidth")<=right,"playlist view control clipped");
          check(draft.tracks().isEmpty(),"new draft expanded implicitly");
          check(MusicFixtureSupport.tracks(AudioConfig.current()).equals(selected),"draft changed selected saved membership");
          check(GL11.glGetError()==0,"GL error");
        }
        mc.setScreen(s);sidebar(s,"Audio");dialog(s,1);toolbar(s);
      });
      test("conversion button is absent without pending MP3 files and hidden in queue/presets",()->{
        musicEditor(s);
        var library=(MusicLibraryScreen)field(s,"library");
        field(library,"conversionNeeded",false);
        check(!library.conversionVisible(),"empty conversion button shown");
        field(library,"conversionNeeded",true);
        check(library.conversionVisible(),"pending conversion hidden");
        library.showQueue();check(!library.conversionVisible(),"queue shows conversion");
        library.openPresets();check(!library.conversionVisible(),"presets show conversion");
        sidebar(s,"Audio");toolbar(s);
      });
      test("reopening Options after preset screens cannot restore a hidden toolbar flag",()->{
        open(s);mc.setScreen(null);
        PowerOptionsScreen reopened=new PowerOptionsScreen(null);mc.setScreen(reopened);
        sidebar(reopened,"Audio");toolbar(reopened);
      });
    } finally {
      AudioController.pause();AudioConfig.preview(before);
      MusicRequests.edit(q->{q.tracks.clear();q.tracks.addAll(queue);});
      MenuPreferences.update(v->v.autoApply=auto);mc.setScreen(null);
    }
    log("AUDIO REFINEMENTS FAILURES "+failures);
  }
}
