package local.luke.power.creative;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CreativeBreakCooldownTest {
  @Test void rapidPressRestartsOnlyItsOwnSuccessfulBreakDelay() {
    var delay = new CreativeBreakCooldown();
    Object world = new Object(), player = new Object();
    assertFalse(delay.restart(world, player, 10, 5));
    delay.broken(world, player, 10);
    assertTrue(delay.restart(world, player, 10, 5));
    assertTrue(delay.restart(world, player, 14, 1));
    assertFalse(delay.restart(world, player, 15, 5));
    assertFalse(delay.restart(world, player, 9, 5));
    assertFalse(delay.restart(world, player, 11, 10));
    assertFalse(delay.restart(world, player, 11, 0));
    assertFalse(delay.restart(new Object(), player, 11, 4));
    assertFalse(delay.restart(world, new Object(), 11, 4));
    delay.clear();
    assertFalse(delay.restart(world, player, 11, 4));
  }

  @Test void contextRetainsOnlyAnUnexpiredDelayForTheCurrentOwners() {
    var delay = new CreativeBreakCooldown();
    Object world = new Object(), player = new Object();
    delay.broken(world, player, 10);
    delay.context(world, player, 14);
    assertTrue(delay.restart(world, player, 14, 1));
    delay.context(world, player, 15);
    assertFalse(delay.restart(world, player, 10, 5));

    delay.broken(world, player, 10);
    delay.context(new Object(), player, 11);
    assertFalse(delay.restart(world, player, 11, 4));
    delay.broken(world, player, 10);
    delay.context(world, new Object(), 11);
    assertFalse(delay.restart(world, player, 11, 4));
    delay.broken(world, player, 10);
    delay.context(null, null, 11);
    assertFalse(delay.restart(world, player, 11, 4));
    delay.broken(world, player, 10);
    delay.context(world, player, 9);
    assertFalse(delay.restart(world, player, 10, 5));
  }
}
