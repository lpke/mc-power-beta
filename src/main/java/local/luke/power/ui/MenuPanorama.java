package local.luke.power.ui;

import local.luke.power.controls.ControlFeatures;
import local.luke.power.input.PanoramaCatalog;
import local.luke.power.input.PanoramaRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;

/** Shared menus use one monotonic animation clock and never render a second world. */
public final class MenuPanorama {
  private MenuPanorama() {}
  public static boolean enabled(Minecraft mc) {
    var settings = ControlFeatures.USER_INTERFACE_CONFIG.panoramaConfig;
    return mc.world == null && !(mc.currentScreen instanceof TitleScreen) && !settings.onlyTitleScreen
        && settings.enablePanorma && !PanoramaCatalog.folder(settings.panoramaFolder).isEmpty();
  }
  public static void render(Minecraft mc) {
    var settings = ControlFeatures.USER_INTERFACE_CONFIG.panoramaConfig;
    PanoramaRenderer.render(mc.textureManager::getTextureId, settings.panoramaFolder, settings.blurBackground,
        PanoramaRenderer.animationTicks(), mc.displayWidth, mc.displayHeight);
  }
}
