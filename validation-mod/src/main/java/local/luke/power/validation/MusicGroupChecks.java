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
  static void run(Minecraft mc, PowerOptionsScreen options, MusicLibraryScreen library) throws Exception {
    var before = AudioConfig.copy();
    var queue = MusicRequests.tracks();
    test("group toggle includes hidden matches, preserves other eras, volumes and queue", () -> {
      library.restore(new MusicLibraryScreen.State(false, "", "aria", 0, 0));
      call(library, "rebuild"); options.render(-1, -1, 0);
      int x = (int) call(library, "right") - 10, y = (int) call(library, "listTop") + 9;
      click(options, x, y);
      check(AudioConfig.current().disabledTracks.size() == 6, "filtered bulk edit missed hidden creative tracks");
      for (var track : BuiltinMusic.TRACKS)
        check(AudioConfig.current().disabledTracks.contains(track.id()) == track.creative(), track.file());
      check(AudioConfig.current().sounds.equals(before.sounds), "group edited volumes");
      check(MusicRequests.tracks().equals(queue), "group edited queue");
      check(library.state().collapsed().isEmpty(), "toggle collapsed heading");
      // An individual edit produces a mixed group; bulk click includes all members.
      Setting aria = find(options.session(), "audio.trackEnabled.music:creative4.ogg");
      aria.cycle(1); options.changed(aria);
      options.render(-1, -1, 0); click(options, x, y);
      check(AudioConfig.current().disabledTracks.isEmpty(), "mixed group did not include all");
    });
    test("collapsed group toggle works at all supported UI widths and can be discarded", () -> {
      library.restore(new MusicLibraryScreen.State(false, "", "", 0, 0, "", Set.copyOf(BuiltinMusic.GROUPS)));
      call(library, "rebuild");
      for (int[] size : new int[][]{{320,240},{427,240},{640,420},{854,480}}) {
        options.init(mc,size[0],size[1]); options.render(-1,-1,0);
        int x=(int)call(library,"right")-10, y=(int)call(library,"listTop")+9;
        click(options,x,y);
        check(AudioConfig.current().disabledTracks.size()==12,"collapsed Alpha edit failed");
        check(library.state().collapsed().contains("Alpha"),"group unexpectedly expanded");
        check(GL11.glGetError()==0,"group rendering GL error");
        click(options,x,y);
      }
      click(options,(int)call(library,"right")-10,(int)call(library,"listTop")+9);
      options.session().discard();
      check(AudioConfig.current().disabledTracks.equals(before.disabledTracks),"Cancel left group exclusions");
      mc.setScreen(options);
      library.restore(new MusicLibraryScreen.State(false,"","",0,0)); call(library,"rebuild");
    });
  }
}
