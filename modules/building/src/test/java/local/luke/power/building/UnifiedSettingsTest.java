package local.luke.power.building;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import java.nio.file.*;
import java.util.List;
import local.luke.power.storage.PowerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UnifiedSettingsTest {
  @TempDir Path game;

  @Test void allBuildingSettingsRoundTripThroughTheGamesGson() throws Exception {
    PowerConfig.configure(game.resolve("config/power-beta.json"));
    var fast = new local.luke.power.fastplace.config.Settings();
    fast.blacklist = local.luke.power.fastplace.config.Settings.parseFilters("1,35:4");
    fast.whitelist = local.luke.power.fastplace.config.Settings.parseFilters("20:0,5");
    var flexible = new local.luke.power.flexible.config.Settings();
    var building = new local.luke.power.building.config.Settings();
    var sneak = new local.luke.power.fakesneak.config.Settings();
    var slab = new local.luke.power.slabplacement.config.Settings();
    Gson json = new Gson();
    for (Object settings : List.of(fast, flexible, building, sneak, slab)) {
      String id = settings.getClass().getPackageName();
      PowerConfig.save(id, settings);
      Object restored = PowerConfig.read(id, settings.getClass());
      assertEquals(json.toJson(settings), json.toJson(restored));
    }
    var restored = PowerConfig.read(fast.getClass().getPackageName(), fast.getClass());
    assertEquals(fast.blacklist, restored.blacklist);
    assertFalse(restored.permits(35, 4));
    assertTrue(restored.permits(35, 3));
    assertFalse(restored.permits(1, 9));
  }
}
