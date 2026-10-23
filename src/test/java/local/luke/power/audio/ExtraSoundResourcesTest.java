package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URL;
import org.junit.jupiter.api.Test;

class ExtraSoundResourcesTest {
  @Test void repairOwnsBothExtraEffectNamespacesAndExcludesMusicAndNativeAudio() throws Exception {
    assertTrue(ExtraSoundResources.owns(new URL("jar:file:/pack.jar!/assets/power_beta/stationapi/sounds/sound/item/bucket/fill1.ogg")));
    assertTrue(ExtraSoundResources.owns(new URL("jar:file:/controls.jar!/assets/power_controls/stationapi/sounds/sound/random/eat1.ogg")));
    assertFalse(ExtraSoundResources.owns(new URL("file:/game/resources/sound/random/splash.ogg")));
    assertFalse(ExtraSoundResources.owns(new URL("jar:file:/pack.jar!/assets/power_beta/music/track.ogg")));
    assertFalse(ExtraSoundResources.owns(new URL("file:/music/personal.ogg?resource=/assets/power_beta/stationapi/sounds/sound/")));
    assertFalse(ExtraSoundResources.owns(null));
  }
}
