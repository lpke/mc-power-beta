package local.luke.power.audio;

import java.net.*;
import java.nio.file.Path;

/** File URLs from scanner metadata. Path.toUri may query the filesystem for directory suffixes. */
final class MusicTrackUrls {
  private MusicTrackUrls() {}

  static URL of(Path source) throws MalformedURLException {
    String path = source.toAbsolutePath().normalize().toString().replace(java.io.File.separatorChar, '/');
    if (!path.startsWith("/")) path = "/" + path;
    try { return new URL(new URI("file", "", path, null, null).toASCIIString()); }
    catch (URISyntaxException invalid) { throw new IllegalArgumentException("Invalid music path", invalid); }
  }

  static String key(Path source) {
    try { return of(source).toExternalForm(); }
    catch (MalformedURLException invalid) { throw new IllegalArgumentException("Invalid music URL", invalid); }
  }
}
