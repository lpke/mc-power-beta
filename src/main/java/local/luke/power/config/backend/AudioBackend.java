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
    add(
        "master",
        "Volume",
        "Master volume",
        Setting.Kind.INTEGER,
        new JsonPrimitive(s.master),
        new JsonPrimitive(100),
        "Multiplies music, sound effects and every category.",
        List.of());
    for (var e : s.categories.entrySet())
      add(
          "category." + e.getKey(),
          "Sound categories",
          Catalog.words(e.getKey()),
          Setting.Kind.INTEGER,
          new JsonPrimitive(e.getValue()),
          new JsonPrimitive(100),
          "Volume in percent, multiplied by master and sound-effects volume.",
          List.of());
    add("preset", "Music library", "World music preset", Setting.Kind.TEXT,
        new JsonPrimitive(s.preset), new JsonPrimitive(""),
        "Load a saved soundtrack selection. Loading replaces current track exclusions and group volumes.", List.of());
    add("presets", "Music data", "Music presets", Setting.Kind.LIST,
        Catalog.JSON.toJsonTree(s.presets), new JsonArray(), "Saved soundtrack selections.", List.of());
    add("exclusions", "Music data", "Excluded tracks", Setting.Kind.LIST,
        Catalog.JSON.toJsonTree(s.disabledTracks), new JsonArray(), "Excluded music identifiers.", List.of());
    add("groupVolumes", "Music data", "Music group volumes", Setting.Kind.LIST,
        Catalog.JSON.toJsonTree(s.groupVolumes), new JsonObject(), "Volume multipliers for music groups.", List.of());
    add("favourites", "Music data", "Favourite tracks", Setting.Kind.LIST,
        Catalog.JSON.toJsonTree(s.favourites), new JsonArray(), "Starred music tracks.", List.of());
    add(
        "musicMode",
        "Music library",
        "World music",
        Setting.Kind.CHOICE,
        new JsonPrimitive(s.musicMode.ordinal()),
        new JsonPrimitive(0),
        "Survival uses in-game survival music. All also includes creative, menu, records and credits. Nether and End music are available only in the library and presets.",
        List.of("Vanilla", "Alpha and Beta survival", "Alpha and Beta all", "Minecraft survival", "Minecraft all"));
    add("customMusic", "Music library", "Custom music handling", Setting.Kind.CHOICE,
        new JsonPrimitive(s.customMusic.ordinal()), new JsonPrimitive(0),
        "Add folder tracks to the world soundtrack, or replace it with folder tracks. Empty folders fall back to World music.",
        List.of("Off", "Add to soundtrack", "Custom only"));
    add("menuMusic", "Music library", "Menu music", Setting.Kind.CHOICE,
        new JsonPrimitive(s.menuMusic.ordinal()), new JsonPrimitive(0),
        "Use World music in menus, prefer menu folders, or mix both pools. Empty menu folders fall back to World music.",
        List.of("World soundtrack", "Menu folders", "Mix both"));
    add("dimensionMusic", "Music library", "On dimension change", Setting.Kind.CHOICE,
        new JsonPrimitive(s.dimensionMusic.ordinal()), new JsonPrimitive(0),
        "Continue music, end only dimension-specific custom tracks, or end any track when using a portal.",
        List.of("Continue", "Stop dimension tracks", "Stop all"));
    add(
        "musicDirectories",
        "Music library",
        "World music folders",
        Setting.Kind.LIST,
        MusicFolders.encode(s.musicDirectories, s.disabledMusicDirectories),
        new JsonArray(),
        "Choose OGG, WAV, MUS or MP3 folders. Switch folders On or Off without removing them. Convert MP3 in the library; original files stay intact.",
        List.of());
    add(
        "menuDirectories",
        "Music library",
        "Menu music folders",
        Setting.Kind.LIST,
        MusicFolders.encode(s.menuDirectories, s.disabledMenuDirectories),
        new JsonArray(),
        "Used when menu music is enabled. Each folder has its own On/Off switch. Supports OGG, WAV, MUS and converted MP3.",
        List.of());
    add(
        "recursive",
        "Music library",
        "Include subfolders",
        Setting.Kind.BOOLEAN,
        new JsonPrimitive(s.recursive),
        new JsonPrimitive(false),
        "Scan up to 12 folder levels. Symbolic links are not followed.",
        List.of());
    add(
        "shuffle",
        "Music library",
        "Shuffle tracks",
        Setting.Kind.BOOLEAN,
        new JsonPrimitive(s.shuffle),
        new JsonPrimitive(true),
        "Off plays tracks in filename order.",
        List.of());
    add(
        "avoidRepeats",
        "Music library",
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
    add("menuControls", "Pause menu music", "Show music controls", Setting.Kind.BOOLEAN,
        new JsonPrimitive(s.menuControls), new JsonPrimitive(false),
        "Show play/pause, previous, next, quiet and track information on the pause menu.", List.of());
    add("menuControlsPosition", "Pause menu music", "Position", Setting.Kind.CHOICE,
        new JsonPrimitive(s.menuControlsPosition.ordinal()), new JsonPrimitive(1),
        "Anchor the music panel beside the menu or at a screen edge. Offsets adjust its position.",
        List.of("Above menu", "Below menu", "Top left", "Top center", "Top right",
            "Middle left", "Middle right", "Bottom left", "Bottom center", "Bottom right"));
    for (boolean horizontal : new boolean[]{true, false}) {
      String key = horizontal ? "menuControlsOffsetX" : "menuControlsOffsetY";
      entries.add(new Setting("audio." + key, id(), "Audio", "Pause menu music",
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
          "Individual sounds",
          Catalog.words(sound.substring(sound.indexOf(':') + 1).replace('.', ' ')),
          Setting.Kind.INTEGER,
          new JsonPrimitive(s.sounds.getOrDefault(sound, 100)),
          new JsonPrimitive(100),
          "Volume in percent, multiplied by its category and sound-effects volumes.",
          List.of());
    }
    entries.addAll(musicEntries(mc));
  }

  private static List<Setting> musicEntries(Minecraft mc) {
    AudioSettings config = AudioConfig.current();
    List<Setting> result = new ArrayList<>();
    for (String track : AudioController.music(mc)) {
      String label = AudioController.musicLabel(track);
      result.add(new Setting("audio.sound." + track, "audio", "Audio", "Individual music tracks", label,
          "Track volume in percent, multiplied by the music and master volumes.",
          Setting.Kind.INTEGER, new JsonPrimitive(AudioController.trackVolume(track)),
          new JsonPrimitive(100), 0, 100, 5, List.of(), false));
      result.add(new Setting("audio.trackEnabled." + track, "audio", "Audio", "Track rotation", label + " in rotation",
          "Include this track in automatic selection. You can still preview or explicitly queue excluded tracks.",
          Setting.Kind.BOOLEAN, new JsonPrimitive(!config.disabledTracks.contains(track)),
          new JsonPrimitive(true), 0, 1, 1, List.of(), false));
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

  public String id() {
    return "audio";
  }

  public List<Path> files() {
    return List.of(AudioConfig.path());
  }

  private AudioSettings draft(Map<String, JsonElement> changes) {
    AudioSettings s = AudioConfig.copy();
    if (changes.containsKey("audio.exclusions")) s.disabledTracks = new TreeSet<>(Arrays.asList(
        Catalog.JSON.fromJson(changes.get("audio.exclusions"), String[].class)));
    for (var e : changes.entrySet()) {
      String key = e.getKey().substring(6);
      JsonElement v = e.getValue();
      if (key.startsWith("category.")) s.categories.put(key.substring(9), v.getAsInt());
      else if (key.startsWith("trackEnabled.")) {
        String track = key.substring("trackEnabled.".length());
        if (v.getAsBoolean()) s.disabledTracks.remove(track); else s.disabledTracks.add(track);
      } else if (key.startsWith("sound.")) {
        String sound = key.substring(6);
        if (v.getAsInt() == 100 && !sound.startsWith("music:custom/")) s.sounds.remove(sound);
        else s.sounds.put(sound, v.getAsInt());
      } else
        switch (key) {
          case "exclusions" -> { }
          case "preset" -> s.preset = v.getAsString();
          case "presets" -> s.presets = new ArrayList<>(Arrays.asList(Catalog.JSON.fromJson(v, MusicPreset[].class)));
          case "groupVolumes" -> s.groupVolumes = Catalog.JSON.fromJson(v,
              new com.google.gson.reflect.TypeToken<TreeMap<String, Integer>>() {}.getType());
          case "favourites" -> s.favourites = new TreeSet<>(Arrays.asList(Catalog.JSON.fromJson(v, String[].class)));
          case "menuControls" -> s.menuControls = v.getAsBoolean();
          case "menuControlsPosition" -> s.menuControlsPosition = AudioSettings.MenuControlsPosition.values()[v.getAsInt()];
          case "menuControlsOffsetX" -> s.menuControlsOffsetX = v.getAsInt();
          case "menuControlsOffsetY" -> s.menuControlsOffsetY = v.getAsInt();
          case "master" -> s.master = v.getAsInt();
          case "musicMode" -> s.musicMode = AudioSettings.MusicMode.values()[v.getAsInt()];
          case "customMusic" -> s.customMusic = AudioSettings.CustomMusic.values()[v.getAsInt()];
          case "menuMusic" -> s.menuMusic = AudioSettings.MenuMusic.values()[v.getAsInt()];
          case "dimensionMusic" -> s.dimensionMusic = AudioSettings.DimensionMusic.values()[v.getAsInt()];
          case "gapMinSeconds" -> s.gapMinSeconds = v.getAsInt();
          case "gapMaxSeconds" -> s.gapMaxSeconds = v.getAsInt();
          case "recursive" -> s.recursive = v.getAsBoolean();
          case "waitBetweenTracks" -> s.waitBetweenTracks = v.getAsBoolean();
          case "delayQueuedTracks" -> s.delayQueuedTracks = v.getAsBoolean();
          case "shuffle" -> s.shuffle = v.getAsBoolean();
          case "avoidRepeats" -> s.avoidRepeats = v.getAsBoolean();
          case "musicDirectories" -> {
            var folders = MusicFolders.decode(v);
            s.musicDirectories = new ArrayList<>(folders.paths());
            s.disabledMusicDirectories = new LinkedHashSet<>(folders.disabled());
          }
          case "menuDirectories" -> {
            var folders = MusicFolders.decode(v);
            s.menuDirectories = new ArrayList<>(folders.paths());
            s.disabledMenuDirectories = new LinkedHashSet<>(folders.disabled());
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
