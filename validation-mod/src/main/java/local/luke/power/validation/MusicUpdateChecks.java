package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import com.google.gson.JsonPrimitive;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;

public final class MusicUpdateChecks {
  private static AudioSettings before;
  private static String playing;
  private static byte[] mp3;
  private static PowerOptionsScreen parent;
  private static MusicLibraryScreen screen;

  private static String id(String folder) {
    return AudioController.customTracks().stream()
        .filter(t -> t.path().toString().contains(folder))
        .findFirst()
        .orElseThrow()
        .id();
  }

  private static Object state(String key) throws Exception {
    Field f = AudioController.class.getDeclaredField(key);
    f.setAccessible(true);
    return f.get(null);
  }

  public static void run(Minecraft mc, String action) throws Exception {
    var system = SoundManagerAccessor.power$system();
    switch (action) {
      case "setup" -> {
        failures = 0;
        before = AudioConfig.copy();
        mp3 = Files.readAllBytes(Path.of("menu-update-music/mp3/Test.mp3"));
        MusicRequests.edit(q -> q.tracks.clear());
        AudioSettings next = AudioConfig.copy();
        next.musicMode = AudioSettings.MusicMode.REPLACE;
        next.musicDirectories =
            List.of("menu-update-music/a", "menu-update-music/b", "menu-update-music/mp3");
        next.menuDirectories = List.of();
        next.master = 100;
        next.disabledTracks.clear();
        AudioConfig.preview(next);
        mc.options.musicVolume = .15f;
        mc.setScreen(new PowerOptionsScreen(null));
      }
      case "library" -> {
        test(
            "aggregate library deduplicates paths and separates same filenames",
            () -> {
              check(AudioController.customTracks().size() == 3, "wrong track count");
              check(!id("/a/").equals(id("/b/")), "duplicate filenames share settings");
              check(
                  !AudioController.customTracks().stream()
                      .filter(MusicLibrary.Track::mp3)
                      .findFirst()
                      .orElseThrow()
                      .playable(),
                  "MP3 falsely playable before conversion");
            });
        parent = (PowerOptionsScreen) mc.currentScreen;
        field(parent,"page","Audio"); field(parent,"libraryOpen",true); search(parent,"");
        screen = (MusicLibraryScreen)field(parent,"library");
        test(
            "library UI changes individual volume and rotation without saving",
            () -> {
              Setting volume = find(parent.session(), "audio.sound." + id("/a/"));
              volume.value = new JsonPrimitive(35);
              parent.changed(volume);
              check(
                  AudioController.trackVolume(id("/a/")) == 35
                      && AudioController.trackVolume(id("/b/")) == 100,
                  "track gains not independent");
              Setting enabled = find(parent.session(), "audio.trackEnabled." + id("/a/"));
              enabled.value = new JsonPrimitive(false);
              parent.changed(enabled);
              for (int i = 0; i < 8; i++)
                check(
                    AudioController.trackId(AudioController.choose(List.of())).equals(id("/b/")),
                    "excluded or unconverted track selected");
              screen.render(0, 0, 0);
            });
        AudioController.playNow(id("/b/"));
      }
      case "pause" -> {
        test(
            "Play starts custom music without leaving library",
            () ->
                check(
                    system.playing("BgMusic") && mc.currentScreen == parent,
                    "music waits for menu exit"));
        playing = (String) state("currentMusic");
        AudioSettings next = AudioConfig.copy();
        next.musicMode = AudioSettings.MusicMode.ADD;
        AudioConfig.preview(next);
      }
      case "paused" -> {
        test(
            "World music mode pauses without advancing",
            () ->
                check(
                    !system.playing("BgMusic")
                        && playing.equals(state("currentMusic"))
                        && AudioController.status().equals("Music paused"),
                    "mode skipped or failed to pause"));
        AudioController.togglePause();
      }
      case "queue" -> {
        test(
            "mode pause resumes the same track in menu",
            () ->
                check(
                    system.playing("BgMusic") && playing.equals(state("currentMusic")),
                    "resume skipped song"));
        MusicRequests.edit(
            q -> {
              q.add(id("/a/"));
              q.add(id("/b/"));
              q.add(id("/a/"));
              q.move(2, -1);
            });
        test(
            "queue keeps explicit excluded requests and saved order",
            () ->
                check(
                    MusicRequests.tracks().equals(List.of(id("/a/"), id("/a/"), id("/b/"))),
                    "queue order wrong"));
        AudioController.playQueue();
        Mp3Converter.start(
            Path.of(".").toAbsolutePath().normalize(), AudioController.customTracks());
      }
      case "converted" -> {
        test(
            "queued excluded track plays immediately and only one request is consumed",
            () ->
                check(
                    system.playing("BgMusic")
                        && state("currentMusic").equals(id("/a/"))
                        && MusicRequests.tracks().size() == 2,
                    "queue not honored"));
        test(
            "MP3 conversion preserves original and publishes playable cache",
            () -> {
              check(!Mp3Converter.busy(), "conversion still running");
              check(
                  Arrays.equals(mp3, Files.readAllBytes(Path.of("menu-update-music/mp3/Test.mp3"))),
                  "MP3 original changed");
              MusicLibrary.Track track =
                  AudioController.customTracks().stream()
                      .filter(MusicLibrary.Track::mp3)
                      .findFirst()
                      .orElseThrow();
              check(
                  track.playable()
                      && Files.exists(track.playbackPath())
                      && !track.path().equals(track.playbackPath()),
                  "MP3 cache missing: " + Mp3Converter.status());
            });
        AudioController.playNow(id("/mp3/"));
      }
      case "finish" -> {
        test(
            "converted MP3 streams while library stays open",
            () ->
                check(
                    system.playing("BgMusic")
                        && state("currentMusic").equals(id("/mp3/"))
                        && mc.currentScreen == parent,
                    "converted track failed"));
        MusicRequests.edit(q -> q.tracks.clear());
        AudioController.pause();
        AudioConfig.preview(before);
        mc.setScreen(null);
        log("MUSIC UPDATE FAILURES " + failures);
      }
    }
  }
}
