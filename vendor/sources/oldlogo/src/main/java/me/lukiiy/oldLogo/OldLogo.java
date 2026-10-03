package me.lukiiy.oldLogo;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class OldLogo implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("oldlogo");
    public static Minecraft minecraft;
    public static final Config CONFIG = new Config("oldLogo");

    public static boolean isLogoRendering = false;

    public static ConfigKey<String[]> logo = ConfigKey.stringArray("logo.lines", new String[]{
            " *   * * *   * *** *** *** *** *** ***",
            " ** ** * **  * *   *   * * * * *    * ",
            " * * * * * * * **  *   **  *** **   * ",
            " *   * * *  ** *   *   * * * * *    * ",
            " *   * * *   * *** *** * * * * *    * "
    });
    public static ConfigKey<float[]> logoRotation = ConfigKey.floatArray("logo.rotation", new float[]{ 15f, 0, 0 });
    public static ConfigKey<Map<Character, Integer>> blockMap;
    public static ConfigKey<Boolean> invertedBlocks = ConfigKey.bool("blocks.inverted", true);
    public static ConfigKey<Boolean> scrollingBackground = ConfigKey.bool("scrollingBackground.enabled", false);
    public static ConfigKey<Double> scrollingBackgroundSpeed = ConfigKey.doubleVal("scrollingBackground.speed", 0.1);
    public static ConfigKey<Boolean> scrollingBackgroundRestricted = ConfigKey.bool("scrollingBackground.onlyTitle", false);
    public static ConfigKey<Boolean> shadow = ConfigKey.bool("logo.shadow", true);
    public static ConfigKey<Boolean> animation = ConfigKey.bool("logo.animation.enabled", true);
    public static ConfigKey<Double> animationSpeed = ConfigKey.doubleVal("logo.animation.speed", 1);

    static {
        Map<Character, Integer> defaultMap = new LinkedHashMap<>();
        defaultMap.put('*', 1);

        blockMap = ConfigKey.charMap("blocks.map", defaultMap);
    }

    @Override
    public void onInitialize() {

    }
}
