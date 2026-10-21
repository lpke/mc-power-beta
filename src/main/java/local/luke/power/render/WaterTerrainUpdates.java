package local.luke.power.render;

import net.minecraft.world.World;

public interface WaterTerrainUpdates {
  void power$prepareWaterColors();
  /** Visits at most 27 nearby render sections, and queues at most one native rebuild. */
  boolean power$prepareHeldLight(World world, double x, double y, double z);
  /** Completes only existing, loaded influence meshes before synchronous image output. */
  boolean power$prepareHeldLightCapture(World world, double x, double y, double z);
}
