package local.luke.power.render;

import local.luke.power.input.RenderedView;
import local.luke.power.visual.VisualConfig;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.class_385;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** Camera-only material queries. Never moves the player or requests terrain. */
public final class CameraEnvironment {
  private static int darkness = 100;
  private CameraEnvironment() {}

  public static boolean inFluid(Entity camera, Material material) {
    if (camera == null) return false;
    if (RenderedView.valid(camera.world, camera))
      return fluid(camera.world, RenderedView.x(), RenderedView.y(), RenderedView.z()) == material;
    // Also serves standalone rendering and first-person hand/capture entry points.
    return fluid(camera.world, camera.x, camera.y + camera.method_1378(), camera.z) == material;
  }

  public static Material fluid(World world, double x, double y, double z) {
    if (world == null || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
        || y < 0 || y >= 128 || Math.abs(x) > 32000000 || Math.abs(z) > 32000000) return null;
    int bx=(int)Math.floor(x), by=(int)Math.floor(y), bz=(int)Math.floor(z);
    var chunk=CullingTerrain.chunk(world, bx >> 4, bz >> 4);
    if (chunk == null || !chunk.method_858(bx >> 4, bz >> 4)) return null;
    int id=chunk.method_859(bx & 15, by, bz & 15);
    Block block=Block.BLOCKS[id];
    if (block == null || (block.field_1900 != Material.WATER && block.field_1900 != Material.LAVA)) return null;
    float height=class_385.method_1218(chunk.method_875(bx & 15, by, bz & 15)) - .11111111F;
    return y < (float)(by + 1) - height ? block.field_1900 : null;
  }

  public static int darkness() { return darkness; }

  public static void updateLighting(Minecraft mc) {
    var settings=VisualConfig.current();
    int next=inFluid(mc.field_2807, Material.WATER) ? settings.underwaterDarkness : settings.underwaterDarknessOutside;
    if (next != darkness) { darkness=next; WaterLighting.clear(); }
    WaterTerrain.prepare(mc);
  }
}
