package local.luke.power.input;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SprintViewTest {
  @AfterEach void resetPublishedStates() {
    SprintView.clearLanding();
    for (var kind : SprintView.Kind.values()) SprintView.publish(kind, false, 1);
  }

  @Test void landingIsConsumedOnceAndNeverFollowsAnotherPlayerOrWorld() {
    Object player = new Object(), world = new Object();
    SprintView.landed(player, world);
    assertTrue(SprintView.consumeLanding(player, world));
    assertFalse(SprintView.consumeLanding(player, world));
    SprintView.landed(player, world);
    assertFalse(SprintView.consumeLanding(player, new Object()));
    assertFalse(SprintView.consumeLanding(player, world));
    SprintView.landed(player, world);
    assertFalse(SprintView.consumeLanding(new Object(), world));
    SprintView.landed(player, world); SprintView.clearLanding();
    assertFalse(SprintView.consumeLanding(player, world));
  }

  @Test void activeSprintAtNormalSpeedRemainsDistinctFromInactive() {
    SprintView.publish(SprintView.Kind.GROUND, true, 1);
    assertTrue(SprintView.active(SprintView.Kind.GROUND));
    assertEquals(1, SprintView.ratio(SprintView.Kind.GROUND));
    SprintView.publish(SprintView.Kind.GROUND, false, 1);
    assertFalse(SprintView.active(SprintView.Kind.GROUND));
    assertEquals(1, SprintView.ratio(SprintView.Kind.GROUND));
  }

  @Test void movementModesPublishIndependentlyAndInactiveRatiosReset() {
    SprintView.publish(SprintView.Kind.GROUND, true, 1);
    SprintView.publish(SprintView.Kind.CREATIVE, true, 2);
    SprintView.publish(SprintView.Kind.SPECTATOR, false, 4);
    assertTrue(SprintView.active(SprintView.Kind.GROUND));
    assertTrue(SprintView.active(SprintView.Kind.CREATIVE));
    assertFalse(SprintView.active(SprintView.Kind.SPECTATOR));
    assertEquals(2, SprintView.ratio(SprintView.Kind.CREATIVE));
    assertEquals(1, SprintView.ratio(SprintView.Kind.SPECTATOR));
    SprintView.publish(SprintView.Kind.GROUND, false, 3);
    assertFalse(SprintView.active(SprintView.Kind.GROUND));
    assertTrue(SprintView.active(SprintView.Kind.CREATIVE));
  }
}
