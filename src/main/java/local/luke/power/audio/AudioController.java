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
  private static final UnderwaterAudio UNDERWATER = new UnderwaterAudio();

  private record Source(String sound, float base, boolean ui, String category) {}

  private static CompletableFuture<MusicLibrary.Scan> pending;
  private static MusicLibrary.Scan library = new MusicLibrary.Scan(List.of(), List.of());
  private static Minecraft client;
  private static MusicRules rules;
  private static Object musicWorld;
  private static boolean dirty = true, paused, wasWorld, rescan, silenced;
  private static String currentMusic = "",
      menuMusic = "",
      status = "";
  private static int cleanup, menuCooldown, libraryRevision, conversionRevision;
  private static boolean trackWasPlaying, currentUsesMenuVolumes;
  private static final MusicProgress progress = new MusicProgress();
  private static AudioSettings rotationConfig;
  private static List<MusicLibrary.Track> enabledTracks = List.of();
  private static MusicLibrarySources.Snapshot sourceLibrary =
      new MusicLibrarySources.Snapshot(List.of(), Map.of(), List.of());
  private record Context(boolean world, int dimension, String biome) {}
  private record PoolKey(AudioSettings settings, MusicRules rules, Context context, int revision) {}
  private static PoolKey activeKey, nextKey;
  private static class_267 upcoming;
  private static Set<String> activeTracks = Set.of();
  private static List<MusicLibrary.Track> custom = List.of();
  private static Map<String, MusicLibrary.Track> customById = Map.of();
  private static Map<String, String> customByUrl = Map.of();
  private static Map<String, MusicLibrary.Track> detachedCustom = Map.of();

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
    currentUsesMenuVolumes = usesMenuVolumes(currentMusic, context(client));
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
    boolean waiting = currentMusic.isEmpty() && menuMusic.isEmpty();
    historyIndex = Math.max(0, historyIndex - (waiting ? 0 : 1));
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
    var water = WaterSoundCatalog.choose(id, RANDOM);
    if (water != null && water.stream())
      system.newStreamingSource(true, "PowerBetaPreview", water.url(), water.filename(), false, 0, 0, 0, 0, 0);
    else system.newSource(false, "PowerBetaPreview", water == null ? track.field_2127 : water.url(),
        water == null ? track.field_2126 : water.filename(), false, 0, 0, 0, 0, 0);
    system.setVolume("PowerBetaPreview", mix("PowerBetaPreview", id,
        client.options.soundVolume * (water == null ? ExtraSounds.previewVolume(id) : water.volume()), false));
    system.setPitch("PowerBetaPreview", ExtraSounds.previewPitch(id));
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
    try { var track=resolveCurrentTrack(id); return track==null?0:MusicSeeking.duration(id,track.field_2127,track.field_2126); }
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
      var track=resolveCurrentTrack(id); if(track==null)return;
      MusicSeeking.request(id,MusicPreview.active()?MusicPreview.SOURCE:"BgMusic",track.field_2127,track.field_2126,
          Math.min(Math.max(0,fraction)*length,Math.max(0,length-0.1)));
    } catch (Exception e) { status="Could not seek this track"; PowerBeta.LOG.warn(status,e); }
  }

  public static String status() {
    if (MusicPreview.active()) return (MusicPreview.paused() ? "Paused: " : "Previewing: ") + musicLabel(MusicPreview.track());
    if (paused) return currentTrackId().isEmpty() ? "Music paused" : "Paused: " + musicLabel(currentTrackId());
    if (AudioConfig.current().master == 0 || client != null && client.options.musicVolume == 0)
      return "Music muted";
    // Source commands are queued on the sound thread. The chosen track owns the
    // label throughout that handoff, even before playing() observes the new source.
    if (!silenced && !currentTrackId().isBlank()) return "Playing: " + musicLabel(currentTrackId());
    String playing = nowPlaying();
    if (!playing.equals("none") && !playing.isBlank()) return "Playing: " + playing;
    if (!status.isEmpty()) return status;
    class_267 next = upcoming();
    return MusicTiming.status(remainingDelay(), next == null ? "next track" : musicLabel(trackId(next)));
  }

  public static List<MusicLibrary.Track> customTracks() { return custom; }

  public static MusicLibrarySources.Snapshot sourceLibrary() {
    updateRotation();
    return sourceLibrary;
  }

  private static void indexLibrary() {
    rotationConfig = null;
    Set<String> owned = new HashSet<>(List.of(currentMusic, menuMusic, MusicPreview.track()));
    Set<String> historyUrls = new HashSet<>();
    history.forEach(track -> historyUrls.add(trackUrl(track)));
    Map<String, MusicLibrary.Track> prior = new HashMap<>(detachedCustom);
    prior.putAll(customById);
    Map<String, MusicLibrary.Track> byId = new LinkedHashMap<>();
    for (var track : library.tracks()) byId.putIfAbsent(track.id(), track);
    // Current streams and bounded history keep their identity when a source goes away.
    // Detached metadata never makes a track available to the catalog or rotation.
    prior.entrySet().removeIf(entry -> byId.containsKey(entry.getKey())
        || !owned.contains(entry.getKey()) && (entry.getValue().playbackPath() == null
            || !historyUrls.contains(entry.getValue().playbackPath().toUri().toString())));
    detachedCustom = Map.copyOf(prior);
    custom = List.copyOf(byId.values()); customById = Map.copyOf(byId);
    Map<String, String> urls = new HashMap<>();
    for (var track : custom) if (track.playable()) urls.put(track.playbackPath().toUri().toString(), track.id());
    for (var track : detachedCustom.values()) if (track.playable())
      urls.putIfAbsent(track.playbackPath().toUri().toString(), track.id());
    customByUrl = Map.copyOf(urls);
  }

  private static String trackUrl(class_267 track) {
    // URL external forms use file:/ while Path URIs use file:///.
    try { return Path.of(track.field_2127.toURI()).toUri().toString(); }
    catch (Exception ignored) { return track.field_2127.toExternalForm(); }
  }

  public static String trackId(class_267 track) {
    return customByUrl.getOrDefault(trackUrl(track), "music:" + track.field_2126);
  }

  private static MusicLibrary.Track trackMetadata(String id) {
    MusicLibrary.Track track = customById.get(id);
    return track == null ? detachedCustom.get(id) : track;
  }

  public static String musicLabel(String id) {
    MusicLibrary.Track track = trackMetadata(id);
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
      if (rotationConfig != null && rotationConfig.musicDirectories.equals(config.musicDirectories)
          && rotationConfig.recursive == config.recursive) {
        rotationConfig = config;
        return;
      }
      Path game = FabricLoader.getInstance().getGameDir();
      sourceLibrary = MusicLibrarySources.build(game, config.musicDirectories, library, config.recursive);
      enabledTracks = sourceLibrary.tracks();
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

  private static boolean menuSelection(Context context, AudioSettings settings) {
    return !context.world && settings.menuMusic != AudioSettings.MenuMusic.WORLD
        && !MusicPlaylist.SAME_AS_WORLD.equals(settings.menuPlaylist);
  }

  private static String selectionContext(Context context, AudioSettings settings) {
    String world = "playlist:" + settings.playlist;
    if (!menuSelection(context, settings)) return world;
    return "menu:" + settings.menuMusic + ":" + settings.menuPlaylist + ":" + world;
  }

  private static List<class_267> orderedTracks(List<String> ids) {
    List<class_267> result = new ArrayList<>();
    for (String id : ids) try {
      class_267 track = resolveTrack(id);
      if (track != null) result.add(track);
    } catch (java.io.IOException ignored) { /* Missing IDs remain in the stored selection. */ }
    return result;
  }

  /** Automatic playback reads the selected saved playlists. */
  private static List<class_267> backgroundPool(Context context) {
    updateRotation();
    AudioSettings settings = AudioConfig.current();
    List<class_267> tracks = orderedTracks(MusicPlaylist.rotation(settings, !context.world));
    // Source switches control availability without changing saved membership.
    Set<String> enabled = new HashSet<>();
    enabledTracks.forEach(track -> enabled.add(track.id()));
    Map<String, class_267> unique = new LinkedHashMap<>();
    for (class_267 track : tracks) {
      String id = trackId(track);
      if ((!customById.containsKey(id) || enabled.contains(id))
          && TrackRules.eligible(track.field_2126, context.dimension, context.biome))
        unique.putIfAbsent(id, track);
    }
    return new ArrayList<>(unique.values());
  }

  /** Saved IDs keep their order and remain present when a file is missing. */
  public static List<String> worldMusicSelection(Minecraft mc) {
    return MusicPlaylist.world(AudioConfig.current()).tracks();
  }

  public static Set<String> activeMusic(Minecraft mc) {
    PoolKey key = new PoolKey(AudioConfig.current(), rules(), context(mc), libraryRevision);
    if (key.equals(activeKey)) return activeTracks;
    Set<String> ids = new LinkedHashSet<>();
    backgroundPool(key.context).forEach(t -> ids.add(trackId(t)));
    activeTracks = Collections.unmodifiableSet(ids);
    activeKey = key;
    return activeTracks;
  }

  private static class_267 resolveCurrentTrack(String id) throws java.io.IOException {
    class_267 track = resolveTrack(id);
    MusicLibrary.Track owned = trackMetadata(id);
    return track != null || owned == null || !id.equals(currentTrackId()) ? track : entry(owned);
  }

  public static class_267 resolveTrack(String id) throws java.io.IOException {
    MusicLibrary.Track customTrack = customById.get(id);
    if (customTrack != null) return sourceLibrary().trackSources().containsKey(id) ? entry(customTrack) : null;
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
                MusicLibrary.scan(game, s.musicDirectories, s.recursive),
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
        List<String> errors = new ArrayList<>(library.warnings());
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
    UNDERWATER.context(mc, system);
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
    boolean keepMusic = AudioConfig.current().continuesAcrossMenus();
    if (mc.world != musicWorld) {
      boolean previewPaused = MusicPreview.active() && MusicPreview.paused();
      MusicPreview.worldChanged(mc, system, inWorld == wasWorld || keepMusic);
      if (previewPaused && !MusicPreview.active()) paused = true;
      musicWorld = mc.world;
    }
    if (inWorld != wasWorld) {
      if (!keepMusic) {
        MusicSeeking.cancel(); progress.reset(0,System.nanoTime());
        system.stop("PowerBetaMenu");
        system.stop("BgMusic");
        currentMusic = "";
        menuMusic = "";
        history.clear();
        historyIndex = -1;
        // Context changes never revoke an explicit pause. Resume chooses a new track if needed.
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

  public static class_267 choose() {
    AudioSettings s = AudioConfig.current();
    class_267 requested = queued();
    if (requested != null) { remember(requested); currentMusic = trackId(requested); musicStarted = System.nanoTime(); trackWasPlaying = false; return requested; }
    Context context = context(client);
    class_267 chosen = upcoming();
    if (chosen != null) WORLD.played(selectionContext(context, s), trackId(chosen));
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
    if (upcoming == null || !key.equals(nextKey)) {
      AudioSettings s = AudioConfig.current();
      var pool = backgroundPool(key.context);
      boolean orderChanged = nextKey != null && (nextKey.settings.shuffle != s.shuffle
          || nextKey.settings.avoidRepeats != s.avoidRepeats
          || !selectionContext(nextKey.context, nextKey.settings).equals(selectionContext(key.context, s))
          || !MusicPlaylist.rotation(nextKey.settings, !nextKey.context.world)
              .equals(MusicPlaylist.rotation(s, !key.context.world)));
      // Gain, timing and UI previews preserve reservations; order edits choose the new successor.
      if (upcoming == null || orderChanged || pool.stream().noneMatch(t -> trackId(t).equals(trackId(upcoming))))
        upcoming = WORLD.reserve(selectionContext(key.context, s), pool, s.shuffle, s.avoidRepeats, RANDOM, AudioController::trackId);
      nextKey = key;
    }
    return upcoming;
  }

  static void resuming() { musicStarted = System.nanoTime(); trackWasPlaying = false; }

  public static void resetDelay() {
    if (client != null) nextDelay((SoundManagerAccessor) client.soundManager);
  }

  public static void changingDimension() {
    if (paused || MusicPreview.active() && MusicPreview.paused()) return;
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
    String category = SoundCategoryScope.current();
    SOURCES.put(source, new Source(id, effects > 0 ? volume / effects : 0, ui, category));
    return source.equals("streaming") && MusicPreview.active() ? 0
        : (float) (volume * AudioConfig.current().gain(id, ui, category) * rules().ambient(id));
  }

  public static String musicGroup(String id) {
    return MusicGroups.id(id, trackMetadata(id));
  }

  private static boolean usesMenuVolumes(String id, Context context) {
    return MusicPlaylist.usesMenuVolumes(AudioConfig.current(), id, !context.world);
  }

  private static Map<String, Integer> volumeGroups(String id) {
    AudioSettings settings = AudioConfig.current();
    boolean menu = id.equals(currentMusic) && !MusicPreview.active()
        ? currentUsesMenuVolumes : usesMenuVolumes(id, context(client));
    return MusicPlaylist.groupVolumes(settings, menu ? settings.menuPlaylist : settings.playlist);
  }

  public static float musicVolume(String id) {
    float base = client == null ? 1 : client.options.musicVolume;
    AudioSettings s = AudioConfig.current();
    return base * s.master / 100f * trackVolume(id) / 100f * MusicGroups.gain(volumeGroups(id), musicGroup(id));
  }

  public static float backgroundVolume(float original) {
    if (MusicPreview.active() || paused) return 0;
    AudioSettings s = AudioConfig.current();
    return original * s.master / 100f * trackVolume(currentMusic) / 100f * MusicGroups.gain(volumeGroups(currentMusic), musicGroup(currentMusic));
  }

  private static SoundSystem system() {
    return SoundManagerAccessor.power$started() ? SoundManagerAccessor.power$system() : null;
  }

  /** Called by the native physical-player tick, never by rendering or paused menu ticks. */
  public static void waterTick(net.minecraft.entity.player.ClientPlayerEntity player) {
    Object game = FabricLoader.getInstance().getGameInstance();
    if (!(game instanceof Minecraft mc) || mc.player != player || mc.world != player.world || mc.paused) return;
    client = mc;
    UNDERWATER.tick(mc, system(), player);
  }

  public static void waterSample(net.minecraft.entity.player.ClientPlayerEntity player) {
    Object game = FabricLoader.getInstance().getGameInstance();
    if (!(game instanceof Minecraft mc) || mc.player != player || mc.world != player.world || mc.paused) return;
    client = mc;
    UNDERWATER.sample(mc, system(), player);
  }

  static float waterVolume(String event, float base) {
    return client == null ? 0 : (float)(base * client.options.soundVolume * AudioConfig.current().gain(event, false));
  }

  public static void refresh() {
    UNDERWATER.refresh();
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
                  * AudioConfig.current().gain(source.sound, source.ui, source.category)
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
    if (MusicPreview.active()) {
      MusicPreview.stop(system(), true);
      return;
    }
    paused = false;
    if (!MusicRequests.tracks().isEmpty()) { playQueue(); return; }
    if (historyIndex + 1 < history.size()) { play(history.get(++historyIndex)); return; }
    play(choose());
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
    for (var track : sourceLibrary().tracks()) ids.add(track.id());
    return ids;
  }
}
