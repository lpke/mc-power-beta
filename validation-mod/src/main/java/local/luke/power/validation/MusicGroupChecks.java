package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Real embedded-panel clicks exercise batching, filtering and draft rollback. */
final class MusicGroupChecks {
  static int groupY(MusicLibraryScreen library, String group) throws Exception {
    for (Object row : (List<?>)field(library,"rows")) {
      if (call(row,"id") == null && group.equals(call(row,"group"))) {
        int y=(int)call(row,"y");
        var scroll=MusicLibraryScreen.class.getDeclaredMethod("scroll",int.class);
        scroll.setAccessible(true);scroll.invoke(library,y);
        return (int)call(library,"listTop")+y-library.state().trackScroll();
      }
    }
    throw new AssertionError("Missing group: "+group);
  }

  static void run(Minecraft mc, PowerOptionsScreen options, MusicLibraryScreen library) throws Exception {
    var saved=local.luke.power.config.Catalog.JSON.toJson(AudioConfig.current().playlists);
    var queue=MusicRequests.tracks();
    test("group draft edits retain other groups, gains and unavailable references",()->{
      var settings=MusicPlaylists460Checks.seed();
      var draft=new MusicPresetDraft(settings.playlists.get(0),BuiltinMusic.TRACKS.stream().map(BuiltinMusic.Track::id).toList());
      for(var track:BuiltinMusic.TRACKS)if(track.group().equals("Beta / Creative"))draft.include(track.id(),true);
      for(var track:BuiltinMusic.TRACKS)if(track.group().equals("Alpha"))draft.include(track.id(),false);
      check(draft.tracks().contains("music:playlist-fixture-missing.ogg"),"group edit dropped missing ID");
      check(draft.tracks().containsAll(BuiltinMusic.TRACKS.stream().filter(t->t.group().equals("Beta / Creative")).map(BuiltinMusic.Track::id).toList()),"creative group incomplete");
      check(draft.volume("Alpha")==40,"group inclusion changed gain");
      check(saved.equals(local.luke.power.config.Catalog.JSON.toJson(AudioConfig.current().playlists)),"draft changed saved list");
      check(MusicRequests.tracks().equals(queue),"draft changed queue");
    });
  }
}
