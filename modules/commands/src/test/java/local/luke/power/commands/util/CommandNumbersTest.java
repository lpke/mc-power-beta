package local.luke.power.commands.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CommandNumbersTest {
  @Test
  void absoluteAndRelativeCoordinatesUseFeetAndCenterWholeBlockXZ() {
    assertArrayEquals(
        new double[] {1.5, 64, -1.5},
        CommandNumbers.position(10, 60, 20, 0, 0, new String[] {"1", "64", "-2"}, 0),
        1e-9);
    assertArrayEquals(
        new double[] {10, 61.5, 18},
        CommandNumbers.position(10, 60, 20, 0, 0, new String[] {"~", "~1.5", "~-2"}, 0),
        1e-9);
    assertArrayEquals(
        new double[] {1, 64, 2},
        CommandNumbers.position(0, 0, 0, 0, 0, new String[] {"1.0", "64", "2.0"}, 0),
        1e-9);
  }

  @Test
  void localCoordinatesRotateWithYawAndPitch() {
    assertArrayEquals(
        new double[] {12, 63, 24},
        CommandNumbers.position(10, 60, 20, 0, 0, new String[] {"^2", "^3", "^4"}, 0),
        1e-9);
    assertArrayEquals(
        new double[] {6, 63, 22},
        CommandNumbers.position(10, 60, 20, 90, 0, new String[] {"^2", "^3", "^4"}, 0),
        1e-9);
    assertArrayEquals(
        new double[] {10, 56, 20},
        CommandNumbers.position(10, 60, 20, 0, 90, new String[] {"^", "^", "^4"}, 0),
        1e-9);
  }

  @Test
  void unsafeIncompleteAndMixedCoordinatesFail() {
    for (String[] values :
        new String[][] {
          {"~"},
          {"^", "~", "^"},
          {"NaN", "0", "0"},
          {"Infinity", "0", "0"},
          {"3e7", "0", "0"},
          {"0", "4097", "0"},
          {"", "0", "0"},
          {"~~1", "0", "0"}
        })
      assertThrows(
          IllegalArgumentException.class, () -> CommandNumbers.position(0, 0, 0, 0, 0, values, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> CommandNumbers.position(1e300, 0, 0, 0, 0, new String[] {"~1e300", "~", "~"}, 0));
  }

  @Test
  void durationsRespectUnitsAndRejectOverflow() {
    assertEquals(20, CommandNumbers.ticks("1s", 1));
    assertEquals(24000, CommandNumbers.ticks("1d", 1));
    assertEquals(1, CommandNumbers.ticks("1t", 20));
    assertEquals(20, CommandNumbers.ticks("1", 20));
    assertEquals(30, CommandNumbers.ticks("1.5s", 1));
    for (String value : new String[] {"-1", "NaN", "Infinity", "1e100", "1x", ""})
      assertThrows(IllegalArgumentException.class, () -> CommandNumbers.ticks(value, 1));
  }

  @Test
  void distanceRangesValidateBothEnds() {
    assertArrayEquals(new double[] {0, 5}, EntityTargets.range("..5"));
    assertArrayEquals(new double[] {2, 5}, EntityTargets.range("2..5"));
    for (String bad : new String[] {"5..2", "-1", "NaN", "1..2..3"})
      assertThrows(IllegalArgumentException.class, () -> EntityTargets.range(bad));
  }
}
