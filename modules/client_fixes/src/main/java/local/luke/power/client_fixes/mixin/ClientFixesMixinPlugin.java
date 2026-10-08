package local.luke.power.client_fixes.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.glasslauncher.mods.gcapi3.impl.GlassYamlFile;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import local.luke.power.client_fixes.Config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class ClientFixesMixinPlugin implements IMixinConfigPlugin {
    public static GlassYamlFile configObject;

    @Override
    public void onLoad(String mixinPackage) {


        configObject = new GlassYamlFile();
        try {
            configObject.loadFromString(local.luke.power.storage.PowerConfig.section("power_client_fixes:config").toString());
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
        Config.config.enableAuthenticationChanges = configObject.getBoolean("enableAuthenticationChanges", true);
        Config.config.enableControlsChanges = configObject.getBoolean("enableControlsChanges", true);
        Config.config.enableDebugGraphChanges = configObject.getBoolean("enableDebugGraphChanges", true);
        Config.config.enableDebugMenuWorldSeed = configObject.getBoolean("enableDebugMenuWorldSeed", true);
        Config.config.enableMultiplayerServerChanges = configObject.getBoolean("enableMultiplayerServerChanges", true);
        Config.config.enableChatChanges = configObject.getBoolean("enableChatChanges", true);
        Config.config.enableWoodenSignChanges = configObject.getBoolean("enableWoodenSignChanges", true);

        if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.controls.ControlsOptionsScreenMixin")) {
            return Config.config.enableControlsChanges;
        } else if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.controls.GameOptionsMixin")) {
            return Config.config.enableControlsChanges;
        } else if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.controls.KeyBindingMixin")) {
            return Config.config.enableControlsChanges;
        } else if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.misc.ChatKeyMixin")) {
            return Config.config.enableChatChanges;
        } else if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.misc.DebugGraphMixin")) {
            return Config.config.enableDebugGraphChanges;
        } else if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.misc.InGameHudMixin")) {
            return Config.config.enableDebugMenuWorldSeed;
        } else if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.misc.MinecraftMixin")) {
            return Config.config.enableAuthenticationChanges;
        } else if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.misc.ScreenMixin")) {
            return (Config.config.enableControlsChanges || Config.config.enableMultiplayerServerChanges);
        } else if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.multiplayer.ReturnToMainMenuMixin")) {
            return Config.config.enableMultiplayerServerChanges;
        } else if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.multiplayer.TitleScreenMixin")) {
            return Config.config.enableMultiplayerServerChanges;
        } else if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.multiplayer.TranslationStorageMixin")) {
            boolean isStationApiLoaded = FabricLoader.getInstance().isModLoaded("stationapi");
            return (Config.config.enableMultiplayerServerChanges && !isStationApiLoaded);
        } else if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.text.TextFieldWidgetMixin")) {
            return (Config.config.enableMultiplayerServerChanges || Config.config.enableChatChanges || Config.config.enableWoodenSignChanges);
        } else if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.text.chat.ChatScreenMixin")) {
            return Config.config.enableChatChanges;
        } else if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.text.chat.SleepingChatScreenMixin")) {
            return Config.config.enableChatChanges;
        } else if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.text.sign.ClientNetworkHandlerMixin")) {
            return Config.config.enableWoodenSignChanges;
        } else if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.text.sign.SignBlockEntityMixin")) {
            return Config.config.enableWoodenSignChanges;
        } else if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.text.sign.SignBlockEntityRendererMixin")) {
            return Config.config.enableWoodenSignChanges;
        } else if (mixinClassName.equals("local.luke.power.client_fixes.mixin.client.text.sign.SignEditScreenMixin")) {
            return Config.config.enableWoodenSignChanges;
        } else {
            return true;
        }
    }
}
