package local.luke.power.controls.mixin;

import local.luke.power.controls.ControlFeatures;
import net.fabricmc.loader.api.FabricLoader;
import net.glasslauncher.mods.gcapi3.impl.GlassYamlFile;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ControlFeaturesMixinPlugin implements IMixinConfigPlugin {
    public static GlassYamlFile ui_config;

    @Override
    public void onLoad(String mixinPackage) {


        ui_config = new GlassYamlFile();
        try {
            ui_config.loadFromString(local.luke.power.storage.PowerConfig.section("power_controls:userinterface").toString());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null; // null = default behaviour
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null; // null = I don't wish to append any mixin
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Cloud Height Slider


        // Clouds Toggle
        //if (isDisabled(mixinClassName, "tweaks.cloudstoggle.WorldRendererMixin", ui_config, "videoSettingsConfig.cloudsToggle")) {
        //    return false;
        //}

        // FPS Limit Slider


        // GUI Scale Slider


        // Render Distance




        if (FabricLoader.getInstance().isModLoaded("stationapi")) {
            if (nonStationMixins.contains(mixinClassName)) {
                ControlFeatures.LOGGER.info("StationAPI Detected. Skipping mixin " + mixinClassName);
                return false;
            }
        }

        if (FabricLoader.getInstance().isModLoaded("nitch")) {
            if (mixinClassName.contains("local.luke.power.controls.mixin.bugfixes.torchbottomfix")) {
                ControlFeatures.LOGGER.info("Nitch Detected. Skipping mixin " + mixinClassName);
                return false;
            }
        }

        return true;
    }

    public static ArrayList<String> nonStationMixins = new ArrayList<>() {{
        add("local.luke.power.controls.mixin.bugfixes.droppeditemfix.ItemRendererMixin");
        add("local.luke.power.controls.mixin.hooks.GameOptionsMixin");
        add("local.luke.power.controls.mixin.hooks.MinecraftMixin");
        add("local.luke.power.controls.mixin.tweaks.mipmap.TextureManagerMixin");
        add("local.luke.power.controls.mixin.tweaks.recipes.FurnaceBlockEntityMixin");
        add("local.luke.power.controls.mixin.tweaks.recipes.BlockMixin");
    }};

    public static boolean isDisabled(String mixinClassName, String mixinName, GlassYamlFile config, String configBool) {
        if (config.contains(configBool)) {
            return mixinClassName.equals("local.luke.power.controls.mixin." + mixinName) && !config.getBoolean(configBool);
        }
        return false;
    }
}
