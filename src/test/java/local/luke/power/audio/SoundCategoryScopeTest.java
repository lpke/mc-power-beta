package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SoundCategoryScopeTest {
  @Test void entityCategoryOverridesTheSharedSampleCategoryAndRestoresNestedCalls() {
    var settings = new AudioSettings();
    settings.categories.put("players", 80); settings.categories.put("passive", 20); settings.categories.put("ambient", 40);
    assertNull(SoundCategoryScope.current());
    SoundCategoryScope.run("passive", () -> {
      assertEquals(.2, settings.gain("power_beta:entity.generic.splash", false, SoundCategoryScope.current()));
      assertThrows(IllegalStateException.class, () -> SoundCategoryScope.run("ambient", () -> {
        assertEquals(.4, settings.gain("power_beta:entity.generic.splash", false, SoundCategoryScope.current()));
        throw new IllegalStateException();
      }));
      assertEquals("passive", SoundCategoryScope.current());
    });
    assertNull(SoundCategoryScope.current());
    assertEquals(.8, settings.gain("power_beta:entity.player.splash", false));
  }
}
