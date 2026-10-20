package local.luke.power.map;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Independent world data. Drafts never mutate the store; a revision survives saves of older
 * snapshots.
 */
public final class WaypointStore {
  public static final int MAX_POINTS = 8192;
  private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();

  public static final class Data {
    public int format = 1;
    public String selectedSet = "Default";
    public boolean allSets;
    public List<Waypoint> waypoints = new ArrayList<>();
    public List<String> sets = new ArrayList<>();

    public void validate() {
      if (format != 1)
        throw new IllegalArgumentException(
            "Unsupported waypoint format " + format + "; original retained");
      Waypoint.text(selectedSet, 48);
      if (sets == null || sets.size() > MAX_POINTS)
        throw new IllegalArgumentException("Too many waypoint sets");
      Set<String> names = new HashSet<>();
      for (String set : sets) {
        Waypoint.text(set, 48);
        if (!names.add(set.toLowerCase(Locale.ROOT)))
          throw new IllegalArgumentException("Duplicate waypoint set");
      }
      if (waypoints == null || waypoints.size() > MAX_POINTS)
        throw new IllegalArgumentException("Too many waypoints");
      Set<String> ids = new HashSet<>();
      for (Waypoint point : waypoints) {
        if (point == null) throw new IllegalArgumentException("Invalid waypoint");
        point.validate();
        if (!ids.add(point.id)) throw new IllegalArgumentException("Duplicate waypoint ID");
      }
    }
  }

  private final Path file;
  private final Consumer<String> errors;
  private CompletableFuture<Loaded> loading;
  private CompletableFuture<byte[]> saving;

  private record Loaded(Data data, byte[] digest) {}

  private Data data = new Data();
  private byte[] digest;
  private long revision, saved, savingRevision;
  private int retryDelay;
  private boolean ready, blocked;

  public WaypointStore(Path root, Consumer<String> errors) {
    file = root.resolve("waypoints.json");
    this.errors = errors;
  }

  public boolean ready() {
    return ready && !blocked;
  }

  public boolean dirty() {
    return saved != revision;
  }

  public long revision() {
    return revision;
  }

  public boolean needsMaintenance() {
    return !blocked && (!ready && loading != null || saving != null || dirty());
  }

  public boolean contains(String id) {
    return data.waypoints.stream().anyMatch(p -> p.id.equals(id));
  }

  public List<Waypoint> points() {
    return data.waypoints.stream().map(Waypoint::copy).toList();
  }

  public String selectedSet() {
    return data.selectedSet;
  }

  public boolean allSets() {
    return data.allSets;
  }

  public List<String> sets() {
    var sets = new TreeSet<String>();
    sets.add("Default");
    sets.add(data.selectedSet);
    sets.addAll(data.sets);
    data.waypoints.forEach(p -> sets.add(p.set));
    return List.copyOf(sets);
  }

  /** Empty named sets remain available after selection changes, deletion and restart. */
  public void createSet(String name) {
    requireReady();
    Waypoint.text(name, 48);
    if (sets().stream().anyMatch(set -> set.equalsIgnoreCase(name)))
      throw new IllegalArgumentException("A set with that name already exists");
    if (data.sets.size() >= MAX_POINTS)
      throw new IllegalArgumentException("Waypoint set limit reached");
    data.sets.add(name);
    data.selectedSet = name;
    data.allSets = false;
    revision++;
  }

  /** Deleting a named set moves its members to Default without changing point data. */
  public void deleteSet(String name) {
    requireReady();
    Waypoint.text(name, 48);
    if (name.equalsIgnoreCase("Default"))
      throw new IllegalArgumentException("The Default set cannot be deleted");
    if (!sets().contains(name)) throw new IllegalArgumentException("Waypoint set not found");
    data.waypoints.replaceAll(point -> {
      if (!point.set.equals(name)) return point;
      Waypoint moved = point.copy(); moved.set = "Default"; return moved;
    });
    data.sets.removeIf(name::equals);
    if (data.selectedSet.equals(name)) data.selectedSet = "Default";
    revision++;
  }

  public void select(String set) {
    requireReady();
    Waypoint.text(set, 48);
    data.selectedSet = set;
    revision++;
  }

  public void allSets(boolean value) {
    requireReady();
    data.allSets = value;
    revision++;
  }

  /** Editor validation runs before accepting any linked shared preferences. */
  public void validatePut(Waypoint point) {
    requireReady(); point.validate();
    Waypoint previous = data.waypoints.stream().filter(p -> p.id.equals(point.id)).findFirst().orElse(null);
    if (previous == null && data.waypoints.size() >= MAX_POINTS) throw new IllegalArgumentException("Waypoint limit reached");
    if (data.sets.size() + missingSets(previous == null ? "Default" : previous.set, point.set).size() > MAX_POINTS)
      throw new IllegalArgumentException("Waypoint set limit reached");
  }

  public void put(Waypoint point) {
    validatePut(point);
    int index = -1;
    for (int i = 0; i < data.waypoints.size(); i++)
      if (data.waypoints.get(i).id.equals(point.id)) {
        index = i;
        break;
      }
    rememberSets(index < 0 ? "Default" : data.waypoints.get(index).set, point.set);
    if (index < 0) data.waypoints.add(point.copy());
    else data.waypoints.set(index, point.copy());
    revision++;
  }

  private List<String> missingSets(String... names) {
    List<String> added = new ArrayList<>();
    for (String name : names) if (!name.equals("Default")
        && data.sets.stream().noneMatch(set -> set.equalsIgnoreCase(name))
        && added.stream().noneMatch(set -> set.equalsIgnoreCase(name))) added.add(name);
    return added;
  }

