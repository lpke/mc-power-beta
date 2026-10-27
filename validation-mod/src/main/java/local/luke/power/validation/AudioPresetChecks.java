package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public final class AudioPresetChecks {
  private static AudioSettings before;
  private static List<String> queueBefore;
  private static PowerOptionsScreen options;
  private static String promised;
  private static boolean auto;
  private static float musicBefore;

  public static void run(Minecraft mc, String action) throws Exception {
    switch (action) {
      case "none" -> MusicPlaylists460Checks.run(mc, "run");
      case "setup" -> {
        SilentAudioChecks.ready(mc); failures=0; before=AudioConfig.copy(); queueBefore=MusicRequests.tracks();
        auto=MenuPreferences.current().autoApply; musicBefore=mc.options.musicVolume;
        MenuPreferences.update(v->v.autoApply=false);
        var settings=MusicPlaylists460Checks.seed();settings.gapMinSeconds=settings.gapMaxSeconds=120;
        AudioConfig.preview(settings);MusicRequests.edit(q->q.tracks.clear());
        mc.options.musicVolume=.1f;AudioController.pause();options=MusicPlaylists460Checks.open(mc);
      }
      case "ui" -> {
        MusicPlaylists460Checks.editor(mc);MusicPlaylists460Checks.libraryControls(mc);
        log("AUDIO PRESETS FAILURES "+failures);
      }
      case "editor" -> {
        var settings=MusicPlaylists460Checks.seed();AudioConfig.preview(settings);
        options=MusicPlaylists460Checks.open(mc);var library=MusicPlaylists460Checks.library(options);
        MusicPlaylists460Checks.edit(options,library,"quiet");var panel=field(library,"presets");
        test("playlist draft keeps native transport and rows usable at all sizes",()->{
          for(int[] size:new int[][]{{320,240},{427,240},{640,420},{854,480}}) {
            options.init(mc,size[0],size[1]);options.render(-1,-1,0);
            check((boolean)call(options,"musicControlsVisible"),"editor transport hidden");
            check((int)call(library,"bottom")-(int)call(library,"listTop")>=24,"editor has no room for rows");
            var tabs=(MusicFilters.Layout)call(library,"filters");
            check(tabs.tabs().stream().map(MusicFilters.Tab::id).toList().equals(List.of("library","playlists","favorites")),"editor tabs changed");
            check((int)call(panel,"top")>=tabs.tabs().get(0).y()+18,"draft header overlaps tabs");
            check(GL11.glGetError()==0,"editor GL error");
          }
        });
      }
      case "play" -> AudioController.playNow("music:calm1.ogg");
      case "pause" -> {
        check(AudioController.playingTrack("music:calm1.ogg"), "track did not start");
        AudioController.togglePause();
      }
      case "resume" -> {
        check(!AudioController.musicPlaying(), "pause left active state");
        AudioController.togglePause();
      }
      case "check-resume" ->
          test(
              "global pause/resume retains audible track identity",
              () -> {
                check(AudioController.playingTrack("music:calm1.ogg"), AudioController.status());
                check(SoundManagerAccessor.power$system().playing("BgMusic"), "source not playing");
                check(AudioController.status().startsWith("Playing: "), AudioController.status());
              });
      case "preview" -> AudioController.previewSound("music:creative4.ogg");
      case "preview-pause" -> AudioController.togglePause();
      case "preview-resume" -> AudioController.togglePause();
      case "check-preview" ->
          test(
              "global playback controls retain preview identity",
              () -> {
                check(
                    AudioController.currentTrackId().equals("music:creative4.ogg"),
                    AudioController.status());
                check(
                    SoundManagerAccessor.power$system().playing("PowerBetaMusicPreview"),
                    "preview not resumed");
              });
      case "stop-preview" -> AudioController.previewSound("music:creative4.ogg");
      case "duration" -> check(AudioController.duration() > 0, "duration not loaded");
      case "seek" -> {
        AudioController.seek(.2);
        AudioController.seek(.8);
        AudioController.seek(.6);
        check(AudioController.position() > AudioController.duration() * .59,
            "scrubber fell back to the old position while decoding");
      }
      case "check-seek" ->
          test(
              "scrub seek streams from requested position",
              () -> {
                check(
                    AudioController.position() > AudioController.duration() * .59,
                    "seek did not move");
                check(AudioController.playingTrack("music:calm1.ogg"), "seek lost identity");
                check(SoundManagerAccessor.power$system().playing("BgMusic"), "seek source failed");
              });
      case "quiet" -> {
        AudioController.quiet();
        promised = AudioController.status();
        check(promised.contains("minutes until"), promised);
      }
      case "next" -> AudioController.next();
      case "check-next" ->
          test(
              "Next plays the song named in the countdown",
              () ->
                  check(
                      promised.endsWith(AudioController.nowPlaying()),
                      promised + " vs " + AudioController.nowPlaying()));
      case "view-settings", "view-library", "view-editor", "view-presets", "view-selector", "view-queue" -> {
        var config = AudioConfig.copy();
        config.playlists = new ArrayList<>(List.of(new MusicPlaylist("quiet", "Quiet afternoons",
            BuiltinMusic.TRACKS.stream().filter(t -> t.usage().equals("Overworld"))
                .map(BuiltinMusic.Track::id).toList())));
        config.favourites.add("music:calm1.ogg");
        AudioConfig.preview(config);
        options = MenuUpdateChecks.open(mc,"Audio");
        field(options,"collapsed",new HashSet<>());call(options,"layout");
        var library=(MusicLibraryScreen)field(options,"library");
        if (!action.equals("view-settings")) field(options,"libraryOpen",true);
        switch (action) {
          case "view-library" -> library.showTrack("music:calm1.ogg");
          case "view-queue" -> {
            MusicRequests.edit(q->{q.tracks.clear();q.add("music:calm1.ogg");q.add("music:creative4.ogg");q.add("music:door.ogg");});
            library.showQueue();
          }
          case "view-presets" -> library.openPresets();
          case "view-selector" -> library.openPresets();
          case "view-editor" -> {
            library.openPresets();
            var panel=field(library,"presets");
            click(options,(int)call(panel,"right")-145,presetRowY(panel,1));
          }
        }
        options.render(-1,-1,0);
      }
      case "finish" -> {
        AudioController.pause();
        AudioConfig.preview(before);
        MusicRequests.edit(
            q -> {
              q.tracks.clear();
              q.tracks.addAll(queueBefore);
            });
        MenuPreferences.update(v -> v.autoApply = auto);
        mc.options.musicVolume = musicBefore;
        mc.setScreen(null);
        log("AUDIO PRESETS FAILURES " + failures);
      }
      default -> throw new IllegalArgumentException(action);
    }
  }
}
