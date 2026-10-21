package local.luke.power.building.hotbar;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Path;
import local.luke.power.input.WorldIdentity;
import org.junit.jupiter.api.Test;

class HotbarJournalIdentityTest {
  private static final String FIRST = "450fb4c0-aa01-4904-840c-f3ca9600d1d2";
  private static final String SECOND = "fb366239-93f1-44d3-a6a9-1e25a41b04cb";

  @Test void stableWorldAndPlayerOwnTheNewNamespace() throws Exception {
    Path game = Path.of("instance");
    Path first = HotbarJournalIdentity.path(game, FIRST, "Player");
    assertEquals(first, HotbarJournalIdentity.path(game, FIRST, "Player"));
    assertNotEquals(first, HotbarJournalIdentity.path(game, SECOND, "Player"));
    assertNotEquals(first, HotbarJournalIdentity.path(game, FIRST, "Other"));
    assertTrue(first.getFileName().toString().startsWith("world-"));
    assertEquals(game.resolve("power-beta-data/hotbar-backups"), first.getParent());
  }

  @Test void readsTheSharedCapabilityAndRejectsUnownedRecords() throws Exception {
    WorldIdentity properties = () -> FIRST;
    assertEquals(FIRST, HotbarJournalIdentity.world(properties));
    assertThrows(IOException.class, () -> HotbarJournalIdentity.world(new Object()));
    assertThrows(IOException.class, () -> HotbarJournalIdentity.world((WorldIdentity)() -> "invalid"));
    for (String id : new String[]{"", "1-1-1-1-1", FIRST.toUpperCase()})
      assertThrows(IOException.class, () -> HotbarJournalIdentity.path(Path.of("instance"), id, "Player"));
    assertThrows(IOException.class, () -> HotbarJournalIdentity.path(Path.of("instance"), FIRST, ""));
    assertThrows(IOException.class, () -> HotbarJournalIdentity.path(Path.of("instance"), FIRST, "A\nB"));
  }
}
