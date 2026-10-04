package local.luke.power.audio;

import java.io.*;
import java.nio.file.*;
import java.util.Random;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AudioResourceTest {
  @TempDir Path directory;

  @Test void compressedJarReadsRemainCompleteUntilActualEndOfFile() throws Exception {
    byte[] data = new byte[83_017]; new Random(17).nextBytes(data);
    Path jar = directory.resolve("audio.jar");
    try (var zip = new ZipOutputStream(Files.newOutputStream(jar))) {
      zip.putNextEntry(new ZipEntry("track.ogg")); zip.write(data); zip.closeEntry();
    }
    var source = java.net.URI.create("jar:" + jar.toUri() + "!/track.ogg").toURL();
    var wrapped = AudioResource.streaming(source);
    assertEquals(source.toExternalForm(), wrapped.toExternalForm());
    for (int attempt = 0; attempt < 2; attempt++) try (var input = wrapped.openStream()) {
      var output = new ByteArrayOutputStream(); byte[] buffer = new byte[4096];
      int count;
      while ((count = input.read(buffer)) >= 0) {
        assertEquals(Math.min(buffer.length, data.length - output.size()), count);
        output.write(buffer, 0, count);
      }
      assertArrayEquals(data, output.toByteArray());
      assertEquals(0, input.read(buffer, 0, 0));
      assertEquals(-1, input.read(buffer));
    }
  }

  @Test void fragmentedReadsPreserveOffsetsErrorsAndCloseOwnership() throws Exception {
    boolean[] closed = {false};
    InputStream fragmented = new ByteArrayInputStream(new byte[]{1,2,3,4,5}) {
      @Override public synchronized int read(byte[] b, int off, int len) { return super.read(b,off,Math.min(1,len)); }
      @Override public void close() { closed[0] = true; }
    };
    try (var input = AudioResource.completeReads(fragmented)) {
      byte[] bytes = new byte[8];
      assertEquals(5, input.read(bytes,2,6));
      assertArrayEquals(new byte[]{0,0,1,2,3,4,5,0},bytes);
      assertThrows(IndexOutOfBoundsException.class,()->input.read(bytes,0,9));
      assertEquals(-1,input.read());
    }
    assertTrue(closed[0]);
  }
}
