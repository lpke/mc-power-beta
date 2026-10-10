package local.luke.power.mechanics;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WaterContactTest {
    private static WaterContact.Water block(int x, int y, int z, int metadata) {
        return (bx, by, bz) -> bx == x && by == y && bz == z ? WaterContact.height(metadata, false) : 0;
    }

    @Test void sourceFlowingAndFallingWaterUseTheirActualHeight() {
        for (int meta = 0; meta < 16; meta++) {
            double height = WaterContact.height(meta, false);
            assertEquals(meta >= 8 ? 8 / 9.0 : (8 - meta) / 9.0, height);
            assertEquals(1, WaterContact.height(meta, true));
            var water = block(0, 0, 0, meta);
            assertTrue(WaterContact.touches(water, .2, 0, .2, .8, 1.8, .8), "water level " + meta);
            assertFalse(WaterContact.touches(water, .2, height, .2, .8, height + 1.8, .8));
        }
    }

    @Test void hitboxMustOverlapWaterInsteadOfOnlyBeingNearIt() {
        var water = block(-1, 0, -1, 7);
        assertTrue(WaterContact.touches(water, -.3, 0, -.3, .3, 1.8, .3));
        assertFalse(WaterContact.touches(water, 0, 0, 0, .6, 1.8, .6));
        assertFalse(WaterContact.touches(water, -.0005, 0, -.0005, .6, 1.8, .6));
        assertFalse(WaterContact.touches(water, -.8, 1, -.8, -.2, 2.8, -.2));
        assertFalse(WaterContact.touches(water, -.8, -2, -.8, -.2, -.2, -.2));
    }

    @Test void fastFallsResetWhenCrossingEvenTheThinnestFlow() {
        for (int meta = 0; meta < 16; meta++) {
            var water = block(0, 0, 0, meta);
            assertTrue(WaterContact.crosses(water, .5, 3, .5, .5, -1, .5));
            assertTrue(WaterContact.crosses(water, .5, -1, .5, .5, 3, .5));
            assertFalse(WaterContact.crosses(water, 1.1, 3, .5, 1.1, -1, .5));
            assertTrue(WaterContact.crosses(water, 0.0, 3, .5, -0.0, -1, .5));
        }
    }

    @Test void diagonalMovementChecksOnlyTheCellsAndFluidHeightsItCrosses() {
        assertTrue(WaterContact.crosses(block(-1, 0, -1, 0), -2, 2, -2, 0, 0, 0));
        assertFalse(WaterContact.crosses(block(-1, 0, -2, 0), -2, 2, -2, 0, 0, 0));
        var flow = block(0, 0, 0, 7);
        assertTrue(WaterContact.crosses(flow, -1, .05, .5, 2, .05, .5));
        assertFalse(WaterContact.crosses(flow, -1, .5, .5, 2, .5, .5));
        assertFalse(WaterContact.crosses(flow, .5, .2, .5, .5, 0, .5));
        assertFalse(WaterContact.crosses(flow, .5, .05, .5, .5, .05, .5));
    }
}
