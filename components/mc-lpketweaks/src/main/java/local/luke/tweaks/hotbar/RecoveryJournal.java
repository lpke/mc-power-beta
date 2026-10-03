package local.luke.tweaks.hotbar;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.Arrays;
import java.util.zip.CRC32;

/** Append-only, checksummed recovery records. Never deletes or truncates older snapshots. */
public final class RecoveryJournal {
  public static final int MAGIC = 0x4c504b48, VERSION = 1, HEADER = 20, MAX_RECORD = 1024 * 1024;
  public static final long MAX_FILE = 64L * 1024 * 1024;
  private final Path path;
  private final long limit;
  private long verifiedSize = -1, verifiedModified = -1;

  public RecoveryJournal(Path path) {
    this(path, MAX_FILE);
  }

  public RecoveryJournal(Path path, long limit) {
    this.path = path;
    this.limit = limit;
  }

  public synchronized void append(byte[] payload) throws IOException {
    if (payload.length == 0 || payload.length > MAX_RECORD)
      throw new IOException("Hotbar snapshot exceeds size limit");
    Files.createDirectories(path.getParent());
    boolean existed = Files.exists(path);
    try (FileChannel channel =
            FileChannel.open(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE);
        FileLock lock = channel.tryLock()) {
      if (lock == null) throw new IOException("Hotbar recovery journal is in use");
      long size = channel.size(), modified = Files.getLastModifiedTime(path).toMillis();
      if (size > limit - HEADER - payload.length)
        throw new IOException("Hotbar recovery journal is full; archive it before swapping again");
      if (size != verifiedSize || modified != verifiedModified) validate(channel, size);
      CRC32 crc = new CRC32();
      crc.update(payload);
      ByteBuffer record = ByteBuffer.allocate(HEADER + payload.length);
      record
          .putInt(MAGIC)
          .putInt(VERSION)
          .putInt(payload.length)
          .putLong(crc.getValue())
          .put(payload)
          .flip();
      channel.position(size);
      while (record.hasRemaining()) channel.write(record);
      channel.force(true);
      channel.position(size);
      ByteBuffer check = ByteBuffer.allocate(record.capacity());
      read(channel, check);
      if (!Arrays.equals(record.array(), check.array()))
        throw new IOException("Hotbar recovery read-back failed");
      if (!existed)
        try (FileChannel directory = FileChannel.open(path.getParent(), StandardOpenOption.READ)) {
          directory.force(true);
        }
      verifiedSize = channel.size();
      verifiedModified = Files.getLastModifiedTime(path).toMillis();
    }
  }

  private static void validate(FileChannel channel, long size) throws IOException {
    channel.position(0);
    while (channel.position() < size) {
      ByteBuffer header = ByteBuffer.allocate(HEADER);
      read(channel, header);
      header.flip();
      if (header.getInt() != MAGIC || header.getInt() != VERSION)
        throw new IOException("Invalid hotbar recovery header");
      int length = header.getInt();
      long checksum = header.getLong();
      if (length <= 0 || length > MAX_RECORD || length > size - channel.position())
        throw new IOException("Incomplete hotbar recovery record");
      ByteBuffer payload = ByteBuffer.allocate(length);
      read(channel, payload);
      CRC32 crc = new CRC32();
      crc.update(payload.array());
      if (crc.getValue() != checksum) throw new IOException("Damaged hotbar recovery record");
    }
  }

  private static void read(FileChannel channel, ByteBuffer target) throws IOException {
    while (target.hasRemaining())
      if (channel.read(target) < 0) throw new IOException("Incomplete hotbar recovery record");
  }
}
