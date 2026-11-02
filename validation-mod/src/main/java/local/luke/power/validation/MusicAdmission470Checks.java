package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.Catalog;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import paulscode.sound.*;

/** Yield between commands so real native decoders, pause and replacement complete. */
final class MusicAdmission470Checks {
  private static final Path DIRECTORY = Path.of("admission-470"), FILE = DIRECTORY.resolve("requested.wav");
  private static final Path REPORT = Path.of("power-beta-data/reports/release-470/music-admission.json");
  private static AudioSettings previous;
  private static Object originalQueue, previousSource;
  private static List<String> expected;
  private static String id;
  private static float volume;
  private static int revision;
  private static final List<Map<String, Object>> observations = new ArrayList<>();

  static void run(Minecraft mc, String action) throws Exception {
    try {
      SilentAudioChecks.ready(mc);
      SoundSystem system = SoundManagerAccessor.power$system();
      if (action.equals("start")) {
        check(previous == null && mc.options.musicVolume == 0 && !Files.exists(DIRECTORY),
            "start from a muted disposable client without an admission fixture");
        previous = AudioConfig.copy(); volume = mc.options.musicVolume;
        originalQueue = field(MusicRequests.class, "queue").get(null);
        observations.clear();
        AudioController.quiet(); wave();
        var settings = new AudioSettings();
        settings.musicDirectories.add(DIRECTORY.toString());
        settings.waitBetweenTracks = true; settings.delayQueuedTracks = false;
        AudioConfig.preview(settings); mc.options.musicVolume = 1; AudioController.pause();
      } else {
        check(previous != null, "start the admission fixture first");
        switch (action) {
          case "arm" -> {
            var track = AudioController.customTracks().stream()
                .filter(value -> value.path().getFileName().toString().equals("requested.wav"))
                .findFirst().orElseThrow(() -> new IllegalStateException("wait for the library scan"));
            id = track.id(); revision = AudioController.libraryRevision();
            Files.delete(FILE);
            check(AudioController.resolveTrack(id) != null, "scanner metadata must survive deletion without a render-time stat");
            replaceQueue(id, id);
            AudioController.playNow("music:calm1.ogg");
          }
          case "missing" -> {
            check(system.playing("BgMusic"), "wait for old background playback");
            startAgainstOldSource(system);
          }
          case "failed" -> {
            check(MusicRequests.tracks() == expected && expected.size() == 2, "failed decoder consumed a queued row");
            check(!pending() && get(admission(), "failed") != null && AudioController.currentTrackId().isEmpty(),
                "wait at least six seconds for retained startup failure");
            Object failure = get(admission(), "failed");
            for (int i = 0; i < 20; i++) AudioController.next();
            check(get(admission(), "failed") == failure && !pending() && AudioController.currentTrackId().isEmpty(),
                "failed queue spun up another decoder without an edit or reload");
            observations.add(Map.of("case", "failed-head", "retainedRows", expected.size(), "blockedAttempts", 20));
          }
          case "reload" -> { wave(); AudioController.pause(); AudioController.reload(); }
          case "retry" -> {
            check(AudioController.libraryRevision() > revision, "wait for Reload to finish");
            check(MusicRequests.tracks() == expected, "Reload changed the queue snapshot");
            AudioController.playQueue();
          }
          case "played" -> {
            accepted(system, 1);
            startAgainstOldSource(system);
          }
          case "duplicate" -> {
            accepted(system, 0);
            check(identity() != previousSource, "same-song request reused the previous native source");
            replaceQueue(id);
            mc.options.musicVolume = 0;
            AudioController.playQueue();
            expected = MusicRequests.tracks();
          }
          case "paused" -> {
            check(MusicRequests.tracks() == expected && pending(), "paused startup expired or consumed its request");
            check(!system.playing("BgMusic"), "muted pending source played");
            mc.options.musicVolume = 1; AudioController.togglePause();
          }
          case "resumed" -> {
            accepted(system, 0);
            replaceQueue(id, id);
            synchronized (SoundSystemConfig.THREAD_SYNC) {
              AudioController.playQueue();
              MusicRequests.edit(queue -> queue.tracks.remove(0));
              expected = MusicRequests.tracks();
              poll(system);
              check(!pending() && MusicRequests.tracks() == expected, "edited duplicate row inherited old admission");
            }
          }
          case "edited" -> {
            check(system.playing("BgMusic") && MusicRequests.tracks() == expected && expected.size() == 1,
                "late playback removed the edited head");
            for (String transport : List.of("manual", "previous", "quiet", "preview")) {
              replaceQueue(id);
              synchronized (SoundSystemConfig.THREAD_SYNC) {
                String prior = AudioController.currentTrackId();
                AudioController.playQueue();
                check(pending(), "expected pending queue before " + transport);
                switch (transport) {
                  case "manual" -> AudioController.playNow("music:calm1.ogg");
                  case "previous" -> {
                    AudioController.previous();
                    check(AudioController.currentTrackId().equals(prior), "Previous skipped pending request's predecessor");
                  }
                  case "quiet" -> AudioController.quiet();
                  case "preview" -> AudioController.previewSound("music:calm2.ogg");
                }
                check(!pending() && MusicRequests.tracks() == expected, transport + " kept queue admission alive");
              }
            }
          }
          case "previewed" -> {
            check(system.playing("PowerBetaMusicPreview") && MusicRequests.tracks() == expected,
                "preview replacement consumed the abandoned request");
          }
          case "stop" -> stop(mc);
          default -> throw new IllegalArgumentException("Unknown admission action " + action);
        }
      }
      observations.add(Map.of("action", action, "result", "pass"));
      log("PASS MUSIC ADMISSION 470 " + action);
    } catch (Throwable failure) {
      observations.add(Map.of("action", action, "failure", failure.toString()));
      stop(mc);
      if (failure instanceof Exception exception) throw exception;
      throw (Error)failure;
    } finally {
      Files.createDirectories(REPORT.getParent()); Files.writeString(REPORT, Catalog.JSON.toJson(observations));
    }
  }

