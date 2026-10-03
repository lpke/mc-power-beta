package com.github.telvarost.unitweakstelsaddons.mixin.extradebuginfo;

import com.github.telvarost.unitweakstelsaddons.Config;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.modificationstation.stationapi.api.entity.player.PlayerHelper;
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

	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/font/TextRenderer;drawWithShadow(Ljava/lang/String;III)V"
			)
	)
	public void uniTweaksTelsAddons_render(float bl, boolean i, int j, int par4, CallbackInfo ci)  {
		TextRenderer var8 = this.minecraft.textRenderer;
		int yOffset = Config.config.overlayAdditionsYOffset;

		if (Config.config.addTotalPlayTimeToDebugOverlay) {
			long realHoursPlayed = Duration.ofSeconds(minecraft.stats.get(Stats.PLAY_ONE_MINUTE) / 20).toHours();
			if (1 == realHoursPlayed) {
				var8.drawWithShadow("Play Time: " + realHoursPlayed + " hour", 2, (96 + yOffset), 14737632);
			} else {
				var8.drawWithShadow("Play Time: " + realHoursPlayed + " hours", 2, (96 + yOffset), 14737632);
			}
		}

		if (  Config.config.addLightLevelToDebugOverlay
		   || Config.config.addBiomeToDebugOverlay
		   || Config.config.addDayCounterToDebugOverlay
		) {
			PlayerEntity player = PlayerHelper.getPlayerFromGame();
			int lightLevel = 0;
			String biomeName = "Unknown";
			long dayCount = 0;
			boolean isSlimeChunk = false;

			if (null != player) {
				float light = player.getBrightnessAtEyes(1.0F);

				if (light < 0.06) {
					lightLevel = 0;
				} else if (light < 0.07) {
					lightLevel = 1;
				} else if (light < 0.09) {
					lightLevel = 2;
				} else if (light < 0.125) {
					lightLevel = 3;
				} else if (light < 0.14) {
					lightLevel = 4;
				} else if (light < 0.16) {
					lightLevel = 5;
				} else if (light < 0.2) {
					lightLevel = 6;
				} else if (light < 0.25) {
					lightLevel = 7;
				} else if (light < 0.3) {
					lightLevel = 8;
				} else if (light < 0.325) {
					lightLevel = 9;
				} else if (light < 0.4) {
					lightLevel = 10;
				} else if (light < 0.5) {
					lightLevel = 11;
				} else if (light < 0.625) {
					lightLevel = 12;
				} else if (light < 0.75) {
					lightLevel = 13;
				} else if (light < 0.875) {
					lightLevel = 14;
				} else {
					lightLevel = 15;
				}

				if (null != player.world) {
					Chunk chunk = player.world.getChunkFromPos(MathHelper.floor(player.x), MathHelper.floor(player.z));
					isSlimeChunk = (chunk.getSlimeRandom(987234911L).nextInt(10) == 0);

					if (null != player.world.getProperties()) {
						dayCount = (int) Math.floor(player.world.getProperties().getTime() / 24000);
					}

					if (null != player.world.method_1781()) {
						Biome biome = player.world.method_1781().getBiome((int)Math.floor(player.x), (int)Math.floor(player.z));
						if (null != biome) {
							biomeName = biome.name;
						}
					}
				}
			}

			if (Config.config.addLightLevelToDebugOverlay) {
				var8.drawWithShadow("Light Level: " + lightLevel, 2, (112 + yOffset), 14737632);
			}

			if (Config.config.addBiomeToDebugOverlay) {
				var8.drawWithShadow("Biome: " + biomeName, 2, (120 + yOffset), 14737632);
			}

			if (Config.config.addDayCounterToDebugOverlay) {
				var8.drawWithShadow("Day: " + dayCount, 2, (128 + yOffset), 14737632);
			}

			if (Config.config.addSlimeChunkToDebugOverlay) {
				var8.drawWithShadow("Slime Chunk: " + isSlimeChunk, 2, (136 + yOffset), 14737632);
			}
		}
	}
}
