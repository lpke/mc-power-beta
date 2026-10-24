package local.luke.power.compat.mixin.extradebuginfo;

import local.luke.power.compat.Config;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import local.luke.power.ui.DebugOverlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import local.luke.power.input.DetachedCamera;
import local.luke.power.input.ReportedPosition;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Duration;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class InGameHudMixin extends DrawContext {

	@Shadow
	private Minecraft minecraft;

	@Inject(method = "render", at = @At("TAIL"))
	public void power$debug(float bl, boolean i, int j, int par4, CallbackInfo ci)  {
		if (!minecraft.options.debugHud || minecraft.player == null || minecraft.world == null) return;
		int yOffset = Config.config.overlayAdditionsYOffset;
        boolean detached = DetachedCamera.isActive();
        var pose = ReportedPosition.current(minecraft.player,
            detached ? DetachedCamera.x() : minecraft.player.x,
            detached ? local.luke.power.input.HeadGeometry.baseY(DetachedCamera.y()) : minecraft.player.boundingBox.minY,
            detached ? DetachedCamera.z() : minecraft.player.z,
            detached ? DetachedCamera.facingYaw() : minecraft.player.yaw, minecraft.player.pitch);
        double x = pose.x(), y = pose.y(), z = pose.z();
        float yaw = pose.yaw();
        if (Config.config.addFacingToDebugOverlay) {
            String[] directions = {"south (+Z)", "west (-X)", "north (-Z)", "east (+X)"};
            int facing = Math.floorMod((int)Math.floor(yaw / 90.0 + 0.5), 4);
            DebugOverlay.add("Facing: " + directions[facing], 2, 144 + yOffset);
        }

		if (Config.config.addTotalPlayTimeToDebugOverlay) {
			long realHoursPlayed = Duration.ofSeconds(minecraft.stats.get(Stats.PLAY_ONE_MINUTE) / 20).toHours();
			if (1 == realHoursPlayed) {
				DebugOverlay.add("Play Time: " + realHoursPlayed + " hour", 2, (96 + yOffset));
			} else {
				DebugOverlay.add("Play Time: " + realHoursPlayed + " hours", 2, (96 + yOffset));
			}
		}

		if (  Config.config.addBlockLightToDebugOverlay
		   || Config.config.addBiomeToDebugOverlay
		   || Config.config.addDayCounterToDebugOverlay
           || Config.config.addSlimeChunkToDebugOverlay
		) {
            String lightLevel = "Unknown", biomeName = "Unknown", slimeChunk = "Unknown";
            long dayCount = Math.floorDiv(((local.luke.power.world.WorldCycles)
                minecraft.world.getProperties()).power$daylightTime(), 24000L);
            if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Math.abs(x) <= 30_000_000 && Math.abs(z) <= 30_000_000) {
                int bx = MathHelper.floor(x), by = MathHelper.floor(y), bz = MathHelper.floor(z);
                // Detached cameras can leave loaded terrain. Never load a chunk for HUD text.
                if (minecraft.world.isPosLoaded(bx, 64, bz)) {
                    Chunk chunk = minecraft.world.getChunkFromPos(bx, bz);
                    if (Config.config.addSlimeChunkToDebugOverlay)
                        slimeChunk = Boolean.toString(chunk.getSlimeRandom(987234911L).nextInt(10) == 0);
                    if (Config.config.addBlockLightToDebugOverlay && by >= 0 && by < 128)
                        lightLevel = Integer.toString(minecraft.world.getBrightness(net.minecraft.world.LightType.BLOCK, bx, by, bz));
                }
                if (Config.config.addBiomeToDebugOverlay) {
                    Biome biome = minecraft.world.method_1781().getBiome(bx, bz);
                    if (biome != null) biomeName = biome.name;
                }
            }

			if (Config.config.addBlockLightToDebugOverlay) {
				DebugOverlay.add("Block light: " + lightLevel, 2, (112 + yOffset));
			}

			if (Config.config.addBiomeToDebugOverlay) {
				DebugOverlay.add("Biome: " + biomeName, 2, (120 + yOffset));
			}

			if (Config.config.addDayCounterToDebugOverlay) {
				DebugOverlay.add("Day: " + dayCount, 2, (128 + yOffset));
			}

			if (Config.config.addSlimeChunkToDebugOverlay) {
				DebugOverlay.add("Slime Chunk: " + slimeChunk, 2, (136 + yOffset));
			}
		}
	}
}