  private static void startAgainstOldSource(SoundSystem system) throws Exception {
    expected = MusicRequests.tracks(); previousSource = identity();
    check(previousSource != null, "native source observer did not publish the old stream");
    // Hold native commands briefly so the previous source is definitely still playing.
    synchronized (SoundSystemConfig.THREAD_SYNC) {
      AudioController.playQueue(); poll(system);
      check(MusicRequests.tracks() == expected && pending(), "old playing source acknowledged an async replacement");
    }
    observations.add(Map.of("case", "old-source-rejected", "queuedRows", expected.size()));
  }

  private static void accepted(SoundSystem system, int remaining) {
    check(system.playing("BgMusic") && AudioController.currentTrackId().equals(id), "wait for requested native playback");
    check(MusicRequests.tracks().size() == remaining && MusicRequests.tracks().stream().allMatch(id::equals),
        "native playback must consume exactly one duplicate request");
  }

  private static void replaceQueue(String... tracks) throws Exception {
    MusicRequests.edit(queue -> { queue.tracks.clear(); queue.tracks.addAll(List.of(tracks)); });
    expected = MusicRequests.tracks();
    check(expected == MusicRequests.tracks(), "queue reads allocated new snapshots");
  }

  private static Object admission() throws Exception { return field(AudioController.class, "QUEUE_ADMISSION").get(null); }
  private static boolean pending() throws Exception { return get(admission(), "pending") != null; }
  private static Object get(Object owner, String name) throws Exception { return field(owner.getClass(), name).get(owner); }
  private static Object identity() throws Exception {
    Object observation = field(MusicStream.class, "background").get(null);
    return observation == null ? null : get(observation, "source");
  }
  private static void poll(SoundSystem system) throws Exception {
    Method method = AudioController.class.getDeclaredMethod("waitingForQueued", SoundSystem.class);
    method.setAccessible(true); method.invoke(null, system);
  }
  private static Field field(Class<?> owner, String name) throws Exception {
    Field field = owner.getDeclaredField(name); field.setAccessible(true); return field;
  }

  private static void wave() throws Exception {
    Files.createDirectories(DIRECTORY);
    int bytes = 22050 * 2 * 90;
    ByteBuffer wave = ByteBuffer.allocate(44 + bytes).order(ByteOrder.LITTLE_ENDIAN);
    wave.put(new byte[] {'R','I','F','F'}).putInt(36 + bytes).put(new byte[] {'W','A','V','E','f','m','t',' '});
    wave.putInt(16).putShort((short)1).putShort((short)1).putInt(22050).putInt(44100).putShort((short)2).putShort((short)16);
    wave.put(new byte[] {'d','a','t','a'}).putInt(bytes);
    Files.write(FILE, wave.array());
  }

  private static void stop(Minecraft mc) throws Exception {
    if (previous == null) return;
    AudioController.quiet(); mc.options.musicVolume = volume;
    var original = (MusicQueue)originalQueue;
    MusicRequests.edit(queue -> { queue.tracks.clear(); queue.tracks.addAll(original.tracks); });
    PowerConfig.flushPending(); field(MusicRequests.class, "queue").set(null, originalQueue);
    AudioConfig.preview(previous); previous = null;
    Files.deleteIfExists(FILE); Files.deleteIfExists(DIRECTORY);
  }
}
