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

  private static void filter(MusicLibraryScreen library, String id) throws Exception {
    var method = MusicLibraryScreen.class.getDeclaredMethod("selectFilter", String.class);
    method.setAccessible(true);
    method.invoke(library, id);
  }

  public static void run(Minecraft mc, String action) throws Exception {
    switch (action) {
      case "setup" -> {
        failures = 0;
        before = AudioConfig.copy();
        queueBefore = MusicRequests.tracks();
        auto = MenuPreferences.current().autoApply;
        MenuPreferences.update(v -> v.autoApply = false);
        var s = new AudioSettings();
        s.musicMode = AudioSettings.MusicMode.ALL_MINECRAFT;
        s.gapMinSeconds = 120;
        s.gapMaxSeconds = 120;
        AudioConfig.preview(s);
        MusicRequests.edit(q -> q.tracks.clear());
        mc.options.musicVolume = .1f;
        AudioController.pause();
        options = MenuUpdateChecks.open(mc, "Audio");
        ((MusicLibraryScreen) field(options, "library"))
            .restore(new MusicLibraryScreen.State(false, "active", "", 0, 0));
      }
      case "ui" -> {
        var library = (MusicLibraryScreen) field(options, "library");
        musicClick(options, AudioToolbar.Action.LIBRARY);
        test(
            "Everything initially collapses all groups and custom folders sort first",
            () -> {
              filter(library, "");
              options.render(-1, -1, 0);
              check(
                  ((List<?>) field(library, "rows"))
                      .stream()
                          .allMatch(
                              r -> {
                                try {
                                  return call(r, "id") == null;
                                } catch (Exception e) {
                                  throw new RuntimeException(e);
                                }
                              }),
                  "tracks expanded by default");
            });
        test(
            "preset editor changes are isolated until explicitly loaded",
            () -> {
              library.openPresets();
              var panel = field(library, "presets");
              click(options, (int) call(panel, "left") + 20, (int) call(panel, "top") + 7);
              options.render(-1, -1, 0);
              check((boolean) call(panel, "editing"), "create missed");
              int x = (int) call(library, "right") - 10, y = (int) call(library, "listTop") + 9;
              click(options, x, y);
              check(
                  AudioConfig.current().disabledTracks.isEmpty(),
                  "draft leaked into live selection");
              ((TextInput) field(panel, "name")).setText("Validation preset");
              click(options, (int) call(panel, "saveX") + 8, (int) call(panel, "editButtonY") + 8);
              check(AudioConfig.current().presets.size() == 1, "save failed");
              check(
                  AudioConfig.current().presets.get(0).excluded().size() == 12,
                  "group draft incomplete");
              check(AudioConfig.current().disabledTracks.isEmpty(), "save loaded preset");
              library.openPresets();
              options.render(-1, -1, 0);
              click(
                  options, (int) call(panel, "right") - 25, presetRowY(panel,1));
              check(
                  AudioConfig.current().disabledTracks.size() == 12,
                  "load did not apply exclusions");
              check(!AudioConfig.current().preset.isEmpty(), "selection not stored");
              check(
                  SettingAccess.reason(
                          options.session(), find(options.session(), "audio.musicMode"))
                      .contains("preset"),
                  "mode not locked");
              check(
                  SettingAccess.lockedValue(find(options.session(), "audio.musicMode"))
                      .equals("Using preset"),
                  "wrong locked label");
              check(
                  AudioController.activeMusic(mc).size() == BuiltinMusic.TRACKS.size(),
                  "preset pool misses dimensions");
            });
        test(
            "renaming active preset preserves identity and current exclusions",
            () -> {
              String id = AudioConfig.current().preset;
              field(options, "libraryOpen", true);
              library.openPresets();
              var panel = field(library, "presets");
              click(options, (int) call(panel, "right") - 145, presetRowY(panel,1));
              ((TextInput) field(panel, "name")).setText("Renamed preset");
              click(options, (int) call(panel, "includeX") + 8, (int) call(panel, "bulkY") + 8);
              click(options, (int) call(panel, "saveX") + 8, (int) call(panel, "editButtonY") + 8);
              check(AudioConfig.current().preset.equals(id), "rename changed identity");
              check(
                  AudioConfig.current().presets.get(0).excluded().isEmpty(),
                  "editor include all failed");
              check(
                  AudioConfig.current().disabledTracks.size() == 12,
                  "editing active preset applied it");
            });
        test(
            "unsaved editor guards Back and Cancel keeps the draft",
            () -> {
              var panel = field(library, "presets");
              click(options, (int) call(panel, "left") + 20, (int) call(panel, "top") + 7);
              click(options, (int) call(options, "left") + 10, 35);
              check(library.modal(), "Back discarded draft");
              int w = Math.min(312, options.width - 20), left = (options.width - w) / 2;
              click(options, left + 2 * (w / 3) + 10, options.height / 2 + 8);
              check(!library.modal() && (boolean) call(panel, "editing"), "Cancel lost editor");
              click(options, (int) call(options, "left") + 10, 35);
              click(options, left + w / 3 + 10, options.height / 2 + 8);
              check(!(boolean) call(panel, "editing"), "Discard failed");
            });
        test(
            "favourites and library row controls are distinct and reversible",
            () -> {
              library.showTrack("music:calm1.ogg");
              options.render(-1, -1, 0);
              var row =
                  ((List<?>) field(library, "rows"))
                      .stream()
                          .filter(
                              r -> {
                                try {
                                  return "music:calm1.ogg".equals(call(r, "id"));
                                } catch (Exception e) {
                                  throw new RuntimeException(e);
                                }
                              })
                          .findFirst()
                          .orElseThrow();
              var geometry = (MusicRowLayout) call(library, "rowLayout");
              int y =
                  (int) call(library, "listTop")
                      + (int) call(row, "y")
                      - library.state().trackScroll()
                      + geometry.controlsY()
                      + 7;
              options.render(geometry.favouriteX() + 5, y, 0);
              field(options, "hoverTicks", 20);
              click(options, geometry.favouriteX() + 5, y);
              options.render(geometry.favouriteX() + 5, y, 0);
              check((int) field(options, "hoverTicks") >= 20, "favourite reset the tooltip delay");
              check(AudioConfig.current().favourites.contains("music:calm1.ogg"), "star missed");
              filter(library, "favourites");
              check(((List<?>) field(library, "tracks")).size() == 1, "favourites filter failed");
              options.session().discard();
              check(
                  AudioConfig.current().presets.isEmpty()
                      && AudioConfig.current().favourites.isEmpty()
                      && AudioConfig.current().disabledTracks.isEmpty(),
                  "Cancel left audio edits");
            });
        test(
            "all embedded audio views render at narrow and wide GUI sizes",
            () -> {
              for (int[] size : new int[][] {{320, 240}, {427, 240}, {640, 420}, {854, 480}}) {
                options.init(mc, size[0], size[1]);
                for (String filter :
                    List.of("active", "", "custom", "folder", "favourites", "presets")) {
                  library.showTracks();
                  filter(library, filter);
                  options.render(-1, -1, 0);
                  check(GL11.glGetError() == 0, "GL error " + filter);
                }
                library.openPresets();
                options.render(-1, -1, 0);
                library.openPresets();
                options.render(-1, -1, 0);
              }
              mc.setScreen(options);
              library.showTrack("music:calm1.ogg");
            });
        log("AUDIO PRESETS FAILURES " + failures);
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
        config.presets = new ArrayList<>(List.of(new MusicPreset("quiet", "Quiet afternoons",
            new TreeSet<>(BuiltinMusic.TRACKS.stream().filter(t -> !t.usage().equals("Overworld"))
                .map(BuiltinMusic.Track::id).toList()))));
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
        mc.setScreen(null);
        log("AUDIO PRESETS FAILURES " + failures);
      }
      default -> throw new IllegalArgumentException(action);
    }
  }
}
