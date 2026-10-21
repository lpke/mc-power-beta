package local.luke.power.world;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class OwnedMetadataTransactionTest {
  @TempDir Path game;
  Path world, level, old;
  final byte[] first = {1, 2, 3}, previous = {4, 5}, next = {6, 7}, nextOld = {8, 9};
  @BeforeEach void setup() throws Exception {
    world = game.resolve("saves/Example"); Files.createDirectories(world);
    level = world.resolve("level.dat"); old = world.resolve("level.dat_old");
    Files.write(level, first); Files.write(old, previous);
  }
  Map<Path, byte[]> before() { return Map.of(level, first, old, previous); }
  Map<Path, byte[]> after() { return Map.of(level, next, old, nextOld); }
  Path manifest() throws IOException {
    try (var paths = Files.walk(game.resolve("power-beta-data/world-metadata-backups"))) {
      return paths.filter(p -> p.getFileName().toString().equals("transaction.json")).findFirst().orElseThrow();
    }
  }
  @Test void commitKeepsVerifiedOriginalEvidenceAndBothIntendedResults() throws Exception {
    try (var transaction = OwnedMetadataTransaction.begin(game, before(), after())) {
      transaction.apply(); transaction.commit();
    }
    assertArrayEquals(next, Files.readAllBytes(level)); assertArrayEquals(nextOld, Files.readAllBytes(old));
    assertEquals("complete", JsonParser.parseString(Files.readString(manifest())).getAsJsonObject().get("state").getAsString());
    OwnedMetadataTransaction.recover(game, world);
    assertArrayEquals(next, Files.readAllBytes(level));
  }
  @Test void anUncommittedPublicationRestoresBothOriginals() throws Exception {
    try (var transaction = OwnedMetadataTransaction.begin(game, before(), after())) { transaction.apply(); }
    assertArrayEquals(first, Files.readAllBytes(level)); assertArrayEquals(previous, Files.readAllBytes(old));
  }
  @Test void externalChangeBeforeFirstWriteIsPreservedWithoutArmingRollback() throws Exception {
    var transaction = OwnedMetadataTransaction.begin(game, before(), after());
    byte[] foreign = {22}; Files.write(level, foreign);
    assertThrows(IOException.class, transaction::apply);
    transaction.close();
    assertArrayEquals(foreign, Files.readAllBytes(level)); assertArrayEquals(previous, Files.readAllBytes(old));
    OwnedMetadataTransaction.recover(game, world);
    assertArrayEquals(foreign, Files.readAllBytes(level));
  }
  @Test void foreignBytesMatchingAnUnattemptedAfterImageAreNeverRolledBack() throws Exception {
    var transaction = OwnedMetadataTransaction.begin(game, before(), after()); Files.write(level, next);
    assertThrows(IOException.class, transaction::apply); transaction.close();
    assertArrayEquals(next, Files.readAllBytes(level)); assertArrayEquals(previous, Files.readAllBytes(old));
  }
  @Test void oneForeignTargetPreventsEveryRollbackReplacement() throws Exception {
    var transaction = OwnedMetadataTransaction.begin(game, before(), after()); transaction.apply();
    byte[] foreign = {33}; Files.write(old, foreign);
    assertThrows(IOException.class, transaction::close);
    assertArrayEquals(next, Files.readAllBytes(level)); assertArrayEquals(foreign, Files.readAllBytes(old));
  }
  @Test void recoveryAfterInterruptedPublicationIsIdempotent() throws Exception {
    var transaction = OwnedMetadataTransaction.begin(game, before(), after()); transaction.apply();
    OwnedMetadataTransaction.recover(game, world); OwnedMetadataTransaction.recover(game, world);
    assertArrayEquals(first, Files.readAllBytes(level)); assertArrayEquals(previous, Files.readAllBytes(old));
  }
  @Test void aDamagedBackupNeverOverwritesWorldFiles() throws Exception {
    var transaction = OwnedMetadataTransaction.begin(game, before(), after()); transaction.apply();
    JsonObject record = JsonParser.parseString(Files.readString(manifest())).getAsJsonObject();
    String evidence = record.getAsJsonArray("files").get(0).getAsJsonObject().get("before").getAsString();
    Files.write(manifest().getParent().resolve(evidence), new byte[]{99});
    assertThrows(IOException.class, () -> OwnedMetadataTransaction.recover(game, world));
    assertArrayEquals(next, Files.readAllBytes(level)); assertArrayEquals(nextOld, Files.readAllBytes(old));
  }
  @Test void unsupportedManifestAndEscapingTargetsAreRejected() throws Exception {
    var transaction = OwnedMetadataTransaction.begin(game, before(), after()); transaction.apply();
    JsonObject record = JsonParser.parseString(Files.readString(manifest())).getAsJsonObject(); record.addProperty("format", 999);
    Files.writeString(manifest(), record.toString());
    assertThrows(IOException.class, () -> OwnedMetadataTransaction.recover(game, world));
    assertThrows(IOException.class, () -> OwnedMetadataTransaction.begin(game, Map.of(game.resolve("config/level.dat"), first), Map.of(game.resolve("config/level.dat"), next)));
    assertArrayEquals(next, Files.readAllBytes(level));
  }
  @Test void aSymlinkOrOversizedInputCannotStartAWrite() throws Exception {
    Path link = world.resolve("link"); Files.createSymbolicLink(link, game.resolve("elsewhere"));
    assertThrows(IOException.class, () -> OwnedMetadataTransaction.begin(game, Map.of(link.resolve("level.dat"), first), Map.of(link.resolve("level.dat"), next)));
    assertThrows(IOException.class, () -> OwnedMetadataTransaction.begin(game, before(), Map.of(level, new byte[4 * 1024 * 1024 + 1])));
    assertArrayEquals(first, Files.readAllBytes(level));
  }
}
