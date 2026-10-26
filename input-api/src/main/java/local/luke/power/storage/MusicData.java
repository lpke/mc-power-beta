package local.luke.power.storage;

import com.google.gson.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Validated music user data. Called only while PowerConfig owns its disk lock. */
final class MusicData {
  static final int VERSION = 1;
  static final int MAX_BYTES = 4 * 1024 * 1024;
  private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
  static final Set<String> FIELDS = Set.of("playlists", "playlistTracks", "menuPlaylistTracks", "disabledTracks", "favourites",
      "groupVolumes", "menuGroupVolumes", "musicDirectories", "menuDirectories", "disabledMusicDirectories", "disabledMenuDirectories");

  static Path path(Path config) { return config.getParent().getParent().resolve("power-beta/music.json"); }

  static JsonObject read(Path path) throws IOException {
    checkPath(path);
    if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Music data is missing: " + path + ". Restore the verified music backup before saving.");
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > MAX_BYTES)
      throw new IOException("Unsafe or oversized music data; original file preserved");
    try {
      JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
      validate(root);
      return root;
    } catch (RuntimeException failure) { throw new IOException("Unsupported or invalid music data; original file preserved: " + failure.getMessage(), failure); }
  }

  static void validate(JsonObject root) {
    require(root != null, "Music data must be an object");
    require(root.has("formatVersion") && integer(root.get("formatVersion")) == VERSION,
        "Supported music data version: " + VERSION);
    require(root.has("audio") && root.get("audio").isJsonObject(), "Music audio must be an object");
    require(root.has("queue") && root.get("queue").isJsonObject(), "Music queue must be an object");
    for (String key : root.keySet()) require(Set.of("formatVersion", "audio", "queue").contains(key), "Unknown music data field " + key);
    JsonObject audio = root.getAsJsonObject("audio");
    for (String key : audio.keySet()) require(FIELDS.contains(key) || key.equals("sounds"), "Unknown music audio field " + key);
    for (String key : List.of("playlistTracks", "menuPlaylistTracks", "disabledTracks", "favourites")) if (audio.has(key)) tracks(audio.get(key), 8192, true);
    for (String key : List.of("musicDirectories", "menuDirectories", "disabledMusicDirectories", "disabledMenuDirectories"))
      if (audio.has(key)) strings(audio.get(key), 32, 4096, true, false);
    for (String key : List.of("Music", "Menu")) {
      String directories = Character.toLowerCase(key.charAt(0)) + key.substring(1) + "Directories";
      String disabled = "disabled" + key + "Directories";
      if (audio.has(disabled)) {
        Set<String> all = audio.has(directories) ? strings(audio.get(directories), 32, 4096, true, false) : Set.of();
        require(all.containsAll(strings(audio.get(disabled), 32, 4096, true, false)), "Disabled folders must remain in the folder list");
      }
    }
    if (audio.has("groupVolumes")) volumes(audio.get("groupVolumes"), 8192, false);
    if (audio.has("menuGroupVolumes")) volumes(audio.get("menuGroupVolumes"), 8192, false);
    if (audio.has("sounds")) volumes(audio.get("sounds"), 4096, true);
    if (audio.has("playlists")) {
      require(audio.get("playlists").isJsonArray() && audio.getAsJsonArray("playlists").size() <= 128, "Use at most 128 playlists");
      Set<String> ids = new HashSet<>();
      for (JsonElement value : audio.getAsJsonArray("playlists")) {
        require(value.isJsonObject(), "Playlist must be an object");
        JsonObject playlist = value.getAsJsonObject();
        require(playlist.keySet().equals(Set.of("id", "name", "tracks", "groupVolumes")), "Unsupported playlist data");
        String id = text(playlist.get("id"), 64);
        require(id.matches("[a-zA-Z0-9_-]{1,64}") && ids.add(id), "Invalid or duplicate playlist identifier");
        text(playlist.get("name"), 80);
        tracks(playlist.get("tracks"), 8192, true);
        volumes(playlist.get("groupVolumes"), 8192, false);
      }
    }
    validateQueue(root.getAsJsonObject("queue"));
  }

  static void validateQueue(JsonObject queue) {
    require(queue.keySet().equals(Set.of("tracks")), "Unsupported music queue fields");
    tracks(queue.get("tracks"), 256, false);
  }

  static JsonObject empty() {
    JsonObject root = new JsonObject(); root.addProperty("formatVersion", VERSION);
    root.add("audio", new JsonObject()); JsonObject queue = new JsonObject(); queue.add("tracks", new JsonArray()); root.add("queue", queue);
    return root;
  }

  static JsonObject split(JsonObject audio) {
    JsonObject result = new JsonObject();
    for (String key : FIELDS) if (audio.has(key)) result.add(key, audio.get(key).deepCopy());
    if (audio.has("sounds")) {
      JsonObject sounds = new JsonObject();
      object(audio.get("sounds"), "Sound overrides").entrySet().stream().filter(e -> e.getKey().startsWith("music:")).forEach(e -> sounds.add(e.getKey(), e.getValue().deepCopy()));
      result.add("sounds", sounds);
    }
    return result;
  }

  static JsonObject preferences(JsonObject audio) {
    JsonObject result = audio.deepCopy();
    FIELDS.forEach(result::remove);
    if (!result.has("playlist")) result.addProperty("playlist", "");
    if (!result.has("menuPlaylist")) result.addProperty("menuPlaylist", "@world");
    JsonObject sounds = new JsonObject();
    if (audio.has("sounds")) object(audio.get("sounds"), "Sound overrides").entrySet().stream().filter(e -> !e.getKey().startsWith("music:")).forEach(e -> sounds.add(e.getKey(), e.getValue().deepCopy()));
    if (audio.has("sounds")) result.add("sounds", sounds);
    validatePreferences(result);
    return result;
  }

  static JsonObject merge(JsonObject preferences, JsonObject music) {
    JsonObject result = preferences.deepCopy();
    for (var entry : music.entrySet()) if (!entry.getKey().equals("sounds")) result.add(entry.getKey(), entry.getValue().deepCopy());
    JsonObject sounds = result.has("sounds") ? object(result.get("sounds"), "Sound overrides") : new JsonObject();
    if (music.has("sounds")) music.getAsJsonObject("sounds").entrySet().forEach(e -> sounds.add(e.getKey(), e.getValue().deepCopy()));
    result.add("sounds", sounds); validateSelections(result, music);
    return result;
  }

  static void validateSelections(JsonObject preferences, JsonObject music) {
    Set<String> ids = new HashSet<>();
    if (music.has("playlists")) for (JsonElement value : music.getAsJsonArray("playlists")) ids.add(value.getAsJsonObject().get("id").getAsString());
    for (String key : List.of("playlist", "menuPlaylist")) if (preferences.has(key)) {
      String id = string(preferences.get(key));
      require(key.equals("playlist") && id.isEmpty() || key.equals("menuPlaylist") && Set.of("@world", "@default").contains(id) || ids.contains(id), "Selected playlist is missing");
    }
  }

  static void validatePreferences(JsonObject audio) {
    require(FIELDS.stream().noneMatch(audio::has), "Music user data belongs in power-beta/music.json");
    require(Set.of("presets", "preset", "presetTrackPool", "musicDataVersion").stream().noneMatch(audio::has), "Legacy audio fields are unsupported; original configuration preserved");
    if (audio.has("sounds")) require(object(audio.get("sounds"), "Sound overrides").keySet().stream().noneMatch(key -> key.startsWith("music:")), "Song gains belong in power-beta/music.json");
    for (String key : List.of("playlist", "menuPlaylist")) if (audio.has(key)) {
      String id = string(audio.get(key));
      require(key.equals("playlist") && id.isEmpty() || id.matches("[a-zA-Z0-9_-]{1,64}") || key.equals("menuPlaylist") && Set.of("@world", "@default").contains(id), "Invalid playlist selection");
    }
  }

  static void backup(Path source, String prefix) throws IOException {
    checkPath(source);
    if (!Files.exists(source, LinkOption.NOFOLLOW_LINKS)) return;
    requireFile(source);
    Path directory = source.getParent().resolve("backups"); checkPath(directory); DurableFiles.createDirectories(directory);
    Path backup = Files.createTempFile(directory, prefix, ".json");
    durableWrite(backup, Files.readAllBytes(source));
  }

  static void write(Path path, JsonObject root) throws IOException {
    byte[] bytes = bytes(root); checkPath(path);
    if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) read(path);
    DurableFiles.createDirectories(path.getParent());
    Path temporary = Files.createTempFile(path.getParent(), ".music-", ".tmp");
    try {
      durableWrite(temporary, bytes);
      Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      DurableFiles.syncDirectory(path.getParent());
      if (!Arrays.equals(bytes, Files.readAllBytes(path))) throw new IOException("Music data verification failed");
    } finally { Files.deleteIfExists(temporary); }
  }

  static byte[] bytes(JsonObject root) throws IOException {
    validate(root);
    byte[] bytes = (JSON.toJson(root) + "\n").getBytes(StandardCharsets.UTF_8);
    if (bytes.length > MAX_BYTES) throw new IOException("Music data exceeds 4 MiB");
    return bytes;
  }

  static void checkPath(Path path) throws IOException {
    for (Path part = path; part != null; part = part.getParent()) if (Files.isSymbolicLink(part)) throw new IOException("Music data path contains a symbolic link");
  }
  private static void requireFile(Path path) throws IOException {
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > MAX_BYTES) throw new IOException("Unsafe or oversized music source");
  }
  private static void durableWrite(Path path, byte[] bytes) throws IOException {
    try (FileChannel output = FileChannel.open(path, StandardOpenOption.WRITE)) {
      ByteBuffer buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) output.write(buffer); output.force(true);
    }
    if (!Arrays.equals(bytes, Files.readAllBytes(path))) throw new IOException("Music backup verification failed");
    DurableFiles.syncDirectory(path.getParent());
  }
  private static Set<String> tracks(JsonElement value, int limit, boolean unique) { return strings(value, limit, 512, unique, true); }
  private static Set<String> strings(JsonElement value, int limit, int length, boolean unique, boolean music) {
    require(value != null && value.isJsonArray() && value.getAsJsonArray().size() <= limit, "Invalid or oversized music list");
    Set<String> values = new LinkedHashSet<>();
    for (JsonElement element : value.getAsJsonArray()) {
      String text = text(element, length); require(!music || text.startsWith("music:"), "Invalid music identifier");
      require(values.add(text) || !unique, "Duplicate music list value");
    }
    return values;
  }
  private static void volumes(JsonElement value, int limit, boolean music) {
    JsonObject volumes = object(value, "Volume overrides"); require(volumes.size() <= limit, "Too many volume overrides");
    for (var entry : volumes.entrySet()) {
      String key = text(new JsonPrimitive(entry.getKey()), music ? 512 : 4096); require(!music || key.startsWith("music:"), "Only music gains belong in music data");
      int gain = integer(entry.getValue()); require(gain >= 0 && gain <= 100, "Volume must be 0 to 100%");
    }
  }
  private static String text(JsonElement value, int length) {
    String text = string(value);
    require(!text.isBlank() && text.length() <= length && text.codePoints().noneMatch(Character::isISOControl), "Invalid music text"); return text;
  }
  private static String string(JsonElement value) {
    require(value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString(), "Music text must be a string"); return value.getAsString();
  }
  private static int integer(JsonElement value) {
    require(value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(), "Music number must be an integer");
    try { return value.getAsBigDecimal().intValueExact(); }
    catch (ArithmeticException failure) { throw new IllegalArgumentException("Music number must be an integer", failure); }
  }
  private static JsonObject object(JsonElement value, String name) {
    require(value != null && value.isJsonObject(), name + " must be an object"); return value.getAsJsonObject();
  }
  private static void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
}
