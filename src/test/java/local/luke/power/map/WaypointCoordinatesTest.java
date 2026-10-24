package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import local.luke.power.input.ReportedPosition;
import org.junit.jupiter.api.Test;

class WaypointCoordinatesTest {
  @Test void relativeValuesFloorAfterAddingTheOffsetToTheUnroundedOrigin() {
    var initial = new Waypoint(); initial.name = "Home";
    var origin = new ReportedPosition.Pose(-16.25, 83.75, 29.5, 0, 0);
    var point = WaypointCoordinates.resolve(initial, "~.5", "~-0.8", "~-30", origin);
    assertEquals("-16 82 -1", point.coordinates());
    assertEquals("Home", point.name); assertEquals(initial.id, point.id);
    assertEquals("0 64 0", initial.coordinates());
  }

  @Test void relativeOriginsAreNotClampedBeforeResolution() {
    var origin = new ReportedPosition.Pose(-32000100.25, 1000.75, 32000100.5, 0, 0);
    var point = WaypointCoordinates.resolve(new Waypoint(), "~101", "~-900", "~-101", origin);
    assertEquals("-32000000 100 31999999", point.coordinates());
    assertThrows(IllegalArgumentException.class,
        () -> WaypointCoordinates.resolve(new Waypoint(), "~", "~-900", "~-101", origin));
  }

  @Test void absoluteAxesRetainIntegerSyntaxAndIgnoreReportedPosition() {
    var origin = new ReportedPosition.Pose(-90000000, 1000, 90000000, 0, 0);
    assertEquals("-32000000 127 32000000",
        WaypointCoordinates.resolve(new Waypoint(), "-32000000", "127", "+32000000", origin).coordinates());
    for (String x : new String[]{"1.5", "NaN", "Infinity", "1e2", "2147483648", "32000001"})
      assertThrows(IllegalArgumentException.class, () -> WaypointCoordinates.resolve(new Waypoint(), x, "64", "0", origin));
  }

  @Test void failuresOnAnyAxisNeverMutateTheDraft() {
    var point = new Waypoint(); point.x = 17; point.y = 83; point.z = -21;
    var origin = new ReportedPosition.Pose(10.25, 64.75, -10.25, 0, 0);
    for (String[] coordinates : new String[][]{{"~", "~64", "~"}, {"~", "~", "~oops"},
        {"~NaN", "~", "~"}, {"~", "~Infinity", "~"}, {"~", "~", "~1e309"},
        {"~", "~", "~-32000000"}, {"~", "~-65", "~"}})
      assertThrows(IllegalArgumentException.class,
          () -> WaypointCoordinates.resolve(point, coordinates[0], coordinates[1], coordinates[2], origin));
    assertEquals("17 83 -21", point.coordinates());
  }
}
