package local.luke.power.camera.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CameraMotionTest {
    @Test void irregularFramesMatchOneContinuousMovement() {
        for (double target : new double[]{12, -12, 24, -24, 0}) {
            double velocity = 3, distance = 0, time = 0;
            for (double dt : new double[]{.005, .04, .01, .08, .016, .002, .1}) {
                var next = CameraMotion.step(velocity, target, 10, dt);
                velocity = next.velocity(); distance += next.distance(); time += dt;
            }
            var continuous = CameraMotion.step(3, target, 10, time);
            assertEquals(continuous.velocity(), velocity, 1e-12);
            assertEquals(continuous.distance(), distance, 1e-12);
        }
    }
    @Test void ascentAndDescentAreSymmetricAndNeverOvershoot() {
        for (double drag : new double[]{.1, 4, 10, 100}) for (double dt : new double[]{.001, .016, .05, .1}) {
            var up = CameraMotion.step(0, 12, drag, dt);
            var down = CameraMotion.step(0, -12, drag, dt);
            assertEquals(-up.velocity(), down.velocity(), 1e-12);
            assertEquals(-up.distance(), down.distance(), 1e-12);
            assertTrue(up.velocity() >= 0 && up.velocity() <= 12);
            assertTrue(up.distance() >= 0 && up.distance() <= 12 * dt);
        }
    }
    @Test void releasingOrOpposingInputDeceleratesWithoutReversing() {
        var released = CameraMotion.step(12, 0, 10, .1);
        assertTrue(released.velocity() > 0 && released.velocity() < 12);
        assertTrue(released.distance() > 0 && released.distance() < 1.2);
        assertEquals(new CameraMotion.Step(0, 0), CameraMotion.step(0, 0, 10, .1));
        assertEquals(new CameraMotion.Step(12, 0), CameraMotion.step(12, 0, 10, 0));
    }
}
