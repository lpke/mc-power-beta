package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonPrimitive;
import java.util.List;
import local.luke.power.ui.SettingsDraft;
import org.junit.jupiter.api.Test;

class InheritedColorsTest {
  private Setting color(String id, int current, int fallback) {
    return Catalog.setting(id, "maps", "Interface", "Minimap", id, "", Setting.Kind.INTEGER,
        new JsonPrimitive(current), new JsonPrimitive(fallback), -1, 0xffffff, 1, List.of(), false);
  }

  @Test void inheritedMobColorsReadTheCurrentCategoryDraftAndRetainCustomBlack() {
    Setting category = color("maps.radarHostileColor", 0x123456, 0xff6655);
    Setting mob = color("maps.radarZombieColor", -1, -1);
    var settings = List.of(category, mob);
    assertTrue(InheritedColors.accepts(mob)); assertFalse(InheritedColors.accepts(category));
    assertFalse(InheritedColors.custom(mob)); assertEquals("Inherit", mob.display());
    category.value = new JsonPrimitive(3);
    InheritedColors.toggle(mob, settings);
    assertTrue(InheritedColors.custom(mob)); assertEquals(3, ColorSettings.rgb(mob));
    assertEquals("Custom", mob.display());
    ColorSettings.apply(mob, null, 0, 100);
    assertEquals(0, ColorSettings.rgb(mob)); assertTrue(InheritedColors.custom(mob));
    InheritedColors.toggle(mob, settings);
    assertEquals(-1, mob.value.getAsInt()); assertEquals("Inherit", mob.display());
    assertThrows(IllegalArgumentException.class, () -> ColorSettings.rgb(mob));
    assertThrows(IllegalArgumentException.class, () -> InheritedColors.inheritedRgb(mob, List.of(mob)));
  }

  @Test void nestedPickerAndModeChangesStayLocalAndShareUndoAndReset() {
    Setting category = color("maps.radarAnimalsColor", 0xffff55, 0xffff55);
    Setting mob = color("maps.radarWolfColor", -1, -1);
    var draft = new SettingsDraft(List.of(category, mob), setting -> true);
    var child = draft.get(mob.id);
    draft.checkpoint(); InheritedColors.toggle(child, draft.settings());
    ColorSettings.apply(child, null, 0x123456, 100);
    assertEquals(-1, mob.value.getAsInt());
    assertEquals(0x123456, child.value.getAsInt());
    draft.undo(); assertEquals(-1, child.value.getAsInt());
    draft.checkpoint(); InheritedColors.toggle(child, draft.settings());
    ColorSettings.reset(child, draft.settings());
    assertEquals(-1, child.value.getAsInt());
    assertEquals(-1, child.defaultValue.getAsInt());
    assertFalse(draft.changed());
  }
}
