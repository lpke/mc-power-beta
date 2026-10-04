package local.luke.power.video;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class VideoSettingsTest {
  @Test
  void betaDefaultsAndCustomCycleAreBounded() {
    VideoSettings s = new VideoSettings();
    assertEquals(List.of(12, 8, 4, 2), s.fogCycle);
    s.validate();
    for (List<Integer> invalid :
        List.of(List.<Integer>of(), List.of(1), List.of(33), List.of(8, 8))) {
      s.fogCycle = invalid;
      assertThrows(IllegalArgumentException.class, s::validate);
    }
    s.fogCycle = List.of(32, 24, 16, 8, 2);
    assertDoesNotThrow(s::validate);
  }
}
