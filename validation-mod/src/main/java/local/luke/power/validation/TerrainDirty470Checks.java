package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.Catalog;
import local.luke.power.render.*;
import net.minecraft.class_66;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;

/** Main-thread invalidation diagnostic. Counts and stack samples are not a frame benchmark. */
public final class TerrainDirty470Checks {
  private static final int LIMIT = 8192;
  private static Task task;
  private TerrainDirty470Checks() {}

  static void run(Minecraft mc, String arguments) throws Exception {
    String[] args = arguments.trim().split("\\s+");
    if (args[0].equals("stop")) { if (task != null) finish("stopped"); return; }
    if (args[0].equals("status")) {
      log(task == null ? "TERRAIN DIRTY 470 inactive" : "TERRAIN DIRTY 470 frames=" + task.frames + " ticks=" + task.ticks);
      return;
    }
    check(args[0].equals("start") && args.length >= 2, "use terrain-dirty-470 start LABEL [10..30 seconds]");
    check(task == null, "terrain dirty observer already active");
    check(args[1].matches("[a-zA-Z0-9_-]{1,60}"), "invalid terrain diagnostic label");
    check(mc.world != null && !mc.world.isRemote && mc.player != null && mc.currentScreen == null && Display.isActive(),
        "focus a disposable singleplayer world");
    int seconds = args.length > 2 ? Integer.parseInt(args[2]) : 16;
    check(seconds >= 10 && seconds <= 30, "terrain diagnostic lasts 10..30 seconds");
    Task next = new Task(mc, args[1], seconds);
    next.samples.add(next.snapshot());
    Files.createDirectories(next.report.getParent());
    Files.writeString(next.report, Catalog.JSON.toJson(Map.of("label", next.label, "status", "running", "valid", false)));
    task = next;
    log("TERRAIN DIRTY 470 START " + next.label + " seconds=" + seconds + " report=" + next.report);
  }

  public static void tick(Minecraft mc) {
    Task current = task;
    if (current == null || Thread.currentThread() != current.thread) return;
    try {
      current.ticks++;
      if (mc != current.mc || mc.world != current.world) { finish("world-changed"); return; }
      long now = System.nanoTime();
      if (now >= current.sampleAt && current.samples.size() < 32) {
        current.samples.add(current.snapshot()); current.sampleAt = now + 1_000_000_000L;
      }
      if (now >= current.until) finish("complete");
    } catch (Exception failure) {
      task = null;
      throw new IllegalStateException("Terrain diagnostic failed; running report is not valid", failure);
    }
  }

  public static void frame() {
    Task current = task;
    if (current != null && Thread.currentThread() == current.thread) current.frames++;
  }

  public static void mark(class_66 section) {
    Task current = task;
    if (current == null || Thread.currentThread() != current.thread) return;
    boolean clean = !section.field_249;
    current.marks++; if (clean) current.cleanMarks++;
    current.count(section, true, clean);
    if (current.stacks.size() < 24 && (current.marks <= 4 || current.marks % 128 == 0)) {
      List<String> stack = StackWalker.getInstance().walk(frames -> frames.skip(1).limit(16).map(Object::toString).toList());
      if (current.stackKeys.add(stack)) current.stacks.add(Map.of("mark", current.marks, "wasDirty", !clean,
          "coordinate", List.of(section.field_231, section.field_232, section.field_233), "stack", stack));
    }
  }

  public static void mesh(class_66 section) {
    Task current = task;
    if (current == null || Thread.currentThread() != current.thread) return;
    if (section.field_249) { current.builds++; current.count(section, false, false); }
    else current.noops++;
  }

  public static void refresh(String source) {
    Task current = task;
    if (current != null && Thread.currentThread() == current.thread) current.refreshes.merge(source, 1L, Long::sum);
  }

  private static void finish(String status) throws Exception {
    Task done = task;
    task = null;
    boolean valid = status.equals("complete") && done.frames > 0 && done.ticks > 0;
    done.samples.add(done.snapshot());
    var report = new LinkedHashMap<String, Object>();
    report.put("label", done.label); report.put("status", valid ? status : "invalid-" + status);
    report.put("valid", valid); report.put("frames", done.frames); report.put("ticks", done.ticks);
    report.put("seconds", (System.nanoTime() - done.started) / 1e9);
    report.put("marks", done.marks); report.put("cleanToDirtyMarks", done.cleanMarks);
    report.put("dirtyMeshEntries", done.builds); report.put("noOpMeshEntries", done.noops);
    report.put("sectionObjects", done.sections.size()); report.put("uniqueCoordinates", done.coordinates.size());
    report.put("truncated", done.truncated); report.put("refreshEntries", done.refreshes);
    report.put("waterPreparations", WaterTerrain.preparations() - done.preparations);
    report.put("waterSections", WaterTerrain.sections() - done.waterSections);
    report.put("waterPasses", WaterTerrain.secondPasses() - done.waterPasses);
    report.put("neutralPasses", WaterTerrain.neutralPasses() - done.neutralPasses);
    report.put("heldLightRequests", HeldLighting.requests() - done.heldRequests);
    report.put("samples", done.samples); report.put("sampledMarkStacks", done.stacks);
    report.put("sections", done.sections.values().stream().sorted(Comparator.comparingLong((Counts row) -> row.builds).reversed()).limit(64).toList());
    report.put("coordinates", done.coordinates.values().stream().sorted(Comparator.comparingLong((Counts row) -> row.builds).reversed()).limit(64).toList());
    Files.writeString(done.report, Catalog.JSON.toJson(report));
    log("TERRAIN DIRTY 470 " + (valid ? "END " : "INVALID ") + done.label + " frames=" + done.frames
        + " ticks=" + done.ticks + " marks=" + done.marks + " builds=" + done.builds + " report=" + done.report);
  }

