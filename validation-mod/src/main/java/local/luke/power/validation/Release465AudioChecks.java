package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.*;
import java.nio.IntBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import local.luke.power.audio.*;
import local.luke.power.config.Catalog;
import local.luke.power.config.NativeStorage;
import local.luke.power.mixin.SoundManagerAccessor;
import net.minecraft.client.Minecraft;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import paulscode.sound.*;

/** Staged real OpenAL checks. Commands yield to the game and sound threads between observations. */
final class Release465AudioChecks {
  private static final String FIRST = "music:calm1.ogg", SECOND = "music:calm2.ogg", THIRD = "music:calm3.ogg";
  private static final String PLAYLIST = "audio465", PREVIEW = "PowerBetaMusicPreview";
  private static final List<String> OWNED = List.of("BgMusic", "PowerBetaMenu", PREVIEW);
  private static final Path REPORT = Path.of("power-beta-data/reports/release-465/audio.json");
  private static final List<Map<String, Object>> observations = new ArrayList<>();
  private static final Class<?> AUDITION;
  static { try { AUDITION = Class.forName("local.luke.power.audio.MusicPreview"); }
    catch (ClassNotFoundException failure) { throw new ExceptionInInitializerError(failure); } }
  private static Session active;

  private Release465AudioChecks() {}

