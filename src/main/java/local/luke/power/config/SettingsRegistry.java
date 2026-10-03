package local.luke.power.config;

import local.luke.power.config.backend.*;
import net.minecraft.client.Minecraft;

public final class SettingsRegistry {
  public static ConfigSession open(Minecraft mc) throws Exception {
    ConfigSession s = new ConfigSession();
    NativeBackend.register(s, mc);
    AudioBackend.register(s, mc);
    ComponentBackend.register(s);
    GlassBackend.register(s);
    LogoBackend.register(s);
    WorldBackend.register(s, mc);
    return s;
  }
}
