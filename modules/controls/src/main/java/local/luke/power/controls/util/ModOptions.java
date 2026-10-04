package local.luke.power.controls.util;

import local.luke.power.controls.ControlFeatures;
import local.luke.power.controls.tweaks.frontviewthirdperson.FrontViewMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.Option;
import org.lwjgl.input.Keyboard;

public class ModOptions {
    public static Option fogDensityOption;
    public static Option cloudsOption;
    public static Option fovOption;
    public static Option cloudHeightOption;
    public static Option fpsLimitOption;
    public static Option renderDistanceOption;
    public static Option brightnessOption;
    public static Option guiScaleOption;

    // Spawn Protection
    public static int spawnProtection = 16;

    // Front View Third Person
    public static boolean frontView = false;

    public static boolean isFrontViewEnabled() {
        return ModOptions.frontView || (ControlFeatures.USER_INTERFACE_CONFIG.frontViewThirdPerson == FrontViewMode.HIDE_HUD && (Minecraft.INSTANCE.options.thirdPerson && Minecraft.INSTANCE.options.hideHud));
    }

    // GUI Scale
    public static float guiScale = 0.0F;
    public static float realGuiScale = 0.0F;

    public static int getGuiScale() {
        return (int) Math.floor(realGuiScale * 8);
    }

    public static int getGuiScaleDisplayValue() {
        return (int) Math.floor(guiScale * 8);
    }

    // Brightness
    public static float brightness = 0.0F;

    public static void updateWorldLightTable(Minecraft minecraft) {
        if (minecraft == null || minecraft.world == null || minecraft.world.dimension == null) {
            return;
        }

        float brightness = ModOptions.brightness;
        float[] lightLevels = minecraft.world.dimension.lightLevelToLuminance;
        float minimumLevel = 0.05F;

        if (minecraft.world.dimension.isNether) {
            minimumLevel = 0.1F + brightness * 0.15F;
        }

        float k = 3.0f * (1.0F - brightness);
        for (int level = 0; level <= 15; ++level) {
            float var3 = 1.0F - (float) level / 15.0f;
            lightLevels[level] = (1.0F - var3) / (var3 * k + 1.0F) * (1.0F - minimumLevel) + minimumLevel;
        }

        minecraft.worldRenderer.reload();
    }


    // Render Distance
    public static float renderDistance = 0.2F;

    private static final int maxRenderDistance = 32;

    public static int getRenderDistanceChunks() {
        return Math.max(2, Math.min(maxRenderDistance, 2 + Math.round(renderDistance * (maxRenderDistance - 2))));
    }

    public static int getGameRendererChunks() {
        int chunks = getRenderDistanceChunks();

        if (ControlFeatures.USER_INTERFACE_CONFIG.videoSettingsConfig.vanillaFarValues && chunks == 12) {
            chunks = 16;
        }

        return chunks;
    }

    public static void setRenderDistanceChunks(int chunks) {
        renderDistance = (1.0F / (maxRenderDistance - 2)) * (chunks - 2);
    }

    public static int[] renderDistanceCycle = {12, 8, 4, 2};

    public static void cycleRenderDistance() {
        int[] cycle = renderDistanceCycle;
        boolean reverse = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        int current = getRenderDistanceChunks(), index = -1;
        for (int i = 0; i < cycle.length; i++) if (cycle[i] == current) { index = i; break; }
        int next = index < 0 ? (reverse ? cycle.length - 1 : 0) : Math.floorMod(index + (reverse ? -1 : 1), cycle.length);
        setRenderDistanceChunks(cycle[next]);
    }

    // FPS Limit
    public static float fpsLimit = 0.12F;

    public static int getFpsLimitValue() {
        return 5 + Math.round(fpsLimit * 1000F);
    }
    
    public static boolean isFramerateLimited() {
        return getFpsLimitValue() <= 1000;
    }

    // Clouds
    public static boolean clouds = true;
    public static float cloudHeight;

    public static float getCloudHeight() {
        return Math.round(108 + cloudHeight * 148);
    }

    // FOV
    public static float fov;

    public static int getFovInDegrees() {
        return Math.round(70.0f + fov * 40.0f);
    }
    
    public static int getFovOffsetInDegrees() {
        return Math.round(fov * 40.0f);
    }

    // Zoom
    public static float zoomFovOffset = 0;

    public static void addZoomFovOffset(float offset) {
        zoomFovOffset = offset;
    }

    // Fog Density
    public static float photoModeFogMultiplier = 1.0F;
    public static float fogDensity = 0.5F;

    public static float getFogMultiplier() {
        if (fogDensity == 0F) {
            return 100F;
        } else {
            return ((1F - Math.min(getFogDisplayValue(), 0.9F)) * 2F) * (photoModeFogMultiplier);
        }
    }

    public static float getFogDisplayValue() {
        return (float) Math.round(fogDensity * 20) / 20;
    }
}
