package local.luke.power.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExtendedKeysTest {
  @Test void extendedKeysStayDistinctAndRoundTripWithModifiers() {
    var seen = new java.util.HashSet<Integer>();
    for (int f=13;f<=24;f++) {
      int key=ExtendedKeys.linux(0xffbe+f-1);
      assertTrue(key>0 && key<256);
      assertTrue(seen.add(key));
      assertEquals(new Chord(key,7),Chord.decode(new Chord(key,7).encoded()));
      if(f>=20) { assertEquals("F"+f,ExtendedKeys.name(key)); assertEquals(key,ExtendedKeys.code("F"+f)); }
    }
    assertEquals(43,ExtendedKeys.linux('\\'));
    assertEquals(43,ExtendedKeys.linux('|'));
    assertEquals(0,ExtendedKeys.linux('a'));
    assertEquals(0,ExtendedKeys.code(null));
    assertNull(ExtendedKeys.name(0));
  }
  @Test void inventoryMovementExcludesShiftMouseAndUnbound() {
    for(int code:new int[]{0,-100,-99,42,54}) assertFalse(MovementScreens.movementKey(code));
    for(int code:new int[]{17,30,31,32,57,114}) assertTrue(MovementScreens.movementKey(code));
  }
}
