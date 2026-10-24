package local.luke.power.controls.tweaks.photomode;

/** Existing renderer-grid coordinates. Reading these never requests terrain. */
public interface PhotoTerrainBounds {
    double powerPhoto$centerX();
    double powerPhoto$centerZ();
    int powerPhoto$spanX();
    int powerPhoto$spanZ();
    local.luke.power.input.PhotoProjection.InitialView powerPhoto$initialView(
        int physicalWidth, int physicalHeight, double tiltDegrees, double yawDegrees);
}
