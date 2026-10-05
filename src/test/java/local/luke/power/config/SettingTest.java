package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.*;

class SettingTest {
  @Test
  void largeMusicSelectionsCanBeSavedWithoutRelaxingOtherListLimits() {
    JsonArray tracks = new JsonArray();
    for (int i = 0; i < 4096; i++) tracks.add("music:custom/" + "a".repeat(64) + i + ".ogg");
    assertTrue(tracks.toString().length() > 16384);
    for (String id : List.of("audio.exclusions", "audio.favourites", "audio.presets", "other")) {
      Setting s = new Setting(id, "audio", "Audio", "Music data", "Music selection", "",
          Setting.Kind.LIST, new JsonArray(), new JsonArray(), 0, 0, 1, List.of(), false);
      if (id.equals("other")) assertThrows(IllegalArgumentException.class, () -> s.validate(tracks));
      else assertDoesNotThrow(() -> s.validate(tracks));
      JsonArray oversized = new JsonArray();
      oversized.add("x".repeat(1048576));
      assertThrows(IllegalArgumentException.class, () -> s.parse(oversized.toString()));
      assertEquals(new JsonArray(), s.value);
    }
  }

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

  @org.junit.jupiter.api.Test void slidersClampAndReachBothEndpoints() {
    Setting s = new Setting("test", "test", "Video", "Test", "Test", "", Setting.Kind.INTEGER,
        new com.google.gson.JsonPrimitive(2), new com.google.gson.JsonPrimitive(2), 2, 32, 1, java.util.List.of(), false);
    s.slide(-1); org.junit.jupiter.api.Assertions.assertEquals(2, s.value.getAsInt());
    s.slide(.5); org.junit.jupiter.api.Assertions.assertEquals(17, s.value.getAsInt());
    s.slide(2); org.junit.jupiter.api.Assertions.assertEquals(32, s.value.getAsInt());
  }
  @org.junit.jupiter.api.Test void duplicateAndEmptyHelpStaysHidden() {
    org.junit.jupiter.api.Assertions.assertEquals("", Tooltips.description("unknown", "Test", "Test"));
    org.junit.jupiter.api.Assertions.assertEquals("", Tooltips.description("unknown", "Test", "Test."));
    org.junit.jupiter.api.Assertions.assertEquals("", Tooltips.description("unknown", "Test", ""));
    org.junit.jupiter.api.Assertions.assertEquals("Details.", Tooltips.description("unknown", "Test", "Test\nDetails."));
  }
}
