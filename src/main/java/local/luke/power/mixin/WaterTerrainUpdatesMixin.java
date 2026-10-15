package local.luke.power.mixin;

import java.util.List;
import local.luke.power.render.WaterTerrainUpdates;
import local.luke.power.render.WaterTerrain;
import local.luke.power.render.WaterTerrainSection;
import net.minecraft.class_66;
import net.minecraft.client.render.WorldRenderer;
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

  @Override public void power$prepareWaterColors() {
    if (field_1809 == null) return;
    // First opt-in also covers deep water whose native skylight is entirely zero.
    // Preserve the renderer's normal update budget and avoid duplicate queue entries.
    for (class_66 section : field_1809) if (!section.field_249) {
      section.method_305();
      field_1807.add(section);
    }
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
