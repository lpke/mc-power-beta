package local.luke.power.hud;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InfoHudDemandTest {
  @Test void hiddenDisabledTransparentAndDebugHiddenClustersRequestNothing() {
    var settings = new InfoHudSettings();
    settings.clusters[0].biome = true;
    assertEquals(0, InfoHudDemand.of(settings, false, false, false));
    settings.enabled = true;
    assertEquals(0, InfoHudDemand.of(settings, true, false, false));
    assertEquals(0, InfoHudDemand.of(settings, false, true, false));
    settings.clusters[0].opacity = 0;
    assertEquals(0, InfoHudDemand.of(settings, false, false, false));
    settings.clusters[0].opacity = 100;
    settings.clusters[0].enabled = false;
    assertEquals(0, InfoHudDemand.of(settings, false, false, false));
  }

  @Test void externalClockAndVisibleFieldsKeepSeparateConsumers() {
    var settings = new InfoHudSettings();
    assertEquals(InfoHudDemand.TIME, InfoHudDemand.of(settings, true, true, true));
    settings.enabled = true;
    assertEquals(InfoHudDemand.COORDINATES | InfoHudDemand.DIRECTION,
        InfoHudDemand.of(settings, false, false, false));
    settings.clusters[1].enabled = true;
    settings.clusters[1].biome = settings.clusters[1].day = true;
    settings.clusters[1].hideWithDebug = false;
    assertEquals(InfoHudDemand.BIOME | InfoHudDemand.DAY,
        InfoHudDemand.of(settings, false, true, false));
  }
}
