package local.luke.power.storage;

import com.google.gson.*;
import java.nio.file.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PowerConfigTest {
  @TempDir static Path dir;
  @BeforeAll static void setup() throws Exception { Files.createDirectories(dir.resolve("config")); PowerConfig.configure(dir.resolve("config/power-beta.json")); }
  @BeforeEach void clear() throws Exception { Files.deleteIfExists(PowerConfig.path()); Files.deleteIfExists(PowerConfig.musicPath()); }
  @AfterEach void finish() throws Exception { PowerConfig.flushPending(); PowerConfig.takeSaveFailure(); }
  @Test void recoveryStatusReadNeverWaitsForTheDiskWriter() throws Exception {
    var field = PowerConfig.class.getDeclaredField("DISK"); field.setAccessible(true);
    var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
    var workers = Executors.newFixedThreadPool(2);
    try {
      Future<?> writer = workers.submit(() -> {
        synchronized (field.get(null)) { entered.countDown(); assertTrue(release.await(5, TimeUnit.SECONDS)); }
        return null;
      });
      assertTrue(entered.await(5, TimeUnit.SECONDS));
      Future<String> status = workers.submit(PowerConfig::problem);
      assertEquals("", status.get(200, TimeUnit.MILLISECONDS), "tick recovery check waited for disk I/O");
      assertFalse(writer.isDone(), "writer was released before the status read");
    } finally { release.countDown(); workers.shutdownNow(); }
  }
  @Test void independentWritersRetainEverySection() throws Exception {
    var workers=Executors.newFixedThreadPool(4);
    try {
      var jobs=new java.util.ArrayList<Future<?>>();
      for(int i=0;i<40;i++) { int id=i; jobs.add(workers.submit(() -> {
        JsonObject data=new JsonObject();data.addProperty("value",id);
        try { PowerConfig.put("section"+id,data); } catch(Exception e) { throw new RuntimeException(e); }
      })); }
      for(var job:jobs) job.get();
    } finally { workers.shutdownNow(); }
    assertEquals(40,PowerConfig.document().getAsJsonObject("settings").size());
    for(int i=0;i<40;i++) assertEquals(i,PowerConfig.section("section"+i).get("value").getAsInt());
  }
  @Test void malformedFileIsNeverOverwritten() throws Exception {
    Files.writeString(PowerConfig.path(),"broken");
    assertThrows(IllegalStateException.class,()->PowerConfig.put("test",new JsonObject()));
    assertEquals("broken",Files.readString(PowerConfig.path()));
  }
  @Test void retiredAndFutureFormatsBlockEveryWriterAndPreserveExactBytes() throws Exception {
    for (int version : new int[]{3, 4, 5, 6, 7, 8, 10, 99}) {
      String original = "{\"schemaVersion\":" + version + ",\"settings\":{\"keep\":{\"value\":true}}}";
      Files.writeString(PowerConfig.path(), original);
      JsonObject fresh = JsonParser.parseString("{\"schemaVersion\":9,\"settings\":{}}").getAsJsonObject();
      assertThrows(IllegalStateException.class, PowerConfig::document);
      assertThrows(IllegalStateException.class, () -> PowerConfig.write(fresh));
      assertThrows(IllegalStateException.class, () -> PowerConfig.put("test", new JsonObject()));
      assertThrows(IllegalStateException.class, () -> PowerConfig.replaceSection("keep", null, new JsonObject()));
      assertEquals(original, Files.readString(PowerConfig.path()));
    }
  }
  @Test void rejectsLinksWithoutTouchingTheirTargets() throws Exception {
    Path target=dir.resolve("keep");Files.writeString(target,"keep");
    Files.createSymbolicLink(PowerConfig.path(),target);
    assertThrows(IllegalStateException.class,()->PowerConfig.put("test",new JsonObject()));
    assertEquals("keep",Files.readString(target));
  }
  @Test void returnedSectionsCannotMutateStoredValues() throws Exception {
    JsonObject data=new JsonObject();data.addProperty("a",5);PowerConfig.put("test",data);
    data.addProperty("a",6);PowerConfig.section("test").addProperty("a",7);
    assertEquals(5,PowerConfig.section("test").get("a").getAsInt());
    try(var files=Files.list(PowerConfig.path().getParent())) { assertFalse(files.anyMatch(p->p.toString().endsWith(".tmp"))); }
  }
  @Test void deferredSnapshotsPreserveOtherSectionsAndSynchronousWritesWin() throws Exception {
    JsonObject old = new JsonObject(); old.addProperty("value", 1);
    PowerConfig.put("unrelated", old);
    PowerConfig.saveDeferred("toggle", old); old.addProperty("value", 2);
    assertEquals(1, PowerConfig.section("toggle").get("value").getAsInt());
    for (int i = 0; i < 40; i++) {
      JsonObject data = new JsonObject(); data.addProperty("value", i);
      PowerConfig.saveDeferred("toggle", data);
    }
    PowerConfig.put("toggle", old);
    assertEquals(2, PowerConfig.section("toggle").get("value").getAsInt());
    assertEquals(1, PowerConfig.section("unrelated").get("value").getAsInt());
    String persisted = Files.readString(PowerConfig.path());
    assertEquals(2, JsonParser.parseString(persisted).getAsJsonObject().getAsJsonObject("settings")
        .getAsJsonObject("toggle").get("value").getAsInt());
  }
  @Test void deferredFailurePreservesTheOriginalAndRetriesAfterRepair() throws Exception {
    JsonObject saved = new JsonObject(); saved.addProperty("value", 1);
    PowerConfig.put("unrelated", saved); String original = Files.readString(PowerConfig.path());
    Files.writeString(PowerConfig.path(), "broken");
    PowerConfig.saveDeferred("toggle", saved);
    assertThrows(java.io.IOException.class, PowerConfig::flushPending);
    assertEquals("broken", Files.readString(PowerConfig.path()));
    Files.writeString(PowerConfig.path(), original); PowerConfig.flushPending();
    assertEquals(1, PowerConfig.section("toggle").get("value").getAsInt());
    assertEquals(1, PowerConfig.section("unrelated").get("value").getAsInt());
  }
  @Test void shutdownWaitsForAQueuedSaveEvenWhenTheWriterIsBlocked() throws Exception {
    Path persisted = dir.resolve("shutdown.json");
    String classpath = java.util.stream.Stream.of(PowerConfig.class, Gson.class, PowerConfigShutdownProbe.class)
        .map(type -> {
          try { return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(); }
          catch (Exception e) { throw new RuntimeException(e); }
        }).collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
    Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/java").toString(),
        "-cp", classpath, PowerConfigShutdownProbe.class.getName(), persisted.toString())
        .redirectErrorStream(true).start();
    try {
      assertTrue(child.waitFor(10, TimeUnit.SECONDS), "shutdown save timed out");
      assertEquals(0, child.exitValue(), new String(child.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
      assertTrue(JsonParser.parseString(Files.readString(persisted)).getAsJsonObject().getAsJsonObject("settings")
          .getAsJsonObject("shutdown").get("enabled").getAsBoolean());
    } finally { if (child.isAlive()) child.destroyForcibly(); }
  }
  @Test void incompatibleTemplatesArePreservedForReviewWithoutBlockingUnrelatedPreferences() throws Exception {
    JsonObject root = new JsonObject(); root.addProperty("schemaVersion", 9);
    JsonObject settings = new JsonObject(), templates = new JsonObject(), future = new JsonObject();
    future.addProperty("version", 99); future.add("values", new JsonObject()); templates.add("survival", future);
    settings.add("gameRules", templates); root.add("settings", settings); PowerConfig.write(root);
    JsonObject other = new JsonObject(); other.addProperty("test", true); PowerConfig.put("unrelated", other);
    assertEquals(templates, PowerConfig.section("gameRules"));
    assertThrows(IllegalArgumentException.class, local.luke.power.rules.RulePresets::survival);
  }

  @Test void audioEditsSplitUserDataAndPreserveIndependentlyQueuedRequests() throws Exception {
    PowerConfig.initializeMusicData();
    JsonObject initial = JsonParser.parseString("""
        {"master":60,"playlist":"quiet","menuPlaylist":"@world",
         "playlists":[{"id":"quiet","name":"Quiet","tracks":["music:missing","music:a"]}],
         "sounds":{"random.click":35,"music:missing":100},"favourites":["music:missing","music:a"],
         "playlistOrder":["quiet","favorites","beta"],
         "libraryGroupVolumes":{"Alpha":23},"libraryOrder":["Alpha","source:/unavailable"],
         "musicDirectories":["/unavailable"]}
        """).getAsJsonObject();
    PowerConfig.saveAudio(initial); JsonObject staleDraft = PowerConfig.audio();
    JsonObject requests = JsonParser.parseString("{\"tracks\":[\"music:missing\",\"music:a\",\"music:missing\"]}").getAsJsonObject();
    PowerConfig.saveMusicQueueDeferred(requests); requests.getAsJsonArray("tracks").add("music:newer-mutation");
    staleDraft.addProperty("master", 80); PowerConfig.saveAudio(staleDraft);
    assertEquals(3, PowerConfig.musicQueue().getAsJsonArray("tracks").size());
    assertEquals(80, PowerConfig.audio().get("master").getAsInt());
    JsonObject preferences = PowerConfig.section("audio");
    assertFalse(preferences.has("playlists")); assertFalse(preferences.has("playlistTracks"));
    assertFalse(preferences.getAsJsonObject("sounds").has("music:missing"));
    JsonObject music = MusicData.read(PowerConfig.musicPath()).getAsJsonObject("audio");
    assertFalse(music.has("master")); assertFalse(music.getAsJsonObject("sounds").has("random.click"));
    assertEquals(100, music.getAsJsonObject("sounds").get("music:missing").getAsInt());
    assertFalse(PowerConfig.document().getAsJsonObject("settings").has("musicQueue"));
    for (String field : new String[]{"favourites", "libraryGroupVolumes", "libraryOrder", "playlistOrder", "musicDirectories"}) {
      assertFalse(preferences.has(field));
      assertEquals(initial.get(field), music.get(field));
      assertEquals(initial.get(field), PowerConfig.audio().get(field));
    }
    JsonObject current = PowerConfig.audio();
    current.addProperty("playlist", "favorites");
    current.addProperty("menuPlaylist", "favorites");
    PowerConfig.saveAudio(current);
    assertEquals("favorites", PowerConfig.audio().get("playlist").getAsString());
    assertEquals(initial.get("favourites"), PowerConfig.audio().get("favourites"));
    assertEquals(3, PowerConfig.musicQueue().getAsJsonArray("tracks").size());
  }

  @Test void damagedMusicBlocksMusicSavesButUnrelatedPreferencesRemainWritable() throws Exception {
    PowerConfig.initializeMusicData();
    byte[] originalConfig;
    JsonObject preferences = new JsonObject(); preferences.addProperty("value", 1); PowerConfig.put("unrelated", preferences);
    originalConfig = Files.readAllBytes(PowerConfig.path());
    for (String original : new String[]{"broken", "{\"formatVersion\":99,\"audio\":{},\"queue\":{\"tracks\":[]}}"}) {
      Files.writeString(PowerConfig.musicPath(), original);
      assertThrows(IllegalStateException.class, PowerConfig::audio);
      assertThrows(java.io.IOException.class, () -> PowerConfig.saveAudio(new JsonObject()));
      assertArrayEquals(originalConfig, Files.readAllBytes(PowerConfig.path()));
      assertEquals(original, Files.readString(PowerConfig.musicPath()));
    }
    preferences.addProperty("value", 2); PowerConfig.put("unrelated", preferences);
    assertEquals(2, PowerConfig.section("unrelated").get("value").getAsInt());
    assertTrue(Files.readString(PowerConfig.musicPath()).contains("99"));
    Files.delete(PowerConfig.musicPath());
    assertThrows(IllegalStateException.class, PowerConfig::audio);
    assertThrows(java.io.IOException.class, () -> PowerConfig.saveMusicQueue(JsonParser.parseString("{\"tracks\":[]}").getAsJsonObject()));
    assertFalse(Files.exists(PowerConfig.musicPath()));
  }

  @Test void deferredQueueNeverWaitsForDiskAndRetriesWithoutBlockingUnrelatedSettings() throws Exception {
    PowerConfig.initializeMusicData();
    var field = PowerConfig.class.getDeclaredField("DISK"); field.setAccessible(true);
    var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
    var workers = Executors.newFixedThreadPool(2);
    JsonObject queue = JsonParser.parseString("{\"tracks\":[\"music:missing\"]}").getAsJsonObject();
    try {
      workers.submit(() -> { synchronized (field.get(null)) { entered.countDown(); release.await(5, TimeUnit.SECONDS); } return null; });
      assertTrue(entered.await(5, TimeUnit.SECONDS));
      workers.submit(() -> PowerConfig.saveMusicQueueDeferred(queue)).get(200, TimeUnit.MILLISECONDS);
    } finally { release.countDown(); workers.shutdownNow(); }
    PowerConfig.flushPending();
    assertEquals(queue, PowerConfig.musicQueue());
    byte[] music = Files.readAllBytes(PowerConfig.musicPath()); Files.writeString(PowerConfig.musicPath(), "broken");
    try {
      PowerConfig.saveMusicQueueDeferred(queue);
      assertThrows(java.io.IOException.class, PowerConfig::flushPending);
      JsonObject preference = new JsonObject(); preference.addProperty("value", true); PowerConfig.put("unrelated", preference);
      assertEquals(preference, PowerConfig.section("unrelated"));
      assertNotNull(PowerConfig.takeSaveFailure());
      assertEquals("broken", Files.readString(PowerConfig.musicPath()));
    } finally { Files.write(PowerConfig.musicPath(), music); PowerConfig.flushPending(); }
  }

  @Test void invalidAudioDraftsLeaveBothDocumentsUnchanged() throws Exception {
    PowerConfig.initializeMusicData(); PowerConfig.saveAudio(new JsonObject());
    byte[] config = Files.readAllBytes(PowerConfig.path()), music = Files.readAllBytes(PowerConfig.musicPath());
    JsonObject invalid = PowerConfig.audio();
    invalid.add("playlistTracks", JsonParser.parseString("[\"music:a\",\"music:a\"]"));
    assertThrows(IllegalArgumentException.class, () -> PowerConfig.saveAudio(invalid));
    assertArrayEquals(config, Files.readAllBytes(PowerConfig.path())); assertArrayEquals(music, Files.readAllBytes(PowerConfig.musicPath()));
    for (String bad : new String[]{"{\"libraryGroupVolumes\":{\"Alpha\":101}}",
        "{\"libraryOrder\":[\"Alpha\",\"Alpha\"]}", "{\"disabledMusicDirectories\":[]}"}) {
      JsonObject badDraft = PowerConfig.audio();
      JsonParser.parseString(bad).getAsJsonObject().entrySet().forEach(e -> badDraft.add(e.getKey(), e.getValue()));
      assertThrows(IllegalArgumentException.class, () -> PowerConfig.saveAudio(badDraft));
      assertArrayEquals(config, Files.readAllBytes(PowerConfig.path()));
      assertArrayEquals(music, Files.readAllBytes(PowerConfig.musicPath()));
    }
    JsonObject oversized = PowerConfig.audio(); oversized.addProperty("unknownFuturePreference", "x".repeat(4 * 1024 * 1024));
    oversized.add("favourites", JsonParser.parseString("[\"music:unsaved\"]"));
    assertThrows(java.io.IOException.class, () -> PowerConfig.saveAudio(oversized));
    assertArrayEquals(config, Files.readAllBytes(PowerConfig.path())); assertArrayEquals(music, Files.readAllBytes(PowerConfig.musicPath()));
  }
  @Test void invalidMenuSelectionCannotTurnAReadFailureIntoAnEmptyLibrarySave() throws Exception {
    PowerConfig.initializeMusicData(); PowerConfig.saveAudio(new JsonObject());
    JsonObject healthy = PowerConfig.audio();
    healthy.add("favourites", JsonParser.parseString("[\"music:retained\"]"));
    PowerConfig.saveAudio(healthy);
    JsonObject invalid = PowerConfig.document();
    invalid.getAsJsonObject("settings").getAsJsonObject("audio").addProperty("menuPlaylist", "");
    Files.writeString(PowerConfig.path(), invalid.toString());
    byte[] config = Files.readAllBytes(PowerConfig.path()), music = Files.readAllBytes(PowerConfig.musicPath());
    assertThrows(RuntimeException.class, PowerConfig::audio);
    assertThrows(RuntimeException.class, () -> PowerConfig.saveAudio(new JsonObject()));
    assertArrayEquals(config, Files.readAllBytes(PowerConfig.path()));
    assertArrayEquals(music, Files.readAllBytes(PowerConfig.musicPath()));
  }
}
