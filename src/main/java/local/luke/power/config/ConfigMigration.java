package local.luke.power.config;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import local.luke.power.storage.PowerConfig;
import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.file.YamlFile;

/** One-time import. Back up exact bytes before replacing any legacy configuration. */
public final class ConfigMigration {
  public static void prepare(Path game) throws Exception {
    moveArchives(game);
    PowerConfig.configure(game.resolve("config/power-beta.json"));
    if (Files.exists(PowerConfig.path())) {
      cleanupLegacy(game);
      JsonObject existing=PowerConfig.document();
      JsonObject upgraded=upgrade(existing);
      if (!existing.equals(upgraded)) {
        backup(game,Map.of(PowerConfig.path(),Files.readAllBytes(PowerConfig.path())));
        PowerConfig.write(upgraded);
      }
      removeEmptyFolders(game.resolve("config"));
      return;
    }
    JsonObject document = PowerConfig.document(), settings = document.getAsJsonObject("settings");
    Map<Path, byte[]> originals = new LinkedHashMap<>();
    try (var in = ConfigMigration.class.getResourceAsStream("/assets/powerbeta/defaults/index.txt")) {
      if (in == null) throw new IOException("Missing configuration defaults");
      for (String name : new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList()) {
        if (name.isBlank()) continue;
        Path path = game.resolve(name).normalize();
        if (!path.startsWith(game) || Files.isSymbolicLink(path)) throw new IOException("Unsafe migration path");
        byte[] bytes;
        if (Files.exists(path)) {
          if (Files.size(path) > 1024 * 1024) throw new IOException("Legacy settings exceed 1 MiB");
          bytes = Files.readAllBytes(path); originals.put(path, bytes);
        } else try (var defaults = ConfigMigration.class.getResourceAsStream("/assets/powerbeta/defaults/" + name)) {
          if (defaults == null) throw new IOException("Missing defaults: " + name);
          bytes = defaults.readAllBytes();
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        String section;
        JsonObject value;
        if (name.equals("options.txt")) {
          section = "native"; value = new JsonObject();
          for (String line : text.lines().toList()) { int split = line.indexOf(':'); if (split > 0) value.addProperty(line.substring(0,split),line.substring(split+1).strip()); }
        } else if (name.endsWith(".yml")) {
          String[] parts = name.split("/"); section = parts[1] + ":" + parts[2].replace(".yml", "");
          YamlFile yaml = new YamlFile(); yaml.loadFromString(text); value = json(yaml);
        } else if (name.endsWith(".properties")) {
          section = name.contains("lpkecreative") ? "creative" : name.contains("lpketweaks") ? "building" : "editor";
          Properties properties = new Properties(); properties.load(new StringReader(text)); value = new JsonObject();
          for (String key : properties.stringPropertyNames()) {
            String raw = properties.getProperty(key); JsonElement v;
            if (key.endsWith("blacklist") || key.endsWith("whitelist")) { JsonArray list = new JsonArray(); for (String item : raw.split(",")) if (!item.isBlank()) list.add(item.strip()); v = list; }
            else if (raw.equals("true") || raw.equals("false")) v = new JsonPrimitive(Boolean.parseBoolean(raw));
            else { try { v = new JsonPrimitive(Integer.decode(raw)); } catch (NumberFormatException e) { v = new JsonPrimitive(raw); } }
            put(value, key, v);
          }
        } else {
          section = name.contains("oldLogo") ? "title" : name.contains("audio.json") ? "audio" : "visual";
          value = JsonParser.parseString(text).getAsJsonObject();
        }
        settings.add(section,value);
      }
    }
    Path keys = game.resolve("config/power-beta/keys.json");
    if (Files.isSymbolicLink(keys) || Files.exists(keys) && Files.size(keys)>1024*1024) throw new IOException("Unsafe key configuration");
    if (Files.exists(keys)) { byte[] bytes = Files.readAllBytes(keys); originals.put(keys,bytes); settings.add("bindings",JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8))); }
    // Split the old aggregate options without silently enabling new mechanics.
    JsonObject sounds = settings.getAsJsonObject("unitweaks:features");
    if (sounds != null) {
      boolean enabled = sounds.has("moreSounds") && sounds.remove("moreSounds").getAsBoolean();
      for (String field : List.of("eatingSounds","burpingSounds","shearingSounds","toolBreakSounds","armorBreakSounds","chestSounds")) sounds.addProperty(field,enabled);
    }
    JsonObject building = settings.getAsJsonObject("building"), tweaks = settings.getAsJsonObject("unitweaks:tweaks");
    if (building != null) {
      boolean steering = building.has("boatSteering") && building.get("boatSteering").getAsBoolean();
      building.addProperty("boatSpeed",steering);
      building.addProperty("boatProtection",steering || tweaks != null && tweaks.has("boatsDontBreak") && tweaks.get("boatsDontBreak").getAsBoolean());
    }
    JsonObject camera = settings.getAsJsonObject("freecam:config");
    if (camera != null) camera.addProperty("enabled", settings.getAsJsonObject("native").has("key_Toggle Freecam") && !settings.getAsJsonObject("native").get("key_Toggle Freecam").getAsString().equals("0"));
    Path positions=game.resolve("config/freecam/camerapositions");
    if (Files.isDirectory(positions) && !Files.isSymbolicLink(positions)) {
      JsonObject saved=new JsonObject();
      try(var files=Files.list(positions)) {
        for(Path path:files.filter(p->p.toString().endsWith(".json")).toList()) {
          if (Files.isSymbolicLink(path) || Files.size(path)>1024*1024) throw new IOException("Unsafe camera positions");
          byte[] bytes=Files.readAllBytes(path);
          JsonObject value=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
          String id=value.get("seed").getAsLong()+":"+value.get("worldName").getAsString();
          String key=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(id.getBytes(StandardCharsets.UTF_8)));
          saved.add(key,value); originals.put(path,bytes);
        }
      }
      settings.add("cameraPositions",saved);
    }
    backup(game,originals);
    document=upgrade(document);
    if (!originals.isEmpty()) {
      JsonObject pending=new JsonObject(), files=new JsonObject();
      originals.forEach((path,bytes)->files.addProperty(game.relativize(path).toString(),hash(bytes)));
      pending.add("files",files);
      pending.add("settings",document.deepCopy());
      FileTransaction.atomicWrite(game.resolve("power-beta-data/pending-config-import.json"),Catalog.JSON.toJson(pending).getBytes(StandardCharsets.UTF_8));
    }
    PowerConfig.write(document);
    cleanupLegacy(game);
    removeEmptyFolders(game.resolve("config"));
  }
  private static String hash(byte[] bytes) {
    try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)); }
    catch(java.security.NoSuchAlgorithmException e) { throw new AssertionError(e); }
  }
  private static void cleanupLegacy(Path game) throws IOException {
    Path marker=game.resolve("power-beta-data/pending-config-import.json");
    if(!Files.exists(marker))return;
    if(Files.isSymbolicLink(marker)||Files.size(marker)>4*1024*1024)throw new IOException("Unsafe config import journal");
    JsonObject pending=JsonParser.parseString(Files.readString(marker)).getAsJsonObject();
    if(!PowerConfig.document().equals(pending.getAsJsonObject("settings")))
      throw new IOException("Imported config changed before cleanup; originals preserved");
    List<Path> removable=new ArrayList<>();
    for(var entry:pending.getAsJsonObject("files").entrySet()) {
      Path path=game.resolve(entry.getKey()).normalize();
      if(!path.startsWith(game.resolve("config"))&&!path.equals(game.resolve("options.txt")))throw new IOException("Unsafe config cleanup path");
      for(Path part=path;!part.equals(game);part=part.getParent())if(Files.isSymbolicLink(part))throw new IOException("Unsafe config cleanup link");
      if(Files.exists(path)) {
        if(Files.size(path)>1024*1024||!hash(Files.readAllBytes(path)).equals(entry.getValue().getAsString()))
          throw new IOException("Legacy config changed during import; originals preserved");
        removable.add(path);
      }
    }
    for(Path path:removable)Files.delete(path);
    Files.delete(marker);
  }
  private static void removeEmptyFolders(Path directory) throws IOException {
    if(!Files.isDirectory(directory))return;
    try(var paths=Files.walk(directory)) {
      for(Path path:paths.sorted(Comparator.reverseOrder()).toList())
        if(Files.isDirectory(path) && !Files.isSymbolicLink(path)) try(var children=Files.list(path)) { if(children.findAny().isEmpty()) Files.delete(path); }
    }
  }
  private static void moveArchives(Path game) throws IOException {
    for(var entry:Map.of("config/power-beta/backups","settings-backups","lpketweaks-hotbar-backups","hotbar-backups").entrySet()) {
      Path source=game.resolve(entry.getKey()),destination=game.resolve("power-beta-data").resolve(entry.getValue());
      if(!Files.exists(source))continue;
      if(Files.isSymbolicLink(source)||Files.isSymbolicLink(destination))throw new IOException("Unsafe archive path");
      Files.createDirectories(destination);
      try(var children=Files.list(source)) {
        for(Path child:children.toList()) {
          if(Files.isSymbolicLink(child))throw new IOException("Unsafe recovery archive");
          Path target=destination.resolve(child.getFileName());
          if(Files.exists(target))target=destination.resolve(UUID.randomUUID()+"-"+child.getFileName());
          Files.move(child,target,StandardCopyOption.ATOMIC_MOVE);
        }
      }
      Files.delete(source);
    }
  }
  private static void backup(Path game,Map<Path,byte[]> originals) throws Exception {
    if (originals.isEmpty()) return;
    Path backup=game.resolve("power-beta-data/config-backups"); Files.createDirectories(backup);
    Path zip=backup.resolve("before-unified-settings-"+UUID.randomUUID()+".zip");
    try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(zip,StandardOpenOption.CREATE_NEW))) {
      for(var e:originals.entrySet()) { out.putNextEntry(new ZipEntry(game.relativize(e.getKey()).toString()));out.write(e.getValue());out.closeEntry(); }
    }
    try(ZipFile check=new ZipFile(zip.toFile())) {
      for(var e:originals.entrySet()) if(!Arrays.equals(check.getInputStream(check.getEntry(game.relativize(e.getKey()).toString())).readAllBytes(),e.getValue()))
        throw new IOException("Configuration backup verification failed");
    }
    try(var channel=java.nio.channels.FileChannel.open(zip,StandardOpenOption.WRITE)) { channel.force(true); }
    try(var channel=java.nio.channels.FileChannel.open(backup,StandardOpenOption.READ)) { channel.force(true); }
  }
  private static JsonObject upgrade(JsonObject root) throws IOException {
    Map<String,String> aliases=new LinkedHashMap<>();
    try(var in=ConfigMigration.class.getResourceAsStream("/assets/powerbeta/legacy-aliases.json")) {
      if(in==null)throw new IOException("Missing legacy import map");
      JsonParser.parseString(new String(in.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject()
          .entrySet().stream().sorted((a,b)->Integer.compare(b.getKey().length(),a.getKey().length()))
          .forEach(e->aliases.put(e.getKey(),e.getValue().getAsString()));
    }
    var pattern=java.util.regex.Pattern.compile(aliases.keySet().stream().map(java.util.regex.Pattern::quote).collect(java.util.stream.Collectors.joining("|")));
    JsonObject next=rename(root,aliases,pattern).getAsJsonObject(); next.addProperty("schemaVersion",3);
    JsonObject all=next.getAsJsonObject("settings");
    if (!next.has("cheatsAccessVersion")) {
      // The old pack disabled the editor by default; the saved world master now provides that protection.
      JsonObject editor = all.getAsJsonObject("editor");
      if (editor != null) { editor.remove("creativeOnly"); editor.addProperty("enabled", true); }
      JsonObject commands = all.getAsJsonObject("commandAccess");
      if (commands != null && commands.has("rules")) {
        JsonObject rules = commands.getAsJsonObject("rules");
        for (String key : new ArrayList<>(rules.keySet())) {
          if (List.of("CREATIVE_ONLY", "ANY_MODE").contains(rules.get(key).getAsString()))
            rules.addProperty(key, "ALLOWED");
        }
      }
      next.addProperty("cheatsAccessVersion", 1);
    }
    JsonObject controls=all.getAsJsonObject("power_controls:userinterface");
    if(controls!=null) {
      controls.remove("fovSlider");
      JsonObject video=controls.getAsJsonObject("videoSettingsConfig");
      if(video!=null) for(String key:List.of("brightnessSlider","cloudHeightSlider","cloudsToggle","fogDensitySlider","guiScaleSlider","fpsLimitSlider","renderDistanceSlider")) video.remove(key);
    }
    JsonObject fixes=all.getAsJsonObject("power_client_fixes:config");
    if(fixes!=null) for(String key:List.of("enableMojangFixTextOnTitleScreen","enableInventoryChanges","enableBitDepthFix","enableDeathScreenScoreFix","enableCommandKey","enableQuitButton","useResourcesDownloadURL")) fixes.remove(key);
    JsonObject tweaks=all.getAsJsonObject("power_controls:tweaks"); if(tweaks!=null)tweaks.remove("boatsDontBreak");
    local.luke.power.audio.AudioMigration.upgrade(next);
    return next;
  }
  private static JsonElement rename(JsonElement value,Map<String,String> aliases,java.util.regex.Pattern pattern) {
    if(value.isJsonObject()) {
      JsonObject result=new JsonObject();
      for(var e:value.getAsJsonObject().entrySet()) {
        String key=pattern.matcher(e.getKey()).replaceAll(m->java.util.regex.Matcher.quoteReplacement(aliases.get(m.group())));
        if(result.has(key)) throw new IllegalArgumentException("Conflicting imported configuration keys: "+key);
        result.add(key,rename(e.getValue(),aliases,pattern));
      }
      return result;
    }
    if(value.isJsonArray()) { JsonArray result=new JsonArray();value.getAsJsonArray().forEach(v->result.add(rename(v,aliases,pattern)));return result; }
    return value.deepCopy();
  }
  public static void main(String[] args) throws Exception {
    if(args.length!=1)throw new IllegalArgumentException("Expected the game directory");
    Path game=Path.of(args[0]).toAbsolutePath().normalize();
    FileTransaction.recover(game);
    prepare(game);
  }
  private static JsonObject json(ConfigurationSection section) {
    JsonObject result = new JsonObject();
    for (String key : section.getKeys(false)) { Object value = section.get(key); result.add(key,value instanceof ConfigurationSection nested ? json(nested) : Catalog.JSON.toJsonTree(value)); }
    return result;
  }
  private static void put(JsonObject root, String path, JsonElement value) {
    String[] keys = path.split("\\.");
    for (int i=0;i<keys.length-1;i++) { if (!root.has(keys[i])) root.add(keys[i],new JsonObject()); root=root.getAsJsonObject(keys[i]); }
    root.add(keys[keys.length-1],value);
  }
}
