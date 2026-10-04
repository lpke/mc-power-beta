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
  private static final TrackSelector WORLD = new TrackSelector(), MENU = new TrackSelector();
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
  private static boolean dirty = true, paused, wasWorld, rescan, nextAfterScan;
  private static String currentMusic = "",
      menuMusic = "",
      status = "Music folders have not been scanned";
  private static int cleanup, menuCooldown, libraryRevision;

  public static int libraryRevision() { return libraryRevision; }
  private static final List<class_267> history = new ArrayList<>();
  private static int historyIndex = -1;
  private static long musicStarted;

  private static void remember(class_267 track) {
    while (history.size() > historyIndex + 1) history.remove(history.size() - 1);
    history.add(track);
    if (history.size() > 128) history.remove(0);
    historyIndex = history.size() - 1;
  }

  private static void play(class_267 track) {
    SoundSystem system = system();
    if (system == null || client == null) return;
    if (track == null || rules().disabled()) {
      system.stop("BgMusic"); system.stop("PowerBetaMenu");
      currentMusic = ""; menuMusic = "";
      return;
    }
    system.stop("BgMusic");
    system.stop("PowerBetaMenu");
    menuMusic = "";
    currentMusic = "music:" + track.field_2126;
    paused = false;
    int dimension = client.player == null ? 0 : client.player.dimensionId;
    LegacyMusic.selected(track.field_2126, client.world != null && TrackRules.dimensionSpecific(track.field_2126, dimension) ? dimension : Integer.MAX_VALUE);
    system.backgroundMusic("BgMusic", track.field_2127, track.field_2126, false);
    system.setVolume("BgMusic", musicVolume(currentMusic));
    system.play("BgMusic");
    musicStarted = System.nanoTime();
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
    system.play("PowerBetaPreview");
  }

  private static void previewMusic(String id) {
    var pool = ((SoundManagerAccessor) client.soundManager).power$music();
    class_267 selected = null;
    synchronized (pool) {
      for (class_267 track : ((SoundPoolAccessor) pool).power$tracks())
        if (id.equals("music:" + track.field_2126)) { selected = track; break; }
    }
    try {
      if (selected == null)
        for (var track : customTracks())
          if (id.equals("music:" + track.name()) && Files.isRegularFile(track.path())) {
            selected = new class_267(track.name(), track.path().toUri().toURL()); break;
          }
      if (selected == null) { status = "Track unavailable. Reload music folders."; return; }
      system().stop("PowerBetaPreview");
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
    nextAfterScan = true;
  }

  public static void rulesChanged() {
    MusicRules before = rules();
    rules = MusicRules.read();
    if (before.vanillaDisabled() != rules.vanillaDisabled() || before.disabled() != rules.disabled()) {
      history.clear(); historyIndex = -1; next();
    }
    refresh();
  }

  public static MusicRules rules() {
    if (rules == null) rules = MusicRules.read();
    return rules;
  }

  public static String status() {
    if (MusicPreview.active()) return "Previewing: " + MusicPreview.track().substring(6);
    return paused
        ? "Music paused"
        : nowPlaying().equals("none") ? status : "Playing: " + nowPlaying();
  }

  public static List<MusicLibrary.Track> customTracks() {
    List<MusicLibrary.Track> tracks = new ArrayList<>(library.world.tracks());
    tracks.addAll(library.menu.tracks());
    return List.copyOf(tracks);
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
    if (dirty) {
      dirty = false;
      rules = MusicRules.read();
      reload();
      refresh();
    }
    if (pending != null && pending.isDone()) {
      try {
        library = pending.join();
        libraryRevision++;
        List<String> errors = new ArrayList<>(library.world.warnings());
        errors.addAll(library.menu.warnings());
        status =
            library.world.tracks().size()
                + " world tracks; "
                + library.menu.tracks().size()
                + " menu tracks";
        if (!errors.isEmpty()) {
          status += "; " + errors.get(0);
          errors.forEach(PowerBeta.LOG::warn);
        }
      } catch (RuntimeException e) {
        PowerBeta.LOG.error("Music scan failed", e);
        status = "Music scan failed. Existing playlist retained.";
      }
      pending = null;
      if (rescan) {
        rescan = false;
        reload();
      } else if (nextAfterScan) {
        nextAfterScan = false;
        next();
      }
    }
    SoundSystem system = system();
    if (system == null) return;
    MusicDisplay.update(nowPlaying());
    boolean inWorld = mc.world != null;
    if (inWorld != wasWorld) {
      system.stop("PowerBetaMenu");
      system.stop("BgMusic");
      currentMusic = "";
      menuMusic = "";
      history.clear();
      historyIndex = -1;
      paused = false;
      wasWorld = inWorld;
    }
    MusicPreview.tick(mc, system);
    if (MusicPreview.active() || paused) return;
    if (rules().disabled() || AudioConfig.current().master == 0) {
      system.stop("BgMusic");
      system.stop("PowerBetaMenu");
      return;
    }
    if (!rules().menuEnabled() || inWorld) system.stop("PowerBetaMenu");
    if (menuCooldown > 0) menuCooldown--;
    if (menuCooldown == 0
        && !inWorld
        && rules().menuEnabled()
        && !library.menu.tracks().isEmpty()
        && !system.playing("PowerBetaMenu")
        && !system.playing("streaming")
        && mc.options.musicVolume > 0) {
      if (!system.playing("BgMusic")) {
        MusicLibrary.Track track =
            MENU.choose(
                library.menu.tracks(),
                AudioConfig.current().shuffle,
                AudioConfig.current().avoidRepeats,
                RANDOM,
                t -> t.path().toString());
        try {
          menuCooldown = 20;
          class_267 selected = new class_267(track.name(), track.path().toUri().toURL());
          remember(selected);
          play(selected);
        } catch (Exception e) {
          PowerBeta.LOG.warn("Could not play menu music", e);
        }
      }
    }
    if (++cleanup >= 100) {
      cleanup = 0;
      SOURCES.keySet().removeIf(source -> !system.playing(source));
    }
  }

  public static boolean blockBackground() {
    SoundSystem s = system();
    if (LegacyMusic.consumeStop() && s != null) {
      s.stop("BgMusic");
      currentMusic = "";
    }
    return MusicPreview.active() || paused
        || (client != null && client.world == null && rules().menuEnabled() && rules().menuOverrides() && !library.menu.tracks().isEmpty())
        || rules().disabled()
        || AudioConfig.current().master == 0
        || (s != null && s.playing("PowerBetaMenu"));
  }

  public static class_267 choose(List<class_267> vanilla) {
    AudioSettings s = AudioConfig.current();
    List<class_267> tracks = new ArrayList<>();
    if (s.musicMode != AudioSettings.MusicMode.REPLACE || library.world.tracks().isEmpty())
      if (!rules().vanillaDisabled()) tracks.addAll(vanilla);
    if (s.musicMode != AudioSettings.MusicMode.VANILLA)
      for (var track : library.world.tracks())
        try {
          tracks.add(new class_267(track.name(), track.path().toUri().toURL()));
        } catch (Exception ignored) {
        }
    int dimension = client != null && client.player != null ? client.player.dimensionId : 0;
    String biome = null;
    if (client != null && client.player != null && client.world != null) {
      var currentBiome =
          client
              .world
              .method_1781()
              .method_1787((int) Math.floor(client.player.x), (int) Math.floor(client.player.z));
      if (currentBiome != null) biome = currentBiome.field_888;
    }
    String currentBiome = biome;
    tracks.removeIf(track -> !TrackRules.eligible(track.field_2126, dimension, currentBiome));
    class_267 chosen =
        WORLD.choose(tracks, s.shuffle, s.avoidRepeats, RANDOM, t -> t.field_2127.toExternalForm());
    if (chosen != null) {
      remember(chosen);
      currentMusic = "music:" + chosen.field_2126;
      LegacyMusic.selected(
          chosen.field_2126,
          TrackRules.dimensionSpecific(chosen.field_2126, dimension)
              ? dimension
              : Integer.MAX_VALUE);
    } else if (client != null) {
      ((SoundManagerAccessor) client.soundManager).power$countdown(20);
    }
    return chosen;
  }

  public static void nextDelay(SoundManagerAccessor sound) {
    MusicRules r = rules();
    sound.power$countdown(r.delayMin() + RANDOM.nextInt(r.delayRandom()));
  }

  public static float mix(String source, String id, float volume, boolean ui) {
    if (source.equals("streaming")) LegacyMusic.record(id.replaceFirst("^records\\.", ""));
    float effects = client == null ? 1 : client.options.soundVolume;
    SOURCES.put(source, new Source(id, effects > 0 ? volume / effects : 0, ui));
    return (float) (volume * AudioConfig.current().gain(id, ui) * rules().ambient(id));
  }

  public static float musicVolume(String id) {
    float base = client == null ? 1 : client.options.musicVolume;
    AudioSettings s = AudioConfig.current();
    return base * s.master / 100f * s.sounds.getOrDefault(id, 100) / 100f;
  }

  public static float backgroundVolume(float original) {
    AudioSettings s = AudioConfig.current();
    return original * s.master / 100f * s.sounds.getOrDefault(currentMusic, 100) / 100f;
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
          (float)
              (source.base
                  * effects
                  * AudioConfig.current().gain(source.sound, source.ui)
                  * rules().ambient(source.sound)));
    }
    if (client != null) {
      s.setVolume("BgMusic", musicVolume(currentMusic));
      s.setVolume("PowerBetaMenu", musicVolume(menuMusic));
      if (MusicPreview.active()) s.setVolume(MusicPreview.SOURCE, musicVolume(MusicPreview.track()));
    }
  }

  public static String nowPlaying() {
    SoundSystem s = system();
    if (s == null) return "none";
    if (s.playing("PowerBetaMenu")) return menuMusic.replaceFirst("^music:", "");
    if (s.playing("BgMusic")) return currentMusic.replaceFirst("^music:", "");
    if (s.playing("streaming")) {
      Source record = SOURCES.get("streaming");
      return record == null ? "Music disc" : record.sound.replaceFirst("^records\\.", "");
    }
    return "none";
  }

  public static void togglePause() {
    SoundSystem s = system();
    if (s == null) return;
    boolean resumedPreview = paused ? MusicPreview.stop(s, true) : MusicPreview.pause(s);
    if (!paused && !resumedPreview && !s.playing("BgMusic") && !s.playing("PowerBetaMenu")) {
      next();
      return;
    }
    paused = !paused;
    if (paused) {
      s.pause("BgMusic");
      s.pause("PowerBetaMenu");
    } else {
      if (!currentMusic.isEmpty()) s.play("BgMusic");
      if (!menuMusic.isEmpty() && (client == null || client.world == null)) s.play("PowerBetaMenu");
    }
  }

  public static void next() {
    if (client == null || system() == null) return;
    MusicPreview.stop(system(), true);
    if (rules().disabled()) { play(null); return; }
    paused = false;
    if (historyIndex + 1 < history.size()) { play(history.get(++historyIndex)); return; }
    if (client.world == null && rules().menuEnabled() && !library.menu.tracks().isEmpty()) {
      var track = MENU.choose(library.menu.tracks(), AudioConfig.current().shuffle,
          AudioConfig.current().avoidRepeats, RANDOM, t -> t.path().toString());
      try {
        var selected = new class_267(track.name(), track.path().toUri().toURL());
        remember(selected);
        play(selected);
      } catch (Exception e) { PowerBeta.LOG.warn("Could not play music", e); }
      return;
    }
    var pool = ((SoundManagerAccessor) client.soundManager).power$music();
    List<class_267> tracks;
    synchronized (pool) { tracks = List.copyOf(((SoundPoolAccessor) pool).power$tracks()); }
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
    var pool = ((SoundManagerAccessor) mc.soundManager).power$music();
    synchronized (pool) {
      for (var track : ((SoundPoolAccessor) pool).power$tracks())
        ids.add("music:" + track.field_2126);
    }
    for (var track : customTracks()) ids.add("music:" + track.name());
    return ids;
  }
}
