package local.luke.power.mixin;

import java.util.List;
import local.luke.power.render.WaterTerrainUpdates;
import local.luke.power.render.WaterTerrain;
import local.luke.power.render.WaterTerrainSection;
import local.luke.power.render.HeldLighting;
import local.luke.power.render.HeldLightMath;
import local.luke.power.render.CullingTerrain;
import net.minecraft.class_66;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WaterTerrainUpdatesMixin implements WaterTerrainUpdates {
  @Shadow private class_66[] field_1809;
  @Shadow private List<class_66> field_1807;
  @Shadow private int field_1810, field_1811, field_1812;
  @Shadow private World field_1805;

  @Override public void power$prepareWaterColors() {
    if (field_1809 == null) return;
    // First opt-in also covers deep water whose native skylight is entirely zero.
    // Preserve the renderer's normal update budget and avoid duplicate queue entries.
    for (class_66 section : field_1809) if (!section.field_249) {
      section.method_305();
      field_1807.add(section);
    }
  }

  @Override public boolean power$prepareHeldLight(World world, double x, double y, double z) {
    return power$prepareHeldLight(world, x, y, z, false);
  }

  @Override public boolean power$prepareHeldLightCapture(World world, double x, double y, double z) {
    return power$prepareHeldLight(world, x, y, z, true);
  }

  @Unique private boolean power$prepareHeldLight(World world, double x, double y, double z, boolean capture) {
    if (world == null || field_1805 != world || field_1809 == null || field_1807 == null
        || field_1810 <= 0 || field_1811 <= 0 || field_1812 <= 0) return false;
    double radius = HeldLightMath.PREPARE_RADIUS, nearest = Double.POSITIVE_INFINITY;
    int minX = Math.floorDiv((int)Math.floor(x - radius), 16), maxX = Math.floorDiv((int)Math.floor(x + radius), 16);
    int minY = Math.max(0, Math.floorDiv((int)Math.floor(y - radius), 16));
    int maxY = Math.min(Math.min(7, field_1811 - 1), Math.floorDiv((int)Math.floor(y + radius), 16));
    int minZ = Math.floorDiv((int)Math.floor(z - radius), 16), maxZ = Math.floorDiv((int)Math.floor(z + radius), 16);
    boolean ready = true, covered = false; class_66 request = null;
    // The 31.5-block diameter spans at most three sections per axis. Ordinary
    // frames use only the renderer grid; captures also verify loaded neighbors.
    for (int cz = minZ; cz <= maxZ; cz++) for (int cy = minY; cy <= maxY; cy++) for (int cx = minX; cx <= maxX; cx++) {
      double distance = HeldLightMath.sectionDistanceSquared(x, y, z, cx * 16, cy * 16, cz * 16);
      if (distance >= radius * radius) continue;
      boolean required = distance < HeldLightMath.RADIUS * HeldLightMath.RADIUS;
      int index = (Math.floorMod(cz, field_1812) * field_1811 + cy) * field_1810 + Math.floorMod(cx, field_1810);
      if (index < 0 || index >= field_1809.length) { if (required) ready = false; continue; }
      class_66 section = field_1809[index];
      if (section == null || section.field_229 != world || section.field_231 != cx * 16
          || section.field_232 != cy * 16 || section.field_233 != cz * 16) {
        if (required) ready = false;
        continue;
      }
      if (required) covered = true;
      var colors = (WaterTerrainSection)section;
      if (capture && required && (section.field_249
          || !colors.power$heldLightReady() && !colors.power$heldLightRejected() && !(section.field_244[0] && section.field_244[1]))) {
        // Native mesh construction samples the surrounding one-block border. Prove
        // all nine chunks already exist before entering that native constructor.
        if (!power$heldNeighborhoodLoaded(world, cx, cz)) { ready = false; continue; }
        if (!section.field_249) { section.method_305(); field_1807.add(section); HeldLighting.requested(); }
        section.method_296();
        section.field_249 = false;
        field_1807.remove(section);
      }
      if (colors.power$heldLightReady() || colors.power$heldLightRejected() || !section.field_249 && section.field_244[0] && section.field_244[1]) continue;
      if (required) ready = false;
      if (!capture && !section.field_249 && distance < nearest) { nearest = distance; request = section; }
    }
    if (request != null) { request.method_305(); field_1807.add(request); HeldLighting.requested(); }
    return ready && covered;
  }

  @Unique private static boolean power$heldNeighborhoodLoaded(World world, int x, int z) {
    for (int cx = x - 1; cx <= x + 1; cx++) for (int cz = z - 1; cz <= z + 1; cz++) {
      var chunk = CullingTerrain.chunk(world, cx, cz);
      if (chunk == null || !chunk.method_858(cx, cz) || !world.method_239(cx * 16, 64, cz * 16)) return false;
    }
    return true;
  }

  @Inject(method = "method_1149", at = @At("RETURN"))
  private void power$column(int x, int y, int z, CallbackInfo ci) { power$columns(x,y,z,x,y,z); }

  @Inject(method = "method_1150", at = @At("RETURN"))
  private void power$columnArea(int x, int y, int z, int maxX, int maxY, int maxZ, CallbackInfo ci) {
    power$columns(x,y,z,maxX,maxY,maxZ);
  }

  @Unique private void power$columns(int x, int y, int z, int maxX, int maxY, int maxZ) {
    if (!WaterTerrain.prepared() || field_1809 == null || field_1810 == 0 || field_1811 == 0 || field_1812 == 0) return;
    int minChunkX=Math.floorDiv(x-1,16), maxChunkX=Math.floorDiv(maxX+1,16);
    int minChunkZ=Math.floorDiv(z-1,16), maxChunkZ=Math.floorDiv(maxZ+1,16);
    int columnsX=Math.min(field_1810,maxChunkX-minChunkX+1), columnsZ=Math.min(field_1812,maxChunkZ-minChunkZ+1);
    int top=Math.min(field_1811-1,Math.floorDiv(maxY+1,16));
    // At most the existing render grid, usually one column of eight sections.
    // No chunk queries or loads. Only meshes that sampled a water column are queued.
    for(int dx=0;dx<columnsX;dx++)for(int dz=0;dz<columnsZ;dz++) {
      int cx=Math.floorMod(minChunkX+dx,field_1810), cz=Math.floorMod(minChunkZ+dz,field_1812);
      int base=cz*field_1811*field_1810+cx;
      for(int cy=0;cy<=top;cy++) {
        class_66 section=field_1809[base+cy*field_1810];
        if(section.field_249 || !((WaterTerrainSection)section).power$waterColumn()
            || (section.field_231>>4)<minChunkX || (section.field_231>>4)>maxChunkX
            || (section.field_233>>4)<minChunkZ || (section.field_233>>4)>maxChunkZ) continue;
        section.method_305();field_1807.add(section);
      }
    }
  }
}
