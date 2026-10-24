package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import local.luke.power.input.ReportedPosition;
import org.junit.jupiter.api.Test;

class WaypointCommandsTest {
  @Test void individualHelpOmitsTeleportWhenDeniedAndKeepsSafeActions() {
    var denied = String.join("\n", WaypointCommands.helpLines(false));
    assertFalse(denied.contains("teleport"));
    for (String action : new String[]{"add", "remove", "list", "destination"})
      assertTrue(denied.contains("/waypoint " + action));
    assertTrue(denied.contains("/waypoint add <name> [x y z]"));
    assertTrue(denied.contains("current reported position"));
    assertTrue(denied.contains("final three-coordinate group"));
    assertTrue(denied.contains("~offset"));
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

  @Test void addWithoutCoordinatesKeepsReportedPositionDimensionSetAndNamesWithSpaces() {
    var point = new Waypoint(); point.x = -17; point.y = 93; point.z = 21;
    point.dimension = -1; point.set = "Building plans";
    for (String name : new String[] {"Home base", "Base 2", "Build 1 2", "1 2 3", "North east corner"}) {
      var result = WaypointCommands.addPoint(point, ("wp add " + name).split(" "));
      assertNotSame(point, result); assertEquals(name, result.name);
      assertEquals("-17 93 21", result.coordinates()); assertEquals(-1, result.dimension);
      assertEquals("Building plans", result.set); assertEquals(point.id, result.id);
    }
    assertEquals("Waypoint", point.name);
  }

  @Test void addUsesOnlyTheFinalCoordinateTripletAndKeepsTheNameAndCurrentDimension() {
    var point = new Waypoint(); point.x = 17; point.dimension = -1; point.set = "Building plans";
    var result = WaypointCommands.addPoint(point,
        new String[] {"waypoint", "add", "Base", "1", "2", "3", "-32000000", "127", "+32000000"});
    assertEquals("Base 1 2 3", result.name); assertEquals("-32000000 127 32000000", result.coordinates());
    assertEquals(-1, result.dimension); assertEquals("Building plans", result.set);
    assertEquals(17, point.x); assertEquals("Waypoint", point.name);
  }

  @Test void addRejectsNonWholeNonFiniteAndOutOfBoundsCoordinatesWithoutChangingTheInput() {
    var point = new Waypoint(); point.x = 17;
    for (String coordinates : new String[] {
        "1.5 64 3", "1 64.5 3", "1 64 3.5", "NaN 64 3", "Infinity 64 3", "1 64 -Infinity",
        "1e2 64 3", "999999999999999999999999999999999999 64 3", "32000001 64 3",
        "-32000001 64 3", "1 -1 3", "1 128 3", "1 64 -2147483648"})
      assertThrows(IllegalArgumentException.class,
          () -> WaypointCommands.addPoint(point, ("way add Home base " + coordinates).split(" ")), coordinates);
    assertThrows(IllegalArgumentException.class, () -> WaypointCommands.addPoint(point, new String[] {"wp", "add"}));
    assertThrows(IllegalArgumentException.class, () -> WaypointCommands.addPoint(point, new String[] {"wp", "add", " "}));
    assertThrows(IllegalArgumentException.class,
        () -> WaypointCommands.addPoint(point, new String[] {"wp", "add", "x".repeat(65), "1", "64", "3"}));
    assertEquals(17, point.x); assertEquals("Waypoint", point.name);
  }

  @Test void aliasesResolveMixedRelativeCoordinatesAndKeepMultiWordNames() {
    var point = new Waypoint(); point.dimension = -1; point.set = "Building plans";
    var origin = new ReportedPosition.Pose(-16.25, 83.75, 29.5, 0, 0);
    for (String alias : new String[]{"waypoint", "way", "wp"}) {
      var result = WaypointCommands.addPoint(point, (alias + " add Example Waypoint ~100 ~ 100").split(" "), origin);
      assertEquals("Example Waypoint", result.name); assertEquals("83 83 100", result.coordinates());
      assertEquals(-1, result.dimension); assertEquals("Building plans", result.set);
      for (String action : new String[]{"dest", "destination"})
        assertEquals("-17 83 29", WaypointCommands.destinationPoint(point,
            (alias + " " + action + " ~ ~ ~").split(" "), origin).coordinates());
    }
    assertEquals("0 64 0", point.coordinates());
  }

  @Test void invalidRelativeSuffixesNeverBecomeWaypointNames() {
    var point = new Waypoint();
    for (String suffix : new String[]{"~", "~ ~", "~ ~ ~oops", "~ foo 10", "~NaN ~ ~", "~Infinity ~ ~",
        "~1e309 ~ ~", "~32000001 ~ ~", "~ ~128 ~", "~ ~ ~-32000001"}) {
      assertThrows(IllegalArgumentException.class,
          () -> WaypointCommands.addPoint(point, ("wp add Home base " + suffix).split(" ")), suffix);
    }
    assertEquals("Waypoint", point.name); assertEquals("0 64 0", point.coordinates());
  }

  @Test void coordinateCompletionOffersRelativeSyntaxWithoutAReadyWaypointStore() {
    for (String alias : new String[]{"waypoint", "way", "wp"}) {
      assertArrayEquals(new String[]{"~"}, WaypointCommands.suggestions(null, 3, "", "/" + alias + " add Home "));
      assertArrayEquals(new String[]{""}, WaypointCommands.suggestions(null, 4, "~", "/" + alias + " add Home ~ ~"));
      for (String action : new String[]{"dest", "destination"}) for (int axis = 2; axis <= 4; axis++)
        assertArrayEquals(new String[]{"~"}, WaypointCommands.suggestions(null, axis, "", "/" + alias + " " + action + " "));
      assertArrayEquals(new String[0], WaypointCommands.suggestions(null, 2, "", "/" + alias + " add "));
    }
  }
}
