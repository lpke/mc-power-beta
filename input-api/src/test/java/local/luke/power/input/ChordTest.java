package local.luke.power.input;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class ChordTest {
  @Test void nativeCodesAndMouseButtonsRoundTrip() {
    for (int key : new int[]{-100, -99, -96, 1, 29, 30, 57, 255})
      for (int mask = 0; mask < 8; mask++) {
        var chord = new Chord(key, mask);
        assertEquals(chord, Chord.decode(chord.encoded()));
      }
  }
  @Test void chordsRequireEveryModifierButAllowEitherSide() {
    var chord = new Chord(35, Chord.CTRL | Chord.SHIFT);
    assertFalse(chord.matches(35, Chord.CTRL));
    assertFalse(chord.matches(36, 7));
    assertTrue(chord.matches(35, 3));
    assertTrue(chord.matches(35, 7));
    assertEquals(Chord.modifier(29), Chord.modifier(157));
    assertEquals(Chord.modifier(42), Chord.modifier(54));
    assertEquals(Chord.modifier(56), Chord.modifier(184));
  }
  @Test void onlyStandardModifiersAreRecognized() {
    assertEquals(0, Chord.modifier(35));
    assertEquals(0, Chord.modifier(219));
    assertThrows(IllegalArgumentException.class, () -> Chord.decode(8 << 16));
    assertThrows(IllegalArgumentException.class, () -> new Chord(0, 1));
    assertFalse(new Chord(0, 0).matches(0, 7));
  }
}
