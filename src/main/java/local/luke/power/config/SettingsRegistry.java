package local.luke.power.config;

import local.luke.power.config.backend.*;
import net.minecraft.client.Minecraft;

public final class SettingsRegistry {
  public static ConfigSession open(Minecraft mc) throws Exception {
    ConfigSession s = globals(mc);
    WorldBackend.register(s, mc);
    return s;
  }

  public static ConfigSession openMap(Minecraft mc) throws Exception {
    ConfigSession s = new ConfigSession();
    NativeBackend.register(s, mc); MapBackend.register(s); InterfaceBackend.register(s);
    return s;
  }

  public static ConfigSession openDefaults(Minecraft mc) throws Exception {
    ConfigSession s = globals(mc);
    RulePresetBackend.register(s);
    DefaultsBackend.register(s);
    return s;
  }

  private static ConfigSession globals(Minecraft mc) throws Exception {
    ConfigSession s = new ConfigSession();
    NativeBackend.register(s, mc);
    VideoBackend.register(s, mc);
    AudioBackend.register(s, mc);
    VisualBackend.register(s, mc);
    LightBackend.register(s);
    local.luke.power.heldlight.HeldLightBackend.register(s);
    MapBackend.register(s);
    SprintBackend.register(s);
    InterfaceBackend.register(s);
    StatusBackend.register(s);
    InfoHudBackend.register(s);
    ComponentBackend.register(s);
    GlassBackend.register(s);
    LogoBackend.register(s);
    DeathBackend.register(s);
    GameplayBackend.register(s);
    VeinBackend.register(s);
    local.luke.power.offhand.OffhandBackend.register(s);
    local.luke.power.inventory.tools.InventoryToolsBackend.register(s);
    local.luke.power.recipes.RecipeBookBackend.register(s);
    local.luke.power.subtitles.SubtitlesBackend.register(s);
    local.luke.power.world.WorldToolsBackend.register(s);
    s.settings().forEach(ItemSettings::attach);
    return s;
  }
}
