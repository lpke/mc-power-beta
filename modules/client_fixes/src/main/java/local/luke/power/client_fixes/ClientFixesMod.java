package local.luke.power.client_fixes;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientFixesMod implements ModInitializer {
    private static Logger LOGGER;
    private static ModMetadata METADATA;
    public static boolean commandsAvailable;

    @Override
    public void onInitialize() {
        ModContainer mod = FabricLoader.getInstance()
                .getModContainer("power_client_fixes")
                .orElseThrow(NullPointerException::new);

        METADATA = mod.getMetadata();
        LOGGER = LoggerFactory.getLogger(METADATA.getName());

        commandsAvailable = (  FabricLoader.getInstance().isModLoaded("spc")
                                || FabricLoader.getInstance().isModLoaded("power_commands")
                                );
    }

    public static Logger getLogger() {
        if (LOGGER == null) {
            throw new IllegalStateException("Logger not yet available");
        }

        return LOGGER;
    }

    public static ModMetadata getMetadata() {
        if (METADATA == null) {
            throw new NullPointerException("Metadata hasn't been populated yet");
        }

        return METADATA;
    }

    public static String getVersion() {
        return getMetadata().getVersion().getFriendlyString();
    }
}
