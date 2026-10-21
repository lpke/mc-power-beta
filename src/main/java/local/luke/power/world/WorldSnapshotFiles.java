package local.luke.power.world;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Bounded streaming and source revalidation; this class never interprets world contents. */
final class WorldSnapshotFiles {
  static final int MAX_FILES = 200000, BUFFER = 65536;
  record Stamp(long size, java.nio.file.attribute.FileTime modified, Object key, boolean directory) {}
  record Source(Path path, String entry, String kind, Stamp stamp) {}
  record Proof(long bytes, String sha256) {}
  private WorldSnapshotFiles() {}

  static List<Source> collect(Path game, Path world, WorldOwnedData.Discovery external, BooleanSupplier cancelled) throws IOException {
    List<Source> result = new ArrayList<>();
    walk(world, "world/", "world", result, cancelled);
    for (var file : external.files()) {
      String relative = game.relativize(file.path()).toString().replace(File.separatorChar, '/');
      walk(file.path(), "external/" + relative, file.kind(), result, cancelled);
    }
    result.sort(Comparator.comparing(Source::entry));
    Set<String> entries = new HashSet<>();
    for (Source source : result) if (!entries.add(source.entry)) throw new IOException("World snapshot contains duplicate paths.");
    return List.copyOf(result);
  }

  private static void walk(Path root, String prefix, String kind, List<Source> result, BooleanSupplier cancelled) throws IOException {
    SavedWorldAccess.safe(root);
    Files.walkFileTree(root, new SimpleFileVisitor<>() {
      private void add(Path path, BasicFileAttributes attributes) throws IOException {
        check(cancelled); SavedWorldAccess.safe(path);
        if (attributes.isSymbolicLink() || !attributes.isDirectory() && !attributes.isRegularFile())
          throw new IOException("World data contains an unsupported file.");
        if (result.size() >= MAX_FILES) throw new IOException("Too many files for one world operation.");
        String relative = root.relativize(path).toString().replace(File.separatorChar, '/');
        String entry = relative.isEmpty() ? prefix : prefix.replaceAll("/$", "") + "/" + relative;
        if (attributes.isDirectory() && !entry.endsWith("/")) entry += "/";
        if (entry.length() > 4096) throw new IOException("World data path is too long.");
        result.add(new Source(path, entry, kind, stamp(attributes)));
      }
      public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) throws IOException { add(directory, attrs); return FileVisitResult.CONTINUE; }
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException { add(file, attrs); return FileVisitResult.CONTINUE; }
    });
  }

  static Stamp stamp(Path file) throws IOException {
    SavedWorldAccess.safe(file);
    return stamp(Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
  }
  private static Stamp stamp(BasicFileAttributes attrs) { return new Stamp(attrs.size(), attrs.lastModifiedTime(), attrs.fileKey(), attrs.isDirectory()); }

  static Proof transfer(Source source, OutputStream output, BooleanSupplier cancelled) throws IOException {
    if (!source.stamp.equals(stamp(source.path))) throw new IOException("World data changed before it could be copied.");
    MessageDigest hash = digest(); long count = 0; byte[] buffer = new byte[BUFFER];
    try (InputStream input = Files.newInputStream(source.path, LinkOption.NOFOLLOW_LINKS)) {
      for (int read; (read = input.read(buffer)) >= 0;) {
        check(cancelled); if (read == 0) continue;
        count = Math.addExact(count, read);
        if (count > source.stamp.size) throw new IOException("World data grew while it was copied. The original was preserved.");
        hash.update(buffer, 0, read); output.write(buffer, 0, read);
      }
    } catch (ArithmeticException tooLarge) { throw new IOException("World file exceeds the supported size.", tooLarge); }
    if (count != source.stamp.size || !source.stamp.equals(stamp(source.path))) throw new IOException("World data changed while it was copied.");
    return new Proof(count, HexFormat.of().formatHex(hash.digest()));
  }

  static Proof hash(Path file, BooleanSupplier cancelled) throws IOException {
    Source source = new Source(file, "", "", stamp(file));
    return transfer(source, OutputStream.nullOutputStream(), cancelled);
  }

  static Proof write(Source source, Path target, BooleanSupplier cancelled) throws IOException {
    SavedWorldAccess.safe(target); Files.createDirectories(target.getParent()); SavedWorldAccess.safe(target);
    Proof proof;
    try (OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
      proof = transfer(source, output, cancelled);
    }
    force(target);
    if (!proof.equals(hash(target, cancelled))) throw new IOException("Copied world data could not be verified.");
    return proof;
  }

  static void force(Path file) throws IOException {
    try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) { channel.force(true); }
  }

  static void check(BooleanSupplier cancelled) throws IOException {
    if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) throw new InterruptedIOException("World operation canceled. Original world preserved.");
  }
  private static MessageDigest digest() {
    try { return MessageDigest.getInstance("SHA-256"); }
    catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
  }
}
