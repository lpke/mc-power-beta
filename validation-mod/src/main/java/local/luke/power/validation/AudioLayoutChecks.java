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
        rules(new MusicRules(false,false,true,true,12000,12000,1,1,1,1));
        AudioSettings next=AudioConfig.copy();next.musicDirectories=new ArrayList<>(FOLDERS);
        next.menuDirectories=new ArrayList<>(List.of("audio-layout/menu"));
        next.disabledMusicDirectories.clear();next.disabledMenuDirectories.clear();next.disabledTracks.clear();
        next.musicMode=AudioSettings.MusicMode.VANILLA;
        AudioConfig.preview(next);AudioController.pause();
      }
      case "checks" -> {
        check(AudioController.customTracks().size()==3,"scan not finished");
        // A completed settings-triggered scan refreshes these rules from the environment backend.
        rules(new MusicRules(false,false,true,true,12000,12000,1,1,1,1));
        String a=id("/a/"),b=id("/b/"),menu=id("/menu/");
        var vanilla=AudioController.activeMusic(mc);
        test("Active tracks shares soundtrack rules but retains individually excluded tracks", () -> {
          check(!vanilla.isEmpty() && vanilla.stream().noneMatch(s -> s.startsWith("music:custom/")),"Vanilla includes custom");
          var next=AudioConfig.copy();next.musicMode=AudioSettings.MusicMode.ADD;
          next.disabledTracks.add(a);AudioConfig.preview(next);
          check(AudioController.activeMusic(mc).containsAll(List.of(a,b)),"Add missing tracks");
          check(!AudioController.activeMusic(mc).contains(menu),"menu-only track included in world");
          check(AudioController.activeMusic(mc)==AudioController.activeMusic(mc),"pool not cached");
          next.musicMode=AudioSettings.MusicMode.REPLACE;AudioConfig.preview(next);
          check(AudioController.activeMusic(mc).equals(Set.of(a,b)),"Replace keeps vanilla");
          for(int i=0;i<8;i++) check(AudioController.trackId(AudioController.choose(List.of())).equals(b),"excluded track enters natural rotation");
        });
        test("folder switches exclude natural selection and preserve manual preview and queue", () -> {
          var next=AudioConfig.copy();next.disabledMusicDirectories.add(FOLDERS.get(1));AudioConfig.preview(next);
          check(AudioController.activeMusic(mc).equals(Set.of(a)),"disabled folder still active");
          check(AudioController.choose(List.of())==null,"disabled folder or excluded track selected");
          check(AudioController.resolveTrack(b)!=null,"disabled folder removed preview");
          MusicRequests.edit(q -> q.add(b));
          check(AudioController.trackId(AudioController.choose(List.of())).equals(b),"explicit queue blocked by folder");
          check(Files.mismatch(Path.of("preview-check-music/preview-test.wav"),Path.of("audio-layout/b/Track.wav"))==-1,"source track changed");
          next.disabledMusicDirectories.add(FOLDERS.get(0));AudioConfig.preview(next);
          check(AudioController.activeMusic(mc).equals(vanilla),"empty enabled folders do not fall back");
          next.disabledMusicDirectories.clear();AudioConfig.preview(next);
        });
        test("Active tracks follows menu overrides independently of world folders", () -> {
          var world=mc.world;var player=mc.player;
          try {
            mc.world=null;mc.player=null;
            check(AudioController.activeMusic(mc).equals(Set.of(menu)),"menu override pool incorrect");
            var next=AudioConfig.copy();next.disabledMenuDirectories.add("audio-layout/menu");AudioConfig.preview(next);
            check(AudioController.activeMusic(mc).equals(Set.of(a,b)),"disabled menu did not fall back to background pool");
            next.disabledMenuDirectories.clear();AudioConfig.preview(next);
          } finally {mc.world=world;mc.player=player;}
        });
        var options=MenuUpdateChecks.open(mc,"Audio");
        field(options,"library",new MusicLibraryScreen(options));
        mc.setScreen(options);
        var library=(MusicLibraryScreen)field(options,"library");
        test("new library defaults to Active and all filter modes remain reachable", () -> {
          check(library.state().folder().equals("active"),"new filter is not Active");
          var cycle=MusicLibraryScreen.class.getDeclaredMethod("cycleFolder",int.class);cycle.setAccessible(true);
          cycle.invoke(library,1);check(library.state().folder().isEmpty(),"missing All");
          cycle.invoke(library,1);check(library.state().folder().equals("custom"),"missing Custom");
          cycle.invoke(library,1);check(((List<?>)field(library,"folders")).contains(library.state().folder()),"missing folder");
          cycle.invoke(library,-1);check(library.state().folder().equals("custom"),"reverse filter broken");
        });
        test("folder toggles are local until Done and preserve rollback", () -> {
          Setting folders=find(options.session(),"audio.musicDirectories");var original=folders.value.deepCopy();
          var screen=new FolderScreen(options,folders);mc.setScreen(screen);
          click(screen,(int)call(screen,"panelLeft")+25,80);
          check(folders.value.equals(original),"toggle escaped local draft");
          click(screen,(int)call(screen,"panelRight")-70,screen.height-20);
          check(folders.value.equals(original),"Cancel changed folders");
          screen=new FolderScreen(options,folders);mc.setScreen(screen);
          click(screen,(int)call(screen,"panelLeft")+25,80);
          click(screen,(int)call(screen,"panelRight")-140,screen.height-20);
          check(AudioConfig.current().disabledMusicDirectories.contains(FOLDERS.get(0)),"Done did not preview switch");
          check(AudioConfig.current().musicDirectories.equals(FOLDERS),"switch erased path");
          check(AudioConfig.current().disabledTracks.contains(a),"switch erased track choice");
          options.session().discard();
          check(AudioConfig.current().disabledMusicDirectories.isEmpty(),"discard did not restore enabled folder");
          check(AudioConfig.current().disabledTracks.contains(a),"rollback erased track choice");
        });
        test("Queue shortcut opens directly and queue controls share one line", () -> {
          AudioController.pause();MusicRequests.edit(q -> {q.tracks.clear();q.add(a);q.add(b);});
          field(options,"libraryOpen",false);call(options,"layout");
          check(musicButton(options,AudioToolbar.Action.QUEUE).label().equals("Queue (2)"),"count missing");
          musicClick(options,AudioToolbar.Action.QUEUE);
          check(mc.currentScreen==options && library.state().queue(),"Queue shortcut opened wrong view");
          options.render(-1,-1,0);
          check((int)call(library,"rowHeight")==24,"queue still has two-line rows");
          check((int)call(library,"right")-(int)call(library,"filterX")<=82,"Clear queue stretched");
          click(options,indexed(library,"queueX",1)+3,(int)call(library,"listTop")+8);
          check(MusicRequests.tracks().equals(List.of(b,a)),"inline down arrow missed");
          options.render(-1,-1,0);
          MusicRequests.edit(q -> q.tracks.remove(0));
          click(options,indexed(library,"queueX",3)+3,(int)call(library,"listTop")+8);
          check(MusicRequests.tracks().equals(List.of(a)),"stale click removed different request");
          options.render(-1,-1,0);
          click(options,(int)call(library,"filterX")+3,(int)call(library,"controlsTop")+8);
          check(MusicRequests.tracks().isEmpty(),"Clear queue missed");
          var empty=musicButton(options,AudioToolbar.Action.QUEUE);
          check(empty.label().equals("Queue") && !empty.enabled(),"empty Queue is not visible and disabled");
        });
        test("direct view navigation preserves filters and positions without hiding empty queue", () -> {
          MusicRequests.edit(q -> {for(int i=0;i<16;i++)q.add(a);});
          library.restore(new MusicLibraryScreen.State(false,"custom","Track",24,0));
          call(library,"rebuild");
          var tracksBefore=library.state();
          musicClick(options,AudioToolbar.Action.QUEUE);library.wheel(-3);
          int queueScroll=library.state().queueScroll();
          check(queueScroll>0,"queue fixture cannot scroll");
          musicClick(options,AudioToolbar.Action.LIBRARY);
          check(!library.queueVisible() && library.state().folder().equals("custom")
              && library.state().query().equals("Track")
              && library.state().trackScroll()==tracksBefore.trackScroll(),"library state lost");
          musicClick(options,AudioToolbar.Action.QUEUE);
          check(library.state().queueScroll()==queueScroll,"queue scroll lost");
          musicClick(options,AudioToolbar.Action.SETTINGS);
          check(!(boolean)field(options,"libraryOpen"),"Back did not open settings");
          MusicRequests.edit(q -> q.tracks.clear());
          musicClick(options,AudioToolbar.Action.QUEUE);
          check(!(boolean)field(options,"libraryOpen"),"disabled Queue button navigated");
          musicClick(options,AudioToolbar.Action.LIBRARY);
          check(!library.queueVisible() && library.state().query().equals("Track"),"Library returned to queue or lost search");
          library.showQueue();
          check(!library.hasQuery(),"hidden track search leaks into queue Clear button");
          library.searchClick(0,0,1);
          check(library.state().query().equals("Track"),"queue title click erased track search");
          library.showTracks();library.clearQuery();
        });
        test("audio views toggle closed through buttons Escape and the Audio tab", () -> {
          field(options,"libraryOpen",false);call(options,"layout");
          musicClick(options,AudioToolbar.Action.LIBRARY);
          library.focus();
          ((ScreenInput)(Object)options).power$key('\0',org.lwjgl.input.Keyboard.KEY_ESCAPE);
          check(mc.currentScreen==options && !(boolean)field(options,"libraryOpen"),"Escape closed Options or stayed in library");
          musicClick(options,AudioToolbar.Action.LIBRARY);musicClick(options,AudioToolbar.Action.LIBRARY);
          check(!(boolean)field(options,"libraryOpen"),"Library did not toggle closed");
          MusicRequests.edit(q -> q.add(a));
          musicClick(options,AudioToolbar.Action.QUEUE);musicClick(options,AudioToolbar.Action.QUEUE);
          check(!(boolean)field(options,"libraryOpen"),"Queue did not toggle closed");
          musicClick(options,AudioToolbar.Action.QUEUE);MusicRequests.edit(q -> q.tracks.clear());
          musicClick(options,AudioToolbar.Action.QUEUE);
          check(!(boolean)field(options,"libraryOpen"),"empty active Queue could not close");
          musicClick(options,AudioToolbar.Action.LIBRARY);
          field(options,"sideScroll",0d);
          click(options,(int)call(options,"origin")+10,24+2*22+8);
          check(!(boolean)field(options,"libraryOpen") && field(options,"page").equals("Audio"),"Audio tab did not return to settings");
          musicClick(options,AudioToolbar.Action.LIBRARY);
        });
        test("audio toolbar queue and folders render at compact and wide sizes", () -> {
          for(int[] size : new int[][]{{320,240},{427,240},{550,380},{854,480}}) {
            options.init(mc,size[0],size[1]);options.render(-1,-1,0);
            for(var button : (List<AudioToolbar.Button>)call(options,"musicButtons"))
              check(mc.textRenderer.getWidth(button.label())<=button.width()-4,"truncated button "+button.label()+" at "+size[0]);
            library.showQueue();options.render(-1,-1,0);
            musicClick(options,AudioToolbar.Action.SETTINGS);options.render(-1,-1,0);
            musicClick(options,AudioToolbar.Action.LIBRARY);
            check(!library.queueVisible(),"Library button failed at "+size[0]);
            var folders=new FolderScreen(options,find(options.session(),"audio.musicDirectories"));
            folders.init(mc,size[0],size[1]);folders.render(-1,-1,0);
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
