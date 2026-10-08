package local.luke.power.capture;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IsometricLayoutTest {
    @Test void boundsFollowRendererGridAndStayWithinCaptureLimit() {
        assertEquals(80, IsometricLayout.blockSpan(5));
        assertEquals(272, IsometricLayout.blockSpan(17));
        assertEquals(416, IsometricLayout.blockSpan(65));
        assertThrows(IllegalArgumentException.class, () -> IsometricLayout.blockSpan(0));
    }
    @Test void alignsPositiveAndNegativeCoordinatesToTheSameChunkCentre() {
        assertEquals(-8, IsometricLayout.chunkOffset(0));
        assertEquals(-8, IsometricLayout.chunkOffset(-16));
        assertEquals(7.5, IsometricLayout.chunkOffset(-.5));
        assertEquals(-7.5, IsometricLayout.chunkOffset(.5));
        assertEquals(0, IsometricLayout.chunkOffset(8));
    }
}
