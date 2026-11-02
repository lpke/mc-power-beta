package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.storage.PowerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MusicRequestsTest {
  @TempDir Path game;

  @Test void retainedSnapshotsStayImmutableAndOnlyTheirOwnHeadCanBeConsumed() throws Exception {
    PowerConfig.flushPending();
    Map<Field, Object> saved = new LinkedHashMap<>();
    for (String name : List.of("file", "recoveryDefaults", "problem")) {
      Field field = PowerConfig.class.getDeclaredField(name); field.setAccessible(true);
      saved.put(field, field.get(null));
    }
    Field queue = MusicRequests.class.getDeclaredField("queue"); queue.setAccessible(true);
    saved.put(queue, queue.get(null));
    try {
      for (Field field : saved.keySet()) if (field.getDeclaringClass() == PowerConfig.class)
        field.set(null, field.getName().equals("problem") ? "" : null);
      PowerConfig.configure(game.resolve("config/power-beta.json"));
      PowerConfig.initializeMusicData();
      MusicRequests.edit(value -> {
        value.tracks.clear(); value.add("music:same"); value.add("music:same");
      });
      List<String> original = MusicRequests.tracks();
      assertSame(original, MusicRequests.tracks());
      assertThrows(UnsupportedOperationException.class, () -> original.clear());
      MusicRequests.edit(value -> { });
      List<String> edited = MusicRequests.tracks();
      assertEquals(original, edited); assertNotSame(original, edited);
      assertFalse(MusicRequests.consume(original));
      assertTrue(MusicRequests.consume(edited));
      assertEquals(List.of("music:same"), MusicRequests.tracks());
      assertEquals(List.of("music:same", "music:same"), original);
      assertFalse(MusicRequests.consume(edited));
      List<String> valid = MusicRequests.tracks();
      assertThrows(IllegalArgumentException.class, () -> MusicRequests.edit(value -> value.tracks.add("invalid")));
      assertSame(valid, MusicRequests.tracks());
      PowerConfig.flushPending();
      assertEquals(valid, PowerConfig.readMusicQueue(MusicQueue.class).tracks);
    } finally {
      PowerConfig.flushPending();
      for (var entry : saved.entrySet()) entry.getKey().set(null, entry.getValue());
    }
  }
}
