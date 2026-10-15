package local.luke.power.camera.util;

/** Exact damped motion over a frame, independent of how that frame is subdivided. */
public final class CameraMotion {
    private CameraMotion() {}

    public record Step(double velocity, double distance) {}

    public static Step step(double velocity, double target, double drag, double seconds) {
        if (seconds <= 0) return new Step(velocity, 0);
        if (drag <= 0) return new Step(velocity, velocity * seconds);
        double response = -Math.expm1(-drag * seconds);
        return new Step(velocity + (target - velocity) * response,
                target * seconds + (velocity - target) * response / drag);
    }
}
