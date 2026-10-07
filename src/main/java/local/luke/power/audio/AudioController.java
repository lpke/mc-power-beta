package local.luke.power.audio;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import local.luke.power.PowerBeta;
import local.luke.power.mixin.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_267;
import net.minecraft.client.Minecraft;
import paulscode.sound.SoundSystem;

public final class AudioController {
  private static final ExecutorService SCANNER =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "Power Beta music scan");
            t.setDaemon(true);
            return t;
          });
  private static final TrackSelector WORLD = new TrackSelector();
  private static final Random RANDOM = new Random();
  private static final Map<String, Source> SOURCES = new HashMap<>();

  private record Source(String sound, float base, boolean ui) {}

  private record Library(MusicLibrary.Scan world, MusicLibrary.Scan menu) {}

  private static CompletableFuture<Library> pending;
  private static Library library =
      new Library(
          new MusicLibrary.Scan(List.of(), List.of()), new MusicLibrary.Scan(List.of(), List.of()));
  private static Minecraft client;
  private static MusicRules rules;
  private static boolean dirty = true, paused, wasWorld, rescan, silenced;
  private static String currentMusic = "",
      menuMusic = "",
      status = "";
  private static int cleanup, menuCooldown, libraryRevision, conversionRevision;
  private static boolean trackWasPlaying;
  private static final MusicProgress progress = new MusicProgress();
  private static AudioSettings rotationConfig;
  private static List<MusicLibrary.Track> enabledWorld = List.of(), enabledMenu = List.of();
  private record Context(boolean world, int dimension, String biome) {}
  private record PoolKey(AudioSettings settings, MusicRules rules, Context context, int revision) {}
  private static PoolKey activeKey, nextKey;
  private static class_267 upcoming;
  private static Set<String> activeTracks = Set.of();
  private static List<MusicLibrary.Track> custom = List.of();
  private static Map<String, MusicLibrary.Track> customById = Map.of();
  private static Map<String, String> customByUrl = Map.of();

  public static int libraryRevision() { return libraryRevision; }
  private static final List<class_267> history = new ArrayList<>();
  private static int historyIndex = -1;
  private static long musicStarted, soundPreviewStarted;
  private static String soundPreview = "";
  private static boolean soundPreviewPlaying;
  private static Object soundPreviewWorld;

  public static boolean previewing(String id) {
    return id != null && (id.equals(soundPreview) || MusicPreview.active() && id.equals(MusicPreview.track()));
  }

  public static boolean musicPlaying() {
    SoundSystem s = system();
    return s != null && (MusicPreview.active() && !MusicPreview.paused() || !MusicPreview.active() && !paused && !silenced && !currentMusic.isBlank() && (s.playing("BgMusic") || s.playing("PowerBetaMenu")
        || !currentMusic.isEmpty() && System.nanoTime() - musicStarted < 1_000_000_000L));
  }

  public static boolean playingTrack(String id) {
    return musicPlaying() && !MusicPreview.active() && (id.equals(currentMusic) || id.equals(menuMusic));
  }

  public static void toggleTrack(String id) {
    if (MusicPreview.active()) { playNow(id); return; }
    if (playingTrack(id)) pause();
    else if (paused && (id.equals(currentMusic) || id.equals(menuMusic))) togglePause();
    else playNow(id);
  }

  private static void stopSoundPreview() {
    SoundSystem s = system();
    if (s != null) { s.stop("PowerBetaPreview"); s.removeSource("PowerBetaPreview"); }
    soundPreview = "";
    soundPreviewWorld = null;
  }

  private static void tickSoundPreview() {
    if (soundPreview.isEmpty()) return;
    if (client.world != soundPreviewWorld) { stopSoundPreview(); return; }
    if (system().playing("PowerBetaPreview")) soundPreviewPlaying = true;
    else if (soundPreviewPlaying || System.nanoTime() - soundPreviewStarted > 1_000_000_000L) stopSoundPreview();
  }

  private static void remember(class_267 track) {
    while (history.size() > historyIndex + 1) history.remove(history.size() - 1);
    history.add(track);
    if (history.size() > 128) history.remove(0);
    historyIndex = history.size() - 1;
  }

  private static void play(class_267 track) {
    SoundSystem system = system();
    if (system == null || client == null) return;
    if (track == null) {
      system.stop("BgMusic"); system.stop("PowerBetaMenu");
      currentMusic = ""; menuMusic = "";
      return;
    }
    MusicSeeking.cancel(); progress.reset(0,System.nanoTime());
    upcoming = null; nextKey = null;
    system.stop("PowerBetaMenu");
    menuMusic = "";
    currentMusic = trackId(track);
    paused = false;
    silenced = false;
    status = "";
    menuCooldown = 0;
    int dimension = client.player == null ? 0 : client.player.dimensionId;
    MusicStream.start(system, "BgMusic", track.field_2127, track.field_2126,
        musicVolume(currentMusic), false);
    musicStarted = System.nanoTime(); trackWasPlaying = false;
    nextDelay((SoundManagerAccessor) client.soundManager);
  }

  public static void previous() {
    if (system() != null) MusicPreview.stop(system(), true);
    if (history.isEmpty()) { next(); return; }
    historyIndex = Math.max(0, historyIndex - 1);
    play(history.get(historyIndex));
  }

  public static void previewSound(String id) {
    if (client == null || system() == null) return;
    if (id.startsWith("music:")) { previewMusic(id); return; }
    if (id.equals(soundPreview)) { stopSoundPreview(); return; }
    MusicPreview.stop(system(), true);
    var manager = (SoundManagerAccessor) client.soundManager;
    boolean record = id.startsWith("records.");
    var pool = record ? manager.power$records() : manager.power$sounds();
    class_267 track;
    synchronized (pool) { track = pool.method_958(record ? id.substring(8) : id); }
    if (track == null) return;
    SoundSystem system = system();
    system.stop("PowerBetaPreview");
    system.removeSource("PowerBetaPreview");
    system.newSource(false, "PowerBetaPreview", track.field_2127, track.field_2126, false, 0, 0, 0, 0, 0);
    system.setVolume("PowerBetaPreview", mix("PowerBetaPreview", id, client.options.soundVolume, false));
    soundPreview = id;
    soundPreviewStarted = System.nanoTime();
    soundPreviewPlaying = false;
    soundPreviewWorld = client.world;
    system.play("PowerBetaPreview");
  }

  private static void previewMusic(String id) {
    try {
      class_267 selected = resolveTrack(id);
      if (selected == null) { status = "Track unavailable. Reload music folders."; return; }
      stopSoundPreview();
      MusicPreview.start(client, system(), selected, !paused && !currentMusic.isEmpty()
          && System.nanoTime() - musicStarted < 1_000_000_000L);
    } catch (RuntimeException | java.io.IOException e) {
      MusicPreview.stop(system(), true);
      status = "Could not preview track. Reload music folders.";
      PowerBeta.LOG.warn("Music preview failed", e);
    }
  }

  private AudioController() {}

  public static void settingsChanged() {
    dirty = true;
    history.clear();
    historyIndex = -1;
    pause();
  }

  public static void rotationChanged() {
    history.clear();
    historyIndex = -1;
    pause();
  }

  public static void rulesChanged() {
    rules = MusicRules.read();
    refresh();
  }

  public static MusicRules rules() {
    if (rules == null) rules = MusicRules.read();
    return rules;
  }

  public static String currentTrackId() {
    if (MusicPreview.active()) return MusicPreview.track();
    if (currentMusic.isEmpty() && menuMusic.isEmpty()) return "";
    return !menuMusic.isEmpty() ? menuMusic : currentMusic;
  }

  public static int remainingDelay() {
    if (client == null) return 0;
    AudioSettings s = AudioConfig.current();
    return MusicTiming.remaining(s.waitBetweenTracks, s.delayQueuedTracks,
        !MusicRequests.tracks().isEmpty(), ((SoundManagerAccessor) client.soundManager).power$countdown());
  }

  public static double duration() {
    String id = currentTrackId(); if (id.isEmpty()) return 0;
    try { var track=resolveTrack(id); return track==null?0:MusicSeeking.duration(id,track.field_2127,track.field_2126); }
    catch (java.io.IOException e) { return 0; }
  }
  public static double position() {
    return MusicSeeking.position(currentTrackId(), MusicPreview.active() ? MusicPreview.SOURCE : "BgMusic",
        MusicPreview.active() ? MusicPreview.position() : progress.seconds());
  }
  public static void seek(double fraction) {
    double length=duration(); if (length<=0 || !Double.isFinite(fraction)) return;
    String id=currentTrackId();
    try {
      var track=resolveTrack(id); if(track==null)return;
      MusicSeeking.request(id,MusicPreview.active()?MusicPreview.SOURCE:"BgMusic",track.field_2127,track.field_2126,
          Math.min(Math.max(0,fraction)*length,Math.max(0,length-0.1)));
    } catch (Exception e) { status="Could not seek this track"; PowerBeta.LOG.warn(status,e); }
  }

  public static String status() {
    if (MusicPreview.active()) return (MusicPreview.paused() ? "Paused: " : "Previewing: ") + musicLabel(MusicPreview.track());
    if (paused) return currentTrackId().isEmpty() ? "Music paused" : "Paused: " + musicLabel(currentTrackId());
    if (AudioConfig.current().master == 0 || client != null && client.options.musicVolume == 0)
      return "Music muted";
    String playing = nowPlaying();
    if (!playing.equals("none") && !playing.isBlank()) return "Playing: " + playing;
    if (!silenced && !currentMusic.isBlank()) return "Starting: " + musicLabel(currentMusic);
    if (!status.isEmpty()) return status;
    class_267 next = upcoming();
    return MusicTiming.status(remainingDelay(), next == null ? "next track" : musicLabel(trackId(next)));
  }

  public static List<MusicLibrary.Track> customTracks() { return custom; }

  private static void indexLibrary() {
    rotationConfig = null;
    Map<String, MusicLibrary.Track> byId = new LinkedHashMap<>();
    for (var scan : List.of(library.world, library.menu)) for (var track : scan.tracks()) byId.putIfAbsent(track.id(), track);
    custom = List.copyOf(byId.values()); customById = Map.copyOf(byId);
    Map<String, String> urls = new HashMap<>();
    for (var track : custom) if (track.playable()) urls.put(track.playbackPath().toUri().toString(), track.id());
    customByUrl = Map.copyOf(urls);
  }

  public static String trackId(class_267 track) {
    // URL external forms use file:/ while Path URIs use file:///.
    String url = track.field_2127.toExternalForm();
    try { url = Path.of(track.field_2127.toURI()).toUri().toString(); } catch (Exception ignored) { }
    return customByUrl.getOrDefault(url, "music:" + track.field_2126);
  }

  public static String musicLabel(String id) {
    MusicLibrary.Track track = customById.get(id);
    return track == null ? BuiltinMusic.label(id.replaceFirst("^music:", "")) : track.name();
  }

  public static int trackVolume(String id) {
    AudioSettings settings = AudioConfig.current();
    return settings.sounds.getOrDefault(id, 100);
  }

  private static class_267 entry(MusicLibrary.Track track) throws java.io.IOException {
    if (!track.playable() || !Files.isRegularFile(track.playbackPath())) return null;
    return new class_267(track.playbackName(), track.playbackPath().toUri().toURL());
  }

  private static void updateRotation() {
    AudioSettings config = AudioConfig.current();
    if (rotationConfig != config) {
      Path game = FabricLoader.getInstance().getGameDir();
      enabledWorld = MusicFolders.enabledTracks(game, library.world, config.musicDirectories,
          config.disabledMusicDirectories, config.recursive);
      enabledMenu = MusicFolders.enabledTracks(game, library.menu, config.menuDirectories,
          config.disabledMenuDirectories, config.recursive);
      rotationConfig = config;
    }
  }

  private static Context context(Minecraft mc) {
    int dimension = mc != null && mc.player != null ? mc.player.dimensionId : 0;
    String biome = null;
    if (mc != null && mc.player != null && mc.world != null) {
      var currentBiome = mc.world.method_1781().method_1787(
          (int) Math.floor(mc.player.x), (int) Math.floor(mc.player.z));
      if (currentBiome != null) biome = currentBiome.field_888;
    }
    return new Context(mc != null && mc.world != null, dimension, biome);
  }

  /** The same background candidates feed automatic selection and the library's Active filter. */
  private static List<class_267> backgroundPool(List<class_267> vanilla, Context context,
      boolean includeExcluded) {
    updateRotation();
    AudioSettings s = AudioConfig.current();
    List<class_267> tracks = new ArrayList<>();
    List<class_267> worldCustom = new ArrayList<>();
    if (s.customMusic != AudioSettings.CustomMusic.OFF)
      for (var track : enabledWorld)
        try {
          if (track.playable()) { class_267 selected = entry(track); if (selected != null) worldCustom.add(selected); }
        } catch (java.io.IOException ignored) { /* A removed file cannot enter automatic rotation. */ }
    if (s.customMusic != AudioSettings.CustomMusic.ONLY || worldCustom.isEmpty()) tracks.addAll(vanilla);
    tracks.addAll(worldCustom);
    if (!context.world && rules().menuEnabled()) {
      List<class_267> menu = new ArrayList<>();
      for (var t : enabledMenu) try {
        if (t.playable()) { var selected = entry(t); if (selected != null) menu.add(selected); }
      } catch (java.io.IOException ignored) { }
      if (!menu.isEmpty() && rules().menuOverrides()) tracks.clear();
      tracks.addAll(menu);
    }
    if (!s.preset.isEmpty()) {
      tracks.clear();
      for (var builtin : BuiltinMusic.TRACKS) tracks.add(BundledMusic.entry(builtin.id()));
      for (var track : custom) try {
        var selected = entry(track); if (selected != null) tracks.add(selected);
      } catch (java.io.IOException ignored) { /* Keep missing tracks out of playback. */ }
    }
    // Overlapping world/menu folders should not increase a track's chance of selection.
    Map<String, class_267> unique = new LinkedHashMap<>();
    tracks.forEach(t -> unique.putIfAbsent(trackId(t), t));
    tracks = new ArrayList<>(unique.values());
    tracks.removeIf(track -> !s.preset.isEmpty() && !s.presetTrackPool.contains(trackId(track))
        || !includeExcluded && s.disabledTracks.contains(trackId(track))
        || s.preset.isEmpty() && !TrackRules.eligible(track.field_2126, context.dimension, context.biome));
    return tracks;
  }

  /** Ignore per-track exclusions and volume, so users can re-enable tracks within their pool. */
  public static Set<String> activeMusic(Minecraft mc) {
    PoolKey key = new PoolKey(AudioConfig.current(), rules(), context(mc), libraryRevision);
    if (key.equals(activeKey)) return activeTracks;
    updateRotation();
    Set<String> ids = new LinkedHashSet<>();
    backgroundPool(BundledMusic.selection(AudioConfig.current().musicMode), key.context, true)
        .forEach(t -> ids.add(trackId(t)));
    activeTracks = Collections.unmodifiableSet(ids);
    activeKey = key;
    return activeTracks;
  }

  public static class_267 resolveTrack(String id) throws java.io.IOException {
    MusicLibrary.Track customTrack = customById.get(id);
    if (customTrack != null) return entry(customTrack);
    class_267 builtin = BundledMusic.entry(id);
    if (builtin != null) return builtin;
    if (client == null) return null;
    var pool = ((SoundManagerAccessor) client.soundManager).power$music();
    synchronized (pool) {
      for (class_267 track : ((SoundPoolAccessor) pool).power$tracks()) if (id.equals("music:" + track.field_2126)) return track;
    }
    return null;
  }

  public static void playNow(String id) {
    if (system() == null) return;
    try {
      class_267 track = resolveTrack(id);
      if (track == null) { status = "Track unavailable. Convert MP3 files or reload folders."; return; }
      MusicPreview.stop(system(), false); remember(track); play(track);
    } catch (Exception e) { status = "Could not play track"; PowerBeta.LOG.warn(status, e); }
  }

  private static class_267 queued() {
    // Unavailable requests remain visible for repair instead of silently disappearing.
    if (MusicRequests.tracks().isEmpty()) return null;
    try {
      class_267 track = resolveTrack(MusicRequests.tracks().get(0));
      if (track == null) { status = "First queued track unavailable. Check the music library."; return null; }
      MusicRequests.edit(q -> q.tracks.remove(0));
      return track;
    } catch (Exception e) { status = "Could not read or save music queue"; PowerBeta.LOG.warn(status, e); return null; }
  }

  public static void playQueue() {
    if (system() == null) return;
    class_267 track = queued();
    if (track != null) { MusicPreview.stop(system(), false); remember(track); play(track); }
  }

  public static void reload() {
    if (pending != null && !pending.isDone()) {
      rescan = true;
      return;
    }
    AudioSettings s = AudioConfig.copy();
    Path game = FabricLoader.getInstance().getGameDir();
    status = "Scanning music folders...";
    pending =
        CompletableFuture.supplyAsync(
            () ->
                new Library(
                    MusicLibrary.scan(game, s.musicDirectories, s.recursive),
                    MusicLibrary.scan(game, s.menuDirectories, s.recursive)),
            SCANNER);
  }

  public static void tick(Minecraft mc) {
    client = mc;
    if (conversionRevision != Mp3Converter.revision()) { conversionRevision = Mp3Converter.revision(); reload(); }
    if (dirty) {
      dirty = false;
      rules = MusicRules.read();
      reload();
      refresh();
    }
    if (pending != null && pending.isDone()) {
      try {
        library = pending.join();
        indexLibrary();
        libraryRevision++;
        List<String> errors = new ArrayList<>(library.world.warnings());
        errors.addAll(library.menu.warnings());
        status = errors.isEmpty() ? "" : errors.get(0);
        errors.forEach(PowerBeta.LOG::warn);
      } catch (RuntimeException e) {
        PowerBeta.LOG.error("Music scan failed", e);
        status = "Music scan failed. Existing playlist retained.";
      }
      pending = null;
      if (rescan) {
        rescan = false;
        reload();
      }
    }
    SoundSystem system = system();
    if (system == null) return;
    progress.update(!paused && !MusicPreview.active() && system.playing("BgMusic"), System.nanoTime());
    MusicSeeking.Prepared seek = MusicSeeking.take();
    if (seek != null) {
      if (!seek.id().equals(currentTrackId())) seek.codec().cleanup();
      else {
        MusicStream.start(system, seek.source(), MusicSeeking.publish(seek), "position.pbseek",
            musicVolume(seek.id()), MusicPreview.active() ? MusicPreview.paused() : paused);
        if (MusicPreview.active()) MusicPreview.seeked(seek.seconds());
        else { progress.reset(seek.seconds(),System.nanoTime()); resuming(); }
      }
    }
    MusicDisplay.update(nowPlaying());
    boolean inWorld = mc.world != null;
    if (inWorld != wasWorld) {
      boolean keepMusic = AudioConfig.current().continuesAcrossMenus();
      MusicPreview.worldChanged(mc, system, keepMusic);
      if (!keepMusic) {
        MusicSeeking.cancel(); progress.reset(0,System.nanoTime());
        system.stop("PowerBetaMenu");
        system.stop("BgMusic");
        currentMusic = "";
        menuMusic = "";
        history.clear();
        historyIndex = -1;
        paused = false;
        silenced = true;
        trackWasPlaying = false;
        nextDelay((SoundManagerAccessor) mc.soundManager);
      }
      // Keep the current stream, pause state, queue and gap. Future choices use the new context.
      upcoming = null;
      nextKey = null;
      wasWorld = inWorld;
    }
    tickSoundPreview();
    MusicPreview.tick(mc, system);
    if (MusicPreview.active()) return;
    if (paused) {
      // A queued play can finish after Pause. Keep it silent and enforce the pause.
      if (system.playing("BgMusic")) system.pause("BgMusic");
      if (system.playing("PowerBetaMenu")) system.pause("PowerBetaMenu");
      return;
    }
    if (AudioConfig.current().master == 0) {
      system.stop("BgMusic");
      system.stop("PowerBetaMenu");
      return;
    }
    automaticMusic(mc, system);
    if (++cleanup >= 100) {
      cleanup = 0;
      SOURCES.keySet().removeIf(source -> !system.playing(source));
    }
  }

  private static void automaticMusic(Minecraft mc, SoundSystem system) {
    if (menuCooldown > 0) menuCooldown--;
    if (silenced) {
      // SoundSystem may finish an older queued Play after Quiet's Stop.
      if (system.playing("BgMusic")) {
        system.setVolume("BgMusic", 0);
        system.stop("BgMusic");
      }
    } else {
      if (system.playing("BgMusic") || system.playing("PowerBetaMenu")) {
        trackWasPlaying = true;
        return;
      }
      long elapsed = System.nanoTime() - musicStarted;
      if (!currentMusic.isEmpty() && (elapsed < 1_000_000_000L
          || !trackWasPlaying && elapsed < 5_000_000_000L)) return;
    }
    currentMusic = "";
    menuMusic = "";
    if (system.playing("streaming") || mc.options.musicVolume <= 0) return;
    var sound = (SoundManagerAccessor) mc.soundManager;
    if (remainingDelay() > 0) {
      sound.power$countdown(Math.max(0, sound.power$countdown() - 1));
      return;
    }
    if (menuCooldown > 0) return;
    if (!MusicRequests.tracks().isEmpty()) {
      playQueue();
    } else {
      next();
    }
    // Missing files / empty pools should not be retried every tick.
    if (currentMusic.isEmpty()) menuCooldown = 40;
  }

  /** Ends this track, keeping automatic playback enabled and the queue intact. */
  public static void quiet() {
    SoundSystem s = system();
    if (s == null || client == null) return;
    MusicPreview.stop(s, false);
    MusicSeeking.cancel();
    s.setVolume("BgMusic", 0); s.setVolume("PowerBetaMenu", 0);
    s.stop("BgMusic"); s.stop("PowerBetaMenu");
    currentMusic = ""; menuMusic = "";
    trackWasPlaying = false; paused = false; silenced = true;
    status = ""; menuCooldown = 0;
    nextDelay((SoundManagerAccessor) client.soundManager);
  }

  public static class_267 choose(List<class_267> vanilla) {
    AudioSettings s = AudioConfig.current();
    class_267 requested = queued();
    if (requested != null) { remember(requested); currentMusic = trackId(requested); musicStarted = System.nanoTime(); trackWasPlaying = false; return requested; }
    Context context = context(client);
    class_267 chosen = upcoming();
    upcoming = null;
    if (chosen != null) {
      remember(chosen);
      currentMusic = trackId(chosen);
      musicStarted = System.nanoTime(); trackWasPlaying = false;
    } else {
      status = "No tracks available for the current music settings.";
    }
    return chosen;
  }

  /** Reserve once; status rendering must never skip or re-roll the advertised track. */
  private static class_267 upcoming() {
    if (!MusicRequests.tracks().isEmpty()) {
      try { return resolveTrack(MusicRequests.tracks().get(0)); }
      catch (java.io.IOException ignored) { return null; }
    }
    if (historyIndex + 1 < history.size()) return history.get(historyIndex + 1);
    PoolKey key = new PoolKey(AudioConfig.current(), rules(), context(client), libraryRevision);
    if (!key.equals(nextKey)) { upcoming = null; nextKey = key; }
    if (upcoming == null) {
      AudioSettings s = AudioConfig.current();
      upcoming = WORLD.choose(backgroundPool(BundledMusic.selection(s.musicMode), key.context, false),
          s.shuffle, s.avoidRepeats, RANDOM, t -> t.field_2127.toExternalForm());
    }
    return upcoming;
  }

  static void resuming() { musicStarted = System.nanoTime(); trackWasPlaying = false; }

  public static void resetDelay() {
    if (client != null) nextDelay((SoundManagerAccessor) client.soundManager);
  }

  public static void changingDimension() {
    var policy = AudioConfig.current().dimensionMusic;
    if (policy == AudioSettings.DimensionMusic.STOP_ALL
        || policy == AudioSettings.DimensionMusic.STOP_SPECIFIC && client != null && client.player != null
            && TrackRules.dimensionSpecific(currentMusic, client.player.dimensionId)) quiet();
  }

  public static void nextDelay(SoundManagerAccessor sound) {
    MusicRules r = rules();
    sound.power$countdown(r.delayMin() + RANDOM.nextInt(r.delayRandom()));
  }

  public static float mix(String source, String id, float volume, boolean ui) {
    float effects = client == null ? 1 : client.options.soundVolume;
    SOURCES.put(source, new Source(id, effects > 0 ? volume / effects : 0, ui));
    return source.equals("streaming") && MusicPreview.active() ? 0
        : (float) (volume * AudioConfig.current().gain(id, ui) * rules().ambient(id));
  }

  public static String musicGroup(String id) {
    return MusicGroups.id(id, customById.get(id));
  }

  public static float musicVolume(String id) {
    float base = client == null ? 1 : client.options.musicVolume;
    AudioSettings s = AudioConfig.current();
    return base * s.master / 100f * trackVolume(id) / 100f * MusicGroups.gain(s.groupVolumes, musicGroup(id));
  }

  public static float backgroundVolume(float original) {
    if (MusicPreview.active() || paused) return 0;
    AudioSettings s = AudioConfig.current();
    return original * s.master / 100f * trackVolume(currentMusic) / 100f * MusicGroups.gain(s.groupVolumes, musicGroup(currentMusic));
  }

  private static SoundSystem system() {
    return SoundManagerAccessor.power$started() ? SoundManagerAccessor.power$system() : null;
  }

  public static void refresh() {
    SoundSystem s = system();
    if (s == null) return;
    float effects = client == null ? 1 : client.options.soundVolume;
    for (var e : SOURCES.entrySet()) {
      Source source = e.getValue();
      s.setVolume(
          e.getKey(),
          e.getKey().equals("streaming") && MusicPreview.active() ? 0 : (float)
              (source.base
                  * effects
                  * AudioConfig.current().gain(source.sound, source.ui)
                  * rules().ambient(source.sound)));
    }
    if (client != null) {
      s.setVolume("BgMusic", (MusicPreview.active() || paused || silenced) ? 0 : musicVolume(currentMusic));
      s.setVolume("PowerBetaMenu", (MusicPreview.active() || paused || silenced) ? 0 : musicVolume(menuMusic));
      if (MusicPreview.active()) s.setVolume(MusicPreview.SOURCE, MusicPreview.paused() ? 0 : musicVolume(MusicPreview.track()));
    }
  }

  public static String nowPlaying() {
    SoundSystem s = system();
    if (s == null) return "none";
    if (!paused && !silenced && !menuMusic.isBlank() && s.playing("PowerBetaMenu")) return musicLabel(menuMusic);
    if (!paused && !silenced && !currentMusic.isBlank() && s.playing("BgMusic")) return musicLabel(currentMusic);
    if (s.playing("streaming")) {
      Source record = SOURCES.get("streaming");
      return record == null ? "Music disc" : record.sound.replaceFirst("^records\\.", "");
    }
    return "none";
  }

  public static void pause() {
    if (MusicPreview.active() && system() != null) { MusicPreview.pause(system()); return; }
    paused = true;
    SoundSystem s = system();
    if (s != null) {
      MusicPreview.pause(s);
      s.setVolume("BgMusic", 0); s.setVolume("PowerBetaMenu", 0);
      s.pause("BgMusic"); s.pause("PowerBetaMenu");
    }
  }

  public static void togglePause() {
    SoundSystem s = system();
    if (s == null) return;
    if (MusicPreview.active()) {
      if (MusicPreview.paused()) MusicPreview.resume(s); else MusicPreview.pause(s);
      return;
    }
    if (musicPlaying()) { pause(); return; }
    if (!paused || currentMusic.isEmpty() && menuMusic.isEmpty()) { next(); return; }
    paused = false;
    resuming();
    refresh();
    if (!currentMusic.isEmpty()) s.play("BgMusic");
    if (!menuMusic.isEmpty() && (client == null || client.world == null)) s.play("PowerBetaMenu");
  }

  public static void next() {
    if (client == null || system() == null) return;
    MusicPreview.stop(system(), false);
    paused = false;
    if (!MusicRequests.tracks().isEmpty()) { playQueue(); return; }
    if (historyIndex + 1 < history.size()) { play(history.get(++historyIndex)); return; }
    List<class_267> tracks = BundledMusic.selection(AudioConfig.current().musicMode);
    play(choose(tracks));
  }

  public static Set<String> sounds(Minecraft mc) {
    Set<String> result = new TreeSet<>();
    var sound = ((SoundManagerAccessor) mc.soundManager).power$sounds();
    synchronized (sound) {
      result.addAll(((SoundPoolAccessor) sound).power$groups().keySet());
    }
    var record = ((SoundManagerAccessor) mc.soundManager).power$records();
    synchronized (record) {
      for (String key : ((SoundPoolAccessor) record).power$groups().keySet())
        result.add("records." + key);
    }
    result.removeIf(id -> !BetaSounds.contains(id));
    return result;
  }

  public static Set<String> music(Minecraft mc) {
    Set<String> ids = new TreeSet<>();
    for (var track : BuiltinMusic.TRACKS) ids.add(track.id());
    for (var track : customTracks()) ids.add(track.id());
    return ids;
  }
}
