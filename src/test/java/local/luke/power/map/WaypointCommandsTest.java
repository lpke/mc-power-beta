package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WaypointCommandsTest {
  @Test void individualHelpOmitsTeleportWhenDeniedAndKeepsSafeActions() {
    var denied = String.join("\n", WaypointCommands.helpLines(false));
    assertFalse(denied.contains("teleport"));
    for (String action : new String[]{"add", "remove", "list", "destination"})
      assertTrue(denied.contains("/waypoint " + action));
    assertTrue(String.join("\n", WaypointCommands.helpLines(true)).contains("/waypoint teleport"));
  }
  @Test void listPlacesWhiteCoordinatesBeforeDimensionSetAndFinalId() {
    var point = new Waypoint(); point.name = "Home"; point.set = "Builds";
    point.x = -12; point.y = 64; point.z = 30; point.dimension = -1;
    String line = WaypointCommands.listEntry(point);
    assertTrue(line.startsWith("§bHome §f-12 64 30 §7· Nether · Builds"));
    assertTrue(line.endsWith("§8[" + point.id + "]"));
    assertEquals(line.indexOf(point.id), line.lastIndexOf(point.id));
  }

  @Test void destinationWithoutCoordinatesRetainsCurrentReportedPositionAndDimension() {
    var point = new Waypoint(); point.x = 17; point.y = 90; point.z = -21; point.dimension = -1;
    var result = WaypointCommands.destinationPoint(point, new String[] {"waypoint", "dest"});
    assertNotSame(point, result);
    assertEquals("17 90 -21", result.coordinates()); assertEquals(-1, result.dimension);
  }

  @Test void explicitDestinationCoordinatesAreBoundedAndDoNotMutateTheInput() {
    var point = new Waypoint(); point.x = 17;
    var result = WaypointCommands.destinationPoint(point,
        new String[] {"wp", "destination", "-32000000", "127", "32000000"});
    assertEquals("-32000000 127 32000000", result.coordinates()); assertEquals(17, point.x);
    for (String[] args : new String[][] {
        {"wp", "dest", "1"}, {"wp", "dest", "1", "2"}, {"wp", "dest", "1", "2", "3", "4"},
        {"wp", "dest", "1.5", "2", "3"}, {"wp", "dest", "1", "-1", "3"},
        {"wp", "dest", "1", "128", "3"}, {"wp", "dest", "32000001", "2", "3"},
        {"wp", "dest", "1", "2", "-2147483648"}})
      assertThrows(IllegalArgumentException.class, () -> WaypointCommands.destinationPoint(point, args));
    assertEquals(17, point.x);
  }
}
