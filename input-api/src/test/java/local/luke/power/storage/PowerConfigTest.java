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
}
