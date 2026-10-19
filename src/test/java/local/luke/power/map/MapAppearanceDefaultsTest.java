package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import local.luke.power.config.Catalog;
import local.luke.power.config.DefaultBases;
import org.junit.jupiter.api.Test;

class MapAppearanceDefaultsTest {
  @Test void allBasesUseUnitZoomNoRecentPathAndSymmetricCaveWindows() throws Exception {
    var fallback = new MapSettings();
    assertEquals(1, fallback.zoom); assertFalse(fallback.footsteps);
    assertEquals(10, fallback.caveAbove); assertEquals(10, fallback.caveBelow);
    try (var input = getClass().getResourceAsStream("/assets/powerbeta/defaults/power-beta.json")) {
      assertNotNull(input);
      var maps = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
          .getAsJsonObject().getAsJsonObject("settings").getAsJsonObject("maps");
      var fresh = new Gson().fromJson(maps, MapSettings.class); fresh.validate();
      assertEquals(new Gson().toJsonTree(fallback), new Gson().toJsonTree(fresh));
    }
    for (var base : DefaultBases.Base.values()) {
      assertEquals(1, DefaultBases.value(base, "maps.zoom", new JsonPrimitive(4)).getAsDouble());
      assertFalse(DefaultBases.value(base, "maps.footsteps", new JsonPrimitive(true)).getAsBoolean());
      for (String id : List.of("caveAbove", "caveBelow")) {
        assertEquals(10, DefaultBases.value(base, "maps." + id, new JsonPrimitive(0)).getAsInt());
        assertEquals(0, Catalog.metadata("maps." + id).get("min").getAsInt());
        assertEquals(64, Catalog.metadata("maps." + id).get("max").getAsInt());
      }
    }
  }
  @Test void existingValuesSurviveNewDefaultsAndAppearanceFieldsRoundTrip() {
    var gson = new Gson();
    var settings = gson.fromJson("{\"zoom\":2,\"footsteps\":true,\"caveAbove\":0,\"caveBelow\":1}", MapSettings.class);
    settings.validate();
    assertEquals(2, settings.zoom); assertTrue(settings.footsteps);
    assertEquals(0, settings.caveAbove); assertEquals(1, settings.caveBelow);
    settings.directionTextSize = 50; settings.belowTextSize = 200; settings.minimapArrowSize = 150;
    settings.destinationArrowColor = 3; settings.destinationArrowOpacity = .125;
    settings.minimapWaypointArrows = settings.minimapDeathpointArrows = false;
    var restored = gson.fromJson(gson.toJson(settings), MapSettings.class); restored.validate();
    assertEquals(gson.toJsonTree(settings), gson.toJsonTree(restored));
    restored.caveBelow = 0; restored.caveAbove = 64;
    assertDoesNotThrow(restored::validate);
    restored.directionTextSize = 49; assertThrows(IllegalArgumentException.class, restored::validate);
    restored.directionTextSize = 200; restored.destinationArrowOpacity = Double.NaN;
    assertThrows(IllegalArgumentException.class, restored::validate);
  }
}
