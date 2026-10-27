package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import local.luke.power.audio.*;
import local.luke.power.config.Catalog;
import local.luke.power.mixin.SoundManagerAccessor;
import net.minecraft.client.Minecraft;
import paulscode.sound.SoundSystem;

/** Real muted streams plus isolated controller contexts; the native world remains loaded. */
final class Release464AudioChecks {
  private static final String FIRST = "music:calm1.ogg", SECOND = "music:calm2.ogg", THIRD = "music:calm3.ogg";
  private static final String WORLD = "audio464-world", MENU = "audio464-menu", PREVIEW_SOURCE = "PowerBetaMusicPreview";
  private static final List<String> SOURCES = List.of("BgMusic", "PowerBetaMenu", "streaming", PREVIEW_SOURCE);
  private static final Path REPORT = Path.of("power-beta-data/reports/release-464/audio");
  private static final List<Map<String, Object>> observations = new ArrayList<>();
  private static Class<?> preview;

  private Release464AudioChecks() {}

  static void run(Minecraft mc, String action) throws Exception {
    check(action.equals("capture") || action.equals("check"), "use release-464-audio capture|check");
    SilentAudioChecks.ready(mc);
    check(mc.world != null && mc.player != null && !mc.world.isRemote, "open a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    preview = Class.forName("local.luke.power.audio.MusicPreview");
    SoundSystem system = SoundManagerAccessor.power$system();
    check(AudioController.currentTrackId().isEmpty(), "Quiet the disposable music player before audio checks");
    for (String source : SOURCES) check(!system.playing(source), "stop disposable source " + source + " before audio checks");
    observations.clear(); failures = 0;
    try (var state = new State(mc, system)) {
      test("RELEASE464 AUDIO Library gains apply to every playlist and imported source root", () -> gains(mc, system));
      test("RELEASE464 AUDIO preview row pause/resume retains audition identity and original background", () -> preview(mc, system));
      test("RELEASE464 AUDIO paused current and automatic world/menu gaps retain navigation and reservation", () -> navigation(mc, system));
      test("RELEASE464 AUDIO queued member and outsider navigation never consumes requests", () -> queuedNavigation(mc, system));
      test("RELEASE464 AUDIO playlist Play starts first/random and preserves all requests", () -> playlistPlay(mc, system));
    } finally {
      Files.createDirectories(REPORT);
      Files.writeString(REPORT.resolve(action + ".json"), Catalog.JSON.toJson(Map.of(
          "action", action, "failures", failures, "observations", observations)));
    }
    log("RELEASE464 AUDIO " + action + " failures=" + failures + " report=" + REPORT.toAbsolutePath());
    check(failures == 0, "release-464-audio failures=" + failures);
  }

  private static AudioSettings settings() {
    var settings = new AudioSettings();
    settings.playlists.add(new MusicPlaylist(WORLD, "Audio world", List.of(FIRST, SECOND)));
    settings.playlists.add(new MusicPlaylist(MENU, "Audio menu", List.of(THIRD)));
    settings.playlistOrder.addAll(List.of(MENU, MusicPlaylist.FAVORITES, WORLD, MusicPlaylist.BETA));
    settings.playlist = WORLD; settings.menuPlaylist = MENU;
    settings.shuffle = false; settings.avoidRepeats = false;
    settings.waitBetweenTracks = settings.delayQueuedTracks = true;
    settings.gapMinSeconds = settings.gapMaxSeconds = 600;
    return settings;
  }

  private static void reset(Minecraft mc, SoundSystem system) throws Exception {
    call(preview, "stop", new Class<?>[] {SoundSystem.class, boolean.class}, system, false);
    put(AudioController.class, "client", mc);
    AudioController.quiet();
    for (String source : SOURCES) { system.stop(source); system.removeSource(source); }
    await(() -> SOURCES.stream().noneMatch(system::playing), "fixture sources did not stop");
    MusicRequests.edit(queue -> queue.tracks.clear());
    AudioConfig.preview(settings());
    put(AudioController.class, "client", mc); put(AudioController.class, "musicWorld", mc.world);
    put(AudioController.class, "wasWorld", true); put(AudioController.class, "dirty", false);
    put(AudioController.class, "pending", null); put(AudioController.class, "rescan", false);
    put(AudioController.class, "paused", false); put(AudioController.class, "silenced", false);
    put(AudioController.class, "currentMusic", ""); put(AudioController.class, "menuMusic", "");
    put(AudioController.class, "currentPlaylistId", ""); put(AudioController.class, "upcoming", null);
    put(AudioController.class, "nextKey", null); put(AudioController.class, "historyIndex", -1);
    ((List<?>)get(AudioController.class, "history")).clear();
    ((List<?>)get(AudioController.class, "historyPlayback")).clear();
  }

  private static void gains(Minecraft mc, SoundSystem system) throws Exception {
    reset(mc, system);
    var settings = settings(); settings.libraryGroupVolumes.put("Alpha", 37);
    settings.sounds.put(FIRST, 50);
    for (String selected : List.of(WORLD, MENU, MusicPlaylist.BETA, MusicPlaylist.FAVORITES)) {
      settings.playlist = selected; AudioConfig.preview(settings);
      put(AudioController.class, "client", null);
      check(Math.abs(AudioController.musicVolume(FIRST) - .185f) < .00001f, "playlist selection changed the Library gain");
    }
    Path root = Path.of("power-beta-data/fixtures/release-464-audio").toAbsolutePath().normalize();
    var imported = new MusicLibrary.Track(root.resolve("album/song.ogg"), "song.ogg");
    settings = settings(); settings.musicDirectories.add(root.toString());
    String source = MusicLibrarySources.id(root.toString());
    settings.libraryGroupVolumes.put(source, 23);
    settings.libraryGroupVolumes.put(MusicGroups.id(imported.id(), imported), 91);
    AudioConfig.preview(settings);
    put(AudioController.class, "library", new MusicLibrary.Scan(List.of(imported), List.of(), List.of(
        new MusicLibrary.SourceFolder(root.toString(), root, true, List.of()))));
    call(AudioController.class, "indexLibrary", new Class<?>[0]);
    put(AudioController.class, "client", null);
    check(AudioController.musicGroup(imported.id()).equals(source), "imported gain used a child directory instead of its Library source");
    check(Math.abs(AudioController.musicVolume(imported.id()) - .23f) < .00001f, "imported source gain did not reach playback");
    check(AudioController.sourceLibrary().trackSources().get(imported.id()).equals(source), "imported source ownership differs from playback gain");
    observations.add(Map.of("case", "global-gains", "builtinGain", .185, "importedGain", .23,
        "importedSourceIdentity", true));
  }

  private static void preview(Minecraft mc, SoundSystem system) throws Exception {
    for (boolean backgroundPaused : new boolean[] {false, true}) {
      reset(mc, system);
      AudioController.playNow(FIRST);
      await(() -> system.playing("BgMusic"), "background did not start");
      if (backgroundPaused) { AudioController.pause(); await(() -> !system.playing("BgMusic"), "background pause did not arrive"); }
      MusicProgress progress = (MusicProgress)get(AudioController.class, "progress");
      progress.reset(12.5, System.nanoTime());
      AudioController.previewSound(SECOND);
      await(() -> system.playing(PREVIEW_SOURCE), "audition did not start");
      AudioController.tick(mc);
      await(() -> !system.playing("BgMusic"), "audition did not pause background");
      check(AudioController.navigationPlaylistId().isEmpty(), "audition of a playlist member did not navigate to Library");
      AudioController.toggleTrack(SECOND);
      await(() -> !system.playing(PREVIEW_SOURCE), "row toggle did not pause the audition");
      check(AudioController.previewing(SECOND) && AudioController.previewPaused(), "row Pause ended the audition");
      check(AudioController.currentTrackId().equals(SECOND), "paused audition lost its song identity");
      check(get(AudioController.class, "currentMusic").equals(FIRST), "row Pause replaced the background identity");
      AudioController.toggleTrack(SECOND);
      await(() -> system.playing(PREVIEW_SOURCE), "row toggle did not resume the audition");
      check(AudioController.previewing(SECOND) && !AudioController.previewPaused(), "row Resume left the audition paused");
      check(!system.playing("BgMusic") && progress.seconds() == 12.5, "preview toggle resumed or advanced the background");
      call(preview, "stop", new Class<?>[] {SoundSystem.class, boolean.class}, system, true);
      await(() -> !system.playing(PREVIEW_SOURCE), "audition did not stop");
      if (backgroundPaused) check(!system.playing("BgMusic") && (boolean)get(AudioController.class, "paused"), "audition restored an explicitly paused background");
      else await(() -> system.playing("BgMusic"), "audition did not restore eligible background");
      check(AudioController.currentTrackId().equals(FIRST), "ending audition lost original song");
      observations.add(Map.of("case", "preview-toggle", "originalPaused", backgroundPaused,
          "restoredSong", AudioController.currentTrackId(), "backgroundSeconds", progress.seconds()));
    }
  }

  private static void navigation(Minecraft mc, SoundSystem system) throws Exception {
    reset(mc, system);
    AudioController.playNow(FIRST); await(() -> system.playing("BgMusic"), "current track did not start");
    AudioController.pause(); await(() -> !system.playing("BgMusic"), "current track did not pause");
    check(AudioController.currentTrackId().equals(FIRST) && AudioController.navigationTrackId().equals(FIRST), "paused current lost identity");
    check(AudioController.navigationPlaylistId().equals(WORLD), "paused world song lost playlist navigation");
    for (boolean menu : new boolean[] {false, true}) {
      reset(mc, system);
      // No native world mutation is needed: a detached controller client has menu context.
      if (menu) put(AudioController.class, "client", null);
      String previous = (String)field(TrackSelector.class, "previous").get(get(AudioController.class, "WORLD"));
      String advertised = AudioController.navigationTrackId();
      Object reservation = get(AudioController.class, "upcoming");
      String playlist = menu ? MENU : WORLD;
      check(MusicPlaylist.find(AudioConfig.current(), playlist).includes(advertised), "gap reserved outside its selected playlist");
      for (int i = 0; i < 8; i++) {
        check(AudioController.navigationTrackId().equals(advertised), "navigation rerolled advertised song");
        check(AudioController.navigationPlaylistId().equals(playlist), "gap navigation opened wrong world/menu playlist");
        check(get(AudioController.class, "upcoming") == reservation, "navigation replaced reserved track");
      }
      check(field(TrackSelector.class, "previous").get(get(AudioController.class, "WORLD")).equals(previous), "navigation advanced selector history");
      check(AudioController.currentTrackId().isEmpty(), "gap navigation started playback");
      check(MusicRequests.tracks().isEmpty(), "gap navigation changed queue");
      AudioController.choose();
      check(AudioController.currentTrackId().equals(advertised) && AudioController.navigationPlaylistId().equals(playlist), "accepted automatic song lost its source context");
      observations.add(Map.of("case", menu ? "menu-gap" : "world-gap", "song", advertised, "playlist", playlist));
    }
  }

  private static void queuedNavigation(Minecraft mc, SoundSystem system) throws Exception {
    for (boolean menu : new boolean[] {false, true}) for (String requested : List.of(FIRST, THIRD)) {
      reset(mc, system);
      if (menu) put(AudioController.class, "client", null);
      MusicRequests.edit(queue -> queue.tracks.addAll(List.of(requested, SECOND, requested)));
      List<String> before = MusicRequests.tracks();
      String destination = requested.equals(FIRST) ? WORLD : "";
      Object reservation = get(AudioController.class, "upcoming");
      for (int i = 0; i < 8; i++) {
        check(AudioController.navigationTrackId().equals(requested), "queued navigation did not identify the first request");
        check(AudioController.navigationPlaylistId().equals(destination), "queued navigation confused automatic menu membership with the world playlist");
        check(MusicRequests.tracks().equals(before), "queued navigation consumed/reordered requests");
        check(get(AudioController.class, "upcoming") == reservation, "queued navigation replaced automatic reservation");
      }
      observations.add(Map.of("case", "queued-navigation", "menu", menu, "song", requested,
          "destination", destination.isEmpty() ? "Library" : destination, "queue", before));
    }
  }

  private static void playlistPlay(Minecraft mc, SoundSystem system) throws Exception {
    reset(mc, system);
    MusicRequests.edit(queue -> queue.tracks.addAll(List.of(THIRD, SECOND, THIRD)));
    List<String> requests = MusicRequests.tracks();
    AudioController.playPlaylist(WORLD);
    await(() -> system.playing("BgMusic"), "ordered playlist did not start");
    check(AudioController.currentTrackId().equals(FIRST), "ordered playlist Play followed cursor/queue instead of first saved song");
    check(MusicRequests.tracks().equals(requests), "ordered playlist Play consumed queue");
    AudioController.playNow(SECOND); AudioController.playPlaylist(WORLD);
    check(AudioController.currentTrackId().equals(FIRST), "ordered playlist Play retained old playback cursor");
    var shuffled = AudioConfig.copy(); shuffled.shuffle = true; AudioConfig.preview(shuffled);
    Random random = (Random)get(AudioController.class, "RANDOM");
    // Copy the RNG state without advancing it to check the exact shuffled choice.
    for (int attempt = 0; attempt < 64 && copyRandom(random).nextInt(2) == 0; attempt++) random.nextInt(2);
    int index = copyRandom(random).nextInt(2);
    AudioController.playPlaylist(WORLD);
    String expected = MusicPlaylist.find(shuffled, WORLD).tracks().get(index);
    check(index == 1 && AudioController.currentTrackId().equals(expected), "shuffled playlist Play did not choose randomly from saved members");
    check(MusicRequests.tracks().equals(requests), "shuffled playlist Play consumed or reordered queue");
    check(AudioController.navigationPlaylistId().equals(WORLD), "playlist Play lost selected world origin");
    observations.add(Map.of("case", "playlist-play", "ordered", FIRST, "shuffled", expected, "queue", requests));
  }

  private static Random copyRandom(Random random) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(random); }
    try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      return (Random)input.readObject();
    }
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
    private final Snapshot controller, previewState, progress, previewProgress, selector;
    private final SoundManagerAccessor sound;
    private final int countdown;
    private final Object display;
    State(Minecraft mc, SoundSystem system) throws Exception {
      this.system = system; files = new MusicPlaylists460Checks.State(mc);
      controller = new Snapshot(AudioController.class, null); previewState = new Snapshot(preview, null);
      progress = new Snapshot(MusicProgress.class, get(AudioController.class, "progress"));
      previewProgress = new Snapshot(MusicProgress.class, get(preview, "progress"));
      Object rotation = get(AudioController.class, "WORLD"); selector = new Snapshot(TrackSelector.class, rotation);
      sound = (SoundManagerAccessor)mc.soundManager; countdown = sound.power$countdown();
      display = get(Class.forName("local.luke.power.environment.ModHelper$ModHelperFields"), "currentBGM");
    }
    public void close() throws Exception {
      try {
        call(preview, "stop", new Class<?>[] {SoundSystem.class, boolean.class}, system, false);
        for (String source : SOURCES) { system.stop(source); system.removeSource(source); }
        await(() -> SOURCES.stream().noneMatch(system::playing), "audio fixture sources did not stop");
        files.close();
      } finally {
        selector.restore(); previewProgress.restore(); progress.restore(); previewState.restore(); controller.restore();
        sound.power$countdown(countdown);
        put(Class.forName("local.luke.power.environment.ModHelper$ModHelperFields"), "currentBGM", display);
      }
    }
  }

  /** Restore mutable scalars and owned final collection contents, including playback origin. */
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
