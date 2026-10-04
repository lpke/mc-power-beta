package local.luke.power;

import local.luke.power.config.ConfigAudit;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.*;

public final class PowerBeta {
  public static final Logger LOG = LogManager.getLogger("Power Beta");
  private static boolean ready;

  public static void tick(Minecraft mc) {
    local.luke.power.audio.AudioController.tick(mc);
    local.luke.power.light.LightOverlay.tick(mc);
    if (!ready && mc.options != null) {
      ready = true;
      try {
        ConfigAudit.write(mc);
      } catch (Exception e) {
        LOG.error("Settings audit failed", e);
      }
    }
  }
}
