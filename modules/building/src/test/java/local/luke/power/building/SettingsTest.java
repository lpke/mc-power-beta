package local.luke.power.building;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import local.luke.power.fastplace.*;
import local.luke.power.building.config.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class SettingsTest {
  @TempDir Path dir;

  @Test
  void allGroupsRoundTripAndModeDefault() throws Exception {
    ConfigStore store = new ConfigStore(dir.resolve("power_building.properties"));
    Settings s = store.load();
    assertEquals(SlabMode.DOUBLE, s.placement.slabMode);
    s.placement.slabMode = SlabMode.DOUBLE;
    s.placement.blacklist = local.luke.power.fastplace.config.Settings.parseFilters("44:2,1");
    s.flexible.overlayColor = 3;
    s.sneak.enabled = true;
    s.slabs.enabled = false;
    s.freeLookToggle = true;
    s.freeLookPerspective = local.luke.power.building.camera.Perspective.FIRST_PERSON;
    s.boatSteering = false;
    s.clickMining = false;
    store.save(s);
    Settings r = store.load();
    assertEquals(SlabMode.DOUBLE, r.placement.slabMode);
    assertEquals(s.placement.blacklist, r.placement.blacklist);
    assertEquals(3, r.flexible.overlayColor);
    assertTrue(r.sneak.enabled);
    assertFalse(r.slabs.enabled);
    assertTrue(r.freeLookToggle);
    assertEquals(local.luke.power.building.camera.Perspective.FIRST_PERSON, r.freeLookPerspective);
    assertFalse(r.boatSteering);
    assertFalse(r.clickMining);
  }

  @Test
  void migrationPreservesLegacySettingsWithoutChangingFiles() throws Exception {
    Path old = dir.resolve("beta-fastplace.properties");
    String data =
        "enabled=true\n"
            + "fastBlockPlacementCount=7\n"
            + "placementRestrictionMode=LINE\n"
            + "keepSlabLayer=true\n";
    Files.writeString(old, data);
    Files.writeString(dir.resolve("free_look.properties"), "toggleMode=true\n");
    Settings s = new ConfigStore(dir.resolve("power_building.properties")).load();
    assertTrue(s.placement.enabled);
    assertEquals(7, s.placement.attemptsPerTick);
    assertEquals(RestrictionMode.LINE, s.placement.restrictionMode);
    assertEquals(SlabMode.CONTINUOUS, s.placement.slabMode);
    assertTrue(s.freeLookToggle);
    assertEquals(data, Files.readString(old));
  }

  @Test
  void malformedAndFailedSavesPreserveFiles() throws Exception {
    Path p = dir.resolve("power_building.properties");
    Files.writeString(p, "placement.slabMode=WRONG\n");
    assertThrows(java.io.IOException.class, () -> new ConfigStore(p).load());
    assertEquals("placement.slabMode=WRONG\n", Files.readString(p));
    Path blocked = dir.resolve("directory");
    Files.createDirectories(blocked);
    Files.writeString(blocked.resolve("keep"), "yes");
    assertThrows(java.io.IOException.class, () -> new ConfigStore(blocked).save(new Settings()));
    assertEquals("yes", Files.readString(blocked.resolve("keep")));
  }

  @Test
  void slabModesMatchTheStartingActionAndLayer() {
    assertTrue(SlabPolicy.allows(SlabMode.CONTINUOUS, false, true, 64, 64));
    assertTrue(SlabPolicy.allows(SlabMode.CONTINUOUS, true, false, 64, 65));
    assertTrue(SlabPolicy.allows(SlabMode.DOUBLE, false, false, 64, 65));
    assertFalse(SlabPolicy.allows(SlabMode.MATCH_FIRST, false, true, 64, 64));
    assertFalse(SlabPolicy.allows(SlabMode.MATCH_FIRST, true, false, 64, 64));
    assertTrue(SlabPolicy.allows(SlabMode.MATCH_FIRST, true, true, 64, 64));
    assertFalse(SlabPolicy.allows(SlabMode.MATCH_FIRST, false, false, 64, 65));
  }
}
