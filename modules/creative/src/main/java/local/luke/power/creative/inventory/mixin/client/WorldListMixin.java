package local.luke.power.creative.inventory.mixin.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.menu.SelectLevelScreen;
import net.minecraft.client.render.Tessellator;
import net.minecraft.level.storage.LevelMetadata;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(targets = "net.minecraft.client.gui.screen.menu.SelectLevelScreen$LevelList")
public abstract class WorldListMixin {
	@SuppressWarnings("target") // Parent class
	@Shadow @Final SelectLevelScreen field_2444;
	
	@Inject(method = "renderEntry", at = @At(
		value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screen/menu/SelectLevelScreen;drawTextWithShadow(Lnet/minecraft/client/render/TextRenderer;Ljava/lang/String;III)V",
		ordinal = 2,
		shift = Shift.AFTER
	), locals = LocalCapture.CAPTURE_FAILSOFT)
	protected void creative_renderEntry(
		int i, int x, int y, int l, Tessellator arg, CallbackInfo info,
		LevelMetadata meta, String levelName, String var8, long var9, String var11
	) {
		@SuppressWarnings("deprecation")
		Minecraft minecraft = (Minecraft) FabricLoader.getInstance().getGameInstance();
    if (((local.luke.power.permissions.CheatWorld) meta).power$cheatsEnabled()) {
      int offset = minecraft.textRenderer.getTextWidth(levelName);
      field_2444.drawTextWithShadow(minecraft.textRenderer, "  [§bCheats§r]", x + offset, y + 1, 0xAAAAAA);
    }
	}
}
