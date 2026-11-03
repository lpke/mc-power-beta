package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;

/** Opt-in, main-thread timestamps. Timed callbacks allocate no rows and perform no I/O. */
public final class CursorLatency473Checks {
  public static final int MOUSE = 0, KEYBOARD = 1;
  private static final int LIMIT = 65536;
  private static Trial trial;
  private CursorLatency473Checks() {}

  public static void run(Minecraft mc, String command) throws Exception {
    String[] args = command.trim().split("\\s+");
    if (args[0].equals("finish")) { finish(); return; }
    check(trial == null, "finish the current cursor latency trial first");
    check(args.length >= 2 && args.length <= 4 && args[0].equals("arm"),
        "cursor-latency-473 arm LABEL [WARMUP_MS] [VERIFIED_X_CLOCK_OFFSET_MS], finish");
    check(args[1].matches("[a-zA-Z0-9_-]{1,80}"), "invalid cursor latency label");
    check(mc.world != null && mc.player != null && Display.isActive(), "focus a disposable world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable client");
    int warmup = args.length > 2 ? Integer.parseInt(args[2]) : 2000;
    check(warmup >= 0 && warmup <= 30000, "warmup must be 0..30000 ms");
    Long offset = args.length > 3 ? Long.parseLong(args[3]) : null;
    // Allocate and log before publishing the observer. The warmup also excludes command dispatch.
    Trial next = new Trial(args[1], warmup, offset, mc.displayWidth, mc.displayHeight);
    log("CURSOR LATENCY473 armed " + args[1] + " warmupMs=" + warmup + " nativeClockCalibrated=" + (offset != null));
    next.from = System.nanoTime() + warmup * 1_000_000L;
    trial = next;
  }

  public static boolean active() { return trial != null && Thread.currentThread() == trial.thread; }

  public static int beforeRelease(boolean requestedGrab, boolean previouslyGrabbed) {
    Trial t = trial;
    if (requestedGrab || t == null || Thread.currentThread() != t.thread) return 0;
    long now = System.nanoTime();
    if (now < t.from) return 0;
    if (t.releases == LIMIT) { t.droppedReleases++; return 0; }
    int row = t.releases++;
    t.release[0][row] = now; t.release[2][row] = previouslyGrabbed ? 1 : 0;
    return row + 1;
  }

  public static void afterRelease(int token, boolean completed) {
    long now = System.nanoTime();
    Trial t = trial;
    if (token == 0 || t == null) return;
    t.release[1][token - 1] = now; t.release[3][token - 1] = completed ? 1 : 0;
  }

  public static int beforeSwap() {
    Trial t = trial;
    if (t == null || Thread.currentThread() != t.thread) return 0;
    long now = System.nanoTime();
    if (now < t.from) return 0;
    if (t.swaps == LIMIT) { t.droppedSwaps++; return 0; }
    int row = t.swaps++; t.swap[0][row] = now; return row + 1;
  }

  public static void afterSwap(int token, boolean completed) {
    long now = System.nanoTime();
    Trial t = trial;
    if (token == 0 || t == null) return;
    t.swap[1][token - 1] = now; t.swap[2][token - 1] = completed ? 1 : 0;
  }

  public static void event(int kind, long dispatchNs, long nativeNs, int code, boolean pressed, int dx, int dy) {
    Trial t = trial;
    if (t == null || Thread.currentThread() != t.thread || dispatchNs < t.from) return;
    if (t.events == LIMIT) { t.droppedEvents++; t.latestEvent[kind] = -1; return; }
    int row = t.events++;
    t.event[0][row] = dispatchNs; t.event[1][row] = nativeNs; t.event[2][row] = kind;
    t.event[3][row] = code; t.event[4][row] = pressed ? 1 : 0;
    t.event[5][row] = dx; t.event[6][row] = dy; t.latestEvent[kind] = row;
  }

  public static int beforeHandler(int kind) {
    Trial t = trial;
    if (t == null || Thread.currentThread() != t.thread) return 0;
    long now = System.nanoTime();
    if (now < t.from) return 0;
    if (t.handlers == LIMIT) { t.droppedHandlers++; return 0; }
    int row = t.handlers++;
    t.handler[0][row] = now; t.handler[2][row] = kind; t.handler[3][row] = t.latestEvent[kind];
    return row + 1;
  }

  public static void afterHandler(int token, boolean completed) {
    long now = System.nanoTime();
    Trial t = trial;
    if (token == 0 || t == null) return;
    t.handler[1][token - 1] = now; t.handler[4][token - 1] = completed ? 1 : 0;
  }

