package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonPrimitive;
import java.util.List;
import org.junit.jupiter.api.Test;

class SettingAccessTest {
  @Test void gatePresentationDoesNotOverwriteEnabledPreferences() {
    for (var kind : List.of(Setting.Kind.BOOLEAN, Setting.Kind.CHOICE, Setting.Kind.INTEGER, Setting.Kind.KEY)) {
      var value = kind == Setting.Kind.BOOLEAN ? new JsonPrimitive(true) : new JsonPrimitive(1);
      var setting = new Setting("worldedit.enabled", "worldedit", "World editing", "Access", "Enabled", "",
          kind, value, value, 0, 2, 1, List.of("Default", "Enabled", "Disabled"), false);
      assertEquals(kind == Setting.Kind.BOOLEAN ? "Off" : "Disabled", SettingAccess.lockedValue(setting));
      assertEquals(value, setting.value);
      assertFalse(setting.changed());
    }
  }
}
