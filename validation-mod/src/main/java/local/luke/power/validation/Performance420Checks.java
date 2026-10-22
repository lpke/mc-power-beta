package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import local.luke.power.input.PlayerMode;
import local.luke.power.map.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import org.lwjgl.opengl.*;

/** Opt-in native-frame observer. No disk writes or blocking GPU reads during a sample. */
public final class Performance420Checks {
  private static final int LIMIT = 16384;
  private static Task task;
  private Performance420Checks() {}

  public static void run(Minecraft mc, String command) throws Exception {
    String[] args = command.trim().split("\\s+");
    if (args[0].equals("status")) { log(task == null ? "PERFORMANCE 420 idle" : "PERFORMANCE 420 " + task.label + " frames=" + task.frames); return; }
    if (args[0].equals("stop")) { finish("stopped"); return; }
    check(task == null, "performance observer already running");
    check(args.length >= 2 && Set.of("start", "turn", "travel").contains(args[0]), "use performance-420 start|turn|travel LABEL [SECONDS], status or stop");
    check(args[1].matches("[a-zA-Z0-9_-]{1,60}"), "invalid report label");
    check(mc.world != null && !mc.world.isRemote && mc.player != null && mc.currentScreen == null, "open a disposable singleplayer world");
    if (args[0].equals("travel")) check(mc.player instanceof PlayerMode mode && (mode.power$spectator() || mode.power$flying()), "travel requires an already flying or Spectator player");
    int seconds = args.length > 2 ? Integer.parseInt(args[2]) : 10;
    check(seconds >= 2 && seconds <= 60, "sample duration must be 2–60 seconds");
    task = new Task(mc, args[0], args[1], seconds);
    log("PERFORMANCE 420 START " + task.label + " renderer=" + GL11.glGetString(GL11.GL_RENDERER));
  }

  public static long start() { return task == null ? 0 : System.nanoTime(); }
  public static void measured(String kind, long started) {
    Task current = task;
    if (started == 0 || current == null || Thread.currentThread() != current.thread) return;
    double elapsed = (System.nanoTime() - started) / 1e6;
    current.metrics.computeIfAbsent(kind, ignored -> new Metric()).add(elapsed);
    if (kind.equals("terrainBuild")) { current.frameBuilds++; current.frameBuildMs += elapsed; }
    if (kind.equals("chunkGenerate")) { current.frameGenerations++; current.frameGenerationMs += elapsed; }
  }

