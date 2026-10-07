package local.luke.power.fastplace;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import local.luke.power.fastplace.config.*;
import org.junit.jupiter.api.Test;

class ConfigTest {



  @Test
  void listsAreStrictAndSupportMetadata() {
    var s = new Settings();
    s.listMode = Settings.ListMode.WHITELIST;
    assertFalse(s.permits(1, 0));
    s.whitelist = Settings.parseFilters("1,44:2,1");
    assertEquals(2, s.whitelist.size());
    assertTrue(s.permits(1, 7));
    assertTrue(s.permits(44, 2));
    assertFalse(s.permits(44, 3));
    for (String invalid : List.of("-1", "foo", "44:", "44:-1", "1,,2", "1:2:3", "99999999999"))
      assertThrows(IllegalArgumentException.class, () -> Settings.parseFilters(invalid));
  }



  @Test
  void tiedAndIndependentRestriction() {
    var s = new Settings();
    s.setEnabled(false);
    assertFalse(s.restrictionEnabled);
    s.setEnabled(true);
    assertTrue(s.restrictionEnabled);
    s.restrictionTiedToFast = false;
    s.restrictionEnabled = false;
    s.setEnabled(true);
    assertFalse(s.restrictionEnabled);
  }


}
