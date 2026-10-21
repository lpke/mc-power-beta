package local.luke.power.storage;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.util.ArrayList;

public final class DurableFiles {
  private DurableFiles() {}

  /** Force every new directory link, including the first feature-data directory under an instance. */
  public static void createDirectories(Path directory) throws IOException {
    var missing = new ArrayList<Path>();
    for (Path path = directory.toAbsolutePath().normalize(); path != null && !Files.exists(path); path = path.getParent())
      missing.add(path);
    Files.createDirectories(directory);
    for (int i = missing.size() - 1; i >= 0; i--) {
      Path created = missing.get(i);
      if (created.getParent() != null) syncDirectory(created.getParent());
      syncDirectory(created);
    }
  }

  /** Windows does not expose directory handles through FileChannel; file writes still use force. */
  public static void syncDirectory(Path directory) throws IOException {
    if (System.getProperty("os.name", "").startsWith("Windows")) return;
    try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) { channel.force(true); }
  }
}
