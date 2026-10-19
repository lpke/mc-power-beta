package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class WaypointLabelTest {
  @Test void aimingExpandsOnlyAnOrdinaryWaypointNameWhileLegacyAndExplicitAbbreviationsRemainReadable() {
    var point = new Waypoint(); point.name = "North ridge camp"; point.abbreviation = "NRC";
    var settings = new MapSettings();
    assertEquals("NRC", WaypointLabel.of(point, settings, false, 64, 37.6).name());
    assertEquals("North ridge camp", WaypointLabel.of(point, settings, true, 64, 37.6).name());
    assertEquals("38m", WaypointLabel.of(point, settings, true, 64, 37.6).distance());
    point.abbreviation = null;
    assertEquals("N", WaypointLabel.of(point, settings, false, 64, 37.6).name());
    point.name = "Camp at lake";
    assertEquals("C", WaypointLabel.of(point, settings, false, 64, 37.6).name());
    settings.waypointNames = false;
    assertEquals("", WaypointLabel.of(point, settings, false, 64, 37.6).name());
    assertEquals("", WaypointLabel.of(point, settings, true, 64, 37.6).name());
    assertEquals("38m", WaypointLabel.of(point, settings, true, 64, 37.6).distance());
  }

  @Test void destinationsAlwaysUseTheirCoordinatesAndDeathsNeverAcquireANameFromAiming() {
    var destination = new Waypoint(); destination.id = "temporary-destination";
    destination.name = "Destination"; destination.abbreviation = "D";
    destination.x = -120; destination.y = 64; destination.z = 300;
    var death = new Waypoint(); death.death = true; death.name = "Death 2"; death.abbreviation = "DEAD";
    var settings = new MapSettings();
    for (boolean names : new boolean[]{false, true}) for (boolean aimed : new boolean[]{false, true}) {
      settings.waypointNames = names;
      assertEquals("-120,64,300", WaypointLabel.of(destination, settings, aimed, 200, 123.4).name());
      var label = WaypointLabel.of(death, settings, aimed, 200, 123.4);
      assertEquals("", label.name()); assertEquals("123m", label.distance());
    }
    settings.waypointDistances = false;
    assertEquals("", WaypointLabel.of(death, settings, true, 200, 123.4).distance());
    var label = WaypointLabel.of(destination, settings, false, 200, 123.4);
    assertEquals("-120,64,300", label.name()); assertEquals("", label.distance());
  }

  @Test void distanceScalingIsContinuousMonotoneAndFiniteAtEverySupportedTextSize() {
    var point = new Waypoint(); var settings = new MapSettings();
    for (int size : new int[]{50, 75, 100, 150}) {
      settings.waypointTextSize = size;
      double base = size / 100d, previous = Double.POSITIVE_INFINITY;
      for (double distance : new double[]{-1, 0, 1, 31.99, 32, 64, 128, 512, 2048, Double.MAX_VALUE}) {
        double scale = WaypointLabel.of(point, settings, false, distance, 0).scale();
        assertTrue(Double.isFinite(scale));
        assertTrue(scale >= base * .65 && scale <= base);
        assertTrue(scale <= previous); previous = scale;
      }
      double a = WaypointLabel.of(point, settings, false, 128, 0).scale();
      double b = WaypointLabel.of(point, settings, false, 128.01, 0).scale();
      assertTrue(b < a && a - b < .001, "distance scaling must not snap between ranges");
      for (double malformed : new double[]{Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
        double scale = WaypointLabel.of(point, settings, false, malformed, 0).scale();
        assertTrue(Double.isFinite(scale)); assertTrue(scale >= base * .65 && scale <= base);
      }
      settings.waypointDistanceScaling = false;
      for (double distance : new double[]{0, 64, 1000000, Double.NaN, Double.POSITIVE_INFINITY})
        assertEquals(base, WaypointLabel.of(point, settings, false, distance, 0).scale());
      settings.waypointDistanceScaling = true;
    }
  }

  @Test void minimapAndWorldDestinationSymbolsShareColorAndOpacityWithoutRecoloringSavedWaypoints() {
    var settings = new MapSettings();
    var destination = new Waypoint(); destination.id = "temporary-destination"; destination.colour = 0x123456;
    var ordinary = new Waypoint(); ordinary.colour = 0x654321;
    var death = new Waypoint(); death.death = true; death.colour = 0xffcc33;
    for (int color : new int[]{0, 3, 0x5599ff, 0xffffff}) for (double opacity : new double[]{0, .125, 1}) {
      settings.destinationArrowColor = color; settings.destinationArrowOpacity = opacity;
      var arrow = MinimapArrows.style(destination, settings, true);
      assertEquals(color, WaypointSymbols.color(destination, settings));
      assertEquals(opacity, WaypointSymbols.opacity(destination, settings));
      assertEquals(arrow.color(), WaypointSymbols.color(destination, settings));
      assertEquals(arrow.opacity(), WaypointSymbols.opacity(destination, settings));
      for (var point : List.of(ordinary, death)) {
        assertEquals(point.colour, WaypointSymbols.color(point, settings));
        assertEquals(1, WaypointSymbols.opacity(point, settings));
      }
    }
    assertEquals(0x123456, destination.colour); assertEquals(0x654321, ordinary.colour);
  }
}
