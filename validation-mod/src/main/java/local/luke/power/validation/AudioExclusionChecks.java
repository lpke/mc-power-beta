package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.Catalog;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Library browsing never edits playlist membership; only an isolated draft owns checkboxes. */
final class AudioExclusionChecks {
  static void run(Minecraft mc) throws Exception {
    SilentAudioChecks.ready(mc);failures=0;
    try(var preserved=new MusicPlaylists460Checks.State(mc)) {
      MenuPreferences.update(v->v.autoApply=false);AudioConfig.preview(MusicPlaylists460Checks.seed());
      var options=MusicPlaylists460Checks.open(mc);var library=MusicPlaylists460Checks.library(options);
      String saved=Catalog.JSON.toJson(AudioConfig.current().playlists);
      test("Library includes nonmembers and cannot toggle saved membership",()->{
        library.showTrack("music:creative1.ogg");options.render(-1,-1,0);
        check(ids(library).contains("music:creative1.ogg"),"Library hid a world nonmember");
        click(options,(int)call(library,"right")-10,MusicGroupChecks.groupY(library,"Beta / Creative")+8);
        check(saved.equals(Catalog.JSON.toJson(AudioConfig.current().playlists)),"library header changed saved membership");
        for(int[] size:new int[][]{{320,240},{427,240},{640,420},{854,480}}) {
          options.init(mc,size[0],size[1]);options.render(-1,-1,0);int last=0;
          for(Object row:(List<?>)field(library,"rows")) {
            int y=(int)call(row,"y"),height=(int)call(row,"height");
            check(y==last,"library row gap/overlap");last=y+height;
          }
          check(GL11.glGetError()==0,"library GL error");
        }
      });
      test("queue retains explicitly requested playlist nonmembers",()->{
        MusicRequests.edit(q->{q.tracks.clear();q.add("music:creative1.ogg");});library.showQueue();
        check(ids(library).equals(Set.of("music:creative1.ogg")),"queue hid explicit nonmember");
        check(saved.equals(Catalog.JSON.toJson(AudioConfig.current().playlists)),"queue expanded saved membership");
      });
      test("playlist draft membership is independent from Library and queue",()->{
        library.showTracks();MusicPlaylists460Checks.edit(options,library,"quiet");var panel=field(library,"presets");
        var draft=(MusicPresetDraft)call(panel,"draft");var queued=MusicRequests.tracks();
        draft.include("music:creative1.ogg",true);draft.include("music:calm1.ogg",false);
        check(saved.equals(Catalog.JSON.toJson(AudioConfig.current().playlists)),"draft edited saved membership");
        check(queued.equals(MusicRequests.tracks()),"draft edited queue");
        check(draft.tracks().contains("music:playlist-fixture-missing.ogg"),"draft dropped unavailable reference");
      });
    }
    log("AUDIO EXCLUSIONS FAILURES "+failures);check(failures==0,"library membership checks failed");
  }
  private static Set<String> ids(MusicLibraryScreen library) throws Exception {
    Set<String> result=new HashSet<>();
    for(Object row:(List<?>)field(library,"rows")){String id=(String)call(row,"id");if(id!=null)result.add(id);}
    return result;
  }
}
