package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MusicProgressTest {
  @Test
  void startupPauseAndSeekDoNotAdvanceTheClock() {
    MusicProgress p = new MusicProgress();
    p.reset(0, 0);
    p.update(false, 1_000_000_000L);
    p.update(true, 2_000_000_000L);
    assertEquals(0, p.seconds());
    p.update(true, 3_000_000_000L);
    assertEquals(1, p.seconds());
    p.update(false, 4_000_000_000L);
    p.update(false, 9_000_000_000L);
    assertEquals(1, p.seconds());
    p.reset(90, 10_000_000_000L);
    p.update(true, 11_000_000_000L);
    assertEquals(90, p.seconds());
    p.update(true, 12_000_000_000L);
    assertEquals(91, p.seconds());
  }
}
