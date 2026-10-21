package local.luke.power.building.hotbar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import local.luke.power.input.WorldIdentity;

/** New records have stable ownership; legacy name/seed journals remain untouched. */
final class HotbarJournalIdentity {
  static final int VERSION = 2;
  private HotbarJournalIdentity() {}

  static String world(Object properties) throws IOException {
    if (!(properties instanceof WorldIdentity identity))
      throw new IOException("The world has no stable recovery identity");
    return validate(identity.power$commandWorldId());
  }

  static Path path(Path game, String world, String player) throws IOException {
    world = validate(world);
    if (player == null || player.isBlank() || player.length() > 128
        || player.chars().anyMatch(Character::isISOControl))
      throw new IOException("The player has no valid recovery identity");
    String owner = world + "\n" + player;
    return game.resolve("power-beta-data/hotbar-backups")
        .resolve("world-" + UUID.nameUUIDFromBytes(owner.getBytes(StandardCharsets.UTF_8)) + ".journal");
  }

  private static String validate(String value) throws IOException {
    try {
      String canonical = UUID.fromString(value).toString();
      if (!canonical.equals(value)) throw new IllegalArgumentException();
      return canonical;
    } catch (IllegalArgumentException | NullPointerException invalid) {
      throw new IOException("The world has no valid stable recovery identity", invalid);
    }
  }
}
