package local.luke.power.world;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldSnapshotFilesTest {
  @TempDir Path root;
  @Test void continuouslyGrowingSourceStopsBeforeAnyBytesBeyondOriginalSizeAreWritten() throws Exception {
    Path file = root.resolve("growing"); Files.write(file, new byte[]{1});
    var source = new WorldSnapshotFiles.Source(file, "growing", "world", WorldSnapshotFiles.stamp(file));
    ByteArrayOutputStream received = new ByteArrayOutputStream();
    OutputStream growing = new OutputStream() {
      public void write(int value) throws IOException { received.write(value); Files.write(file, new byte[]{2, 3}, StandardOpenOption.APPEND); }
      public void write(byte[] bytes, int start, int length) throws IOException {
        received.write(bytes, start, length); Files.write(file, new byte[]{2, 3}, StandardOpenOption.APPEND);
      }
    };
    assertThrows(IOException.class, () -> WorldSnapshotFiles.transfer(source, growing, () -> false));
    assertEquals(1, received.size()); assertEquals(3, Files.size(file));
  }
}
