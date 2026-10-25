package local.luke.power.light;

import net.minecraft.block.Block;
import net.minecraft.class_56;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public final class LightOverlay {
  private static final LightCache CACHE = new LightCache();
  private static final LightTextBatch TEXT = new LightTextBatch();
  private static LightSettings style;
  private static int low, high;
  private static World world;

  public static void tick(Minecraft mc) {
    LightSettings settings = LightConfig.current();
    if (!settings.enabled || mc.world == null || mc.player == null || mc.options.hideHud) {
      CACHE.clear();
      world = null;
      return;
    }
    if (world != mc.world) { CACHE.clear(); world = mc.world; }
    if (mc.currentScreen != null) return;
    var camera = mc.field_2807 == null ? mc.player : mc.field_2807;
    CACHE.tick(world, (int)Math.floor(camera.x), (int)Math.floor(camera.boundingBox.minY) - 1,
        (int)Math.floor(camera.z), settings, (x, y, z) -> sample(world, settings, x, y, z));
  }

  /** Never asks the chunk provider to load or generate terrain. Beta's land-spawn
   * support test differs from modern Minecraft; this does not simulate mob RNG. */
  public static int sample(World world, LightSettings settings, int x, int y, int z) {
    if (y < 0 || y > 126 || !world.method_239(x, y, z)) return -1;
    int id = world.getBlockId(x, y, z);
    Block support = Block.BLOCKS[id];
    if (support == null || !support.method_1623()) return -1;
    Box box = support.method_1624(world, x, y, z);
    if (box == null || box.minX > x || box.maxX < x + 1 || box.minZ > z
        || box.maxZ < z + 1 || box.maxY != y + 1) return -1;
    if (!clear(world, x, y + 1, z)) return -1;
    if (settings.spawnableOnly && (!world.method_1780(x, y, z)
        || y > 125 || !clear(world, x, y + 2, z))) return -1;
    return settings.lightSource == 0 ? world.method_164(class_56.BLOCK, x, y + 1, z)
        : world.method_158(x, y + 1, z, false);
  }

  private static boolean clear(World world, int x, int y, int z) {
    Block block = Block.BLOCKS[world.getBlockId(x, y, z)];
    return block == null || !block.field_1900.method_893() && block.method_1624(world, x, y, z) == null;
  }

  public static void render(Minecraft mc, float delta) {
    LightSettings s = LightConfig.current();
    if (mc.currentScreen instanceof local.luke.power.input.PhotoProjection.CameraOwner
        || !s.enabled || mc.world == null || mc.world != world || mc.player == null
        || mc.options.hideHud || CACHE.cells().isEmpty()) return;
    var camera = mc.field_2807 == null ? mc.player : mc.field_2807;
    double cx = camera.field_1637 + (camera.x - camera.field_1637) * delta;
    double cy = camera.field_1638 + (camera.y - camera.field_1638) * delta;
    double cz = camera.field_1639 + (camera.z - camera.field_1639) * delta;
    if (style != s) { low = LightSettings.rgb(s.lowColor); high = LightSettings.rgb(s.highColor); style = s; }
    TEXT.prepare(mc.textRenderer);
    // Rotate in 90-degree steps so the text reads toward the viewing direction.
    float angle = Math.round(camera.yaw / 90f) * 90f;
    double cosine = Math.cos(Math.toRadians(angle)), sine = Math.sin(Math.toRadians(angle));
    double scale = s.textSize / 12;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glPushMatrix();
    try {
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_FOG);
      GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glDepthMask(false);
      TEXT.begin();
      try {
        for (LightCache.Cell cell : CACHE.cells()) {
          double dx = cell.x() + .5 - camera.x, dz = cell.z() + .5 - camera.z;
          if (dx * dx + dz * dz > s.radius * s.radius
              || Math.abs(cell.y() + 1 - camera.boundingBox.minY) > s.verticalRange + 1) continue;
          TEXT.number(cell.level(), cell.x() + .5 - cx, cell.y() + 1.006 - cy, cell.z() + .5 - cz,
              scale, cosine, sine, cell.level() < s.greenFrom ? low : high);
        }
      } finally { TEXT.end(); }
    } finally {
      GL11.glPopMatrix();
      GL11.glPopAttrib();
    }
  }
}
