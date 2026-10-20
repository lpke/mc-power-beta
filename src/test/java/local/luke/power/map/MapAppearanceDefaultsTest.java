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
  @Test void powerBetaAppearanceOverridesMatchFreshDefaultsAndKeepOtherBasesStable() throws Exception {
    var fallback = new MapSettings();
    assertEquals(1, fallback.zoom); assertFalse(fallback.footsteps);
    assertEquals(10, fallback.caveAbove); assertEquals(10, fallback.caveBelow);
    try (var input = getClass().getResourceAsStream("/assets/powerbeta/defaults/power-beta.json")) {
      assertNotNull(input);
      var maps = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
          .getAsJsonObject().getAsJsonObject("settings").getAsJsonObject("maps");
      var fresh = new Gson().fromJson(maps, MapSettings.class); fresh.validate();
      for (var field : MapSettings.class.getFields()) {
        var metadata = Catalog.metadata("maps." + field.getName());
        JsonElement original = metadata.has("default") ? metadata.get("default")
            : new Gson().toJsonTree(field.get(fallback));
        assertEquals(DefaultBases.value(DefaultBases.Base.POWER_BETA, "maps." + field.getName(), original),
            maps.get(field.getName()), field.getName());
      }
      assertTrue(fresh.pauseMap && fresh.items);
      assertEquals(90, fresh.size); assertEquals(1.25, fresh.zoom);
      assertFalse(fresh.biome); assertEquals(60, fresh.minimapArrowSize);
      assertEquals(70, fresh.minimapWaypointTextSize); assertEquals(90, fresh.minimapWaypointSize);
      assertEquals(0xcc66ff, fresh.radarItemsColor);
      assertEquals(0x00b1ff, fresh.radarVehiclesColor); assertEquals(0x00b1ff, fresh.radarProjectilesColor); assertEquals(0x00b1ff, fresh.radarOtherColor);
      assertEquals(75, fresh.radarIconSize); assertEquals(0xff402e, fresh.radarHostileColor);
      assertFalse(fresh.worldMapRadarIcons); fresh.minimapArrowSize = 5; fresh.validate();
      assertEquals(75, fresh.directionTextSize); assertEquals(90, fresh.belowTextSize);
    }
    for (var base : DefaultBases.Base.values()) {
      assertEquals(base == DefaultBases.Base.POWER_BETA ? 1.25 : 1,
          DefaultBases.value(base, "maps.zoom", new JsonPrimitive(4)).getAsDouble());
      assertFalse(DefaultBases.value(base, "maps.footsteps", new JsonPrimitive(true)).getAsBoolean());
      for (String id : List.of("caveAbove", "caveBelow")) {
        assertEquals(10, DefaultBases.value(base, "maps." + id, new JsonPrimitive(0)).getAsInt());
        assertEquals(0, Catalog.metadata("maps." + id).get("min").getAsInt());
        assertEquals(64, Catalog.metadata("maps." + id).get("max").getAsInt());
      }
    }
    for (var base : List.of(DefaultBases.Base.BETA, DefaultBases.Base.REFINED)) {
      assertEquals(108, DefaultBases.value(base, "maps.size", new JsonPrimitive(fallback.size)).getAsInt());
      assertFalse(DefaultBases.value(base, "maps.pauseMap", new JsonPrimitive(fallback.pauseMap)).getAsBoolean());
      assertFalse(DefaultBases.value(base, "maps.items", new JsonPrimitive(fallback.items)).getAsBoolean());
      assertTrue(DefaultBases.value(base, "maps.biome", new JsonPrimitive(fallback.biome)).getAsBoolean());
      for (String id : List.of("directionTextSize", "belowTextSize", "minimapArrowSize"))
        assertEquals(100, DefaultBases.value(base, "maps." + id, new JsonPrimitive(100)).getAsInt());
    }
  }
  @Test void existingValuesSurviveNewDefaultsAndAppearanceFieldsRoundTrip() {
    var gson = new Gson();
    var settings = gson.fromJson("{\"zoom\":2,\"footsteps\":true,\"caveAbove\":0,\"caveBelow\":1}", MapSettings.class);
    settings.validate();
    assertEquals(2, settings.zoom); assertTrue(settings.footsteps);
    assertEquals(0, settings.caveAbove); assertEquals(1, settings.caveBelow);
    settings.directionTextSize = 50; settings.belowTextSize = 200; settings.minimapArrowSize = 150;
    settings.minimapWaypointTextSize = 200; settings.minimapWaypointSize = 50;
    settings.directionTextColor = 0; settings.directionTextOpacity = .25;
    settings.minimapArrowOpacity = .6; settings.radarWolfColor = 3;
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

  @Test void everyNativeMobInheritsItsCategoryUntilGivenAValidCustomRgb() throws Exception {
    var settings = new MapSettings();
    var ids = java.util.Map.ofEntries(
        java.util.Map.entry("Creeper", "Creeper"), java.util.Map.entry("Skeleton", "Skeleton"),
        java.util.Map.entry("Spider", "Spider"), java.util.Map.entry("Giant", "Giant"),
        java.util.Map.entry("Zombie", "Zombie"), java.util.Map.entry("Slime", "Slime"),
        java.util.Map.entry("Ghast", "Ghast"), java.util.Map.entry("PigZombie", "ZombiePigman"),
        java.util.Map.entry("Pig", "Pig"), java.util.Map.entry("Sheep", "Sheep"),
        java.util.Map.entry("Cow", "Cow"), java.util.Map.entry("Chicken", "Chicken"),
        java.util.Map.entry("Squid", "Squid"), java.util.Map.entry("Wolf", "Wolf"));
    for (var entry : ids.entrySet()) {
      String fieldName = "radar" + entry.getValue() + "Color";
      var field = MapSettings.class.getField(fieldName);
      assertEquals(-1, field.getInt(settings));
      assertEquals(0xabcdef, settings.mobColor(entry.getKey(), 0xabcdef));
      int originalStyle = settings.radarColorStyle();
      field.setInt(settings, 0); settings.validate();
      assertEquals(0, settings.mobColor(entry.getKey(), 0xabcdef));
      assertNotEquals(originalStyle, settings.radarColorStyle());
      var restored = new Gson().fromJson(new Gson().toJson(settings), MapSettings.class);
      assertEquals(0, restored.mobColor(entry.getKey(), 0xabcdef));
      field.setInt(settings, -2); assertThrows(IllegalArgumentException.class, settings::validate);
      field.setInt(settings, 0x1000000); assertThrows(IllegalArgumentException.class, settings::validate);
      field.setInt(settings, -1);
    }
    assertEquals(0xabcdef, settings.mobColor("CustomMob", 0xabcdef));
    assertEquals(0xabcdef, settings.mobColor(null, 0xabcdef));
    settings.directionTextOpacity = Double.NaN;
    assertThrows(IllegalArgumentException.class, settings::validate);
  }
}
