package local.luke.power.environment.mixin.client;

import local.luke.power.environment.Config;
import local.luke.power.environment.ModHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
	public void powerCompat_render(float bl, boolean i, int j, int par4, CallbackInfo ci)  {
		if (Config.config.MUSIC_CONFIG.overlayForMusicInDebug) {
			this.minecraft.textRenderer.drawWithShadow(
					"Music: " + ModHelper.ModHelperFields.currentBGM,
					2,
					(144 + Config.config.MUSIC_CONFIG.overlayForMusicInDebugYOffset),
					0xE0E0E0
			);
		}
	}
}
