package local.luke.power.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import local.luke.power.visual.VisualConfig;
import net.mine_diver.smoothbeta.client.render.gl.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

/** Shared native/water/neutral capture. Two reusable packed stages; one final GPU upload per stage. */
public final class WaterTerrain {
  private static final int MAX_BYTES = 4 * 1024 * 1024;
  private static final int BASE = 0, CLEAR = 1, NEUTRAL = 2;
  private static final Slot[] slots = {new Slot(), new Slot()};
  private static ByteBuffer packed;
  private static World owner;
  private static Thread thread, meshThread;
  private static boolean prepared, building, affected, waterColumn, overflow, uploading;
  private static boolean held, baseDiffers, clearDiffers, clearRan, mismatch, neutralComplete;
  private static int count, phase, meshDepth, buildingDepth;
  private static long sections, secondPasses, neutralPasses, mismatches, neutralStarted, maxNeutralNanos;
  private WaterTerrain() {}

  public static void reset() {
    owner = null; prepared = false;
    for (Slot slot : slots) { slot.buffer = null; slot.data = null; }
    packed = null;
    HeldLighting.reset();
  }

  public static void prepare(Minecraft mc) {
    boolean changedWorld = owner != mc.world;
    if (changedWorld) { owner = mc.world; prepared = false; }
    var s = VisualConfig.current();
    if (!prepared && mc.world != null && (s.underwaterDarkness < 100 || s.underwaterDarknessOutside < 100)) {
      prepared = true;
      if (!changedWorld && mc.worldRenderer instanceof WaterTerrainUpdates updates) updates.power$prepareWaterColors();
    }
  }

  // Covers EVERY native mesh build, even when endpoint capture is disabled.
  // Fixed-function held lighting must never become a source-dependent baked mesh color.
  public static void beginMesh() {
    if (meshDepth == 0) meshThread = Thread.currentThread();
    meshDepth++;
  }
  public static void endMesh() { if (--meshDepth == 0) meshThread = null; }
  public static boolean meshing() { return meshDepth > 0 && meshThread == Thread.currentThread(); }

  public static boolean begin(boolean captureHeld) {
    if (building || (!prepared && !captureHeld) || !RenderControls.rendering() || RenderControls.fullBright()) return false;
    thread = Thread.currentThread(); building = true; buildingDepth = meshDepth;
    phase = BASE; held = captureHeld;
    affected = waterColumn = overflow = baseDiffers = clearDiffers = clearRan = mismatch = neutralComplete = false;
    count = 0; sections++;
    return true;
  }

  private static boolean current() { return building && thread == Thread.currentThread() && meshDepth == buildingDepth; }
  public static boolean neutral() { return current() && phase == NEUTRAL; }
  public static boolean observing() { return current() && held && phase != NEUTRAL; }
  public static boolean detecting() { return current() && prepared && phase == BASE; }
  public static int buildDarkness(int fallback) { return current() ? phase == CLEAR ? 0 : 100 : fallback; }
  public static void affected() { if (detecting()) affected = true; }
  public static void waterColumn() { if (current() && phase != NEUTRAL) waterColumn = true; }
  public static boolean dependsOnWaterColumn() { return waterColumn; }
  public static boolean prepared() { return prepared; }

  public static void observe(float shade, float neutralShade) {
    if (!current() || !held || shade == neutralShade) return;
    if (phase == BASE) baseDiffers = true;
    else if (phase == CLEAR) clearDiffers = true;
  }

  public static boolean secondPass() {
    if (!affected || overflow || count == 0) return false;
    phase = CLEAR; clearRan = true; secondPasses++;
    return true;
  }

  public static boolean neutralPass() {
    if (!held || overflow) return false;
    if (clearRan) for (int i = 0; i < count; i++) if (!slots[i].waterSeen) badMatch();
    if (mismatch) return false;
    if (!baseDiffers || clearRan && !clearDiffers) {
      for (int i = 0; i < count; i++) {
        if (!baseDiffers) TerrainVertexColors.reuseNative(slots[i].data);
        else TerrainVertexColors.reuseWater(slots[i].data);
      }
      neutralComplete = true;
      return false;
    }
    phase = NEUTRAL; neutralPasses++; neutralStarted = System.nanoTime();
    return true;
  }

