package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.management.ManagementFactory;
import java.lang.reflect.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.function.ToIntFunction;
import local.luke.power.audio.*;
import local.luke.power.config.Catalog;
import local.luke.power.hud.*;
import net.minecraft.class_267;
import net.minecraft.client.Minecraft;

/** Explicit paired microbenchmarks; run outside frame samples. No playback or preference writes. */
final class Smoothness470AudioHudChecks {
  private static final int ITERATIONS = 2000;
  private static volatile Object consumed;
  private static long legacyStats, legacyUris, legacyExceptions;
  private Smoothness470AudioHudChecks() {}
  @FunctionalInterface private interface Work { Object run() throws Exception; }
  private record Measurement(long nanos, long allocatedBytes) {}

  static void run(Minecraft mc, String action) throws Exception {
    check(action.equals("check"), "use smoothness-470-audio-hud check");
    Path directory = Path.of("power-beta-data/reports/release-470"); Files.createDirectories(directory);
    Path owned = null;
    MusicLibrary.Track track = AudioController.customTracks().stream().filter(MusicLibrary.Track::playable).findFirst().orElse(null);
    if (track == null) {
      owned = Files.createTempFile(directory, "music-metadata-", ".wav"); Files.write(owned, new byte[100]);
      track = new MusicLibrary.Track(owned.toAbsolutePath(), "metadata.wav");
    }
    try {
      Method entry = AudioController.class.getDeclaredMethod("entry", MusicLibrary.Track.class); entry.setAccessible(true);
      MusicLibrary.Track selected = track;
      var bundled = new class_267("calm1.ogg", new URL("jar:file:/unused-reference.jar!/calm1.ogg"));
      var cluster = new InfoHudSettings.Cluster();
      cluster.coordinates = cluster.direction = cluster.biome = cluster.timeOfDay = cluster.day = cluster.chunk = cluster.fps = true;
      InfoHudData data = InfoHudData.sample(); Object font = mc.textRenderer;
      ToIntFunction<String> measure = mc.textRenderer::getWidth;
      var frameCache = new InfoHudFrame.Cache();
      Work oldEntry = () -> legacyEntry(selected), newEntry = () -> entry.invoke(null, selected);
      Work oldId = () -> legacyId(bundled), newId = () -> AudioController.trackId(bundled);
      Work oldHud = () -> InfoHudFrame.at(cluster, data, 320, 240, measure);
      Work newHud = () -> frameCache.get(cluster, data, 320, 240, font, measure);
      for (int i = 0; i < 300; i++) {
        consumed = oldEntry.run(); consumed = newEntry.run(); consumed = oldId.run(); consumed = newId.run();
        consumed = oldHud.run(); consumed = newHud.run();
      }
      legacyStats = legacyUris = legacyExceptions = 0;
      Map<String, Object> report = new LinkedHashMap<>();
      report.put("iterationsPerArm", ITERATIONS);
      report.put("entry", paired(oldEntry, newEntry));
      report.put("bundledTrackId", paired(oldId, newId));
      report.put("hudFrame", paired(oldHud, newHud));
      report.put("legacyExplicitRegularFileChecks", legacyStats);
      report.put("legacyPathToUriCalls", legacyUris);
      report.put("legacyBundledUrlExceptions", legacyExceptions);
      report.put("currentControllerRegularFileChecks", 0);
      report.put("currentControllerPathToUriCalls", 0);
      report.put("currentBundledUrlExceptions", 0);
      report.put("filesystemCountsNote", "Legacy counters count calls in the frozen reference. Current zero counts follow the inspected metadata-only controller path; no JRE instrumentation is installed.");
      check(legacyStats == ITERATIONS * 2L && legacyUris == legacyStats && legacyExceptions == ITERATIONS * 2L,
          "paired reference did not exercise expected filesystem/exception paths");
      check(newId.run().equals(oldId.run()), "bundled stable track identity changed");
      check(((class_267)newEntry.run()).field_2127.toExternalForm().equals(((class_267)oldEntry.run()).field_2127.toExternalForm()), "custom file URL changed");
      check(newHud.run() == newHud.run(), "steady HUD frames were rebuilt");
      Files.writeString(directory.resolve("smoothness-audio-hud.json"), Catalog.JSON.toJson(report));
      log("PASS SMOOTHNESS470 AUDIO HUD paired metadata, URL and retained-frame checks");
    } finally { if (owned != null) Files.deleteIfExists(owned); consumed = null; }
  }

  private static List<Map<String, Object>> paired(Work before, Work after) throws Exception {
    var rows = new ArrayList<Map<String, Object>>();
    for (int arm : new int[]{0, 1, 1, 0}) {
      Measurement result = sample(arm == 0 ? before : after);
      rows.add(Map.of("arm", arm == 0 ? "reference" : "current", "nanos", result.nanos,
          "mainThreadAllocatedBytes", result.allocatedBytes));
    }
    return rows;
  }
  private static Measurement sample(Work work) throws Exception {
    long allocated = allocated(), start = System.nanoTime();
    for (int i = 0; i < ITERATIONS; i++) consumed = work.run();
    return new Measurement(System.nanoTime() - start, allocated() - allocated);
  }
  private static long allocated() {
    var bean = ManagementFactory.getThreadMXBean();
    if (!(bean instanceof com.sun.management.ThreadMXBean allocation) || !allocation.isThreadAllocatedMemorySupported()) return 0;
    if (!allocation.isThreadAllocatedMemoryEnabled()) allocation.setThreadAllocatedMemoryEnabled(true);
    return allocation.getThreadAllocatedBytes(Thread.currentThread().getId());
  }
  private static Object legacyEntry(MusicLibrary.Track track) throws Exception {
    if (!track.playable()) return null;
    legacyStats++;
    if (!Files.isRegularFile(track.playbackPath())) return null;
    legacyUris++;
    return new class_267(track.playbackName(), track.playbackPath().toUri().toURL());
  }
  private static String legacyId(class_267 track) {
    String key;
    try { key = Path.of(track.field_2127.toURI()).toUri().toString(); }
    catch (Exception ignored) { legacyExceptions++; key = track.field_2127.toExternalForm(); }
    // This synthetic bundled URL cannot belong to an imported source.
    return "music:" + track.field_2126;
  }
}
