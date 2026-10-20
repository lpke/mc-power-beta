package local.luke.power.chat;
import com.google.gson.*;
import java.nio.file.*;
import java.util.List;
import local.luke.power.storage.PowerConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class ChatNotesTest {
  @TempDir Path dir;
  Object previous;
  void reset() throws Exception {
    var loaded = ChatNotes.class.getDeclaredField("loaded"); loaded.setAccessible(true); loaded.set(null, false);
    var readonly = ChatNotes.class.getDeclaredField("readOnly"); readonly.setAccessible(true); readonly.set(null, false);
    var error = ChatNotes.class.getDeclaredField("error"); error.setAccessible(true); error.set(null, "");
    var messages = ChatNotes.class.getDeclaredField("messages"); messages.setAccessible(true); ((List<?>)messages.get(null)).clear();
  }
  @BeforeEach void isolate() throws Exception {
    PowerConfig.flushPending(); var file = PowerConfig.class.getDeclaredField("file"); file.setAccessible(true); previous = file.get(null); file.set(null, null);
    PowerConfig.configure(dir.resolve("config.json")); reset(); ChatNotes.limits(() -> 1, () -> 2);
  }
  @AfterEach void restore() throws Exception {
    PowerConfig.flushPending(); reset(); ChatNotes.limits(() -> 100, () -> 100);
    var file = PowerConfig.class.getDeclaredField("file"); file.setAccessible(true); file.set(null, previous);
  }
  @Test void onlySubmittedMessagesSurviveWorkerFlushAndRestartWithTheirOwnLimit() throws Exception {
    ChatNotes.record("first note"); ChatNotes.record("second note"); ChatNotes.record("/waypoint list");
    PowerConfig.flushPending(); reset();
    assertEquals(List.of("second note", "/waypoint list"), ChatNotes.history());
    assertEquals(1, ChatNotes.otherLimit());
  }
  @Test void futureNotesStayReadOnlyAndAreNeverReplacedByNewSubmissions() throws Exception {
    JsonObject data = new JsonObject(); data.addProperty("version", 99); data.add("messages", new JsonArray());
    PowerConfig.put("chatNotes", data); ChatNotes.record("unsaved note"); PowerConfig.flushPending();
    assertEquals(data, PowerConfig.section("chatNotes")); assertFalse(ChatNotes.error().isBlank());
  }
}