  public static boolean complete() {
    if (phase == NEUTRAL) {
      maxNeutralNanos = Math.max(maxNeutralNanos, System.nanoTime() - neutralStarted);
      for (int i = 0; i < count; i++) if (!slots[i].neutralSeen) badMatch();
      neutralComplete = !mismatch;
    }
    if (mismatch || overflow) for (int i = 0; i < count; i++) TerrainVertexColors.clearNeutral(slots[i].data);
    return held && !mismatch && !overflow && (neutralComplete || count == 0);
  }

  /** Native block renderers still own geometry, tint and ambient occlusion. */
  public static boolean upload(VertexBuffer buffer, ByteBuffer data) {
    if (uploading) return false;
    if (current()) {
      if (phase == BASE && !overflow && data.limit() <= MAX_BYTES && count < slots.length) {
        Slot slot = slots[count++];
        slot.buffer = buffer; slot.published = slot.waterSeen = slot.neutralSeen = false;
        slot.data = capacity(slot.data, TerrainVertexColors.outputSize(data.limit()));
        TerrainVertexColors.pack(data, null, slot.data);
        return true;
      }
      if (phase != BASE) {
        for (int i = 0; i < count; i++) {
          Slot slot = slots[i];
          if (slot.buffer != buffer || (phase == CLEAR ? slot.waterSeen : slot.neutralSeen)) continue;
          boolean matches;
          if (phase == CLEAR) { matches = TerrainVertexColors.water(slot.data, data); slot.waterSeen = true; }
          else { matches = TerrainVertexColors.neutral(slot.data, data); slot.neutralSeen = true; }
          if (!matches) badMatch();
          return true;
        }
        badMatch();
        return true;
      }
      overflow = true; flush();
    }
    publishNative(buffer, data);
    return true;
  }

  private static void badMatch() { mismatch = true; mismatches++; }
  private static void flush() {
    for (int i = 0; i < count; i++) {
      Slot slot = slots[i];
      if (!slot.published) { publish(slot.buffer, slot.data); slot.published = true; }
    }
  }

  public static void end() {
    try {
      if (held && !neutralComplete) for (int i = 0; i < count; i++) TerrainVertexColors.clearNeutral(slots[i].data);
      flush();
    }
    finally {
      for (Slot slot : slots) slot.buffer = null;
      building = false; thread = null; count = 0;
    }
  }

  private static void publishNative(VertexBuffer buffer, ByteBuffer nativeData) {
    int size = TerrainVertexColors.outputSize(nativeData.limit());
    ByteBuffer output = size <= MAX_BYTES * 2 ? (packed = capacity(packed, size)) : direct(size);
    TerrainVertexColors.pack(nativeData, null, output);
    publish(buffer, output);
  }
  private static void publish(VertexBuffer buffer, ByteBuffer output) {
    uploading = true;
    try { buffer.upload(output); }
    finally { uploading = false; }
  }
  private static ByteBuffer capacity(ByteBuffer old, int size) {
    if (old != null && old.capacity() >= size) return old;
    return direct(Math.max(1024, Integer.highestOneBit(Math.max(1, size - 1)) << 1));
  }
  private static ByteBuffer direct(int bytes) { return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder()); }
  public static long sections() { return sections; }
  public static long secondPasses() { return secondPasses; }
  public static long neutralPasses() { return neutralPasses; }
  public static long mismatches() { return mismatches; }
  public static long maxNeutralNanos() { return maxNeutralNanos; }
  public static long retainedBytes() {
    long total = packed == null ? 0 : packed.capacity();
    for (Slot slot : slots) if (slot.data != null) total += slot.data.capacity();
    return total;
  }
  private static final class Slot { VertexBuffer buffer; ByteBuffer data; boolean published, waterSeen, neutralSeen; }
}
