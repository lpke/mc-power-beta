package local.luke.power.input;

/** Existing renderer-grid bounds in block coordinates. Maximum edges are exclusive. */
public interface RenderedTerrainBounds {
  double powerTerrain$minX();
  double powerTerrain$minZ();
  double powerTerrain$maxX();
  double powerTerrain$maxZ();
}