  private static void finish() throws Exception {
    Trial t = trial;
    check(t != null, "no cursor latency trial is armed");
    trial = null;
    long until = System.nanoTime();
    Map<String, Object> report = new LinkedHashMap<>();
    report.put("label", t.label); report.put("sampleStartNs", t.from); report.put("sampleEndNs", until);
    report.put("window", List.of(t.width, t.height)); report.put("warmupMs", t.warmup);
    report.put("clock", "System.nanoTime for all entry, exit, dispatch and handler timestamps");
    report.put("nativeClockOffsetMs", t.nativeClockOffsetMs);
    report.put("nativeClock", "LWJGL X event milliseconds multiplied by 1000000; 32-bit millisecond wrap; zero/synthetic events excluded from age metrics");
    report.put("limits", List.of("Swap timestamps are CPU calls/returns, not scanout or compositor presentation.",
        "First queue dispatch after release may be synthetic, stale or idle input. Match deliberately injected native events in raw data; uncalibrated ages cannot establish input response.",
        "Timestamped queue-dispatch proxies exclude zero native timestamps and the synthetic mouse WARP_EVENT code -2, but may still contain stale or idle input.",
        "Base Screen handler return measures dispatch completion, not a visual change or successful action. Overrides that skip the base method are not captured.",
        "Native ages require an independently verified X-to-JVM monotonic millisecond offset; no automatic clock-origin assumption.",
        "Collect stock/patched compositor and baseline/candidate mod as separate labels; keep workload, frame cap, VSync, window and warmup fixed.",
        "Correct cursor coordinates, physical input-to-photon latency and owner-session behavior are checked separately."));
    Map<String, Metric> metrics = new LinkedHashMap<>();
    List<Map<String, Object>> releases = new ArrayList<>();
    for (int i = 0; i < t.releases; i++) {
      long start = t.release[0][i], end = t.release[1][i];
      var row = row(i, "entryNs", start, "exitNs", end);
      boolean transition = t.release[2][i] == 1, completed = t.release[3][i] == 1;
      row.put("previouslyGrabbed", transition); row.put("completed", completed);
      if (end != 0) metric(metrics, transition ? "releaseWrapperMs" : "alreadyFreeWrapperMs", end - start);
      if (transition && completed && end != 0) {
        int swap = first(t.swap[0], t.swaps, end), event = first(t.event[0], t.events, end), handler = first(t.handler[0], t.handlers, end);
        row.put("firstFollowingSwap", swap); row.put("firstFollowingQueueDispatch", event); row.put("firstFollowingBaseScreenHandler", handler);
        if (swap >= 0) {
          metric(metrics, "releaseExitToNextSwapEntryMs", t.swap[0][swap] - end);
          if (t.swap[1][swap] != 0) metric(metrics, "releaseEntryToNextSwapReturnMs", t.swap[1][swap] - start);
          if (swap > 0) metric(metrics, "releaseCrossingSwapGapMs", t.swap[0][swap] - t.swap[0][swap - 1]);
        }
        if (event >= 0) {
          metric(metrics, "releaseExitToNextQueueDispatchMs", t.event[0][event] - end);
          while (event < t.events && !timestamped(t, event)) event++;
          row.put("firstFollowingTimestampedQueueDispatch", event == t.events ? -1 : event);
          if (event < t.events) metric(metrics, "releaseExitToNextTimestampedQueueDispatchMs", t.event[0][event] - end);
        }
        if (handler >= 0 && t.handler[1][handler] != 0) metric(metrics, "releaseExitToNextBaseScreenHandlerReturnMs", t.handler[1][handler] - end);
      }
      releases.add(row);
    }
    List<Map<String, Object>> swaps = new ArrayList<>();
    for (int i = 0; i < t.swaps; i++) {
      swaps.add(row(i, "entryNs", t.swap[0][i], "exitNs", t.swap[1][i], "completed", t.swap[2][i]));
      if (t.swap[1][i] != 0) metric(metrics, "swapCallMs", t.swap[1][i] - t.swap[0][i]);
      if (i > 0) metric(metrics, "swapEntryGapMs", t.swap[0][i] - t.swap[0][i - 1]);
      if (i > 0 && t.swap[1][i] != 0 && t.swap[1][i - 1] != 0) metric(metrics, "swapReturnGapMs", t.swap[1][i] - t.swap[1][i - 1]);
    }
    List<Map<String, Object>> events = new ArrayList<>(); int rejectedNativeAges = 0;
    for (int i = 0; i < t.events; i++) {
      var row = row(i, "dispatchNs", t.event[0][i], "nativeNs", t.event[1][i], "kind", t.event[2][i],
          "code", t.event[3][i], "pressed", t.event[4][i], "dx", t.event[5][i], "dy", t.event[6][i]);
      row.put("timestampedNonSynthetic", timestamped(t, i));
      if (t.nativeClockOffsetMs != null && timestamped(t, i)) {
        long age = (t.event[0][i] / 1_000_000L - t.nativeClockOffsetMs - t.event[1][i] / 1_000_000L) & 0xffffffffL;
        if (age <= 10000) { row.put("nativeAgeMs", age); metric(metrics, t.event[2][i] == MOUSE ? "mouseNativeDispatchAgeMs" : "keyboardNativeDispatchAgeMs", age * 1_000_000L); }
        else rejectedNativeAges++;
      }
      events.add(row);
    }
    List<Map<String, Object>> handlers = new ArrayList<>();
    for (int i = 0; i < t.handlers; i++) {
      int event = (int)t.handler[3][i];
      handlers.add(row(i, "entryNs", t.handler[0][i], "exitNs", t.handler[1][i], "kind", t.handler[2][i], "event", event, "completed", t.handler[4][i]));
      if (t.handler[1][i] != 0) {
        metric(metrics, "screenHandlerMs", t.handler[1][i] - t.handler[0][i]);
        if (event >= 0 && timestamped(t, event)) {
          metric(metrics, "dispatchToScreenHandlerReturnMs", t.handler[1][i] - t.event[0][event]);
          int swap = first(t.swap[0], t.swaps, t.handler[1][i]);
          if (swap >= 0) {
            metric(metrics, "timestampedQueueDispatchToNextSwapEntryMs", t.swap[0][swap] - t.event[0][event]);
            if (t.swap[1][swap] != 0) metric(metrics, "timestampedQueueDispatchToNextSwapReturnMs", t.swap[1][swap] - t.event[0][event]);
          }
        }
      }
    }
    Map<String, Object> summaries = new LinkedHashMap<>(); metrics.forEach((name, metric) -> summaries.put(name, metric.report()));
    report.put("metricsMs", summaries); report.put("releases", releases); report.put("swaps", swaps); report.put("events", events); report.put("screenHandlers", handlers);
    report.put("rejectedNativeAges", rejectedNativeAges);
    report.put("dropped", Map.of("releases", t.droppedReleases, "swaps", t.droppedSwaps, "events", t.droppedEvents, "handlers", t.droppedHandlers));
    Path path = Path.of("power-beta-data/reports/cursor-latency-473", t.label + "-" + System.currentTimeMillis() + ".json");
    Files.createDirectories(path.getParent()); Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n");
    log("CURSOR LATENCY473 finished releases=" + t.releases + " swaps=" + t.swaps + " events=" + t.events + " report=" + path);
  }

