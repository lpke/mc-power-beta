package local.luke.power.ui;

import java.util.*;

/** Stable identities for native and bundled left-side F3 providers. */
public enum DebugLines {
  VERSION("version", "Version and frame rate", "Minecraft Beta 1.7.3 (120 fps, 0 chunk updates)", ""),
  TERRAIN("terrain", "Terrain statistics", "C: 256/1024. F: 64, O: 12, E: 2", ""),
  ENTITIES("entities", "Entity statistics", "E: 8/32. B: 0, I: 32", ""),
  PARTICLES("particles", "Particle statistics", "P: 4. T: 1024", ""),
  WORLD("world", "Chunk cache statistics", "ChunkCache: 1024", ""),
  X("x", "X coordinate", "x: 128.0", ""),
  Y("y", "Y coordinate", "y: 64.0", ""),
  Z("z", "Z coordinate", "z: -32.0", ""),
  NATIVE_FACING("nativeFacing", "Native facing value", "f: 0", ""),
  SEED("seed", "World seed", "Seed: 123456789", "power_client_fixes:config.enableDebugMenuWorldSeed"),
  PLAY_TIME("playTime", "Hours played", "Play Time: 3 hours", "power_compat:config.addTotalPlayTimeToDebugOverlay"),
  LIGHT("blockLight", "Block light", "Block light: 0", "power_compat:config.addBlockLightToDebugOverlay"),
  BIOME("biome", "Biome", "Biome: Forest", "power_compat:config.addBiomeToDebugOverlay"),
  DAY("day", "Day count", "Day: 12", "power_compat:config.addDayCounterToDebugOverlay"),
  SLIME("slime", "Slime chunk", "Slime Chunk: false", "power_compat:config.addSlimeChunkToDebugOverlay"),
  FACING("facing", "Facing direction", "Facing: south (+Z)", "power_compat:config.addFacingToDebugOverlay"),
  TIME("time", "Time of day", "Time: 10:30 AM", "interface.debugTimeOfDay"),
  MUSIC("music", "Current music", "Music: C418 - Sweden", "power_environment:config.MUSIC_CONFIG.overlayForMusicInDebug"),
  CULL_TIME("cullingTime", "Culling pass time", "[Culling] Last pass: 1ms", ""),
  CULL_BLOCKS("cullingBlocks", "Block entity culling counts", "[Culling] Rendered Block Entities: 4 Skipped: 12", ""),
  CULL_ENTITIES("cullingEntities", "Entity culling counts", "[Culling] Rendered Entities: 8 Skipped: 24", "");

  public final String id, label, sample;
  private final String setting;
  DebugLines(String id, String label, String sample, String setting) {
    this.id = id; this.label = label; this.sample = sample; this.setting = setting;
  }
  public String setting() { return setting.isEmpty() ? "interface.debugLines." + id : setting; }
  public boolean ownsSetting() { return setting.isEmpty(); }
  public boolean culling() { return id.startsWith("culling"); }
  public static DebugLines find(String id) { for (var line : values()) if (line.id.equals(id)) return line; return null; }
  public static String identify(String text, int x, int y) {
    if (text.startsWith("Used memory:") || text.startsWith("Allocated memory:") || text.equals("64-bit Java") || x > 4) return "";
    if (text.startsWith("Minecraft ") || text.startsWith("Power Beta ")) return VERSION.id;
    if (text.startsWith("C:")) return TERRAIN.id;
    if (text.startsWith("E:")) return ENTITIES.id;
    if (text.startsWith("P:") || text.startsWith("Particles:")) return PARTICLES.id;
    if (text.contains("ChunkCache") || text.startsWith("T:")) return WORLD.id;
    if (text.startsWith("x:")) return X.id;
    if (text.startsWith("y:")) return Y.id;
    if (text.startsWith("z:")) return Z.id;
    if (text.startsWith("f:")) return NATIVE_FACING.id;
    if (text.startsWith("[Culling] Last pass:")) return CULL_TIME.id;
    if (text.startsWith("[Culling] Rendered Block Entities:")) return CULL_BLOCKS.id;
    if (text.startsWith("[Culling] Rendered Entities:")) return CULL_ENTITIES.id;
    for (var line : values()) {
      int colon = line.sample.indexOf(':');
      if (colon > 0 && text.startsWith(line.sample.substring(0, colon + 1))) return line.id;
    }
    return "provider." + Integer.toUnsignedString(text.split(":", 2)[0].hashCode(), 16);
  }
}