  public static void beforeFrame(Minecraft mc) {
    Task current = task;
    if (current == null || mc.world != current.world) return;
    current.poll();
    long now = System.nanoTime();
    if (current.lastFrame != 0) current.intervals.add((now - current.lastFrame) / 1e6);
    current.lastFrame = now;
    if (current.gpu && !current.available.isEmpty() && GL15.glGetQueryi(GL33.GL_TIME_ELAPSED, GL15.GL_CURRENT_QUERY) == 0) {
      current.activeQuery = current.available.remove();
      GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, current.activeQuery);
    }
    current.frameStart = System.nanoTime();
  }

  public static void afterFrame(Minecraft mc) {
    Task current = task;
    if (current == null || current.frameStart == 0) return;
    double elapsed = (System.nanoTime() - current.frameStart) / 1e6;
    current.frameCpu.add(elapsed); current.frames++;
    if (current.activeQuery != 0) {
      GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
      current.pending.add(current.activeQuery); current.activeQuery = 0;
    }
    if (mc.player != null) {
      var sample = new SlowFrame(elapsed, current.frameBuilds, current.frameBuildMs,
          current.frameGenerations, current.frameGenerationMs, mc.player.x, mc.player.z, mc.player.yaw);
      current.slowest.add(sample);
      if (current.slowest.size() > 24) current.slowest.remove();
    }
    current.frameBuilds = current.frameGenerations = 0;
    current.frameBuildMs = current.frameGenerationMs = 0;
    current.frameStart = 0; current.poll();
  }

  public static void tick(Minecraft mc) {
    Task current = task;
    if (current == null) return;
    try {
      if (mc.world != current.world || mc.player == null) { finish("world-changed"); return; }
      if (System.nanoTime() >= current.until) { finish("complete"); return; }
      if (mc.currentScreen != null || !Display.isActive()) { finish("interrupted"); return; }
      current.ticks++;
      if (current.action.equals("turn")) mc.player.yaw = current.yaw + current.ticks * 2;
      if (current.action.equals("travel")) {
        mc.player.method_1340(current.x + current.ticks * .5, current.y, current.z);
        mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
      }
    } catch (Exception error) { throw new IllegalStateException("Performance observer failed", error); }
  }

  private static void finish(String status) throws Exception {
    Task done = task;
    if (done == null) return;
    task = null;
    done.poll();
    if (done.activeQuery != 0) { GL15.glEndQuery(GL33.GL_TIME_ELAPSED); GL15.glDeleteQueries(done.activeQuery); }
    for (int query : done.pending) GL15.glDeleteQueries(query);
    for (int query : done.available) GL15.glDeleteQueries(query);
    if (done.mc.world == done.world && done.mc.player != null) {
      if (done.action.equals("travel")) {
        done.mc.player.method_1340(done.x, done.y, done.z);
        done.mc.player.prevX = done.x; done.mc.player.prevY = done.y; done.mc.player.prevZ = done.z;
        done.mc.player.velocityX = done.vx; done.mc.player.velocityY = done.vy; done.mc.player.velocityZ = done.vz;
      }
      if (done.action.equals("turn")) done.mc.player.prevYaw = done.mc.player.yaw = done.yaw;
    }
    var report = new LinkedHashMap<String, Object>();
    report.put("label", done.label); report.put("action", done.action); report.put("status", status);
    report.put("renderer", GL11.glGetString(GL11.GL_RENDERER)); report.put("vendor", GL11.glGetString(GL11.GL_VENDOR));
    report.put("gpuTimers", done.gpu); report.put("frames", done.frames); report.put("ticks", done.ticks);
    report.put("seconds", (System.nanoTime() - done.started) / 1e9);
    report.put("renderDistanceChunks", Class.forName("local.luke.power.controls.util.ModOptions")
        .getMethod("getRenderDistanceChunks").invoke(null));
    report.put("window", List.of(done.mc.displayWidth, done.mc.displayHeight));
    report.put("mapSettings", MapConfig.copy());
    report.put("frameCpuMs", done.frameCpu.report()); report.put("frameGpuMs", done.frameGpu.report());
    report.put("frameIntervalMs", done.intervals.report());
    var metrics = new LinkedHashMap<String, Object>();
    done.metrics.forEach((name, metric) -> metrics.put(name, metric.report())); report.put("work", metrics);
    var slow = new ArrayList<>(done.slowest); slow.sort(Comparator.comparingDouble(SlowFrame::cpuMs).reversed());
    report.put("slowestFrames", slow);
    Path file = Path.of("power-beta-data/reports/performance-420", done.label + "-" + System.currentTimeMillis() + ".json");
    Files.createDirectories(file.getParent());
    Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n", StandardOpenOption.CREATE_NEW);
    log("PERFORMANCE 420 " + status.toUpperCase(Locale.ROOT) + " report=" + file);
  }

  private record SlowFrame(double cpuMs, int terrainBuilds, double terrainBuildMs,
      int generatedChunks, double generationMs, double x, double z, float yaw) {}

  private static final class Metric {
    final ArrayList<Double> samples = new ArrayList<>();
    long calls;
    double total, maximum;
    void add(double value) { calls++; total += value; maximum = Math.max(maximum, value); if (samples.size() < LIMIT) samples.add(value); }
    Map<String, Object> report() {
      var result = new LinkedHashMap<>(HeldLight400Frames.percentiles(samples));
      result.put("calls", calls); result.put("total", total); result.put("max", maximum);
      result.put("truncated", calls > samples.size()); return result;
    }
  }

  private static final class Task {
    final Minecraft mc;
    final World world;
    final Thread thread = Thread.currentThread();
    final String action, label;
    final long started = System.nanoTime(), until;
    final double x, y, z, vx, vy, vz;
    final float yaw;
    final boolean gpu = GLContext.getCapabilities().OpenGL33;
    final ArrayDeque<Integer> available = new ArrayDeque<>(), pending = new ArrayDeque<>();
    final Metric frameCpu = new Metric(), frameGpu = new Metric(), intervals = new Metric();
    final Map<String, Metric> metrics = new LinkedHashMap<>();
    final PriorityQueue<SlowFrame> slowest = new PriorityQueue<>(Comparator.comparingDouble(SlowFrame::cpuMs));
    long frameStart, lastFrame, frames;
    int activeQuery, ticks, frameBuilds, frameGenerations;
    double frameBuildMs, frameGenerationMs;
    Task(Minecraft mc, String action, String label, int seconds) {
      this.mc = mc; world = mc.world; this.action = action; this.label = label;
      until = started + seconds * 1_000_000_000L;
      x = mc.player.x; y = mc.player.y; z = mc.player.z; yaw = mc.player.yaw;
      vx = mc.player.velocityX; vy = mc.player.velocityY; vz = mc.player.velocityZ;
      if (gpu) for (int i = 0; i < 8; i++) available.add(GL15.glGenQueries());
    }
    void poll() {
      while (!pending.isEmpty() && GL15.glGetQueryObjecti(pending.peek(), GL15.GL_QUERY_RESULT_AVAILABLE) != 0) {
        int query = pending.remove(); frameGpu.add(GL33.glGetQueryObjectui64(query, GL15.GL_QUERY_RESULT) / 1e6); available.add(query);
      }
    }
  }
}
