package local.luke.power.storage;

import com.google.gson.*;
import local.luke.power.rules.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SprintRuleConfigTest {
  @Test void olderTemplatesRemainOpaqueAndCannotAuthorizeCreation() {
    JsonObject source = JsonParser.parseString("{\"schemaVersion\":8,\"settings\":{\"gameRules\":{\"survival\":{\"version\":3,\"values\":{\"allow_creative_mode\":true}}}},\"legacyWorldRules\":{\"keep\":true}}").getAsJsonObject();
    assertEquals(source, ConfigVersions.convert(source));
    JsonObject retained = ConfigVersions.convert(source).getAsJsonObject("settings").getAsJsonObject("gameRules").getAsJsonObject("survival");
    assertThrows(IllegalArgumentException.class, () -> TemplateDocuments.read(retained, false));
  }
  @Test void explicitSprintPresetsSurviveNormalizationAndGlobalPreferenceChanges() {
    RuleSet survival = new RuleSet();
    survival.set(GameRule.ALLOW_SPRINTING, true);
    JsonObject settings = new JsonObject(), presets = new JsonObject(), source = new JsonObject();
    presets.add("survival", survival.document());
    presets.add("cheats", RuleSet.cheats().document());
    settings.add("gameRules", presets);
    source.addProperty("schemaVersion", ConfigVersions.CURRENT);
    source.add("settings", settings);
    for (boolean enabled : new boolean[]{true, false, true}) {
      JsonObject sprint = new JsonObject();
      sprint.addProperty("enabled", enabled);
      settings.add("sprint", sprint);
      JsonObject persisted = ConfigVersions.convert(source).getAsJsonObject("settings").getAsJsonObject("gameRules");
      assertTrue(RuleSet.read(persisted.getAsJsonObject("survival"), new RuleSet()).enabled(GameRule.ALLOW_SPRINTING));
      assertFalse(RuleSet.read(persisted.getAsJsonObject("cheats"), RuleSet.cheats()).enabled(GameRule.ALLOW_SPRINTING));
    }
  }
}
