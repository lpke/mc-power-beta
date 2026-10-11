package local.luke.power.death;

import com.google.gson.Gson;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

/** Durable, append-only deaths and recovery intentions. Damaged tails are preserved. */
public final class DeathJournal {
  private static final int MAGIC = 0x50424448, VERSION = 2, HEADER = 20;
  public static final int MAX_RECORD = 1024 * 1024;
  private static final long MAX_FILE = 64L * 1024 * 1024;
  private static final Gson JSON = new Gson();
  private final Path path;
  private final List<DeathRecord> records = new ArrayList<>();
  private final Map<String, String> states = new HashMap<>();
  private final Map<String, Set<Integer>> claimed = new HashMap<>();
  private final Map<String, List<Integer>> pending = new HashMap<>();
  private boolean damaged;
  private long size, next = 1;

  static final class Event {
    String kind, id, before, after;
    DeathRecord death;
    List<Integer> indices;
  }

  public DeathJournal(Path path) throws IOException {
    this.path = path;
    safePath();
    resetPreviousFormat();
    if (!Files.exists(path)) return;
    size = Files.size(path);
    if (size > MAX_FILE) throw new IOException("Death journal exceeds 64 MiB");
    try (DataInputStream input = new DataInputStream(Files.newInputStream(path))) {
      long remaining = size;
      while (remaining > 0) {
        try {
          if (remaining < HEADER || input.readInt() != MAGIC || input.readInt() != VERSION)
            throw new IOException("Invalid death journal header");
          int length = input.readInt(); long checksum = input.readLong();
          if (length <= 0 || length > MAX_RECORD || length > remaining - HEADER)
            throw new IOException("Incomplete death journal record");
          byte[] bytes = input.readNBytes(length);
          CRC32 crc = new CRC32(); crc.update(bytes);
          if (bytes.length != length || crc.getValue() != checksum) throw new IOException("Death journal checksum failed");
          apply(JSON.fromJson(new String(bytes, StandardCharsets.UTF_8), Event.class));
          remaining -= HEADER + length;
        } catch (IOException | RuntimeException e) { damaged = true; break; }
      }
    }
  }

