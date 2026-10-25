package local.luke.power.controls.tweaks.photomode;

/** Existing renderer-grid coordinates. Reading these never requests terrain. */
public interface PhotoTerrainBounds extends local.luke.power.input.RenderedTerrainBounds {
    double powerPhoto$centerX();
    double powerPhoto$centerZ();
    int powerPhoto$spanX();
    int powerPhoto$spanZ();
    default double powerTerrain$minX() { return powerPhoto$centerX() - powerPhoto$spanX() / 2d; }
    default double powerTerrain$minZ() { return powerPhoto$centerZ() - powerPhoto$spanZ() / 2d; }
    default double powerTerrain$maxX() { return powerPhoto$centerX() + powerPhoto$spanX() / 2d; }
    default double powerTerrain$maxZ() { return powerPhoto$centerZ() + powerPhoto$spanZ() / 2d; }
    local.luke.power.input.PhotoProjection.InitialView powerPhoto$initialView(
        int physicalWidth, int physicalHeight, double tiltDegrees, double yawDegrees);
}
