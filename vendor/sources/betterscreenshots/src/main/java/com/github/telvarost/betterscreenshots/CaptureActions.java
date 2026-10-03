package com.github.telvarost.betterscreenshots;

import local.luke.power.input.Bindings;
import local.luke.power.input.Chord;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** One press path shared by keyboard and mouse bindings. */
public final class CaptureActions {
    private CaptureActions() {}
    public static void press(int key) {
        Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
        if (mc.world == null || mc.currentScreen != null) return;
        if (Bindings.matches(KeyBindingListener.takeCustomResolutionScreenshot, key)) {
            String result = ModHelper.mainSaveCustomResolutionPhotoScreenshot(mc, Minecraft.getRunDirectory(),
                    mc.displayWidth, mc.displayHeight, Config.config.customResolutionPhotoWidth,
                    Config.config.customResolutionPhotoHeight, (Bindings.heldModifiers() & Chord.CTRL) != 0);
            mc.inGameHud.addChatMessage((result.startsWith("Failed") ? "\u00a7c" : "\u00a7a") + result + "\u00a7r");
        } else if (Bindings.matches(KeyBindingListener.takeIsometricScreenshot, key)) {
            mc.progressRenderer.progressStart("Taking isometric screenshot");
            new IsometricScreenshotRenderer(mc, Minecraft.getRunDirectory()).doRender();
        }
    }
}
