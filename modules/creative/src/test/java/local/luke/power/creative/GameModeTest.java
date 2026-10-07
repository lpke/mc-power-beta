package local.luke.power.creative;

import static org.junit.jupiter.api.Assertions.*;

import local.luke.power.creative.api.GameMode;
import local.luke.power.creative.config.Settings;
import org.junit.jupiter.api.Test;

class GameModeTest {
  @Test
  void namesAndCommandShortcuts() {
    assertEquals(GameMode.SURVIVAL, GameMode.parse("survival"));
    assertEquals(GameMode.CREATIVE, GameMode.parse("1"));
    assertEquals(GameMode.SPECTATOR, GameMode.parse("SPECTATOR"));
    assertEquals(GameMode.SPECTATOR, GameMode.parse("3"));
    assertThrows(IllegalArgumentException.class, () -> GameMode.parse("2"));
  }

  @Test
  void defaultSettingsAndCopiesAreIndependent() {
    Settings s = new Settings();
    s.validate();
    Settings copy = s.copy();
    copy.glide = 0;
    assertEquals(5, s.glide);
    assertEquals(50, s.blockReach);
    assertEquals(50, s.entityReach);
    copy.flightSpeed = Integer.MAX_VALUE;
    assertThrows(IllegalArgumentException.class, copy::validate);
  }
}
