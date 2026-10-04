import java.net.URI;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Arrays;
import local.luke.power.audio.AudioResource;
import paulscode.sound.codecs.CodecJOrbis;

/** Run with the Beta client JAR and build/classes/java/main on the classpath. */
class VerifyMusicStreams {
  record Decoded(long bytes, byte[] digest) {}

  static Decoded decode(java.net.URL url) throws Exception {
    var codec = new CodecJOrbis();
    try {
      if (!codec.initialize(url)) throw new AssertionError("Decoder initialization failed: " + url);
      var hash = MessageDigest.getInstance("SHA-256");
      long bytes = 0;
      while (!codec.endOfStream()) {
        var buffer = codec.read();
        if (buffer == null) break;
        bytes += buffer.audioData.length; hash.update(buffer.audioData);
      }
      if (bytes == 0 || !codec.endOfStream()) throw new AssertionError("Incomplete stream: " + url);
      return new Decoded(bytes,hash.digest());
    } finally { codec.cleanup(); }
  }

  public static void main(String[] args) throws Exception {
    Path jar = Path.of(args[0]).toAbsolutePath(), assets = Path.of(args[1]);
    int count = 0;
    try (var files = Files.list(assets)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".ogg")).sorted().toList()) {
        var packaged = URI.create("jar:" + jar.toUri() + "!/assets/powerbeta/music/" + file.getFileName()).toURL();
        Decoded direct = decode(file.toUri().toURL()), streamed = decode(AudioResource.streaming(packaged));
        if (direct.bytes != streamed.bytes || !Arrays.equals(direct.digest,streamed.digest))
          throw new AssertionError("Packaged PCM differs from source: " + file);
        System.out.println("PASS " + file.getFileName() + " " + direct.bytes + " decoded bytes");
        count++;
      }
    }
    System.out.println("PASS: " + count + " complete streams match source PCM byte for byte.");
  }
}
