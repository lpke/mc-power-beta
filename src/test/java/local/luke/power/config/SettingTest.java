package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.*;

class SettingTest {
  @Test void flightResponseSlidersUseWholeStepsWithoutQuantizingManualEntry() {
    for(String id:List.of("creative.acceleration","creative.spectatorAcceleration","power_camera:config.acceleration",
        "creative.glide","creative.spectatorGlide","power_camera:config.drag")) {
      var setting=Catalog.setting(id,"test","","","","",Setting.Kind.DECIMAL,
          new JsonPrimitive(3.5),new JsonPrimitive(3.5),0,5,.1,List.of(),false);
      assertEquals(1,setting.step);
      for(int point=0;point<=100;point++) {
        setting.slide(point/100.0);
        assertEquals(Math.rint(setting.value.getAsDouble()),setting.value.getAsDouble());
      }
      setting.parse("2.375");assertEquals(2.375,setting.value.getAsDouble());
      setting.reset();assertEquals(setting.defaultValue.getAsDouble(),setting.value.getAsDouble());
      assertThrows(IllegalArgumentException.class,()->setting.parse("5.01"));
    }
  }
  @Test void freecamPercentEditingKeepsStoredSpeedAndAcceptsRoundedBoundaries() {
    double base=local.luke.power.input.FlightMotion.HORIZONTAL_SPEED;
    Setting s=Catalog.setting("power_camera:config.speed","test","Camera","Free camera","Speed","",
        Setting.Kind.DECIMAL,new JsonPrimitive(base*1.1),new JsonPrimitive(12),0,1000,.1,List.of(),false);
    assertEquals("110",s.editText());
    assertEquals("110%",s.display());
    assertEquals("Freecam speed",s.label);
    assertEquals(110,s.defaultValue.getAsDouble()*100/base,1e-7);
    s.parse("100");assertEquals(base,s.value.getAsDouble(),1e-9);
    s.parse("40");assertEquals(base*.4,s.value.getAsDouble(),1e-9);
    s.slide(1);String boundary=s.editText();s.parse(boundary);assertEquals(s.max,s.value.getAsDouble());
    s.parse("0");assertEquals(0,s.value.getAsDouble());
    assertThrows(IllegalArgumentException.class,()->s.parse("100000"));
  }

  @Test void percentagesShareLabelsAndDisplayWithoutChangingExactEntryOrStoredUnits() {
    for (String id : List.of("worldedit.opacity", "tweaks.flexible.overlayOpacity", "creative.flightSpeed",
        "visual.underwaterFogDensity", "native.music", "native.sensitivity", "infoHud.clusters.0.opacity",
        "audio.master", "audio.category.blocks", "audio.sound.music:test", "rules.creation.explosion_drop_chance")) {
      var s = new Setting(id,"test","","","Test %","",Setting.Kind.INTEGER,
          new JsonPrimitive(75),new JsonPrimitive(100),0,200,1,List.of(),false);
      assertFalse(s.label.contains("%"),id); assertEquals("75%",s.display(),id);
      assertEquals("75",s.editText(),id); s.parse("35"); assertEquals(35,s.value.getAsInt(),id);
    }
    var ambient = Catalog.setting("power_environment:config.MUSIC_CONFIG.volumeRainAmbient", "test", "", "", "", "",
        Setting.Kind.DECIMAL,new JsonPrimitive(.75),new JsonPrimitive(1),0,1,.05,List.of(),false);
    assertEquals("75%",ambient.display()); assertEquals("75",ambient.editText());
    ambient.parse("25"); assertEquals(.25,ambient.value.getAsDouble());
    assertThrows(IllegalArgumentException.class,()->ambient.parse("101"));
  }

  @Test void durationEditorsUseSecondsWithoutChangingStorageOrPrecision() {
    Setting s = new Setting("creative.doubleTapTicks", "creative", "", "", "", "",
        Setting.Kind.INTEGER, new JsonPrimitive(7), new JsonPrimitive(7), 2, 20, 1, List.of(), false);
    assertEquals("0.35", s.display());
    assertEquals("0.35", s.editText());
    assertEquals("0.1 to 1 seconds", s.rangeText());
    s.parse("0.55");
    assertEquals(11, s.value.getAsInt());
    assertThrows(IllegalArgumentException.class, () -> s.parse("0.551"));
    assertEquals(11, s.value.getAsInt());
    s.reset(); assertEquals(7, s.value.getAsInt());
    assertTrue(Tooltips.setting(s, "").contains("Range: 0.1 to 1 seconds."));
  }

  @Test void validationRejectsEditsBeforePublishingAndNoticesHaveBlankLines() {
    Setting s = new Setting("worldedit.wandItem", "worldedit", "", "", "Wand", "Select a tool.",
        Setting.Kind.INTEGER, new JsonPrimitive(271), new JsonPrimitive(271), 1, 32767, 1, List.of(), true);
    s.validator(value -> { if (value.getAsInt() != 271) throw new IllegalArgumentException("Unknown item"); });
    assertFalse(s.slider());
    assertThrows(IllegalArgumentException.class, () -> s.parse("999"));
    assertEquals(271, s.value.getAsInt());
    assertTrue(Tooltips.setting(s, "Click to edit.").contains("\n\nRestart required."));
    assertTrue(Tooltips.setting(s, "Click to edit.").contains("\n\nRange: 1 to 32767."));
  }

  @Test
  void largeMusicSelectionsCanBeSavedWithoutRelaxingOtherListLimits() {
    JsonArray tracks = new JsonArray();
    for (int i = 0; i < 4096; i++) tracks.add("music:custom/" + "a".repeat(64) + i + ".ogg");
    assertTrue(tracks.toString().length() > 16384);
    for (String id : List.of("audio.favourites", "audio.playlists", "audio.musicDirectories", "audio.libraryGroupVolumes", "audio.libraryOrder", "audio.playlistOrder", "audio.exclusions", "other")) {
      Setting s = new Setting(id, "audio", "Audio", "Music data", "Music selection", "",
          Setting.Kind.LIST, new JsonArray(), new JsonArray(), 0, 0, 1, List.of(), false);
      if (id.equals("other") || id.equals("audio.exclusions")) assertThrows(IllegalArgumentException.class, () -> s.validate(tracks));
      else assertDoesNotThrow(() -> s.validate(tracks));
      JsonArray oversized = new JsonArray();
      oversized.add("x".repeat(4194304));
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
