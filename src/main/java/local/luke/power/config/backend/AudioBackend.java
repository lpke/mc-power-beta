package local.luke.power.config.backend;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import net.minecraft.client.Minecraft;

public final class AudioBackend implements Backend {
  private final List<Setting> entries = new ArrayList<>();

  public AudioBackend(Minecraft mc) {
    AudioSettings s = AudioConfig.copy();
    add("redstonePlacementSound", "Extra sounds", "Redstone placement sound", Setting.Kind.BOOLEAN,
        new JsonPrimitive(s.redstonePlacementSound), new JsonPrimitive(new AudioSettings().redstonePlacementSound),
        "Play the stone placement sound when placing redstone dust. Uses the Blocks sound volume.", List.of());
    extra("seedPlantingSounds", "Seed planting sounds", s.seedPlantingSounds,
        "Play crop planting sounds after planting seeds in singleplayer. Uses the Blocks sound volume.");
    extra("hoeTillingSounds", "Hoe tilling sounds", s.hoeTillingSounds,
        "Use modern hoe sounds after tilling soil in singleplayer. Uses the Blocks sound volume.");
    extra("bucketSounds", "Water bucket sounds", s.bucketSounds,
        "Play water bucket and cow milking sounds in singleplayer. Uses Players, Blocks or Passive creatures volume.");
    extra("lavaBucketSounds", "Lava bucket sounds", s.lavaBucketSounds,
        "Play lava bucket filling and emptying sounds in singleplayer. Uses Players or Blocks volume.");
    extra("boneMealSounds", "Bone meal sounds", s.boneMealSounds,
        "Play bone meal sounds after fertilizing plants in singleplayer. Uses the Blocks sound volume.");
    extra("lavaPopSounds", "Lava pop sounds", s.lavaPopSounds,
        "Play modern lava pops when an exposed lava surface emits a particle in singleplayer. Uses Ambient volume.");
    extra("lavaAmbientSounds", "Lava ambience", s.lavaAmbientSounds,
        "Play modern bubbling at exposed lava surfaces in singleplayer. Uses Ambient volume.");
    extra("playerFireExtinguish", "Player fire extinguish", s.playerFireExtinguish,
        "Play the modern extinguishing sound when water puts out your fire in singleplayer. Uses Players volume.");
    extra("waterSplashSounds", "Water splash sounds", s.waterSplashSounds,
        "Use modern water entry sounds for players, creatures, items and vehicles in singleplayer. Uses each entity sound category.");
    extra("swimmingSounds", "Swimming sounds", s.swimmingSounds,
        "Play modern swimming sounds as you move through water in singleplayer. Uses the Players sound volume.");
    extra("underwaterEnterSounds", "Submerging sounds", s.underwaterEnterSounds,
        "Play the modern entry sound when your eyes enter water in singleplayer. Uses Ambient volume.");
    extra("underwaterExitSounds", "Surfacing sounds", s.underwaterExitSounds,
        "Play the modern exit sound when your eyes leave water in singleplayer. Uses Ambient volume.");
    extra("underwaterAmbience", "Underwater ambience", s.underwaterAmbience,
        "Fade modern background water ambience in and out as your eyes cross the surface. Uses Ambient volume.");
    extra("underwaterAdditions", "Underwater bubbles and water", s.underwaterAdditions,
        "Play occasional modern bubbles and water sounds while underwater. Uses Ambient volume.");
    extra("underwaterRareAdditions", "Rare underwater sounds", s.underwaterRareAdditions,
        "Play rare modern underwater animal, creaking and dripping sounds. Uses Ambient volume.");
    extra("underwaterUltraRareAdditions", "Very rare underwater sounds", s.underwaterUltraRareAdditions,
        "Play very rare modern deep underwater sounds. Uses Ambient volume.");
    add(
        "master",
        "Volume",
        "Master volume",
        Setting.Kind.INTEGER,
        new JsonPrimitive(s.master),
        new JsonPrimitive(100),
        "Multiplies Music, Game sounds and every category.",
        List.of());
    for (var e : s.categories.entrySet())
      add(
          "category." + e.getKey(),
          "Volume",
          "Game sounds / " + Catalog.words(e.getKey()),
          Setting.Kind.INTEGER,
          new JsonPrimitive(e.getValue()),
          new JsonPrimitive(100),
          "Volume in percent, multiplied by Master volume and Game sounds.",
          List.of());
    add("playlist", "Music playback", "World playlist", Setting.Kind.TEXT,
        new JsonPrimitive(s.playlist), new JsonPrimitive(MusicPlaylist.BETA),
        "Choose Beta 1.7.3 or a saved playlist for world playback.", List.of());
    add("menuPlaylist", "Music playback", "Menu playlist", Setting.Kind.TEXT,
        new JsonPrimitive(s.menuPlaylist), new JsonPrimitive(MusicPlaylist.SAME_AS_WORLD),
        "Use the world playlist, Beta 1.7.3 or a saved playlist for menu music.", List.of());
    add("playlists", "Music data", "Music playlists", Setting.Kind.LIST,
        Catalog.JSON.toJsonTree(s.playlists), new JsonArray(), "Saved ordered music selections.", List.of());
    add("favourites", "Music data", "Favorite tracks", Setting.Kind.LIST,
        Catalog.JSON.toJsonTree(s.favourites), new JsonArray(), "Starred music tracks.", List.of());
    add("favoritesGroupVolumes", "Music data", "Favorites group volumes", Setting.Kind.LIST,
        Catalog.JSON.toJsonTree(s.favoritesGroupVolumes), new JsonObject(), "Group volumes for Favorites.", List.of());
    add("libraryOrder", "Music data", "Library group order", Setting.Kind.LIST,
        Catalog.JSON.toJsonTree(s.libraryOrder), new JsonArray(), "Display order of library groups.", List.of());
    add("dimensionMusic", "Music playback", "On dimension change", Setting.Kind.CHOICE,
        new JsonPrimitive(s.dimensionMusic.ordinal()), new JsonPrimitive(0),
        "Continue music, end only dimension-specific custom tracks, or end any track when using a portal.",
        List.of("Continue", "Stop dimension tracks", "Stop all"));
    add(
        "musicDirectories",
        "Music data",
        "Music folders",
        Setting.Kind.LIST,
        MusicFolders.encode(s.musicDirectories),
        new JsonArray(),
        "Import OGG, WAV, MUS or MP3 folders. Removing a folder preserves its files and saved playlist membership. Convert MP3 in the library; original files stay intact.",
        List.of());
    add(
        "shuffle",
        "Music playback",
        "Shuffle tracks",
        Setting.Kind.BOOLEAN,
        new JsonPrimitive(s.shuffle),
        new JsonPrimitive(true),
        "Off follows the saved playlist order.",
        List.of());
    add(
        "avoidRepeats",
        "Music playback",
        "Avoid consecutive repeats",
        Setting.Kind.BOOLEAN,
        new JsonPrimitive(s.avoidRepeats),
        new JsonPrimitive(true),
        "Prevents the same track playing twice when another is available.",
        List.of());
    add("delayQueuedTracks", "Music gaps", "Wait before queued tracks", Setting.Kind.BOOLEAN,
        new JsonPrimitive(s.delayQueuedTracks), new JsonPrimitive(false),
        "Queued tracks use the same random delay as other music. Requires Wait between tracks.", List.of());
    add("waitBetweenTracks", "Music gaps", "Wait between tracks", Setting.Kind.BOOLEAN,
        new JsonPrimitive(s.waitBetweenTracks), new JsonPrimitive(true),
        "Use the music delay settings between tracks. Off plays tracks back to back, including the queue.", List.of());
    for (boolean minimum : new boolean[]{true, false}) {
      String key = minimum ? "gapMinSeconds" : "gapMaxSeconds";
      entries.add(new Setting("audio." + key, id(), "Audio", "Music gaps",
          (minimum ? "Minimum" : "Maximum") + " gap (seconds)",
          "A random gap within the minimum and maximum is chosen after each track. Zero allows immediate playback.",
          Setting.Kind.INTEGER, new JsonPrimitive(minimum ? s.gapMinSeconds : s.gapMaxSeconds),
          new JsonPrimitive(minimum ? 600 : 1200), 0, 86400, 1, List.of(), false));
    }
    add("menuControls", "Music menu controls", "Show music controls", Setting.Kind.BOOLEAN,
        new JsonPrimitive(s.menuControls), new JsonPrimitive(true),
        "Show play/pause, previous, next, quiet and track information on menus.", List.of());
    add("menuControlsMainMenu", "Music menu controls", "Show in main menu", Setting.Kind.BOOLEAN,
        new JsonPrimitive(s.menuControlsMainMenu), new JsonPrimitive(true),
        "Also show the controls on the title screen when Show music controls is on.", List.of());
    add("menuControlsScrub", "Music menu controls", "Show scrub bar", Setting.Kind.BOOLEAN,
        new JsonPrimitive(s.menuControlsScrub), new JsonPrimitive(false),
        "Show playback position and seek within the current song on menus.", List.of());
    add("menuControlsPosition", "Music menu controls", "Position", Setting.Kind.CHOICE,
        new JsonPrimitive(s.menuControlsPosition.ordinal()), new JsonPrimitive(1),
        "Anchor the music panel beside the menu or at a screen edge. Offsets adjust its position.",
        List.of("Above menu", "Below menu", "Top left", "Top center", "Top right",
            "Middle left", "Middle right", "Bottom left", "Bottom center", "Bottom right"));
    for (boolean horizontal : new boolean[]{true, false}) {
      String key = horizontal ? "menuControlsOffsetX" : "menuControlsOffsetY";
      entries.add(new Setting("audio." + key, id(), "Audio", "Music menu controls",
          horizontal ? "Horizontal offset" : "Vertical offset",
          horizontal ? "Move right with positive values, left with negative values. Uses scaled GUI pixels."
              : "Move down with positive values, up with negative values. Uses scaled GUI pixels.",
          Setting.Kind.INTEGER, new JsonPrimitive(horizontal ? s.menuControlsOffsetX : s.menuControlsOffsetY),
          new JsonPrimitive(0), -4096, 4096, 1, List.of(), false));
    }
    Set<String> sounds = AudioController.sounds(mc);

    for (String sound : sounds) {
      if (sound.startsWith("music:")) continue;
      if (Set.of("portal.portal", "ambient.weather.rain", "ambient.cave.cave", "mob.ghast.moan")
          .contains(sound)) continue;
      add(
          "sound." + sound,
          "Game sounds / All",
          Catalog.words(sound.substring(sound.indexOf(':') + 1).replace('.', ' ')),
          Setting.Kind.INTEGER,
          new JsonPrimitive(s.sounds.getOrDefault(sound, 100)),
          new JsonPrimitive(100),
          "Volume in percent, multiplied by its category, Game sounds and Master volume.",
          List.of());
    }
    entries.addAll(musicEntries(mc));
  }

