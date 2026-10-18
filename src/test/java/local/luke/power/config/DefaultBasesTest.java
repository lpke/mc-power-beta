package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.SettingsDraft;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultBasesTest {
  @TempDir Path game;
  private Object priorFile, priorBase, priorSetup;
  @org.junit.jupiter.api.BeforeEach void isolateProcessState() throws Exception {
    PowerConfig.flushPending();
    var file = PowerConfig.class.getDeclaredField("file"); file.setAccessible(true); priorFile = file.get(null); file.set(null, null);
    var base = DefaultBases.class.getDeclaredField("current"); base.setAccessible(true); priorBase = base.get(null);
    var setup = DefaultBases.class.getDeclaredField("setupComplete"); setup.setAccessible(true); priorSetup = setup.get(null);
  }
  @org.junit.jupiter.api.AfterEach void restoreProcessState() throws Exception {
    PowerConfig.flushPending();
    var file = PowerConfig.class.getDeclaredField("file"); file.setAccessible(true); file.set(null, priorFile);
    var base = DefaultBases.class.getDeclaredField("current"); base.setAccessible(true); base.set(null, priorBase);
    var setup = DefaultBases.class.getDeclaredField("setupComplete"); setup.setAccessible(true); setup.set(null, priorSetup);
  }
  @Test void changingBaseOnlyWritesMetadataAndRefreshesResetTargets() throws Exception {
    ConfigDefaults.prepare(game);
    JsonObject before = PowerConfig.document().getAsJsonObject("settings").deepCopy();
    Setting setting = new Setting("power_gold:config.enableGoldSwordLooting", "test", "", "", "Looting", "", Setting.Kind.BOOLEAN,
        new JsonPrimitive(true), new JsonPrimitive(true), 0, 1, 1, List.of(), false);
    try {
      DefaultBases.use(DefaultBases.Base.BETA, game);
      setting.refreshDefault();
      assertTrue(setting.value.getAsBoolean()); assertFalse(setting.changed()); assertFalse(setting.defaultValue.getAsBoolean());
      JsonObject after = PowerConfig.document().getAsJsonObject("settings").deepCopy();
      before.remove("defaults"); after.remove("defaults"); assertEquals(before, after);
      SettingsDraft nested = new SettingsDraft(List.of(setting), s -> true);
      DefaultBases.use(DefaultBases.Base.POWER_BETA, game);
      nested.settings().forEach(Setting::refreshDefault);
      assertTrue(nested.get(setting.id).defaultValue.getAsBoolean());
      assertTrue(nested.get(setting.id).value.getAsBoolean());
      DefaultBases.reload(); assertEquals(DefaultBases.Base.POWER_BETA, DefaultBases.current());
    } finally { DefaultBases.use(DefaultBases.Base.POWER_BETA, game); }
  }
  @Test void existingInstallsSkipSetupAndFreshInstallsAskOnce() throws Exception {
    ConfigDefaults.prepare(game); assertTrue(DefaultBases.needsSetup());
    var state = new DefaultBases.State(); state.setupComplete = true; PowerConfig.save("defaults", state);
    ConfigDefaults.prepare(game); assertFalse(DefaultBases.needsSetup());
    JsonObject doc = PowerConfig.document(); doc.getAsJsonObject("settings").remove("defaults"); PowerConfig.write(doc);
    ConfigDefaults.prepare(game); assertFalse(DefaultBases.needsSetup());
    assertTrue(PowerConfig.section("defaults").get("setupComplete").getAsBoolean());
  }
  @Test void refinedAndBetaKeepGameplayVanillaWhileFixesAndInventoryDiffer() {
    var yes = new JsonPrimitive(true);
    for (var base : List.of(DefaultBases.Base.BETA, DefaultBases.Base.REFINED)) {
      assertFalse(DefaultBases.value(base, "power_gold:config.enableGoldSwordLooting", yes).getAsBoolean());
      assertTrue(DefaultBases.value(base, "rules.survival.player_crop_trampling", new JsonPrimitive(false)).getAsBoolean());
      assertEquals(30, DefaultBases.value(base, "rules.cheats.explosion_drop_chance", new JsonPrimitive(100)).getAsInt());
    }
    assertFalse(DefaultBases.value(DefaultBases.Base.BETA, "power_controls:bugfixes.miningDelayFix", yes).getAsBoolean());
    assertTrue(DefaultBases.value(DefaultBases.Base.REFINED, "power_controls:bugfixes.miningDelayFix", new JsonPrimitive(false)).getAsBoolean());
    assertFalse(DefaultBases.value(DefaultBases.Base.BETA, "power_inventory:config.MODERN_MINECRAFT_CONFIG.EnableLeftClickDrag", yes).getAsBoolean());
    assertTrue(DefaultBases.value(DefaultBases.Base.REFINED, "power_inventory:config.MODERN_MINECRAFT_CONFIG.EnableLeftClickDrag", yes).getAsBoolean());
  }
}
