package local.luke.power.camera.util;

/** Converts the renderer's rigid view transform into a world-space camera pose. */
public final class CameraPose {
    private CameraPose() {}

    public static CameraPosition fromView(float[] m, double x, double y, double z, float preferredYaw) {
        double horizontal = Math.hypot(m[2], m[10]);
        double pitch = Math.atan2(m[6], horizontal);
        double yaw, roll;
        if (horizontal < 1e-6) {
            yaw = Math.toRadians(preferredYaw);
            roll = Math.copySign(1, m[6]) * yaw - Math.atan2(m[1], m[0]);
        } else {
            yaw = Math.atan2(-m[2], m[10]);
            roll = Math.atan2(m[4], m[5]);
        }
        return new CameraPosition(
                x - (m[0] * (double)m[12] + m[1] * (double)m[13] + m[2] * (double)m[14]),
                y - (m[4] * (double)m[12] + m[5] * (double)m[13] + m[6] * (double)m[14]),
                z - (m[8] * (double)m[12] + m[9] * (double)m[13] + m[10] * (double)m[14]),
                (float)Math.toDegrees(pitch), (float)Math.toDegrees(yaw), (float)Math.toDegrees(roll));
    }
}
