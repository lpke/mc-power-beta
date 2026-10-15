package local.luke.power.storage;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigCleanupTest {
  private JsonObject convert(String settings) {
    return ConfigVersions.convert(JsonParser.parseString("{\"schemaVersion\":3,\"settings\":" + settings + "}").getAsJsonObject());
  }
  @Test void removingAvailabilitySwitchesDoesNotActivatePreviouslyDisabledBoundActions() {
    JsonObject root = convert("{\"building\":{\"autoWalk\":false,\"autoMine\":true},\"native\":{\"key_Auto-walk (toggle)\":\"19\",\"key_Auto-mine (toggle)\":\"50\"},\"bindings\":{\"Auto-walk (toggle)\":1}}");
    JsonObject settings = root.getAsJsonObject("settings"), keys = settings.getAsJsonObject("native");
    assertEquals("0", keys.get("key_Auto-walk (toggle)").getAsString());
    assertEquals("50", keys.get("key_Auto-mine (toggle)").getAsString());
    assertFalse(settings.getAsJsonObject("bindings").has("Auto-walk (toggle)"));
    assertFalse(settings.getAsJsonObject("building").has("autoWalk"));
  }
  @Test void frozenWorldEvidenceSurvivesRemovingLiveGlobalSwitches() {
    JsonObject root = convert("{\"gameplay\":{\"fireSpread\":false,\"torchShovel\":true}}");
    assertFalse(root.getAsJsonObject("legacyWorldRules").getAsJsonObject("gameplay").get("fireSpread").getAsBoolean());
    assertFalse(root.getAsJsonObject("settings").getAsJsonObject("gameplay").has("fireSpread"));
    assertTrue(root.getAsJsonObject("settings").getAsJsonObject("gameplay").get("torchShovel").getAsBoolean());
    assertEquals(root, ConfigVersions.convert(root));
  }
  @Test void dormantRecipesRemainDisabledWhileIndependentToolRepairIsKept() {
    JsonObject root = convert("{\"power_controls:recipes\":{\"enableRecipes\":false,\"toolRepair\":true,\"modern\":{\"snowLayerRecipe\":true},\"tweaked\":{\"stairsPerCraft\":8}}}");
    JsonObject recipes = root.getAsJsonObject("settings").getAsJsonObject("power_controls:recipes");
    assertFalse(recipes.has("enableRecipes"));
    assertTrue(recipes.get("toolRepair").getAsBoolean());
    assertFalse(recipes.getAsJsonObject("modern").get("snowLayerRecipe").getAsBoolean());
    assertEquals(4, recipes.getAsJsonObject("tweaked").get("stairsPerCraft").getAsInt());
  }
  @Test void graphAndCommandBindingsPreserveDisabledChoices() {
    JsonObject settings = convert("{\"visual\":{\"slashChat\":true},\"power_client_fixes:config\":{\"enableDebugGraphChanges\":false}}").getAsJsonObject("settings");
    assertEquals("53", settings.getAsJsonObject("native").get("key_powerbeta.openCommands").getAsString());
    assertEquals("0", settings.getAsJsonObject("native").get("key_powerbeta.debugGraph").getAsString());
    assertFalse(settings.getAsJsonObject("bindings").has("powerbeta.debugGraph"));
  }
}
