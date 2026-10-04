package local.luke.power.light;

import net.minecraft.block.Block;
import net.minecraft.class_56;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public final class LightOverlay {
  private static final LightCache CACHE = new LightCache();
  private static final String[] NUMBERS = new String[16];
  private static World world;
  static { for (int i = 0; i < NUMBERS.length; i++) NUMBERS[i] = Integer.toString(i); }

  public static void tick(Minecraft mc) {
    LightSettings settings = LightConfig.current();
    if (!settings.enabled || mc.world == null || mc.player == null) {
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
    if (!s.enabled || mc.world == null || mc.world != world || mc.player == null
        || mc.options.hideHud || CACHE.cells().isEmpty()) return;
    var camera = mc.field_2807 == null ? mc.player : mc.field_2807;
    double cx = camera.field_1637 + (camera.x - camera.field_1637) * delta;
    double cy = camera.field_1638 + (camera.y - camera.field_1638) * delta;
    double cz = camera.field_1639 + (camera.z - camera.field_1639) * delta;
    int low = LightSettings.rgb(s.lowColor), high = LightSettings.rgb(s.highColor);
    // Rotate in 90-degree steps so the text reads toward the viewing direction.
    float angle = Math.round(camera.yaw / 90f) * 90f;
    double scale = s.textSize / 12;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glPushMatrix();
    try {
      GL11.glTranslated(-cx, -cy, -cz);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_FOG);
      GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glDepthMask(false);
      for (LightCache.Cell cell : CACHE.cells()) {
        double dx = cell.x() + .5 - camera.x, dz = cell.z() + .5 - camera.z;
        if (dx * dx + dz * dz > s.radius * s.radius
            || Math.abs(cell.y() + 1 - camera.boundingBox.minY) > s.verticalRange + 1) continue;
        GL11.glPushMatrix();
        try {
          GL11.glTranslated(cell.x() + .5, cell.y() + 1.006, cell.z() + .5);
          GL11.glRotatef(-angle, 0, 1, 0);
          GL11.glRotatef(90, 1, 0, 0);
          GL11.glScaled(-scale, -scale, scale);
          String number = NUMBERS[cell.level()];
          mc.textRenderer.draw(number, -mc.textRenderer.getWidth(number) / 2, -4,
              cell.level() < s.greenFrom ? low : high);
        } finally { GL11.glPopMatrix(); }
      }
    } finally {
      GL11.glPopMatrix();
      GL11.glPopAttrib();
    }
  }
}
