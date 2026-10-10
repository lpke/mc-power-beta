package local.luke.power.mechanics;

/** Modern Java fluid contact, without Beta's 0.4-block vertical hitbox inset. */
public final class WaterContact {
    private static final double INSET = 0.001;

    private WaterContact() {}

    public interface Water {
        /** Fluid height in this block, or zero when it is not water. */
        double height(int x, int y, int z);
    }

    public static double height(int metadata, boolean waterAbove) {
        return waterAbove ? 1.0 : (8 - (metadata >= 8 ? 0 : metadata)) / 9.0;
    }

    public static boolean touches(Water water, double minX, double minY, double minZ,
                                  double maxX, double maxY, double maxZ) {
        minX += INSET; minY += INSET; minZ += INSET;
        maxX -= INSET; maxY -= INSET; maxZ -= INSET;
        for (int x = floor(minX); x < Math.ceil(maxX); x++) {
            for (int y = floor(minY); y < Math.ceil(maxY); y++) {
                for (int z = floor(minZ); z < Math.ceil(maxZ); z++) {
                    double height = water.height(x, y, z);
                    if (height > 0 && y + height >= minY) return true;
                }
            }
        }
        return false;
    }

    /** Trace the feet through water when a tick can cross an entire fluid block. */
    public static boolean crosses(Water water, double fromX, double fromY, double fromZ,
                                  double toX, double toY, double toZ) {
        double dx = toX - fromX, dy = toY - fromY, dz = toZ - fromZ;
        if (dx * dx + dy * dy + dz * dz < 1.0) return false;
        int x = floor(fromX), y = floor(fromY), z = floor(fromZ);
        int stepX = direction(dx), stepY = direction(dy), stepZ = direction(dz);
        double nextX = boundary(fromX, dx, x, stepX);
        double nextY = boundary(fromY, dy, y, stepY);
        double nextZ = boundary(fromZ, dz, z, stepZ);
        double entered = 0;
        while (entered <= 1) {
            double exited = Math.min(1, Math.min(nextX, Math.min(nextY, nextZ)));
            double height = water.height(x, y, z);
            if (height > 0 && Math.min(fromY + dy * entered, fromY + dy * exited) <= y + height) {
                return true;
            }
            if (exited == 1) break;
            // Advance all tied axes so an exact corner does not visit unrelated cells.
            double next = Math.min(nextX, Math.min(nextY, nextZ));
            if (nextX == next) { x += stepX; nextX += 1 / Math.abs(dx); }
            if (nextY == next) { y += stepY; nextY += 1 / Math.abs(dy); }
            if (nextZ == next) { z += stepZ; nextZ += 1 / Math.abs(dz); }
            entered = next;
        }
        return false;
    }

    private static int floor(double value) { return (int)Math.floor(value); }

    private static int direction(double value) { return value == 0 ? 0 : value > 0 ? 1 : -1; }

    private static double boundary(double from, double delta, int block, int step) {
        return step == 0 ? Double.POSITIVE_INFINITY : ((step > 0 ? block + 1 : block) - from) / delta;
    }
}
