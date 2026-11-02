package local.luke.power.render;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WaterTerrainPreparationTest {
  @Test void disabledRetainsReadinessAndUnchangedReenableDoesNotRefresh(){
    var state=new WaterTerrainPreparation();var world=new Object();
    assertFalse(state.update(world,true));assertTrue(state.active());
    assertFalse(state.update(world,true));assertFalse(state.update(world,false));assertFalse(state.active());
    assertFalse(state.update(world,true));assertTrue(state.active());
  }
  @Test void disabledChangesRequestExactlyOneCatchupAndRespectWorldOwner(){
    var state=new WaterTerrainPreparation();var world=new Object();state.update(world,true);state.update(world,false);
    state.invalidate(new Object());assertFalse(state.update(world,true));
    state.update(world,false);state.invalidate(world);state.invalidate(world);
    assertTrue(state.update(world,true));assertFalse(state.update(world,true));
    state.invalidate(world);assertFalse(state.update(world,true));
  }
  @Test void firstOptinAndWorldReplacementKeepNativeInitialBuilds(){
    var state=new WaterTerrainPreparation();var world=new Object();assertFalse(state.update(world,false));
    assertTrue(state.update(world,true));assertFalse(state.update(new Object(),true));
    state.reset();assertFalse(state.active());assertFalse(state.update(world,true));
    assertFalse(state.update(null,true));assertFalse(state.active());
  }
}
