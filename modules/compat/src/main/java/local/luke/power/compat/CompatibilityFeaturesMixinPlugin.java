package local.luke.power.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.glasslauncher.mods.gcapi3.impl.GlassYamlFile;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class CompatibilityFeaturesMixinPlugin implements IMixinConfigPlugin {
    public static GlassYamlFile config;

    @Override
    public void onLoad(String mixinPackage) {


        config = new GlassYamlFile();
        try {
            config.loadFromString(local.luke.power.storage.PowerConfig.section("power_compat:config").toString());
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
        Config.config.slabPlacementFixesEnabled = config.getBoolean("slabPlacementFixesEnabled", true);

        if (mixinClassName.equals("local.luke.power.compat.mixin.modernslabplacement.SlabBlockItemMixin")) {
            return Config.config.slabPlacementFixesEnabled;
        } else {
            return true;
        }
    }
}
