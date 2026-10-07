package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import com.google.gson.JsonPrimitive;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;

public final class AudioPolishChecks {
  private static AudioSettings before;
  private static List<String> queueBefore;
  private static float volumeBefore;
  private static String a, b;
  private static PowerOptionsScreen options;

  private static String track(String name) {
    return AudioController.customTracks().stream().filter(t -> t.path().toString().contains(name))
        .findFirst().orElseThrow().id();
  }

  public static void run(Minecraft mc, String action) throws Exception {
    var system = SoundManagerAccessor.power$system();
    var sound = (SoundManagerAccessor) mc.soundManager;
    switch (action) {
      case "setup" -> {
        failures = 0;
        before = AudioConfig.copy(); queueBefore = MusicRequests.tracks(); volumeBefore = mc.options.musicVolume;
        var config = AudioConfig.copy(); config.master = 100; config.customMusic = AudioSettings.CustomMusic.ONLY;
        config.musicDirectories = List.of("audio-polish"); config.menuDirectories = List.of();
        config.disabledMusicDirectories.clear(); config.disabledMenuDirectories.clear(); config.disabledTracks.clear();
        config.waitBetweenTracks = true; config.delayQueuedTracks = true;
        MusicRequests.edit(q -> q.tracks.clear()); AudioConfig.preview(config); AudioController.pause();
        mc.options.musicVolume = .15f;
      }
      case "ui" -> {
        a = track("One.wav"); b = track("Two.wav");
        options = MenuUpdateChecks.open(mc,"General");
        test("General uses requested groups and setting order", () -> {
          field(options,"collapsed",new HashSet<>()); call(options,"layout");
          List<String> actual = new ArrayList<>();
          for (Object row : (List<?>)field(options,"rows")) {
            Setting s = (Setting)call(row,"setting"); actual.add(s == null ? (String)call(row,"group") : s.id);
          }
          check(actual.equals(List.of("Game","world.difficulty","world.cheats","power_controls:general.autosaveInterval",
              "Interface","native.guiScale","visual.slashChat","power_controls:general.pauseOnLostFocus","interface.pauseToOptions",
              "Input","native.sensitivity","native.invert","power_controls:general.rawInput","power_controls:general.disableControllerInit")), actual.toString());
        });
        test("locked cheat values show outcome and retain stored preferences", () -> {
          var cheats=find(options.session(),"world.cheats"); var previous=cheats.value; cheats.value=new JsonPrimitive(false);
          var value=PowerOptionsScreen.class.getDeclaredMethod("value",Setting.class);value.setAccessible(true);
          for (Setting s : options.session().settings()) if (SettingAccess.cheat(s)) {
            var stored=s.value.deepCopy();String display=(String)value.invoke(options,s);
            check(display.equals(s.kind==Setting.Kind.BOOLEAN?"Off":"Disabled"),s.id+" displays "+display);
            check(stored.equals(s.value),"display altered "+s.id);
          }
          cheats.value=previous;
        });
        field(options,"page","Audio"); search(options,"");
        MusicRequests.edit(q -> {q.tracks.clear();q.add(a);q.add(b);});
        musicClick(options,AudioToolbar.Action.QUEUE);
        var library=(MusicLibraryScreen)field(options,"library"); options.render(-1,-1,0);
        test("queue volume controls change gain without consuming requests", () -> {
          click(options,(int)call(library,"volumeX")+4,(int)call(library,"listTop")+8);
          library.removed();
          check(AudioController.trackVolume(a)==0,"queue slider did not apply");
          check(MusicRequests.tracks().equals(List.of(a,b)),"volume modified queue");
          options.session().discard();
        });
        AudioController.playNow(b);
        test("playing status opens All tracks and locates song", () -> {
          click(options,(int)call(options,"left")+2,(int)call(options,"audioContentTop")+4);
          check(!library.queueVisible() && library.state().folder().isEmpty(),"wrong view");
          var rows=(List<?>)field(library,"rows");
          Object match=rows.stream().filter(r -> {try{return b.equals(call(r,"id"));}catch(Exception e){throw new RuntimeException(e);}}).findFirst().orElseThrow();
          int offset=(int)call(match,"y")-library.state().trackScroll();
          check(offset>=0 && offset<(int)call(library,"bottom")-(int)call(library,"listTop"),"track outside view");
          check((int)call(options,"searchLeft")==(int)call(options,"left")+48,"Back not beside search");
          click(options,(int)call(options,"left")+3,32);
          check(!(boolean)field(options,"libraryOpen"),"Back did not return to settings");
        });
        MusicRequests.edit(q -> {q.tracks.clear();q.add(a);});
        var config=AudioConfig.copy();config.waitBetweenTracks=true;config.delayQueuedTracks=true;AudioConfig.preview(config);
        AudioController.quiet(); sound.power$countdown(2400);
        test("Quiet keeps automatic music enabled with readable countdown", () -> {
          check(AudioController.status().contains("sec until next track"),AudioController.status());
          check(MusicRequests.tracks().equals(List.of(a)),"Quiet consumed queue");
        });
      }
      case "quiet" -> {
        test("Quiet silences async playback while waiting", () -> {
          check(!system.playing("BgMusic"),"Quiet still playing");
          check(AudioController.remainingDelay()>0 && AudioController.remainingDelay()<2400,"countdown did not tick in Options");
          check(MusicRequests.tracks().equals(List.of(a)),"delayed queue consumed early");
        });
        var config=AudioConfig.copy();config.waitBetweenTracks=false;AudioConfig.preview(config);
      }
      case "queue" -> {
        test("wait off starts queued track without exiting Options", () -> {
          check(system.playing("BgMusic") && AudioController.playingTrack(a),"queued music did not start");
          check(MusicRequests.tracks().isEmpty(),"queue did not advance");
          check(mc.currentScreen==options,"screen changed");
        });
        AudioController.quiet();
      }
      case "natural" -> {
        test("wait off starts natural music back to back", () -> check(system.playing("BgMusic"),"natural music waited"));
        AudioController.pause();
      }
      case "paused" -> {
        test("Pause blocks auto-play even with wait disabled", () -> {
          check(!system.playing("BgMusic") && AudioController.status().matches("Music paused|Paused: .+"),"pause resumed");
        });
        var config=AudioConfig.copy();config.waitBetweenTracks=true;config.delayQueuedTracks=false;AudioConfig.preview(config);
        MusicRequests.edit(q -> q.add(b));AudioController.quiet();sound.power$countdown(2400);
      }
      case "immediate-queue" -> {
        test("queue delay off bypasses normal delay", () -> {
          check(system.playing("BgMusic") && AudioController.playingTrack(b),"queue waited despite delay off");
          check(MusicRequests.tracks().isEmpty(),"queue request not consumed");
        });
        AudioController.pause();
        MusicRequests.edit(q -> {q.tracks.clear();q.add("music:custom/missing-test");});
        AudioController.quiet();
      }
      case "missing" -> {
        test("unavailable queue request is retained with useful status", () -> {
          check(MusicRequests.tracks().equals(List.of("music:custom/missing-test")),"missing request discarded");
          check(AudioController.status().contains("unavailable"),AudioController.status());
        });
        log("AUDIO POLISH FAILURES "+failures);
      }
      case "end-start" -> {
        var config=AudioConfig.copy();config.waitBetweenTracks=false;AudioConfig.preview(config);
        MusicRequests.edit(q -> {q.tracks.clear();q.add(b);});
        AudioController.playNow(track("Short.wav"));
      }
      case "end-check" -> {
        test("natural end of decoded track advances queue in Options", () -> {
          check(system.playing("BgMusic") && AudioController.playingTrack(b),"queue did not follow completed track");
          check(MusicRequests.tracks().isEmpty(),"request remains after natural end");
          check(mc.currentScreen==options,"menu changed");
        });
        log("AUDIO POLISH FAILURES "+failures);
      }
      case "finish" -> {
        AudioController.pause(); AudioConfig.preview(before); mc.options.musicVolume=volumeBefore;
        MusicRequests.edit(q -> {q.tracks.clear();q.tracks.addAll(queueBefore);}); mc.setScreen(null);
      }
    }
  }
}