  static void run(Minecraft mc, String action) throws Exception {
    try {
      if (action.equals("start")) {
        check(active == null, "finish the existing release-465-audio session first");
        SilentAudioChecks.ready(mc);
        check(mc.world != null && mc.player != null && !mc.world.isRemote, "open a disposable singleplayer world");
        check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "start with the disposable client muted");
        check(AudioController.currentTrackId().isEmpty() && !MusicSeeking.busy(), "Quiet music and finish any pending seek first");
        SoundSystem system = SoundManagerAccessor.power$system();
        for (String source : List.of("BgMusic", "PowerBetaMenu", PREVIEW, "streaming")) {
          Source nativeSource = source(system, source);
          check(nativeSource == null || !nativeSource.playing() && !nativeSource.paused(), "finish the existing source " + source);
        }
        check(get(AudioController.class, "pending") == null, "wait for the music folder scan before starting");
        observations.clear(); active = new Session(mc, system); active.start();
      } else {
        check(active != null, "run release-465-audio start first");
        check(active.mc == mc, "native client changed during the fixture");
        active.diagnostics("before", action);
        active.command(action);
        active.diagnostics("after", action);
        if (action.equals("finish")) active = null;
      }
      observe("pass", action);
      log("PASS RELEASE465 AUDIO " + action + " stage=" + (active == null ? "restored" : active.stage));
    } catch (Throwable failure) {
      observe("failure", action + ": " + failure);
      if (active != null) {
        try { active.diagnostics("failure", action); } catch (Throwable diagnostic) { failure.addSuppressed(diagnostic); }
        try { active.close(); } catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
        active = null;
      }
      if (failure instanceof Exception exception) throw exception;
      if (failure instanceof Error error) throw error;
      throw new RuntimeException(failure);
    } finally {
      Files.createDirectories(REPORT.getParent());
      Files.writeString(REPORT, Catalog.JSON.toJson(Map.of("stage", active == null ? "restored" : active.stage,
          "clockBasis", "MusicProgress accumulates native playing time and seek offset; OpenAL offset is relative to its streaming queue",
          "observations", observations)));
    }
  }

  private static void observe(String result, String action) {
    observations.add(Map.of("result", result, "action", action, "timeNanos", System.nanoTime()));
  }

  private static AudioSettings settings() {
    AudioSettings settings = new AudioSettings();
    settings.playlists.add(new MusicPlaylist(PLAYLIST, "Audio transport fixture", List.of(FIRST, SECOND, THIRD)));
    settings.playlist = PLAYLIST; settings.menuPlaylist = MusicPlaylist.SAME_AS_WORLD;
    settings.shuffle = false; settings.avoidRepeats = false;
    settings.waitBetweenTracks = settings.delayQueuedTracks = true;
    settings.gapMinSeconds = settings.gapMaxSeconds = 600;
    return settings;
  }

  private static final class Session implements AutoCloseable {
    final Minecraft mc;
    final SoundSystem system;
    final MusicPlaylists460Checks.State files;
    final Snapshot controller, audition, audio, backgroundClock, auditionClock;
    final TrackSelector savedSelector;
    final float music, sound;
    final int countdown, backgroundPreloads, auditionPreloads;
    final Object display;
    String stage = "new", previousMode = "";
    NativePosition background, preview;
    Object seekSource, previousSource, reservation;
    Playback playback;
    List<?> restoreSources;
    double pausedPosition, target;

    Session(Minecraft mc, SoundSystem system) throws Exception {
      this.mc = mc; this.system = system;
      files = new MusicPlaylists460Checks.State(mc);
      controller = new Snapshot(AudioController.class, null); audition = new Snapshot(AUDITION, null);
      audio = new Snapshot(AudioConfig.class, null);
      backgroundClock = new Snapshot(MusicProgress.class, get(AudioController.class, "progress"));
      auditionClock = new Snapshot(MusicProgress.class, get(AUDITION, "progress"));
      savedSelector = Catalog.JSON.fromJson(Catalog.JSON.toJson(get(AudioController.class, "WORLD")), TrackSelector.class);
      music = mc.options.musicVolume; sound = mc.options.soundVolume;
      countdown = ((SoundManagerAccessor)mc.soundManager).power$countdown();
      backgroundPreloads = preloads("BgMusic"); auditionPreloads = preloads(PREVIEW);
      display = get(Class.forName("local.luke.power.environment.ModHelper$ModHelperFields"), "currentBGM");
    }

    void start() throws Exception {
      NativeStorage.preview(() -> { mc.options.musicVolume = .2f; mc.options.soundVolume = 0; mc.soundManager.method_2008(); });
      reset();
      stage = "native-running";
    }

    void command(String action) throws Exception {
      switch (action) {
        case "mute-native" -> {
          expect("native-running"); running("BgMusic");
          background = NativePosition.read(system, "BgMusic");
          check(background.clockSeconds > .25, "allow native playback to advance the cumulative song clock before muting");
          playback = Playback.read(); pausedPosition = AudioController.position();
          nativeVolume(0);
          check(AudioController.status().equals("Music muted") && paused(), "native Music=0 did not latch muted pause");
          stage = "native-muted";
        }
        case "unmute-native" -> {
          expect("native-muted"); background = retained(background, false);
          check(background.clockSeconds > .25, "native Music=0 reset the cumulative song clock");
          check(Math.abs(AudioController.position() - pausedPosition) < .15, "native mute changed the song clock");
          playback.unchanged(); nativeVolume(.2f); stage = "native-unmuted";
        }
        case "resume-native" -> {
          expect("native-unmuted"); retained(background, true); playback.unchanged();
          check(paused() && AudioController.status().startsWith("Paused:"), "native unmute resumed music");
          AudioController.togglePause(); stage = "native-resumed";
        }
        case "preview-start" -> {
          expect("native-resumed"); resumed(background);
          AudioController.previewSound(THIRD); stage = "preview-running";
        }
        case "mute-master" -> {
          expect("preview-running"); running(PREVIEW);
          background = NativePosition.read(system, "BgMusic"); preview = NativePosition.read(system, PREVIEW);
          check(background.source.paused(), "audition did not own the background pause");
          check(preview.clockSeconds > .25, "allow native playback to advance the cumulative audition clock before muting");
          playback = Playback.read(); master(0);
          check(AudioController.status().equals("Music muted") && paused() && AudioController.previewPaused(), "Master=0 did not pause both owners");
          stage = "master-muted";
        }
        case "unmute-master" -> {
          expect("master-muted"); background = retained(background, true); preview = retained(preview, false);
          playback.unchanged(); master(100); stage = "master-unmuted";
        }
        case "resume-preview" -> {
          expect("master-unmuted"); retained(background, true); retained(preview, true); playback.unchanged();
          check(paused() && AudioController.previewPaused() && AudioController.status().startsWith("Paused:"), "Master preview rollback resumed a source");
          AudioController.togglePause(); stage = "preview-resumed";
        }
        case "end-preview" -> {
          expect("preview-resumed"); resumed(preview); retained(background, true);
          stopPreview(true); stage = "preview-ended";
        }
        case "resume-background" -> {
          expect("preview-ended"); retained(background, true);
          check(AudioController.currentTrackId().equals(SECOND) && paused(), "ending audition lost the muted background pause");
          AudioController.togglePause(); stage = "background-resumed";
        }
        case "seek-start" -> {
          expect("background-resumed"); running("BgMusic");
          seekSource = source(system, "BgMusic"); playback = Playback.read();
          request(SECOND, "BgMusic", 45); check(MusicSeeking.busy(), "seek did not queue");
          // Both previews run in one native tick, before it can accept the prepared codec.
          master(0); master(100); check(paused(), "rapid Master preview rollback cleared pause");
          stage = "seek-preparing";
        }
        case "resume-seek" -> {
          expect("seek-preparing"); prepared("BgMusic", 45);
          Source replacement = source(system, "BgMusic");
          check(replacement != seekSource && replacement.stopped() && !replacement.playing(), "prepared seek must create an idle replacement source");
          check(paused(), "seek completion or unmute cleared pause"); playback.unchanged();
          seekSource = replacement; AudioController.togglePause(); stage = "seek-resumed";
        }
        case "check-seek" -> {
          expect("seek-resumed"); running("BgMusic");
          check(source(system, "BgMusic") == seekSource && AudioController.position() > 45.05, "manual resume did not retain prepared seek offset");
          observe("native-seek", "position=" + AudioController.position()); stage = "seek-complete";
        }
        case "previous-preview" -> prepareAuditionThreshold();
        case "previous-press" -> pressPrevious();
        case "previous-result" -> previousResult();
        case "previous-restore" -> {
          expect("previous-audition-complete"); stopPreview(true);
          check(source(system, "BgMusic") == seekSource && paused(), "ending restarted audition replaced/resumed its paused background");
          check(AudioController.currentTrackId().equals(SECOND) && AudioController.position() == 45, "ending restarted audition lost its prepared background offset");
          playback.unchanged(); AudioController.togglePause(); stage = "previous-restoring";
        }
        case "previous-restored" -> {
          expect("previous-restoring"); running("BgMusic");
          check(source(system, "BgMusic") == seekSource && AudioController.position() > 45.05, "audition restart invalidated the retained prepared background stream");
          check(NativePosition.read(system, "BgMusic").state == AL10.AL_PLAYING, "restored native background did not reach OpenAL playback");
          playback.unchanged(); stage = "previous-complete";
        }
        case "stop" -> { cleanup(); stage = "stopping"; }
        case "finish" -> {
          expect("stopping");
          for (String source : OWNED) check(source(system, source) == null, "wait for owned source removal before finish: " + source);
          close();
        }
        default -> {
          if (action.startsWith("previous ")) preparePrevious(action.substring(9).trim());
          else throw new IllegalArgumentException("Unknown release-465-audio action " + action);
        }
      }
    }

    private void nativeVolume(float volume) {
      NativeStorage.preview(() -> { mc.options.musicVolume = volume; mc.soundManager.method_2008(); });
      AudioController.refresh();
    }
    private void master(int volume) { AudioSettings next = AudioConfig.copy(); next.master = volume; AudioConfig.preview(next); }
    private boolean paused() throws Exception { return (boolean)get(AudioController.class, "paused"); }
    private void expect(String expected) { check(stage.equals(expected), "expected stage " + expected + ", found " + stage); }
    private void running(String name) throws Exception {
      check(system.playing(name), "wait for real source playback: " + name);
      check(NativePosition.read(system, name).state == AL10.AL_PLAYING, "native source did not reach OpenAL playback: " + name);
    }

    private void diagnostics(String phase, String action) throws Exception {
      for (String name : OWNED) {
        Source source = source(system, name);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("result", "source-diagnostic"); row.put("phase", phase); row.put("action", action);
        row.put("stage", stage); row.put("source", name); row.put("timeNanos", System.nanoTime());
        row.put("sourceClockMillis", clockSeconds(name) * 1000);
        row.put("present", source != null); row.put("preloads", preloads(name));
        if (source != null) {
          row.put("playing", source.playing()); row.put("paused", source.paused()); row.put("stopped", source.stopped());
          row.put("sourceIdentity", System.identityHashCode(source)); row.put("channelPresent", source.channel != null);
          try {
            NativePosition position = NativePosition.optional(system, name);
            if (position != null) {
              row.put("channelIdentity", System.identityHashCode(position.channel)); row.put("nativeId", position.nativeId);
              row.put("offsetSeconds", position.offsetSeconds); row.put("openAlState", position.state);
              row.put("queuedBuffers", position.queued); row.put("processedBuffers", position.processed);
            }
          } catch (Exception | AssertionError transientSnapshot) {
            row.put("nativeSnapshotChanged", transientSnapshot.toString());
          }
        }
        observations.add(row);
      }
    }

    private void reset() throws Exception {
      cleanup(); AudioConfig.preview(settings());
      put(AudioController.class, "client", mc); put(AudioController.class, "musicWorld", mc.world);
      put(AudioController.class, "wasWorld", true); put(AudioController.class, "dirty", false);
      put(AudioController.class, "pending", null); put(AudioController.class, "rescan", false);
      put(AudioController.class, "historyIndex", -1);
      ((List<?>)get(AudioController.class, "history")).clear();
      ((List<?>)get(AudioController.class, "historyPlayback")).clear();
      put(AudioController.class, "upcoming", null); put(AudioController.class, "nextKey", null);
      MusicRequests.edit(queue -> queue.tracks.clear());
      AudioController.playNow(FIRST); AudioController.playNow(SECOND);
      reservation = call(AudioController.class, "upcoming", new Class<?>[0]);
      check(AudioController.trackId((net.minecraft.class_267)reservation).equals(THIRD), "ordered fixture did not reserve the third saved song");
      MusicRequests.edit(queue -> queue.tracks.addAll(List.of(FIRST, THIRD, FIRST)));
    }

    private void request(String id, String name, double seconds) throws Exception {
      net.minecraft.class_267 entry = name.equals(PREVIEW)
          ? (net.minecraft.class_267)get(AUDITION, "entry") : (net.minecraft.class_267)get(AudioController.class, "currentEntry");
      check(entry != null, "owned native track entry missing");
      MusicSeeking.request(id, name, entry.field_2127, entry.field_2126, seconds);
    }
    private void prepared(String name, double seconds) throws Exception {
      check(!MusicSeeking.busy(), "allow the background seek worker and native tick to finish before checking");
      check(Math.abs(AudioController.position() - seconds) < .001, "prepared song clock differs from requested seek");
      Source source = source(system, name);
      check(source != null && !source.playing(), "seek completed with unintended native playback");
      check(source.stopped() && !source.paused(), "idle prepared seek was marked natively paused and would skip its first preload");
      observe("native-idle-seek", name + " offset=" + seconds + " stopped=" + source.stopped() + " paused=" + source.paused());
    }

    private void preparePrevious(String mode) throws Exception {
      check(Set.of("background-ten", "background-over", "preview-ten", "preview-over", "gap").contains(mode), "unsupported Previous mode " + mode);
      check(stage.equals("seek-complete") || stage.equals("previous-complete"), "finish transport checks or preceding Previous result first");
      previousMode = mode; reset(); AudioController.pause();
      if (mode.equals("gap")) { AudioController.quiet(); stage = "previous-preparing"; return; }
      target = mode.startsWith("preview") ? 45 : mode.endsWith("over") ? 10.25 : 10;
      request(SECOND, "BgMusic", target); stage = mode.startsWith("preview") ? "previous-background-preparing" : "previous-preparing";
    }
    private void prepareAuditionThreshold() throws Exception {
      expect("previous-background-preparing"); prepared("BgMusic", 45);
      AudioController.previewSound(THIRD); AudioController.pause();
      target = previousMode.endsWith("over") ? 10.25 : 10;
      request(THIRD, PREVIEW, target); stage = "previous-preparing";
    }
    private void pressPrevious() throws Exception {
      expect("previous-preparing");
      boolean gap = previousMode.equals("gap"), audition = previousMode.startsWith("preview"), over = previousMode.endsWith("over");
      if (gap) check(AudioController.currentTrackId().isEmpty() && AudioController.remainingDelay() > 0, "Quiet did not preserve a waiting interval");
      else prepared(audition ? PREVIEW : "BgMusic", target);
      check(AudioController.previousRestartsCurrent() == over, "Previous threshold disagrees with the visible prepared song");
      playback = Playback.read(); previousSource = source(system, audition ? PREVIEW : "BgMusic");
      seekSource = audition ? source(system, "BgMusic") : null;
      background = audition ? NativePosition.optional(system, "BgMusic") : null;
      restoreSources = List.copyOf((List<?>)get(AUDITION, "resume"));
      AudioController.previous();
      String expected = over ? audition ? THIRD : SECOND : gap ? SECOND : FIRST;
      check(AudioController.currentTrackId().equals(expected), "Previous selected the wrong song for " + previousMode);
      check(MusicRequests.tracks().equals(playback.queue), "Previous consumed or reordered queued requests");
      if (over) {
        playback.unchanged();
        check(get(AudioController.class, "upcoming") == reservation, "restart replaced the reserved ordered successor");
        check(AudioController.position() == 0, "restart did not reset the visible song clock");
        if (audition) {
          check(get(AudioController.class, "currentMusic").equals(SECOND), "audition restart replaced background identity");
          check(source(system, "BgMusic") == seekSource, "audition restart replaced its native background source");
          Source retained = (Source)seekSource;
          check(retained.stopped() && !retained.paused(), "audition pause poisoned the unstarted background seek's first preload");
          check(((MusicProgress)get(AudioController.class, "progress")).seconds() == 45, "audition restart reset background position");
          check(((List<?>)get(AUDITION, "resume")).equals(restoreSources), "audition restart replaced background restoration ownership");
        }
      } else check((int)get(AudioController.class, "historyIndex") == (gap ? 1 : 0), "Previous moved the wrong history distance");
      stage = "previous-result";
    }
    private void previousResult() throws Exception {
      expect("previous-result");
      boolean audition = previousMode.equals("preview-over");
      String name = audition ? PREVIEW : "BgMusic";
      running(name);
      check(source(system, name) != previousSource, "Previous did not replace the native song source");
      check(AudioController.position() < 8, "check restart promptly before its clock reaches eight seconds");
      if (audition) {
        if (background != null) retained(background, true);
        check(source(system, "BgMusic") == seekSource, "audition restart replaced its idle native background source");
        check(AudioController.previewing(THIRD), "Previous ended a long audition");
      } else if (previousMode.equals("background-over")) {
        MusicRequests.edit(queue -> queue.tracks.clear()); AudioController.next();
        check(AudioController.currentTrackId().equals(THIRD), "restarting changed the ordered next song");
      }
      observe("previous", previousMode + " real source playing, history/order/queue checked");
      stage = audition ? "previous-audition-complete" : "previous-complete";
    }

    private NativePosition retained(NativePosition before, boolean exactCursor) throws Exception {
      NativePosition now = NativePosition.read(system, before.name);
      check(now.source == before.source && now.channel == before.channel && now.nativeId == before.nativeId, "mute/restoration replaced native source/channel " + before.name);
      check(now.source.paused() && !now.source.stopped() && !now.source.playing(), "native source was stopped or resumed instead of paused: " + before.name);
      check(now.state == AL10.AL_PAUSED, "OpenAL source is not paused: " + before.name);
      check(now.preloads == before.preloads, "pause/unmute preloaded the stream again: " + before.name);
      check(Math.abs(now.clockSeconds - before.clockSeconds) < .15, "paused cumulative song clock moved: " + before.name);
      if (exactCursor) check(Math.abs(now.offsetSeconds - before.offsetSeconds) < .002, "paused OpenAL cursor moved: " + before.name);
      observe("native-paused", before.name + " sourceMillis=" + now.clockSeconds * 1000 + " offsetSeconds=" + now.offsetSeconds
          + " state=" + now.state + " queued=" + now.queued + " processed=" + now.processed);
      return now;
    }
    private void resumed(NativePosition before) throws Exception {
      running(before.name); NativePosition now = NativePosition.read(system, before.name);
      check(now.source == before.source && now.channel == before.channel && now.nativeId == before.nativeId, "manual resume replaced the source/channel");
      check(now.preloads == before.preloads, "manual resume rewound/preloaded the retained stream");
      check(!now.source.paused() && !now.source.stopped() && now.state == AL10.AL_PLAYING, "manual resume did not reach OpenAL playback");
      check(now.clockSeconds > before.clockSeconds + .05, "manual resume did not advance the cumulative native-playing clock");
    }
    private void stopPreview(boolean restore) throws Exception {
      call(AUDITION, "stop", new Class<?>[] {SoundSystem.class, boolean.class}, system, restore);
    }
    private void cleanup() throws Exception {
      MusicSeeking.cancel(); stopPreview(false);
      put(AudioController.class, "client", mc); AudioController.quiet();
      for (String name : OWNED) { system.stop(name); system.removeSource(name); }
    }
    @Override public void close() throws Exception {
      try { cleanup(); files.close(); }
      finally {
        mc.options.musicVolume = music; mc.options.soundVolume = sound;
        NativeStorage.preview(mc.soundManager::method_2008);
        Object selector = get(AudioController.class, "WORLD");
        @SuppressWarnings("unchecked") Map<Object, Object> cursors = (Map<Object, Object>)field(TrackSelector.class, "cursors").get(selector);
        cursors.clear(); cursors.putAll((Map<?, ?>)field(TrackSelector.class, "cursors").get(savedSelector));
        field(TrackSelector.class, "previous").set(selector, field(TrackSelector.class, "previous").get(savedSelector));
        auditionClock.restore(); backgroundClock.restore(); audition.restore(); controller.restore(); audio.restore();
        ((SoundManagerAccessor)mc.soundManager).power$countdown(countdown);
        ((AtomicInteger)get(AudioStartChecks.class, "background")).set(backgroundPreloads);
        ((AtomicInteger)get(AudioStartChecks.class, "preview")).set(auditionPreloads);
        put(Class.forName("local.luke.power.environment.ModHelper$ModHelperFields"), "currentBGM", display);
      }
    }
  }

  private record Playback(List<?> history, List<?> origins, int index, String selector, List<String> queue) {
    static Playback read() throws Exception {
      return new Playback(List.copyOf((List<?>)get(AudioController.class, "history")),
          List.copyOf((List<?>)get(AudioController.class, "historyPlayback")), (int)get(AudioController.class, "historyIndex"),
          Catalog.JSON.toJson(get(AudioController.class, "WORLD")), MusicRequests.tracks());
    }
    void unchanged() throws Exception {
      check(history.equals(get(AudioController.class, "history")) && origins.equals(get(AudioController.class, "historyPlayback"))
          && index == (int)get(AudioController.class, "historyIndex"), "music operation changed history/origin");
      check(selector.equals(Catalog.JSON.toJson(get(AudioController.class, "WORLD"))), "music operation changed ordered selector cursor");
      check(queue.equals(MusicRequests.tracks()), "music operation consumed/reordered queued requests");
    }
  }

  /** Beta has no millisecondsPlayed API. Keep cumulative native-playing time separate from queue-relative OpenAL offsets. */
  private record NativePosition(String name, Source source, Object channel, int nativeId,
      double clockSeconds, float offsetSeconds, int state, int queued, int processed, int preloads) {
    static NativePosition optional(SoundSystem system, String name) throws Exception {
      synchronized (SoundSystemConfig.THREAD_SYNC) {
        Source source = Release465AudioChecks.source(system, name); return source == null || source.channel == null ? null : read(system, name);
      }
    }
    static NativePosition read(SoundSystem system, String name) throws Exception {
      synchronized (SoundSystemConfig.THREAD_SYNC) {
        Source source = Release465AudioChecks.source(system, name);
        check(source != null && source.channel != null, "native source/channel missing: " + name);
        IntBuffer id = (IntBuffer)field(source.channel.getClass(), "ALSource").get(source.channel);
        int nativeId = id.get(0);
        return new NativePosition(name, source, source.channel, nativeId, Release465AudioChecks.clockSeconds(name),
            AL10.alGetSourcef(nativeId, AL11.AL_SEC_OFFSET), AL10.alGetSourcei(nativeId, AL10.AL_SOURCE_STATE),
            AL10.alGetSourcei(nativeId, AL10.AL_BUFFERS_QUEUED), AL10.alGetSourcei(nativeId, AL10.AL_BUFFERS_PROCESSED), Release465AudioChecks.preloads(name));
      }
    }
  }

  private static double clockSeconds(String name) throws Exception {
    return ((MusicProgress)get(name.equals(PREVIEW) ? AUDITION : AudioController.class, "progress")).seconds();
  }

  private static int preloads(String name) throws Exception { return ((AtomicInteger)get(AudioStartChecks.class, name.equals(PREVIEW) ? "preview" : "background")).get(); }
  private static Source source(SoundSystem system, String name) throws Exception {
    synchronized (SoundSystemConfig.THREAD_SYNC) {
      Object library = field(system.getClass(), "soundLibrary").get(system);
      return (Source)((Map<?, ?>)invoke(library, "getSources", new Class<?>[0])).get(name);
    }
  }
  private static Field field(Class<?> type, String name) throws Exception {
    for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) try {
      Field field = owner.getDeclaredField(name); field.setAccessible(true); return field;
    } catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  private static Object get(Class<?> type, String name) throws Exception { return field(type, name).get(null); }
  private static void put(Class<?> type, String name, Object value) throws Exception { field(type, name).set(null, value); }
  private static Object call(Class<?> type, String name, Class<?>[] types, Object... args) throws Exception {
    Method method = type.getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(null, args);
  }
  private static Object invoke(Object owner, String name, Class<?>[] types, Object... args) throws Exception {
    Method method = owner.getClass().getMethod(name, types); return method.invoke(owner, args);
  }

  /** Includes newly added retained entries and controller caches without maintaining a field whitelist. */
  private static final class Snapshot {
    private final Object owner;
    private final Map<Field, Object> values = new LinkedHashMap<>();
    private final Map<Field, List<?>> lists = new LinkedHashMap<>();
    private final Map<Field, Map<?, ?>> maps = new LinkedHashMap<>();
    Snapshot(Class<?> type, Object owner) throws Exception {
      this.owner = owner;
      for (Field field : type.getDeclaredFields()) {
        if (Modifier.isStatic(field.getModifiers()) != (owner == null)) continue;
        field.setAccessible(true); Object value = field.get(owner);
        if (!Modifier.isFinal(field.getModifiers())) values.put(field, value);
        else if (value instanceof List<?> list) lists.put(field, new ArrayList<>(list));
        else if (value instanceof Map<?, ?> map) maps.put(field, new LinkedHashMap<>(map));
      }
    }
    @SuppressWarnings({"rawtypes", "unchecked"}) void restore() throws Exception {
      for (var value : values.entrySet()) value.getKey().set(owner, value.getValue());
      for (var value : lists.entrySet()) { List target = (List)value.getKey().get(owner); target.clear(); target.addAll(value.getValue()); }
      for (var value : maps.entrySet()) { Map target = (Map)value.getKey().get(owner); target.clear(); target.putAll(value.getValue()); }
    }
  }
}
