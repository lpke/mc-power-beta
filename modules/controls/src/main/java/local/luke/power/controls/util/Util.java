package local.luke.power.controls.util;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.client.Minecraft;

public class Util {
    public static int atlasHeight = 256;

    public static void notify(String message, boolean notifyInChat) {
        ControlFeatures.LOGGER.info(message);
        if (Minecraft.INSTANCE.inGameHud != null && notifyInChat) {
            Minecraft.INSTANCE.inGameHud.addChatMessage("\u00a77" + message + "\u00a7r");
        }
    }

    public static int ceil(float value) {
        int i = (int) value;
        return value > (float) i ? i + 1 : i;
    }

    @SuppressWarnings("ManualMinMaxCalculation")
    public static float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    @SuppressWarnings("ManualMinMaxCalculation")
    public static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
