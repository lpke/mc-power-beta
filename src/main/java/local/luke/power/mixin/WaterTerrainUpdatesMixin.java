package local.luke.power.mixin;

import java.util.List;
import local.luke.power.render.WaterTerrainUpdates;
import net.minecraft.class_66;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WorldRenderer.class)
public class WaterTerrainUpdatesMixin implements WaterTerrainUpdates {
  @Shadow private class_66[] field_1809;
  @Shadow private List<class_66> field_1807;

  @Override public void power$prepareWaterColors() {
    if (field_1809 == null) return;
    // First opt-in also covers deep water whose native skylight is entirely zero.
    // Preserve the renderer's normal update budget and avoid duplicate queue entries.
    for (class_66 section : field_1809) if (!section.field_249) {
      section.method_305();
      field_1807.add(section);
    }
  }
}
