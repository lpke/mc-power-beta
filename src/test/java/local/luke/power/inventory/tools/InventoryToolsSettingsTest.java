package local.luke.power.inventory.tools;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class InventoryToolsSettingsTest {
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
}