  private record Coordinate(int x, int y, int z) {}
  private static final class Counts {
    final int slot;
    Coordinate coordinate;
    long marks, cleanMarks, builds;
    Counts(int slot, Coordinate coordinate) { this.slot = slot; this.coordinate = coordinate; }
    void add(boolean mark, boolean clean) { if (mark) { marks++; if (clean) cleanMarks++; } else builds++; }
  }

  private static final class Task {
    final Minecraft mc;
    final Object world;
    final Thread thread = Thread.currentThread();
    final String label;
    final Path report;
    final Field grid, queue;
    final long started = System.nanoTime(), until;
    final long preparations = WaterTerrain.preparations(), waterSections = WaterTerrain.sections();
    final long waterPasses = WaterTerrain.secondPasses(), neutralPasses = WaterTerrain.neutralPasses();
    final long heldRequests = HeldLighting.requests();
    final IdentityHashMap<class_66, Counts> sections = new IdentityHashMap<>();
    final Map<Coordinate, Counts> coordinates = new HashMap<>();
    final Map<String, Long> refreshes = new LinkedHashMap<>();
    final List<Map<String, Object>> samples = new ArrayList<>(), stacks = new ArrayList<>();
    final Set<List<String>> stackKeys = new HashSet<>();
    long frames, ticks, marks, cleanMarks, builds, noops, sampleAt;
    boolean truncated;
    Task(Minecraft mc, String label, int seconds) throws Exception {
      this.mc = mc; world = mc.world; this.label = label; until = started + seconds * 1_000_000_000L;
      sampleAt = started + 1_000_000_000L;
      report = Path.of("power-beta-data/reports/release-470/terrain-dirty-" + label + "-" + System.currentTimeMillis() + ".json");
      grid = field(mc.worldRenderer, "field_1809"); queue = field(mc.worldRenderer, "field_1807");
    }
    void count(class_66 section, boolean mark, boolean clean) {
      var point = new Coordinate(section.field_231, section.field_232, section.field_233);
      Counts slot = sections.get(section);
      if (slot == null && sections.size() < LIMIT) { slot = new Counts(sections.size(), point); sections.put(section, slot); }
      if (slot == null) truncated = true;
      else { slot.coordinate = point; slot.add(mark, clean); }
      Counts coordinate = coordinates.get(point);
      if (coordinate == null && coordinates.size() < LIMIT) { coordinate = new Counts(-1, point); coordinates.put(point, coordinate); }
      if (coordinate == null) truncated = true;
      else coordinate.add(mark, clean);
    }
    Map<String, Object> snapshot() throws Exception {
      class_66[] live = (class_66[])grid.get(mc.worldRenderer);
      List<?> queued = (List<?>)queue.get(mc.worldRenderer);
      Set<Object> unique = Collections.newSetFromMap(new IdentityHashMap<>());
      int nulls = 0, dirty = 0;
      for (Object value : queued) if (value == null) nulls++; else unique.add(value);
      for (class_66 section : live) if (section != null && section.field_249) dirty++;
      var sample = new LinkedHashMap<String, Object>();
      sample.put("seconds", (System.nanoTime() - started) / 1e9); sample.put("frames", frames); sample.put("ticks", ticks);
      sample.put("gridSections", live.length); sample.put("gridDimensions", List.of(
          field(mc.worldRenderer, "field_1810").getInt(mc.worldRenderer),
          field(mc.worldRenderer, "field_1811").getInt(mc.worldRenderer),
          field(mc.worldRenderer, "field_1812").getInt(mc.worldRenderer)));
      sample.put("dirtySections", dirty); sample.put("queueEntries", queued.size());
      sample.put("uniqueQueuedSections", unique.size()); sample.put("nullQueueEntries", nulls);
      sample.put("skylightSubtracted", mc.world.field_202); sample.put("simulationTime", mc.world.getTime());
      sample.put("marks", marks); sample.put("cleanToDirtyMarks", cleanMarks); sample.put("dirtyMeshEntries", builds);
      return sample;
    }
  }
  private static Field field(Object owner, String name) throws Exception {
    for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) try {
      Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    } catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
}