  private static List<Setting> musicEntries(Minecraft mc) {
    List<Setting> result = new ArrayList<>();
    AudioSettings settings = AudioConfig.current();
    Set<String> referenced = new LinkedHashSet<>(AudioController.music(mc));
    referenced.addAll(MusicRequests.tracks());
    for (String track : settings.musicTrackIds(referenced)) {
      String label = AudioController.musicLabel(track);
      result.add(new Setting("audio.sound." + track, "audio", "Audio", "Individual music tracks", label,
          "Track volume in percent, multiplied by the music and master volumes.",
          Setting.Kind.INTEGER, new JsonPrimitive(AudioController.trackVolume(track)),
          new JsonPrimitive(100), 0, 100, 5, List.of(), false));
    }
    return result;
  }

  public static boolean discoverMusic(ConfigSession session, Minecraft mc) {
    return session.addDiscovered(musicEntries(mc));
  }

  private void add(
      String key,
      String group,
      String label,
      Setting.Kind kind,
      JsonElement value,
      JsonElement defaults,
      String description,
      List<String> choices) {
    entries.add(
        new Setting(
            "audio." + key,
            id(),
            "Audio",
            group,
            label,
            description,
            kind,
            value,
            defaults,
            0,
            100,
            5,
            choices,
            false));
  }

