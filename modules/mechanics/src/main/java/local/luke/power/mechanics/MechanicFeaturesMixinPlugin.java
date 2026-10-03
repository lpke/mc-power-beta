package local.luke.power.mechanics;

import net.fabricmc.loader.api.FabricLoader;
import net.glasslauncher.mods.gcapi3.impl.GlassYamlFile;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class MechanicFeaturesMixinPlugin implements IMixinConfigPlugin {
    public static GlassYamlFile config;

    @Override
    public void onLoad(String mixinPackage) {


        config = new GlassYamlFile();
        try {
            config.loadFromString(local.luke.power.storage.PowerConfig.section("power_mechanics:config").toString());
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

        if (null != config.getConfigurationSection("EXPLOSION_AND_FIRE_CONFIG")) {
            Config.config.EXPLOSION_AND_FIRE_CONFIG.enableFireTurnsGrassIntoDirt = config.getConfigurationSection("EXPLOSION_AND_FIRE_CONFIG").getBoolean("enableFireTurnsGrassIntoDirt", false);
            Config.config.EXPLOSION_AND_FIRE_CONFIG.enableGhastFireballsToInstaKillGhasts = config.getConfigurationSection("EXPLOSION_AND_FIRE_CONFIG").getBoolean("enableGhastFireballsToInstaKillGhasts", false);
        }

        if (null != config.getConfigurationSection("MOB_CONFIG")) {
            Config.config.MOB_CONFIG.enableZombieDropItem = config.getConfigurationSection("MOB_CONFIG").getBoolean("enableZombieDropItem", false);
            Config.config.MOB_CONFIG.enableZombiePigmanDropItem = config.getConfigurationSection("MOB_CONFIG").getBoolean("enableZombiePigmanDropItem", false);
        }

        if (null != config.getConfigurationSection("FLORA_CONFIG")) {
            Config.config.FLORA_CONFIG.enableLogRotation = config.getConfigurationSection("FLORA_CONFIG").getBoolean("enableLogRotation", false);
        }

        if (mixinClassName.equals("local.luke.power.mechanics.mixin.FireBlockMixin")) {
            return Config.config.EXPLOSION_AND_FIRE_CONFIG.enableFireTurnsGrassIntoDirt;
        } else if (mixinClassName.equals("local.luke.power.mechanics.mixin.GhastDamageMixin")) {
            return Config.config.EXPLOSION_AND_FIRE_CONFIG.enableGhastFireballsToInstaKillGhasts;
        } else if (mixinClassName.equals("local.luke.power.mechanics.mixin.ZombieMixin")) {
            return Config.config.MOB_CONFIG.enableZombieDropItem;
        } else if (mixinClassName.equals("local.luke.power.mechanics.mixin.ZombiePigmanMixin")) {
            return Config.config.MOB_CONFIG.enableZombiePigmanDropItem;
        } else if (mixinClassName.equals("local.luke.power.mechanics.mixin.LogBlockMixin")) {
            return Config.config.FLORA_CONFIG.enableLogRotation;
        } else if (mixinClassName.equals("local.luke.power.mechanics.mixin.LogItemMixin")) {
            return Config.config.FLORA_CONFIG.enableLogRotation;
        } else if (mixinClassName.equals("local.luke.power.mechanics.mixin.client.BlockRenderManagerMixin")) {
            return Config.config.FLORA_CONFIG.enableLogRotation;
        } else {
            return true;
        }
    }
}
