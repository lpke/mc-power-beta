package local.luke.power.world;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SavedWorldAccessTest {
  @TempDir Path game;
  private SavedWorldAccess create() throws Exception {
    Files.createDirectories(game.resolve("saves/World")); return new SavedWorldAccess(game, "World", () -> true);
  }
  @Test void leaseExcludesOtherOfflineEditsAndWorldOpeningAcrossAllWorlds() throws Exception {
    var access = create(); Files.createDirectories(game.resolve("saves/Other"));
    try (var lease = access.acquire()) {
      access.checkIdle(lease);
      assertThrows(IOException.class, access::checkIdle);
      assertThrows(IOException.class, () -> SavedWorldAccess.requireOpenable(game, "Other"));
      assertThrows(IOException.class, () -> new SavedWorldAccess(game, "Other", () -> true));
    }
    SavedWorldAccess.requireOpenable(game, "World"); try (var lease = access.acquire()) { access.checkIdle(lease); }
  }
  @Test void closedOrWrongInstanceLeaseCannotAuthorizeAnOperation() throws Exception {
    var access = create(); var lease = access.acquire(); lease.close();
    assertThrows(IOException.class, () -> access.checkIdle(lease));
  }
  @Test void loadedWorldChangesAreRecheckedAfterAcquiringLease() throws Exception {
    create(); AtomicBoolean idle = new AtomicBoolean(true); var access = new SavedWorldAccess(game, "World", idle::get);
    try (var lease = access.acquire()) { idle.set(false); assertThrows(IOException.class, () -> access.checkIdle(lease)); }
  }
  @Test void traversalAndLinkedDirectoriesAreRejected() throws Exception {
    create(); assertThrows(IOException.class, () -> new SavedWorldAccess(game, "../World", () -> true));
    assertThrows(IOException.class, () -> SavedWorldAccess.requireOpenable(game, "../World"));
    Files.createSymbolicLink(game.resolve("saves/Link"), game.resolve("saves/World"));
    assertThrows(IOException.class, () -> new SavedWorldAccess(game, "Link", () -> true));
  }
}
