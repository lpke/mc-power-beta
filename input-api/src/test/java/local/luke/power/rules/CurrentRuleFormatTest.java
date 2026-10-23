package local.luke.power.rules;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CurrentRuleFormatTest {
  @Test void templatesAndRuleReadersRejectRetiredAndFutureVersionsWithoutChangingData() {
    for (int version : new int[]{0, 1, 2, 3, 5, 99}) {
      JsonObject document = RuleSet.cheats().document(); document.addProperty("version", version);
      JsonObject original = document.deepCopy();
      assertThrows(IllegalArgumentException.class, () -> RuleSet.read(document, new RuleSet()));
      assertThrows(IllegalArgumentException.class, () -> TemplateDocuments.read(document, false));
      assertThrows(IllegalArgumentException.class, () -> TemplateDocuments.read(document, true));
      assertEquals(original, document);
    }
  }
  @Test void currentRuleFormatRejectsRetiredAliasesAndPreservesCanonicalRules() {
    assertNull(GameRule.find("sleep_ambushes")); assertNull(GameRule.find("sleepAmbushes"));
    assertSame(GameRule.SLEEP_NIGHTMARES, GameRule.find("sleep_nightmares"));
    assertSame(GameRule.SLEEP_NIGHTMARES, GameRule.find("sleepNightmares"));
    JsonObject document = new RuleSet().document(); document.getAsJsonObject("values").addProperty("sleep_ambushes", true);
    assertThrows(IllegalArgumentException.class, () -> RuleSet.read(document, new RuleSet()));
    RuleSet expected = RuleSet.cheats(); expected.set(GameRule.SLEEP_NIGHTMARES, false);
    assertEquals(expected, TemplateDocuments.read(expected.document(), true));
  }
}