  public List<DeathRecord> records() { return List.copyOf(records); }
  public String nextId() { return Long.toString(next); }
  public String state(String id) { return states.getOrDefault(id, "available"); }
  public boolean damaged() { return damaged; }
  public List<Integer> remaining(String id) {
    DeathRecord record = records.stream().filter(r -> r.id.equals(id)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown death ID"));
    Set<Integer> done = claimed.getOrDefault(id, Set.of());
    return java.util.stream.IntStream.range(0, record.items.size()).filter(i -> !done.contains(i)).boxed().toList();
  }
  public void death(DeathRecord death) throws IOException {
    death.validate();
    if (!death.id.equals(nextId())) throw new IOException("Invalid death ID sequence");
    Event event = new Event(); event.kind = "death"; event.death = death; append(event);
  }
  public void recovery(String id, String before, String after) throws IOException {
    recovery(id, remaining(id), before, after);
  }
  public void recovery(String id, List<Integer> indices, String before, String after) throws IOException {
    if (records.stream().noneMatch(r -> r.id.equals(id))) throw new IOException("Unknown death ID");
    Event event = new Event(); event.kind = "pending"; event.id = id; event.before = before; event.after = after;
    event.indices = List.copyOf(indices);
    try { intent(event); } catch (IllegalArgumentException e) { throw new IOException(e.getMessage(), e); }
    append(event);
  }
  public void finish(String id, boolean recovered) throws IOException {
    if (!state(id).equals("pending")) throw new IOException("No pending recovery");
    Event event = new Event(); event.kind = recovered ? "recovered" : "available"; event.id = id; append(event);
  }

  private void apply(Event event) {
    if (event == null || event.kind == null) throw new IllegalArgumentException("Invalid death journal event");
    if (event.kind.equals("death")) {
      if (event.death == null) throw new IllegalArgumentException("Missing death");
      event.death.validate();
      long id = Long.parseLong(event.death.id);
      if (id != next || id == Long.MAX_VALUE) throw new IllegalArgumentException("Invalid death ID sequence");
      records.add(event.death); next = id + 1;
      if (event.death.kept) {
        claimed.put(event.death.id, new HashSet<>(remaining(event.death.id)));
        states.put(event.death.id, "kept");
      }
    } else {
      if (event.id == null || records.stream().noneMatch(r -> r.id.equals(event.id)))
        throw new IllegalArgumentException("Unknown recovery ID");
      if (event.kind.equals("pending")) {
        pending.put(event.id, intent(event)); states.put(event.id, "pending");
      } else {
        if (!Set.of("recovered", "available").contains(event.kind) || !state(event.id).equals("pending"))
          throw new IllegalArgumentException("Invalid recovery completion");
        if (event.kind.equals("recovered")) claimed.computeIfAbsent(event.id, k -> new HashSet<>()).addAll(pending.get(event.id));
        pending.remove(event.id);
        states.put(event.id, remaining(event.id).isEmpty() && event.kind.equals("recovered") ? "recovered"
            : claimed.getOrDefault(event.id, Set.of()).isEmpty() ? "available" : "partial");
      }
    }
  }

  private List<Integer> intent(Event event) {
    if (!Set.of("available", "partial").contains(state(event.id)) || event.before == null || event.after == null)
      throw new IllegalArgumentException("This death has already been recovered or has an interrupted recovery");
    List<Integer> remaining = remaining(event.id);
    List<Integer> indices = event.indices == null ? remaining : event.indices; // Version 1 whole-inventory claims.
    if (indices.stream().anyMatch(Objects::isNull) || new HashSet<>(indices).size() != indices.size()
        || !remaining.containsAll(indices) || indices.isEmpty() && !remaining.isEmpty())
      throw new IllegalArgumentException("Invalid or already recovered item selection");
    return List.copyOf(indices);
  }

  private void safePath() throws IOException {
    for (Path part = path; part != null; part = part.getParent())
      if (Files.isSymbolicLink(part)) throw new IOException("Unsafe death journal path");
  }
  private void resetPreviousFormat() throws IOException {
    if (!Files.exists(path) || Files.size(path) < 8) return;
    try (DataInputStream input = new DataInputStream(Files.newInputStream(path))) {
      if (input.readInt() != MAGIC || input.readInt() != 1) return;
    }
    // 1.4.2 deliberately starts fresh decimal IDs. Never decode or migrate old records.
    Files.delete(path);
    try (FileChannel directory = FileChannel.open(path.getParent(), StandardOpenOption.READ)) { directory.force(true); }
  }
  private synchronized void append(Event event) throws IOException {
    if (damaged) throw new IOException("Death journal needs repair; original records preserved");
    safePath();
    byte[] bytes = JSON.toJson(event).getBytes(StandardCharsets.UTF_8);
    if (bytes.length > MAX_RECORD || size > MAX_FILE - HEADER - bytes.length)
      throw new IOException("Death journal is full; archive it before adding records");
    Files.createDirectories(path.getParent());
    boolean existed = Files.exists(path);
    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
         var lock = channel.tryLock()) {
      if (lock == null || channel.size() != size) throw new IOException("Death journal changed or is in use");
      CRC32 crc = new CRC32(); crc.update(bytes);
      ByteBuffer frame = ByteBuffer.allocate(HEADER + bytes.length);
      frame.putInt(MAGIC).putInt(VERSION).putInt(bytes.length).putLong(crc.getValue()).put(bytes).flip();
      channel.position(size);
      while (frame.hasRemaining()) channel.write(frame);
      channel.force(true);
      channel.position(size);
      ByteBuffer check = ByteBuffer.allocate(frame.capacity());
      while (check.hasRemaining()) if (channel.read(check) < 0) throw new IOException("Death journal read-back failed");
      if (!Arrays.equals(frame.array(), check.array())) throw new IOException("Death journal read-back differs");
      if (!existed) try (FileChannel directory = FileChannel.open(path.getParent(), StandardOpenOption.READ)) { directory.force(true); }
      apply(event); size = channel.size();
    } catch (IOException | RuntimeException e) { damaged = true; throw new IOException("Could not protect death records", e); }
  }
}
