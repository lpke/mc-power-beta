package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import local.luke.power.audio.*;
import local.luke.power.config.Catalog;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import paulscode.sound.SoundSystem;

/** Real sound-thread handoffs, with reflected controller context and no native world replacement. */
final class AudioTransition461Checks {
  private static final String FIRST = "music:calm1.ogg", SECOND = "music:calm2.ogg", THIRD = "music:calm3.ogg";
  private static final String PREVIEW_SOURCE = "PowerBetaMusicPreview";
  private static final List<String> SOURCES = List.of("BgMusic", "PowerBetaMenu", "streaming", PREVIEW_SOURCE);
  private static final Path REPORT = Path.of("power-beta-data/reports/release-461/audio-transitions.json");
  private static final List<Map<String, Object>> observations = new ArrayList<>();
  private static Class<?> preview;

  private AudioTransition461Checks() {}

  static void run(Minecraft mc, String action) throws Exception {
    check(action.equals("check"), "use audio-transition-461 check");
    SilentAudioChecks.ready(mc);
    check(mc.world != null && mc.player != null && !mc.world.isRemote, "open a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    preview = Class.forName("local.luke.power.audio.MusicPreview");
    SoundSystem system = SoundManagerAccessor.power$system();
    check(!AudioController.previewing(AudioController.currentTrackId()), "finish the current audition before transition checks");
    check(AudioController.currentTrackId().isEmpty(), "Quiet the disposable music player before transition checks");
    for (String source : SOURCES) check(!system.playing(source), "stop the disposable " + source + " source before transition checks");
    observations.clear(); failures = 0;
    try (var state = new State(mc, system)) {
      test("AUDIO_TRANSITION461 old jukebox late Play cannot regain preview restoration after a world handoff", () -> recordRace(mc, system));
      test("AUDIO_TRANSITION461 an audition and its original background pause survive the handoff", () -> pausedPreview(mc, system));
      test("AUDIO_TRANSITION461 manual playback finishes across a change to a different automatic playlist", () -> request(mc, system, false));
      test("AUDIO_TRANSITION461 a consumed queued request finishes without consuming its successor at handoff", () -> request(mc, system, true));
      test("AUDIO_TRANSITION461 an ineligible automatic background still stops at handoff", () -> background(mc, system));
      test("AUDIO_TRANSITION461 Previous restores explicit origin while ordinary Next remains automatic", () -> history(mc, system));
    } finally {
      Files.createDirectories(REPORT.getParent());
      Files.writeString(REPORT, Catalog.JSON.toJson(Map.of("failures", failures, "observations", observations)));
    }
    log("AUDIO_TRANSITION461 failures=" + failures + " report=" + REPORT.toAbsolutePath());
    check(failures == 0, "audio-transition-461 failures=" + failures);
  }

  private static AudioSettings settings(boolean oldWorldPool) {
    var settings = AudioConfig.copy();
    settings.playlists = new ArrayList<>(List.of(
        new MusicPlaylist("transition461-world", "Transition world", List.of(oldWorldPool ? SECOND : FIRST)),
        new MusicPlaylist("transition461-menu", "Transition menu", List.of(SECOND))));
    settings.playlist = "transition461-world"; settings.menuPlaylist = "transition461-menu";
    settings.shuffle = false; settings.avoidRepeats = false; settings.master = 100;
    settings.waitBetweenTracks = settings.delayQueuedTracks = true;
    settings.gapMinSeconds = settings.gapMaxSeconds = 600;
    return settings;
  }

  private static void reset(Minecraft mc, SoundSystem system, boolean oldWorldPool) throws Exception {
    call(preview, "stop", new Class<?>[] {SoundSystem.class, boolean.class}, system, false);
    AudioController.quiet();
    for (String source : SOURCES) { system.stop(source); system.removeSource(source); }
    await(() -> SOURCES.stream().noneMatch(system::playing), "previous fixture sources did not stop");
    MusicRequests.edit(queue -> queue.tracks.clear());
    AudioConfig.preview(settings(oldWorldPool));
    put(AudioController.class, "client", mc); put(AudioController.class, "musicWorld", mc.world);
    put(AudioController.class, "wasWorld", true); put(AudioController.class, "dirty", false);
    put(AudioController.class, "pending", null); put(AudioController.class, "rescan", false);
  }

  private static void handoff(Minecraft mc) throws Exception {
    // The native world remains loaded. These are precisely the two remembered values
    // consulted by AudioController.tick when entering it from a menu context.
    put(AudioController.class, "musicWorld", new Object());
    put(AudioController.class, "wasWorld", false);
    AudioController.tick(mc);
  }

  private static void recordRace(Minecraft mc, SoundSystem system) throws Exception {
    reset(mc, system, false);
    AudioController.playNow(FIRST); await(() -> system.playing("BgMusic"), "background did not start");
    var record = AudioController.resolveTrack(SECOND); check(record != null, "bundled record-race source is unavailable");
    Class<?> stream = Class.forName("local.luke.power.audio.MusicStream");
    call(stream, "start", new Class<?>[] {SoundSystem.class, String.class, java.net.URL.class, String.class, float.class, boolean.class},
        system, "streaming", record.field_2127, record.field_2126, 0f, false);
    await(() -> system.playing("streaming"), "native record source did not start");
    AudioController.previewSound(THIRD);
    await(() -> system.playing(PREVIEW_SOURCE), "audition did not start");
    AudioController.tick(mc);
    await(() -> !system.playing("streaming") && !system.playing("BgMusic"), "audition did not pause original sources");
    check(((List<?>)get(preview, "resume")).contains("streaming"), "fixture did not capture the old jukebox before handoff");
    handoff(mc);
    check(AudioController.previewing(THIRD), "world handoff interrupted the audition");
    check(!((List<?>)get(preview, "resume")).contains("streaming"), "handoff retained the old jukebox restoration entry");
    // A sound-thread Play queued before departure may become visible only now.
    system.play("streaming"); await(() -> system.playing("streaming"), "late sound-thread record Play was not observed");
    AudioController.tick(mc);
    await(() -> !system.playing("streaming"), "audition did not pause the late record start");
    boolean recaptured = ((List<?>)get(preview, "resume")).contains("streaming");
    observations.add(Map.of("case", "late-record", "recaptured", recaptured));
    check(!recaptured, "preview tick re-added a departed world's jukebox to its restoration list");
    AudioController.previewSound(THIRD);
    await(() -> !system.playing(PREVIEW_SOURCE) && system.playing("BgMusic"), "stopping the audition did not resume its eligible background");
    Thread.sleep(80);
    check(!system.playing("streaming"), "stopping the audition revived the departed world's jukebox");
  }

  private static void pausedPreview(Minecraft mc, SoundSystem system) throws Exception {
    reset(mc, system, false);
    AudioController.playNow(FIRST); await(() -> system.playing("BgMusic"), "background did not start");
    var progress = (MusicProgress)get(AudioController.class, "progress"); progress.reset(12.5, System.nanoTime());
    AudioController.pause(); await(() -> !system.playing("BgMusic"), "original explicit pause was not observed");
    MusicRequests.edit(queue -> queue.add(SECOND)); List<String> queued = MusicRequests.tracks();
    AudioController.previewSound(THIRD); await(() -> system.playing(PREVIEW_SOURCE), "audition did not start over the paused background");
    AudioController.pause(); await(() -> !system.playing(PREVIEW_SOURCE), "audition pause was not observed");
    handoff(mc);
    check(AudioController.previewing(THIRD) && (boolean)get(preview, "paused"), "world handoff lost the audition's explicit pause");
    check(get(AudioController.class, "currentMusic").equals(FIRST) && (boolean)get(AudioController.class, "paused"), "world handoff replaced the original paused background");
    AudioController.previewSound(THIRD); AudioController.tick(mc);
    Thread.sleep(80);
    check(!system.playing("BgMusic") && !system.playing(PREVIEW_SOURCE), "ending the audition resumed an explicitly paused background");
    check(progress.seconds() == 12.5, "preview handoff reset or advanced the paused background position");
    check(MusicRequests.tracks().equals(queued), "audition handoff consumed the queue");
    observations.add(Map.of("case", "paused-preview", "background", get(AudioController.class, "currentMusic"), "position", progress.seconds()));
  }

  private static void request(Minecraft mc, SoundSystem system, boolean queued) throws Exception {
    reset(mc, system, false);
    if (queued) { MusicRequests.edit(queue -> queue.tracks.addAll(List.of(SECOND, THIRD))); AudioController.playQueue(); }
    else AudioController.playNow(SECOND);
    await(() -> system.playing("BgMusic"), "explicit request did not start");
    List<String> remaining = MusicRequests.tracks();
    check(get(AudioController.class, "currentMusic").equals(SECOND), "request did not own its selected identity");
    handoff(mc);
    String current = (String)get(AudioController.class, "currentMusic");
    observations.add(Map.of("case", queued ? "queued-request" : "manual-request", "currentAfterHandoff", current, "queue", MusicRequests.tracks()));
    check(current.equals(SECOND), "different automatic playlist interrupted a " + (queued ? "consumed queued" : "manual") + " request");
    check(MusicRequests.tracks().equals(remaining), "handoff consumed or reordered the next queued request");
    check(system.playing("BgMusic"), "handoff stopped the explicitly requested native stream");
    check(!queued || remaining.equals(List.of(THIRD)), "queued fixture did not consume exactly its first request");
  }

  private static void background(Minecraft mc, SoundSystem system) throws Exception {
    reset(mc, system, true);
    AudioController.next(); await(() -> system.playing("BgMusic"), "automatic background did not start");
    check(get(AudioController.class, "currentMusic").equals(SECOND), "automatic fixture selected the wrong old pool");
    AudioConfig.preview(settings(false));
    handoff(mc);
    check(!get(AudioController.class, "currentMusic").equals(SECOND), "ineligible automatic background survived the changed playlist");
    await(() -> !system.playing("BgMusic"), "ineligible automatic native stream did not stop");
    observations.add(Map.of("case", "automatic-background", "currentAfterHandoff", get(AudioController.class, "currentMusic")));
  }

  private static void history(Minecraft mc, SoundSystem system) throws Exception {
    reset(mc, system, false);
    AudioController.playNow(SECOND); await(() -> system.playing("BgMusic"), "manual history entry did not start");
    AudioController.next(); await(() -> system.playing("BgMusic") && AudioController.currentTrackId().equals(FIRST), "Next did not choose the automatic world pool");
    check(!(boolean)get(AudioController.class, "currentExplicitMusic"), "ordinary Next acquired explicit playback origin");
    AudioController.previous(); await(() -> system.playing("BgMusic") && AudioController.currentTrackId().equals(SECOND), "Previous did not recover the manual history entry");
    check((boolean)get(AudioController.class, "currentExplicitMusic"), "Previous discarded the manual history entry's playback origin");
    handoff(mc);
    check(get(AudioController.class, "currentMusic").equals(SECOND), "Previous's restored manual request was interrupted at handoff");
    observations.add(Map.of("case", "history-origin", "currentAfterHandoff", get(AudioController.class, "currentMusic")));
  }

  private static void await(BooleanSupplier condition, String failure) throws Exception {
    long deadline = System.nanoTime() + 3_000_000_000L;
    while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(5);
    check(condition.getAsBoolean(), failure);
  }
  private static Object get(Class<?> type, String name) throws Exception { return field(type, name).get(null); }
  private static void put(Class<?> type, String name, Object value) throws Exception { field(type, name).set(null, value); }
  private static Field field(Class<?> type, String name) throws Exception { var field = type.getDeclaredField(name); field.setAccessible(true); return field; }
  private static Object call(Class<?> type, String name, Class<?>[] types, Object... args) throws Exception {
    var method = type.getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(null, args);
  }

  private static final class State implements AutoCloseable {
    private final SoundSystem system;
    private final MusicPlaylists460Checks.State files;
    private final Snapshot controller, previewState, progress, previewProgress, rotation;
    private final SoundManagerAccessor sound;
    private final int countdown;
    private final Object display;
    State(Minecraft mc, SoundSystem system) throws Exception {
      this.system = system; files = new MusicPlaylists460Checks.State(mc);
      controller = new Snapshot(AudioController.class, null); previewState = new Snapshot(preview, null);
      progress = new Snapshot(MusicProgress.class, get(AudioController.class, "progress"));
      previewProgress = new Snapshot(MusicProgress.class, get(preview, "progress"));
      Object selector = get(AudioController.class, "WORLD"); rotation = new Snapshot(selector.getClass(), selector);
      sound = (SoundManagerAccessor)mc.soundManager; countdown = sound.power$countdown();
      display = get(Class.forName("local.luke.power.environment.ModHelper$ModHelperFields"), "currentBGM");
    }
    public void close() throws Exception {
      try {
        call(preview, "stop", new Class<?>[] {SoundSystem.class, boolean.class}, system, false);
        AudioController.quiet();
        for (String source : SOURCES) { system.stop(source); system.removeSource(source); }
        await(() -> SOURCES.stream().noneMatch(system::playing), "transition fixture sources did not stop");
        files.close();
      } finally {
        rotation.restore(); previewProgress.restore(); progress.restore(); previewState.restore(); controller.restore();
        sound.power$countdown(countdown);
        put(Class.forName("local.luke.power.environment.ModHelper$ModHelperFields"), "currentBGM", display);
      }
    }
  }

  /** Scalars retain their identity; mutable final lists/maps restore their owned contents. */
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
      for (var entry : values.entrySet()) entry.getKey().set(owner, entry.getValue());
      for (var entry : lists.entrySet()) { var target = (List)entry.getKey().get(owner); target.clear(); target.addAll(entry.getValue()); }
      for (var entry : maps.entrySet()) { var target = (Map)entry.getKey().get(owner); target.clear(); target.putAll(entry.getValue()); }
    }
  }
}
