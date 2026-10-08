package local.luke.power.worldedit.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CalculatorTest {
  @Test void arithmeticPrecedencePowersAndFunctions() {
    assertEquals(14, Calculator.evaluate("2 + 3 * 4"));
    assertEquals(512, Calculator.evaluate("2^3^2"));
    assertEquals(-4, Calculator.evaluate("-2^2"));
    assertEquals(.25, Calculator.evaluate("2^-2"));
    assertEquals(18, Calculator.evaluate("max(4, 2*9)"));
    assertEquals(7, Calculator.evaluate("sqrt(49)"));
    assertEquals(1, Calculator.evaluate("sin(pi/2)"), .00001);
    assertEquals(100.25, Calculator.evaluate("1e2 + .25"));
  }
  @Test void errorsCannotHangOrReturnInvalidNumbers() {
    for (String input : new String[]{"", "1/0", "sqrt(-1)", "9^99999", "foo(1)", "1+", "1 2", "max(1,2,3)", "(".repeat(100) + "1" + ")".repeat(100), "-".repeat(100) + "1"})
      assertThrows(IllegalArgumentException.class, () -> Calculator.evaluate(input), input);
  }
}