  private static int first(long[] values, int count, long time) {
    int low = 0, high = count;
    while (low < high) { int mid = (low + high) >>> 1; if (values[mid] < time) low = mid + 1; else high = mid; }
    return low == count ? -1 : low;
  }
  private static boolean timestamped(Trial t, int event) {
    return t.event[1][event] > 0 && (t.event[2][event] != MOUSE || t.event[3][event] != -2);
  }
  private static Map<String, Object> row(int index, Object... pairs) {
    Map<String, Object> out = new LinkedHashMap<>(); out.put("index", index);
    for (int i = 0; i < pairs.length; i += 2) out.put((String)pairs[i], pairs[i + 1]);
    return out;
  }
  private static void metric(Map<String, Metric> metrics, String name, long ns) {
    metrics.computeIfAbsent(name, ignored -> new Metric()).values.add(ns / 1e6);
  }
  private static final class Metric {
    final List<Double> values = new ArrayList<>();
    Map<String, Object> report() {
      Collections.sort(values); int n = values.size();
      return Map.of("count", n, "p50", values.get((int)Math.ceil(n * .50) - 1),
          "p95", values.get((int)Math.ceil(n * .95) - 1), "p99", values.get((int)Math.ceil(n * .99) - 1), "max", values.get(n - 1));
    }
  }
  private static final class Trial {
    final Thread thread = Thread.currentThread();
    final String label; final int warmup, width, height; final Long nativeClockOffsetMs;
    final long[][] release = new long[4][LIMIT], swap = new long[3][LIMIT], event = new long[7][LIMIT], handler = new long[5][LIMIT];
    final int[] latestEvent = {-1, -1};
    long from; int releases, swaps, events, handlers, droppedReleases, droppedSwaps, droppedEvents, droppedHandlers;
    Trial(String label, int warmup, Long nativeClockOffsetMs, int width, int height) {
      this.label = label; this.warmup = warmup; this.nativeClockOffsetMs = nativeClockOffsetMs; this.width = width; this.height = height;
    }
  }
}
