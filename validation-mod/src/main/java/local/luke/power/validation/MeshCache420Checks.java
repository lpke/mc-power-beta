package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.field;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import local.luke.power.render.*;
import net.minecraft.class_66;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Matched native mesh rebuilds in one tick: no terrain, lighting or camera changes between arms. */
public final class MeshCache420Checks {
  public static boolean bypass;
  private static final int SECTIONS = 12, WARMUP = 24, PAIRS = 24;
  private MeshCache420Checks() {}

  public static void run(Minecraft mc, String label) throws Exception {
    check(label.matches("[a-zA-Z0-9_-]{1,60}"), "invalid report label");
    check(mc.world != null && mc.player != null && mc.currentScreen == null && !mc.world.isRemote,
        "open a disposable singleplayer world");
    check(!RenderControls.fullBright(), "disable full bright for the mesh benchmark");
    check(HeldLighting.emission() == 0, "remove held light sources before the mesh benchmark");
    class_66[] live = (class_66[])field(mc.worldRenderer, "field_1809");
    var selected = Arrays.stream(live).filter(m -> m != null && (!m.field_244[0] || !m.field_244[1]))
        .filter(m -> mc.world.method_155(m.field_231 - 1, m.field_232 - 1, m.field_233 - 1,
            m.field_231 + 16, m.field_232 + 16, m.field_233 + 16))
        .sorted(Comparator.comparingDouble(m -> distance(mc, m))).limit(SECTIONS).toList();
    check(selected.size() == SECTIONS, "benchmark needs twelve loaded nonempty sections");
    int lists = GL11.glGenLists(SECTIONS * 3);
    var meshes = new ArrayList<class_66>();
    var coordinates = new ArrayList<List<Integer>>();
    double[] enabled = new double[PAIRS * 2], disabled = new double[PAIRS * 2];
    long mismatches = WaterTerrain.mismatches(), time = mc.world.getTime();
    int sky = mc.world.field_202;
    try {
      for (int i = 0; i < selected.size(); i++) {
        var source = selected.get(i);
        meshes.add(new class_66(mc.world, new ArrayList<>(), source.field_231, source.field_232,
            source.field_233, 16, lists + i * 3));
        coordinates.add(List.of(source.field_231, source.field_232, source.field_233));
      }
      WaterTerrain.prepare(mc);
      for (int i = 0; i < WARMUP; i++) batch(meshes, (i & 1) == 0);
      batch(meshes, false); var cachedBytes = snapshot(meshes);
      batch(meshes, true); check(cachedBytes.equals(snapshot(meshes)), "cache changed native packed mesh bytes");
      for (int pair = 0; pair < PAIRS; pair++) {
        // ABBA reverses order within every pair and balances slow JIT/temperature drift.
        disabled[pair * 2] = batch(meshes, true);
        enabled[pair * 2] = batch(meshes, false);
        enabled[pair * 2 + 1] = batch(meshes, false);
        disabled[pair * 2 + 1] = batch(meshes, true);
      }
      check(time == mc.world.getTime() && sky == mc.world.field_202, "daylight changed during paired rebuilds");
      check(WaterTerrain.mismatches() == mismatches, "lighting pass geometry mismatch");
      check(GL11.glGetError() == 0, "OpenGL error during mesh benchmark");
      var report = new LinkedHashMap<String, Object>();
      report.put("label", label); report.put("renderer", GL11.glGetString(GL11.GL_RENDERER));
      report.put("sections", coordinates); report.put("warmupBatches", WARMUP); report.put("meshesPerArm", SECTIONS * enabled.length);
      report.put("daylightTime", time); report.put("skyDarkness", sky); report.put("packedBytesEqual", true);
      var visual = local.luke.power.visual.VisualConfig.current();
      report.put("underwaterDarkness", visual.underwaterDarkness);
      report.put("underwaterDarknessOutside", visual.underwaterDarknessOutside);
      report.put("heldEmission", HeldLighting.emission());
      report.put("cacheEnabled", stats(enabled)); report.put("cacheBypassed", stats(disabled));
      report.put("totalReductionPercent", (1 - Arrays.stream(enabled).sum() / Arrays.stream(disabled).sum()) * 100);
      Path file = Path.of("power-beta-data/reports/performance-420", "meshes-" + label + "-" + System.currentTimeMillis() + ".json");
      Files.createDirectories(file.getParent());
      Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(report));
      log("MESH CACHE 420 COMPLETE " + file + " " + report);
    } finally {
      bypass = false; WaterLighting.clear();
      for (var section : meshes) {
        for (int pass = 0; pass < 2; pass++) {
          Object buffer = section.getClass().getMethod("smoothbeta_getBuffer", int.class).invoke(section, pass);
          buffer.getClass().getMethod("upload", java.nio.ByteBuffer.class).invoke(buffer, BufferUtils.createByteBuffer(0));
        }
        section.method_302();
      }
      GL11.glDeleteLists(lists, SECTIONS * 3);
    }
  }

  private static double distance(Minecraft mc, class_66 section) {
    double x = section.field_231 + 8 - mc.player.x, y = section.field_232 + 8 - mc.player.y,
        z = section.field_233 + 8 - mc.player.z;
    return x * x + y * y + z * z;
  }
  private static double batch(List<class_66> meshes, boolean disable) {
    bypass = disable; GL11.glFinish(); RenderControls.beginRender(false);
    long start = System.nanoTime();
    try { for (var section : meshes) { section.field_249 = true; section.method_296(); } }
    finally { RenderControls.endRender(); }
    return (System.nanoTime() - start) / 1e6;
  }
  private static List<java.nio.ByteBuffer> snapshot(List<class_66> meshes) throws Exception {
    var result = new ArrayList<java.nio.ByteBuffer>();
    for (var mesh : meshes) for (int pass = 0; pass < 2; pass++)
      result.add(WaterTerrainChecks.read(mesh, pass));
    return result;
  }
  private static Map<String, Object> stats(double[] values) {
    double[] sorted = values.clone(); Arrays.sort(sorted);
    return Map.of("batchMs", values, "totalMs", Arrays.stream(values).sum(),
        "medianBatchMs", sorted[sorted.length / 2], "p95BatchMs", sorted[(int)Math.ceil(sorted.length * .95) - 1]);
  }
}
