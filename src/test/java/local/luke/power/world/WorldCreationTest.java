package local.luke.power.world;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WorldCreationTest {
  @Test void creationChoiceIsScopedEvenOnFailureAndNestedCalls() {
    assertEquals(2, WorldCreation.difficulty());
    for (int value = 0; value < 4; value++) {
      final int chosen = value;
      WorldCreation.withDifficulty(chosen, () -> {
        assertEquals(chosen, WorldCreation.difficulty());
        assertThrows(IllegalStateException.class, () -> WorldCreation.withDifficulty(3, () -> { throw new IllegalStateException(); }));
        assertEquals(chosen, WorldCreation.difficulty());
      });
      assertEquals(2, WorldCreation.difficulty());
    }
    assertThrows(IllegalArgumentException.class, () -> WorldCreation.withDifficulty(-1, () -> fail("invalid creation")));
    assertThrows(IllegalArgumentException.class, () -> WorldCreation.withDifficulty(4, () -> fail("invalid creation")));
    assertEquals(2, WorldCreation.difficulty());
  }
}
