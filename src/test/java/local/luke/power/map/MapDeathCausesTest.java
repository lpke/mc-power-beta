package local.luke.power.map;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MapDeathCausesTest {
  @Test void optionalCausesPreserveOldWaypointsAndFormatTheRecordedReason() {
    var json=new Gson();var old=json.fromJson("{\"id\":\"old-death\",\"name\":\"Death\",\"death\":true}",Waypoint.class);
    old.validate();assertEquals("",MapDeathCauses.caption(old));
    old.deathCause="Drowning";assertEquals("Drowned",MapDeathCauses.caption(old));
    old.deathCause="Fall";assertEquals("Died from a fall",MapDeathCauses.caption(old));
    old.deathCause="Killed by Zombie";assertEquals("Killed by Zombie",MapDeathCauses.caption(old));
    var restored=json.fromJson(json.toJson(old),Waypoint.class);assertEquals(old.deathCause,restored.deathCause);
    assertEquals(old.deathCause,old.copy().deathCause);
    old.deathCause="Unknown cause";assertEquals("",MapDeathCauses.caption(old));
    old.deathCause="invalid\nline";assertThrows(IllegalArgumentException.class,old::validate);
  }
}
