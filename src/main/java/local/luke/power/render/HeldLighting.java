package local.luke.power.render;

import java.nio.FloatBuffer;
import local.luke.power.heldlight.HeldLightConfig;
import local.luke.power.offhand.Offhand;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.lwjgl.BufferUtils;

/** One physical player source, sampled once per frame. No world light or chunk writes. */
public final class HeldLighting {
  private static final float[] curve = new float[16];
  private static final FloatBuffer uniformCurve = BufferUtils.createFloatBuffer(16);
  private static World owner;
  private static double x, y, z;
  private static int emission, admitted, refreshed, captureDepth;
  private static float strength;
  private static long lastFrame, requested, captures, refreshes;
  private HeldLighting() {}

  public static void reset() {
    owner = null; emission = admitted = refreshed = captureDepth = 0; strength = 0; lastFrame = 0;
  }

  public static void frame(Minecraft mc, float delta) {
    frame(mc, delta, true);
  }

  private static void frame(Minecraft mc, float delta, boolean prepare) {
    if (captureDepth != 0) return;
    long now = System.nanoTime();
    float elapsed = lastFrame == 0 ? 0 : Math.min(.05f, Math.max(0, (now - lastFrame) / 1_000_000_000f));
    lastFrame = now; admitted = refreshed = 0;
    if (mc == null || mc.world == null || mc.player == null || mc.player.world != mc.world
        || mc.player.dead || mc.player.health <= 0 || !HeldLightConfig.current().enabled || RenderControls.fullBright()) {
      owner = null; emission = 0; strength = 0; return;
    }
    var player = mc.player;
    int next = emission(player.inventory.main[player.inventory.selectedSlot]);
    if (Offhand.enabled(player)) next = Math.max(next, emission(Offhand.get(player.inventory)));
    if (next == 0) { owner = mc.world; emission = 0; strength = 0; return; }
    delta = Float.isFinite(delta) ? Math.max(0, Math.min(1, delta)) : 1;
    double nextX = player.prevX + (player.x - player.prevX) * delta;
    double nextY = player.prevY + (player.y - player.prevY) * delta - player.field_1631 + 1.0;
    double nextZ = player.prevZ + (player.z - player.prevZ) * delta;
    if (!HeldLightMath.finiteSource(nextX, nextY, nextZ)) { reset(); return; }
    double dx = x - nextX, dy = y - nextY, dz = z - nextZ;
    if (owner != mc.world || dx * dx + dy * dy + dz * dz > HeldLightMath.RADIUS * HeldLightMath.RADIUS) strength = 0;
    owner = mc.world; x = nextX; y = nextY; z = nextZ; emission = next;
    for (int i = 0; i < 16; i++) {
      float value = owner.dimension.field_2178[i];
      curve[i] = Float.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0;
    }
    uniformCurve.clear(); uniformCurve.put(curve).flip();
    boolean ready = prepare && mc.worldRenderer instanceof WaterTerrainUpdates updates
        && updates.power$prepareHeldLight(owner, x, y, z);
    strength = HeldLightMath.advanceStrength(strength, elapsed, ready);
  }

  /** Freeze the physical source and finish its loaded influence meshes before the first image tile. */
  public static void beginCapture(Minecraft mc, float delta) {
    if (captureDepth != 0) { captureDepth++; return; }
    frame(mc, delta, false);
    captureDepth = 1;
    admitted = 0;
    strength = 0;
    if (owner != null && emission != 0 && mc.worldRenderer instanceof WaterTerrainUpdates updates
        && updates.power$prepareHeldLightCapture(owner, x, y, z)) strength = 1;
  }

  public static void endCapture() {
    if (captureDepth == 0 || --captureDepth != 0) return;
    admitted = 0;
    lastFrame = System.nanoTime();
  }

  private static int emission(ItemStack stack) {
    if (stack == null || stack.count <= 0) return 0;
    int id = stack.itemId;
    // Lava buckets are items rather than block stacks. All block sources use the native registry.
    if (id == 327) return 15;
    return id >= 0 && id < Block.BLOCKS.length && Block.BLOCKS[id] != null
        ? Math.min(15, Math.max(0, Block.field_1943[id])) : 0;
  }

  /** One new admission per frame, plus bounded existing-endpoint refreshes; captures admit at most 27. */
  public static boolean admit(World world, int sx, int sy, int sz) {
    return admit(world, sx, sy, sz, false);
  }

  /** Refresh only the at-most-eight loaded sections in the physical source's actual influence. */
  public static boolean admit(World world, int sx, int sy, int sz, boolean prepared) {
    if (owner != world || emission == 0
        || !RenderControls.rendering() || RenderControls.fullBright()
        || HeldLightMath.sectionDistanceSquared(x, y, z, sx, sy, sz) >= HeldLightMath.PREPARE_RADIUS * HeldLightMath.PREPARE_RADIUS)
      return false;
    if (captureDepth == 0 && prepared && strength > 0
        && HeldLightMath.sectionDistanceSquared(x, y, z, sx, sy, sz) < HeldLightMath.RADIUS * HeldLightMath.RADIUS) {
      if (refreshed >= 8) return false;
      refreshed++; refreshes++; captures++; return true;
    }
    if (admitted >= (captureDepth == 0 ? 1 : 27)) return false;
    admitted++; captures++; return true;
  }

  public static float shade(World world, int sx, int sy, int sz, float nativeShade, float neutralShade) {
    if (!shading(world)) return nativeShade;
    double dx = sx + .5 - x, dy = sy + .5 - y, dz = sz + .5 - z;
    return Math.max(nativeShade, neutralShade * strength * HeldLightMath.brightness(curve, emission, dx * dx + dy * dy + dz * dz));
  }

  public static boolean shading(World world) {
    return world == owner && emission != 0 && strength != 0 && RenderControls.rendering() && !WaterTerrain.meshing();
  }

  public static float strength() { return RenderControls.rendering() && !RenderControls.fullBright() ? strength : 0; }
  public static float regionStrength(int ox, int oy, int oz) {
    double radius = HeldLightMath.RADIUS, sx = x - ox, sy = y - oy, sz = z - oz;
    return sx < -radius || sx > 1024 + radius || sy < -radius || sy > 128 + radius
        || sz < -radius || sz > 1024 + radius ? 0 : strength();
  }
  public static int emission() { return emission; }
  public static float regionX(int origin) { return (float)(x - origin); }
  public static float regionY(int origin) { return (float)(y - origin); }
  public static float regionZ(int origin) { return (float)(z - origin); }
  public static FloatBuffer curve() { uniformCurve.position(0); return uniformCurve; }
  public static void requested() { requested++; }
  public static long requests() { return requested; }
  public static long captures() { return captures; }
  public static long refreshes() { return refreshes; }
}
