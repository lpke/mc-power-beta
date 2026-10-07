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

  static class PreviewFake extends Fake {
    PreviewFake(String id, Path file) { super(id, file); }
    public boolean previews(Setting s) { return true; }
    public void preview(Map<String, JsonElement> values) throws Exception {
      live = values.values().iterator().next().getAsInt();
      if (fail && live == 2) throw new java.io.IOException("preview failed");
    }
  }

  @Test void previewDoesNotSaveAndDiscardRestoresOriginal() throws Exception {
    PreviewFake b = new PreviewFake("a", game.resolve("a.txt"));
    ConfigSession session = new ConfigSession(); Setting value = setting("a");
    session.add(b, List.of(value));
    value.value = new JsonPrimitive(2); session.preview();
    assertEquals(2, b.live); assertEquals(1, session.changes());
    assertFalse(Files.exists(b.file));
    session.discard(); assertEquals(1, b.live); assertEquals(0, session.changes());
  }

  @Test void applyBecomesTheNewDiscardTarget() throws Exception {
    PreviewFake b = new PreviewFake("a", game.resolve("a.txt"));
    ConfigSession session = new ConfigSession(); Setting value = setting("a");
    session.add(b, List.of(value));
    value.value = new JsonPrimitive(2); session.preview(); session.save(game);
    value.value = new JsonPrimitive(3); session.preview(); session.discard();
    assertEquals(2, b.live); assertEquals("2", Files.readString(b.file));
  }

  @Test void failedPreviewRestoresAllAttemptedBackends() throws Exception {
    PreviewFake a = new PreviewFake("a", game.resolve("a.txt")), b = new PreviewFake("b", game.resolve("b.txt"));
    Setting x = setting("a"), y = setting("b"); ConfigSession session = new ConfigSession();
    session.add(a, List.of(x)); session.add(b, List.of(y));
    x.value = y.value = new JsonPrimitive(2); b.fail = true;
    assertThrows(Exception.class, session::preview);
    assertEquals(1, a.live); assertEquals(1, b.live);
    assertFalse(Files.exists(a.file)); assertFalse(Files.exists(b.file));
  }

  @Test void failedSaveAfterPreviewCanBeRetriedOrDiscarded() throws Exception {
    PreviewFake b = new PreviewFake("a", game.resolve("a.txt"));
    Setting value = setting("a"); ConfigSession session = new ConfigSession(); session.add(b, List.of(value));
    value.value = new JsonPrimitive(2); session.preview(); b.fail = true;
    assertThrows(Exception.class, () -> session.save(game));
    session.discard(); assertEquals(1, b.live);
    assertFalse(Files.exists(b.file));
  }

  @Test void failedSaveRestoresLaterUncommittedPreviewsOnDiscard() throws Exception {
    PreviewFake a = new PreviewFake("a", game.resolve("a.txt")), b = new PreviewFake("b", game.resolve("b.txt"));
    Setting x = setting("a"), y = setting("b"); ConfigSession session = new ConfigSession();
    session.add(a, List.of(x)); session.add(b, List.of(y));
    x.value = y.value = new JsonPrimitive(2); session.preview(); a.fail = true;
    assertThrows(Exception.class, () -> session.save(game));
    session.discard(); assertEquals(1, a.live); assertEquals(1, b.live);
  }

  @Test void disablingCheatsDiscardsHiddenDraftsAndRestoresTheirPreviewsOnly() throws Exception {
    var creative = new PreviewFake("creative", game.resolve("creative.txt"));
    var normal = new PreviewFake("normal", game.resolve("normal.txt"));
    Setting hidden = new Setting("creative.flightSpeed", "creative", "Creative", "Flight", "Speed", "",
        Setting.Kind.INTEGER, new JsonPrimitive(1), new JsonPrimitive(1), 0, 10, 1, List.of(), false);
    Setting qol = setting("normal");
    Setting cheats = new Setting("world.cheats", "world", "General", "Game", "Cheats", "", Setting.Kind.BOOLEAN,
        new JsonPrimitive(true), new JsonPrimitive(false), 0, 1, 1, List.of(), false);
    var session = new ConfigSession();
    session.add(creative, List.of(hidden)); session.add(normal, List.of(qol));
    session.add(new Fake("world", game.resolve("world.txt")), List.of(cheats));
    hidden.value = qol.value = new JsonPrimitive(2); session.preview();
    assertEquals(2, creative.live); assertEquals(2, normal.live);
    cheats.value = new JsonPrimitive(false); session.link(cheats); session.preview();
    assertEquals(1, creative.live); assertEquals(2, normal.live);
    assertFalse(hidden.changed()); assertTrue(qol.changed());
    cheats.value = new JsonPrimitive(true); session.link(cheats);
    assertEquals(1, hidden.value.getAsInt());
    session.discard(); assertEquals(1, normal.live);
  }
}
