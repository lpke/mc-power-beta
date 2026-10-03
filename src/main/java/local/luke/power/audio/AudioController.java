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
  private static boolean dirty = true, paused, wasWorld, rescan;
  private static String currentMusic = "",
      menuMusic = "",
      status = "Music folders have not been scanned";
  private static int cleanup, menuCooldown;

  private AudioController() {}

  public static void settingsChanged() {
    dirty = true;
  }

  public static MusicRules rules() {
    if (rules == null) rules = MusicRules.read();
    return rules;
  }

  public static String status() {
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
      }
    }
    SoundSystem system = system();
    if (system == null) return;
    MusicDisplay.update(nowPlaying());
    boolean inWorld = mc.world != null;
    if (inWorld != wasWorld) {
      system.stop("PowerBetaMenu");
      menuMusic = "";
      wasWorld = inWorld;
    }
    if (paused) return;
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
      if (!system.playing("BgMusic") || rules().menuOverrides()) {
        MusicLibrary.Track track =
            MENU.choose(
                library.menu.tracks(),
                AudioConfig.current().shuffle,
                AudioConfig.current().avoidRepeats,
                RANDOM,
                t -> t.path().toString());
        try {
          menuCooldown = 20;
          system.stop("BgMusic");
          menuMusic = "music:" + track.name();
          system.backgroundMusic(
              "PowerBetaMenu", track.path().toUri().toURL(), track.name(), false);
          system.setVolume("PowerBetaMenu", musicVolume(menuMusic));
          system.play("PowerBetaMenu");
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
    return paused
        || rules().disabled()
        || AudioConfig.current().master == 0
        || (s != null && s.playing("PowerBetaMenu"));
  }

  public static class_267 choose(List<class_267> vanilla) {
    AudioSettings s = AudioConfig.current();
    List<class_267> tracks = new ArrayList<>();
    if (s.musicMode != AudioSettings.MusicMode.REPLACE || library.world.tracks().isEmpty())
      tracks.addAll(vanilla);
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
    if (!paused && !s.playing("BgMusic") && !s.playing("PowerBetaMenu")) {
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
    SoundSystem s = system();
    if (s != null) {
      s.stop("BgMusic");
      s.stop("PowerBetaMenu");
    }
    paused = false;
    if (client != null) ((SoundManagerAccessor) client.soundManager).power$countdown(0);
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
