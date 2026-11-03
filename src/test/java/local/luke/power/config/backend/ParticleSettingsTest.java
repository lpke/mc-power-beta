package local.luke.power.config.backend;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ParticleSettingsTest {
  private static final String PREFIX = "power_compat:config.PARTICLES_CONFIG.";
  @TempDir Path game;

  @Test void allParticleDefaultsDisplayOnWhileTheirStoredValuesStayUnchanged() throws Exception {
    JsonObject particles;
    try (var input = getClass().getResourceAsStream("/assets/powerbeta/defaults/power-beta.json")) {
      assertNotNull(input);
      particles = JsonParser.parseReader(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8))
          .getAsJsonObject().getAsJsonObject("settings").getAsJsonObject("power_compat:config").getAsJsonObject("PARTICLES_CONFIG");
    }
    assertEquals(17, particles.size());
    for (var entry : particles.entrySet()) {
      String key = PREFIX + entry.getKey();
      boolean positive = entry.getKey().equals("blockBreakingParticles");
      assertEquals(positive, entry.getValue().getAsBoolean(), key);
      JsonElement shown = GlassValues.encode(key, entry.getValue().getAsBoolean());
      assertTrue(shown.getAsBoolean(), key);
      JsonObject metadata = Catalog.metadata(key);
      assertFalse(metadata.get("label").getAsString().startsWith("Disable"), key);
      assertTrue(metadata.get("default").getAsBoolean(), key);
      for (var base : DefaultBases.Base.values())
        assertTrue(DefaultBases.value(base, key, shown).getAsBoolean(), base.id + ": " + key);
      assertEquals(entry.getValue().getAsBoolean(), GlassValues.decode(key, shown, Boolean.class), key);
    }
    assertEquals("All particles", Catalog.metadata(PREFIX + "disableAllParticles").get("label").getAsString());
  }

  @Test void inversionIsLimitedToNegativeParticleBooleansAndExistingInversions() {
    assertFalse(GlassValues.encode(PREFIX + "disableWaterBubbleParticle", true).getAsBoolean());
    assertTrue(GlassValues.encode(PREFIX + "disableWaterBubbleParticle", false).getAsBoolean());
    assertFalse(GlassValues.encode(PREFIX + "blockBreakingParticles", false).getAsBoolean());
    assertFalse(GlassValues.encode("unrelated.disableAllParticles", false).getAsBoolean());
    assertFalse(GlassValues.encode("power_compat:config.disableOtherFeature", false).getAsBoolean());
    assertEquals(7, GlassValues.encode(PREFIX + "disableNumeric", 7).getAsInt());
    assertEquals(7, GlassValues.decode(PREFIX + "disableNumeric", new JsonPrimitive(7), Integer.class));
    assertFalse(GlassValues.encode("entityculling:config.disableEntityCulling", true).getAsBoolean());
    assertTrue((Boolean)GlassValues.decode("entityculling:config.disableEntityCulling", new JsonPrimitive(false), Boolean.class));
  }

  @Test void mixedExistingFlagsRoundTripThroughPreviewCancelApplyAndReload() throws Exception {
    var stored = new LinkedHashMap<String, Boolean>();
    stored.put(PREFIX + "disableAllParticles", true);
    stored.put(PREFIX + "disableWaterBubbleParticle", false);
    stored.put(PREFIX + "disableFireSmokeParticle", true);
    stored.put(PREFIX + "blockBreakingParticles", false);
    Fixture backend = new Fixture(game.resolve("particles.json"), stored);
    byte[] original = Files.readAllBytes(backend.file);
    ConfigSession session = backend.open();
    assertFalse(find(session, "disableAllParticles").value.getAsBoolean());
    assertTrue(find(session, "disableWaterBubbleParticle").value.getAsBoolean());
    assertFalse(find(session, "disableFireSmokeParticle").value.getAsBoolean());
    assertFalse(find(session, "blockBreakingParticles").value.getAsBoolean());
    assertEquals(0, session.changes());
    for (Setting setting : session.settings()) setting.value = new JsonPrimitive(!setting.value.getAsBoolean());
    session.preview();
    stored.forEach((key, value) -> assertEquals(!value, backend.live.get(key), key));
    assertArrayEquals(original, Files.readAllBytes(backend.file));
    session.discard();
    assertEquals(stored, backend.live);
    assertEquals(0, session.changes());
    assertArrayEquals(original, Files.readAllBytes(backend.file));

    find(session, "disableAllParticles").value = new JsonPrimitive(true);
    find(session, "disableFireSmokeParticle").value = new JsonPrimitive(true);
    find(session, "blockBreakingParticles").value = new JsonPrimitive(true);
    session.preview(); session.save(game);
    var expected = new LinkedHashMap<>(stored);
    expected.put(PREFIX + "disableAllParticles", false);
    expected.put(PREFIX + "disableFireSmokeParticle", false);
    expected.put(PREFIX + "blockBreakingParticles", true);
    assertEquals(expected, backend.live);
    JsonObject persisted = JsonParser.parseString(Files.readString(backend.file)).getAsJsonObject();
    expected.forEach((key, value) -> assertEquals(value, persisted.get(key).getAsBoolean(), key));
    assertEquals(expected.keySet(), persisted.keySet());
    for (Setting setting : session.settings()) setting.value = new JsonPrimitive(false);
    session.preview(); session.discard();
    assertEquals(expected, backend.live);
    ConfigSession reopened = backend.open();
    for (Setting setting : reopened.settings()) assertTrue(setting.value.getAsBoolean(), setting.id);
  }

  private static Setting find(ConfigSession session, String field) {
    return session.settings().stream().filter(setting -> setting.id.equals(PREFIX + field)).findFirst().orElseThrow();
  }

  private static final class Fixture implements Backend {
    private final Path file;
    private final Map<String, Boolean> live = new LinkedHashMap<>();
    Fixture(Path file, Map<String, Boolean> saved) throws Exception {
      this.file = file;
      live.putAll(saved);
      Files.writeString(file, Catalog.JSON.toJson(live));
    }
    ConfigSession open() throws Exception {
      JsonObject saved = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
      List<Setting> entries = new ArrayList<>();
      for (var entry : saved.entrySet()) {
        live.put(entry.getKey(), entry.getValue().getAsBoolean());
        entries.add(new Setting(entry.getKey(), id(), "Video", "Particles", "", "", Setting.Kind.BOOLEAN,
            GlassValues.encode(entry.getKey(), entry.getValue().getAsBoolean()), new JsonPrimitive(true), 0, 1, 1, List.of(), false));
      }
      ConfigSession session = new ConfigSession(); session.add(this, entries); return session;
    }
    public String id() { return "particle-fixture"; }
    public List<Path> files() { return List.of(file); }
    public void validate(Map<String, JsonElement> values) {}
    public boolean previews(Setting setting) { return true; }
    public void preview(Map<String, JsonElement> values) {
      values.forEach((key, value) -> live.put(key, (Boolean)GlassValues.decode(key, value, Boolean.class)));
    }
    public void apply(Map<String, JsonElement> values) throws Exception {
      preview(values);
      Files.writeString(file, Catalog.JSON.toJson(live));
    }
  }
}
