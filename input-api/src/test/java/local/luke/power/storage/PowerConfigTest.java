package local.luke.power.storage;

import com.google.gson.*;
import java.nio.file.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PowerConfigTest {
  @TempDir static Path dir;
  @BeforeAll static void setup() { PowerConfig.configure(dir.resolve("settings.json")); }
  @BeforeEach void clear() throws Exception { Files.deleteIfExists(PowerConfig.path()); }
  @AfterEach void finish() throws Exception { PowerConfig.flushPending(); PowerConfig.takeSaveFailure(); }
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
    try(var files=Files.list(dir)) { assertFalse(files.anyMatch(p->p.toString().endsWith(".tmp"))); }
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
}
