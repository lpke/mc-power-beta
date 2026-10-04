package local.luke.power.audio;

import java.io.*;
import java.net.*;

/** Adapts compressed resource streams to the complete reads Beta's OGG decoder expects. */
public final class AudioResource {
  private AudioResource() {}

  public static URL streaming(URL resource) {
    try {
      return new URL(null, resource.toExternalForm(), new URLStreamHandler() {
        @Override protected URLConnection openConnection(URL url) {
          return new URLConnection(url) {
            @Override public void connect() { connected = true; }
            @Override public InputStream getInputStream() throws IOException {
              connect();
              return completeReads(resource.openStream());
            }
          };
        }
      });
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid audio resource", e);
    }
  }

  static InputStream completeReads(InputStream source) {
    return new FilterInputStream(new BufferedInputStream(source)) {
      @Override public int read(byte[] bytes, int offset, int length) throws IOException {
        // InflaterInputStream can return part of an OGG page even before EOF.
        // JOrbis in Beta treats that short read as the end of the stream.
        int count = in.readNBytes(bytes, offset, length);
        return count == 0 && length != 0 ? -1 : count;
      }
      @Override public int read(byte[] bytes) throws IOException {
        return read(bytes, 0, bytes.length);
      }
    };
  }
}
