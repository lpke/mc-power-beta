package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import java.time.ZonedDateTime;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

class WaypointRefinementsTest {
  @Test void deathTimesUseLocalDateAndTwelveHourClockIncludingMidnightAndSingleDigitDates() {
    TimeZone original = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("Australia/Sydney"));
      long morning = ZonedDateTime.parse("2026-03-07T00:05:00+11:00[Australia/Sydney]").toInstant().toEpochMilli();
      assertEquals("7/3 12:05", WaypointTimes.compact(morning));
      assertEquals("12:05", WaypointTimes.time(morning));
      assertEquals("7/3/2026 12:05 AM", WaypointTimes.detail(morning));
      long afternoon = ZonedDateTime.parse("2026-10-20T15:09:00+11:00[Australia/Sydney]").toInstant().toEpochMilli();
      assertEquals("20/10 03:09", WaypointTimes.compact(afternoon));
      assertEquals("03:09", WaypointTimes.time(afternoon));
      assertEquals("20/10/2026 03:09 PM", WaypointTimes.detail(afternoon));
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
      assertEquals("6/3 01:05", WaypointTimes.compact(morning), "cached label must track timezone changes");
      assertEquals("01:05", WaypointTimes.time(morning));
    } finally { TimeZone.setDefault(original); }
  }

  @Test void caveLightPreviewChangesCavesOnlyAndLeavesStoredTerrainIntact() {
    var settings = new MapSettings();
    var cave = new MapTile(new MapMath.Key(0, 5, 0, 0, 0));
    cave.put(0, 0x809060, 0x809060, 63, 0, 1, 0);
    var surface = new MapTile(new MapMath.Key(0, -1, 0, 0, 0));
    surface.put(0, 0x809060, 0x809060, 63, 0, 1, 0);
    int surfaceColor = surface.color(0, settings), dark = cave.color(0, settings), style = settings.style();
    settings.caveLighting = false;
    assertNotEquals(style, settings.style()); assertNotEquals(dark, cave.color(0, settings));
    assertEquals(surfaceColor, surface.color(0, settings)); assertEquals(0, cave.light(0));
    settings.caveLighting = true; assertEquals(dark, cave.color(0, settings));
  }

  @Test void markerSizeAndBobModeValidateAndFacetsRetainDistinctLightAndShade() {
    var settings = new MapSettings(); settings.validate();
    assertEquals(0x00ecff, settings.destinationArrowColor);
    assertEquals(85, settings.worldMapWaypointSize); assertEquals(1, settings.waypointBobbing);
    settings.waypointBobbing = 1; settings.validate();
    settings.waypointBobbing = 2; assertThrows(IllegalArgumentException.class, settings::validate);
    settings.waypointBobbing = 0; settings.worldMapWaypointSize = 151;
    assertThrows(IllegalArgumentException.class, settings::validate);
    assertNotEquals(WaypointSymbols.facetColor(0xffcc33, 1), WaypointSymbols.facetColor(0xffcc33, 3));
  }
}
