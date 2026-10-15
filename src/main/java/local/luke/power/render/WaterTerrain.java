package local.luke.power.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import local.luke.power.visual.VisualConfig;
import net.mine_diver.smoothbeta.client.render.gl.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

/** One section's scratch data, reused on the render thread. GPU geometry is never duplicated. */
public final class WaterTerrain {
  private static final int MAX_BYTES = 4 * 1024 * 1024;
  private static final Slot[] slots = {new Slot(), new Slot()};
  private static ByteBuffer packed;
  private static World owner;
  private static Thread thread;
  private static boolean prepared, building, clearPass, affected, waterColumn, overflow, uploading;
  private static int count;
  private static long sections, secondPasses, mismatches;
  private WaterTerrain() {}

  public static void reset() {
    owner = null; prepared = false;
    // No world or native buffer references survive a world exit.
    for (Slot slot : slots) { slot.buffer = null; slot.data = null; }
    packed = null;
  }

  public static void prepare(Minecraft mc) {
    boolean changedWorld = owner != mc.world;
    if (changedWorld) { owner = mc.world; prepared = false; }
    var s = VisualConfig.current();
    if (!prepared && mc.world != null && (s.underwaterDarkness < 100 || s.underwaterDarknessOutside < 100)) {
      prepared = true;
      // Only the first opt-in needs endpoints added to already-built terrain.
      // Surface crossings, slider changes and Cancel subsequently update a uniform.
      if (!changedWorld && mc.worldRenderer instanceof WaterTerrainUpdates updates) updates.power$prepareWaterColors();
    }
  }

  public static boolean begin() {
    if (building || !prepared || !RenderControls.rendering() || RenderControls.fullBright()) return false;
    thread = Thread.currentThread(); building = true; clearPass = affected = waterColumn = overflow = false; count = 0;
    sections++;
    return true;
  }

  private static boolean current() { return building && thread == Thread.currentThread(); }
  public static boolean detecting() { return current() && !clearPass; }
  public static int buildDarkness(int fallback) { return current() ? clearPass ? 0 : 100 : fallback; }
  public static void affected() { if (current()) affected = true; }
  public static void waterColumn() { if (current()) waterColumn = true; }
  public static boolean dependsOnWaterColumn() { return waterColumn; }
  public static boolean prepared() { return prepared; }
  public static boolean secondPass() {
    if (!affected || overflow || count == 0) return false;
    clearPass = true; secondPasses++;
    return true;
  }

  /** Intercepts uploads only; native block renderers still own geometry and AO. */
  public static boolean upload(VertexBuffer buffer, ByteBuffer data) {
    if (uploading) return false;
    if (current()) {
      if (!clearPass && !overflow && data.limit() <= MAX_BYTES && count < slots.length) {
        Slot slot = slots[count++];
        slot.buffer = buffer; slot.merged = false;
        slot.data = capacity(slot.data, data.limit());
        slot.data.clear(); slot.data.put(data.duplicate().position(0)); slot.data.flip();
        return true;
      }
      if (clearPass) {
        for (int i = 0; i < count; i++) {
          Slot slot = slots[i];
          if (slot.buffer != buffer || slot.merged) continue;
          boolean matches = publish(buffer, slot.data, data);
          slot.merged = true;
          if (!matches) mismatches++;
          return true;
        }
        // Ignore a new, non-deterministic pass. The original pass flags and data win.
        mismatches++;
        return true;
      }
      overflow = true;
      // Flush earlier stages before an unusually large/multi-upload model.
      flush();
    }
    publish(buffer, data, null);
    return true;
  }

  private static void flush() {
    for (int i = 0; i < count; i++) {
      Slot slot = slots[i];
      if (!slot.merged) { publish(slot.buffer, slot.data, null); slot.merged = true; }
    }
  }

  public static void end() {
    try { flush(); }
    finally {
      for (Slot slot : slots) slot.buffer = null;
      building = clearPass = false; thread = null; count = 0;
    }
  }

  private static boolean publish(VertexBuffer buffer, ByteBuffer nativeData, ByteBuffer clearData) {
    int size = TerrainVertexColors.outputSize(nativeData.limit());
    // Normal sections retain a bounded reusable buffer. Exceptional models use a temporary one.
    ByteBuffer output = size <= MAX_BYTES * 2 ? (packed = capacity(packed, size)) : direct(size);
    boolean matches = TerrainVertexColors.pack(nativeData, clearData, output);
    uploading = true;
    try { buffer.upload(output); }
    finally { uploading = false; }
    return matches;
  }

  private static ByteBuffer capacity(ByteBuffer old, int size) {
    if (old != null && old.capacity() >= size) return old;
    return direct(Math.max(1024, Integer.highestOneBit(Math.max(1, size - 1)) << 1));
  }
  private static ByteBuffer direct(int bytes) { return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder()); }
  public static long sections() { return sections; }
  public static long secondPasses() { return secondPasses; }
  public static long mismatches() { return mismatches; }
  private static final class Slot { VertexBuffer buffer; ByteBuffer data; boolean merged; }
}
