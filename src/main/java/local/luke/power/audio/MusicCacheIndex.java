package local.luke.power.audio;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Scanner-owned index of converted songs. Never read or written from render/input paths. */
final class MusicCacheIndex {
  private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
  private final Path game, directory, path;
  private final JsonObject entries;

  private MusicCacheIndex(Path game, JsonObject entries) {
    this.game = game;
    directory = game.resolve("power-beta-data/music-cache");
    path = directory.resolve("index.json");
    this.entries = entries;
  }

  static MusicCacheIndex read(Path game) throws IOException {
    MusicCacheIndex index = new MusicCacheIndex(game, new JsonObject());
    if (!Files.exists(index.path, LinkOption.NOFOLLOW_LINKS)) return index;
    if (!index.directory.toRealPath().equals(game.toRealPath().resolve("power-beta-data/music-cache")))
      throw new IOException("Music cache must be inside this instance");
    if (!Files.isRegularFile(index.path, LinkOption.NOFOLLOW_LINKS) || Files.size(index.path) > 4 * 1024 * 1024)
      throw new IOException("Music cache index is unavailable or oversized");
    try {
      JsonObject root = JsonParser.parseString(Files.readString(index.path)).getAsJsonObject();
      if (root.get("version").getAsInt() != 1) throw new IllegalArgumentException();
      JsonObject entries = root.getAsJsonObject("tracks");
      if (entries == null || entries.size() > 8192) throw new IllegalArgumentException();
      MusicCacheIndex loaded = new MusicCacheIndex(game, entries);
      for (String id : entries.keySet()) loaded.entry(id);
      return loaded;
    } catch (RuntimeException failure) { throw new IOException("Music cache index is invalid; existing file preserved", failure); }
  }

  Path cached(String id, Path source) throws IOException {
    JsonObject entry = entry(id);
    if (entry == null || entry.get("bytes").getAsLong() != Files.size(source)
        || entry.get("modified").getAsLong() != Files.getLastModifiedTime(source).toMillis()) return null;
    return playback(entry);
  }

  void remember(MusicLibrary.Track track, Path identity) throws IOException {
    if (!track.mp3() || !track.playable()) return;
    JsonObject entry = new JsonObject();
    entry.addProperty("identity", identity.toString());
    entry.addProperty("name", track.name());
    entry.addProperty("cache", track.playbackPath().getFileName().toString());
    entry.addProperty("bytes", Files.size(track.path()));
    entry.addProperty("modified", Files.getLastModifiedTime(track.path()).toMillis());
    entries.add(track.id(), entry);
  }

  List<MusicLibrary.Track> missing(Path physicalRoot, Path identityRoot) throws IOException {
    List<MusicLibrary.Track> result = new ArrayList<>();
    for (var value : entries.entrySet()) {
      JsonObject entry = entry(value.getKey());
      if (entry == null) continue;
      Path identity = Path.of(entry.get("identity").getAsString());
      if (!identity.startsWith(identityRoot)) continue;
      Path playback = playback(entry);
      if (playback != null) result.add(new MusicLibrary.Track(
          physicalRoot.resolve(identityRoot.relativize(identity)), entry.get("name").getAsString(), playback, value.getKey()));
    }
    return result;
  }

  private JsonObject entry(String id) throws IOException {
    if (!entries.has(id)) return null;
    try {
      JsonObject entry = entries.getAsJsonObject(id);
      if (!Path.of(entry.get("identity").getAsString()).isAbsolute()
          || !id.equals(MusicLibrary.trackId(Path.of(entry.get("identity").getAsString())))
          || !entry.get("name").getAsString().toLowerCase(Locale.ROOT).endsWith(".mp3")
          || !entry.get("cache").getAsString().matches("[0-9a-f]{64}\\.wav")
          || entry.get("bytes").getAsLong() < 4 || entry.get("modified").getAsLong() < 0)
        throw new IllegalArgumentException();
      return entry;
    } catch (RuntimeException failure) { throw new IOException("Music cache index contains an invalid song; existing file preserved", failure); }
  }

  private Path playback(JsonObject entry) throws IOException {
    Path file = directory.resolve(entry.get("cache").getAsString());
    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) <= 44) return null;
    if (!file.toRealPath().getParent().equals(game.toRealPath().resolve("power-beta-data/music-cache")))
      throw new IOException("Music cache must be inside this instance");
    return file;
  }

  void save() throws IOException {
    if (entries.size() == 0) return;
    if (entries.size() > 8192) throw new IOException("Music cache index reached its song limit");
    Files.createDirectories(directory);
    if (!directory.toRealPath().equals(game.toRealPath().resolve("power-beta-data/music-cache")))
      throw new IOException("Music cache must be inside this instance");
    JsonObject root = new JsonObject(); root.addProperty("version", 1); root.add("tracks", entries);
    String encoded = JSON.toJson(root) + "\n";
    if (encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 4 * 1024 * 1024)
      throw new IOException("Music cache index exceeds 4 MiB");
    if (Files.exists(path) && Files.readString(path).equals(encoded)) return;
    Path temporary = Files.createTempFile(directory, ".index-", ".json");
    try {
      Files.writeString(temporary, encoded);
      Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally { Files.deleteIfExists(temporary); }
  }
}
