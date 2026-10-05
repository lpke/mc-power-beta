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

  private static void sidebar(PowerOptionsScreen s, int index) throws Exception {
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

  private static MusicLibraryScreen open(PowerOptionsScreen s, boolean choosing) throws Exception {
    field(s,"libraryOpen",true);
    var library=(MusicLibraryScreen)field(s,"library");
    library.openPresets(choosing);
    s.render(-1,-1,0);
    check(!(boolean)call(s,"musicControlsVisible"),"toolbar shown over presets");
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
    check(!AudioController.status().equals("Music paused"),"visible Play button did not respond");
    AudioController.pause();
  }

  static void run(Minecraft mc) throws Exception {
    failures=0;
    AudioSettings before=AudioConfig.copy();
    var queue=MusicRequests.tracks();
    boolean auto=MenuPreferences.current().autoApply;
    MenuPreferences.update(v->v.autoApply=false);
    try {
      var config=new AudioSettings();
      config.musicMode=AudioSettings.MusicMode.ALL_MINECRAFT;
      config.presets.add(new MusicPreset("test","Test",Set.of("music:calm1.ogg")));
      AudioConfig.preview(config);
      AudioController.pause();
      PowerOptionsScreen s=MenuUpdateChecks.open(mc,"Audio");
      for(boolean choosing:new boolean[]{false,true}) for(String route:List.of("Back","Escape","Audio","Sidebar"))
        test("preset "+(choosing?"selector":"list")+" exit via "+route+" restores toolbar",()->{
          open(s,choosing);
          switch(route) {
            case "Back" -> back(s);
            case "Escape" -> escape(s);
            case "Audio" -> sidebar(s,2);
            case "Sidebar" -> { sidebar(s,0);sidebar(s,2); }
          }
          toolbar(s);
        });
      for(int choice:new int[]{0,1,2}) {
        final int outcome=choice;
        test("new preset navigation handles Save/Discard/Cancel "+choice,()->{
          var library=open(s,false);
          var panel=create(s,library);
          ((TextInput)field(panel,"name")).setText("Exit test "+outcome);
          sidebar(s,2);
          check(library.modal(),"dirty draft exited without prompt");
          dialog(s,outcome);
          if(outcome==2) {
            check(library.dirtyPreset()&&!library.modal(),"Cancel lost draft");
            check(!(boolean)call(s,"musicControlsVisible"),"Cancel exposed toolbar behind editor");
            escape(s);dialog(s,1);escape(s);
          }
          toolbar(s);
        });
      }
      test("unchanged editor Back returns through the list with no stale panel state",()->{
        var library=open(s,false);var panel=field(library,"presets");
        click(s,(int)call(panel,"right")-85,(int)call(panel,"rowTop")+12);
        check((boolean)call(panel,"editing"),"Edit missed");
        back(s);check(!library.dirtyPreset()&&library.presetVisible(),"editor did not return to list");
        back(s);toolbar(s);
      });
      for(int row:new int[]{0,1}) test("loading preset selector row "+row+" restores toolbar",()->{
        var library=open(s,true);var panel=field(library,"presets");
        click(s,(int)call(panel,"right")-20,(int)call(panel,"rowTop")+row*40+12);
        toolbar(s);
      });
      test("library Exclude all covers hidden filter results and preserves volumes and queue",()->{
        musicClick(s,AudioToolbar.Action.LIBRARY);
        var library=(MusicLibraryScreen)field(s,"library");
        library.restore(new MusicLibraryScreen.State(false,"","calm1",0,0));call(library,"rebuild");
        var volumes=new TreeMap<>(AudioConfig.current().sounds);
        click(s,(int)call(library,"excludeX")+8,(int)call(library,"bulkTop")+8);
        check(AudioConfig.current().disabledTracks.containsAll(AudioController.music(mc)),"hidden tracks were not excluded");
        check(AudioConfig.current().sounds.equals(volumes),"volumes changed");
        check(MusicRequests.tracks().equals(queue),"queue changed");
        click(s,(int)call(library,"includeX")+8,(int)call(library,"bulkTop")+8);
        check(AudioConfig.current().disabledTracks.isEmpty(),"Include all missed exclusions");
        s.session().discard();
        check(AudioConfig.current().disabledTracks.isEmpty(),"discard retained bulk changes");
      });
      test("library bulk buttons never overlap filter tabs at supported sizes",()->{
        var library=(MusicLibraryScreen)field(s,"library");
        library.showTracks();
        library.restore(new MusicLibraryScreen.State(false,"","",0,0));
        for(int[] size:new int[][]{{320,240},{427,240},{640,420},{854,480}}) {
          s.init(mc,size[0],size[1]);s.render(-1,-1,0);
          var filters=(MusicFilters.Layout)call(library,"filters");
          int bulkY=(int)call(library,"bulkTop"),includeX=(int)call(library,"includeX");
          for(var tab:filters.tabs()) if(tab.y()==bulkY)
            check(tab.x()+tab.width()<includeX,"filter overlaps bulk buttons");
          check((int)call(library,"listTop")>=bulkY+18,"tracks overlap bulk buttons");
        }
        mc.setScreen(s);
      });
      test("preset bulk edits stay isolated and headers fit narrow/wide screens",()->{
        var library=open(s,false);field(library,"trackScroll",100);var panel=create(s,library);
        check(library.state().trackScroll()==0,"editor inherited library scroll");
        for(int[] size:new int[][]{{320,240},{427,240},{640,420},{854,480}}) {
          s.init(mc,size[0],size[1]);s.render(-1,-1,0);
          int left=(int)call(panel,"left"),right=(int)call(panel,"right");
          int nx=(int)call(panel,"nameX"),nw=(int)call(panel,"nameWidth");
          check(nx>left&&nw>=60&&nx+nw<=right,"name input clipped");
          check((int)call(panel,"includeX")>=left,"bulk buttons clipped");
          click(s,(int)call(panel,"excludeX")+8,(int)call(panel,"bulkY")+8);
          var draft=(MusicPresetDraft)call(panel,"draft");
          check(draft.snapshot("Test").excluded().containsAll(AudioController.music(mc)),"preset Exclude all missed tracks");
          check(AudioConfig.current().disabledTracks.isEmpty(),"preset edit changed live selection");
          click(s,(int)call(panel,"includeX")+8,(int)call(panel,"bulkY")+8);
          check(draft.snapshot("Test").excluded().isEmpty(),"preset Include all missed tracks");
          check(GL11.glGetError()==0,"GL error");
        }
        mc.setScreen(s);sidebar(s,2);dialog(s,1);toolbar(s);
      });
      test("conversion button is absent without pending MP3 files and hidden in queue/presets",()->{
        musicClick(s,AudioToolbar.Action.LIBRARY);
        var library=(MusicLibraryScreen)field(s,"library");
        field(library,"conversionNeeded",false);
        check(!library.conversionVisible(),"empty conversion button shown");
        field(library,"conversionNeeded",true);
        check(library.conversionVisible(),"pending conversion hidden");
        library.showQueue();check(!library.conversionVisible(),"queue shows conversion");
        library.openPresets(false);check(!library.conversionVisible(),"presets show conversion");
        sidebar(s,2);toolbar(s);
      });
      test("reopening Options after preset screens cannot restore a hidden toolbar flag",()->{
        open(s,false);mc.setScreen(null);
        PowerOptionsScreen reopened=new PowerOptionsScreen(null);mc.setScreen(reopened);
        sidebar(reopened,2);toolbar(reopened);
      });
    } finally {
      AudioController.pause();AudioConfig.preview(before);
      MusicRequests.edit(q->{q.tracks.clear();q.tracks.addAll(queue);});
      MenuPreferences.update(v->v.autoApply=auto);mc.setScreen(null);
    }
    log("AUDIO REFINEMENTS FAILURES "+failures);
  }
}
