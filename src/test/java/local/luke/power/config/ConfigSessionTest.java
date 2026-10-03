package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class ConfigSessionTest {
  @TempDir Path game;

  static class Fake implements Backend {
    final String id;
    final Path file;
    int live = 1;
    boolean fail;

    Fake(String id, Path file) {
      this.id = id;
      this.file = file;
    }

    public String id() {
      return id;
    }

    public List<Path> files() {
      return List.of(file);
    }

    public void validate(Map<String, JsonElement> values) {}

    public void apply(Map<String, JsonElement> values) throws Exception {
      live = values.values().iterator().next().getAsInt();
      Files.writeString(file, "" + live);
      if (fail && live == 2) throw new java.io.IOException("injected failure");
    }
  }

  Setting setting(String id) {
    return new Setting(
        id,
        id,
        "General",
        "Group",
        id,
        "",
        Setting.Kind.INTEGER,
        new JsonPrimitive(1),
        new JsonPrimitive(1),
        0,
        10,
        1,
        List.of(),
        false);
  }

  @Test
  void failingLaterBackendRestoresFilesAndLiveState() throws Exception {
    Fake a = new Fake("a", game.resolve("a.txt")), b = new Fake("b", game.resolve("b.txt"));
    Files.writeString(a.file, "1");
    Files.writeString(b.file, "1");
    b.fail = true;
    ConfigSession s = new ConfigSession();
    Setting x = setting("a"), y = setting("b");
    s.add(a, List.of(x));
    s.add(b, List.of(y));
    x.value = y.value = new JsonPrimitive(2);
    assertThrows(Exception.class, () -> s.save(game));
    assertEquals(1, a.live);
    assertEquals(1, b.live);
    assertEquals("1", Files.readString(a.file));
    assertEquals("1", Files.readString(b.file));
    assertEquals(2, s.changes());
  }

  @Test
  void rejectsAllInvalidValuesBeforeAnyWrite() throws Exception {
    Fake a = new Fake("a", game.resolve("a.txt"));
    Setting x = setting("a");
    ConfigSession s = new ConfigSession();
    s.add(a, List.of(x));
    x.value = new JsonPrimitive(200);
    assertThrows(IllegalArgumentException.class, () -> s.save(game));
    assertEquals(1, a.live);
    assertFalse(Files.exists(a.file));
  }
}
