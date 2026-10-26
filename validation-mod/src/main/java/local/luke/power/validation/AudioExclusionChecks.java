package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

final class AudioExclusionChecks {
  static void run(Minecraft mc) throws Exception {
    failures=0;
    var before=AudioConfig.copy(); var queue=MusicRequests.tracks();
    boolean auto=MenuPreferences.current().autoApply;
    MenuPreferences.update(v->v.autoApply=false);
    PowerOptionsScreen options=null;
    try {
      var settings=new AudioSettings();settings.musicMode=AudioSettings.MusicMode.ALL_MINECRAFT;
      settings.disabledTracks.add("music:calm1.ogg");
      settings.playlists.add(new MusicPlaylist("filter-test","Filter test",AudioController.music(mc).stream().filter(id -> !id.equals("music:creative1.ogg")).toList(),
          Map.of()));
      AudioConfig.preview(settings);AudioController.pause();
      options=MenuUpdateChecks.open(mc,"Audio");var screen=options;
      field(screen,"libraryOpen",true);
      var library=(MusicLibraryScreen)field(screen,"library");
      test("library Hide excluded filters individual songs without changing settings or queue",()->{
        library.showTrack("music:calm1.ogg");screen.render(-1,-1,0);
        check(ids(library).contains("music:calm1.ogg"),"excluded track missing before Hide");
        click(screen,(int)call(library,"hideX")+8,(int)call(library,"hideY")+8);
        check(!ids(library).contains("music:calm1.ogg")&&ids(library).contains("music:calm2.ogg"),
            "mixed group lost included song or retained exclusion");
        var state=library.state();library.restore(state);call(library,"rebuild");
        check(!ids(library).contains("music:calm1.ogg"),"view restoration lost filter");
        ((TextInput)field(library,"search")).setText(AudioController.musicLabel("music:calm1.ogg"));
        call(library,"rebuild");
        check(!ids(library).contains("music:calm1.ogg"),"search revealed excluded track");
        click(screen,(int)call(library,"hideX")+8,(int)call(library,"hideY")+8);
        check(ids(library).contains("music:calm1.ogg"),"Show excluded lost search result");
        check(AudioConfig.current().disabledTracks.equals(Set.of("music:calm1.ogg")),"filter changed inclusion");
        check(MusicRequests.tracks().equals(queue),"filter changed queue");
      });
      test("queue retains explicitly requested excluded songs",()->{
        MusicRequests.edit(q->{q.tracks.clear();q.add("music:calm1.ogg");});
        field(library,"hideExcluded",true);library.showQueue();
        check(ids(library).equals(Set.of("music:calm1.ogg")),"library filter hid a queued request");
      });
      test("preset Hide excluded filters its own draft without editing live music",()->{
        library.openPresets();var panel=field(library,"presets");
        click(screen,(int)call(panel,"right")-145,presetRowY(panel,1));
        check((boolean)call(panel,"editing"),"preset Edit missed");
        ((Set<?>)field(library,"collapsed")).clear();call(library,"rebuildRows");
        var draft=(MusicPresetDraft)call(panel,"draft");var snapshot=draft.snapshot("Filter test");
        check(!ids(library).contains("music:creative1.ogg"),"draft exclusion visible");
        check(ids(library).contains("music:calm1.ogg"),"live exclusions leaked into preset filter");
        click(screen,(int)call(panel,"hideX")+8,(int)call(panel,"editButtonY")+8);
        check(ids(library).contains("music:creative1.ogg"),"Show excluded missed draft song");
        check(draft.snapshot("Filter test").tracks().equals(snapshot.tracks()),"visibility edited draft");
        check(AudioConfig.current().disabledTracks.equals(Set.of("music:calm1.ogg")),"draft visibility edited live music");
        for(int[] size:new int[][]{{320,240},{427,240},{640,420},{854,480}}) {
          screen.init(mc,size[0],size[1]);screen.render(-1,-1,0);
          int last=0;
          for(Object row:(List<?>)field(library,"rows")) {
            int y=(int)call(row,"y"),height=(int)call(row,"height");
            check(y==last,"filtered row geometry has gaps or overlaps");last=y+height;
          }
          check(GL11.glGetError()==0,"filtered music layout GL error");
        }
      });
    } finally {
      if(options!=null)options.session().discard();
      AudioController.pause();AudioConfig.preview(before);
      MusicRequests.edit(q->{q.tracks.clear();q.tracks.addAll(queue);});
      MenuPreferences.update(v->v.autoApply=auto);mc.setScreen(null);
    }
    log("AUDIO EXCLUSIONS FAILURES "+failures);
  }

  private static Set<String> ids(MusicLibraryScreen library) throws Exception {
    Set<String> result=new HashSet<>();
    for(Object row:(List<?>)field(library,"rows")) {
      String id=(String)call(row,"id");if(id!=null)result.add(id);
    }
    return result;
  }
}
