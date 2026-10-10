package local.luke.power;

import local.luke.power.config.ConfigAudit;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.*;

public final class PowerBeta {
  public static final Logger LOG = LogManager.getLogger("Power Beta");
  private static boolean ready;

  public static void tick(Minecraft mc) {
    var saveFailure = local.luke.power.storage.PowerConfig.takeSaveFailure();
    if (saveFailure != null) {
      LOG.error("Could not save gameplay settings; retaining changes for retry", saveFailure);
      if (mc.inGameHud != null)
        mc.inGameHud.addChatMessage("\u00a7cSettings could not be saved. Unsaved changes will be retried.\u00a7r");
    }
    local.luke.power.audio.AudioController.tick(mc);
    local.luke.power.light.LightOverlay.tick(mc);
    if (!ready && mc.options != null) {
      ready = true;
      local.luke.power.input.TweakIndicators.register(
          local.luke.power.input.TweakIndicators.Tweak.CINEMATIC_CAMERA,
          () -> mc.options.cinematicMode ? "" : null,
          () -> local.luke.power.status.StatusConfig.current().cinematicCameraMessages);
      local.luke.power.chat.HelpOutput.scrolling(() -> {
        try {
          Object config = Class.forName("local.luke.power.hud.Config").getField("config").get(null);
          return Boolean.TRUE.equals(config.getClass().getField("enableChatScroll").get(config));
        } catch (ReflectiveOperationException e) { return false; }
      });
      local.luke.power.permissions.CommandPermissions.context(() -> local.luke.power.commands.CommandContext.current(mc));
      local.luke.power.input.Bindings.availability(id ->
          !id.equals("power_creative.picker") && !id.equals("power_creative.modifier")
              || local.luke.power.permissions.CommandPermissions.cheatsEnabled());
      local.luke.power.video.VideoConfig.current();
      try {
        ConfigAudit.write(mc);
      } catch (Exception e) {
        LOG.error("Settings audit failed", e);
      }
    }
    local.luke.power.status.TweakMessages.tick(mc);
  }
}
