package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class FileTransactionTest {
  @TempDir Path game;

  @Test
  void rollbackRestoresExactBytesAndRemovesNewFiles() throws Exception {
    Path a = game.resolve("config/a"), b = game.resolve("options.txt");
    Files.createDirectories(a.getParent());
    byte[] before = {0, 1, 10, -1};
    Files.write(a, before);
    try (var tx = FileTransaction.begin(game, List.of(a, b))) {
      Files.writeString(a, "changed");
      Files.writeString(b, "new");
    }
    assertArrayEquals(before, Files.readAllBytes(a));
    assertFalse(Files.exists(b));
  }

  @Test
  void completedTransactionSurvivesRecovery() throws Exception {
    Path a = game.resolve("options.txt");
    Files.writeString(a, "old");
    try (var tx = FileTransaction.begin(game, List.of(a))) {
      Files.writeString(a, "new");
      tx.commit();
    }
    FileTransaction.recover(game);
    assertEquals("new", Files.readString(a));
  }

  @Test
  void interruptedSaveRecoversBeforeStartup() throws Exception {
    Path a = game.resolve("options.txt");
    Files.writeString(a, "old");
    FileTransaction.begin(game, List.of(a));
    Files.writeString(a, "half-written");
    FileTransaction.recover(game);
    assertEquals("old", Files.readString(a));
    FileTransaction.recover(game);
    assertEquals("old", Files.readString(a));
  }

  @Test
  void refusesFilesOutsideTheInstance() throws Exception {
    assertThrows(
        java.io.IOException.class,
        () -> FileTransaction.begin(game, List.of(game.resolve("../outside"))));
  }

  @Test
  void refusesFileSymlinks() throws Exception {
    Path a = game.resolve("options.txt"), b = game.resolve("other.txt");
    Files.writeString(b, "safe");
    Files.createSymbolicLink(a, b);
    assertThrows(java.io.IOException.class, () -> FileTransaction.begin(game, List.of(a)));
    assertEquals("safe", Files.readString(b));
  }

  @Test
  void refusesLinkedParentDirectories() throws Exception {
    Path outside = Files.createTempDirectory(game.getParent(), "external-config-");
    Files.createSymbolicLink(game.resolve("config"), outside);
    assertThrows(
        java.io.IOException.class,
        () -> FileTransaction.begin(game, List.of(game.resolve("config/settings.json"))));
    assertFalse(Files.exists(outside.resolve("power-beta")));
  }

  @Test
  void recoveryRefusesAParentReplacedWithALink() throws Exception {
    Path folder = Files.createDirectory(game.resolve("settings")), file = folder.resolve("a.json");
    Files.writeString(file, "old");
    FileTransaction.begin(game, List.of(file));
    Files.delete(file);
    Files.delete(folder);
    Path other = Files.createDirectory(game.resolve("other"));
    Files.writeString(other.resolve("a.json"), "untouched");
    Files.createSymbolicLink(folder, other);
    assertThrows(java.io.IOException.class, () -> FileTransaction.recover(game));
    assertEquals("untouched", Files.readString(other.resolve("a.json")));
  }
}
