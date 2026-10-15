package local.luke.power.storage;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class DurableFiles {
  private DurableFiles() {}

  /** Windows does not expose directory handles through FileChannel; file writes still use force. */
  public static void syncDirectory(Path directory) throws IOException {
    if (System.getProperty("os.name", "").startsWith("Windows")) return;
    try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) { channel.force(true); }
  }
}
