package local.luke.power.map;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import local.luke.power.storage.DurableFiles;

final class MapFiles {
  private MapFiles() {}

  /**
   * A failed write leaves the previous file in place. Temporary files belong to this write only.
   */
  static void replace(Path target, byte[] bytes) throws IOException {
    Files.createDirectories(target.getParent());
    Path temp = Files.createTempFile(target.getParent(), ".map-", ".pending");
    try {
      try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        while (buffer.hasRemaining()) channel.write(buffer);
        channel.force(true);
      }
      if (!java.util.Arrays.equals(bytes, Files.readAllBytes(temp)))
        throw new IOException("Map write verification failed");
      try {
        Files.move(
            temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        throw new IOException("Map storage needs atomic file replacement", e);
      }
      DurableFiles.syncDirectory(target.getParent());
    } finally {
      Files.deleteIfExists(temp);
    }
  }
}
