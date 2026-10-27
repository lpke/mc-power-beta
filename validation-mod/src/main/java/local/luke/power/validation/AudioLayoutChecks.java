package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public final class AudioLayoutChecks {
  private static AudioSettings before;
  private static MusicRules rulesBefore;
  private static List<String> queueBefore;
  private static final List<String> FOLDERS = List.of("audio-layout/a", "audio-layout/b");

  private static void rules(MusicRules value) throws Exception {
    var field = AudioController.class.getDeclaredField("rules");
    field.setAccessible(true);field.set(null,value);
  }

  private static String id(String folder) {
    return AudioController.customTracks().stream().filter(t -> t.path().toString().contains(folder))
        .findFirst().orElseThrow().id();
  }

  private static void click(Object screen, int x, int y) {
    ((ScreenInput) screen).power$click(x,y,0);
  }

  private static int indexed(Object screen, String method, int index) throws Exception {
    var m = screen.getClass().getDeclaredMethod(method,int.class);m.setAccessible(true);
    return (int)m.invoke(screen,index);
  }

  public static void run(Minecraft mc, String action) throws Exception {
    switch (action) {
      case "setup" -> {
        failures=0;
        before=AudioConfig.copy();rulesBefore=AudioController.rules();queueBefore=MusicRequests.tracks();
        for (String folder : List.of("audio-layout/a", "audio-layout/b", "audio-layout/menu")) {
          Path path=Path.of(folder);Files.createDirectories(path);
          Files.copy(Path.of("preview-check-music/preview-test.wav"),path.resolve("Track.wav"),StandardCopyOption.REPLACE_EXISTING);
        }
        MenuPreferences.update(v -> v.autoApply=false);
        MusicRequests.edit(q -> q.tracks.clear());
        rules(new MusicRules(12000,12000,1,1,1,1));
        AudioSettings next=AudioConfig.copy();next.musicDirectories=new ArrayList<>(List.of("audio-layout/a", "audio-layout/b", "audio-layout/menu"));

        MusicPlaylist.load(next, MusicPlaylist.BETA);
        AudioConfig.preview(next);AudioController.pause();
      }
      case "checks" -> {
        check(AudioController.customTracks().size()==3,"scan not finished");
        // A completed settings-triggered scan refreshes these rules from the environment backend.
        rules(new MusicRules(12000,12000,1,1,1,1));
        String a=id("/a/"),b=id("/b/"),menu=id("/menu/");
        test("selected saved tracks stay ordered and library membership remains independent", () -> {
          var next=AudioConfig.copy();MusicFixtureSupport.selection(next,List.of(b));AudioConfig.preview(next);
          check(new ArrayList<>(AudioController.activeMusic(mc)).equals(List.of(b)),"saved imported membership ignored");
          check(AudioController.resolveTrack(a)!=null,"nonmember unavailable for manual playback");
          MusicRequests.edit(q->q.add(a));
          check(AudioController.trackId(AudioController.choose()).equals(a),"explicit nonmember request blocked");
          check(MusicFixtureSupport.tracks(AudioConfig.current()).equals(List.of(b)),"queue expanded saved playlist");
          check(Files.mismatch(Path.of("preview-check-music/preview-test.wav"),Path.of("audio-layout/b/Track.wav"))==-1,"source track changed");
        });
        test("world and menu saved memberships are independent in the shared import library", () -> {
          var next=AudioConfig.copy();
          next.playlists.add(new MusicPlaylist("layout-menu","Menu fixture",List.of(menu)));
          MusicPlaylist.loadMenu(next,"layout-menu");AudioConfig.preview(next);
          check(MusicFixtureSupport.menuTracks(next).equals(List.of(menu)),"menu saved membership lost");
          check(MusicFixtureSupport.tracks(next).equals(List.of(b)),"menu assignment changed world membership");
        });
        var options=MenuUpdateChecks.open(mc,"Audio");
        field(options,"library",new MusicLibraryScreen(options));
        mc.setScreen(options);
        var library=(MusicLibraryScreen)field(options,"library");
        test("new library has fixed Library Playlists Favorites tabs", () -> {
          library.showTracks();
          var tabs=(MusicFilters.Layout)call(library,"filters");
          check(tabs.tabs().stream().map(MusicFilters.Tab::id).toList().equals(List.of("library","playlists","favorites")),"editor tabs missing");
        });
        test("Library source Remove confirmation and Options discard preserve folders", () -> {
          library.showTracks();
          MusicFolderChecks.sourceClick(options, library, FOLDERS.get(0), "sourceRemoveX");
          check(library.modal(), "Remove skipped confirmation");
          ((ScreenInput)(Object)options).power$key('\0',org.lwjgl.input.Keyboard.KEY_ESCAPE);
          check(AudioConfig.current().musicDirectories.contains(FOLDERS.get(0)), "Remove cancel changed source");
          options.session().discard();
        });
        test("Queue shortcut opens directly and queue controls share one line", () -> {
          AudioController.pause();MusicRequests.edit(q -> {q.tracks.clear();q.add(a);q.add(b);});
          field(options,"libraryOpen",false);call(options,"layout");
          check(musicButton(options,AudioToolbar.Action.QUEUE).label().equals("Queue (2)"),"count missing");
          musicClick(options,AudioToolbar.Action.QUEUE);
          check(mc.currentScreen==options && library.state().queue(),"Queue shortcut opened wrong view");
          options.render(-1,-1,0);
          check((int)call(library,"rowHeight")==24,"queue is not one line");
          int x=(int)call(library,"left")+22, top=(int)call(library,"listTop");
          ((ScreenInput)(Object)options).power$click(x,top+8,0);
          ((ScreenInput)(Object)options).power$release(x,top+46,0);
          check(MusicRequests.tracks().equals(List.of(b,a)),"queue row drag missed");
          MusicPlaylists460Checks.context(options,library,b,List.of("Add to queue","Favourite","Preview","Remove from queue"));
          MusicRequests.edit(q -> q.tracks.remove(0));
          MusicPlaylists460Checks.choose(options,3);
          check(MusicRequests.tracks().equals(List.of(a)),"stale context removed different request");
          call(library,"refresh");
          click(options,(int)call(library,"right")-79,(int)call(library,"controlsTop")+8);
          check(MusicRequests.tracks().isEmpty(),"Clear queue missed");
          check(((List<AudioToolbar.Button>)call(options,"musicButtons")).stream()
              .noneMatch(v -> v.action()==AudioToolbar.Action.QUEUE),"empty Queue remains visible");
        });
        test("direct view navigation preserves filters and positions without hiding empty queue", () -> {
          MusicRequests.edit(q -> {for(int i=0;i<16;i++)q.add(a);});
          library.restore(new MusicLibraryScreen.State(false,"library","Track",24,0));
          call(library,"rebuild");
          var tracksBefore=library.state();
          musicClick(options,AudioToolbar.Action.QUEUE);library.wheel(-3);
          int queueScroll=library.state().queueScroll();
          check(queueScroll>0,"queue fixture cannot scroll");
          musicEditor(options);
          check(!library.queueVisible() && library.state().folder().equals("library")
              && library.state().query().equals("Track")
              && library.state().trackScroll()==tracksBefore.trackScroll(),"library state lost");
          musicClick(options,AudioToolbar.Action.QUEUE);
          check(library.state().queueScroll()==queueScroll,"queue scroll lost");
          click(options,(int)call(options,"left")+3,32);
          check(!(boolean)field(options,"libraryOpen"),"Back did not open settings");
          MusicRequests.edit(q -> q.tracks.clear());
          check(((List<AudioToolbar.Button>)call(options,"musicButtons")).stream()
              .noneMatch(v -> v.action()==AudioToolbar.Action.QUEUE),"empty Queue remains visible");
          musicEditor(options);
          check(!library.queueVisible() && library.state().query().equals("Track"),"Library returned to queue or lost search");
          library.showQueue();
          check(!library.hasQuery(),"hidden track search leaks into queue Clear button");
          library.searchClick(0,0,1);
          check(library.state().query().equals("Track"),"queue title click erased track search");
          library.showTracks();library.clearQuery();
        });
        test("audio views toggle closed through buttons Escape and the Audio tab", () -> {
          field(options,"libraryOpen",false);call(options,"layout");
          musicEditor(options);
          library.focus();
          ((ScreenInput)(Object)options).power$key('\0',org.lwjgl.input.Keyboard.KEY_ESCAPE);
          check(mc.currentScreen==options && !(boolean)field(options,"libraryOpen"),"Escape closed Options or stayed in library");
          musicEditor(options);musicEditor(options);
          check(!(boolean)field(options,"libraryOpen"),"Library did not toggle closed");
          MusicRequests.edit(q -> q.add(a));
          musicClick(options,AudioToolbar.Action.QUEUE);musicClick(options,AudioToolbar.Action.QUEUE);
          check(!(boolean)field(options,"libraryOpen"),"Queue did not toggle closed");
          musicClick(options,AudioToolbar.Action.QUEUE);MusicRequests.edit(q -> q.tracks.clear());
          click(options,(int)call(options,"left")+3,32);
          check(!(boolean)field(options,"libraryOpen"),"empty active Queue could not close via Back");
          musicEditor(options);
          field(options,"sideScroll",0d);
          click(options,(int)call(options,"origin")+10,24+2*22+8);
          check(!(boolean)field(options,"libraryOpen") && field(options,"page").equals("Audio"),"Audio tab did not return to settings");
          musicEditor(options);
        });
        test("audio toolbar queue and folders render at compact and wide sizes", () -> {
          for(int[] size : new int[][]{{320,240},{427,240},{550,380},{854,480}}) {
            options.init(mc,size[0],size[1]);options.render(-1,-1,0);
            for(var button : (List<AudioToolbar.Button>)call(options,"musicButtons"))
              check(mc.textRenderer.getWidth(button.label())<=button.width()-4,"truncated button "+button.label()+" at "+size[0]);
            library.showQueue();options.render(-1,-1,0);
            click(options,(int)call(options,"left")+3,32);options.render(-1,-1,0);
            musicEditor(options);
            check(!library.queueVisible(),"Library button failed at "+size[0]);
            var picker=new DirectoryScreen(options,values -> {},List.of());
            picker.init(mc,size[0],size[1]);picker.render(-1,-1,0);picker.removed();
            check(GL11.glGetError()==0,"OpenGL error at "+size[0]);
          }
          mc.setScreen(options);
        });
        log("AUDIO LAYOUT FAILURES "+failures);
      }
      case "finish" -> {
        AudioConfig.preview(before);rules(rulesBefore);
        MusicRequests.edit(q -> {q.tracks.clear();q.tracks.addAll(queueBefore);});
        AudioController.pause();mc.setScreen(null);
      }
    }
  }
}
