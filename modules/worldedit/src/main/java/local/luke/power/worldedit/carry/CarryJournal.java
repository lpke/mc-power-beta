package local.luke.power.worldedit.carry;

import com.google.gson.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import local.luke.power.worldedit.core.*;

/** Write-ahead records retain the original inventory even after successful placement. */
public final class CarryJournal {
  // Beta ships Gson 2.8.9, which predates Java record deserialization.
  private static final Gson JSON =
      new GsonBuilder()
          .registerTypeAdapter(
              Pos.class,
              (JsonDeserializer<Pos>)
                  (json, type, context) -> {
                    JsonObject value = json.getAsJsonObject();
                    return new Pos(
                        value.get("x").getAsInt(),
                        value.get("y").getAsInt(),
                        value.get("z").getAsInt());
                  })
          .create();
  public String phase = "removing", id = UUID.randomUUID().toString();

  public static final class Part {
    public Pos source, target;
    public int metadata;
    public String payload;

    private Part(Pos source, BlockValue value) {
      this.source = source;
      metadata = value.meta;
      payload = Base64.getEncoder().encodeToString(Objects.requireNonNull(value.nbt()));
    }
  }

  public Part second;
  public Pos source, target;
  public int sourceDimension, targetDimension;
  public int block, metadata;
  public String payload, checksum, recordChecksum;

  public CarryJournal(Pos source, int dimension, BlockValue value) {
    this.source = source;
    sourceDimension = dimension;
    block = value.id;
    metadata = value.meta;
    payload = Base64.getEncoder().encodeToString(Objects.requireNonNull(value.nbt()));
    checksum = contentHash();
  }

  public CarryJournal(
      Pos source, int dimension, BlockValue value, Pos other, BlockValue otherValue) {
    this(source, dimension, value);
    second = new Part(other, otherValue);
    if (otherValue.id != 54) throw new IllegalArgumentException("Second block must be a chest");
    checksum = contentHash();
    value();
  }

  public int size() {
    return second == null ? 1 : 2;
  }

  public Pos source(int index) {
    return index == 0 ? source : second.source;
  }

  public Pos target(int index) {
    return index == 0 ? target : second.target;
  }

  public BlockValue value(int index) {
    BlockValue first = value();
    return index == 0
        ? first
        : new BlockValue(54, second.metadata, Base64.getDecoder().decode(second.payload));
  }

  public void targets(Pos first, Pos other, int dimension) {
    target = first;
    targetDimension = dimension;
    if (second != null) second.target = other;
  }

  private static boolean adjacent(Pos a, Pos b) {
    return a != null
        && b != null
        && a.valid()
        && b.valid()
        && a.y() == b.y()
        && Math.abs((long) a.x() - b.x()) + Math.abs((long) a.z() - b.z()) == 1;
  }

  public BlockValue value() {
    if (id == null
        || !UUID.fromString(id).toString().equals(id)
        || payload == null
        || payload.length() > 90000
        || !Set.of(23, 54, 61, 62).contains(block)
        || metadata < 0
        || metadata > 15
        || source == null
        || !source.valid()
        || !Set.of("removing", "held", "placing", "complete").contains(phase)
        || !Objects.equals(contentHash(), checksum)
        || phase.equals("placing") && (target == null || !target.valid()))
      throw new IllegalArgumentException("Invalid container recovery record");
    if (second != null
        && (block != 54
            || !adjacent(source, second.source)
            || second.metadata < 0
            || second.metadata > 15
            || second.payload == null
            || second.payload.length() > 90000
            || Base64.getDecoder().decode(second.payload).length > 65536
            || phase.equals("placing") && !adjacent(target, second.target)))
      throw new IllegalArgumentException("Invalid double chest recovery record");
    byte[] bytes = Base64.getDecoder().decode(payload);
    if (bytes.length > 65536) throw new IllegalArgumentException("Container data exceeds 64 KiB");
    return new BlockValue(block, metadata, bytes);
  }

  private String contentHash() {
    return hash(
        id
            + ":"
            + source
            + ":"
            + sourceDimension
            + ":"
            + block
            + ":"
            + metadata
            + ":"
            + payload
            + (second == null
                ? ""
                : ":pair:" + second.source + ":" + second.metadata + ":" + second.payload));
  }

  private String recordHash() {
    return hash(
        checksum
            + ":"
            + phase
            + ":"
            + target
            + ":"
            + targetDimension
            + (second == null ? "" : ":pair:" + second.target));
  }

  private static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  public static CarryJournal read(Path file) throws IOException {
    if (Files.isSymbolicLink(file) || Files.size(file) > 262144)
      throw new IOException("Unsafe container recovery file");
    try {
      CarryJournal record = JSON.fromJson(Files.readString(file), CarryJournal.class);
      record.value();
      if (!Objects.equals(record.recordChecksum, record.recordHash()))
        throw new IllegalArgumentException("Recovery phase checksum failed");
      return record;
    } catch (RuntimeException e) {
      throw new IOException("Container recovery data is invalid; file preserved", e);
    }
  }

  public void write(Path file) throws IOException {
    value();
    recordChecksum = recordHash();
    if (Files.isSymbolicLink(file) || Files.isSymbolicLink(file.getParent()))
      throw new IOException("Unsafe container recovery path");
    Files.createDirectories(file.getParent());
    byte[] bytes = JSON.toJson(this).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    Path temporary = Files.createTempFile(file.getParent(), "carry-", ".tmp");
    try {
      try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        while (buffer.hasRemaining()) channel.write(buffer);
        channel.force(true);
      }
      Files.move(
          temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      try (FileChannel directory = FileChannel.open(file.getParent(), StandardOpenOption.READ)) {
        directory.force(true);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }
}
