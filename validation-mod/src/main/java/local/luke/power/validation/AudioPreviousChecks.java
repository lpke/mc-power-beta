package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import local.luke.power.audio.*;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;

/** Short silent files reach the real end-of-track path, on a muted test sink. */
final class AudioPreviousChecks {
  private static AudioSettings before;
  private static List<String> queue;
  private static float volume;
  private static String first, second;
  private static final String QUEUED = "music:calm1.ogg";

  static void run(Minecraft mc, String action) throws Exception {
    switch (action) {
      case "setup" -> {
        before = AudioConfig.copy(); queue = MusicRequests.tracks(); volume = mc.options.musicVolume;
        AudioController.quiet();
        var s = AudioConfig.copy(); s.master = 100; s.waitBetweenTracks = s.delayQueuedTracks = true;
        s.gapMinSeconds = s.gapMaxSeconds = 600;
        s.musicDirectories = List.of("power-beta-data/validation/audio-previous-152"); MusicFixtureSupport.imported(s, false);
        AudioConfig.preview(s); mc.options.musicVolume = .2F;
        MusicRequests.edit(q -> { q.tracks.clear(); q.tracks.add(QUEUED); });
      }
      case "start" -> {
        first = track("song_1.ogg"); second = track("song_2.ogg");
        AudioController.playNow(first);
        check(AudioController.currentTrackId().equals(first), "first track did not start");
      }
      case "active" -> {
        check(AudioController.currentTrackId().equals(second) && SoundManagerAccessor.power$system().playing("BgMusic"), "song_2 is not playing");
        previous(mc);
        check(AudioController.currentTrackId().equals(first), "Previous during song_2 did not select song_1");
        intactQueue(); log("PASS Previous during playback selects the preceding track");
      }
      case "second" -> AudioController.playNow(second);
      case "gap" -> {
        gap(); previous(mc);
        check(AudioController.currentTrackId().equals(second), "Previous in break2 skipped song_2");
        intactQueue(); log("PASS Previous during the natural gap replays song_2 and keeps the queue");
      }
      case "again" -> {
        previous(mc); check(AudioController.currentTrackId().equals(first), "second Previous did not step backwards");
        intactQueue(); MusicRequests.edit(q -> q.tracks.clear());
        AudioController.next(); check(AudioController.currentTrackId().equals(second), "gap replay changed forward history");
        MusicRequests.edit(q -> q.tracks.add(QUEUED));
        log("PASS repeated Previous and Next retain history order");
      }
      case "paused" -> {
        AudioController.playNow(first); AudioController.playNow(second); AudioController.pause(); previous(mc);
        check(AudioController.currentTrackId().equals(first), "paused track treated as a gap");
        intactQueue(); log("PASS Previous on a paused track retains its existing behavior");
      }
      case "preview" -> {
        AudioController.playNow(first); AudioController.playNow(second); AudioController.previewSound(first); previous(mc);
        check(!AudioController.previewing(first) && AudioController.currentTrackId().equals(first), "preview changed playback history");
        intactQueue(); log("PASS Previous during a preview retains background history");
      }
      case "gap-preview" -> {
        gap(); AudioController.previewSound(first); previous(mc);
        check(!AudioController.previewing(first) && AudioController.currentTrackId().equals(second), "preview in a gap skipped the last track");
        intactQueue(); log("PASS Previous during a gap preview replays the last background track");
      }
      case "state" -> {
        JsonObject state = new JsonObject(); state.addProperty("track", AudioController.currentTrackId());
        state.addProperty("playing", SoundManagerAccessor.power$system().playing("BgMusic"));
        state.addProperty("delay", AudioController.remainingDelay());
        Files.writeString(Path.of("power-beta-audio-previous-state.json"), state.toString());
      }
      case "finish" -> {
        AudioController.quiet(); AudioConfig.preview(before); mc.options.musicVolume = volume;
        MusicRequests.edit(q -> { q.tracks.clear(); q.tracks.addAll(queue); }); mc.setScreen(null);
        log("PASS audio test settings and queue restored");
      }
      default -> throw new IllegalArgumentException("Unknown audio-previous action " + action);
    }
  }

  private static String track(String filename) {
    return AudioController.customTracks().stream().filter(t -> t.path().getFileName().toString().equals(filename))
        .map(MusicLibrary.Track::id).findFirst().orElseThrow(() -> new AssertionError("Test track missing " + filename));
  }
  private static void gap() {
    check(AudioController.currentTrackId().isEmpty() && !AudioController.musicPlaying()
        && AudioController.remainingDelay() > 0, "not in a natural waiting period");
  }
  private static void intactQueue() { check(MusicRequests.tracks().equals(List.of(QUEUED)), "Previous consumed or changed the queue"); }
  private static void previous(Minecraft mc) throws Exception {
    PowerOptionsScreen.openMusic(mc, mc.currentScreen, false, null);
    musicClick((PowerOptionsScreen)mc.currentScreen, AudioToolbar.Action.PREVIOUS);
  }
}
