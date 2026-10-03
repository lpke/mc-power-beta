package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.*;

class SettingTest {
  Setting number(Setting.Kind kind, double value) {
    return new Setting(
        "x",
        "test",
        "General",
        "Group",
        "Amount",
        "",
        kind,
        new JsonPrimitive(value),
        new JsonPrimitive(5),
        0,
        10,
        1,
        List.of(),
        false);
  }

  @Test
  void cyclesInBothDirectionsAndWraps() {
    Setting s = number(Setting.Kind.INTEGER, 0);
    for (int i = 0; i < 1000; i++) {
      JsonElement old = s.value;
      s.cycle(1);
      s.cycle(-1);
      assertEquals(old.getAsInt(), s.value.getAsInt());
      s.cycle(1);
    }
    s.value = new JsonPrimitive(0);
    s.cycle(-1);
    assertEquals(10, s.value.getAsInt());
  }

  @Test
  void invalidInputDoesNotChangeDraft() {
    Setting s = number(Setting.Kind.INTEGER, 5);
    for (String bad :
        List.of("NaN", "Infinity", "11", "-1", "1.5", "", "999999999999999999999999")) {
      assertThrows(RuntimeException.class, () -> s.parse(bad));
      assertEquals(5, s.value.getAsInt());
    }
  }

  @Test
  void decimalCyclesDoNotAccumulateFloatingPointError() {
    Setting s =
        new Setting(
            "x",
            "a",
            "a",
            "a",
            "a",
            "",
            Setting.Kind.DECIMAL,
            new JsonPrimitive(.3),
            new JsonPrimitive(.3),
            0,
            1,
            .1,
            List.of(),
            false);
    for (int i = 0; i < 10000; i++) {
      s.cycle(1);
      s.cycle(-1);
    }
    assertEquals("0.3", s.display());
  }

  @Test
  void draftResetAndDiscardAreIndependent() {
    Setting s = number(Setting.Kind.INTEGER, 3);
    s.cycle(1);
    assertTrue(s.changed());
    s.reset();
    assertEquals(5, s.value.getAsInt());
    assertEquals(3, s.original().getAsInt());
    s.accept();
    assertFalse(s.changed());
  }

  @Test
  void choicesRejectInvalidOrdinals() {
    Setting s =
        new Setting(
            "x",
            "a",
            "a",
            "a",
            "a",
            "",
            Setting.Kind.CHOICE,
            new JsonPrimitive(0),
            new JsonPrimitive(0),
            0,
            2,
            1,
            List.of("A", "B", "C"),
            false);
    s.cycle(-1);
    assertEquals("C", s.display());
    s.cycle(1);
    assertEquals("A", s.display());
    assertThrows(IllegalArgumentException.class, () -> s.parse("3"));
    assertThrows(IllegalArgumentException.class, () -> s.parse("-1"));
  }
}