  public static void register(ConfigSession s, Minecraft mc) {
    AudioBackend b = new AudioBackend(mc);
    s.add(b, b.entries);
  }

  private void extra(String key, String label, boolean value, String description) {
    add(key, "Extra sounds", label, Setting.Kind.BOOLEAN, new JsonPrimitive(value),
        new JsonPrimitive(false), description, List.of());
  }

  public String id() {
    return "audio";
  }

  public List<Path> files() {
    return List.of(AudioConfig.path(), local.luke.power.storage.PowerConfig.musicPath());
  }

  private AudioSettings draft(Map<String, JsonElement> changes) {
    AudioSettings s = AudioConfig.copy();
    if (changes.containsKey("audio.playlist")) s.playlist = changes.get("audio.playlist").getAsString();
    if (changes.containsKey("audio.menuPlaylist")) s.menuPlaylist = changes.get("audio.menuPlaylist").getAsString();
    for (var e : changes.entrySet()) {
      String key = e.getKey().substring(6);
      JsonElement v = e.getValue();
      if (key.startsWith("category.")) s.categories.put(key.substring(9), v.getAsInt());
      else if (key.startsWith("sound.")) {
        String sound = key.substring(6);
        if (v.getAsInt() == 100 && !sound.startsWith("music:custom/")) s.sounds.remove(sound);
        else s.sounds.put(sound, v.getAsInt());
      } else
        switch (key) {
          case "redstonePlacementSound" -> s.redstonePlacementSound = v.getAsBoolean();
          case "seedPlantingSounds" -> s.seedPlantingSounds = v.getAsBoolean();
          case "hoeTillingSounds" -> s.hoeTillingSounds = v.getAsBoolean();
          case "bucketSounds" -> s.bucketSounds = v.getAsBoolean();
          case "lavaBucketSounds" -> s.lavaBucketSounds = v.getAsBoolean();
          case "boneMealSounds" -> s.boneMealSounds = v.getAsBoolean();
          case "lavaPopSounds" -> s.lavaPopSounds = v.getAsBoolean();
          case "lavaAmbientSounds" -> s.lavaAmbientSounds = v.getAsBoolean();
          case "playerFireExtinguish" -> s.playerFireExtinguish = v.getAsBoolean();
          case "waterSplashSounds" -> s.waterSplashSounds = v.getAsBoolean();
          case "swimmingSounds" -> s.swimmingSounds = v.getAsBoolean();
          case "underwaterEnterSounds" -> s.underwaterEnterSounds = v.getAsBoolean();
          case "underwaterExitSounds" -> s.underwaterExitSounds = v.getAsBoolean();
          case "underwaterAmbience" -> s.underwaterAmbience = v.getAsBoolean();
          case "underwaterAdditions" -> s.underwaterAdditions = v.getAsBoolean();
          case "underwaterRareAdditions" -> s.underwaterRareAdditions = v.getAsBoolean();
          case "underwaterUltraRareAdditions" -> s.underwaterUltraRareAdditions = v.getAsBoolean();
          case "playlist" -> s.playlist = v.getAsString();
          case "playlists" -> s.playlists = new ArrayList<>(Arrays.asList(Catalog.JSON.fromJson(v, MusicPlaylist[].class)));
          case "menuPlaylist" -> s.menuPlaylist = v.getAsString();
          case "favourites" -> {
            List<String> tracks = Arrays.asList(Catalog.JSON.fromJson(v, String[].class));
            MusicPlaylist.validateTracks(tracks);
            s.favourites = new LinkedHashSet<>(tracks);
          }
          case "favoritesGroupVolumes" -> {
            Map<String, Integer> volumes = new TreeMap<>();
            v.getAsJsonObject().entrySet().forEach(entry -> volumes.put(entry.getKey(), entry.getValue().getAsInt()));
            s.favoritesGroupVolumes = MusicGroups.copy(volumes);
          }
          case "libraryOrder" -> s.libraryOrder = new ArrayList<>(Arrays.asList(Catalog.JSON.fromJson(v, String[].class)));
          case "menuControls" -> s.menuControls = v.getAsBoolean();
          case "menuControlsMainMenu" -> s.menuControlsMainMenu = v.getAsBoolean();
          case "menuControlsScrub" -> s.menuControlsScrub = v.getAsBoolean();
          case "menuControlsPosition" -> s.menuControlsPosition = AudioSettings.MenuControlsPosition.values()[v.getAsInt()];
          case "menuControlsOffsetX" -> s.menuControlsOffsetX = v.getAsInt();
          case "menuControlsOffsetY" -> s.menuControlsOffsetY = v.getAsInt();
          case "master" -> s.master = v.getAsInt();
          case "dimensionMusic" -> s.dimensionMusic = AudioSettings.DimensionMusic.values()[v.getAsInt()];
          case "gapMinSeconds" -> s.gapMinSeconds = v.getAsInt();
          case "gapMaxSeconds" -> s.gapMaxSeconds = v.getAsInt();
          case "waitBetweenTracks" -> s.waitBetweenTracks = v.getAsBoolean();
          case "delayQueuedTracks" -> s.delayQueuedTracks = v.getAsBoolean();
          case "shuffle" -> s.shuffle = v.getAsBoolean();
          case "avoidRepeats" -> s.avoidRepeats = v.getAsBoolean();
          case "musicDirectories" -> {
            s.musicDirectories = new ArrayList<>(MusicFolders.decode(v));
          }
          default -> throw new IllegalArgumentException("Unknown audio setting");
        }
    }
    s.validate();
    return s;
  }

  public void validate(Map<String, JsonElement> changes) {
    draft(changes);
  }

  public boolean previews(Setting s) { return true; }

  public void preview(Map<String, JsonElement> changes) {
    AudioConfig.preview(draft(changes));
  }

  public void apply(Map<String, JsonElement> changes) throws Exception {
    AudioConfig.save(draft(changes));
  }
}
