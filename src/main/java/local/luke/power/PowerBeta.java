package local.luke.power;

import local.luke.power.config.ConfigAudit;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.*;

public final class PowerBeta {
  public static final Logger LOG = LogManager.getLogger("Power Beta");
  private static boolean ready;

  public static void tick(Minecraft mc) {
    local.luke.power.debug.DebugKeys.tick(mc);
    if (local.luke.power.world.WorldLoadGuard.tick(mc)) return;
    local.luke.power.ui.TextDrag.context(mc.currentScreen, org.lwjgl.opengl.Display.isActive());
    var saveFailure = local.luke.power.storage.PowerConfig.takeSaveFailure();
    if (saveFailure != null) {
      LOG.error("Could not save gameplay settings; retaining changes for retry", saveFailure);
      if (mc.inGameHud != null)
        mc.inGameHud.addChatMessage("\u00a7cSettings could not be saved. Unsaved changes will be retried.\u00a7r");
    }
    local.luke.power.inventory.tools.InventoryAutomation.tick(mc);
    local.luke.power.subtitles.Subtitles.tick(mc);
    local.luke.power.chat.PlayerChat.tick(mc);
    local.luke.power.audio.AudioController.tick(mc);
    local.luke.power.light.LightOverlay.tick(mc);
    local.luke.power.render.RenderControls.tick(mc);
    local.luke.power.hud.InfoHud.tick(mc);
    local.luke.power.map.Maps.tick(mc);
    local.luke.power.sprint.SprintController.tick(mc);
    local.luke.power.mining.VeinMining.tick(mc);
    if (!ready && mc.options != null) {
      ready = true;
      local.luke.power.input.SpectatorContainers.register(() -> local.luke.power.inventory.SpectatorContainerScreen.open(mc));
      local.luke.power.inventory.tools.InventoryAutomation.registerIndicators(mc);
      local.luke.power.input.ReportedPosition.register(() -> mc.player, () -> {
        if (mc.player == null || mc.world == null) return null;
        boolean detached = local.luke.power.input.DetachedCamera.isActive();
        return new local.luke.power.input.ReportedPosition.Pose(
            detached ? local.luke.power.input.DetachedCamera.x() : mc.player.x,
            local.luke.power.hud.HudCoordinates.y(mc.player),
            detached ? local.luke.power.input.DetachedCamera.z() : mc.player.z,
            detached ? local.luke.power.input.DetachedCamera.facingYaw() : mc.player.yaw,
            detached ? (float)local.luke.power.input.DetachedCamera.pitch() : mc.player.pitch);
      });
      local.luke.power.input.CameraView.configure(
          () -> local.luke.power.visual.VisualConfig.current().thirdPersonDistance,
          local.luke.power.controls.util.ModOptions::isFrontViewEnabled);
      local.luke.power.input.TweakLifecycle.register("interface", () -> {
        local.luke.power.subtitles.Subtitles.leave();
        local.luke.power.inventory.NativeInventoryPlan.cancel();
        local.luke.power.inventory.tools.InventoryAutomation.clear();
        local.luke.power.sprint.SprintController.reset();
        local.luke.power.mining.VeinMining.reset();
        mc.options.cinematicMode = false;
        local.luke.power.render.RenderControls.reset(mc);
        if (local.luke.power.light.LightConfig.current().enabled) local.luke.power.light.LightConfig.toggle();
      });
      local.luke.power.death.DeathCommands.register();
      local.luke.power.map.WaypointCommands.register();
      local.luke.power.input.SpectatorTarget.register(() -> local.luke.power.inventory.InteractionTargets.spectator(mc));
      local.luke.power.commands.GameRuleCommand.register();
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
          local.luke.power.map.MapKeys.available(id)
              && local.luke.power.inventory.tools.InventoryFeatureKeys.available(mc, id)
              && (!id.equals("powerbeta.sprint") || local.luke.power.sprint.SprintController.allowed(mc))
              && (!id.equals("power_creative.picker") && !id.equals("power_creative.modifier")
              || (local.luke.power.permissions.CommandPermissions.allowed(local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE)
                  || local.luke.power.permissions.CommandPermissions.allowed(local.luke.power.rules.GameRule.ALLOW_SPECTATOR_MODE))));
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