  private void rememberSets(String... names) {
    List<String> added = missingSets(names);
    if (data.sets.size() + added.size() > MAX_POINTS) throw new IllegalArgumentException("Waypoint set limit reached");
    data.sets.addAll(added);
  }

  /** Moving retains both named sets while preserving the point identity. */
  public void move(String id, String set) {
    requireReady();
    String target = sets().stream().filter(s -> s.equals(set)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Waypoint set not found"));
    Waypoint point = data.waypoints.stream().filter(p -> p.id.equals(id)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Waypoint not found"));
    if (point.set.equals(target)) return;
    rememberSets(point.set, target);
    Waypoint moved = point.copy(); moved.set = target; put(moved);
  }

  public void remove(String id) {
    requireReady();
    Waypoint point = data.waypoints.stream().filter(p -> p.id.equals(id)).findFirst().orElse(null);
    if (point != null) { rememberSets(point.set); data.waypoints.removeIf(p -> p.id.equals(id)); revision++; }
  }

  public void death(int x, int y, int z, int dimension, boolean keepOld) {
    death(x, y, z, dimension, keepOld, System.currentTimeMillis());
  }

  public void death(int x, int y, int z, int dimension, boolean keepOld, long time) {
    death(x, y, z, dimension, keepOld, time, "");
  }
  public void death(int x, int y, int z, int dimension, boolean keepOld, long time, String cause) {
    requireReady();
    if (!keepOld) data.waypoints.removeIf(p -> p.death);
    Waypoint point = new Waypoint();
    point.name = "Death " + new java.text.SimpleDateFormat("MM-dd HH:mm").format(new Date(time));
    point.created = time;
    point.x = x;
    point.y = Math.max(0, Math.min(127, y));
    point.z = z;
    point.dimension = dimension;
    point.death = true;
    point.deathCause = cause;
    point.colour = 0xff5555;
    point.set = data.selectedSet;
    point.global = true;
    put(point);
  }

  private void requireReady() {
    if (!ready())
      throw new IllegalStateException("Waypoints are loading or protected after a storage error");
  }

  public void tick() { tick(true); }

  /** Disabled displays may finish existing edits without starting a fresh store read. */
  public void tick(boolean allowLoad) {
    if (blocked) return;
    if (!ready) {
      if (loading == null && !allowLoad) return;
      if (loading == null)
        loading =
            MapIO.submit(
                () -> {
                  if (!Files.exists(file)) return new Loaded(new Data(), null);
                  if (Files.size(file) > 8 * 1024 * 1024)
                    throw new IOException("Waypoint file too large");
                  byte[] bytes = Files.readAllBytes(file);
                  return new Loaded(decode(bytes), hash(bytes));
                });
      if (loading == null || !loading.isDone()) return;
      try {
        Loaded loaded = loading.join();
        data = loaded.data;
        digest = loaded.digest;
        ready = true;
      } catch (RuntimeException e) {
        blocked = true;
        errors.accept("Waypoints retained without changes: " + MapCache.reason(e));
      }
      return;
    }
    if (saving != null && saving.isDone()) {
      try {
        digest = saving.join();
        saved = savingRevision;
      } catch (RuntimeException e) {
        errors.accept("Waypoints could not be saved: " + MapCache.reason(e));
        retryDelay = 200;
      }
      saving = null;
    }
    if (retryDelay > 0) {
      retryDelay--;
      return;
    }
    if (saving == null && dirty()) {
      byte[] bytes = encode(data), expected = digest == null ? null : digest.clone();
      saving =
          MapIO.submit(
              () -> {
                byte[] original = Files.exists(file) ? Files.readAllBytes(file) : null;
                if (!Arrays.equals(expected, original == null ? null : hash(original)))
                  throw new IOException(
                      "Waypoint file changed outside this client; original retained");
                if (original != null) {
                  decode(original);
                  MapFiles.replace(file.resolveSibling("waypoints.previous.json"), original);
                }
                MapFiles.replace(file, bytes);
                return hash(bytes);
              });
      if (saving != null) savingRevision = revision;
    }
  }

  public void finish() {
    retryDelay = 0;
    for (int i = 0; i < 8; i++) {
      tick();
      if (loading != null && !loading.isDone())
        try {
          loading.join();
        } catch (RuntimeException ignored) {
        }
      if (saving != null)
        try {
          saving.join();
        } catch (RuntimeException ignored) {
        }
      else if (blocked || ready && !dirty() || retryDelay > 0) break;
      else MapIO.awaitQueued();
    }
    tick();
  }

  public static byte[] encode(Data data) {
    data.validate();
    return (JSON.toJson(data) + "\n").getBytes(StandardCharsets.UTF_8);
  }

  public static Data decode(byte[] bytes) throws IOException {
    try {
      JsonObject object =
          JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
      if (!object.has("format") || !object.has("waypoints"))
        throw new IllegalArgumentException("Missing waypoint format or list");
      for (JsonElement entry : object.getAsJsonArray("waypoints")) {
        if (entry.isJsonObject() && entry.getAsJsonObject().has("abbreviation")) {
          JsonElement abbreviation = entry.getAsJsonObject().get("abbreviation");
          if (!abbreviation.isJsonNull() && (!abbreviation.isJsonPrimitive() || !abbreviation.getAsJsonPrimitive().isString()))
            throw new IllegalArgumentException("Invalid waypoint abbreviation");
        }
      }
      Data data = JSON.fromJson(object, Data.class);
      data.validate();
      return data;
    } catch (RuntimeException e) {
      throw new IOException("Invalid waypoint data: " + e.getMessage(), e);
    }
  }

  public static byte[] hash(byte[] bytes) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }
}
