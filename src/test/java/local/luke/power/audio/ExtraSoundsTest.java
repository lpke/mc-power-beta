package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;

class ExtraSoundsTest {
  @Test void extraSoundRowsPreviewRegisteredSoundsWithoutChangingTheirEnablement() {
    assertEquals("step.stone", ExtraSounds.previewId("audio.redstonePlacementSound"));
    for (String setting : List.of("seedPlantingSounds", "hoeTillingSounds", "bucketSounds", "boneMealSounds"))
      assertTrue(ExtraSounds.contains(ExtraSounds.previewId("audio." + setting)), setting);
    for (String key : List.of("eatingSounds", "shearingSounds", "toolBreakSounds", "armorBreakSounds", "moreSoundsChests"))
      assertTrue(BetaSounds.contains(ExtraSounds.previewId("power_controls:features." + key)), key);
    assertNull(ExtraSounds.previewId("audio.master"));
    var settings = new AudioSettings();
    assertFalse(settings.seedPlantingSounds || settings.hoeTillingSounds || settings.bucketSounds || settings.boneMealSounds);
  }
  @Test void optionalActionsKeepOffFallbacksAndRoundTripWithGameGson() {
    var gson = new Gson();
    AudioSettings settings = gson.fromJson("{}", AudioSettings.class);
    assertFalse(settings.seedPlantingSounds);
    assertFalse(settings.hoeTillingSounds);
    assertFalse(settings.bucketSounds);
    assertFalse(settings.boneMealSounds);
    settings.seedPlantingSounds = settings.hoeTillingSounds = settings.bucketSounds = settings.boneMealSounds = true;
    AudioSettings restored = gson.fromJson(gson.toJson(settings), AudioSettings.class);
    assertTrue(restored.seedPlantingSounds && restored.hoeTillingSounds && restored.bucketSounds && restored.boneMealSounds);
    assertDoesNotThrow(restored::validate);
  }

  @Test void officialAssetsAreCompleteUnchangedAndRegisteredUnderTheModNamespace() throws Exception {
    try (var input = getClass().getResourceAsStream("/assets/powerbeta/extra-sounds.json")) {
      assertNotNull(input);
      var manifest = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
      Set<String> expected = Set.of("item.crop.plant", "item.hoe.till", "item.bucket.fill", "item.bucket.fill_lava",
          "item.bucket.empty", "item.bucket.empty_lava", "entity.cow.milk", "item.bone_meal.use");
      assertEquals(expected, manifest.getAsJsonObject("events").keySet());
      Map<String, Set<String>> assets = new HashMap<>();
      for (var value : manifest.getAsJsonArray("assets")) {
        var asset = value.getAsJsonObject();
        String event = asset.get("event").getAsString(), file = asset.get("file").getAsString();
        assertTrue(ExtraSounds.contains("power_beta:" + event));
        assertTrue(file.startsWith("assets/power_beta/stationapi/sounds/sound/"));
        String pool = file.substring("assets/power_beta/stationapi/sounds/sound/".length())
            .replaceFirst("[0-9]+\\.ogg$", "").replace('/', '.');
        assertEquals(event, pool);
        try (var stream = getClass().getResourceAsStream("/" + file)) {
          assertNotNull(stream, file);
          byte[] bytes = stream.readAllBytes();
          assertEquals(asset.get("size").getAsInt(), bytes.length);
          assertEquals(asset.get("sha1").getAsString(), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes)));
          assertEquals("OggS", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
        }
        assets.computeIfAbsent(event, key -> new HashSet<>()).add(asset.get("asset").getAsString());
      }
      for (String event : expected) {
        Set<String> official = new HashSet<>();
        for (var value : manifest.getAsJsonObject("events").getAsJsonObject(event).getAsJsonArray("sounds")) {
          String name = value.isJsonPrimitive() ? value.getAsString() : value.getAsJsonObject().get("name").getAsString();
          official.add("minecraft/sounds/" + name + ".ogg");
        }
        assertEquals(official, assets.get(event), event);
      }
      assertEquals(30, manifest.getAsJsonArray("assets").size());
    }
    assertFalse(ExtraSounds.contains("power_beta:unknown"));
  }

  @Test void actionSoundsUseTheAdvertisedCategoriesAndAllVolumeControls() {
    var settings = new AudioSettings();
    settings.master = 50;
    settings.categories.put("blocks", 20);
    settings.categories.put("passive", 40);
    settings.sounds.put("power_beta:item.crop.plant", 25);
    assertEquals(.025, settings.gain("power_beta:item.crop.plant", false), 1e-9);
    assertEquals(.1, settings.gain("power_beta:item.hoe.till", false), 1e-9);
    assertEquals(.1, settings.gain("power_beta:item.bucket.fill_lava", false), 1e-9);
    assertEquals(.1, settings.gain("power_beta:item.bone_meal.use", false), 1e-9);
    assertEquals(.2, settings.gain("power_beta:entity.cow.milk", false), 1e-9);
  }
}
