package local.luke.power.world;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;
import net.minecraft.nbt.*;

/** Read-only discovery. Opening a backup must never run a journal's recovery/reset code. */
final class WorldOwnedData {
  static final int MAX_RECORD = 1024 * 1024;
  static final long MAX_JOURNAL = 64L * 1024 * 1024;
  record Owner(Set<String> ids, String name, Long seed, String folder) {
    Owner { ids = Set.copyOf(ids); }
  }
  record External(Path path, String kind, boolean uncertain, boolean pending) {}
  record Discovery(List<External> files, List<String> warnings) {
    Discovery { files = List.copyOf(files); warnings = List.copyOf(warnings); }
  }
  private record JournalOwner(boolean known, boolean matches, boolean pending) {}
  private WorldOwnedData() {}

  static Owner owner(Path world) throws IOException {
    Set<String> ids = new TreeSet<>(); String name = world.getFileName().toString(); Long seed = null;
    for (String file : List.of("level.dat", "level.dat_old")) {
      Path path = world.resolve(file); SavedWorldAccess.safe(path);
      if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) continue;
      try {
        NbtCompound data = WorldRuleFiles.read(path).getCompound("Data");
        String id = data.getString("PowerBetaCommandWorldId");
        try { if (UUID.fromString(id).toString().equals(id)) ids.add(id); }
        catch (IllegalArgumentException ignored) { }
        if (file.equals("level.dat") || seed == null) {
          if (!data.getString("LevelName").isBlank()) name = data.getString("LevelName");
          if (data.contains("RandomSeed")) seed = data.getLong("RandomSeed");
        }
      } catch (IOException | RuntimeException unreadable) { /* Raw backups still preserve this file. */ }
    }
    return new Owner(ids, name, seed, world.getFileName().toString());
  }

  static Discovery discover(Path game, Owner owner) throws IOException {
    List<External> files = new ArrayList<>(); List<String> warnings = new ArrayList<>();
    for (String id : owner.ids) {
      String key = UUID.nameUUIDFromBytes(("world:" + id).getBytes(StandardCharsets.UTF_8)).toString();
      Path maps = game.resolve("power-beta-data/maps").resolve(key); SavedWorldAccess.safe(maps);
      if (Files.exists(maps, LinkOption.NOFOLLOW_LINKS)) files.add(new External(maps, "maps", false, false));
    }
    for (String kind : List.of("deaths", "hotbar-backups", "inventory-recovery")) {
      Path root = game.resolve("power-beta-data").resolve(kind); SavedWorldAccess.safe(root);
      if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) continue;
      if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Recovery data is not a directory.");
      try (var paths = Files.list(root)) {
        List<Path> journals = paths.sorted().limit(8193).toList();
        if (journals.size() > 8192) throw new IOException("Too many recovery files for one world operation.");
        for (Path path : journals) {
          SavedWorldAccess.safe(path);
          if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Recovery data contains an unsupported file.");
          JournalOwner found;
          try { found = switch (kind) { case "deaths" -> death(path, owner); case "inventory-recovery" -> inventory(path, owner); default -> hotbar(path, owner); }; }
          catch (IOException | RuntimeException damaged) { found = new JournalOwner(false, false, false); }
          if (found.matches || !found.known || owner.ids.isEmpty()) {
            boolean uncertain = !found.known || owner.ids.isEmpty();
            files.add(new External(path, kind, uncertain, found.pending));
            if (uncertain && warnings.stream().noneMatch(v -> v.startsWith("Unclassified")))
              warnings.add("Unclassified recovery evidence is included conservatively; its ownership could not be verified.");
          }
        }
      }
    }
    if (owner.ids.isEmpty()) {
      Path maps = game.resolve("power-beta-data/maps"); SavedWorldAccess.safe(maps);
      if (Files.exists(maps, LinkOption.NOFOLLOW_LINKS)) files.add(new External(maps, "unclassified-maps", true, false));
      warnings.add("The save has no readable stable world ID. Unclassified maps are included in backups; they cannot be assigned safely to a duplicate.");
    }
    for (String kind : List.of("settings-backups", "world-metadata-backups")) {
      Path root = game.resolve("power-beta-data").resolve(kind); SavedWorldAccess.safe(root);
      if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) continue;
      if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Metadata backup storage is not a directory.");
      try (var paths = Files.list(root)) {
        List<Path> directories = paths.sorted().limit(8193).toList();
        if (directories.size() > 8192) throw new IOException("Too many metadata backups for one world operation.");
        for (Path directory : directories) {
          SavedWorldAccess.safe(directory);
          if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Metadata backup storage contains an unsupported file.");
          boolean uncertain = false, relevant = false, pending = false;
          List<String> candidates = kind.equals("settings-backups") ? List.of("pending.json", "complete.json", "recovered.json") : List.of("transaction.json");
          for (String candidate : candidates) {
            Path marker = directory.resolve(candidate); SavedWorldAccess.safe(marker);
            if (!Files.exists(marker, LinkOption.NOFOLLOW_LINKS)) continue;
            try {
              if (!Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS) || Files.size(marker) > 256 * 1024) throw new IOException("Invalid metadata backup manifest.");
              JsonObject manifest = JsonParser.parseString(Files.readString(marker)).getAsJsonObject();
              Collection<String> targets;
              if (kind.equals("settings-backups")) targets = manifest.keySet();
              else {
                List<String> names = new ArrayList<>();
                for (JsonElement file : manifest.getAsJsonArray("files")) names.add(file.getAsJsonObject().get("path").getAsString());
                targets = names;
              }
              for (String target : targets) relevant |= game.resolve(target).normalize().startsWith(game.resolve("saves").resolve(owner.folder));
              pending |= candidate.equals("pending.json") || candidate.equals("transaction.json")
                  && (!manifest.has("state") || !Set.of("complete", "recovered").contains(manifest.get("state").getAsString()));
            } catch (IOException | RuntimeException damaged) { uncertain = true; }
          }
          if (relevant || uncertain) {
            files.add(new External(directory, kind, uncertain, pending));
            if (uncertain && warnings.stream().noneMatch(v -> v.startsWith("Unclassified metadata")))
              warnings.add("Unclassified metadata backup evidence is included conservatively; its ownership could not be verified.");
          }
        }
      }
    }
    return new Discovery(files, warnings);
  }

  private static JournalOwner hotbar(Path path, Owner owner) throws IOException {
    if (Files.size(path) == 0) return new JournalOwner(false, false, false);
    try (DataInputStream input = open(path)) {
      byte[] bytes = frame(input, 0x4c504b48, 1);
      NbtCompound record = NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes)));
      if (record.contains("WorldId")) {
        String id = record.getString("WorldId");
        if (!UUID.fromString(id).toString().equals(id)) throw new IOException("Invalid recovery world ID.");
        return new JournalOwner(true, owner.ids.contains(id), false);
      }
      // The seed survives rename. Legacy copies may share it, so preserve all matching evidence.
      if (record.contains("Seed") && owner.seed != null)
        return new JournalOwner(true, record.getLong("Seed") == owner.seed, false);
      return new JournalOwner(false, false, false);
    }
  }

  private static JournalOwner inventory(Path path, Owner owner) throws IOException {
    if (Files.size(path) == 0) return new JournalOwner(false, false, false);
    try (DataInputStream input = open(path)) {
      JsonObject record = JsonParser.parseString(new String(frame(input, 0x4c504b48, 1), StandardCharsets.UTF_8)).getAsJsonObject();
      if (record.get("format").getAsInt() != 1 || !record.get("phase").getAsString().equals("prepared"))
        throw new IOException("Unsupported inventory recovery evidence.");
      String id = record.get("world").getAsString();
      if (!UUID.fromString(id).toString().equals(id)) throw new IOException("Invalid inventory recovery world ID.");
      // Prepared is immutable before/after evidence, never proof of publication or a replay instruction.
      return new JournalOwner(true, owner.ids.contains(id), false);
    }
  }

  private static JournalOwner death(Path path, Owner owner) throws IOException {
    if (Files.size(path) == 0) return new JournalOwner(false, false, false);
    try (DataInputStream input = open(path)) {
      boolean known = false, matches = false; Set<String> pending = new HashSet<>();
      while (input.available() > 0) {
        JsonObject event = JsonParser.parseString(new String(frame(input, 0x50424448, 2), StandardCharsets.UTF_8)).getAsJsonObject();
        String kind = event.get("kind").getAsString();
        if (kind.equals("death")) {
          String id = event.getAsJsonObject("death").get("world").getAsString();
          if (!UUID.fromString(id).toString().equals(id)) throw new IOException("Invalid death world ID.");
          known = true; matches |= owner.ids.contains(id);
          if (!matches) return new JournalOwner(true, false, false);
        } else if (kind.equals("pending")) pending.add(event.get("id").getAsString());
        else if (kind.equals("recovered") || kind.equals("available")) pending.remove(event.get("id").getAsString());
        else throw new IOException("Unsupported death journal event.");
      }
      return new JournalOwner(known, matches, !pending.isEmpty());
    }
  }

  private static DataInputStream open(Path path) throws IOException {
    if (Files.size(path) > MAX_JOURNAL) throw new IOException("Recovery evidence exceeds its supported parsing limit.");
    return new DataInputStream(new BufferedInputStream(Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)));
  }

  private static byte[] frame(DataInputStream input, int magic, int version) throws IOException {
    if (input.readInt() != magic || input.readInt() != version) throw new IOException("Unsupported recovery journal.");
    int size = input.readInt(); long checksum = input.readLong();
    if (size < 1 || size > MAX_RECORD) throw new IOException("Invalid recovery record size.");
    byte[] bytes = input.readNBytes(size); CRC32 crc = new CRC32(); crc.update(bytes);
    if (bytes.length != size || crc.getValue() != checksum) throw new IOException("Damaged recovery evidence.");
    return bytes;
  }
}
