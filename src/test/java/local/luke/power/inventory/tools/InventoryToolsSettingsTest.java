package local.luke.power.inventory.tools;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import java.util.*;
import org.junit.jupiter.api.Test;

class InventoryToolsSettingsTest {
  @Test void automationActivationIsNotSavedOrRestoredFromLegacySettings() {
    var gson = new Gson();
    var settings = gson.fromJson("{\"restock\":true,\"toolSwitch\":true,\"restockThreshold\":3,\"toolsFromInventory\":false}", InventoryToolsSettings.class);
    assertFalse(settings.restock); assertFalse(settings.toolSwitch);
    assertEquals(3, settings.restockThreshold); assertFalse(settings.toolsFromInventory);
    settings.restock = settings.toolSwitch = true;
    var saved = gson.toJsonTree(settings).getAsJsonObject();
    assertFalse(saved.has("restock")); assertFalse(saved.has("toolSwitch"));
    assertEquals(3, saved.get("restockThreshold").getAsInt());
    assertFalse(saved.get("toolsFromInventory").getAsBoolean());
  }
  @Test void legacySettingsKeepAutomationOffAndStartWithAnUnrestrictedLayout() {
    var settings = new Gson().fromJson("{}", InventoryToolsSettings.class);
    settings.validate();
    assertFalse(settings.restock); assertFalse(settings.toolSwitch); assertFalse(settings.sortDescending);
    assertTrue(settings.protectedSlots.isEmpty()); assertTrue(settings.preferredItems.isEmpty());
  }
  @Test void layoutRoundTripsThroughTheGameGsonVersionWithIntegerSlotKeys() {
    var settings = new InventoryToolsSettings();
    settings.protectedSlots.add(9); settings.preferredItems.put(35, "35:4");
    var gson = new Gson(); var restored = gson.fromJson(gson.toJson(settings), InventoryToolsSettings.class);
    restored.validate();
    assertEquals(settings.protectedSlots, restored.protectedSlots);
    assertEquals("35:4", restored.preferredItems.get(35));
  }
  @Test void layoutRejectsEquipmentHotbarAndMalformedPreferences() {
    var settings = new InventoryToolsSettings(); settings.protectedSlots.add(0);
    assertThrows(IllegalArgumentException.class, settings::validate);
    settings.protectedSlots.clear(); settings.preferredItems.put(45, "1");
    assertThrows(IllegalArgumentException.class, settings::validate);
    settings.preferredItems.clear(); settings.preferredItems.put(9, "bad:item:data");
    assertThrows(IllegalArgumentException.class, settings::validate);
  }
  @Test void missingWhitelistKeepsAllToolsAndExplicitEmptyKeepsNoneAfterRestart() {
    Gson gson = new Gson();
    var legacy = gson.fromJson("{}", InventoryToolsSettings.class); legacy.validate();
    assertEquals(List.of("*"), legacy.toolWhitelist);
    legacy.toolWhitelist = new ArrayList<>();
    var reloaded = gson.fromJson(gson.toJson(legacy), InventoryToolsSettings.class); reloaded.validate();
    assertTrue(reloaded.toolWhitelist.isEmpty());
  }
  @Test void explicitWhitelistRejectsInvalidAndMixedWildcardValues() {
    var settings = new InventoryToolsSettings();
    settings.toolWhitelist = List.of("257", "278"); assertDoesNotThrow(settings::validate);
    for (var invalid : List.of(List.of("*", "278"), List.of("-1"), List.of("0"), List.of("278:4"))) {
      settings.toolWhitelist = invalid; assertThrows(IllegalArgumentException.class, settings::validate);
    }
  }
}
