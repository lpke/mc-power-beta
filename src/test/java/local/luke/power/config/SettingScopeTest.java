package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonPrimitive;
import java.util.List;
import org.junit.jupiter.api.Test;

class SettingScopeTest {
  private Setting setting(String id, String backend) {
    return new Setting(id, backend, "General", "Test", id, "Adjust the value.", Setting.Kind.INTEGER,
        new JsonPrimitive(1), new JsonPrimitive(1), 0, 4, 1, List.of(), true);
  }
  @Test void scopeFollowsPersistenceIncludingCommandOverrides() {
    for (String id : List.of("world.difficulty", "world.cheats", "world.daylightCycle", "world.weatherCycle", "worldedit.worldOverride"))
      assertTrue(SettingScope.perWorld(setting(id, "world")), id);
    for (String id : List.of("commands.world.help", "commands.worldMode.creative"))
      assertTrue(SettingScope.perWorld(setting(id, "commandAccess")), id);
    for (String id : List.of("commands.rule.help", "commands.mode.creative", "audio.preset", "worldedit.enabled", "tweaks.autoWalk"))
      assertFalse(SettingScope.perWorld(setting(id, "test")), id);
  }
  @Test void rangeScopeAndNoticesAreSeparateParagraphs() {
    assertEquals("Adjust the value.\n\nRange: 0 to 4.\n\nGlobal setting. Shared across worlds in this instance.\n\nUnavailable here.\n\nRestart required.",
        Tooltips.setting(setting("test", "test"), "Unavailable here."));
  }
}
