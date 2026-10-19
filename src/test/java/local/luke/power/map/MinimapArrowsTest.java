package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MinimapArrowsTest {
  @Test void ordinaryDestinationAndDeathArrowsHaveIndependentMinimapSwitches() {
    var settings = new MapSettings();
    var waypoint = new Waypoint(); var death = new Waypoint(); death.death = true;
    var destination = new Waypoint(); destination.id = "temporary-destination";
    settings.minimapWaypointArrows = false;
    assertFalse(MinimapArrows.style(waypoint, settings, true).visible());
    assertTrue(MinimapArrows.style(death, settings, true).visible());
    assertTrue(MinimapArrows.style(destination, settings, true).visible());
    settings.minimapDeathpointArrows = false;
    assertFalse(MinimapArrows.style(death, settings, true).visible());
    assertTrue(MinimapArrows.style(destination, settings, true).visible());
    settings.minimapDestinationArrow = false;
    assertFalse(MinimapArrows.style(destination, settings, true).visible());
    settings.minimapWaypointArrows = settings.minimapDestinationArrow = settings.minimapDeathpointArrows = true;
    assertFalse(MinimapArrows.style(waypoint, settings, false).visible());
    assertFalse(MinimapArrows.style(destination, settings, false).visible());
    settings.waypoints = false;
    assertFalse(MinimapArrows.style(destination, settings, true).visible());
  }
  @Test void destinationHasItsOwnColorAlphaAndScaledEdgeClearance() {
    var settings = new MapSettings(); settings.destinationArrowColor = 3;
    settings.destinationArrowOpacity = .125; settings.minimapArrowSize = 200;
    var destination = new Waypoint(); destination.id = "temporary-destination";
    var style = MinimapArrows.style(destination, settings, true);
    assertEquals(3, style.color()); assertEquals(.125, style.opacity()); assertEquals(2, style.scale());
    var view = new MapProjection(0, 0, 1, 0, 0, 108, 108, 0);
    for (boolean circle : new boolean[]{false, true}) {
      var edge = view.edge(view.screen(10000, -10000), circle, style.inset());
      assertTrue(view.contains(edge.x(), edge.y(), circle, style.scale() * 6.5));
    }
    var ordinary = new Waypoint(); ordinary.colour = 0x123456;
    var normal = MinimapArrows.style(ordinary, settings, true);
    assertEquals(ordinary.colour, normal.color()); assertEquals(1, normal.opacity());
  }
}
